package com.framework.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

import com.framework.core.ClassScanner;
import com.framework.model.ModelView;
import com.framework.annotation.RequestParam;

public class FrontServlet extends HttpServlet {

    private Map<Pattern, Method> urlMapping = new HashMap<>();
    private Map<Pattern, Class<?>> controllerMapping = new HashMap<>();
    private String packageController = "com.app.controllers";

    @Override
    public void init() throws ServletException {
        try {
            System.out.println("=== FrontServlet.init() : scan des controllers ===");
            ClassScanner scanner = new ClassScanner(packageController);
            scanner.scanControllers();
            urlMapping.putAll(scanner.getUrlMapping());
            controllerMapping.putAll(scanner.getControllerMapping());
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

        // 1️⃣ Gérer les fichiers statiques
        if (forwardStaticFileIfExists(url, request, response)) return;

        // 2️⃣ Trouver la méthode et le controller
        MethodControllerPair pair = findControllerMethod(url);
        if (pair == null) {
            response.setContentType("text/plain");
            response.getWriter().println("URL introuvable : " + url);
            return;
        }

        // 3️⃣ Appeler la méthode avec les paramètres dynamiques
        try {
            Object retour = invokeControllerMethod(pair, request);
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
    private MethodControllerPair findControllerMethod(String url) {
        for (Pattern pattern : urlMapping.keySet()) {
            if (pattern.matcher(url).matches()) {
                return new MethodControllerPair(urlMapping.get(pattern), controllerMapping.get(pattern));
            }
        }
        return null;
    }

    // 🔹 Appeler la méthode du controller avec les paramètres dynamiques (SPRINT 6 & 6 BIS)
    private Object invokeControllerMethod(MethodControllerPair pair, HttpServletRequest request) throws Exception {
        Object controllerInstance = pair.controller.getDeclaredConstructor().newInstance();
        Parameter[] params = pair.method.getParameters();
        Object[] args = new Object[params.length];

        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            System.out.println("Paramètre disponible: " + name + " = " + request.getParameter(name));
        }
        
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            String paramValue = null;
            
            System.out.println("\nTraitement paramètre " + i + ": " + param.getName() + " (type: " + param.getType() + ")");
            paramValue = request.getParameter(param.getName());
            System.out.println("  -> Valeur trouvée par nom: " + paramValue);
            
            // Conversion de la valeur
            if (paramValue != null) {
                args[i] = convertParameter(paramValue, param.getType());
                System.out.println("  -> Valeur convertie: " + args[i]);
            } else {
                args[i] = getDefaultValue(param.getType());
                System.out.println("  -> Valeur par défaut: " + args[i]);
            }
        }
        
        
        return pair.method.invoke(controllerInstance, args);
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

    // 🔹 Classe interne pour retourner méthode + controller
    private static class MethodControllerPair {
        Method method;
        Class<?> controller;
        MethodControllerPair(Method m, Class<?> c) {
            this.method = m;
            this.controller = c;
        }
    }
}