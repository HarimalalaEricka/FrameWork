package com.framework.filter;


import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class FrontFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Optionnel : tu peux initialiser des ressources ici
        System.out.println("FrontFilter initialisé");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String servletPath = request.getServletPath();
        ServletContext servletContext = request.getServletContext();

        // Vérifie si la ressource demandée existe
        if (servletContext.getResource(servletPath) != null) {
            // La ressource existe → on laisse passer la requête
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // La ressource n'existe pas → on affiche le chemin (ou redirige)
            response.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter printWriter = response.getWriter()) {
                printWriter.print("Ressource non trouvée : " + servletPath);
            }
        }
    }

    @Override
    public void destroy() {
        // Optionnel : libération des ressources si nécessaire
        System.out.println("FrontFilter détruit");
    }
}
