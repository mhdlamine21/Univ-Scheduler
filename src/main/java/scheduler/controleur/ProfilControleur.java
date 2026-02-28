package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import scheduler.util.SessionUtilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import java.sql.SQLException;
import java.io.IOException;

/**
 * Contrôleur pour la fenêtre modale de consultation et modification du profil utilisateur.
 */
public class ProfilControleur extends TableauBordControleur {
    
    @FXML private Label roleHeaderLabel;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    
    @FXML private VBox etudiantPane;
    @FXML private TextField numeroEtudiantField;
    @FXML private TextField ufrField;
    @FXML private TextField classeField;
    @FXML private Label statutResponsableBadge;
    
    @FXML private VBox enseignantPane;
    @FXML private TextField matriculeField;
    
    @FXML private Label messageLabel;
    @FXML private HBox actionsConsultationBox;
    @FXML private HBox actionsEditionBox;
    @FXML private Button modifierButton;
    @FXML private Button annulerButton;
    @FXML private Button sauvegarderButton;
    @FXML private Button changerMotDePasseButton;
    @FXML private Button retourButton;
    
    private UtilisateurService utilisateurService;
    private UfrService ufrService;
    private ClasseService classeService;
    
    private Stage modalStage;
    private TableauBordControleur parentControleur;
    private boolean modeEdition = false;
    
    @Override
    public void initialize() {
        super.initialize();
        this.utilisateurService = new UtilisateurService();
        this.ufrService = new UfrService();
        this.classeService = new ClasseService();
        configurerEvenements();
    }
    
    @Override
    protected void initialiserTableauBord() {
        chargerProfil();
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerProfil();
    }
    
    /**
     * Initialise le contrôleur en mode modale fermable.
     */
    public void initialiserEnModal(Utilisateur utilisateur, Stage modalStage, TableauBordControleur parentControleur) {
        this.modalStage = modalStage;
        this.parentControleur = parentControleur;
        this.utilisateurConnecte = utilisateur;
        chargerProfil();
    }
    
    @Override
    public void initialiserAvecUtilisateur(Utilisateur utilisateur, Stage stage) {
        this.modalStage = stage;
        this.utilisateurConnecte = utilisateur;
        chargerProfil();
    }
    
    private void configurerEvenements() {
        if (modifierButton != null) modifierButton.setOnAction(e -> activerEdition());
        if (annulerButton != null) annulerButton.setOnAction(e -> annulerEdition());
        if (sauvegarderButton != null) sauvegarderButton.setOnAction(e -> sauvegarderModifications());
        if (changerMotDePasseButton != null) changerMotDePasseButton.setOnAction(e -> naviguerVersChangementMotDePasse());
        if (retourButton != null) retourButton.setOnAction(e -> handleRetour());
    }
    
