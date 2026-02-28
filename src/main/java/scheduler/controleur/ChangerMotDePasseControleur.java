package scheduler.controleur;

import scheduler.modele.Utilisateur;
import scheduler.service.UtilisateurService;
import scheduler.util.SessionUtilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import java.sql.SQLException;
import java.io.IOException;

/**
 * Contrôleur pour l'écran ChangerMotDePasse.
 
 */
public class ChangerMotDePasseControleur extends TableauBordControleur {
    
    @FXML private PasswordField ancienMotDePasseField;
    @FXML private PasswordField nouveauMotDePasseField;
    @FXML private PasswordField confirmerMotDePasseField;
    
    @FXML private Label ancienMessageLabel;
    @FXML private Label nouveauMessageLabel;
    @FXML private Label confirmerMessageLabel;
    @FXML private Label messageLabel;
    
    @FXML private ProgressIndicator strengthIndicator;
    @FXML private Label strengthLabel;
    
    @FXML private Button changerButton;
    @FXML private Button annulerButton;
    
    private UtilisateurService utilisateurService;
    
    @Override
    public void initialize() {  // public (comme dans la classe mère)
        super.initialize();
        
        this.utilisateurService = new UtilisateurService();
        
        configurerValidateurs();
        configurerEvenements();
    }
    
    @Override
    protected void initialiserTableauBord() {  // protected (comme dans la classe mère)
        // Rien à initialiser spécifiquement
    }
    
    @Override
    protected void rafraichirDonnees() {  // protected (comme dans la classe mère)
        // Rien à rafraîchir
    }
    
    /**
     * Configure les validateurs des champs.
     */
    private void configurerValidateurs() {
        ancienMotDePasseField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) ancienMessageLabel.setText("");
        });
        
        nouveauMotDePasseField.textProperty().addListener((obs, oldVal, newVal) -> {
            evaluerForceMotDePasse(newVal);
            
            if (newVal.length() < 8) {
                nouveauMessageLabel.setText("Minimum 8 caractères");
                nouveauMessageLabel.setStyle("-fx-text-fill: #f44336;");
            } else if (!newVal.matches(".*[A-Z].*")) {
                nouveauMessageLabel.setText("Au moins une majuscule");
                nouveauMessageLabel.setStyle("-fx-text-fill: #f44336;");
            } else if (!newVal.matches(".*[a-z].*")) {
                nouveauMessageLabel.setText("Au moins une minuscule");
                nouveauMessageLabel.setStyle("-fx-text-fill: #f44336;");
            } else if (!newVal.matches(".*\\d.*")) {
                nouveauMessageLabel.setText("Au moins un chiffre");
                nouveauMessageLabel.setStyle("-fx-text-fill: #f44336;");
            } else {
                nouveauMessageLabel.setText("✓ Mot de passe valide");
                nouveauMessageLabel.setStyle("-fx-text-fill: #4CAF50;");
            }
        });
        
        confirmerMotDePasseField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.equals(nouveauMotDePasseField.getText())) {
                confirmerMessageLabel.setText("✓ Correspond");
                confirmerMessageLabel.setStyle("-fx-text-fill: #4CAF50;");
            } else {
                confirmerMessageLabel.setText("✗ Ne correspond pas");
                confirmerMessageLabel.setStyle("-fx-text-fill: #f44336;");
            }
        });
    }
    
    private void configurerEvenements() {
        changerButton.setOnAction(e -> changerMotDePasse());
        annulerButton.setOnAction(e -> retourProfil());
    }
    
    /**
     * Évalue la force du mot de passe.
     * @param password le mot de passe à évaluer
     */
    private void evaluerForceMotDePasse(String password) {
        int force = 0;
        
        if (password.length() >= 8) force++;
        if (password.matches(".*[A-Z].*")) force++;
        if (password.matches(".*[a-z].*")) force++;
        if (password.matches(".*\\d.*")) force++;
        
        strengthIndicator.setProgress(force / 4.0);
        
        if (force <= 2) {
            strengthLabel.setText("Faible");
            strengthLabel.setStyle("-fx-text-fill: #f44336;");
        } else if (force <= 3) {
            strengthLabel.setText("Moyen");
            strengthLabel.setStyle("-fx-text-fill: #FF9800;");
        } else {
            strengthLabel.setText("Fort");
            strengthLabel.setStyle("-fx-text-fill: #4CAF50;");
        }
    }
    
    /**
     * Change le mot de passe après validation.
     */
    protected void changerMotDePasse() {
        String ancien = ancienMotDePasseField.getText();
        String nouveau = nouveauMotDePasseField.getText();
        String confirmer = confirmerMotDePasseField.getText();
        
        if (ancien.isEmpty()) {
            ancienMessageLabel.setText("Mot de passe requis");
            return;
        }
        
        if (!utilisateurConnecte.getMotDePasse().equals(ancien)) {
            ancienMessageLabel.setText("Mot de passe incorrect");
            return;
        }
        
        if (nouveau.isEmpty()) {
            nouveauMessageLabel.setText("Nouveau mot de passe requis");
            return;
        }
        
        if (nouveau.length() < 8) {
            nouveauMessageLabel.setText("Minimum 8 caractères");
            return;
        }
        
        if (!nouveau.matches(".*[A-Z].*")) {
            nouveauMessageLabel.setText("Au moins une majuscule");
            return;
        }
        
        if (!nouveau.matches(".*[a-z].*")) {
            nouveauMessageLabel.setText("Au moins une minuscule");
            return;
        }
        
        if (!nouveau.matches(".*\\d.*")) {
            nouveauMessageLabel.setText("Au moins un chiffre");
            return;
        }
        
        if (!nouveau.equals(confirmer)) {
            confirmerMessageLabel.setText("Les mots de passe ne correspondent pas");
            return;
        }
        
        if (nouveau.equals(ancien)) {
            messageLabel.setText("Le nouveau mot de passe doit être différent");
            return;
        }
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                utilisateurConnecte.setMotDePasse(nouveau);
                utilisateurService.modifier(utilisateurConnecte);
                return null;
            }
            
            @Override
            protected void succeeded() {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText("Mot de passe modifié");
                alert.showAndWait();
                
                retourProfil();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("Erreur changement");
                messageLabel.setStyle("-fx-text-fill: #f44336;");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Retourne à l'écran du profil.
     */
    private void retourProfil() {
        Stage currentStage = getStageFromScene();
        if (currentStage != null) {
            currentStage.close();
        } else if (primaryStage != null) {
            primaryStage.close();
        }
    }
}