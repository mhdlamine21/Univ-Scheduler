package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.AuthentificationService;
import scheduler.service.ClasseService;
import scheduler.service.UfrService;
import scheduler.util.ValidationDonnees;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Contrôleur pour l'écran Inscription.
 */
public class InscriptionControleur {
    
    private static final Logger logger = LoggerFactory.getLogger(InscriptionControleur.class);
    
    @FXML private ToggleGroup roleGroup;
    @FXML private RadioButton etudiantRadio;
    @FXML private RadioButton enseignantRadio;
    @FXML private RadioButton gestionnaireRadio;
    
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    
    @FXML private VBox etudiantPanel;
    @FXML private TextField numeroEtudiantField;
    @FXML private ComboBox<Ufr> ufrComboBox;
    @FXML private ComboBox<Classe> classeComboBox;
    @FXML private CheckBox responsableClasseCheckBox;
    @FXML private Label ufrInfoLabel;
    
    @FXML private VBox enseignantPanel;
    @FXML private TextField matriculeField;
    @FXML private ComboBox<String> gradeComboBox; 
    @FXML private Label gradeInfoLabel; 
    
    @FXML private VBox gestionnairePanel;
    
    @FXML private Button retourButton;
    @FXML private Button inscrireButton;
    @FXML private Button annulerButton;
    
    @FXML private Label messageLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label dateLabel;
    
    private AuthentificationService authService;
    private UfrService ufrService;
    private ClasseService classeService;
    private Stage primaryStage;
    private boolean isProcessing = false;
    private boolean modeAdmin = false;
    
    private static final double FENETRE_LARGEUR = 600;
    private static final double FENETRE_HAUTEUR = 750;
    
    public InscriptionControleur() {
        this.authService = new AuthentificationService();
        this.ufrService = new UfrService();
        this.classeService = new ClasseService();
    }
    
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        mettreAJourDate();
        
