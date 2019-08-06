package com.lmx.common.oplog;

import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.util.JdbcConstants;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.transaction.SpringManagedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;

@Intercepts({
        @Signature(method = "query", type = Executor.class, args = {
                MappedStatement.class, Object.class, RowBounds.class,
                ResultHandler.class}),
        @Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
public class OpMybatisInterceptor implements Interceptor {
    private Executor target;
    private Logger log = LoggerFactory.getLogger(OpMybatisInterceptor.class);
    private ThreadLocal cachesQry = new ThreadLocal();//缓存查询结果

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
        if (ContextHolder.hasContext() && sqlCommandType == SqlCommandType.UPDATE
                && cachesQry.get() == null) {
            sql = sql.toLowerCase();
            long start = System.currentTimeMillis();
            try {
                this.buildQuerySql(sql, cache);
                log.info("operator help query cost {}ms", System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.error("operator help query error", e);
            }
        }
        Object returnObj = invocation.proceed();
        //针对update埋点抓取
        if (sqlCommandType == SqlCommandType.UPDATE) {
            //事务提交成功则记录日志
            if (returnObj instanceof Integer && (Integer) returnObj > 0 && ContextHolder.hasContext()) {
                sql = sql.toLowerCase();
                try {
                    ContextHolder.set(cachesQry);
                    ContextHolder.set(this.compareDiff(sql, cache));
                } catch (Exception e) {
                    log.error("operator log error", e);
                } finally {
                    //每次比对完成，需要释放
                    cachesQry.remove();
                }
            }
        }
        if (!(returnObj instanceof Integer) && ContextHolder.hasContext()) {
            //缓存当前线程查询的数据
            if (cachesQry.get() == null) {
                List list = Lists.newArrayList();
                //注意：这里以文本缓存，对象需要深度复制，否则会因为持久完成被更新掉
                list.add(new Gson().toJson(returnObj));
                cachesQry.set(list);
            }
        }
        return returnObj;
    }

    private javax.sql.DataSource getDataSource() {
        org.apache.ibatis.transaction.Transaction transaction = this.target.getTransaction();
        if (transaction == null) {
            log.error(String.format("Could not find transaction on target [%s]", this.target));
            return null;
        }
        if (transaction instanceof SpringManagedTransaction) {
            String fieldName = "dataSource";
            Field field = ReflectionUtils.findField(transaction.getClass(), fieldName, javax.sql.DataSource.class);

            if (field == null) {
                log.error(String.format("Could not find field [%s] of type [%s] on target [%s]",
                        fieldName, javax.sql.DataSource.class, this.target));
                return null;
            }

            ReflectionUtils.makeAccessible(field);
            javax.sql.DataSource dataSource = (javax.sql.DataSource) ReflectionUtils.getField(field, transaction);
            return dataSource;
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
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
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
        SQLStatementParser sqlStatementParser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
        SQLUpdateStatement sqlUpdateStatement = sqlStatementParser.parseUpdateStatement();
        List<SQLObject> sqlObjects = sqlUpdateStatement.getWhere().getChildren();
        StringBuilder whereCondition = new StringBuilder();
        for (int i = 0; i < sqlObjects.size(); i++) {
            if (i % 2 == 0)
                whereCondition.append(sqlObjects.get(i) + "=");
            else
                whereCondition.append(sqlObjects.get(i) + " ");
        }
        String tableName = sqlUpdateStatement.getTableName().getSimpleName();
        List<SQLUpdateSetItem> sqlUpdateSetItems = sqlUpdateStatement.getItems();
        List list = Lists.newArrayList();
        sqlUpdateSetItems.forEach(e -> list.add(e.getColumn()));
        String selectColums = Joiner.on(",").join(list);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select").append(" ").append(selectColums).append(" ");
        stringBuilder.append("from").append(" ").append(tableName).append(" ");
        stringBuilder.append("where").append(" ").append(whereCondition.toString());
        String querySQL = stringBuilder.toString();
        log.info("operator exec querySQL={}", querySQL);

        DataSource dataSource = this.getDataSource();
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery(querySQL)) {
            while (rs.next()) {
                for (int i = 1; i <= list.size(); i++) {
                    String val = rs.getString(i);
                    cache.add(val);
                }
            }
            log.info("operator cache ={}", cache);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private List compareDiff(String sql, List cache) {
        SQLStatementParser sqlStatementParser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
        SQLUpdateStatement sqlUpdateStatement = sqlStatementParser.parseUpdateStatement();
        List<SQLUpdateSetItem> sqlUpdateSetItems = sqlUpdateStatement.getItems();
        List data = Lists.newArrayList();
        List data_ = Lists.newArrayList();
        for (int i = 0; i < sqlUpdateSetItems.size(); i++) {
            SQLUpdateSetItem sqlUpdateSetItem = sqlUpdateSetItems.get(i);
            String field = sqlUpdateSetItem.getColumn().toString();
            String value = sqlUpdateSetItem.getValue().toString();
            List<String> list = (List) cachesQry.get();
            if (list != null) {
                for (String object : list) {
                    List l = new Gson().fromJson(object, List.class);
                    for (Object o : l) {
                        Map m = (Map) o;
                        String v;
                        Object obj = m.get(field);
                        //处理sql计算的值更新
                        if (value.contains("+")) {
                            String[] val = value.split("\\+");
                            String incrVal = val[1];
                            if (obj instanceof Double) {
                                value = String.valueOf((Double) obj + Double.valueOf(incrVal));
                            }
                        }
                        if (obj instanceof Double) {
                            value = String.valueOf(Double.valueOf(value));
                        }
                        if ((v = (String.valueOf(obj))) != null && !v.equals(value)) {
                            data.add(v);//before
                            data.add(value);//after
                        }
                    }
                }
            } else {
                data_.add(value);
            }
        }
        for (int i = 0; i < cache.size(); i++) {
            if (!cache.get(i).equals(data_.get(i))) {
                data.add(cache.get(i));//before
                String value = (String) data_.get(i);
                //处理sql计算的值更新
                if (value.contains("+")) {
                    String[] val = value.split("\\+");
                    String incrVal = val[1];
                    try {
                        Double oldVal = Double.parseDouble((String) cache.get(i));
                        value = String.valueOf(oldVal + Double.valueOf(incrVal));
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }
                data.add(value);//after
            }
        }
        return data;
    }
}
