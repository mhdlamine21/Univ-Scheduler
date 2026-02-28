package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.concurrent.Task;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Gestion des matières enseignées.
 */
public class GestionMatieresControleur extends TableauBordControleur {
    
    @FXML private TableView<Matiere> matiereTable;
    @FXML private TableColumn<Matiere, Integer> idColumn;
    @FXML private TableColumn<Matiere, String> nomColumn;
    @FXML private TableColumn<Matiere, String> codeColumn;
    @FXML private TableColumn<Matiere, String> filiereColumn;
    @FXML private TableColumn<Matiere, Integer> volumeColumn;
    @FXML private TableColumn<Matiere, String> descriptionColumn;
    @FXML private TableColumn<Matiere, Void> actionsColumn;
    
    @FXML private TextField nomField;
    @FXML private TextField codeField;
    @FXML private ComboBox<String> filiereCombo;
    @FXML private Spinner<Integer> volumeSpinner;
    @FXML private TextArea descriptionArea;
    @FXML private Label messageLabel;
    
    @FXML private TextField rechercheMatiereField;
    @FXML private ComboBox<String> filtreFiliereCombo;
    @FXML private Button rechercherMatiereButton;
    @FXML private Button reinitialiserMatiereButton;
    @FXML private Label totalMatieresLabel;
    
    @FXML private Button ajouterButton;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button annulerButton;
    @FXML private Button rafraichirButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private MatiereService matiereService;
    private ClasseService classeService;
    private Matiere matiereSelectionne;
    private ObservableList<Matiere> matiereList;
    private List<Matiere> toutesMatieres; 
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.matiereService = new MatiereService();
        this.classeService = new ClasseService();
        this.matiereList = FXCollections.observableArrayList();
        this.toutesMatieres = new ArrayList<>();
        
        try {
            List<String> filieres = classeService.listerFilieres();
            
            List<Matiere> matieres = matiereService.listerToutes();
            for (Matiere m : matieres) {
            }
        } catch (SQLException e) {
        logger.error("Erreur", e);        }
        