        primaryStage.setMinWidth(FENETRE_LARGEUR);
        primaryStage.setMinHeight(FENETRE_HAUTEUR);
        primaryStage.setWidth(FENETRE_LARGEUR);
        primaryStage.setHeight(FENETRE_HAUTEUR);
        primaryStage.setResizable(true);
    }
    
    public void setModeAdmin(boolean mode) {
        this.modeAdmin = mode;
    }
    
    @FXML
    private void initialize() {
    	 configurerGroupes();
    	    configurerEvenements();
    	    configurerValidateurs();
    	    chargerUfr();
    	    configurerGradeCombo();
    	    mettreAJourDate();
    	    
    	    etudiantPanel.setVisible(true);
    	    etudiantPanel.setManaged(true);
    	    enseignantPanel.setVisible(false);
    	    enseignantPanel.setManaged(false);
    	    if (gestionnairePanel != null) {
    	        gestionnairePanel.setVisible(false);
    	        gestionnairePanel.setManaged(false);
    	    }
    	}
    
    private void configurerGradeCombo() {
        gradeComboBox.getItems().addAll("Mr.", "Mme.", "Dr.");
        gradeComboBox.setPromptText("Grade *");
        gradeInfoLabel.setText("Choisissez le grade de l'enseignant");
        gradeInfoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
    }
    
    private Timeline horlogeTimeline;
    
    /**
     * Démarre l'horloge en temps réel sur l'écran d'inscription.
     */
    private void mettreAJourDate() {
        if (horlogeTimeline != null) {
            horlogeTimeline.stop();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.FRENCH);
        Runnable actualiser = () -> {
            if (dateLabel != null) {
                dateLabel.setText(LocalDateTime.now().format(formatter));
            }
        };
        actualiser.run();
        horlogeTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> actualiser.run()));
        horlogeTimeline.setCycleCount(Animation.INDEFINITE);
        horlogeTimeline.play();
    }
    
    private void configurerGroupes() {
        roleGroup = new ToggleGroup();
        etudiantRadio.setToggleGroup(roleGroup);
        enseignantRadio.setToggleGroup(roleGroup);
        gestionnaireRadio.setToggleGroup(roleGroup);
        etudiantRadio.setSelected(true);
    }
    
    private void configurerEvenements() {
        roleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal == etudiantRadio) {
                    adapterFormulaire("etudiant");
                } else if (newVal == enseignantRadio) {
                    adapterFormulaire("enseignant");
                } else if (newVal == gestionnaireRadio) {
                    adapterFormulaire("gestionnaire");
                }
            }
        });
        
        ufrComboBox.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    chargerClassesParUfr(newVal.getId());
                }
            });
        
        inscrireButton.setOnAction(e -> handleInscription());
        annulerButton.setOnAction(e -> retourLogin());
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> retourLogin());
        }
    }
    
    private void configurerValidateurs() {
        ValidationDonnees.ajouterValidateurNonVide(nomField, "Nom requis");
        ValidationDonnees.ajouterValidateurNonVide(prenomField, "Prénom requis");
        ValidationDonnees.ajouterValidateurEmail(emailField);
    }
    
    private void adapterFormulaire(String role) {
        // Cacher tous les panneaux
        etudiantPanel.setVisible(false);
        etudiantPanel.setManaged(false);
        enseignantPanel.setVisible(false);
        enseignantPanel.setManaged(false);
        if (gestionnairePanel != null) {
            gestionnairePanel.setVisible(false);
            gestionnairePanel.setManaged(false);
        }
        
        switch (role) {
            case "etudiant":
                etudiantPanel.setVisible(true);
                etudiantPanel.setManaged(true);
                break;
            case "enseignant":
                enseignantPanel.setVisible(true);
                enseignantPanel.setManaged(true);
                break;
            case "gestionnaire":
                if (gestionnairePanel != null) {
                    gestionnairePanel.setVisible(true);
                    gestionnairePanel.setManaged(true);
                }
                break;
        }
    }
    
    private void chargerUfr() {
        try {
            List<Ufr> ufrs = ufrService.listerTous();
            ufrComboBox.getItems().setAll(ufrs);
            
            ufrComboBox.setCellFactory(lv -> new ListCell<Ufr>() {
                @Override
                protected void updateItem(Ufr item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.getNom());
                }
            });
            
            ufrComboBox.setButtonCell(new ListCell<Ufr>() {
                @Override
                protected void updateItem(Ufr item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.getNom());
                }
            });
            
            if (!ufrs.isEmpty()) {
                ufrComboBox.getSelectionModel().selectFirst();
                ufrInfoLabel.setText(ufrs.size() + " UFR disponibles");
            }
            
        } catch (SQLException e) {
            logger.error("Erreur chargement UFR", e);
            ufrInfoLabel.setText("Erreur de chargement");
        }
    }
    
    private void chargerClassesParUfr(int ufrId) {
        try {
            List<Classe> classes = classeService.listerParUfr(ufrId);
            classeComboBox.getItems().setAll(classes);
            
            classeComboBox.setCellFactory(lv -> new ListCell<Classe>() {
                @Override
                protected void updateItem(Classe item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : 
                        item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                }
            });
            
            classeComboBox.setButtonCell(new ListCell<Classe>() {
                @Override
                protected void updateItem(Classe item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : 
                        item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                }
            });
            
            if (!classes.isEmpty()) {
                classeComboBox.getSelectionModel().selectFirst();
            }
            
        } catch (SQLException e) {
            logger.error("Erreur chargement classes", e);
        }
    }
    
    @FXML
    private void handleInscription() {
        System.out.println("🔍 handleInscription appelée !");  // ← AJOUTER
        
        if (isProcessing) {
            System.out.println("❌ isProcessing = true, sortie");  // ← AJOUTER
            return;
        }
        
        if (!validerFormulaire()) {
            System.out.println("❌ Formulaire invalide");  // ← AJOUTER
            return;
        }
        
        System.out.println("✅ Formulaire valide, lancement du thread");  // ← AJOUTER
        
        isProcessing = true;
        afficherChargement(true);
        
        new Thread(() -> {
            try {
                System.out.println("🔄 Début de l'inscription...");  // ← AJOUTER
                DemandeInscription demande = construireDemande();
                System.out.println("📝 Demande construite: " + demande.getEmail());  // ← AJOUTER
                authService.inscrire(demande);
                System.out.println("✅ Demande enregistrée !");  // ← AJOUTER
                
                javafx.application.Platform.runLater(() -> {
                    afficherSuccès();
                    afficherChargement(false);
                    isProcessing = false;
                });
                
            } catch (SQLException e) {
                System.err.println("❌ Erreur SQL: " + e.getMessage());  // ← AJOUTER
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    messageLabel.setText("Erreur : " + e.getMessage());
                    messageLabel.setStyle("-fx-text-fill: #f44336;");
                    afficherChargement(false);
                    isProcessing = false;
                });
            } catch (Exception e) {
                System.err.println("❌ Erreur inconnue: " + e.getMessage());  // ← AJOUTER
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    messageLabel.setText("Erreur : " + e.getMessage());
                    messageLabel.setStyle("-fx-text-fill: #f44336;");
                    afficherChargement(false);
                    isProcessing = false;
                });
            }
        }).start();
    }
    
    /**
     * Valide le formulaire d'inscription.
     * @return true si le formulaire est valide
     */
    private boolean validerFormulaire() {
        System.out.println("🔍 Validation du formulaire...");
        
        if (nomField.getText().trim().isEmpty() ||
            prenomField.getText().trim().isEmpty() ||
            emailField.getText().trim().isEmpty()) {
            
            messageLabel.setText("Veuillez remplir tous les champs");
            System.out.println("❌ Champ manquant");  // ← AJOUTER
            return false;
        }
        
        if (!ValidationDonnees.validerEmail(emailField.getText())) {
            messageLabel.setText("Email invalide");
            System.out.println("❌ Email invalide: " + emailField.getText());  // ← AJOUTER
            return false;
        }
        
        RadioButton selected = (RadioButton) roleGroup.getSelectedToggle();
        if (selected == null) {
            System.out.println("❌ Aucun rôle sélectionné");  // ← AJOUTER
            return false;
        }
        
        System.out.println("✅ Rôle sélectionné: " + selected.getText());  // ← AJOUTER
        
        if (selected == etudiantRadio) {
            System.out.println("🔍 Vérification étudiant...");
            if (numeroEtudiantField.getText().trim().isEmpty()) {
                messageLabel.setText("Numéro étudiant requis");
                System.out.println("❌ Numéro étudiant manquant");
                return false;
            }
            if (ufrComboBox.getValue() == null) {
                messageLabel.setText("UFR requise");
                System.out.println("❌ UFR non sélectionnée");
                return false;
            }
            if (classeComboBox.getValue() == null) {
                messageLabel.setText("Classe requise");
                System.out.println("❌ Classe non sélectionnée");
                return false;
            }
            System.out.println("✅ Étudiant OK");
        }
        
        if (selected == enseignantRadio) {
            System.out.println("🔍 Vérification enseignant...");
            if (matriculeField.getText().trim().isEmpty()) {
                messageLabel.setText("Matricule requis");
                System.out.println("❌ Matricule manquant");
                return false;
            }
            if (gradeComboBox.getValue() == null) {
                messageLabel.setText("Grade requis");
                System.out.println("❌ Grade non sélectionné");
                return false;
            }
            System.out.println("✅ Enseignant OK");
        }
        
        System.out.println("✅ Formulaire valide !");
        return true;
    }
    
    /**
     * Construit une demande d'inscription à partir des champs du formulaire.
     * @return la demande d'inscription
     */
    private DemandeInscription construireDemande() {
        RadioButton selected = (RadioButton) roleGroup.getSelectedToggle();
        DemandeInscription demande = new DemandeInscription();
        
        demande.setNom(nomField.getText().trim());
        demande.setPrenom(prenomField.getText().trim());
        demande.setEmail(emailField.getText().trim());
        demande.setStatut("en_attente");
        
        System.out.println("📝 Construction demande pour: " + demande.getEmail());
        
        if (selected == etudiantRadio) {
            demande.setRoleDemande("etudiant");
            demande.setNumeroEtudiant(numeroEtudiantField.getText().trim());
            demande.setUfrId(ufrComboBox.getValue().getId());
            demande.setClasseId(classeComboBox.getValue().getId());
            boolean isResp = responsableClasseCheckBox != null && responsableClasseCheckBox.isSelected();
            demande.setTypeEtudiant(isResp ? "responsable" : "normal");
            System.out.println("   → Étudiant (responsable=" + isResp + ") - UFR: " + demande.getUfrId() + ", Classe: " + demande.getClasseId());
        } else if (selected == enseignantRadio) {
            demande.setRoleDemande("enseignant");
            demande.setMatriculeEnseignant(matriculeField.getText().trim());
            demande.setGrade(gradeComboBox.getValue());
            System.out.println("   → Enseignant - Matricule: " + demande.getMatriculeEnseignant());
        } else {
            demande.setRoleDemande("gestionnaire");
            System.out.println("   → Gestionnaire");
        }
        
        return demande;
    }
    
    /**
     * Affiche un message de succès et retourne à l'écran de login.
     */
    private void afficherSuccès() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Inscription envoyée");
        alert.setHeaderText("Votre demande a été enregistrée");
        alert.setContentText("L'administrateur validera votre compte sous 24-48h.");
        alert.showAndWait();
        retourLogin();
    }
    
    private void retourLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            
            LoginControleur controleur = loader.getController();
            controleur.setPrimaryStage(primaryStage);
            
            Scene scene = new Scene(root);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour login", e);
        }
    }
    
    /**
     * Affiche ou masque l'indicateur de chargement.
     * @param visible true pour afficher, false pour masquer
     */
    private void afficherChargement(boolean visible) {
        loadingIndicator.setVisible(visible);
        inscrireButton.setDisable(visible);
        annulerButton.setDisable(visible);
        if (retourButton != null) {
            retourButton.setDisable(visible);
        }
    }
}