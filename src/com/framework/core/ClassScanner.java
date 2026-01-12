package com.framework.core;

import com.framework.annotation.Controller;
import com.framework.annotation.*;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ClassScanner {

    private String basePackage;
    private Map<Pattern, Method> urlMapping;           
    private Map<Pattern, Class<?>> controllerMapping;  
    private Map<Pattern, String> httpMethodMapping;

    public ClassScanner(String basePackage) {
        this.basePackage = basePackage;
        this.urlMapping = new HashMap<>();
        this.controllerMapping = new HashMap<>();
        this.httpMethodMapping = new HashMap<>();
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
                    String url = null;
                    String httpMethod = null;
                    
                    // 1. Vérifier @HandleGet
                    HandleGet getUrl = method.getAnnotation(HandleGet.class);
                    if (getUrl != null) {
                        url = getUrl.value();
                        httpMethod = "GET";
                    }
                    
                    // 2. Vérifier @HandlePost
                    HandlePost postUrl = method.getAnnotation(HandlePost.class);
                    if (postUrl != null) {
                        if (url != null) {
                            throw new RuntimeException("Une méthode ne peut avoir qu'une annotation @HandleGet ou @HandlePost: " + method.getName());
                        }
                        url = postUrl.value();
                        httpMethod = "POST";
                    }
                    
                    // 3. Vérifier l'ancienne annotation @HandleUrl (pour compatibilité)
                    HandleUrl handleUrl = method.getAnnotation(HandleUrl.class);
                    if (handleUrl != null) {
                        if (url != null) {
                            throw new RuntimeException("Une méthode ne peut avoir qu'une annotation @HandleUrl, @HandleGet ou @HandlePost: " + method.getName());
                        }
                        url = handleUrl.value();
                        httpMethod = "ANY"; // Méthode universelle
                    }
                    
                    // Si une URL a été trouvée, enregistrer la route
                    if (url != null) {
                        // Transforme {param} en groupe nommé regex
                        String regex = url.replaceAll("\\{([^/]+)\\}", "(?<$1>[^/]+)");
                        Pattern pattern = Pattern.compile("^" + regex + "$");
                        
                        // Stocker avec la méthode HTTP
                        urlMapping.put(pattern, method);
                        controllerMapping.put(pattern, controllerClass);
                        
                        // ⭐ STOCKER LA MÉTHODE HTTP DANS UNE MAP SÉPARÉE ⭐
                        httpMethodMapping.put(pattern, httpMethod);
                        
                        System.out.println("Route enregistrée: " + httpMethod + " " + pattern.pattern() + " → " + method.getName());
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

    public Map<Pattern, String> getHttpMethodMapping() {
        return httpMethodMapping;
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
