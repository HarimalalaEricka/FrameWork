package com.framework.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.framework.annotation.*;
import com.framework.core.*;

public class FrontServlet extends HttpServlet {

    private Map<String, Method> urlMapping = new HashMap<>();
    private Map<String, Class<?>> controllerMapping = new HashMap<>();
    private String packageController = "com.app.controller";

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

        // Rechercher si l’URL correspond à un controller
        if (!urlMapping.containsKey(url)) {
            response.setContentType("text/plain");
            response.getWriter().println("URL introuvable : " + url);
            return;
        }
        Method method = urlMapping.get(url);
        Class<?> controllerClass = controllerMapping.get(url);

        try {
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

            // Appeler la méthode du controller
            Object retour = method.invoke(controllerInstance);

            // Si retour = String → affichage DIRECT
            if (retour instanceof String) {
                response.setContentType("text/plain");
                PrintWriter out = response.getWriter();
                out.print((String) retour);
                return;
            }
            response.setContentType("text/plain");
            response.getWriter().println("Type de retour non supporté : " + retour.getClass());

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/plain");
            response.getWriter().println("Erreur framework : " + e.getMessage());
        }
    }
}
