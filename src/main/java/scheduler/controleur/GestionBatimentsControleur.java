package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.collections.*;
import javafx.concurrent.Task;
import java.sql.SQLException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import scheduler.util.IconHelper;
import scheduler.util.VisualiseurPhoto;

/**
 * Contrôleur pour l'écran GestionBatiments.
 */
public class GestionBatimentsControleur extends TableauBordControleur {
    
    @FXML private TableView<Batiment> batimentTable;
    @FXML private TableColumn<Batiment, Integer> idColumn;
    @FXML private TableColumn<Batiment, String> nomColumn;
    @FXML private TableColumn<Batiment, String> localisationColumn;
    @FXML private TableColumn<Batiment, Integer> etagesColumn;
    @FXML private TableColumn<Batiment, String> ufrColumn;
    @FXML private TableColumn<Batiment, String> statutColumn;
    @FXML private TableColumn<Batiment, Void> actionsColumn;
    
    @FXML private ComboBox<Ufr> ufrCombo;
    @FXML private TextField nomField;
    @FXML private TextField localisationField;
    @FXML private Spinner<Integer> etagesSpinner;
    @FXML private TextArea motifArea;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Label messageLabel;
    @FXML private Label totalBatimentsLabel;
    @FXML private Button retourButton;
    
    // Filtres
    @FXML private TextField rechercheField;
    @FXML private ComboBox<Ufr> filtreUfrCombo;
    @FXML private ComboBox<String> filtreStatutCombo;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    
    @FXML private Button ajouterButton;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button rendreIndisponibleButton;
    @FXML private Button rendreDisponibleButton;
    @FXML private Button annulerButton;
    @FXML private Button rafraichirButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private BatimentService batimentService;
    private UfrService ufrService;
    private Batiment batimentSelectionne;
    private ObservableList<Batiment> batimentList;
    private List<Batiment> tousBatiments;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.batimentService = new BatimentService();
        this.ufrService = new UfrService();
        this.batimentList = FXCollections.observableArrayList();
        this.tousBatiments = new ArrayList<>();
        
