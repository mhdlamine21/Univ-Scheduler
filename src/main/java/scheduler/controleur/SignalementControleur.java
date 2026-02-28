package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Contrôleur pour l'écran Signalement.
 */
public class SignalementControleur extends TableauBordControleur {
    
    @FXML private ComboBox<Salle> salleCombo;
    @FXML private ComboBox<String> typeProblemeCombo;
    @FXML private ComboBox<String> prioriteCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Label salleInfoLabel;
    @FXML private Button soumettreButton;
    @FXML private Button annulerButton;
    
    @FXML private ImageView photoPreview;
    @FXML private Button choisirPhotoButton;
    @FXML private Button supprimerPhotoButton;
    private String cheminPhotoPreuve;
    
    @FXML private TableView<Signalement> mesSignalementsTable;
    @FXML private TableColumn<Signalement, String> photoMesColumn;
    @FXML private TableColumn<Signalement, Integer> idColumn;
    @FXML private TableColumn<Signalement, String> salleColumn;
    @FXML private TableColumn<Signalement, String> typeColumn;
    @FXML private TableColumn<Signalement, String> prioriteColumn;
    @FXML private TableColumn<Signalement, String> dateColumn;
    @FXML private TableColumn<Signalement, String> statutColumn;
    @FXML private TableColumn<Signalement, Void> actionsColumn;
    @FXML private ListView<String> equipementsListView;
    
    @FXML private TableView<Signalement> tousSignalementsTable;
    @FXML private TableColumn<Signalement, String> photoTousColumn;
    @FXML private TableColumn<Signalement, Integer> idAllColumn;
    @FXML private TableColumn<Signalement, String> utilisateurColumn;
    @FXML private TableColumn<Signalement, String> salleAllColumn;
    @FXML private TableColumn<Signalement, String> typeAllColumn;
    @FXML private TableColumn<Signalement, String> prioriteAllColumn;
    @FXML private TableColumn<Signalement, String> dateAllColumn;
    @FXML private TableColumn<Signalement, String> statutAllColumn;
    @FXML private TableColumn<Signalement, Void> actionsAllColumn;
    
    @FXML private Label totalSignalementsLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label resolusLabel;
    @FXML private TabPane tabPane;
    
    @FXML private ComboBox<String> filtreStatutCombo;
    @FXML private ComboBox<String> filtrePrioriteCombo;
    @FXML private Button rafraichirButton;
    @FXML private ProgressIndicator chargementIndicator;
    
    private SignalementService signalementService;
    private SalleService salleService;
    private UtilisateurService utilisateurService;
    private EmailService emailService;
    private EquipementService equipementService;
    private NotificationService notificationService;
    
    private ObservableList<Signalement> mesSignalementsList;
    private ObservableList<Signalement> tousSignalementsList;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.signalementService = new SignalementService();
        this.salleService = new SalleService();
        this.utilisateurService = new UtilisateurService();
        this.emailService = new EmailService();
        this.notificationService = new NotificationService();
        this.equipementService = new EquipementService();
        this.mesSignalementsList = FXCollections.observableArrayList();
        this.tousSignalementsList = FXCollections.observableArrayList();
        
        configurerTypesProblemes();
        configurerPriorites();
        configurerTableauMesSignalements();
        configurerTableauTousSignalements();
        configurerEvenements();
        
        chargerSalles();
        
        // Si ce n'est pas un admin, masquer l'onglet "Tous les signalements"
        if (utilisateurConnecte != null && !utilisateurConnecte.getRole().equals("admin")) {
            if (tabPane != null && tabPane.getTabs().size() > 2) {
                tabPane.getTabs().remove(2);
            }
        }
        
