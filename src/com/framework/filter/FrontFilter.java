package com.framework.filter;

import javax.servlet.*;
import javax.servlet.http.*;
import org.reflections.Reflections;
import java.lang.reflect.*;
import java.io.IOException;
import java.io.PrintWriter;
import com.framework.annotation.*;
import java.util.*;

public class FrontFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("=== Initialisation du FrontFilter ===");

        ServletContext context = filterConfig.getServletContext();
        String basePackage = "com.app.controllers";
        Set<String> availableUrls = new HashSet<>();

        try {
            // Scanner les classes avec @Controller
            Reflections reflections = new Reflections(basePackage);
            Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);

            for (Class<?> controllerClass : controllers) {
                for (Method method : controllerClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(HandleUrl.class)) {
                        HandleUrl annotation = method.getAnnotation(HandleUrl.class);
                        String url = annotation.value();
                        availableUrls.add(url);
                        System.out.println("→ URL trouvée : " + url);
                    }
                }
            }

            // Stocke les URLs dans le contexte
            context.setAttribute("availableUrls", availableUrls);
            System.out.println("=== FrontFilter prêt (" + availableUrls.size() + " URLs enregistrées) ===");

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur pendant l'initialisation du FrontFilter", e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String servletPath = request.getServletPath();
        ServletContext servletContext = request.getServletContext();

        // Récupère la liste des URLs enregistrées
        Set<String> availableUrls = (Set<String>) servletContext.getAttribute("availableUrls");

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            if (availableUrls != null && availableUrls.contains(servletPath)) {
                out.println("<html><body style='font-family:sans-serif;'>");
                out.println("<h2 style='color:green;'>✅ URL trouvée :</h2>");
                out.println("<p>" + servletPath + " correspond à une route connue.</p>");
                out.println("</body></html>");
            } else {
                out.println("<html><body style='font-family:sans-serif;'>");
                out.println("<h2 style='color:red;'>❌ URL non trouvée :</h2>");
                out.println("<p>" + servletPath + " ne correspond à aucune route détectée.</p>");
                out.println("</body></html>");
            }
        }
    }

    @Override
    public void destroy() {
        System.out.println("FrontFilter détruit");
    }
}
