package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.SQLException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import scheduler.util.IconHelper;
import scheduler.util.VisualiseurPhoto;

/**
 * Gestion des salles de l'université avec support photos.
 */
public class GestionSallesControleur extends TableauBordControleur {
    
    @FXML private TableView<Salle> salleTable;
    @FXML private TableColumn<Salle, String> photoColumn;
    @FXML private TableColumn<Salle, Integer> idColumn;
    @FXML private TableColumn<Salle, String> numeroColumn;
    @FXML private TableColumn<Salle, Integer> capaciteColumn;
    @FXML private TableColumn<Salle, String> typeColumn;
    @FXML private TableColumn<Salle, String> batimentColumn;
    @FXML private TableColumn<Salle, Integer> etageColumn;
    @FXML private TableColumn<Salle, String> statutColumn;
    @FXML private TableColumn<Salle, String> equipementsColumn;
    @FXML private TableColumn<Salle, Void> actionsColumn;
    
    @FXML private ImageView photoPreview;
    @FXML private Button choisirPhotoButton;
    @FXML private Button supprimerPhotoButton;
    
    @FXML private ComboBox<Batiment> batimentCombo;
    @FXML private TextField numeroField;
    @FXML private Spinner<Integer> capaciteSpinner;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Spinner<Integer> etageSpinner;
    @FXML private ListView<Equipement> equipementsDisponiblesList;
    @FXML private ListView<Equipement> equipementsSalleList;
    @FXML private TextArea motifArea;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Label messageLabel;
    @FXML private Label totalSallesLabel;
    @FXML private Button retourButton;
    
    @FXML private TextField rechercheField;
    @FXML private ComboBox<String> filtreTypeCombo;
    @FXML private ComboBox<String> filtreStatutCombo;
    @FXML private ComboBox<Batiment> filtreBatimentCombo;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    
    @FXML private Button ajouterButton;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button rendreIndisponibleButton;
    @FXML private Button rendreDisponibleButton;
    @FXML private Button ajouterEquipementButton;
    @FXML private Button retirerEquipementButton;
    @FXML private Button annulerButton;
    @FXML private Button rafraichirButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private SalleService salleService;
    private BatimentService batimentService;
    private EquipementService equipementService;
    private Salle salleSelectionne;
    private ObservableList<Salle> salleList;
    private List<Salle> toutesSalles;
    private ObservableList<Equipement> equipementsDisponibles;
    private ObservableList<Equipement> equipementsSalle;
    private Map<Integer, Integer> stockEquipements; // Stock total par équipement
    private String cheminPhotoActuelle;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.salleService = new SalleService();
        this.batimentService = new BatimentService();
        this.equipementService = new EquipementService();
        this.salleList = FXCollections.observableArrayList();
        this.toutesSalles = new ArrayList<>();
        this.equipementsDisponibles = FXCollections.observableArrayList();
        this.equipementsSalle = FXCollections.observableArrayList();
        this.stockEquipements = new HashMap<>();
        
