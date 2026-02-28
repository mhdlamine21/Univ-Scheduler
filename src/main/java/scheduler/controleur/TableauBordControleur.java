package scheduler.controleur;

import scheduler.modele.Utilisateur;
import scheduler.service.ExportService;
import scheduler.service.NotificationService;
import scheduler.util.SessionUtilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import javafx.stage.Modality;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import scheduler.service.EchangeCreneauService;

/**
 * Contrôleur abstrait de base pour tous les tableaux de bord de l'application UNIV-SCHEDULER.
 * 
 * Ce contrôleur fournit les fonctionnalités communes transversales :
 * - Gestion du contexte utilisateur et de la session applicative active.
 * - Horloge dynamique en temps réel animée chaque seconde sur le thread JavaFX.
 * - Actions globales du menu supérieur (déconnexion sécurisée, profil, mot de passe).
 * - Navigation unifiée entre les différentes vues et fenêtres de l'application.
 * - Intégration des services partagés (notifications, exportations, audit).
 * 
 * Les contrôleurs spécialisés (Admin, Gestionnaire, Enseignant, Étudiant) dérivent
 * de cette classe et implémentent leurs logiques métier spécifiques.
 */
public abstract class TableauBordControleur {
    
    protected static final Logger logger = LoggerFactory.getLogger(TableauBordControleur.class);
    
    @FXML protected Label utilisateurLabel;
    @FXML protected Label roleLabel;
    @FXML protected Label dateLabel;
    @FXML protected Button deconnexionButton;
    @FXML protected Button profilButton;
    @FXML protected Button retourButton;
    @FXML protected MenuItem deconnexionMenuItem;
    @FXML protected MenuItem profilMenuItem;
    @FXML protected MenuItem changerMotDePasseMenuItem;
    @FXML protected MenuItem aideMenuItem;
    @FXML protected MenuItem aProposMenuItem;
    
    protected Utilisateur utilisateurConnecte;
    protected Stage primaryStage;
    protected SessionUtilisateur session;
    protected Timeline horlogeTempsReel;
    
    protected NotificationService notificationService;
    protected ExportService exportService;
    
    @FXML
    public void initialize() {
        this.session = SessionUtilisateur.getInstance();
        this.notificationService = new NotificationService();
        this.exportService = new ExportService();
        
        // Récupérer l'utilisateur depuis la session si disponible
        if (session != null && session.getUtilisateurConnecte() != null) {
            this.utilisateurConnecte = session.getUtilisateurConnecte();
            System.out.println("✅ TableauBordControleur - Utilisateur récupéré depuis session: " + 
                               utilisateurConnecte.getEmail());
        }
        
        configurerEvenementsCommuns();
        demarrerHorlogeTempsReel();
    }
    
    protected abstract void initialiserTableauBord();
    protected abstract void rafraichirDonnees();
    
    /**
     * Initialise le contrôleur avec l'utilisateur connecté.
     * @param utilisateur l'utilisateur connecté
     * @param stage la fenêtre principale
     */
    public void initialiserAvecUtilisateur(Utilisateur utilisateur, Stage stage) {
        if (utilisateur == null) {
            logger.error("Tentative d'initialisation avec un utilisateur null");
            return;
        }
        
        this.utilisateurConnecte = utilisateur;
        this.primaryStage = stage;
        
        // ✅ AJOUT : Mettre à jour la session
        if (session != null) {
            session.connecter(utilisateur);
        }
        
        mettreAJourInterface();
        initialiserTableauBord();
    }
    
