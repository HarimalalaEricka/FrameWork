package com.framework.core;

import com.framework.annotation.Controller;
import com.framework.annotation.HandleUrl;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ClassScanner {

    private String basePackage;
    private Map<String, Method> urlMapping;           // URL → Méthode
    private Map<String, Class<?>> controllerMapping;  // URL → Controller

    public ClassScanner(String basePackage) {
        this.basePackage = basePackage;
        this.urlMapping = new HashMap<>();
        this.controllerMapping = new HashMap<>();
    }

    /**
     * Scanne le package et stocke toutes les routes URL → Method / Controller
     */
    public void scanControllers() {
        try {
            Reflections reflections = new Reflections(basePackage);

            for (Class<?> controllerClass : reflections.getTypesAnnotatedWith(Controller.class)) {
                for (Method method : controllerClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(HandleUrl.class)) {
                        HandleUrl annotation = method.getAnnotation(HandleUrl.class);
                        String url = annotation.value();

                        urlMapping.put(url, method);
                        controllerMapping.put(url, controllerClass);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Map<String, Method> getUrlMapping() {
        return urlMapping;
    }

    public Map<String, Class<?>> getControllerMapping() {
        return controllerMapping;
    }

    public void printRoutes() {
        System.out.println("=== Routes trouvées par Controller ===");

        Map<Class<?>, Map<String, Method>> grouped = new HashMap<>();

        for (String url : urlMapping.keySet()) {
            Method m = urlMapping.get(url);
            Class<?> c = controllerMapping.get(url);

            grouped.putIfAbsent(c, new HashMap<>());
            grouped.get(c).put(url, m);
        }

        for (Class<?> controller : grouped.keySet()) {
            System.out.println("Controller : " + controller.getSimpleName());
            Map<String, Method> routes = grouped.get(controller);
            for (String url : routes.keySet()) {
                System.out.println("    " + url + " → " + routes.get(url).getName());
            }
            System.out.println();
        }

        System.out.println("=== Fin des routes ===");
    }

}
