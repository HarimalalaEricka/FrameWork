package com.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AllowedRoles {
    String[] value();
    
    /**
     * Stratégie de vérification:
     * - ANY: au moins un rôle requis (par défaut)
     * - ALL: tous les rôles requis
     */
    Strategy strategy() default Strategy.ANY;
    
    /**
     * Message d'erreur personnalisé
     */
    String message() default "Accès interdit: rôles insuffisants";
    
    public enum Strategy {
        ANY, ALL
    }
}
