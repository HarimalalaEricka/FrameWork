# FrameWork S5
Developpement d'un framework

# Résumé des fonctionnalités par sprint (d'après le code)

## Sprint 3
- Détection des URLs dynamiques avec paramètres (ex: `/user/{id}`)
- Méthodes de contrôleur peuvent recevoir des arguments correspondant aux variables d'URL

## Sprint 5
- Utilisation de la classe `ModelView` pour transmettre des données à la vue (attributs pour la requête)

## Sprint 6
- Injection automatique des paramètres GET/POST dans les arguments des méthodes de contrôleur (par nom)
- Si un argument n'est pas trouvé dans la requête, il reste à null

## Sprint 6 bis
- Support de l'annotation `@RequestParam` pour lier explicitement un argument à un paramètre de la requête

## Sprint 6 ter
- Extraction automatique des variables dynamiques de l'URL et injection dans les arguments correspondants
- Priorité : URL > request.getParameter > null/exception

## Sprint 7
- Gestion des annotations `@HandleGet` et `@HandlePost` (équivalent à `@GetMapping` et `@PostMapping`)

## Sprint 8
- Support de l'injection d'un `Map<String, Object>` dans les méthodes de contrôleur pour récupérer tous les paramètres de la requête (similaire à Spring Boot)

## Sprint 8 bis
- Data binding automatique vers des objets personnalisés (ex: Employee, User)

## Sprint 9
- Ajout d'API REST (ex: `/api/users`, `/api/employee`)
- Gestion de réponses JSON

## Sprint 10
- Gestion de l'upload de fichiers via des objets `UploadedFile`

## Sprint 11
- Gestion avancée de la session utilisateur avec `@SessionParam`, `@SessionAttributes`, et injection de `HttpSession`

