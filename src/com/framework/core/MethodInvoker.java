package com.framework.core;

import java.lang.reflect.Method;

public class MethodInvoker {

    private Class<?> controllerClass;
    private Method method;

    public MethodInvoker(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }
}
