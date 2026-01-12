package com.framework.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher; 
import java.lang.reflect.ParameterizedType;

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
    // 🔹 Appeler la méthode du controller avec les paramètres dynamiques (SPRINT 6, 6 BIS, 8)
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
        System.out.println("=== DEBUG SPRINT 8 ===");
        System.out.println("Path params: " + match.pathParams);
        System.out.println("All sources: " + allParamSources);
        
        // Traiter chaque paramètre
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Class<?> paramType = param.getType();
            
            System.out.println("\nParamètre " + i + ": " + param.getName() + 
                            " (type: " + paramType.getSimpleName() + ")");

            // SPRINT 8: Support de Map<String, Object> - DOIT ÊTRE EN PREMIER !
            if (Map.class.isAssignableFrom(paramType) || paramType.getName().equals("Map")) {
                System.out.println("  -> ⚡ DÉTECTION SPRINT 8: C'est une Map!");
                
                // Créer une Map<String, Object> avec TOUS les paramètres
                Map<String, Object> paramMap = new HashMap<>(allParamSources);
                args[i] = paramMap;
                
                System.out.println("  -> ✓ Map<String, Object> injectée avec " + paramMap.size() + " éléments");
                System.out.println("  -> Contenu: " + paramMap);
                
                continue; 
            }
            
            // SPRINT 8BIS: Data Binding - Objet Personnalisé
            // Vérifier si c'est une classe personnalisée (pas primitive, pas String, pas Map)
            if (!isSimpleType(paramType) && !paramType.isArray() && !paramType.isInterface()) {
                System.out.println("  -> 🎯 DÉTECTION SPRINT 8BIS: Classe personnalisée détectée!");
                
                try {
                    // Créer une instance de l'objet
                    Object obj = paramType.getDeclaredConstructor().newInstance();
                    System.out.println("  -> Instance créée: " + obj.getClass().getName());
                    
                    // Remplir l'objet avec les paramètres
                    int filledFields = fillObjectFromParams(obj, allParamSources);
                    
                    args[i] = obj;
                    System.out.println("  -> ✓ Objet injecté avec " + filledFields + " champs remplis");
                    continue;
                    
                } catch (Exception e) {
                    System.out.println("  -> ✗ Erreur lors de la création de l'objet: " + e.getMessage());
                    args[i] = null;
                    continue;
                }
            }
            // si un tableau User[]
            if (paramType.isArray()) {
                System.out.println("  -> 🎯 DÉTECTION: Tableau détecté!");
                System.out.println("  -> Type du tableau: " + paramType.getComponentType().getSimpleName());
                
                Class<?> componentType = paramType.getComponentType();
                
                // Si c'est un tableau de types simples (String[], int[], etc.)
                if (isSimpleType(componentType)) {
                    System.out.println("  -> Tableau de types simples");
                    args[i] = handleSimpleArray(componentType, allParamSources, param.getName());
                }
                // Si c'est un tableau d'objets (Employee[], User[], etc.)
                else if (!componentType.isInterface() && !Map.class.isAssignableFrom(componentType)) {
                    System.out.println("  -> Tableau d'objets complexes");
                    args[i] = handleObjectArray(componentType, allParamSources);
                } else {
                    args[i] = null;
                }
                
                if (args[i] != null) {
                    System.out.println("  -> ✓ Tableau injecté");
                } else {
                    System.out.println("  -> ✗ Tableau non créé");
                }
                continue;
            }
            
            // SPRINT 6 BIS : Priorité 1 - @RequestParam
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                String paramName = requestParam.value();
                String paramValue = allParamSources.get(paramName);
                System.out.println("  -> Recherche via @RequestParam(\"" + paramName + "\"): " + paramValue);
                
                if (paramValue != null) {
                    args[i] = convertParameter(paramValue, paramType);
                    System.out.println("  -> ✓ Converti: " + args[i] + " (via @RequestParam)");
                } else {
                    args[i] = getDefaultValue(paramType);
                    System.out.println("  -> ✗ Non trouvé, valeur par défaut: " + args[i]);
                }
                continue;
            }
            
            // SPRINT 6 : Priorité 2 - Nom de l'argument
            String paramName = param.getName();
            String paramValue = allParamSources.get(paramName);
            System.out.println("  -> Recherche via nom d'argument \"" + paramName + "\": " + paramValue);
            
            if (paramValue != null) {
                args[i] = convertParameter(paramValue, paramType);
                System.out.println("  -> ✓ Converti: " + args[i] + " (via nom d'argument)");
            } else {
                args[i] = getDefaultValue(paramType);
                System.out.println("  -> ✗ Non trouvé, valeur par défaut: " + args[i]);
            }
        }
        
        System.out.println("=== FIN DEBUG SPRINT 8 ===\n");
        
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
            
            if (type == boolean.class || type == Boolean.class) {
                if ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || 
                    "yes".equalsIgnoreCase(value) || "checked".equalsIgnoreCase(value) ||
                    "1".equals(value)) {
                    return true;
                }
                return Boolean.parseBoolean(value);
            }
            
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
    // Vérifie si c'est un type simple (primitif, String, wrapper, etc.)
    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || 
            type == String.class || 
            type == Integer.class || type == int.class ||
            type == Double.class || type == double.class ||
            type == Boolean.class || type == boolean.class ||
            type == Float.class || type == float.class ||
            type == Long.class || type == long.class ||
            type == Short.class || type == short.class ||
            type == Byte.class || type == byte.class ||
            type == Character.class || type == char.class ||
            type.isEnum();
    }

    // Remplit un objet avec les paramètres de la requête
    private int fillObjectFromParams(Object obj, Map<String, String> params) throws Exception {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        int filledCount = 0;
        
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            String paramValue = params.get(fieldName);
            
            if (paramValue != null && !paramValue.trim().isEmpty()) {
                try {
                    Object convertedValue = convertParameter(paramValue, field.getType());
                    field.set(obj, convertedValue);
                    filledCount++;
                } catch (Exception e) {
                    System.out.println("Erreur set field " + fieldName + ": " + e.getMessage());
                }
            }
        }
        
        return filledCount;
    }

    // Gère les tableaux de types simples (String[], int[], etc.)
    private Object handleSimpleArray(Class<?> componentType, Map<String, String> params, String paramName) {
        try {
            // Pour les tableaux simples, on s'attend à des paramètres comme:
            // names[0]=Jean, names[1]=Marie OU names=Jean,Marie
            List<String> values = new ArrayList<>();
            
            // 1. Chercher les paramètres indexés: names[0], names[1], etc.
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith(paramName + "[")) {
                    values.add(entry.getValue());
                }
            }
            
            // 2. Si pas trouvé, chercher un paramètre simple avec valeurs séparées par virgule
            if (values.isEmpty()) {
                String simpleValue = params.get(paramName);
                if (simpleValue != null && !simpleValue.isEmpty()) {
                    String[] parts = simpleValue.split(",");
                    values = Arrays.asList(parts);
                }
            }
            
            // 3. Si toujours vide, chercher avec suffixe: names0, names1
            if (values.isEmpty()) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (entry.getKey().matches(paramName + "\\d+")) {
                        values.add(entry.getValue());
                    }
                }
            }
            
            // Créer le tableau
            if (!values.isEmpty()) {
                Object array = Array.newInstance(componentType, values.size());
                for (int j = 0; j < values.size(); j++) {
                    Object converted = convertParameter(values.get(j), componentType);
                    Array.set(array, j, converted);
                }
                return array;
            }
            
        } catch (Exception e) {
            System.out.println("Erreur création tableau simple: " + e.getMessage());
        }
        return null;
    }

    // Gère les tableaux d'objets (Employee[], User[], etc.)
    private Object handleObjectArray(Class<?> componentType, Map<String, String> params) {
        try {
            // Déterminer combien d'objets il y a en cherchant les préfixes
            // Exemple: employees[0].name, employees[0].age, employees[1].name, employees[1].age
            
            Map<Integer, Map<String, String>> objectsData = new HashMap<>();
            
            // Regrouper les données par index
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                
                // Pattern: nom[index].champ
                if (key.matches(".*\\[\\d+\\]\\..*")) {
                    // Extraire l'index
                    int start = key.indexOf('[');
                    int end = key.indexOf(']');
                    if (start != -1 && end != -1) {
                        String indexStr = key.substring(start + 1, end);
                        int index = Integer.parseInt(indexStr);
                        
                        // Extraire le nom du champ
                        String fieldName = key.substring(end + 2); // après "]. "
                        
                        // Stocker
                        objectsData.putIfAbsent(index, new HashMap<>());
                        objectsData.get(index).put(fieldName, entry.getValue());
                    }
                }
                // Pattern alternatif: nomIndexChamp (employees0name)
                else if (key.matches(".*\\d+.*")) {
                    // Logique plus complexe pour ce pattern
                }
            }
            
            // Si on a trouvé des données indexées
            if (!objectsData.isEmpty()) {
                int maxIndex = Collections.max(objectsData.keySet()) + 1;
                Object array = Array.newInstance(componentType, maxIndex);
                
                for (int index = 0; index < maxIndex; index++) {
                    if (objectsData.containsKey(index)) {
                        // Créer l'objet
                        Object obj = componentType.getDeclaredConstructor().newInstance();
                        fillObjectFromParams(obj, objectsData.get(index));
                        Array.set(array, index, obj);
                    }
                }
                
                return array;
            }
            
        } catch (Exception e) {
            System.out.println("Erreur création tableau objets: " + e.getMessage());
            e.printStackTrace();
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