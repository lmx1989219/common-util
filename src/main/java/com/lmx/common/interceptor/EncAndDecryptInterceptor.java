package com.lmx.common.interceptor;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.google.common.collect.Lists;
import com.lmx.common.annotation.EncDecrypt;
import com.lmx.common.service.EncAndDecryptService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * 实体字段加解密拦截器
 * Created by lucas on 2020/7/8.
 */
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
  , @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class EncAndDecryptInterceptor implements Interceptor {
  private static final String NAME_ENTITY = Constants.ENTITY, NATIVE_SQL_ENTITY_PREFIX = "param", NATIVE_SQL_ENTITY = "param1";
  private final Map<Class<?>, EntityField> versionFieldCache = new ConcurrentHashMap<>();
  private EncAndDecryptService encAndDecryptService;

  public EncAndDecryptInterceptor(EncAndDecryptService encAndDecryptService) {
    this.encAndDecryptService = encAndDecryptService;
  }

  public EncAndDecryptInterceptor() {
  }

  @AllArgsConstructor
  class EncField {
    String paramName;
    int idx;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object intercept(Invocation invocation) throws Throwable {
    Object[] args = invocation.getArgs();
    Object param = args[1];
    MappedStatement mappedStatement = (MappedStatement) args[0];

    Method originMethod = getMethod(mappedStatement.getId());
    List<EncField> needEncList = Lists.newArrayList();
    int i = 0;
    for (Parameter parameter : originMethod.getParameters()) {
      EncDecrypt encDecrypt = parameter.getDeclaredAnnotation(EncDecrypt.class);
      Param param1 = parameter.getDeclaredAnnotation(Param.class);
      if (Objects.nonNull(encDecrypt)) {
        needEncList.add(new EncField(param1.value(), (i++) + 1));
      }
    }

    // entity = et
    Object et = null, ew = null;
    if (param instanceof Map) {
      Map map = (Map) param;
      if (map.containsKey(NAME_ENTITY)) {
        et = map.get(NAME_ENTITY);
      }
      if (map.containsKey(NATIVE_SQL_ENTITY)) {
        // non wrapper;
        et = map.get(NATIVE_SQL_ENTITY);
      }
      if (et != null) {
        if (invocation.getMethod().getName().equals("update")) {
          handlerEnc(et);
          return invocation.proceed();
        }
        if (invocation.getMethod().getName().equals("query")) {
          if (!needEncList.isEmpty()) {
            for (EncField encField : needEncList) {
              String key = NATIVE_SQL_ENTITY_PREFIX + encField.idx;
              et = ((Map) param).get(key);
              if (et instanceof String) {
                map.put(encField.paramName, encryptField(et));
                map.put(key, encryptField(et));
              }
            }
          } else {
            //查询参数明文转换为密文
            handlerEnc(et);
          }
          //解密
          Object resp = invocation.proceed();
          return handlerDecrypt(resp);
        }
      }
    } else {
      if (invocation.getMethod().getName().equals("update")) {
        handlerEnc(param);
        return invocation.proceed();
      }
      if (invocation.getMethod().getName().equals("query")) {
        //查询参数明文转换为密文
        handlerEnc(param);
        //解密
        Object resp = invocation.proceed();
        return handlerDecrypt(resp);
      }
    }
    return invocation.proceed();
  }

  private void handlerEnc(Object param) throws Exception {
    Class<?> entityClass = param.getClass();
    TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
    while (tableInfo == null && entityClass != null) {
      entityClass = ClassUtils.getUserClass(entityClass.getSuperclass());
      tableInfo = TableInfoHelper.getTableInfo(entityClass);
    }
    EntityField encDecryptField = this.getEncDecryptField(entityClass, tableInfo);
    if (encDecryptField == null) {
      return;
    }
    Field field = encDecryptField.getField();
    Object originalVal = encDecryptField.getField().get(param);
    Object encryptVal = encryptField(originalVal);
    if (originalVal != null) {
      //加密
      field.set(param, encryptVal);
    }
  }

  private Object handlerDecrypt(Object resp) throws Exception {
    if (resp instanceof List) {
      List<Object> resultList = ((List) resp);
      if (CollectionUtils.isNotEmpty(resultList)) {
        for (Object entity : resultList) {
          for (Field field : entity.getClass().getDeclaredFields()) {
            if (field.getDeclaredAnnotation(EncDecrypt.class) != null) {
              field.setAccessible(true);
              Object enc = field.get(entity);
              field.set(entity, decryptField(enc));
            }
          }
        }
      }
    }
    return resp;
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

  private Object encryptField(Object originalEncDecryptVal) throws Exception {
    return encAndDecryptService == null ? originalEncDecryptVal : encAndDecryptService.encrypt(String.valueOf(originalEncDecryptVal));
  }

  private Object decryptField(Object originalEncDecryptVal) throws Exception {
    return encAndDecryptService == null ? originalEncDecryptVal : encAndDecryptService.decrypt(String.valueOf(originalEncDecryptVal));
  }


  @Override
  public Object plugin(Object target) {
    if (target instanceof Executor) {
      return Plugin.wrap(target, this);
    }
    return target;
  }

  @Override
  public void setProperties(Properties properties) {
    // to do nothing
  }

  private EntityField getEncDecryptField(Class<?> parameterClass, TableInfo tableInfo) {
    try {
      synchronized (parameterClass.getName()) {
        if (versionFieldCache.containsKey(parameterClass)) {
          return versionFieldCache.get(parameterClass);
        }
        // 缓存类信息
        EntityField field = this.getVersionFieldRegular(parameterClass, tableInfo);
        if (field != null) {
          versionFieldCache.put(parameterClass, field);
          return field;
        }
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * <p>
   * 反射检查参数类是否启动加解密
   * </p>
   *
   * @param parameterClass 实体类
   * @param tableInfo      实体数据库反射信息
   * @return
   */
  private EntityField getVersionFieldRegular(Class<?> parameterClass, TableInfo tableInfo) {
    return Object.class.equals(parameterClass) ? null : ReflectionKit.getFieldList(parameterClass).stream().filter(e -> e.isAnnotationPresent(EncDecrypt.class)).map(field -> {
      field.setAccessible(true);
      return new EntityField(field, true, tableInfo.getFieldList().stream().filter(e -> field.getName().equals(e.getProperty())).map(TableFieldInfo::getColumn).findFirst().orElse(null));
    }).findFirst().orElseGet(() -> this.getVersionFieldRegular(parameterClass.getSuperclass(), tableInfo));
  }

  @Data
  private class EntityField {

    EntityField(Field field, boolean encDecrypt) {
      this.field = field;
      this.encDecrypt = encDecrypt;
    }

    public EntityField(Field field, boolean encDecrypt, String columnName) {
      this.field = field;
      this.encDecrypt = encDecrypt;
      this.columnName = columnName;
    }

    private Field field;
    private boolean encDecrypt;
    private String columnName;

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
}
