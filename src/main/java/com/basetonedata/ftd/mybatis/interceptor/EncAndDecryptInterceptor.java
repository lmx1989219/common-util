//package com.basetonedata.ftd.mybatis.interceptor;
//
//import com.google.common.collect.Lists;
//import com.basetonedata.ftd.mybatis.annotation.EncDecrypt;
//import com.basetonedata.ftd.mybatis.service.EncAndDecryptService;
//import com.basetonedata.ftd.mybatis.service.impl.DefaultAesServiceImpl;
//import lombok.AllArgsConstructor;
//import org.apache.ibatis.annotations.Param;
//import org.apache.ibatis.executor.Executor;
//import org.apache.ibatis.mapping.MappedStatement;
//import org.apache.ibatis.plugin.*;
//import org.apache.ibatis.session.ResultHandler;
//import org.apache.ibatis.session.RowBounds;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.lang.reflect.Parameter;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
///**
// * 实体字段加解密拦截器
// * <p>
// * Created by lucas on 2020/7/8.
// */
//@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
//        , @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
//public class EncAndDecryptInterceptor implements Interceptor {
//    private static final String NAME_ENTITY = "et", NATIVE_SQL_ENTITY_PREFIX = "param", NATIVE_SQL_ENTITY = "param1";
//    private final Map<Class<?>, List<Field>> versionFieldCache = new ConcurrentHashMap<>();
//    private EncAndDecryptService encAndDecryptService;
//
//    public EncAndDecryptInterceptor(EncAndDecryptService encAndDecryptService) {
//        this.encAndDecryptService = encAndDecryptService;
//    }
//
//    public EncAndDecryptInterceptor() {
//        this.encAndDecryptService = new DefaultAesServiceImpl();
//    }
//
//    @AllArgsConstructor
//    static class EncField {
//        String paramName;
//        int idx;
//    }
//
//    @Override
//    @SuppressWarnings("unchecked")
//    public Object intercept(Invocation invocation) throws Throwable {
//        Object[] args = invocation.getArgs();
//        Object param = args[1];
//        MappedStatement mappedStatement = (MappedStatement) args[0];
//
//        //可能存在param的顺序和通过反射拿到的参数顺序不一致,后续需要处理下面的逻辑
//        Method originMethod = getMethod(mappedStatement.getId());
//        List<EncField> needEncList = Lists.newArrayList();
//        int i = 0;
//        for (Parameter parameter : originMethod.getParameters()) {
//            EncDecrypt encDecrypt = parameter.getDeclaredAnnotation(EncDecrypt.class);
//            Param param1 = parameter.getDeclaredAnnotation(Param.class);
//            if (Objects.nonNull(encDecrypt)) {
//                needEncList.add(new EncField(param1.value(), (i++) + 1));
//            }
//        }
//
//        // entity = et
//        Object et = null;
//        //处理@Param注解的参数
//        if (param instanceof Map) {
//            Map map = (Map) param;
//            if (map.containsKey(NAME_ENTITY)) {
//                et = map.get(NAME_ENTITY);
//            }
//            if (map.containsKey(NATIVE_SQL_ENTITY)) {
//                // non wrapper;
//                et = map.get(NATIVE_SQL_ENTITY);
//            }
//            if (et != null) {
//                if (invocation.getMethod().getName().equals("update")) {
//                    handlerEnc(et);
//                    return invocation.proceed();
//                }
//                if (invocation.getMethod().getName().equals("query")) {
//                    if (!needEncList.isEmpty()) {
//                        for (EncField encField : needEncList) {
//                            String key = NATIVE_SQL_ENTITY_PREFIX + encField.idx;
//                            et = ((Map) param).get(key);
//                            if (et instanceof String) {
//                                map.put(encField.paramName, encryptField(et));
//                                map.put(key, encryptField(et));
//                            }
//                        }
//                    } else {
//                        //查询参数明文转换为密文
//                        handlerEnc(et);
//                    }
//                    //解密
//                    Object resp = invocation.proceed();
//                    return handlerDecrypt(resp);
//                }
//            }
//        } else {//无param注解参数的处理
//            if (invocation.getMethod().getName().equals("update")) {
//                handlerEnc(param);
//                return invocation.proceed();
//            }
//            if (invocation.getMethod().getName().equals("query")) {
//                //查询参数明文转换为密文
//                handlerEnc(param);
//                //解密
//                Object resp = invocation.proceed();
//                return handlerDecrypt(resp);
//            }
//        }
//        return invocation.proceed();
//    }
//
//    private void handlerEnc(Object param) throws Exception {
//        Class<?> entityClass = param.getClass();
//
//        List<Field> encDecryptField = this.getEncDecryptField(entityClass);
//        if (Objects.isNull(encDecryptField)) {
//            return;
//        }
//        for (Field field : encDecryptField) {
//            field.setAccessible(true);
//            Object originalVal = field.get(param);
//            Object encryptVal = encryptField(originalVal);
//            if (originalVal != null) {
//                field.set(param, encryptVal);
//            }
//        }
//    }
//
//    private Object handlerDecrypt(Object resp) throws Exception {
//        if (resp instanceof List) {
//            List<Object> resultList = ((List) resp);
//            for (Object entity : resultList) {
//                for (Field field : entity.getClass().getDeclaredFields()) {
//                    if (field.getDeclaredAnnotation(EncDecrypt.class) != null) {
//                        field.setAccessible(true);
//                        Object enc = field.get(entity);
//                        if (Objects.nonNull(enc))
//                            field.set(entity, decryptField(enc));
//                    }
//                }
//            }
//        }
//        return resp;
//    }
//
//    /**
//     * 获取mapper定义的方法
//     *
//     * @return Method
//     */
//    private Method getMethod(String statementId) throws ClassNotFoundException {
//        final Class clazz = Class.forName(statementId.substring(0, statementId.lastIndexOf(".")));
//        final String methodName = statementId.substring(statementId.lastIndexOf(".") + 1);
//        for (Method method : clazz.getMethods()) {
//            if (method.getName().equals(methodName)) {
//                return method;
//            }
//        }
//        return null;
//    }
//
//    private Object encryptField(Object originalEncDecryptVal) throws Exception {
//        return encAndDecryptService == null ? originalEncDecryptVal : encAndDecryptService.encrypt(String.valueOf(originalEncDecryptVal));
//    }
//
//    private Object decryptField(Object originalEncDecryptVal) throws Exception {
//        return encAndDecryptService == null ? originalEncDecryptVal : encAndDecryptService.decrypt(String.valueOf(originalEncDecryptVal));
//    }
//
//
//    @Override
//    public Object plugin(Object target) {
//        if (target instanceof Executor) {
//            return Plugin.wrap(target, this);
//        }
//        return target;
//    }
//
//    @Override
//    public void setProperties(Properties properties) {
//        // to do nothing
//    }
//
//    private List<Field> getEncDecryptField(Class<?> parameterClass) {
//        try {
//            synchronized (parameterClass.getName()) {
//                if (versionFieldCache.containsKey(parameterClass)) {
//                    return versionFieldCache.get(parameterClass);
//                }
//                // 缓存类信息
//                List<Field> fields = Lists.newArrayList(parameterClass.getDeclaredFields())
//                        .stream().filter(field -> field.getDeclaredAnnotation(EncDecrypt.class) != null)
//                        .collect(Collectors.toList());
//                if (!fields.isEmpty()) {
//                    versionFieldCache.put(parameterClass, fields);
//                    return fields;
//                }
//                return null;
//            }
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//}
