package com.basetonedata.ftd.mybatis.interceptor;

import com.basetonedata.ftd.mybatis.annotation.EncDecrypt;
import com.basetonedata.ftd.mybatis.service.EncAndDecryptService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * 结果集拦截处理
 * <p>推荐用这个</p>
 *
 * @auth lucas
 * @date 2020-07-13
 */
@Intercepts({@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = Statement.class)})
@Slf4j
public class ResultInterceptor implements Interceptor {

    private EncAndDecryptService encryptDecrypt;

    public ResultInterceptor(EncAndDecryptService encryptDecrypt) {
        this.encryptDecrypt = encryptDecrypt;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        if (invocation.getTarget() instanceof ResultSetHandler) {
            Object result = invocation.proceed();
            if (Objects.isNull(result)) {
                return null;
            }
            if (result instanceof List) {
                List<?> resultList = ((List<?>) result);
                if (!CollectionUtils.isEmpty(resultList)) {
                    for (Object entity : resultList) {
                        if (Objects.nonNull(entity)) {
                            Field[] fields = entity.getClass().getDeclaredFields();
                            for (Field field : fields) {
                                if (field.getDeclaredAnnotation(EncDecrypt.class) != null) {
                                    field.setAccessible(true);
                                    Object enc = field.get(entity);
                                    if (Objects.nonNull(enc) && Objects.nonNull(encryptDecrypt)) {
                                        field.set(entity, encryptDecrypt.decrypt(String.valueOf(enc)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}

