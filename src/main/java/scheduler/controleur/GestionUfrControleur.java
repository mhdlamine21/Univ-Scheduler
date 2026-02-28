package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Contrôleur pour l'écran GestionUfr avec support photo et code.
 */
public class GestionUfrControleur extends TableauBordControleur {
    
    @FXML private TableView<Ufr> ufrTable;
    @FXML private TableColumn<Ufr, String> photoColumn;
    @FXML private TableColumn<Ufr, Integer> idColumn;
    @FXML private TableColumn<Ufr, String> codeColumn;
    @FXML private TableColumn<Ufr, String> nomColumn;
    @FXML private TableColumn<Ufr, String> localisationColumn;
    @FXML private TableColumn<Ufr, String> descriptionColumn;
    @FXML private TableColumn<Ufr, String> dateColumn;
    @FXML private TableColumn<Ufr, Void> actionsColumn;
    
    @FXML private TextField codeField;
    @FXML private TextField nomField;
    @FXML private TextField localisationField;
    @FXML private TextArea descriptionArea;
    @FXML private Label messageLabel;
    
    @FXML private ImageView photoPreview;
    @FXML private Button choisirPhotoButton;
    @FXML private Button supprimerPhotoButton;
    
    @FXML private Button ajouterButton;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button annulerButton;
    @FXML private Button rafraichirButton;
    @FXML private Button retourButton;  
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private UfrService ufrService;
    private Ufr ufrSelectionne;
    private ObservableList<Ufr> ufrList;
    private String cheminPhotoActuelle;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.ufrService = new UfrService();
        this.ufrList = FXCollections.observableArrayList();
        
