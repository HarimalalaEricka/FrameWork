package com.framework.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher; 

import com.framework.core.ClassScanner;
import com.framework.model.ModelView;
import com.framework.annotation.*;

public class FrontServlet extends HttpServlet {

    private Map<Pattern, Method> urlMapping = new HashMap<>();
    private Map<Pattern, Class<?>> controllerMapping = new HashMap<>();
    private String packageController = "com.app.controllers";
    private Map<Pattern, String> httpMethodMapping = new HashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            System.out.println("=== FrontServlet.init() : scan des controllers ===");
            ClassScanner scanner = new ClassScanner(packageController);
            scanner.scanControllers();
            urlMapping.putAll(scanner.getUrlMapping());
            controllerMapping.putAll(scanner.getControllerMapping());
            httpMethodMapping.putAll(scanner.getHttpMethodMapping());
            scanner.printRoutes();

            ServletContext context = getServletContext();
            context.setAttribute("controllerPackage", packageController);
            context.setAttribute("urlMapping", this.urlMapping);
            context.setAttribute("controllerMapping", this.controllerMapping);
            System.out.println("✅ Package contrôleur et routes enregistrés dans le ServletContext !");
            System.out.println("=== FrontServlet.init() terminé ===");

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur lors du scan des controllers", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String url = getRequestUrl(request);
        String requestMethod = request.getMethod();

        // 1️⃣ Gérer les fichiers statiques
        if (forwardStaticFileIfExists(url, request, response)) return;

        // 2️⃣ Trouver la méthode et le controller
        ControllerMatch match = findControllerMethod(url, requestMethod);
        if (match == null) {
            response.setContentType("text/plain");
            response.getWriter().println("URL introuvable : " + url);
            return;
        }

        // 3️⃣ Vérifier la méthode HTTP
        // String requiredMethod = httpMethodMapping.get(match.pattern);
        
        // if (requiredMethod != null) {
        //     if ("ANY".equals(requiredMethod)) {
        //         // @HandleUrl : accepte GET et POST
        //     } else if (!requiredMethod.equalsIgnoreCase(requestMethod)) {
        //         // Méthode HTTP incorrecte
        //         response.setContentType("text/plain");
        //         response.getWriter().println("Erreur 405 - Méthode non autorisée");
        //         response.getWriter().println("URL: " + url);
        //         response.getWriter().println("Méthode requise: " + requiredMethod);
        //         response.getWriter().println("Méthode reçue: " + requestMethod);
        //         return;
        //     }
        // }

        // 4 Appeler la méthode avec les paramètres dynamiques
        try {
            Object retour = invokeControllerMethod(match, request);
            handleReturnValue(retour, request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/plain");
            response.getWriter().println("Erreur framework : " + e.getMessage());
        }
    }

