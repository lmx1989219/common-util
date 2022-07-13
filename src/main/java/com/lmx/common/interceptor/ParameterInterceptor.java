package com.lmx.common.interceptor;

import com.google.common.collect.Lists;
import com.lmx.common.annotation.EncDecrypt;
import com.lmx.common.service.EncAndDecryptService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 参数拦截处理
 * <p>推荐用这个</p>
 *
 * @auth lucas
 * @date 2020-07-13
 */
@Intercepts({@Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})})
@Slf4j
public class ParameterInterceptor implements Interceptor {
    private static final String NATIVE_SQL_ENTITY_PREFIX = "param", NATIVE_SQL_ENTITY = "param1";
    private EncAndDecryptService encryptDecrypt;

    public ParameterInterceptor(EncAndDecryptService encryptDecrypt) {
        this.encryptDecrypt = encryptDecrypt;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
        Field parameterField = parameterHandler.getClass().getDeclaredField("parameterObject");
        parameterField.setAccessible(true);
        Object parameterObject = parameterField.get(parameterHandler);
        if (parameterObject instanceof Map) {
            Object target = invocation.getTarget();
            Field field = target.getClass().getDeclaredField("mappedStatement");
            field.setAccessible(true);
            MappedStatement mappedStatement = (MappedStatement) field.get(target);
            Method originMethod = getMethod(mappedStatement.getId());
            List<EncField> needEncList = Lists.newArrayList();
            int i = 0;
            if (Objects.nonNull(originMethod.getParameters())) {
                for (Parameter parameter : originMethod.getParameters()) {
                    EncDecrypt encDecrypt = parameter.getDeclaredAnnotation(EncDecrypt.class);
                    Param param1 = parameter.getDeclaredAnnotation(Param.class);
                    if (Objects.nonNull(encDecrypt)) {
                        needEncList.add(new EncField(param1.value(), (i++) + 1));
                    }
                }
            }
            Map<String, Object> map = (Map<String, Object>) parameterObject;
            Object et = null;
            if (map.containsKey(NATIVE_SQL_ENTITY)) {
                et = map.get(NATIVE_SQL_ENTITY);
            }
            if (et != null) {
                if (!needEncList.isEmpty()) {
                    for (EncField encField : needEncList) {
                        String key = NATIVE_SQL_ENTITY_PREFIX + encField.idx;
                        et = ((Map) parameterObject).get(key);
                        if (et instanceof String && Objects.nonNull(encryptDecrypt)) {
                            map.put(encField.paramName, encryptDecrypt.encrypt(et.toString()));
                            map.put(key, encryptDecrypt.encrypt(et.toString()));
                        }
                        if (et instanceof Long && Objects.nonNull(encryptDecrypt)) {
                            map.put(encField.paramName, Long.valueOf(encryptDecrypt.encrypt(et.toString())));
                            map.put(key, Long.valueOf(encryptDecrypt.encrypt(et.toString())));
                        }
                    }
                } else {
                    encField(et);
                }
                return invocation.proceed();
            }
        } else {
            encField(parameterObject);
        }
        return invocation.proceed();
    }

    private void encField(Object param) {
        if (Objects.isNull(param)) {
            return;
        }
        Class<?> parameterObjectClass = param.getClass();
        Field[] declaredFields = parameterObjectClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.getDeclaredAnnotation(EncDecrypt.class) != null) {
                declaredField.setAccessible(true);
                try {
                    Object originalVal = declaredField.get(param);
                    if (Objects.nonNull(originalVal) && Objects.nonNull(encryptDecrypt)) {
                        Object encryptVal = encryptDecrypt.encrypt(originalVal.toString());
                        declaredField.set(param, encryptVal);
                    }
                } catch (IllegalAccessException e) {
                    log.error("", e);
                }
            }
        }
    }

    /**
     * 获取mapper定义的方法
     *
     * @return Method
     */
    private Method getMethod(String statementId) throws ClassNotFoundException {
        final Class clazz = Class.forName(statementId.substring(0, statementId.lastIndexOf(".")));
        final String methodName = statementId.substring(statementId.lastIndexOf(".") + 1);
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
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

    @AllArgsConstructor
    class EncField {
        String paramName;
        int idx;
    }
}