        configurerTableau();
        configurerEvenements();
        configurerSpinners();
        configurerFiltres();
        chargerFilieres();
        chargerMatieres();
    }
    
    @Override
    protected void initialiserTableauBord() {
        configurerTableau();
        configurerEvenements();
        configurerSpinners();
        configurerFiltres();
        chargerFilieres();
        chargerMatieres();
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerMatieres();
        chargerFilieres();
    }
    
    private void configurerTableau() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        filiereColumn.setCellValueFactory(new PropertyValueFactory<>("filiere"));
        volumeColumn.setCellValueFactory(new PropertyValueFactory<>("volumeHoraire"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        actionsColumn.setCellFactory(param -> new TableCell<Matiere, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            
            {
                editBtn.setOnAction(event -> {
                    Matiere matiere = getTableView().getItems().get(getIndex());
                    chargerMatierePourEdition(matiere);
                });
                
                deleteBtn.setOnAction(event -> {
                    Matiere matiere = getTableView().getItems().get(getIndex());
                    supprimerMatiere(matiere);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, editBtn, deleteBtn));
                }
            }
        });
        
        matiereTable.setItems(matiereList);
        
        matiereTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    matiereSelectionne = newVal;
                    chargerMatierePourEdition(newVal);
                }
            }
        );
    }
    
    private void configurerFiltres() {
        rechercheMatiereField.setPromptText("Rechercher par nom ou code...");
        
        rechercherMatiereButton.setOnAction(e -> filtrerMatieres());
        reinitialiserMatiereButton.setOnAction(e -> {
            rechercheMatiereField.clear();
            filtreFiliereCombo.setValue(null);
            matiereList.setAll(toutesMatieres);
            mettreAJourTotal();
        });
        
        matiereList.addListener((ListChangeListener<Matiere>) c -> mettreAJourTotal());
    }
    
    /**
     * Filtre les matières par nom ou code.
     */
    private void filtrerMatieres() {
        String recherche = rechercheMatiereField.getText().toLowerCase().trim();
        String filiere = filtreFiliereCombo.getValue();
        
        List<Matiere> resultats = new ArrayList<>();
        
        for (Matiere m : toutesMatieres) {
            boolean correspond = true;
            
            if (!recherche.isEmpty()) {
                if (!m.getNom().toLowerCase().contains(recherche) &&
                    !m.getCode().toLowerCase().contains(recherche)) {
                    correspond = false;
                }
            }
            
            if (correspond && filiere != null && !filiere.isEmpty()) {
                if (!filiere.equals(m.getFiliere())) {
                    correspond = false;
                }
            }
            
            if (correspond) {
                resultats.add(m);
            }
        }
        
        matiereList.setAll(resultats);
    }
    
    private void mettreAJourTotal() {
        totalMatieresLabel.setText(matiereList.size() + " matière(s)");
    }
    
    private void configurerEvenements() {
        ajouterButton.setOnAction(e -> ajouterMatiere());
        modifierButton.setOnAction(e -> modifierMatiere());
        supprimerButton.setOnAction(e -> {
            if (matiereSelectionne != null) {
                supprimerMatiere(matiereSelectionne);
            }
        });
        annulerButton.setOnAction(e -> viderFormulaire());
        rafraichirButton.setOnAction(e -> chargerMatieres());
    }
    
    private void configurerSpinners() {
        volumeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 200, 30));
    }
    
    private void chargerFilieres() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws SQLException {
                return classeService.listerFilieres();
            }
            
            @Override
            protected void succeeded() {
                List<String> filieres = getValue();
                filiereCombo.getItems().setAll(filieres);
                filtreFiliereCombo.getItems().setAll(filieres);
                filtreFiliereCombo.getItems().add(0, "Toutes");
                
                if (!filieres.isEmpty()) {
                    filiereCombo.setValue(filieres.get(0));
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement filières", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Charge la liste des matières depuis la base.
     */
    private void chargerMatieres() {
        chargementIndicator.setVisible(true);
        
        Task<List<Matiere>> task = new Task<>() {
            @Override
            protected List<Matiere> call() throws SQLException {
                return matiereService.listerToutes();
            }
            
            @Override
            protected void succeeded() {
                toutesMatieres = getValue();
                matiereList.setAll(toutesMatieres);
                mettreAJourTotal();
                chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement matières", getException());
                chargementIndicator.setVisible(false);
                messageLabel.setText("❌ Erreur de chargement");
                messageLabel.setStyle("-fx-text-fill: #f44336;");
                messageLabel.setVisible(true);
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerMatierePourEdition(Matiere matiere) {
        nomField.setText(matiere.getNom());
        codeField.setText(matiere.getCode());
        filiereCombo.setValue(matiere.getFiliere());
        volumeSpinner.getValueFactory().setValue(matiere.getVolumeHoraire());
        
        // CORRECTION : Gérer le cas où description est null
        String description = matiere.getDescription();
        descriptionArea.setText(description != null ? description : "");
        
        matiereSelectionne = matiere;
        
        modifierButton.setDisable(false);
        supprimerButton.setDisable(false);
        ajouterButton.setDisable(true);
    }
    
    private void viderFormulaire() {
        nomField.clear();
        codeField.clear();
        filiereCombo.setValue(null);
        volumeSpinner.getValueFactory().setValue(30);
        descriptionArea.clear();
        
        matiereSelectionne = null;
        
        modifierButton.setDisable(true);
        supprimerButton.setDisable(true);
        ajouterButton.setDisable(false);
        
        matiereTable.getSelectionModel().clearSelection();
    }
    
    /**
     * Ajoute une nouvelle matière.
     */
    private void ajouterMatiere() {
        if (!validerFormulaire()) return;
        
        String description = descriptionArea.getText();
        if (description == null) {
            description = "";
        }
        
        Matiere matiere = new Matiere(
            nomField.getText().trim(),
            codeField.getText().trim().toUpperCase(),
            filiereCombo.getValue(),
            volumeSpinner.getValue(),
            description
        );
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                matiereService.ajouter(matiere);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Matière ajoutée");
                messageLabel.setStyle("-fx-text-fill: #4CAF50;");
                messageLabel.setVisible(true);
                viderFormulaire();
                chargerMatieres();
                
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> messageLabel.setVisible(false));
                }).start();
            }
            
            @Override
            protected void failed() {
                String erreur = getException().getMessage();
                logger.error("Erreur ajout matière", getException());
                messageLabel.setText("❌ Erreur ajout: " + (erreur != null ? erreur : "Vérifiez le code unique"));
                messageLabel.setStyle("-fx-text-fill: #f44336;");
                messageLabel.setVisible(true);
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Modifie la matière sélectionnée.
     */
    private void modifierMatiere() {
        if (matiereSelectionne == null) return;
        if (!validerFormulaire()) return;
        
        String description = descriptionArea.getText();
        if (description == null) {
            description = "";
        }
        
        matiereSelectionne.setNom(nomField.getText().trim());
        matiereSelectionne.setCode(codeField.getText().trim().toUpperCase());
        matiereSelectionne.setFiliere(filiereCombo.getValue());
        matiereSelectionne.setVolumeHoraire(volumeSpinner.getValue());
        matiereSelectionne.setDescription(description);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                matiereService.modifier(matiereSelectionne);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Matière modifiée");
                messageLabel.setStyle("-fx-text-fill: #4CAF50;");
                messageLabel.setVisible(true);
                viderFormulaire();
                chargerMatieres();
                
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> messageLabel.setVisible(false));
                }).start();
            }
            
            @Override
            protected void failed() {
                String erreur = getException().getMessage();
                messageLabel.setText("❌ Erreur modification: " + (erreur != null ? erreur : "Vérifiez le code unique"));
                messageLabel.setStyle("-fx-text-fill: #f44336;");
                messageLabel.setVisible(true);
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Supprime une matière après confirmation.
     * @param matiere la matière à supprimer
     */
    private void supprimerMatiere(Matiere matiere) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la matière ?");
        alert.setContentText("Supprimer " + matiere.getNom() + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        matiereService.supprimer(matiere.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        messageLabel.setText("✅ Matière supprimée");
                        messageLabel.setStyle("-fx-text-fill: #4CAF50;");
                        messageLabel.setVisible(true);
                        viderFormulaire();
                        chargerMatieres();
                        
                        new Thread(() -> {
                            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                            javafx.application.Platform.runLater(() -> messageLabel.setVisible(false));
                        }).start();
                    }
                    
                    @Override
                    protected void failed() {
                        messageLabel.setText("❌ Erreur suppression");
                        messageLabel.setStyle("-fx-text-fill: #f44336;");
                        messageLabel.setVisible(true);
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    private boolean validerFormulaire() {
        if (nomField.getText().trim().isEmpty()) {
            messageLabel.setText("Le nom est requis");
            messageLabel.setStyle("-fx-text-fill: #f44336;");
            messageLabel.setVisible(true);
            return false;
        }
        if (codeField.getText().trim().isEmpty()) {
            messageLabel.setText("Le code est requis");
            messageLabel.setStyle("-fx-text-fill: #f44336;");
            messageLabel.setVisible(true);
            return false;
        }
        if (filiereCombo.getValue() == null) {
            messageLabel.setText("La filière est requise");
            messageLabel.setStyle("-fx-text-fill: #f44336;");
            messageLabel.setVisible(true);
            return false;
        }
        return true;
    }
    
    @FXML
	protected void handleRetour() {
        try {
            Stage stage = (Stage) ((Node) messageLabel).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordGestionnaire.fxml"));
            Parent root = loader.load();
            
            TableauBordGestionnaireControleur controleur = loader.getController();
            if (controleur != null && utilisateurConnecte != null) {
                controleur.initialiserAvecUtilisateur(utilisateurConnecte, stage);
            }
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner");
        }
    }
}