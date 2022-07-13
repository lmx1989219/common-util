package com.lmx.common.oplog;

import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.util.JdbcConstants;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.transaction.SpringManagedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * 目前只适用mysql
 * <p>
 * 基于dbType很容易扩展
 */
@Intercepts({
        @Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
public class OpMybatisInterceptor implements Interceptor {
    private Executor target;
    private Logger log = LoggerFactory.getLogger(OpMybatisInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = null;
        if (invocation.getArgs().length > 1) {
            parameter = invocation.getArgs()[1];
        }
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        Configuration configuration = mappedStatement.getConfiguration();
        //得到sql语句
        String sql = showSql(configuration, boundSql);
        log.info("operator found biz sql={}", sql);
        //获取SQL类型
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        List cache = Lists.newArrayList();
        //更新前 查询
        if (ContextHolder.hasContext()) {
            sql = sql.toLowerCase();
            try {
                this.buildQuerySql(sql, cache);
            } catch (Exception e) {
                log.error("operator help query error", e);
            }
        }
        Object returnObj = invocation.proceed();
        //针对update埋点抓取
        //事务提交成功则记录日志
        if (returnObj instanceof Integer && (Integer) returnObj > 0 && ContextHolder.hasContext()) {
            sql = sql.toLowerCase();
            try {
                ContextHolder.set(this.compareDiff(sql, cache));
            } catch (Exception e) {
                log.error("operator log error", e);
            }
        }
        return returnObj;
    }

    private Connection getConnect() {
        org.apache.ibatis.transaction.Transaction transaction = this.target.getTransaction();
        if (transaction == null) {
            log.error(String.format("Could not find transaction on target [%s]", this.target));
            return null;
        }
        if (transaction instanceof SpringManagedTransaction) {
            try {
                return transaction.getConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        log.error(String.format("---the transaction is not SpringManagedTransaction:%s", transaction.getClass().toString()));

        return null;
    }

    /**
     * 解析sql语句
     *
     * @param configuration
     * @param boundSql
     * @return
     */
    public String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));

            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        if (propertyName.contains("createTime") || propertyName.contains("updateTime")) {
                            sql = sql.replaceFirst("\\?", "'" + Matcher.quoteReplacement(getParameterValue(obj)) + "'");
                        } else {
                            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                        }
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        if (propertyName.contains("createTime") || propertyName.contains("updateTime")) {
                            sql = sql.replaceFirst("\\?", "'" + Matcher.quoteReplacement(getParameterValue(obj)) + "'");
                        } else {
                            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                        }
                    }
                }
            }
        }
        return sql;
    }

    /**
     * 参数解析
     *
     * @param obj
     * @return
     */
    private String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(new Date()) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        return value;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            this.target = (Executor) target;
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private void buildQuerySql(String sql, List cache) {
        long start = System.currentTimeMillis();
        SQLStatementParser sqlStatementParser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
        if (sql.toLowerCase().startsWith("update")) {
            SQLUpdateStatement sqlUpdateStatement = sqlStatementParser.parseUpdateStatement();
            List<SQLObject> sqlObjects = sqlUpdateStatement.getWhere().getChildren();
            if (CollectionUtils.isEmpty(sqlObjects)) {
                log.warn("不支持全表更新埋点");
                return;
            }
            StringBuilder whereCondition = new StringBuilder();
            for (int i = 0; i < sqlObjects.size(); i++) {
                if (i % 2 == 0)
                    whereCondition.append(sqlObjects.get(i) + "=");
                else
                    whereCondition.append(sqlObjects.get(i) + " ");
            }
            qryData(sqlUpdateStatement, whereCondition, cache);
        }
        log.info("operator help query cost {}ms", System.currentTimeMillis() - start);
    }

    private void qryData(SQLUpdateStatement sqlUpdateStatement, StringBuilder whereCondition, List cache) {
        String tableName = sqlUpdateStatement.getTableName().getSimpleName();
        List<SQLUpdateSetItem> sqlUpdateSetItems = sqlUpdateStatement.getItems();
        List list = Lists.newArrayList();
        sqlUpdateSetItems.forEach(e -> list.add(e.getColumn()));
        String selectColumns = Joiner.on(",").join(list);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select").append(" ").append(selectColumns).append(" ");
        stringBuilder.append("from").append(" ").append(tableName).append(" ");
        stringBuilder.append("where").append(" ").append(whereCondition.toString());
        String querySQL = stringBuilder.toString();
        log.info("operator exec querySQL={}", querySQL);

        Connection conn = this.getConnect();
        try (Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery(querySQL);) {
            while (rs.next()) {
                for (int i = 1; i <= list.size(); i++) {
                    cache.add(rs.getString(i));
                }
            }
            log.info("operator cache ={}", cache);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private List compareDiff(String sql, List cache) {
        SQLStatementParser sqlStatementParser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
        List dataDiff = Lists.newArrayList();
        List dataModify = Lists.newArrayList();
        if (sql.toLowerCase().startsWith("update")) {
            List cache_ = Lists.newArrayList();
            buildQuerySql(sql, cache_);
            dataModify.addAll(cache_);
            for (int i = 0; i < cache.size(); i++) {
                if (!cache.get(i).equals(dataModify.get(i))) {
                    dataDiff.add(cache.get(i));//before
                    dataDiff.add(dataModify.get(i));//after
                }
            }
        } else if (sql.toLowerCase().startsWith("insert")) {
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement) sqlStatementParser.parseInsert();
            sqlInsertStatement.getChildren();
        }
        return dataDiff;
    }
}
