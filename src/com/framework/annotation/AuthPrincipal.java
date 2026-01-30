package com.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour injecter le UserPrincipal dans les paramètres des méthodes
 * Usage: public ModelView maMethode(@AuthPrincipal UserPrincipal user)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AuthPrincipal {
    /**
     * Si true, la méthode échoue si le principal est null
     */
    boolean required() default true;
}