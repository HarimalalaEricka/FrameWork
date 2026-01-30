package com.framework.auth;

import java.io.*;
import java.util.Properties;

public class AuthConfigLoader {
    private static AuthConfig instance;
    
    public static AuthConfig load() {
        if (instance != null) {
            return instance;
        }
        
        instance = new AuthConfig();
        Properties props = new Properties();
        
        System.out.println("=== 🔍 CHARGEMENT CONFIG AUTH ===");
        
        // ESSAIE PLUSIEURS EMPLACEMENTS
        boolean fileLoaded = false;
        
        // 1. Essaie depuis le classpath (après compilation)
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("auth.properties")) {
            
            if (input != null) {
                System.out.println("✅ Fichier trouvé dans classpath");
                props.load(input);
                fileLoaded = true;
            }
        } catch (IOException e) {
            System.err.println("⚠️ Erreur classpath: " + e.getMessage());
        }
        
        // 2. Si pas trouvé, essaie depuis le système de fichiers (dev)
        if (!fileLoaded) {
            System.out.println("🔍 Recherche dans le système de fichiers...");
            
            // Liste des chemins possibles
            String[] possiblePaths = {
                "auth.properties",                    // À côté de src/
                "../auth.properties",                 // Un niveau au-dessus
                "src/../auth.properties",             // Depuis src/
                System.getProperty("user.dir") + "/auth.properties" // Répertoire courant
            };
            
            for (String path : possiblePaths) {
                File file = new File(path);
                System.out.println("  Testing: " + file.getAbsolutePath() + 
                                 " (exists: " + file.exists() + ")");
                
                if (file.exists()) {
                    try (InputStream input = new FileInputStream(file)) {
                        System.out.println("✅ Fichier trouvé: " + file.getAbsolutePath());
                        props.load(input);
                        fileLoaded = true;
                        break;
                    } catch (IOException e) {
                        System.err.println("Erreur lecture " + path + ": " + e.getMessage());
                    }
                }
            }
        }
        
        if (fileLoaded) {
            System.out.println("📋 Configuration chargée depuis auth.properties");
            
            // CHANGES CRITIQUES ICI :
            instance.setPrincipalClass(props.getProperty("auth.principal.class", ""));
            instance.setSessionKey(props.getProperty("auth.session.key", "APP_USER_PRINCIPAL"));
            instance.setAnonymousEnabled(Boolean.parseBoolean(
                props.getProperty("auth.anonymous.enabled", "true")));
            // ⭐ CHANGE ICI : /auth/login au lieu de /login ⭐
            instance.setDefaultRedirect(props.getProperty("auth.default.redirect", "/auth/login"));
            instance.setErrorPage(props.getProperty("auth.error.page", "/views/error/403.jsp"));
            // ⭐ CHANGE ICI AUSSI ⭐
            instance.setLoginPage(props.getProperty("auth.login.page", "/auth/login.jsp"));
            
            System.out.println("🔧 Redirect configuré: " + instance.getDefaultRedirect());
        } else {
            System.out.println("⚠️ Aucun fichier auth.properties trouvé");
            System.out.println("💡 Créez 'auth.properties' à côté du dossier 'src/'");
            
            // Valeurs par défaut CORRIGÉES
            instance.setDefaultRedirect("/auth/login");  // ⭐ IMPORTANT ⭐
            instance.setLoginPage("/auth/login.jsp");    // ⭐ IMPORTANT ⭐
            instance.setSessionKey("APP_USER_PRINCIPAL");
            instance.setAnonymousEnabled(true);
            
            System.out.println("🔧 Utilisation valeurs par défaut - redirect: /auth/login");
        }
        
        return instance;
    }
}