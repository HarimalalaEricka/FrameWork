package com.framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {
    String value();                 // nom du paramètre
    boolean required() default true; // obligatoire ou non
    String defaultValue() default ""; // valeur par défaut
}
