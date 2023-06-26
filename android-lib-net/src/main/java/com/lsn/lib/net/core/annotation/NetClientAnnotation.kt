package com.lsn.lib.net.core.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class NetClientClazz()

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(RetentionPolicy.RUNTIME)
annotation class NetBaseUrlFunc(val baseUrl: String = "")


@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(RetentionPolicy.RUNTIME)
annotation class NetResponseFunc(val isStandard: Boolean = false)