        configurerTableau();
        configurerEvenements();
        configurerSpinners();
        configurerFiltres();
        chargerUfr();
        chargerBatiments();
        verifierEtMettreAJourStatut(); // AJOUTÉ : vérification auto des dates passées
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetourGestion());
        }
    }
    
    @Override
    protected void initialiserTableauBord() {
        // Déjà fait dans initialize()
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerBatiments();
        verifierEtMettreAJourStatut(); // AJOUTÉ
    }
    
    /**
     * Vérifie automatiquement les bâtiments dont la période d'indisponibilité est passée.
     */
    private void verifierEtMettreAJourStatut() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                List<Batiment> batiments = batimentService.listerTous();
                LocalDate aujourdhui = LocalDate.now();
                
                for (Batiment b : batiments) {
                    if ("indisponible".equals(b.getStatut()) && b.getDateFinIndisponibilite() != null) {
                        try {
                            LocalDate dateFin = LocalDate.parse(b.getDateFinIndisponibilite());
                            if (dateFin.isBefore(aujourdhui)) {
                                batimentService.rendreDisponible(b.getId());
                            }
                        } catch (Exception e) {
                            // Ignorer les dates mal formées
                        }
                    }
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                chargerBatiments();
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur vérification statut bâtiments", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private void configurerTableau() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        localisationColumn.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        etagesColumn.setCellValueFactory(new PropertyValueFactory<>("nbEtages"));
        
        idColumn.setStyle("-fx-alignment: CENTER;");
        nomColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        localisationColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        etagesColumn.setStyle("-fx-alignment: CENTER;");
        
        ufrColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomUfr(cellData.getValue().getUfrId())
            )
        );
        ufrColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        statutColumn.setStyle("-fx-alignment: CENTER;");
        
        statutColumn.setCellFactory(column -> new TableCell<Batiment, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                } else {
                    setText(statut);
                    if (statut.equals("indisponible")) {
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });
        
        actionsColumn.setCellFactory(param -> new TableCell<Batiment, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Batiment b = getTableView().getItems().get(getIndex());
                    
                    Button viewBtn = IconHelper.createIconButton("view.png", "Voir la photo et détails", "#F5EFEB");
                    Button editBtn = IconHelper.createIconButton("edit.png", "Modifier le bâtiment", "#F5EFEB");
                    Button deleteBtn = IconHelper.createIconButton("delete.png", "Supprimer le bâtiment", "#FBEBEB");
                    
                    boolean estDispo = "disponible".equalsIgnoreCase(b.getStatut());
                    Button indispoBtn = IconHelper.createIconButton(
                        estDispo ? "lock.png" : "unlock.png",
                        estDispo ? "Marquer indisponible" : "Rendre disponible",
                        estDispo ? "#FBEBEB" : "#EDF7ED"
                    );
                    
                    viewBtn.setOnAction(event -> {
                        VisualiseurPhoto.afficher("Bâtiment " + b.getNom(), 
                            "Localisation : " + b.getLocalisation() + " • " + b.getNbEtages() + " étages", 
                            b.getPhotoUrl(), "Bâtiment universitaire");
                    });
                    editBtn.setOnAction(event -> chargerBatimentPourEdition(b));
                    deleteBtn.setOnAction(event -> supprimerBatiment(b));
                    indispoBtn.setOnAction(e -> {
                        if (estDispo) {
                            ouvrirDialogueIndisponibilite(b);
                        } else {
                            rendreDisponible(b);
                        }
                    });
                    
                    HBox box = new HBox(5, viewBtn, editBtn, indispoBtn, deleteBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        
        batimentTable.setItems(batimentList);
        batimentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        batimentTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    batimentSelectionne = newVal;
                    chargerBatimentPourEdition(newVal);
                }
            }
        );
        
        batimentTable.setPrefHeight(400);
        batimentTable.setMinHeight(350);
    }
    
    private void configurerFiltres() {
        filtreStatutCombo.getItems().addAll("Tous", "disponible", "indisponible");
        filtreStatutCombo.setValue("Tous");
        
        rechercheField.setPromptText("Rechercher par nom...");
        
        rechercherButton.setOnAction(e -> filtrerBatiments());
        reinitialiserButton.setOnAction(e -> {
            rechercheField.clear();
            filtreUfrCombo.setValue(null);
            filtreStatutCombo.setValue("Tous");
            batimentList.setAll(tousBatiments);
        });
    }
    
    private void filtrerBatiments() {
        List<Batiment> filtrees = new ArrayList<>(tousBatiments);
        
        String recherche = rechercheField.getText().toLowerCase().trim();
        if (!recherche.isEmpty()) {
            filtrees.removeIf(b -> !b.getNom().toLowerCase().contains(recherche) && 
                                   !b.getLocalisation().toLowerCase().contains(recherche));
        }
        
        String statut = filtreStatutCombo.getValue();
        if (!"Tous".equals(statut)) {
            filtrees.removeIf(b -> !b.getStatut().equals(statut));
        }
        
        Ufr ufr = filtreUfrCombo.getValue();
        if (ufr != null) {
            filtrees.removeIf(b -> b.getUfrId() != ufr.getId());
        }
        
        batimentList.setAll(filtrees);
    }
    
    private void configurerEvenements() {
        ajouterButton.setOnAction(e -> ajouterBatiment());
        modifierButton.setOnAction(e -> modifierBatiment());
        supprimerButton.setOnAction(e -> {
            if (batimentSelectionne != null) {
                supprimerBatiment(batimentSelectionne);
            }
        });
        annulerButton.setOnAction(e -> viderFormulaire());
        rafraichirButton.setOnAction(e -> chargerBatiments());
    }
    
    private void configurerSpinners() {
        etagesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
    }
    
    private void chargerUfr() {
        Task<List<Ufr>> task = new Task<>() {
            @Override
            protected List<Ufr> call() throws SQLException {
                return ufrService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                List<Ufr> ufrs = getValue();
                
                // ✅ CORRECTION : Ajouter CellFactory pour afficher le nom
                ufrCombo.setCellFactory(lv -> new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom());
                    }
                });
                
                // ✅ CORRECTION : Ajouter ButtonCell pour l'affichage sélectionné
                ufrCombo.setButtonCell(new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Sélectionner une UFR" : item.getNom());
                    }
                });
                
                ufrCombo.getItems().setAll(ufrs);
                
                // Pour le filtre aussi
                filtreUfrCombo.setCellFactory(lv -> new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Toutes les UFR");
                        } else {
                            setText(item.getNom());
                        }
                    }
                });
                
                filtreUfrCombo.setButtonCell(new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Toutes les UFR" : item.getNom());
                    }
                });
                
                filtreUfrCombo.getItems().clear();
                filtreUfrCombo.getItems().add(null);
                filtreUfrCombo.getItems().addAll(ufrs);
            }
        };
        new Thread(task).start();
    }
    
    private void chargerBatiments() {
        Task<List<Batiment>> task = new Task<>() {
            @Override
            protected List<Batiment> call() throws SQLException {
                return batimentService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                tousBatiments = getValue();
                batimentList.setAll(tousBatiments);
                totalBatimentsLabel.setText(tousBatiments.size() + " bâtiment(s)");
                chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement bâtiments", getException());
                messageLabel.setText("Erreur de chargement");
                chargementIndicator.setVisible(false);
            }
        };
        
        chargementIndicator.setVisible(true);
        new Thread(task).start();
    }
    
    private String getNomUfr(int ufrId) {
        for (Ufr ufr : ufrCombo.getItems()) {
            if (ufr.getId() == ufrId) {
                return ufr.getNom();
            }
        }
        return "Inconnue";
    }
    
    private void chargerBatimentPourEdition(Batiment batiment) {
        nomField.setText(batiment.getNom());
        localisationField.setText(batiment.getLocalisation());
        etagesSpinner.getValueFactory().setValue(batiment.getNbEtages());
        
        for (Ufr ufr : ufrCombo.getItems()) {
            if (ufr.getId() == batiment.getUfrId()) {
                ufrCombo.setValue(ufr);
                break;
            }
        }
        
        batimentSelectionne = batiment;
        
        modifierButton.setDisable(false);
        supprimerButton.setDisable(false);
        ajouterButton.setDisable(true);
    }
    
    private void viderFormulaire() {
        nomField.clear();
        localisationField.clear();
        etagesSpinner.getValueFactory().setValue(1);
        ufrCombo.setValue(null);
        motifArea.clear();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        
        batimentSelectionne = null;
        
        modifierButton.setDisable(true);
        supprimerButton.setDisable(true);
        ajouterButton.setDisable(false);
        
        batimentTable.getSelectionModel().clearSelection();
    }
    
    /**
     * Ajoute un nouveau bâtiment.
     */
    private void ajouterBatiment() {
        if (!validerFormulaireBase()) return;
        
        Batiment batiment = new Batiment(
            nomField.getText().trim(),
            localisationField.getText().trim(),
            etagesSpinner.getValue(),
            ufrCombo.getValue().getId()
        );
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                batimentService.ajouter(batiment);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Bâtiment ajouté");
                viderFormulaire();
                chargerBatiments();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur ajout");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Modifie le bâtiment sélectionné.
     */
    private void modifierBatiment() {
        if (batimentSelectionne == null) return;
        if (!validerFormulaireBase()) return;
        
        batimentSelectionne.setNom(nomField.getText().trim());
        batimentSelectionne.setLocalisation(localisationField.getText().trim());
        batimentSelectionne.setNbEtages(etagesSpinner.getValue());
        batimentSelectionne.setUfrId(ufrCombo.getValue().getId());
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                batimentService.modifier(batimentSelectionne);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Bâtiment modifié");
                viderFormulaire();
                chargerBatiments();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur modification");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Supprime un bâtiment après confirmation.
     * @param batiment le bâtiment à supprimer
     */
    private void supprimerBatiment(Batiment batiment) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le bâtiment ?");
        alert.setContentText("Supprimer " + batiment.getNom() + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        batimentService.supprimer(batiment.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        messageLabel.setText("✅ Bâtiment supprimé");
                        viderFormulaire();
                        chargerBatiments();
                    }
                    
                    @Override
                    protected void failed() {
                        messageLabel.setText("❌ Erreur suppression");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Ouvre le dialogue pour rendre un bâtiment indisponible.
     * @param batiment le bâtiment à rendre indisponible
     */
    private void ouvrirDialogueIndisponibilite(Batiment batiment) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Rendre indisponible");
        dialog.setHeaderText("Rendre " + batiment.getNom() + " indisponible");
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        TextArea motifArea = new TextArea();
        motifArea.setPromptText("Motif de l'indisponibilité");
        DatePicker debutPicker = new DatePicker(LocalDate.now());
        DatePicker finPicker = new DatePicker(LocalDate.now().plusMonths(1));
        
        grid.add(new Label("Motif:"), 0, 0);
        grid.add(motifArea, 1, 0);
        grid.add(new Label("Début:"), 0, 1);
        grid.add(debutPicker, 1, 1);
        grid.add(new Label("Fin:"), 0, 2);
        grid.add(finPicker, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !motifArea.getText().trim().isEmpty()) {
                rendreIndisponible(batiment, motifArea.getText(), 
                    debutPicker.getValue().toString(), finPicker.getValue().toString());
            }
        });
    }
    
    /**
     * Rend un bâtiment indisponible.
     * @param batiment le bâtiment concerné
     * @param motif le motif de l'indisponibilité
     * @param debut date de début
     * @param fin date de fin
     */
    private void rendreIndisponible(Batiment batiment, String motif, String debut, String fin) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                batimentService.rendreIndisponible(batiment.getId(), motif, debut, fin);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Bâtiment indisponible");
                chargerBatiments();
                viderFormulaire();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Rend un bâtiment disponible.
     * @param batiment le bâtiment à rendre disponible
     */
    private void rendreDisponible(Batiment batiment) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                batimentService.rendreDisponible(batiment.getId());
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Bâtiment disponible");
                chargerBatiments();
                viderFormulaire();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur");
            }
        };
        
        new Thread(task).start();
    }
    
    private boolean validerFormulaireBase() {
        if (nomField.getText().trim().isEmpty()) {
            messageLabel.setText("❌ Nom requis");
            return false;
        }
        if (ufrCombo.getValue() == null) {
            messageLabel.setText("❌ UFR requise");
            return false;
        }
        return true;
    }
    
    private void handleRetourGestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordAdmin.fxml"));
            Parent root = loader.load();
            
            TableauBordAdminControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            controleur.selectionnerOnglet(1);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour gestion", e);
            afficherErreur("Impossible de retourner au menu Gestion");
        }
    }
}