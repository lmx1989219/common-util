package com.lmx.common.clog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作声明
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OpLog {
    /**
     * 操作id
     *
     * @return
     */
    String opId() default "";

    /**
     * 操作名称
     *
     * @return
     */
    String opName() default "";

    /**
     * 操作类别
     *
     * @return
     */
    String operationType() default "";

    /**
     * 操作名称
     *
     * @return
     */
    String operationName() default "";

    /**
     * 操作描述
     *
     * @return
     */
    String description() default "";
}
