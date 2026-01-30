package com.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nécessite que l'utilisateur ait au moins une des autorisations spécifiées
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AllowedAuthorities {
    String[] value();
    
    Strategy strategy() default Strategy.ANY;
    
    String message() default "Accès interdit: permissions insuffisantes";
    
    public enum Strategy {
        ANY, ALL
    }
}
