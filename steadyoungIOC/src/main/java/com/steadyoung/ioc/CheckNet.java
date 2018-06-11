package com.steadyoung.ioc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件点击网络监测
 * @author wayne
 * @date 2018/6/7
 */

/*
 * ElementType.FIELD 代表annotation的位置
 * FIELD：属性注解
 * CONSTRUCTOR：构造器注解
 * METHOD：方法注解
 * TYPE：类上注解
 */
@Target(ElementType.METHOD)
/*
@Retention(RetentionPolicy.CLASS)什么时候生效
CLASS 编译时
RUNTIME 运行时
SOURCR 源码资源
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckNet {
    String value();
}
