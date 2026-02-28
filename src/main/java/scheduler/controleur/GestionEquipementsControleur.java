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
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import java.sql.SQLException;
import java.io.IOException;
import java.util.*;
import scheduler.util.IconHelper;

/**
 * Contrôleur pour l'écran GestionEquipements.
 */
public class GestionEquipementsControleur extends TableauBordControleur {
    
    @FXML private TableView<Equipement> equipementTable;
    @FXML private TableColumn<Equipement, Integer> idColumn;
    @FXML private TableColumn<Equipement, String> nomColumn;
    @FXML private TableColumn<Equipement, String> descriptionColumn;
    @FXML private TableColumn<Equipement, Integer> quantiteColumn;
    @FXML private TableColumn<Equipement, Integer> quantiteUtiliseeColumn; // AJOUTÉ
    @FXML private TableColumn<Equipement, Integer> quantiteDisponibleColumn; // AJOUTÉ
    @FXML private TableColumn<Equipement, Void> actionsColumn;
    
    @FXML private TextField nomField;
    @FXML private TextArea descriptionArea;
    @FXML private Spinner<Integer> quantiteSpinner;
    @FXML private Label messageLabel;
    @FXML private Label totalEquipementsLabel;
    @FXML private Label totalUtilisesLabel; 
    @FXML private Label totalDisponiblesLabel; 
    @FXML private Button retourButton;
    
    @FXML private TextField rechercheField;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    
    @FXML private Button ajouterButton;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button annulerButton;
    @FXML private Button rafraichirButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private EquipementService equipementService;
    private SalleService salleService; 
    private Equipement equipementSelectionne;
    private ObservableList<Equipement> equipementList;
    private List<Equipement> tousEquipements;
    private Map<Integer, Integer> quantitesUtilisees; 
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.equipementService = new EquipementService();
        this.salleService = new SalleService(); // AJOUTÉ
        this.equipementList = FXCollections.observableArrayList();
        this.tousEquipements = new ArrayList<>();
        this.quantitesUtilisees = new HashMap<>();
        
