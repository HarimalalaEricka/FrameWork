package com.framework.core;

import com.framework.annotation.Controller;
import com.framework.annotation.HandleUrl;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ClassScanner {

    private String basePackage;
    private Map<Pattern, Method> urlMapping;           
    private Map<Pattern, Class<?>> controllerMapping;  

    public ClassScanner(String basePackage) {
        this.basePackage = basePackage;
        this.urlMapping = new HashMap<>();
        this.controllerMapping = new HashMap<>();
    }

    /**
     * Scanne le package et stocke toutes les routes URL → Method / Controller
     * Transforme les {param} en regex avec groupes nommés
     */
    public void scanControllers() {
        try {
            Reflections reflections = new Reflections(basePackage);

            for (Class<?> controllerClass : reflections.getTypesAnnotatedWith(Controller.class)) {
                for (Method method : controllerClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(HandleUrl.class)) {
                        HandleUrl annotation = method.getAnnotation(HandleUrl.class);
                        String url = annotation.value(); // ex: /user/{id}/edit

                        // Transforme {param} en groupe nommé regex : (?<param>[^/]+)
                        String regex = url.replaceAll("\\{([^/]+)\\}", "(?<$1>[^/]+)");
                        Pattern pattern = Pattern.compile(regex);

                        urlMapping.put(pattern, method);
                        controllerMapping.put(pattern, controllerClass);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<Pattern, Method> getUrlMapping() {
        return urlMapping;
    }

    public Map<Pattern, Class<?>> getControllerMapping() {
        return controllerMapping;
    }

    public void printRoutes() {
        System.out.println("=== Routes trouvées par Controller ===");
        for (Pattern pattern : urlMapping.keySet()) {
            Method m = urlMapping.get(pattern);
            Class<?> c = controllerMapping.get(pattern);
            System.out.println("Controller: " + c.getSimpleName() + " → " + pattern.pattern() + " → " + m.getName());
        }
        System.out.println("=== Fin des routes ===");
    }
}