        configurerTableau();
        configurerEvenements();
        configurerSpinners();
        configurerFiltres();
        chargerBatiments();
        chargerEquipements();
        chargerSalles();
        verifierEtMettreAJourStatutSalles(); 
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetourGestion());
        }
        
        if (rendreIndisponibleButton != null) {
            rendreIndisponibleButton.setOnAction(e -> {
                if (salleSelectionne != null) {
                    ouvrirDialogueIndisponibilite(salleSelectionne);
                } else {
                    messageLabel.setText("❌ Sélectionnez une salle d'abord");
                }
            });
        }
        
        if (rendreDisponibleButton != null) {
            rendreDisponibleButton.setOnAction(e -> {
                if (salleSelectionne != null) {
                    if (salleSelectionne.getStatut().equals("indisponible")) {
                        rendreDisponible(salleSelectionne);
                    } else {
                        messageLabel.setText("ℹ️ Cette salle est déjà disponible");
                    }
                } else {
                    messageLabel.setText("❌ Sélectionnez une salle d'abord");
                }
            });
        }
    }
    
    @Override
    protected void initialiserTableauBord() {
        // Déjà fait dans initialize()
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerSalles();
        chargerBatiments();
        chargerEquipements();
        verifierEtMettreAJourStatutSalles(); // AJOUT
    }
    
    /**
     * Vérifie automatiquement les salles dont la période d'indisponibilité est passée.
     */
    private void verifierEtMettreAJourStatutSalles() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                List<Salle> salles = salleService.listerTous();
                LocalDate aujourdhui = LocalDate.now();
                
                for (Salle s : salles) {
                    if ("indisponible".equals(s.getStatut()) && s.getDateFinIndisponibilite() != null) {
                        try {
                            LocalDate dateFin = LocalDate.parse(s.getDateFinIndisponibilite());
                            if (dateFin.isBefore(aujourdhui)) {
                                salleService.rendreDisponible(s.getId());
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                chargerSalles();
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur vérification statut salles", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    
    private void configurerTableau() {
        if (photoColumn != null) {
            photoColumn.setCellFactory(col -> new TableCell<Salle, String>() {
                private final ImageView imgView = new ImageView();
                {
                    imgView.setFitWidth(36);
                    imgView.setFitHeight(28);
                    imgView.setPreserveRatio(true);
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setCursor(javafx.scene.Cursor.DEFAULT);
                    } else {
                        Salle s = getTableRow().getItem();
                        String photo = s.getPhotoUrl();
                        if (photo != null && !photo.isBlank()) {
                            Image img = ImageUploadService.chargerImage(photo, 36, 28);
                            if (img != null) {
                                imgView.setImage(img);
                                setGraphic(imgView);
                                setCursor(javafx.scene.Cursor.HAND);
                                setOnMouseClicked(e -> {
                                    VisualiseurPhoto.afficher("Salle " + s.getNumero(), 
                                        "Bâtiment : " + getNomBatiment(s.getBatimentId()) + " • " + s.getCapacite() + " places", 
                                        s.getPhotoUrl(), "Salle " + s.getType());
                                });
                                return;
                            }
                        }
                        ImageView fallbackIcon = IconHelper.getIcon("room.png", 22);
                        setGraphic(fallbackIcon != null ? fallbackIcon : new Label("🏫"));
                        setCursor(javafx.scene.Cursor.DEFAULT);
                        setOnMouseClicked(null);
                    }
                }
            });
        }
        
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        numeroColumn.setCellValueFactory(new PropertyValueFactory<>("numero"));
        capaciteColumn.setCellValueFactory(new PropertyValueFactory<>("capacite"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        etageColumn.setCellValueFactory(new PropertyValueFactory<>("etage"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        
        idColumn.setStyle("-fx-alignment: CENTER;");
        numeroColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        capaciteColumn.setStyle("-fx-alignment: CENTER;");
        typeColumn.setStyle("-fx-alignment: CENTER;");
        etageColumn.setStyle("-fx-alignment: CENTER;");
        
        idColumn.setPrefWidth(50);
        numeroColumn.setPrefWidth(90);
        capaciteColumn.setPrefWidth(75);
        typeColumn.setPrefWidth(85);
        etageColumn.setPrefWidth(60);
        statutColumn.setPrefWidth(100);
        batimentColumn.setPrefWidth(140);
        equipementsColumn.setPrefWidth(160);
        actionsColumn.setPrefWidth(210);
        
        batimentColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomBatiment(cellData.getValue().getBatimentId())
            )
        );
        batimentColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        
        equipementsColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    List<Integer> eqIds = cellData.getValue().getEquipements();
                    if (eqIds == null || eqIds.isEmpty()) return "Aucun";
                    
                    Map<Integer, Integer> compteur = new HashMap<>();
                    for (int id : eqIds) {
                        compteur.put(id, compteur.getOrDefault(id, 0) + 1);
                    }
                    
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<Integer, Integer> entry : compteur.entrySet()) {
                        if (sb.length() > 0) sb.append(", ");
                        String nom = getNomEquipement(entry.getKey());
                        sb.append(entry.getValue()).append("x ").append(nom);
                    }
                    return sb.toString();
                }
            )
        );
        equipementsColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        
        statutColumn.setCellFactory(column -> new TableCell<Salle, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                } else {
                    setText(statut.substring(0, 1).toUpperCase() + statut.substring(1));
                    if (statut.equalsIgnoreCase("indisponible")) {
                        setStyle("-fx-text-fill: #9E2A2B; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });
        
        actionsColumn.setCellFactory(param -> new TableCell<Salle, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Salle salle = getTableView().getItems().get(getIndex());
                    
                    Button editBtn = new Button("✏️");
                    editBtn.setTooltip(new Tooltip("Modifier la salle"));
                    editBtn.setStyle("-fx-background-color: #6B4226; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 9; -fx-background-radius: 6; -fx-cursor: hand;");
                    
                    Button viewBtn = new Button("👁️");
                    viewBtn.setTooltip(new Tooltip("Voir la photo et détails"));
                    viewBtn.setStyle("-fx-background-color: #D39A43; -fx-text-fill: #3D261A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 9; -fx-background-radius: 6; -fx-cursor: hand;");
                    
                    Button equipBtn = new Button("🔧");
                    equipBtn.setTooltip(new Tooltip("Gérer les équipements"));
                    equipBtn.setStyle("-fx-background-color: #8C6D58; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 9; -fx-background-radius: 6; -fx-cursor: hand;");
                    
                    boolean estDispo = "disponible".equalsIgnoreCase(salle.getStatut());
                    Button indispoBtn = new Button(estDispo ? "🔒" : "🔓");
                    indispoBtn.setTooltip(new Tooltip(estDispo ? "Marquer indisponible" : "Rendre disponible"));
                    indispoBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 9; -fx-background-radius: 6; -fx-cursor: hand;",
                        estDispo ? "#FBEBEB" : "#EDF7ED", estDispo ? "#A42424" : "#2E7D32"));
                    
                    Button deleteBtn = new Button("🗑️");
                    deleteBtn.setTooltip(new Tooltip("Supprimer la salle"));
                    deleteBtn.setStyle("-fx-background-color: #9E2A2B; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 9; -fx-background-radius: 6; -fx-cursor: hand;");
                    
                    editBtn.setOnAction(e -> chargerSallePourEdition(salle));
                    deleteBtn.setOnAction(e -> supprimerSalle(salle));
                    equipBtn.setOnAction(e -> chargerSallePourEdition(salle));
                    viewBtn.setOnAction(e -> {
                        VisualiseurPhoto.afficher("Salle " + salle.getNumero(), 
                            "Bâtiment : " + getNomBatiment(salle.getBatimentId()) + " • Capacité : " + salle.getCapacite() + " places", 
                            salle.getPhotoUrl(), "Salle " + salle.getType());
                    });
                    indispoBtn.setOnAction(e -> {
                        if (estDispo) {
                            ouvrirDialogueIndisponibilite(salle);
                        } else {
                            rendreDisponible(salle);
                        }
                    });
                    
                    HBox box = new HBox(6, viewBtn, editBtn, equipBtn, indispoBtn, deleteBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        
        salleTable.setItems(salleList);
        salleTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        salleTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    salleSelectionne = newVal;
                    chargerSallePourEdition(newVal);
                    rendreIndisponibleButton.setDisable(!newVal.getStatut().equals("disponible"));
                    rendreDisponibleButton.setDisable(!newVal.getStatut().equals("indisponible"));
                }
            }
        );
    }
    
    private String getNomEquipement(int equipementId) {
        for (Equipement e : equipementsDisponibles) {
            if (e.getId() == equipementId) {
                return e.getNom();
            }
        }
        return "ID " + equipementId;
    }
    
    private void configurerFiltres() {
        filtreTypeCombo.getItems().addAll("Tous", "TD", "TP", "Amphi", "Autre");
        filtreTypeCombo.setValue("Tous");
        filtreTypeCombo.setPrefWidth(100);
        
        filtreStatutCombo.getItems().addAll("Tous", "disponible", "indisponible");
        filtreStatutCombo.setValue("Tous");
        filtreStatutCombo.setPrefWidth(100);
        
        filtreBatimentCombo.setPrefWidth(150);
        filtreBatimentCombo.setPromptText("Tous les bâtiments");
        
        rechercheField.setPromptText("Numéro de salle...");
        rechercheField.setPrefWidth(200);
        
        rechercherButton.setText("Rechercher");
        rechercherButton.setStyle("-fx-background-color: #6B4226; -fx-text-fill: white; -fx-padding: 6 16; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        
        reinitialiserButton.setText("Réinitialiser");
        reinitialiserButton.setStyle("-fx-background-color: #8C6D58; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
        
        rechercherButton.setOnAction(e -> filtrerSalles());
        reinitialiserButton.setOnAction(e -> {
            rechercheField.clear();
            filtreTypeCombo.setValue("Tous");
            filtreStatutCombo.setValue("Tous");
            filtreBatimentCombo.setValue(null);
            salleList.setAll(toutesSalles);
        });
    }
    
    /**
     * Filtre les salles selon les critères de recherche.
     */
    private void filtrerSalles() {
        if (toutesSalles == null || toutesSalles.isEmpty()) return;
        
        List<Salle> filtrees = new ArrayList<>(toutesSalles);
        
        String recherche = rechercheField.getText().toLowerCase().trim();
        if (!recherche.isEmpty()) {
            filtrees.removeIf(s -> !s.getNumero().toLowerCase().contains(recherche));
        }
        
        String type = filtreTypeCombo.getValue();
        if (type != null && !"Tous".equals(type)) {
            filtrees.removeIf(s -> !s.getType().equals(type));
        }
        
        String statut = filtreStatutCombo.getValue();
        if (statut != null && !"Tous".equals(statut)) {
            filtrees.removeIf(s -> !s.getStatut().equals(statut));
        }
        
        Batiment batiment = filtreBatimentCombo.getValue();
        if (batiment != null) {
            filtrees.removeIf(s -> s.getBatimentId() != batiment.getId());
        }
        
        salleList.setAll(filtrees);
    }
    
    private void configurerEvenements() {
        ajouterButton.setOnAction(e -> ajouterSalle());
        modifierButton.setOnAction(e -> modifierSalle());
        supprimerButton.setOnAction(e -> {
            if (salleSelectionne != null) supprimerSalle(salleSelectionne);
        });
        
        // ✅ AJOUTER VÉRIFICATION NULL
        if (ajouterEquipementButton != null) {
            ajouterEquipementButton.setOnAction(e -> {
                Equipement eq = equipementsDisponiblesList.getSelectionModel().getSelectedItem();
                if (eq != null && salleSelectionne != null) {
                    int stockDisponible = calculerStockDisponible(eq.getId());
                    
                    if (stockDisponible <= 0) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Stock épuisé");
                        alert.setHeaderText("Équipement non disponible");
                        alert.setContentText("Il n'y a plus de " + eq.getNom() + " en stock.");
                        alert.show();
                        return;
                    }
                    
                    ajouterEquipementASalle(eq);
                    messageLabel.setText("✅ " + eq.getNom() + " ajouté");
                }
            });
        }
        
        if (retirerEquipementButton != null) {
            retirerEquipementButton.setOnAction(e -> {
                Equipement eq = equipementsSalleList.getSelectionModel().getSelectedItem();
                if (eq != null && salleSelectionne != null) {
                    retirerEquipementDeSalle(eq);
                    messageLabel.setText("✅ " + eq.getNom() + " retiré");
                }
            });
        }
        
        annulerButton.setOnAction(e -> viderFormulaire());
        rafraichirButton.setOnAction(e -> chargerSalles());
        
        if (choisirPhotoButton != null) {
            choisirPhotoButton.setOnAction(e -> choisirPhoto());
        }
        if (supprimerPhotoButton != null) {
            supprimerPhotoButton.setOnAction(e -> supprimerPhoto());
        }
        if (photoPreview != null) {
            photoPreview.setCursor(javafx.scene.Cursor.HAND);
            photoPreview.setOnMouseClicked(e -> {
                if (cheminPhotoActuelle != null && !cheminPhotoActuelle.isBlank()) {
                    VisualiseurPhoto.afficher("Aperçu de la salle", 
                        numeroField.getText() != null ? "Salle " + numeroField.getText() : "Salle", 
                        cheminPhotoActuelle, "Format agrandi");
                }
            });
        }
        
        typeCombo.getItems().addAll("TD", "TP", "Amphi", "Autre");
    }
    
    private void choisirPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir la photo de la salle");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images (*.png, *.jpg, *.jpeg, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File fichier = fileChooser.showOpenDialog(salleTable.getScene().getWindow());
        if (fichier != null) {
            try {
                String chemin = ImageUploadService.sauvegarderPhotoSalle(fichier);
                cheminPhotoActuelle = chemin;
                Image img = ImageUploadService.chargerImage(chemin, 110, 90);
                if (photoPreview != null && img != null) {
                    photoPreview.setImage(img);
                }
                if (supprimerPhotoButton != null) {
                    supprimerPhotoButton.setVisible(true);
                }
            } catch (IOException ex) {
                logger.error("Erreur enregistrement photo salle", ex);
                messageLabel.setText("❌ Erreur de sauvegarde de la photo");
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
    
    private int calculerStockDisponible(int equipementId) {
        Integer stockTotal = stockEquipements.get(equipementId);
        if (stockTotal == null) return 0;
        
        int utilise = 0;
        for (Salle s : toutesSalles) {
            for (int id : s.getEquipements()) {
                if (id == equipementId) utilise++;
            }
        }
        if (salleSelectionne != null) {
            for (Equipement e : equipementsSalle) {
                if (e.getId() == equipementId) utilise++;
            }
        }
        
        return stockTotal - utilise;
    }
    
    private void configurerSpinners() {
        capaciteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 30));
        etageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
    }
    
    private void chargerBatiments() {
        Task<List<Batiment>> task = new Task<>() {
            @Override
            protected List<Batiment> call() throws SQLException {
                return batimentService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                List<Batiment> batiments = getValue();
                
                batimentCombo.setCellFactory(lv -> new ListCell<Batiment>() {
                    @Override
                    protected void updateItem(Batiment item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom());
                    }
                });
                
                batimentCombo.setButtonCell(new ListCell<Batiment>() {
                    @Override
                    protected void updateItem(Batiment item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Sélectionner un bâtiment" : item.getNom());
                    }
                });
                
                batimentCombo.getItems().setAll(batiments);
                
                filtreBatimentCombo.setCellFactory(lv -> new ListCell<Batiment>() {
                    @Override
                    protected void updateItem(Batiment item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Tous les bâtiments");
                        } else {
                            setText(item.getNom());
                        }
                    }
                });
                
                filtreBatimentCombo.setButtonCell(new ListCell<Batiment>() {
                    @Override
                    protected void updateItem(Batiment item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Tous les bâtiments" : item.getNom());
                    }
                });
            }
        };
        new Thread(task).start();
    }
    
    private void chargerEquipements() {
        Task<List<Equipement>> task = new Task<>() {
            @Override
            protected List<Equipement> call() throws SQLException {
                return equipementService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                List<Equipement> equipements = getValue();
                
                if (equipements.isEmpty()) {
                    messageLabel.setText("ℹ️ Aucun équipement trouvé dans la base");
                }
                
                stockEquipements.clear();
                for (Equipement e : equipements) {
                    stockEquipements.put(e.getId(), e.getQuantite());
                }
                
                equipementsDisponibles.setAll(equipements);
                equipementsDisponiblesList.setItems(equipementsDisponibles);
                
                equipementsDisponiblesList.setCellFactory(lv -> new ListCell<Equipement>() {
                    @Override
                    protected void updateItem(Equipement item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            int dispo = calculerStockDisponible(item.getId());
                            setText(item.getNom() + " (Dispo: " + dispo + "/" + item.getQuantite() + ")");
                        }
                    }
                });
                
                equipementsSalleList.setCellFactory(lv -> new ListCell<Equipement>() {
                    @Override
                    protected void updateItem(Equipement item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom());
                    }
                });
            }
            
            @Override
            protected void failed() {
                logger.error("❌ Erreur chargement équipements", getException());
                messageLabel.setText("❌ Erreur de chargement des équipements");
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerSalles() {
        chargementIndicator.setVisible(true);
        
        Task<List<Salle>> task = new Task<>() {
            @Override
            protected List<Salle> call() throws SQLException {
                return salleService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                toutesSalles = getValue();
                salleList.setAll(toutesSalles);
                totalSallesLabel.setText(toutesSalles.size() + " salle(s)");
                chargementIndicator.setVisible(false);
                
                if (equipementsDisponiblesList.getItems() != null) {
                    equipementsDisponiblesList.refresh();
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement salles", getException());
                messageLabel.setText("❌ Erreur de chargement");
                chargementIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private String getNomBatiment(int batimentId) {
        for (Batiment b : batimentCombo.getItems()) {
            if (b.getId() == batimentId) {
                return b.getNom();
            }
        }
        return "Inconnu";
    }
    
    private void chargerSallePourEdition(Salle salle) {
        numeroField.setText(salle.getNumero());
        capaciteSpinner.getValueFactory().setValue(salle.getCapacite());
        typeCombo.setValue(salle.getType());
        etageSpinner.getValueFactory().setValue(salle.getEtage());
        
        for (Batiment b : batimentCombo.getItems()) {
            if (b.getId() == salle.getBatimentId()) {
                batimentCombo.setValue(b);
                break;
            }
        }
        
        salleSelectionne = salle;
        cheminPhotoActuelle = salle.getPhotoUrl();
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
        
        Task<List<Equipement>> task = new Task<>() {
            @Override
            protected List<Equipement> call() throws SQLException {
                return equipementService.listerParSalle(salle.getId());
            }
            
            @Override
            protected void succeeded() {
                equipementsSalle.setAll(getValue());
                equipementsSalleList.setItems(equipementsSalle);
                equipementsDisponiblesList.refresh();
            }
        };
        
        new Thread(task).start();
        
        modifierButton.setDisable(false);
        supprimerButton.setDisable(false);
        ajouterButton.setDisable(true);
        rendreIndisponibleButton.setDisable(!salle.getStatut().equals("disponible"));
        rendreDisponibleButton.setDisable(!salle.getStatut().equals("indisponible"));
    }
    
    private void viderFormulaire() {
        numeroField.clear();
        capaciteSpinner.getValueFactory().setValue(30);
        typeCombo.setValue(null);
        etageSpinner.getValueFactory().setValue(0);
        batimentCombo.setValue(null);
        motifArea.clear();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        
        equipementsSalle.clear();
        supprimerPhoto();
        
        salleSelectionne = null;
        
        modifierButton.setDisable(true);
        supprimerButton.setDisable(true);
        ajouterButton.setDisable(false);
        rendreIndisponibleButton.setDisable(true);
        rendreDisponibleButton.setDisable(true);
        
        salleTable.getSelectionModel().clearSelection();
        messageLabel.setText("");
    }
    
    /**
     * Ajoute une nouvelle salle.
     */
    private void ajouterSalle() {
        if (!validerFormulaireBase()) return;
        
        Salle salle = new Salle(
            numeroField.getText().trim(),
            capaciteSpinner.getValue(),
            typeCombo.getValue(),
            batimentCombo.getValue().getId(),
            etageSpinner.getValue()
        );
        salle.setPhotoUrl(cheminPhotoActuelle);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                salleService.ajouter(salle);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Salle ajoutée");
                viderFormulaire();
                chargerSalles();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur ajout");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Modifie la salle sélectionnée.
     */
    private void modifierSalle() {
        if (salleSelectionne == null) return;
        if (!validerFormulaireBase()) return;
        
        salleSelectionne.setNumero(numeroField.getText().trim());
        salleSelectionne.setCapacite(capaciteSpinner.getValue());
        salleSelectionne.setType(typeCombo.getValue());
        salleSelectionne.setBatimentId(batimentCombo.getValue().getId());
        salleSelectionne.setEtage(etageSpinner.getValue());
        salleSelectionne.setPhotoUrl(cheminPhotoActuelle);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                salleService.modifier(salleSelectionne);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Salle modifiée");
                viderFormulaire();
                chargerSalles();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur modification");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Supprime une salle après vérification (pas de réservations ni de cours).
     * @param salle la salle à supprimer
     */
    private void supprimerSalle(Salle salle) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la salle ?");
        alert.setContentText("Voulez-vous vraiment supprimer " + salle.getNumero() + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        salleService.supprimer(salle.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        messageLabel.setText("✅ Salle supprimée");
                        viderFormulaire();
                        chargerSalles();
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
    
    private void ajouterEquipementASalle(Equipement equipement) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                salleService.ajouterEquipement(salleSelectionne.getId(), equipement.getId());
                return null;
            }
            
            @Override
            protected void succeeded() {
                chargerSallePourEdition(salleSelectionne);
                messageLabel.setText("✅ Équipement ajouté");
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur ajout équipement");
            }
        };
        
        new Thread(task).start();
    }
    
    private void retirerEquipementDeSalle(Equipement equipement) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                salleService.retirerEquipement(salleSelectionne.getId(), equipement.getId());
                return null;
            }
            
            @Override
            protected void succeeded() {
                chargerSallePourEdition(salleSelectionne);
                messageLabel.setText("✅ Équipement retiré");
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur retrait équipement");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Ouvre le dialogue pour rendre une salle indisponible.
     * @param salle la salle à rendre indisponible
     */
    private void ouvrirDialogueIndisponibilite(Salle salle) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Rendre indisponible");
        dialog.setHeaderText("Rendre " + salle.getNumero() + " indisponible");
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        TextArea motifArea = new TextArea();
        motifArea.setPromptText("Motif de l'indisponibilité (obligatoire)");
        DatePicker debutPicker = new DatePicker(LocalDate.now());
        DatePicker finPicker = new DatePicker(LocalDate.now().plusWeeks(2));
        
        finPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (debutPicker.getValue() != null && newVal != null && newVal.isBefore(debutPicker.getValue())) {
                finPicker.setValue(debutPicker.getValue().plusDays(1));
                messageLabel.setText("⚠️ La date de fin doit être après la date de début");
            }
        });
        
        grid.add(new Label("Motif *:"), 0, 0);
        grid.add(motifArea, 1, 0);
        grid.add(new Label("Début:"), 0, 1);
        grid.add(debutPicker, 1, 1);
        grid.add(new Label("Fin:"), 0, 2);
        grid.add(finPicker, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String motif = motifArea.getText().trim();
                if (motif.isEmpty()) {
                    messageLabel.setText("❌ Le motif est obligatoire");
                    return;
                }
                
                if (debutPicker.getValue() == null || finPicker.getValue() == null) {
                    messageLabel.setText("❌ Les dates sont obligatoires");
                    return;
                }
                
                if (finPicker.getValue().isBefore(debutPicker.getValue())) {
                    messageLabel.setText("❌ La date de fin doit être après la date de début");
                    return;
                }
                
                rendreIndisponible(salle, motif, 
                    debutPicker.getValue().toString(), finPicker.getValue().toString());
            }
        });
    }
    
    /**
     * Rend une salle indisponible.
     * @param salle la salle concernée
     * @param motif le motif de l'indisponibilité
     * @param debut date de début
     * @param fin date de fin
     */
    private void rendreIndisponible(Salle salle, String motif, String debut, String fin) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                salleService.rendreIndisponible(salle.getId(), motif, debut, fin);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Salle marquée indisponible du " + debut + " au " + fin);
                chargerSalles();
                viderFormulaire();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur lors du changement de statut");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Rend une salle disponible.
     * @param salle la salle à rendre disponible
     */
    private void rendreDisponible(Salle salle) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Rendre disponible ?");
        alert.setContentText("Voulez-vous rendre " + salle.getNumero() + " disponible ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        salleService.rendreDisponible(salle.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        messageLabel.setText("✅ Salle disponible");
                        chargerSalles();
                        viderFormulaire();
                    }
                    
                    @Override
                    protected void failed() {
                        messageLabel.setText("❌ Erreur");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    private boolean validerFormulaireBase() {
        if (numeroField.getText().trim().isEmpty()) {
            messageLabel.setText("❌ Numéro requis");
            return false;
        }
        if (typeCombo.getValue() == null) {
            messageLabel.setText("❌ Type requis");
            return false;
        }
        if (batimentCombo.getValue() == null) {
            messageLabel.setText("❌ Bâtiment requis");
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