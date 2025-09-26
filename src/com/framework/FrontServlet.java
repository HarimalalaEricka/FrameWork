package com.framework;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class FrontServlet extends HttpServlet {

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