    /**
     * Charge et affiche les informations du profil utilisateur connecté.
     */
    private void chargerProfil() {
        if (utilisateurConnecte == null) {
            this.utilisateurConnecte = SessionUtilisateur.getInstance().getUtilisateurConnecte();
        }
        if (utilisateurConnecte == null) return;
        
        if (roleHeaderLabel != null) {
            String roleNom = switch (utilisateurConnecte.getRole().toLowerCase()) {
                case "admin" -> "Administrateur Système";
                case "gestionnaire" -> "Gestionnaire de Scolarité";
                case "enseignant" -> "Enseignant / Chercheur";
                case "etudiant" -> "Étudiant";
                default -> utilisateurConnecte.getRole();
            };
            roleHeaderLabel.setText(roleNom + " - UIDT");
        }
        
        nomField.setText(utilisateurConnecte.getNom());
        prenomField.setText(utilisateurConnecte.getPrenom());
        emailField.setText(utilisateurConnecte.getEmail());
        
        if (utilisateurConnecte instanceof Etudiant etudiant) {
            if (etudiantPane != null) {
                etudiantPane.setVisible(true);
                etudiantPane.setManaged(true);
            }
            if (enseignantPane != null) {
                enseignantPane.setVisible(false);
                enseignantPane.setManaged(false);
            }
            
            numeroEtudiantField.setText(etudiant.getNumeroEtudiant() != null ? etudiant.getNumeroEtudiant() : "");
            
            boolean estResponsable = etudiant.estResponsable();
            if (statutResponsableBadge != null) {
                statutResponsableBadge.setVisible(estResponsable);
                statutResponsableBadge.setManaged(estResponsable);
            }
            
            Task<Void> taskUfr = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    Ufr ufr = etudiant.getUfrId() > 0 ? ufrService.trouverParId(etudiant.getUfrId()) : null;
                    Classe classe = etudiant.getClasseId() > 0 ? classeService.trouverParId(etudiant.getClasseId()) : null;
                    
                    javafx.application.Platform.runLater(() -> {
                        if (ufr != null && ufrField != null) ufrField.setText(ufr.getNom());
                        if (classe != null && classeField != null) classeField.setText(classe.getIntitule());
                    });
                    return null;
                }
            };
            new Thread(taskUfr).start();
            
        } else if (utilisateurConnecte instanceof Enseignant enseignant) {
            if (etudiantPane != null) {
                etudiantPane.setVisible(false);
                etudiantPane.setManaged(false);
            }
            if (enseignantPane != null) {
                enseignantPane.setVisible(true);
                enseignantPane.setManaged(true);
            }
            if (matriculeField != null) {
                matriculeField.setText(enseignant.getMatricule() != null ? enseignant.getMatricule() : "");
            }
        } else {
            if (etudiantPane != null) {
                etudiantPane.setVisible(false);
                etudiantPane.setManaged(false);
            }
            if (enseignantPane != null) {
                enseignantPane.setVisible(false);
                enseignantPane.setManaged(false);
            }
        }
        
        desactiverEdition();
    }
    
    /**
     * Active le mode édition des champs autorisés.
     */
    private void activerEdition() {
        modeEdition = true;
        nomField.setEditable(true);
        prenomField.setEditable(true);
        emailField.setEditable(true);
        
        nomField.setStyle("-fx-padding: 8 10; -fx-background-color: white; -fx-border-color: #D39A43; -fx-border-radius: 6;");
        prenomField.setStyle("-fx-padding: 8 10; -fx-background-color: white; -fx-border-color: #D39A43; -fx-border-radius: 6;");
        emailField.setStyle("-fx-padding: 8 10; -fx-background-color: white; -fx-border-color: #D39A43; -fx-border-radius: 6;");
        
        if (actionsConsultationBox != null) {
            actionsConsultationBox.setVisible(false);
            actionsConsultationBox.setManaged(false);
        }
        if (actionsEditionBox != null) {
            actionsEditionBox.setVisible(true);
            actionsEditionBox.setManaged(true);
        }
        if (messageLabel != null) messageLabel.setVisible(false);
    }
    
    /**
     * Désactive le mode édition.
     */
    private void desactiverEdition() {
        modeEdition = false;
        nomField.setEditable(false);
        prenomField.setEditable(false);
        emailField.setEditable(false);
        
        nomField.setStyle("-fx-padding: 8 10; -fx-background-color: #F8F6F2; -fx-border-color: #E6DCCD; -fx-border-radius: 6;");
        prenomField.setStyle("-fx-padding: 8 10; -fx-background-color: #F8F6F2; -fx-border-color: #E6DCCD; -fx-border-radius: 6;");
        emailField.setStyle("-fx-padding: 8 10; -fx-background-color: #F8F6F2; -fx-border-color: #E6DCCD; -fx-border-radius: 6;");
        
        if (actionsConsultationBox != null) {
            actionsConsultationBox.setVisible(true);
            actionsConsultationBox.setManaged(true);
        }
        if (actionsEditionBox != null) {
            actionsEditionBox.setVisible(false);
            actionsEditionBox.setManaged(false);
        }
    }
    
    private void annulerEdition() {
        chargerProfil();
        desactiverEdition();
        if (messageLabel != null) messageLabel.setVisible(false);
    }
    
    /**
     * Sauvegarde les modifications apportées aux informations de profil.
     */
    private void sauvegarderModifications() {
        if (!validerFormulaire()) return;
        
        utilisateurConnecte.setNom(nomField.getText().trim());
        utilisateurConnecte.setPrenom(prenomField.getText().trim());
        utilisateurConnecte.setEmail(emailField.getText().trim());
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                utilisateurService.modifier(utilisateurConnecte);
                return null;
            }
            
            @Override
            protected void succeeded() {
                SessionUtilisateur.getInstance().connecter(utilisateurConnecte);
                desactiverEdition();
                if (messageLabel != null) {
                    messageLabel.setText("✅ Profil mis à jour avec succès !");
                    messageLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    messageLabel.setVisible(true);
                }
                if (parentControleur != null) {
                    parentControleur.mettreAJourInterface();
                }
            }
            
            @Override
            protected void failed() {
                if (messageLabel != null) {
                    messageLabel.setText("❌ Erreur lors de la mise à jour du profil.");
                    messageLabel.setStyle("-fx-text-fill: #9E2A2B; -fx-font-weight: bold;");
                    messageLabel.setVisible(true);
                }
            }
        };
        new Thread(task).start();
    }
    
    private boolean validerFormulaire() {
        if (nomField.getText().trim().isEmpty() || prenomField.getText().trim().isEmpty()) {
            afficherMessageErreur("Le nom et le prénom sont obligatoires.");
            return false;
        }
        if (emailField.getText().trim().isEmpty() || !emailField.getText().contains("@")) {
            afficherMessageErreur("Veuillez saisir une adresse email valide.");
            return false;
        }
        return true;
    }
    
    private void afficherMessageErreur(String msg) {
        if (messageLabel != null) {
            messageLabel.setText("⚠️ " + msg);
            messageLabel.setStyle("-fx-text-fill: #9E2A2B; -fx-font-weight: bold;");
            messageLabel.setVisible(true);
        }
    }
    
    /**
     * Ouvre la fenêtre modale de changement de mot de passe sans quitter la page actuelle.
     */
    private void naviguerVersChangementMotDePasse() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChangerMotDePasse.fxml"));
            Parent root = loader.load();
            
            ChangerMotDePasseControleur controleur = loader.getController();
            
            Stage pwdStage = new Stage();
            pwdStage.initModality(Modality.APPLICATION_MODAL);
            if (modalStage != null) {
                pwdStage.initOwner(modalStage);
            }
            pwdStage.setTitle("Modification du mot de passe");
            pwdStage.setResizable(false);
            
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, pwdStage);
            
            Scene scene = new Scene(root, 480, 520);
            try {
                String css = getClass().getResource("/css/style.css").toExternalForm();
                if (css != null) scene.getStylesheets().add(css);
            } catch (Exception ignored) {}
            
            pwdStage.setScene(scene);
            pwdStage.showAndWait();
            
        } catch (IOException e) {
            logger.error("Erreur ouverture changement mot de passe", e);
            afficherMessageErreur("Impossible d'ouvrir le changement de mot de passe.");
        }
    }
    
    @FXML
    @Override
    protected void handleRetour() {
        // En mode modale, fermer simplement la fenêtre popup !
        if (modalStage != null) {
            modalStage.close();
        } else {
            super.handleRetour();
        }
    }
}