        configurerTableau();
        configurerEvenements();
        configurerFiltres();
        configurerSpinners();
        chargerEquipements();
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetourGestion());
        }
    }
    
    @Override
    protected void initialiserTableauBord() {
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerEquipements();
    }
    
    private void configurerTableau() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        quantiteColumn.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        
        quantiteUtiliseeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createIntegerBinding(
                () -> getQuantiteUtilisee(cellData.getValue().getId())
            ).asObject()
        );
        
        quantiteDisponibleColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createIntegerBinding(
                () -> cellData.getValue().getQuantite() - getQuantiteUtilisee(cellData.getValue().getId())
            ).asObject()
        );
        
        idColumn.setStyle("-fx-alignment: CENTER;");
        nomColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        descriptionColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        quantiteColumn.setStyle("-fx-alignment: CENTER;");
        quantiteUtiliseeColumn.setStyle("-fx-alignment: CENTER;");
        quantiteDisponibleColumn.setStyle("-fx-alignment: CENTER;");
        
        idColumn.setPrefWidth(50);
        nomColumn.setPrefWidth(150);
        descriptionColumn.setPrefWidth(250);
        quantiteColumn.setPrefWidth(100);
        quantiteUtiliseeColumn.setPrefWidth(100);
        quantiteDisponibleColumn.setPrefWidth(100);
        actionsColumn.setPrefWidth(100);
        
        quantiteColumn.setCellFactory(column -> new TableCell<Equipement, Integer>() {
            @Override
            protected void updateItem(Integer quantite, boolean empty) {
                super.updateItem(quantite, empty);
                if (empty || quantite == null) {
                    setText(null);
                } else {
                    setText(quantite + " unité(s)");
                }
            }
        });
        
        // Formatage de la colonne quantité utilisée
        quantiteUtiliseeColumn.setCellFactory(column -> new TableCell<Equipement, Integer>() {
            @Override
            protected void updateItem(Integer quantite, boolean empty) {
                super.updateItem(quantite, empty);
                if (empty || quantite == null) {
                    setText(null);
                } else {
                    setText(quantite + " utilisé(s)");
                    setStyle("-fx-text-fill: #FF9800; -fx-alignment: CENTER;");
                }
            }
        });
        
        quantiteDisponibleColumn.setCellFactory(column -> new TableCell<Equipement, Integer>() {
            @Override
            protected void updateItem(Integer quantite, boolean empty) {
                super.updateItem(quantite, empty);
                if (empty || quantite == null) {
                    setText(null);
                } else {
                    setText(quantite + " disponible(s)");
                    if (quantite <= 3) {
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if (quantite <= 10) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50; -fx-alignment: CENTER;");
                    }
                }
            }
        });
        
        actionsColumn.setCellFactory(param -> new TableCell<Equipement, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Equipement equipement = getTableView().getItems().get(getIndex());
                    
                    Button detailsBtn = new Button("👁️");
                    detailsBtn.setTooltip(new Tooltip("Détails de l'équipement"));
                    detailsBtn.setStyle("-fx-background-color: #D39A43; -fx-text-fill: #3D261A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 9; -fx-background-radius: 6; -fx-cursor: hand;");
                    
                    Button editBtn = new Button("✏️");
                    editBtn.setTooltip(new Tooltip("Modifier l'équipement"));
                    editBtn.setStyle("-fx-background-color: #6B4226; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 9; -fx-background-radius: 6; -fx-cursor: hand;");
                    
                    Button deleteBtn = new Button("🗑️");
                    deleteBtn.setTooltip(new Tooltip("Supprimer l'équipement"));
                    deleteBtn.setStyle("-fx-background-color: #9E2A2B; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 9; -fx-background-radius: 6; -fx-cursor: hand;");
                    
                    editBtn.setOnAction(event -> chargerEquipementPourEdition(equipement));
                    detailsBtn.setOnAction(event -> afficherDetailsEquipement(equipement));
                    deleteBtn.setOnAction(event -> supprimerEquipement(equipement));
                    
                    HBox box = new HBox(6, detailsBtn, editBtn, deleteBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        
        equipementTable.setItems(equipementList);
        equipementTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        equipementTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    equipementSelectionne = newVal;
                    chargerEquipementPourEdition(newVal);
                }
            }
        );
        
        equipementTable.setPrefHeight(750);
        equipementTable.setMinHeight(500);
    }
    
    /**
     * Calcule la quantité utilisée d'un équipement.
     * @param equipementId l'identifiant de l'équipement
     * @return nombre de salles utilisant cet équipement
     */
    private int getQuantiteUtilisee(int equipementId) {
        return quantitesUtilisees.getOrDefault(equipementId, 0);
    }
    
    private void configurerSpinners() {
        quantiteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 1));
    }
    
    private void configurerFiltres() {
        rechercheField.setPromptText("Rechercher un équipement...");
        
        rechercherButton.setOnAction(e -> filtrerEquipements());
        reinitialiserButton.setOnAction(e -> {
            rechercheField.clear();
            equipementList.setAll(tousEquipements);
        });
    }
    
    private void filtrerEquipements() {
        String recherche = rechercheField.getText().toLowerCase().trim();
        if (recherche.isEmpty()) {
            equipementList.setAll(tousEquipements);
            return;
        }
        
        List<Equipement> filtres = new ArrayList<>();
        for (Equipement e : tousEquipements) {
            if (e.getNom().toLowerCase().contains(recherche) ||
                (e.getDescription() != null && e.getDescription().toLowerCase().contains(recherche))) {
                filtres.add(e);
            }
        }
        
        equipementList.setAll(filtres);
    }
    
    private void configurerEvenements() {
        ajouterButton.setOnAction(e -> ajouterEquipement());
        modifierButton.setOnAction(e -> modifierEquipement());
        supprimerButton.setOnAction(e -> {
            if (equipementSelectionne != null) {
                supprimerEquipement(equipementSelectionne);
            }
        });
        annulerButton.setOnAction(e -> viderFormulaire());
        rafraichirButton.setOnAction(e -> chargerEquipements());
    }
    
    private void chargerEquipements() {
        chargementIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                tousEquipements = equipementService.listerTous();
                
                List<Salle> toutesSalles = salleService.listerTous();
                Map<Integer, Integer> utilisation = new HashMap<>();
                
                for (Salle salle : toutesSalles) {
                    for (int equipId : salle.getEquipements()) {
                        utilisation.put(equipId, utilisation.getOrDefault(equipId, 0) + 1);
                    }
                }
                
                quantitesUtilisees.clear();
                quantitesUtilisees.putAll(utilisation);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                equipementList.setAll(tousEquipements);
                mettreAJourStatistiques();
                chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur de chargement");
                chargementIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void mettreAJourStatistiques() {
        int total = tousEquipements.size();
        int totalQuantite = 0;
        int totalUtilise = 0;
        
        for (Equipement e : tousEquipements) {
            totalQuantite += e.getQuantite();
            totalUtilise += getQuantiteUtilisee(e.getId());
        }
        
        totalEquipementsLabel.setText(total + " type(s)");
        totalUtilisesLabel.setText(totalUtilise + " utilisé(s)");
        totalDisponiblesLabel.setText((totalQuantite - totalUtilise) + " disponible(s)");
    }
    
    /**
     * Affiche les détails d'un équipement dans une boîte de dialogue.
     * @param equipement l'équipement à afficher
     */
    private void afficherDetailsEquipement(Equipement equipement) {
        int utilise = getQuantiteUtilisee(equipement.getId());
        int disponible = equipement.getQuantite() - utilise;
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'équipement");
        alert.setHeaderText(equipement.getNom());
        
        String contenu = String.format(
            "ID: %d\n" +
            "Description: %s\n" +
            "Quantité totale: %d\n" +
            "Quantité utilisée: %d\n" +
            "Quantité disponible: %d\n",
            equipement.getId(),
            equipement.getDescription() != null ? equipement.getDescription() : "Aucune description",
            equipement.getQuantite(),
            utilise,
            disponible
        );
        
        alert.setContentText(contenu);
        alert.show();
    }
    
    private void chargerEquipementPourEdition(Equipement equipement) {
        nomField.setText(equipement.getNom());
        descriptionArea.setText(equipement.getDescription());
        quantiteSpinner.getValueFactory().setValue(equipement.getQuantite());
        equipementSelectionne = equipement;
        
        modifierButton.setDisable(false);
        supprimerButton.setDisable(false);
        ajouterButton.setDisable(true);
    }
    
    private void viderFormulaire() {
        nomField.clear();
        descriptionArea.clear();
        quantiteSpinner.getValueFactory().setValue(1);
        equipementSelectionne = null;
        
        modifierButton.setDisable(true);
        supprimerButton.setDisable(true);
        ajouterButton.setDisable(false);
        
        equipementTable.getSelectionModel().clearSelection();
    }
    
    /**
     * Ajoute un nouvel équipement.
     */
    private void ajouterEquipement() {
        if (!validerFormulaire()) return;
        
        int quantite = quantiteSpinner.getValue();
        
        Equipement equipement = new Equipement(
            nomField.getText().trim(),
            descriptionArea.getText().trim()
        );
        equipement.setQuantite(quantite);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                equipementService.ajouter(equipement);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Équipement ajouté");
                viderFormulaire();
                chargerEquipements();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur ajout");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Modifie un équipement existant.
     */
    private void modifierEquipement() {
        if (equipementSelectionne == null) return;
        if (!validerFormulaire()) return;
        
        int nouvelleQuantite = quantiteSpinner.getValue();
        int utilise = getQuantiteUtilisee(equipementSelectionne.getId());
        
        if (nouvelleQuantite < utilise) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de quantité");
            alert.setHeaderText("Impossible de réduire la quantité");
            alert.setContentText(String.format(
                "Cet équipement est utilisé dans %d salle(s).\n" +
                "Quantité minimale autorisée: %d",
                utilise, utilise
            ));
            alert.show();
            return;
        }
        
        equipementSelectionne.setNom(nomField.getText().trim());
        equipementSelectionne.setDescription(descriptionArea.getText().trim());
        equipementSelectionne.setQuantite(nouvelleQuantite);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                equipementService.modifier(equipementSelectionne);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Équipement modifié");
                viderFormulaire();
                chargerEquipements();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur modification");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Supprime un équipement s'il n'est pas utilisé.
     * @param equipement l'équipement à supprimer
     */
    private void supprimerEquipement(Equipement equipement) {
        int utilise = getQuantiteUtilisee(equipement.getId());
        
        if (utilise > 0) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Équipement utilisé");
            alert.setContentText(String.format(
                "Cet équipement est utilisé dans %d salle(s).\n" +
                "Êtes-vous sûr de vouloir le supprimer ?",
                utilise
            ));
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    executerSuppression(equipement);
                }
            });
        } else {
            executerSuppression(equipement);
        }
    }
    
    private void executerSuppression(Equipement equipement) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'équipement ?");
        alert.setContentText("Supprimer " + equipement.getNom() + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        equipementService.supprimer(equipement.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        messageLabel.setText("✅ Équipement supprimé");
                        viderFormulaire();
                        chargerEquipements();
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
    
    private boolean validerFormulaire() {
        if (nomField.getText().trim().isEmpty()) {
            messageLabel.setText("❌ Le nom est requis");
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
            controleur.selectionnerOnglet(1); // Onglet Gestion
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour gestion", e);
            afficherErreur("Impossible de retourner au menu Gestion");
        }
    }
}