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

        // Récupérer le chemin demandé
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        // Construire le chemin réel sur le serveur
        String realPath = getServletContext().getRealPath(path);
        File fichier = new File(realPath);

        if (fichier.exists() && fichier.isFile()) {
            // Si la ressource existe (ex: JSP), on y fait un forward
            RequestDispatcher dispatcher = request.getRequestDispatcher(path);
            dispatcher.forward(request, response);
        } else {
            // Sinon on affiche un message
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("Votre URL demandée est : " + path);
        }
    }
}