        configurerTableau();
        configurerEvenements();
        chargerUfr();
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetour());
        }
    }
    
    @Override
    protected void initialiserTableauBord() {
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerUfr();
    }
    
    private void configurerTableau() {
        if (idColumn != null) idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (codeColumn != null) codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        if (nomColumn != null) nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (localisationColumn != null) localisationColumn.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        if (descriptionColumn != null) descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        if (dateColumn != null) {
            dateColumn.setCellValueFactory(cellData -> 
                javafx.beans.binding.Bindings.createStringBinding(
                    () -> cellData.getValue().getDateCreation() != null ?
                        cellData.getValue().getDateCreation().toLocalDate().toString() : ""
                )
            );
        }
        
        if (photoColumn != null) {
            photoColumn.setCellFactory(col -> new TableCell<Ufr, String>() {
                private final ImageView imgView = new ImageView();
                {
                    imgView.setFitWidth(40);
                    imgView.setFitHeight(30);
                    imgView.setPreserveRatio(true);
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        Ufr ufr = getTableRow().getItem();
                        String photo = ufr.getPhotoUrl();
                        if (photo != null && !photo.isBlank()) {
                            Image img = ImageUploadService.chargerImage(photo, 40, 30);
                            if (img != null) {
                                imgView.setImage(img);
                                setGraphic(imgView);
                                return;
                            }
                        }
                        Label placeholder = new Label("🏛️");
                        setGraphic(placeholder);
                    }
                }
            });
        }
        
        if (actionsColumn != null) {
            actionsColumn.setCellFactory(param -> new TableCell<Ufr, Void>() {
                private final Button editBtn = new Button("✏️");
                private final Button deleteBtn = new Button("🗑️");
                
                {
                    editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    
                    editBtn.setOnAction(event -> {
                        Ufr ufr = getTableView().getItems().get(getIndex());
                        chargerUfrPourEdition(ufr);
                    });
                    
                    deleteBtn.setOnAction(event -> {
                        Ufr ufr = getTableView().getItems().get(getIndex());
                        supprimerUfr(ufr);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(new HBox(8, editBtn, deleteBtn));
                    }
                }
            });
        }
        
        ufrTable.setItems(ufrList);
        
        ufrTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    ufrSelectionne = newVal;
                    chargerUfrPourEdition(newVal);
                }
            }
        );
    }
    
    private void configurerEvenements() {
        if (ajouterButton != null) ajouterButton.setOnAction(e -> ajouterUfr());
        if (modifierButton != null) modifierButton.setOnAction(e -> modifierUfr());
        if (supprimerButton != null) {
            supprimerButton.setOnAction(e -> {
                if (ufrSelectionne != null) {
                    supprimerUfr(ufrSelectionne);
                }
            });
        }
        if (annulerButton != null) annulerButton.setOnAction(e -> viderFormulaire());
        if (rafraichirButton != null) rafraichirButton.setOnAction(e -> chargerUfr());
        
        if (choisirPhotoButton != null) {
            choisirPhotoButton.setOnAction(e -> choisirPhoto());
        }
        if (supprimerPhotoButton != null) {
            supprimerPhotoButton.setOnAction(e -> supprimerPhoto());
        }
    }
    
    private void choisirPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir le logo ou la photo de l'UFR");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images (*.png, *.jpg, *.jpeg, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File fichier = fileChooser.showOpenDialog(ufrTable.getScene().getWindow());
        if (fichier != null) {
            try {
                String chemin = ImageUploadService.sauvegarderPhotoUfr(fichier);
                cheminPhotoActuelle = chemin;
                Image img = ImageUploadService.chargerImage(chemin, 110, 90);
                if (photoPreview != null && img != null) {
                    photoPreview.setImage(img);
                }
                if (supprimerPhotoButton != null) {
                    supprimerPhotoButton.setVisible(true);
                }
            } catch (IOException ex) {
                logger.error("Erreur enregistrement photo UFR", ex);
                afficherMessage("Erreur d'import de photo", true);
            }
        }
    }
    
    private void supprimerPhoto() {
        cheminPhotoActuelle = null;
        if (photoPreview != null) {
            Image defLogo = ImageUploadService.chargerImage("/images/logo.png", 110, 90);
            photoPreview.setImage(defLogo);
        }
        if (supprimerPhotoButton != null) {
            supprimerPhotoButton.setVisible(false);
        }
    }
    
    private void chargerUfr() {
        Task<List<Ufr>> task = new Task<>() {
            @Override
            protected List<Ufr> call() throws SQLException {
                return ufrService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                ufrList.setAll(getValue());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement UFR", getException());
                afficherMessage("Erreur de chargement des UFR", true);
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
        };
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        new Thread(task).start();
    }
    
    private void chargerUfrPourEdition(Ufr ufr) {
        if (nomField != null) nomField.setText(ufr.getNom());
        if (codeField != null) codeField.setText(ufr.getCode() != null ? ufr.getCode() : "");
        if (localisationField != null) localisationField.setText(ufr.getLocalisation() != null ? ufr.getLocalisation() : "");
        if (descriptionArea != null) descriptionArea.setText(ufr.getDescription());
        
        cheminPhotoActuelle = ufr.getPhotoUrl();
        if (photoPreview != null) {
            if (cheminPhotoActuelle != null && !cheminPhotoActuelle.isBlank()) {
                Image img = ImageUploadService.chargerImage(cheminPhotoActuelle, 110, 90);
                if (img != null) photoPreview.setImage(img);
                if (supprimerPhotoButton != null) supprimerPhotoButton.setVisible(true);
            } else {
                Image defLogo = ImageUploadService.chargerImage("/images/logo.png", 110, 90);
                photoPreview.setImage(defLogo);
                if (supprimerPhotoButton != null) supprimerPhotoButton.setVisible(false);
            }
        }
        
        ufrSelectionne = ufr;
        if (modifierButton != null) modifierButton.setDisable(false);
        if (supprimerButton != null) supprimerButton.setDisable(false);
        if (ajouterButton != null) ajouterButton.setDisable(true);
    }
    
    private void viderFormulaire() {
        if (nomField != null) nomField.clear();
        if (codeField != null) codeField.clear();
        if (localisationField != null) localisationField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        supprimerPhoto();
        ufrSelectionne = null;
        
        if (modifierButton != null) modifierButton.setDisable(true);
        if (supprimerButton != null) supprimerButton.setDisable(true);
        if (ajouterButton != null) ajouterButton.setDisable(false);
        
        ufrTable.getSelectionModel().clearSelection();
        if (messageLabel != null) messageLabel.setVisible(false);
    }
    
    private void ajouterUfr() {
        if (!validerFormulaire()) return;
        
        String nom = nomField.getText().trim();
        String desc = descriptionArea.getText().trim();
        String code = codeField != null ? codeField.getText().trim() : "";
        String localisation = localisationField != null ? localisationField.getText().trim() : "";
        
        Ufr ufr = new Ufr(nom, desc);
        ufr.setCode(code);
        ufr.setLocalisation(localisation);
        ufr.setPhotoUrl(cheminPhotoActuelle);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                ufrService.ajouter(ufr);
                return null;
            }
            
            @Override
            protected void succeeded() {
                afficherMessage("UFR ajoutée avec succès", false);
                viderFormulaire();
                chargerUfr();
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur ajout UFR", getException());
                afficherMessage("Erreur lors de l'ajout de l'UFR", true);
            }
        };
        
        new Thread(task).start();
    }
    
    private void modifierUfr() {
        if (ufrSelectionne == null) return;
        if (!validerFormulaire()) return;
        
        ufrSelectionne.setNom(nomField.getText().trim());
        ufrSelectionne.setDescription(descriptionArea.getText().trim());
        if (codeField != null) ufrSelectionne.setCode(codeField.getText().trim());
        if (localisationField != null) ufrSelectionne.setLocalisation(localisationField.getText().trim());
        ufrSelectionne.setPhotoUrl(cheminPhotoActuelle);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                ufrService.modifier(ufrSelectionne);
                return null;
            }
            
            @Override
            protected void succeeded() {
                afficherMessage("UFR modifiée avec succès", false);
                viderFormulaire();
                chargerUfr();
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur modification UFR", getException());
                afficherMessage("Erreur lors de la modification", true);
            }
        };
        
        new Thread(task).start();
    }
    
    private void supprimerUfr(Ufr ufr) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'UFR ?");
        alert.setContentText("Voulez-vous vraiment supprimer " + ufr.getNom() + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        ufrService.supprimer(ufr.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherMessage("UFR supprimée", false);
                        viderFormulaire();
                        chargerUfr();
                    }
                    
                    @Override
                    protected void failed() {
                        afficherMessage("Erreur lors de la suppression", true);
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    private boolean validerFormulaire() {
        if (nomField == null || nomField.getText().trim().isEmpty()) {
            afficherMessage("Le nom de l'UFR est obligatoire", true);
            return false;
        }
        return true;
    }
    
    private void afficherMessage(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle(isError ? "-fx-text-fill: #9E2A2B; -fx-font-weight: bold;" : "-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
            messageLabel.setVisible(true);
        }
    }
    
    @Override
    protected void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordAdmin.fxml"));
            Parent root = loader.load();
            
            TableauBordAdminControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner au tableau de bord");
        }
    }
}