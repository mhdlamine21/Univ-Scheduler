package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.AuthentificationService;
import scheduler.service.EmailService;
import scheduler.service.NotificationService;
import scheduler.service.UtilisateurService;
import scheduler.util.GenerateurMotDePasse;
import scheduler.util.SessionUtilisateur;
import scheduler.util.ValidationDonnees;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class LoginControleur {

    private static final Logger logger = LoggerFactory.getLogger(LoginControleur.class);
    private static final int MAX_TENTATIVES = 3;

    @FXML private TextField         emailField;
    @FXML private PasswordField     passwordField;
    @FXML private Button            loginButton;
    @FXML private Hyperlink         inscriptionLink;
    @FXML private Hyperlink         motDePasseOublieLink;
    @FXML private Label             messageLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private AuthentificationService authService;
    private NotificationService     notificationService;
    private UtilisateurService      utilisateurService;
    private EmailService            emailService;
    private Stage   primaryStage;
    private int     tentativesEchouees = 0;
    private boolean isProcessing = false;

    public LoginControleur() {
        this.authService         = new AuthentificationService();
        this.notificationService = new NotificationService();
        this.utilisateurService  = new UtilisateurService();
        this.emailService        = new EmailService();
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        if (stage != null) {
            stage.setResizable(true);
            stage.setMinWidth(480);
            stage.setMinHeight(560);
        }
    }

    @FXML
    private void initialize() {
        if (messageLabel != null) {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
        configurerEvenements();
    }

    private void configurerEvenements() {
        if (loginButton        != null) loginButton.setOnAction(e -> handleLogin());
        if (inscriptionLink    != null) inscriptionLink.setOnAction(e -> handleCreerCompte());
        if (motDePasseOublieLink != null) motDePasseOublieLink.setOnAction(e -> handleMotDePasseOublie());
        if (passwordField      != null) passwordField.setOnAction(e -> handleLogin());
        if (emailField         != null) emailField.setOnAction(e -> {
            if (passwordField != null) passwordField.requestFocus();
        });
    }

    /**
     * Gère la tentative de connexion.
     */
    private void handleLogin() {
        if (isProcessing) return;
        String email = emailField != null ? emailField.getText().trim() : "";
        String mdp   = passwordField != null ? passwordField.getText() : "";

        if (email.isEmpty() || mdp.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }
        
        System.out.println("🔐 Tentative de connexion - Email: " + email);

        if (!ValidationDonnees.validerEmail(email)) {
            afficherErreur("Format d'email invalide.");
            return;
        }
        if (tentativesEchouees >= MAX_TENTATIVES) {
            afficherErreur("Compte verrouillé. Réessayez dans 5 minutes.");
            if (loginButton != null) loginButton.setDisable(true);
            planifierReactivation();
            return;
        }

        isProcessing = true;
        afficherChargement(true);

        new Thread(() -> {
            try {
                Utilisateur u = authService.login(email, mdp);
                javafx.application.Platform.runLater(() -> {
                    afficherChargement(false);
                    isProcessing = false;
                    if (u != null) {
                        System.out.println("✅ Connexion réussie - Email: " + email);
                        System.out.println("   Rôle récupéré: '" + u.getRole() + "'");
                        System.out.println("   ID: " + u.getId());
                        System.out.println("   Nom: " + u.getNom());
                        System.out.println("   Prénom: " + u.getPrenom());
                        
                        if (u instanceof Administrateur) {
                            System.out.println("   Type: Administrateur");
                        } else if (u instanceof Gestionnaire) {
                            System.out.println("   Type: Gestionnaire");
                        } else if (u instanceof Enseignant) {
                            System.out.println("   Type: Enseignant");
                        } else if (u instanceof Etudiant) {
                            System.out.println("   Type: Étudiant");
                        } else {
                            System.out.println("   Type: Utilisateur de base");
                        }
                        
                        connexionReussie(u);
                    } else {
                        System.out.println("❌ Échec de connexion - Email: " + email);
                        connexionEchouee();
                    }
                });
            } catch (SQLException ex) {
                logger.error("Erreur SQL lors de la connexion", ex);
                javafx.application.Platform.runLater(() -> {
                    afficherChargement(false);
                    isProcessing = false;
                    afficherErreur("Erreur de connexion à la base de données.");
                });
            }
        }).start();
    }

    /**
     * Traite une connexion réussie.
     * @param utilisateur l'utilisateur connecté
     */
    private void connexionReussie(Utilisateur utilisateur) {
        tentativesEchouees = 0;
        SessionUtilisateur.getInstance().connecter(utilisateur);
        afficherSucces("Connexion réussie !");
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> redirigerVersTableauBord(utilisateur));
        pause.play();
    }

    /**
     * Traite une connexion échouée (incrémente le compteur de tentatives).
     */
    private void connexionEchouee() {
        tentativesEchouees++;
        String msg = tentativesEchouees >= MAX_TENTATIVES
            ? "Compte verrouillé temporairement."
            : String.format("Email ou mot de passe incorrect. %d tentative(s) restante(s).",
                MAX_TENTATIVES - tentativesEchouees);
        afficherErreur(msg);
        if (passwordField != null) passwordField.clear();
    }

    /**
     * Redirige vers le tableau de bord correspondant au rôle.
     * @param utilisateur l'utilisateur connecté
     */
    private void redirigerVersTableauBord(Utilisateur utilisateur) {
        if (primaryStage == null) return;
        
        try {
            String role = utilisateur.getRole();
            System.out.println("🔍 Redirection - Rôle: '" + role + "'");
            
            String fxml = getFxmlPath(role);
            System.out.println("📂 Chargement FXML: " + fxml);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof TableauBordControleur) {
                ((TableauBordControleur) ctrl).initialiserAvecUtilisateur(utilisateur, primaryStage);
            }

            Scene scene = new Scene(root);
            try {
                String css = getClass().getResource("/css/style.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception ignored) {}

            primaryStage.setScene(scene);
            primaryStage.setTitle("SCHEDULER - " + utilisateur.getPrenom() + " " + utilisateur.getNom());
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true);

        } catch (IOException ex) {
            logger.error("Erreur navigation tableau de bord", ex);
            afficherErreur("Erreur de navigation : " + ex.getMessage());
        }
    }

    private String getFxmlPath(String role) {
        if (role == null) {
            System.out.println("⚠️ Rôle null, retour vers Login");
            return "/fxml/Login.fxml";
        }
        
        System.out.println("📁 Sélection FXML pour rôle: '" + role + "'");
        
        switch (role.toLowerCase().trim()) {
            case "admin":
            case "administrateur":
                System.out.println("   → TableauBordAdmin.fxml");
                return "/fxml/TableauBordAdmin.fxml";
            case "gestionnaire":
                System.out.println("   → TableauBordGestionnaire.fxml");
                return "/fxml/TableauBordGestionnaire.fxml";
            case "enseignant":
                System.out.println("   → TableauBordEnseignant.fxml");
                return "/fxml/TableauBordEnseignant.fxml";
            case "etudiant":
                System.out.println("   → TableauBordEtudiant.fxml");
                return "/fxml/TableauBordEtudiant.fxml";
            default:
                System.out.println("⚠️ Rôle non reconnu: '" + role + "', retour vers Admin par défaut");
                return "/fxml/TableauBordAdmin.fxml";
        }
    }

    private void handleCreerCompte() {
        if (primaryStage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Inscription.fxml"));
            Parent root = loader.load();
            InscriptionControleur ctrl = loader.getController();
            ctrl.setPrimaryStage(primaryStage);
            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); }
            catch (Exception ignored) {}
            primaryStage.setScene(scene);
        } catch (IOException ex) {
            logger.error("Erreur navigation inscription", ex);
            afficherErreur("Impossible d'ouvrir la page d'inscription.");
        }
    }

    /**
     * Gère la réinitialisation du mot de passe.
     */
    private void handleMotDePasseOublie() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mot de passe oublié");
        dialog.setHeaderText("Réinitialisation automatique");
        dialog.setContentText("Entrez votre adresse email :");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(email -> {
            if (!ValidationDonnees.validerEmail(email)) {
                afficherErreur("Email invalide.");
                return;
            }
            
            afficherInfo("⏳ Envoi du mot de passe par email en cours...");
            new Thread(() -> {
                try {
                    Utilisateur u = authService.trouverParEmail(email);
                    if (u != null && u.isEstValide()) {
                        String nouveauMotDePasse = GenerateurMotDePasse.generer();
                        utilisateurService.updateMotDePasse(u.getId(), nouveauMotDePasse);
                        
                        emailService.envoyerNouveauMotDePasse(
                            u.getEmail(),
                            u.getNom(),
                            u.getPrenom(),
                            nouveauMotDePasse
                        );
                        
                        javafx.application.Platform.runLater(() -> 
                            afficherSucces("✅ Un nouveau mot de passe a été envoyé à " + email)
                        );
                    } else {
                        javafx.application.Platform.runLater(() -> 
                            afficherErreur("Aucun compte actif trouvé avec cet email.")
                        );
                    }
                } catch (SQLException e) {
                    logger.error("Erreur vérification email", e);
                    javafx.application.Platform.runLater(() -> 
                        afficherErreur("Erreur technique. Veuillez réessayer.")
                    );
                }
            }).start();
        });
    }
    
    private String genererMotDePasse() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }    
        

    private void afficherErreur(String message) {
        if (messageLabel == null) return;
        messageLabel.setText(message);
        messageLabel.setStyle(
            "-fx-text-fill:#C62828;-fx-font-weight:700;" +
            "-fx-background-color:#FFEBEE;-fx-padding:8 12;-fx-background-radius:7;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void afficherSucces(String message) {
        if (messageLabel == null) return;
        messageLabel.setText(message);
        messageLabel.setStyle(
            "-fx-text-fill:#2E7D32;-fx-font-weight:700;" +
            "-fx-background-color:#E8F5E9;-fx-padding:8 12;-fx-background-radius:7;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void afficherInfo(String message) {
        if (messageLabel == null) return;
        messageLabel.setText(message);
        messageLabel.setStyle(
            "-fx-text-fill:#D39A43;-fx-font-weight:700;" +
            "-fx-background-color:#FFF8E7;-fx-padding:8 12;-fx-background-radius:7;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void afficherChargement(boolean visible) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(visible);
            loadingIndicator.setManaged(visible);
        }
        if (loginButton       != null) loginButton.setDisable(visible);
        if (inscriptionLink   != null) inscriptionLink.setDisable(visible);
        if (motDePasseOublieLink != null) motDePasseOublieLink.setDisable(visible);
    }

    private void planifierReactivation() {
        PauseTransition p = new PauseTransition(Duration.minutes(5));
        p.setOnFinished(e -> {
            tentativesEchouees = 0;
            if (loginButton != null) loginButton.setDisable(false);
        });
        p.play();
    }
}