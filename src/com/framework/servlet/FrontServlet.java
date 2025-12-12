package com.framework.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

import com.framework.core.ClassScanner;
import com.framework.model.ModelView;

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

        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String url = uri.substring(context.length());

        // Vérifier si la ressource existe physiquement
        String realPath = getServletContext().getRealPath(url);
        File fichier = new File(realPath);
        if (fichier.exists() && fichier.isFile()) {
            RequestDispatcher dispatcher = request.getRequestDispatcher(url);
            dispatcher.forward(request, response);
            return;
        }

        // Rechercher le controller correspondant avec Pattern
        Method methodToCall = null;
        Class<?> controllerClass = null;
        for (Pattern pattern : urlMapping.keySet()) {
            if (pattern.matcher(url).matches()) {
                methodToCall = urlMapping.get(pattern);
                controllerClass = controllerMapping.get(pattern);
                break;
            }
        }

        if (methodToCall == null) {
            response.setContentType("text/plain");
            response.getWriter().println("URL introuvable : " + url);
            return;
        }

        try {
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

            // Tous les paramètres restent null pour l'instant
            Parameter[] params = methodToCall.getParameters();
            Object[] args = new Object[params.length];
            Arrays.fill(args, null);

            Object retour = methodToCall.invoke(controllerInstance, args);

            // Gérer le type de retour
            if (retour instanceof String) {
                response.setContentType("text/plain");
                response.getWriter().print((String) retour);
            } else if (retour instanceof ModelView) {
                ModelView model = (ModelView) retour;
                String view = model.getView();
                for (Map.Entry<String, Object> entry : model.getModel().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
                RequestDispatcher dispatcher = request.getRequestDispatcher(view);
                dispatcher.forward(request, response);
            } else {
                response.setContentType("text/plain");
                response.getWriter().println("Type de retour non supporté : " + retour.getClass());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/plain");
            response.getWriter().println("Erreur framework : " + e.getMessage());
        }
    }
}