        // Charger les statistiques si admin
        if (utilisateurConnecte != null && utilisateurConnecte.getRole().equals("admin")) {
            chargerStatistiques();
        }
    }
    
    @Override
    protected void initialiserTableauBord() {}
    
    @Override
    protected void rafraichirDonnees() {
        chargerDonnees();
        if (utilisateurConnecte != null && utilisateurConnecte.getRole().equals("admin")) {
            chargerStatistiques();
        }
    }
    
    
    private void configurerPriorites() {
        if (prioriteCombo != null) {
            prioriteCombo.getItems().addAll("HAUTE", "MOYENNE", "BASSE");
            prioriteCombo.setValue("MOYENNE");
        }
        if (filtrePrioriteCombo != null) {
            filtrePrioriteCombo.getItems().addAll("Tous", "HAUTE", "MOYENNE", "BASSE");
            filtrePrioriteCombo.setValue("Tous");
        }
    }
    
    private void configurerTypesProblemes() {
        typeProblemeCombo.getItems().addAll(
            "Vidéoprojecteur en panne",
            "Tableau interactif défectueux",
            "Climatisation ne fonctionne pas",
            "Problème électrique",
            "Connexion internet",
            "Mobilier dégradé",
            "Nettoyage",
            "Autre"
        );
    }
    
    private void configurerTableauMesSignalements() {
        if (photoMesColumn != null) {
            photoMesColumn.setCellFactory(col -> new TableCell<Signalement, String>() {
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
                        Signalement s = getTableRow().getItem();
                        String desc = s.getDescription();
                        if (desc != null && desc.contains("[PHOTO:")) {
                            int start = desc.indexOf("[PHOTO:") + 7;
                            int end = desc.indexOf("]", start);
                            if (end > start) {
                                String photo = desc.substring(start, end);
                                Image img = ImageUploadService.chargerImage(photo, 40, 30);
                                if (img != null) {
                                    imgView.setImage(img);
                                    setGraphic(imgView);
                                    return;
                                }
                            }
                        }
                        setGraphic(new Label("⚠️"));
                    }
                }
            });
        }
        
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        salleColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNumeroSalle(cellData.getValue().getSalleId())
            )
        );
        
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("typeProbleme"));
        
        prioriteColumn.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        prioriteColumn.setCellFactory(column -> new TableCell<Signalement, String>() {
            @Override
            protected void updateItem(String priorite, boolean empty) {
                super.updateItem(priorite, empty);
                if (empty || priorite == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(priorite);
                    switch (priorite) {
                        case "HAUTE": setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;"); break;
                        case "MOYENNE": setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); break;
                        case "BASSE": setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); break;
                    }
                }
            }
        });
        
        dateColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getDateSignalement() != null ?
                    cellData.getValue().getDateSignalement().toLocalDate().toString() : ""
            )
        );
        
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        statutColumn.setCellFactory(column -> new TableCell<Signalement, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                } else {
                    setText(statut);
                    switch (statut) {
                        case "en_attente": setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); break;
                        case "en_cours": setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;"); break;
                        case "resolu": setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); break;
                    }
                }
            }
        });
        
        actionsColumn.setCellFactory(param -> new TableCell<Signalement, Void>() {
            private final Button detailsBtn = new Button("👁️ Détails");
            
            {
                detailsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                detailsBtn.setOnAction(event -> {
                    Signalement s = getTableView().getItems().get(getIndex());
                    afficherDetails(s);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : detailsBtn);
            }
        });
        
        mesSignalementsTable.setItems(mesSignalementsList);
    }
    
    private void configurerTableauTousSignalements() {
        if (photoTousColumn != null) {
            photoTousColumn.setCellFactory(col -> new TableCell<Signalement, String>() {
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
                        Signalement s = getTableRow().getItem();
                        String desc = s.getDescription();
                        if (desc != null && desc.contains("[PHOTO:")) {
                            int start = desc.indexOf("[PHOTO:") + 7;
                            int end = desc.indexOf("]", start);
                            if (end > start) {
                                String photo = desc.substring(start, end);
                                Image img = ImageUploadService.chargerImage(photo, 40, 30);
                                if (img != null) {
                                    imgView.setImage(img);
                                    setGraphic(imgView);
                                    return;
                                }
                            }
                        }
                        setGraphic(new Label("⚠️"));
                    }
                }
            });
        }
        
        idAllColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        utilisateurColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomUtilisateur(cellData.getValue().getUtilisateurId())
            )
        );
        
        salleAllColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNumeroSalle(cellData.getValue().getSalleId())
            )
        );
        
        typeAllColumn.setCellValueFactory(new PropertyValueFactory<>("typeProbleme"));
        
        prioriteAllColumn.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        prioriteAllColumn.setCellFactory(column -> new TableCell<Signalement, String>() {
            @Override
            protected void updateItem(String priorite, boolean empty) {
                super.updateItem(priorite, empty);
                if (empty || priorite == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(priorite);
                    switch (priorite) {
                        case "HAUTE": setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;"); break;
                        case "MOYENNE": setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); break;
                        case "BASSE": setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); break;
                    }
                }
            }
        });
        
        dateAllColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getDateSignalement() != null ?
                    cellData.getValue().getDateSignalement().toLocalDate().toString() : ""
            )
        );
        
        statutAllColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        statutAllColumn.setCellFactory(column -> new TableCell<Signalement, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                } else {
                    setText(statut);
                    switch (statut) {
                        case "en_attente": setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); break;
                        case "en_cours": setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;"); break;
                        case "resolu": setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); break;
                    }
                }
            }
        });
        
        actionsAllColumn.setCellFactory(param -> new TableCell<Signalement, Void>() {
            private final Button prendreBtn = new Button("📋 Prendre en charge");
            private final Button resoudreBtn = new Button("✅ Résoudre");
            private final Button detailsBtn = new Button("👁️ Détails");
            private final Button prioriteBtn = new Button("🔧 Priorité");
            
            {
                prendreBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                resoudreBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                detailsBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                prioriteBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                
                prendreBtn.setOnAction(event -> {
                    Signalement s = getTableView().getItems().get(getIndex());
                    prendreEnCharge(s);
                });
                
                resoudreBtn.setOnAction(event -> {
                    Signalement s = getTableView().getItems().get(getIndex());
                    resoudreSignalement(s);
                });
                
                detailsBtn.setOnAction(event -> {
                    Signalement s = getTableView().getItems().get(getIndex());
                    afficherDetails(s);
                });
                
                prioriteBtn.setOnAction(event -> {
                    Signalement s = getTableView().getItems().get(getIndex());
                    modifierPriorite(s);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Signalement s = getTableView().getItems().get(getIndex());
                    HBox box = new HBox(5, detailsBtn, prioriteBtn);
                    
                    if (s.getStatut().equals("en_attente")) {
                        box.getChildren().add(prendreBtn);
                    } else if (s.getStatut().equals("en_cours")) {
                        box.getChildren().add(resoudreBtn);
                    }
                    
                    setGraphic(box);
                }
            }
        });
        
        tousSignalementsTable.setItems(tousSignalementsList);
    }
    
    private void configurerEvenements() {
        soumettreButton.setOnAction(e -> soumettreSignalement());
        annulerButton.setOnAction(e -> viderFormulaire());
        rafraichirButton.setOnAction(e -> chargerDonnees());
        
        if (choisirPhotoButton != null) {
            choisirPhotoButton.setOnAction(e -> choisirPhoto());
        }
        if (supprimerPhotoButton != null) {
            supprimerPhotoButton.setOnAction(e -> supprimerPhoto());
        }
        
        salleCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                salleInfoLabel.setText("Cap: " + newVal.getCapacite() + " - " + newVal.getType());
            }
        });
        
        filtreStatutCombo.getItems().addAll("Tous", "en_attente", "en_cours", "resolu");
        filtreStatutCombo.setValue("Tous");
        filtreStatutCombo.valueProperty().addListener((obs, oldVal, newVal) -> filtrerSignalements());
        
        if (filtrePrioriteCombo != null) {
            filtrePrioriteCombo.valueProperty().addListener((obs, oldVal, newVal) -> filtrerSignalements());
        }
        salleCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                salleInfoLabel.setText("Cap: " + newVal.getCapacite() + " - " + newVal.getType());
                chargerEquipementsSalle(newVal.getId());
            }
        });
    }
    
    
    private void chargerSalles() {
        Task<List<Salle>> task = new Task<>() {
            @Override
            protected List<Salle> call() throws SQLException {
                return salleService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                salleCombo.getItems().setAll(getValue());
                salleCombo.setCellFactory(lv -> new ListCell<Salle>() {
                    @Override
                    protected void updateItem(Salle item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item.getNumero());
                    }
                });
                salleCombo.setButtonCell(new ListCell<Salle>() {
                    @Override
                    protected void updateItem(Salle item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item.getNumero());
                    }
                });
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement salles", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerEquipementsSalle(int salleId) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws SQLException {
                List<String> nomsEquipements = new ArrayList<>();
                List<Equipement> equipements = equipementService.listerParSalle(salleId);
                
                for (Equipement e : equipements) {
                    nomsEquipements.add(e.getNom() + (e.getDescription() != null ? " - " + e.getDescription() : ""));
                }
                
                if (nomsEquipements.isEmpty()) {
                    nomsEquipements.add("Aucun équipement dans cette salle");
                }
                
                return nomsEquipements;
            }
            
            @Override
            protected void succeeded() {
                if (equipementsListView != null) {
                    equipementsListView.setItems(FXCollections.observableArrayList(getValue()));
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement équipements", getException());
                if (equipementsListView != null) {
                    equipementsListView.setItems(FXCollections.observableArrayList("Erreur de chargement"));
                }
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerMesSignalements() {
        if (utilisateurConnecte == null) return;
        
        Task<List<Signalement>> task = new Task<>() {
            @Override
            protected List<Signalement> call() throws SQLException {
                List<Signalement> tous = signalementService.listerTous();
                List<Signalement> mesSignalements = new ArrayList<>();
                for (Signalement s : tous) {
                    if (s.getUtilisateurId() == utilisateurConnecte.getId()) {
                        mesSignalements.add(s);
                    }
                }
                return mesSignalements;
            }
            
            @Override
            protected void succeeded() {
                mesSignalementsList.setAll(getValue());
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement mes signalements", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    @Override
    public void initialiserAvecUtilisateur(Utilisateur utilisateur, Stage stage) {
        super.initialiserAvecUtilisateur(utilisateur, stage);
        
        chargerDonnees();
        if (utilisateurConnecte != null && utilisateurConnecte.getRole().equals("admin")) {
            chargerStatistiques();
        }
    }

    private void chargerTousSignalements() {
        if (utilisateurConnecte == null || !utilisateurConnecte.getRole().equals("admin")) {
            return;
        }
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        
        Task<List<Signalement>> task = new Task<>() {
            @Override
            protected List<Signalement> call() throws SQLException {
                return signalementService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                List<Signalement> signalements = getValue();
                tousSignalementsList.setAll(signalements);
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement signalements", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerStatistiques() {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws SQLException {
                Map<String, Object> stats = new HashMap<>();
                List<Signalement> tous = signalementService.listerTous();
                
                int enAttente = 0, enCours = 0, resolus = 0;
                for (Signalement s : tous) {
                    switch (s.getStatut()) {
                        case "en_attente": enAttente++; break;
                        case "en_cours": enCours++; break;
                        case "resolu": resolus++; break;
                    }
                }
                
                stats.put("total", tous.size());
                stats.put("enAttente", enAttente);
                stats.put("enCours", enCours);
                stats.put("resolus", resolus);
                return stats;
            }
            
            @Override
            protected void succeeded() {
                Map<String, Object> stats = getValue();
                if (totalSignalementsLabel != null) totalSignalementsLabel.setText(String.valueOf(stats.getOrDefault("total", 0)));
                if (enAttenteLabel != null) enAttenteLabel.setText(String.valueOf(stats.getOrDefault("enAttente", 0)));
                if (enCoursLabel != null) enCoursLabel.setText(String.valueOf(stats.getOrDefault("enCours", 0)));
                if (resolusLabel != null) resolusLabel.setText(String.valueOf(stats.getOrDefault("resolus", 0)));
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement statistiques", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    
    private void filtrerSignalements() {
        String statut = filtreStatutCombo.getValue();
        String priorite = filtrePrioriteCombo != null ? filtrePrioriteCombo.getValue() : "Tous";
        
        if ("Tous".equals(statut) && "Tous".equals(priorite)) {
            tousSignalementsTable.setItems(tousSignalementsList);
        } else {
            List<Signalement> filtres = new ArrayList<>(tousSignalementsList);
            if (!"Tous".equals(statut)) filtres.removeIf(s -> !s.getStatut().equals(statut));
            if (!"Tous".equals(priorite)) filtres.removeIf(s -> !s.getPriorite().equals(priorite));
            tousSignalementsTable.setItems(FXCollections.observableArrayList(filtres));
        }
    }
    
    
    private String getNumeroSalle(int salleId) {
        for (Salle s : salleCombo.getItems()) {
            if (s.getId() == salleId) return s.getNumero();
        }
        return "Inconnue";
    }
    
    private String getNomUtilisateur(int utilisateurId) {
        try {
            Utilisateur u = utilisateurService.trouverParId(utilisateurId);
            return u != null ? u.getPrenom() + " " + u.getNom() : "Utilisateur " + utilisateurId;
        } catch (SQLException e) {
            return "Utilisateur " + utilisateurId;
        }
    }
    
    /**
    * Soumet un nouveau signalement.
    */
    private void soumettreSignalement() {
        if (salleCombo.getValue() == null) {
            afficherErreur("Veuillez sélectionner une salle");
            return;
        }
        if (typeProblemeCombo.getValue() == null) {
            afficherErreur("Veuillez sélectionner un type");
            return;
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            afficherErreur("Veuillez décrire le problème");
            return;
        }
        
        String desc = descriptionArea.getText().trim();
        if (cheminPhotoPreuve != null && !cheminPhotoPreuve.isBlank()) {
            desc += "\n[PHOTO:" + cheminPhotoPreuve + "]";
        }
        
        Signalement signalement = new Signalement(
            utilisateurConnecte.getId(),
            salleCombo.getValue().getId(),
            typeProblemeCombo.getValue(),
            desc
        );
        
        if (prioriteCombo != null) {
            signalement.setPriorite(prioriteCombo.getValue());
        }
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                signalementService.signaler(signalement);
                notifierAdministrateurs(signalement);
                return null;
            }
            
            @Override
            protected void succeeded() {
                AuditService.log(
                    utilisateurConnecte != null ? utilisateurConnecte.getEmail() : "anonyme",
                    utilisateurConnecte != null ? utilisateurConnecte.getRole() : "visiteur",
                    "SIGNALEMENT_INCIDENT",
                    "SALLE",
                    signalement.getSalleId(),
                    "Incident : " + signalement.getTypeProbleme() + " (Priorité: " + signalement.getPriorite() + ")"
                );
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Signalement envoyé");
                alert.setHeaderText("Votre signalement a été enregistré avec succès");
                alert.setContentText("L'équipe technique et l'administrateur ont été alertés.");
                alert.show();
                
                viderFormulaire();
                chargerMesSignalements();
                if (tabPane != null) tabPane.getSelectionModel().select(1);
            }
            
            @Override
            protected void failed() {
                afficherErreur("Erreur lors de l'envoi");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Notifie les administrateurs d'un nouveau signalement.
     * @param signalement le signalement créé
     */
    private void notifierAdministrateurs(Signalement signalement) {
        try {
            List<Administrateur> admins = utilisateurService.getAdministrateurs();
            Salle salle = salleService.trouverParId(signalement.getSalleId());
            String salleNom = salle != null ? salle.getNumero() : "Inconnue";
            
            for (Administrateur admin : admins) {
                notificationService.ajouterNotification(admin.getId(),
                    "⚠️ NOUVEAU SIGNALEMENT [" + signalement.getPriorite() + "] - Salle " + salleNom + "\n" +
                    "Type: " + signalement.getTypeProbleme() + "\n" +
                    "Par: " + utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom());
                
                emailService.envoyerEmail(admin.getEmail(),
                    "⚠️ [SCHEDULER] Nouveau signalement - Priorité " + signalement.getPriorite(),
                    "Bonjour,\n\nUn nouveau signalement a été créé :\n\n" +
                    "Salle: " + salleNom + "\n" +
                    "Type: " + signalement.getTypeProbleme() + "\n" +
                    "Priorité: " + signalement.getPriorite() + "\n" +
                    "Description: " + signalement.getDescription() + "\n" +
                    "Signalé par: " + utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom() + "\n\n" +
                    "Connectez-vous à l'application pour traiter ce signalement.");
            }
            
            logger.info("📧 Notification envoyée à {} administrateur(s)", admins.size());
            
        } catch (SQLException e) {
            logger.error("Erreur notification", e);
        }
    }
    
    /**
     * Modifie la priorité d'un signalement.
     * @param signalement le signalement concerné
     */
    private void modifierPriorite(Signalement signalement) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(signalement.getPriorite(), "HAUTE", "MOYENNE", "BASSE");
        dialog.setTitle("Modifier la priorité");
        dialog.setHeaderText("Changer la priorité du signalement #" + signalement.getId());
        
        dialog.showAndWait().ifPresent(nouvellePriorite -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    signalementService.updatePriorite(signalement.getId(), nouvellePriorite);
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    afficherNotification("Succès", "Priorité modifiée en " + nouvellePriorite);
                    chargerTousSignalements();
                }
                
                @Override
                protected void failed() {
                    afficherErreur("Erreur modification");
                }
            };
            new Thread(task).start();
        });
    }
    
    /**
     * Prend en charge un signalement.
     * @param signalement le signalement à prendre en charge
     */
    private void prendreEnCharge(Signalement signalement) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                signalementService.prendreEnCharge(signalement.getId());
                return null;
            }
            
            @Override
            protected void succeeded() {
                afficherNotification("Succès", "Signalement pris en charge");
                chargerTousSignalements();
                chargerStatistiques();
            }
            
            @Override
            protected void failed() {
                afficherErreur("Erreur prise en charge");
            }
        };
        new Thread(task).start();
    }
    
    /**
     * Résout un signalement.
     * @param signalement le signalement à résoudre
     */
    private void resoudreSignalement(Signalement signalement) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Résolution");
        dialog.setHeaderText("Commentaire de résolution");
        
        dialog.showAndWait().ifPresent(commentaire -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    signalementService.resoudre(signalement.getId(), commentaire);
                    notifierEnseignantResolution(signalement);
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    afficherNotification("Succès", "Signalement résolu");
                    chargerTousSignalements();
                    chargerStatistiques();
                }
                
                @Override
                protected void failed() {
                    afficherErreur("Erreur résolution");
                }
            };
            new Thread(task).start();
        });
    }
    
    private void notifierEnseignantResolution(Signalement signalement) {
        try {
            Utilisateur enseignant = utilisateurService.trouverParId(signalement.getUtilisateurId());
            if (enseignant != null) {
                emailService.envoyerEmail(enseignant.getEmail(),
                    "✅ [SCHEDULER] Votre signalement a été résolu",
                    "Bonjour " + enseignant.getPrenom() + " " + enseignant.getNom() + ",\n\n" +
                    "Votre signalement concernant la salle " + getNumeroSalle(signalement.getSalleId()) + " a été résolu.\n\n" +
                    "Résolution: " + signalement.getCommentaireResolution() + "\n\n" +
                    "Cordialement,\nL'équipe SCHEDULER");
                
                notificationService.ajouterNotification(enseignant.getId(),
                    "✅ Votre signalement (Salle " + getNumeroSalle(signalement.getSalleId()) + ") a été résolu.");
            }
        } catch (SQLException e) {
            logger.error("Erreur notification enseignant", e);
        }
    }
    
    private void afficherDetails(Signalement signalement) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du signalement");
        alert.setHeaderText("Signalement #" + signalement.getId());
        
        StringBuilder sb = new StringBuilder();
        sb.append("📅 Date : ").append(signalement.getDateSignalement()).append("\n");
        sb.append("🏫 Salle : ").append(getNumeroSalle(signalement.getSalleId())).append("\n");
        sb.append("🔧 Type : ").append(signalement.getTypeProbleme()).append("\n");
        sb.append("⚠️ Priorité : ").append(signalement.getLibellePriorite()).append("\n");
        sb.append("📌 Statut : ").append(signalement.getStatut()).append("\n");
        sb.append("👤 Signalé par : ").append(getNomUtilisateur(signalement.getUtilisateurId())).append("\n\n");
        sb.append("📝 Description :\n").append(signalement.getDescription()).append("\n");
        
        if (signalement.getCommentaireResolution() != null) {
            sb.append("\n✅ Résolution :\n").append(signalement.getCommentaireResolution()).append("\n");
            if (signalement.getDateResolution() != null) {
                sb.append("📅 Résolu le : ").append(signalement.getDateResolution()).append("\n");
            }
        }
        
        alert.setContentText(sb.toString());
        alert.getDialogPane().setMinWidth(450);
        alert.show();
    }
    
    private void viderFormulaire() {
        if (salleCombo != null) salleCombo.setValue(null);
        if (typeProblemeCombo != null) typeProblemeCombo.setValue(null);
        if (prioriteCombo != null) prioriteCombo.setValue("MOYENNE");
        if (descriptionArea != null) descriptionArea.clear();
        if (salleInfoLabel != null) salleInfoLabel.setText("");
        supprimerPhoto();
    }
    
    private void choisirPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de preuve (panne, incident...)");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images (*.png, *.jpg, *.jpeg, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File fichier = fileChooser.showOpenDialog(salleCombo.getScene().getWindow());
        if (fichier != null) {
            try {
                String chemin = ImageUploadService.sauvegarderPhotoSignalement(fichier);
                cheminPhotoPreuve = chemin;
                Image img = ImageUploadService.chargerImage(chemin, 90, 70);
                if (photoPreview != null && img != null) {
                    photoPreview.setImage(img);
                }
                if (supprimerPhotoButton != null) {
                    supprimerPhotoButton.setVisible(true);
                }
            } catch (IOException ex) {
                logger.error("Erreur enregistrement photo signalement", ex);
                afficherErreur("Impossible d'enregistrer la photo");
            }
        }
    }
    
    private void supprimerPhoto() {
        cheminPhotoPreuve = null;
        if (photoPreview != null) {
            Image defLogo = ImageUploadService.chargerImage("/images/logo.png", 90, 70);
            photoPreview.setImage(defLogo);
        }
        if (supprimerPhotoButton != null) {
            supprimerPhotoButton.setVisible(false);
        }
    }
    
    private void chargerDonnees() {
        chargerMesSignalements();
        if (utilisateurConnecte != null && utilisateurConnecte.getRole().equals("admin")) {
            chargerTousSignalements();
            chargerStatistiques();
        }
    }
    
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (salleCombo != null && salleCombo.getScene() != null) {
            return (Stage) salleCombo.getScene().getWindow();
        }
        return null;
    }
}