    // 🔹 Extraire l'URL de la requête
    private String getRequestUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return uri.substring(context.length());
    }

    // 🔹 Vérifier et forwarder un fichier statique si trouvé
    private boolean forwardStaticFileIfExists(String url, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String realPath = getServletContext().getRealPath(url);
        File fichier = new File(realPath);
        if (fichier.exists() && fichier.isFile()) {
            RequestDispatcher dispatcher = request.getRequestDispatcher(url);
            dispatcher.forward(request, response);
            return true;
        }
        return false;
    }

    // 🔹 Chercher le controller et la méthode correspondante à l'URL
    private ControllerMatch findControllerMethod(String url, String requestMethod) {
        List<ControllerMatch> allMatches = new ArrayList<>();
        
        // Étape 1: Trouver TOUTES les routes qui correspondent à l'URL
        for (Pattern pattern : urlMapping.keySet()) {
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches()) {
                // Extraire les paramètres des groupes nommés
                Map<String, String> pathParams = extractNamedGroups(matcher);
                
                ControllerMatch match = new ControllerMatch(
                    urlMapping.get(pattern), 
                    controllerMapping.get(pattern),
                    pathParams,
                    pattern
                );
                allMatches.add(match);
            }
        }
        
        if (allMatches.isEmpty()) {
            return null;
        }
        
        // Étape 2: Si une seule correspondance, la retourner
        if (allMatches.size() == 1) {
            return allMatches.get(0);
        }
        
        // Étape 3: Si plusieurs, filtrer par méthode HTTP
        List<ControllerMatch> methodMatches = new ArrayList<>();
        for (ControllerMatch match : allMatches) {
            String requiredMethod = httpMethodMapping.get(match.pattern);
            
            // Vérifier si la méthode correspond
            if ("ANY".equals(requiredMethod) || 
                (requiredMethod != null && requiredMethod.equalsIgnoreCase(requestMethod))) {
                methodMatches.add(match);
            }
        }
        
        // Étape 4: Gérer les résultats filtrés
        if (methodMatches.isEmpty()) {
            // Aucune méthode ne correspond à la méthode HTTP
            return null;
        } else if (methodMatches.size() == 1) {
            return methodMatches.get(0);
        } else {
            // Plusieurs méthodes correspondent, prioriser les spécifiques sur "ANY"
            for (ControllerMatch match : methodMatches) {
                String method = httpMethodMapping.get(match.pattern);
                if (!"ANY".equals(method)) {
                    return match; // Retourner la première méthode spécifique
                }
            }
            // Sinon retourner le premier "ANY"
            return methodMatches.get(0);
        }
    }

    // 🔹 Appeler la méthode du controller avec les paramètres dynamiques (SPRINT 6 & 6 BIS)
    private Object invokeControllerMethod(ControllerMatch match, HttpServletRequest request) throws Exception {
        Object controllerInstance = match.controller.getDeclaredConstructor().newInstance();
        Parameter[] params = match.method.getParameters();
        Object[] args = new Object[params.length];

        // Combiner TOUTES les sources de paramètres
        Map<String, String> allParamSources = new HashMap<>();

        // 1. Paramètres du chemin (/{id}/) - SPRINT 6 TER
        allParamSources.putAll(match.pathParams);

        // 2. Paramètres GET/POST (?name=value) - SPRINT 6
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            allParamSources.put(name, request.getParameter(name));
        }
        
        // DEBUG
        System.out.println("=== DEBUG COMBINÉ ===");
        System.out.println("Path params: " + match.pathParams);
        System.out.println("All sources: " + allParamSources);
        
        // Traiter chaque paramètre
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            String paramValue = null;
            String searchSource = "";
            
            System.out.println("\nParamètre " + i + ": " + param.getName() + 
                            " (type: " + param.getType().getSimpleName() + ")");
            
            // SPRINT 6 BIS : Priorité 1 - @RequestParam
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                String paramName = requestParam.value();
                paramValue = allParamSources.get(paramName);
                searchSource = "@RequestParam(\"" + paramName + "\")";
                System.out.println("  -> Recherche via " + searchSource + ": " + paramValue);
            }
            
            // SPRINT 6 : Priorité 2 - Nom de l'argument
            if (paramValue == null) {
                String paramName = param.getName();
                paramValue = allParamSources.get(paramName);
                searchSource = "nom d'argument \"" + paramName + "\"";
                System.out.println("  -> Recherche via " + searchSource + ": " + paramValue);
            }
            
            // Conversion
            if (paramValue != null) {
                args[i] = convertParameter(paramValue, param.getType());
                System.out.println("  -> ✓ Converti: " + args[i] + " (" + searchSource + ")");
            } else {
                args[i] = getDefaultValue(param.getType());
                System.out.println("  -> ✗ Non trouvé, valeur par défaut: " + args[i]);
            }
        }
        
        System.out.println("=== FIN DEBUG ===\n");
        
        return match.method.invoke(controllerInstance, args);
    }

    // 🔹 Convertir un paramètre de String vers le type attendu
    private Object convertParameter(String value, Class<?> type) {
        if (value == null || value.trim().isEmpty()) {
            return getDefaultValue(type);
        }
        
        try {
            if (type == String.class) return value;
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
            if (type == float.class || type == Float.class) return Float.parseFloat(value);
            if (type == long.class || type == Long.class) return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return getDefaultValue(type);
        }
        return null;
    }

    // 🔹 Nouvelle méthode : obtenir la valeur par défaut selon le type
    private Object getDefaultValue(Class<?> type) {
        if (type == String.class) return null;
        if (type == int.class) return 0;
        if (type == Integer.class) return null;
        if (type == double.class) return 0.0;
        if (type == Double.class) return null;
        if (type == boolean.class) return false;
        if (type == Boolean.class) return null;
        if (type == float.class) return 0.0f;
        if (type == Float.class) return null;
        if (type == long.class) return 0L;
        if (type == Long.class) return null;
        return null;
    }

    // 🔹 Gérer le type de retour d'une méthode
    private void handleReturnValue(Object retour, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (retour instanceof String) {
            response.setContentType("text/plain");
            response.getWriter().print((String) retour);
        } else if (retour instanceof ModelView mv) {
            for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
            RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getView());
            dispatcher.forward(request, response);
        } else {
            response.setContentType("text/plain");
            response.getWriter().println("Type de retour non supporté : " + retour.getClass());
        }
    }

    private Map<String, String> extractNamedGroups(Matcher matcher) {
        Map<String, String> params = new HashMap<>();
        
        // Votre pattern est (?<id>[^/]+) donc on peut extraire par nom
        try {
            // Les groupes nommés sont stockés dans la Matcher
            // On doit les extraire manuellement car Java n'a pas de méthode directe
            // On peut utiliser reflection ou analyser le pattern
            
            // Solution simple : extraire tous les groupes
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String groupName = getGroupName(matcher.pattern(), i);
                if (groupName != null) {
                    params.put(groupName, matcher.group(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return params;
    }

    // Méthode utilitaire pour obtenir le nom d'un groupe
    private String getGroupName(Pattern pattern, int groupIndex) {
        String patternStr = pattern.pattern();
        // Chercher les groupes nommés dans le pattern
        java.util.regex.Matcher groupMatcher = java.util.regex.Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(patternStr);
        
        int currentGroup = 1;
        while (groupMatcher.find()) {
            if (currentGroup == groupIndex) {
                return groupMatcher.group(1);
            }
            currentGroup++;
        }
        return null;
    }

    // 🔹 Classe interne pour retourner méthode + controller
    private static class ControllerMatch {
        Method method;
        Class<?> controller;
        Map<String, String> pathParams; // Paramètres extraits de l'URL
        Pattern pattern;
        
        ControllerMatch(Method m, Class<?> c, Map<String, String> params, Pattern p) {
            this.method = m;
            this.controller = c;
            this.pathParams = params;
            this.pattern = p;
        }
    }
}