    /**
     * Configure les événements communs à tous les tableaux de bord.
     */
    protected void configurerEvenementsCommuns() {
        if (deconnexionButton != null) {
            deconnexionButton.setOnAction(e -> handleDeconnexion());
        }
        
        if (deconnexionMenuItem != null) {
            deconnexionMenuItem.setOnAction(e -> handleDeconnexion());
        }
        
        if (profilButton != null) {
            profilButton.setOnAction(e -> voirProfil());
        }
        
        if (profilMenuItem != null) {
            profilMenuItem.setOnAction(e -> voirProfil());
        }
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetour());
        }
        
        if (changerMotDePasseMenuItem != null) {
            changerMotDePasseMenuItem.setOnAction(e -> changerMotDePasse());
        }
        
        if (aideMenuItem != null) {
            aideMenuItem.setOnAction(e -> afficherAide());
        }
        
        if (aProposMenuItem != null) {
            aProposMenuItem.setOnAction(e -> afficherAPropos());
        }
    }
    
    protected Stage getStageFromScene() {
        if (primaryStage != null) {
            return primaryStage;
        }
        
        if (utilisateurLabel != null && utilisateurLabel.getScene() != null) {
            return (Stage) utilisateurLabel.getScene().getWindow();
        }
        if (deconnexionButton != null && deconnexionButton.getScene() != null) {
            return (Stage) deconnexionButton.getScene().getWindow();
        }
        if (profilButton != null && profilButton.getScene() != null) {
            return (Stage) profilButton.getScene().getWindow();
        }
        if (dateLabel != null && dateLabel.getScene() != null) {
            return (Stage) dateLabel.getScene().getWindow();
        }
        
        return null;
    }
    
    protected void handleRetour() {
        if (utilisateurConnecte == null) {
            logger.error("utilisateurConnecte est null dans handleRetour()");
            retourLogin();
            return;
        }
        
        try {
            String fxmlFile = getFxmlPath(utilisateurConnecte.getRole());
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            
            Object controleur = loader.getController();
            if (controleur instanceof TableauBordControleur) {
                ((TableauBordControleur) controleur).initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            }
            
            Scene scene = new Scene(root);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            
            Stage stage = getStageFromScene();
            if (stage != null) {
                stage.setScene(scene);
            }
            
        } catch (IOException e) {
            logger.error("Erreur retour tableau bord", e);
            afficherErreur("Impossible de retourner au tableau de bord");
        } catch (Exception e) {
            logger.error("Erreur inattendue", e);
            afficherErreur("Une erreur inattendue est survenue");
        }
    }
    
    protected String getFxmlPath(String role) {
        switch (role) {
            case "admin": return "/fxml/TableauBordAdmin.fxml";
            case "gestionnaire": return "/fxml/TableauBordGestionnaire.fxml";
            case "enseignant": return "/fxml/TableauBordEnseignant.fxml";
            case "etudiant": return "/fxml/TableauBordEtudiant.fxml";
            default: return "/fxml/TableauBordAdmin.fxml";
        }
    }
    
    protected void retourLogin() {
        try {
            Stage stageToUse = getStageFromScene();
            
            if (stageToUse == null) {
                logger.error("❌ Impossible de récupérer le stage pour retourner au login");
                afficherErreur("Erreur de navigation");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            
            LoginControleur loginControleur = loader.getController();
            loginControleur.setPrimaryStage(stageToUse);
            
            Scene scene = new Scene(root);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            
            stageToUse.setScene(scene);
            stageToUse.setTitle("SCHEDULER - Connexion");
            stageToUse.setResizable(true);
            
        } catch (IOException e) {
            logger.error("Erreur retour login", e);
            afficherErreur("Impossible de retourner à la page de connexion");
        }
    }
    
    protected void voirProfil() {
        if (utilisateurConnecte != null) {
            ouvrirProfil(utilisateurConnecte);
            return;
        }
        
        if (session != null && session.getUtilisateurConnecte() != null) {
            this.utilisateurConnecte = session.getUtilisateurConnecte();
            ouvrirProfil(utilisateurConnecte);
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            LoginControleur loginControleur = loader.getController();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Session expirée");
            alert.setHeaderText("Votre session a expiré");
            alert.setContentText("Veuillez vous reconnecter pour accéder à votre profil.");
            alert.showAndWait();
            
            Stage stage = getStageFromScene();
            if (stage != null) {
                Scene scene = new Scene(root);
                String css = getClass().getResource("/css/style.css").toExternalForm();
                if (css != null) scene.getStylesheets().add(css);
                stage.setScene(scene);
                stage.setTitle("SCHEDULER - Connexion");
            }
            
        } catch (IOException e) {
            logger.error("Erreur retour login", e);
        }
    }
    
    private void ouvrirProfil(Utilisateur utilisateur) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profil.fxml"));
            Parent root = loader.load();
            
            ProfilControleur controleur = loader.getController();
            
            Stage parentStage = getStageFromScene();
            if (parentStage == null) {
                parentStage = primaryStage;
            }
            
            Stage dialogStage = new Stage();
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            if (parentStage != null) {
                dialogStage.initOwner(parentStage);
            }
            dialogStage.setTitle("Mon Profil - UNIV-SCHEDULER");
            dialogStage.setResizable(false);
            
            controleur.initialiserEnModal(utilisateur, dialogStage, this);
            
            Scene scene = new Scene(root, 580, 620);
            try {
                String css = getClass().getResource("/css/style.css").toExternalForm();
                if (css != null) scene.getStylesheets().add(css);
            } catch (Exception ignored) {}
            
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
            // Recharger l'utilisateur connecté après la fermeture de la modale
            if (SessionUtilisateur.getInstance().getUtilisateurConnecte() != null) {
                this.utilisateurConnecte = SessionUtilisateur.getInstance().getUtilisateurConnecte();
                mettreAJourInterface();
            }
            
        } catch (IOException e) {
            logger.error("Erreur affichage profil en modale", e);
            afficherErreur("Impossible d'afficher le profil : " + e.getMessage());
        }
    }
    
    /**
     * Ouvre la modale de consultation et de reprogrammation des cours reportés.
     */
    public void ouvrirModalCoursReportes() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        Stage owner = getStageFromScene();
        if (owner != null) dialogStage.initOwner(owner);
        dialogStage.setTitle("🔄 Cours Reportés & Reprogrammation - Univ-Scheduler");

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FBF8F3;");
        root.setPrefWidth(720);
        root.setPrefHeight(540);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label("🔄");
        iconLbl.setStyle("-fx-font-size: 28px;");
        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Suivi des Cours Reportés & À Reprogrammer");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #3D261A;");
        Label subLbl = new Label("Consultez les créneaux cédés lors de négociations d'urgence et reprogrammez-les facilement.");
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C6D58;");
        titleBox.getChildren().addAll(titleLbl, subLbl);
        header.getChildren().addAll(iconLbl, titleBox);

        VBox listContainer = new VBox(10);
        listContainer.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #E6DCCD; -fx-border-width: 1;");
        
        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(380);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        int enseignantId = (utilisateurConnecte != null && "enseignant".equalsIgnoreCase(utilisateurConnecte.getRole()))
                ? utilisateurConnecte.getId() : 0;
        
        EchangeCreneauService echangeService = new EchangeCreneauService();
        List<Map<String, Object>> coursReportes = echangeService.listerCoursReportes(enseignantId);

        if (coursReportes.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));
            Label emptyIcon = new Label("🎉");
            emptyIcon.setStyle("-fx-font-size: 36px;");
            Label emptyText = new Label("Aucun cours reporté en attente de reprogrammation !");
            emptyText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            Label emptySub = new Label("Tous les cours sont actuellement planifiés normalement sur leur créneau.");
            emptySub.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C6D58;");
            emptyBox.getChildren().addAll(emptyIcon, emptyText, emptySub);
            listContainer.getChildren().add(emptyBox);
        } else {
            for (Map<String, Object> cr : coursReportes) {
                int crId = (int) cr.get("id");
                String matiere = (String) cr.getOrDefault("matiere", "Cours magistral / TD");
                String dateInitiale = (String) cr.getOrDefault("dateInitiale", "-");
                String heureDebut = (String) cr.getOrDefault("heureDebutInitiale", "--:--");
                String heureFin = (String) cr.getOrDefault("heureFinInitiale", "--:--");
                String motif = (String) cr.getOrDefault("motifReport", "Libération négociée");
                int classeId = (int) cr.getOrDefault("classeId", 0);

                HBox card = new HBox(14);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color: #FBF8F3; -fx-padding: 12 16; -fx-background-radius: 8; -fx-border-color: #E6DCCD; -fx-border-width: 1;");

                VBox details = new VBox(3);
                HBox.setHgrow(details, Priority.ALWAYS);
                Label matiereLbl = new Label("📚 " + matiere);
                matiereLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3D261A;");
                Label dateLbl = new Label("🗓️ Créneau d'origine : " + dateInitiale + " (" + heureDebut + " - " + heureFin + ")");
                dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B4226;");
                Label motifLbl = new Label("💬 Motif du report : " + motif);
                motifLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8C6D58; -fx-font-style: italic;");
                details.getChildren().addAll(matiereLbl, dateLbl, motifLbl);

                Button reprogrammerBtn = new Button("⚡ Reprogrammer");
                reprogrammerBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 14; -fx-cursor: hand;");
                reprogrammerBtn.setOnAction(e -> {
                    dialogStage.close();
                    ouvrirFenetreReservationPourReport(crId, matiere, classeId);
                });

                card.getChildren().addAll(details, reprogrammerBtn);
                listContainer.getChildren().add(card);
            }
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color: #8C6D58; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 18; -fx-font-weight: bold; -fx-cursor: hand;");
        fermerBtn.setOnAction(e -> dialogStage.close());
        actions.getChildren().add(fermerBtn);

        root.getChildren().addAll(header, scroll, actions);

        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
        dialogStage.show();
    }

    private void ouvrirFenetreReservationPourReport(int coursReporteId, String matiere, int classeId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Reservation.fxml"));
            Parent root = loader.load();
            ReservationControleur resCtrl = loader.getController();
            resCtrl.initialiserAvecUtilisateur(utilisateurConnecte, getStageFromScene());
            resCtrl.initialiserPourReprogrammation(coursReporteId, matiere, classeId);

            Stage stage = getStageFromScene();
            if (stage != null) {
                Scene scene = new Scene(root);
                String css = getClass().getResource("/css/style.css") != null ? getClass().getResource("/css/style.css").toExternalForm() : null;
                if (css != null) scene.getStylesheets().add(css);
                stage.setScene(scene);
            }
        } catch (Exception ex) {
            logger.error("Erreur ouverture réservation pour report", ex);
        }
    }
    
    protected void handleDeconnexion() {
        if (utilisateurConnecte == null) {
            retourLogin();
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Voulez-vous vraiment vous déconnecter ?");
        alert.setContentText("Toutes les modifications non sauvegardées seront perdues.");
        
        ButtonType sauvegarderButton = new ButtonType("Sauvegarder et déconnecter");
        ButtonType deconnecterButton = new ButtonType("Déconnecter");
        ButtonType annulerButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(sauvegarderButton, deconnecterButton, annulerButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent()) {
            if (result.get() == sauvegarderButton) {
                sauvegarderToutesModifications();
                afficherNotification("Sauvegarde", "Toutes les modifications ont été sauvegardées");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                effectuerDeconnexion();
            } else if (result.get() == deconnecterButton) {
                effectuerDeconnexion();
            }
        }
    }
    
    protected void sauvegarderToutesModifications() {
        // À surcharger dans les classes filles
    }
    
    private void effectuerDeconnexion() {
        arreterHorloge();
        if (session != null) {
            session.deconnecter();
        }
        this.utilisateurConnecte = null;
        retourLogin();
    }
    
    /**
     * Met à jour les informations textuelles de l'interface et démarre l'horloge temps réel.
     */
    protected void mettreAJourInterface() {
        if (utilisateurConnecte != null) {
            String nomComplet = utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom();
            if (utilisateurLabel != null) {
                utilisateurLabel.setText(nomComplet);
            }
            if (roleLabel != null) {
                roleLabel.setText("(" + traduireRole(utilisateurConnecte.getRole()) + ")");
            }
        }
        demarrerHorlogeTempsReel();
    }

    /**
     * Initialise et démarre l'horloge dynamique en temps réel.
     * Actualise automatiquement l'heure et la date chaque seconde dans le dateLabel.
     */
    protected void demarrerHorlogeTempsReel() {
        if (horlogeTempsReel != null) {
            horlogeTempsReel.stop();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy - HH:mm:ss", Locale.FRENCH);
        Runnable actualiser = () -> {
            if (dateLabel != null) {
                String texteDate = LocalDateTime.now().format(formatter);
                if (!texteDate.isEmpty()) {
                    texteDate = Character.toUpperCase(texteDate.charAt(0)) + texteDate.substring(1);
                }
                dateLabel.setText(texteDate);
            }
        };
        actualiser.run();
        horlogeTempsReel = new Timeline(new KeyFrame(Duration.seconds(1), event -> actualiser.run()));
        horlogeTempsReel.setCycleCount(Animation.INDEFINITE);
        horlogeTempsReel.play();
    }

    /**
     * Arrête l'horloge dynamique en temps réel pour libérer les ressources JavaFX.
     */
    public void arreterHorloge() {
        if (horlogeTempsReel != null) {
            horlogeTempsReel.stop();
            horlogeTempsReel = null;
        }
    }
    
    /**
     * Traduit un rôle technique en libellé français.
     * @param role le rôle technique
     * @return le libellé en français
     */
    protected String traduireRole(String role) {
        switch (role) {
            case "admin": return "Administrateur";
            case "gestionnaire": return "Gestionnaire";
            case "enseignant": return "Enseignant";
            case "etudiant": return "Étudiant";
            default: return role;
        }
    }
    
    /**
     * Ouvre la fenêtre de changement de mot de passe.
     */
    protected void changerMotDePasse() {
        if (utilisateurConnecte == null) {
            logger.error("utilisateurConnecte est null dans changerMotDePasse()");
            afficherErreur("Impossible de changer le mot de passe : utilisateur non connecté");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChangerMotDePasse.fxml"));
            Parent root = loader.load();
            
            ChangerMotDePasseControleur controleur = loader.getController();
            
            Stage stage = getStageFromScene();
            if (stage == null) {
                stage = primaryStage;
            }
            
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, stage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            if (stage != null) {
                stage.setScene(scene);
            }
            
        } catch (IOException e) {
            logger.error("Erreur changement mot de passe", e);
            afficherErreur("Impossible de changer le mot de passe");
        }
    }
    
    /**
     * Affiche l'aide.
     */
    protected void afficherAide() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aide");
        alert.setHeaderText("Guide d'utilisation");
        alert.setContentText("Pour toute assistance, contactez le support :\n" +
                           "support@scheduler.univ.sn\n" +
                           "Tél: 77 123 45 67");
        alert.show();
    }
    
    /**
     * Affiche les informations "À propos".
     */
    protected void afficherAPropos() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText("SCHEDULER - Gestion des emplois du temps");
        alert.setContentText("Version 2.0.0\n" +
                           "© 2025 Université Iba Der Thiam de Thiès");
        alert.show();
    }
    
    protected void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.show();
    }
    
    /**
     * Affiche une boîte de dialogue de confirmation.
     * @param titre le titre
     * @param message le message
     * @return true si l'utilisateur a confirmé
     */
    protected boolean afficherConfirmation(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titre);
        alert.setHeaderText(message);
        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }
    
    /**
     * Affiche une notification.
     * @param titre le titre
     * @param message le message
     */
    protected void afficherNotification(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    
    protected Stage getPrimaryStage() {
        return primaryStage;
    }
    
    protected void verifierNotifications() {
        if (utilisateurConnecte == null) {
            return;
        }
        
        new Thread(() -> {
            try {
                var notifications = notificationService.getNotifications(utilisateurConnecte.getId());
                if (!notifications.isEmpty()) {
                    javafx.application.Platform.runLater(() -> {
                        afficherNotification("Notifications", 
                            "Vous avez " + notifications.size() + " notification(s)");
                    });
                }
            } catch (Exception e) {
                logger.error("Erreur vérification notifications", e);
            }
        }).start();
    }
    
    /**
     * Navigue vers un FXML en remplaçant le contenu principal si possible,
     * sinon remplace toute la scène.
     */
    protected void naviguerVers(String fxmlPath, boolean conserverSidebar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            // Transmettre l'utilisateur
            Object controleur = loader.getController();
            if (controleur instanceof TableauBordControleur) {
                ((TableauBordControleur) controleur).initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            }
            
            // Essayer de trouver le contenuPrincipal
            StackPane contenuPrincipal = trouverContenuPrincipal();
            if (contenuPrincipal != null && conserverSidebar) {
                contenuPrincipal.getChildren().clear();
                contenuPrincipal.getChildren().add(root);
            } else {
                Scene scene = new Scene(root);
                String css = getClass().getResource("/css/style.css").toExternalForm();
                if (css != null) scene.getStylesheets().add(css);
                primaryStage.setScene(scene);
            }
            
        } catch (IOException e) {
            logger.error("Erreur navigation vers {}", fxmlPath, e);
            afficherErreur("Impossible d'accéder à cette page");
        }
    }
    
    /**
     * Récupère le StackPane contenuPrincipal depuis la hiérarchie.
     * Utile pour les contrôleurs enfants.
     */
    protected StackPane getContenuPrincipal() {
        if (primaryStage != null && primaryStage.getScene() != null) {
            Parent root = primaryStage.getScene().getRoot();
            Node found = root.lookup("#contenuPrincipal");
            if (found instanceof StackPane) {
                return (StackPane) found;
            }
        }
        return null;
    }

    /**
     * Trouve le StackPane contenuPrincipal dans la hiérarchie.
     */
    private StackPane trouverContenuPrincipal() {
        if (primaryStage != null && primaryStage.getScene() != null) {
            Parent root = primaryStage.getScene().getRoot();
            Node found = root.lookup("#contenuPrincipal");
            if (found instanceof StackPane) {
                return (StackPane) found;
            }
        }
        return null;
    }
    
}