package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.concurrent.Task;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.*;
import javafx.scene.control.ButtonBar;
import scheduler.service.PlanningService;
import scheduler.service.CoursService;
import scheduler.service.MatiereService;




public class RechercheControleur extends TableauBordControleur {
    
    @FXML private ToggleGroup rechercheType;
    @FXML private RadioButton maintenantRadio;
    @FXML private RadioButton datePreciseRadio;
    
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> heureDebutCombo;
    @FXML private ComboBox<String> heureFinCombo;
    @FXML private Spinner<Integer> dureeSpinner;
    
    @FXML private Spinner<Integer> capaciteMinSpinner;
    @FXML private ComboBox<String> typeSalleCombo;
    @FXML private ComboBox<Batiment> batimentCombo;
    @FXML private ListView<Equipement> equipementsList;
    
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    
    @FXML private TableView<Salle> resultatsTable;
    @FXML private TableColumn<Salle, String> photoColumn;
    @FXML private TableColumn<Salle, String> numeroColumn;
    @FXML private TableColumn<Salle, Integer> capaciteColumn;
    @FXML private TableColumn<Salle, String> typeColumn;
    @FXML private TableColumn<Salle, String> batimentColumn;
    @FXML private TableColumn<Salle, Integer> etageColumn;
    @FXML private TableColumn<Salle, String> disponibiliteColumn;
    @FXML private TableColumn<Salle, Void> actionsColumn;
    
    @FXML private TextField rechercheRapideField;
    @FXML private Label resultatsCountLabel;
    @FXML private ProgressIndicator chargementIndicator;
    @FXML private Button retourButton;
    
    private javafx.animation.PauseTransition debounceTimer;
    
    private RechercheService rechercheService;
    private SalleService salleService;
    private BatimentService batimentService;
    private EquipementService equipementService;
    private ReservationService reservationService;
    
    private ObservableList<Salle> resultatsList;
    private List<Equipement> tousEquipements;
    private PlanningService planningService;     
    private CoursService coursService;           
    private MatiereService matiereService;       
    
    
    @Override
    public void initialiserAvecUtilisateur(Utilisateur utilisateur, javafx.stage.Stage stage) {
        super.initialiserAvecUtilisateur(utilisateur, stage);
        System.out.println("🔍 RechercheControleur - Utilisateur initialisé: " + (utilisateur != null ? utilisateur.getEmail() : "null"));
        initialiserTableauBord();
    }
    
    @Override
    public void initialize() {
    	if (session != null && session.getUtilisateurConnecte() != null && utilisateurConnecte == null) {
            this.utilisateurConnecte = session.getUtilisateurConnecte();
            System.out.println("✅ RechercheControleur - Utilisateur récupéré depuis session: " + 
                               utilisateurConnecte.getEmail());
        }
        super.initialize();
        
        this.rechercheService = new RechercheService();
        this.salleService = new SalleService();
        this.batimentService = new BatimentService();
        this.equipementService = new EquipementService();
        this.reservationService = new ReservationService();
        this.planningService = new PlanningService();   
        this.coursService = new CoursService();         
        this.matiereService = new MatiereService(); 
        
        this.resultatsList = FXCollections.observableArrayList();
        this.tousEquipements = new ArrayList<>();
        
        javafx.application.Platform.runLater(() -> {
            if (resultatsTable != null) {
                initialiserTableauBord();
            }
        });
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetour());
        }
    }
    
    public void setUtilisateurEtStage(Utilisateur utilisateur, Stage stage) {
        this.utilisateurConnecte = utilisateur;
        this.primaryStage = stage;
        System.out.println("🔍 RechercheControleur.setUtilisateurEtStage()");
        System.out.println("   Utilisateur: " + (utilisateur != null ? utilisateur.getEmail() : "null"));
        System.out.println("   Stage: " + stage);
        
        // Recharger les données après réception
        if (resultatsTable != null) {
            rechercherMaintenant();
        }
    }
    
    @Override
    protected void initialiserTableauBord() {
        if (resultatsTable == null) return;
        configurerRecherche();
        configurerTableau();
        configurerHeures();
        chargerDonneesInitiales();
        
        // Charger les salles par défaut (maintenant)
        rechercherMaintenant();
    }
    
    @Override
    protected void rafraichirDonnees() {
        if (maintenantRadio.isSelected()) {
            rechercherMaintenant();
        }
    }
    
    private void configurerRecherche() {
        if (rechercheType == null) {
            rechercheType = new ToggleGroup();
        }
        if (maintenantRadio != null) {
            maintenantRadio.setToggleGroup(rechercheType);
            maintenantRadio.setSelected(true);
        }
        if (datePreciseRadio != null) {
            datePreciseRadio.setToggleGroup(rechercheType);
        }
        
        if (datePicker != null) datePicker.setDisable(true);
        if (heureDebutCombo != null) heureDebutCombo.setDisable(true);
        if (heureFinCombo != null) heureFinCombo.setDisable(true);
        if (dureeSpinner != null) dureeSpinner.setDisable(true);
        
        // Configuration du debounce timer (300 ms de réactivité sans freeze)
        debounceTimer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        debounceTimer.setOnFinished(e -> handleRechercher());
        
        if (rechercheType != null) {
            rechercheType.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                boolean dateMode = newVal == datePreciseRadio;
                if (datePicker != null) datePicker.setDisable(!dateMode);
                if (heureDebutCombo != null) heureDebutCombo.setDisable(!dateMode);
                if (heureFinCombo != null) heureFinCombo.setDisable(!dateMode);
                if (dureeSpinner != null) dureeSpinner.setDisable(!dateMode);
                if (dateMode && datePicker != null) datePicker.setValue(LocalDate.now());
                if (dateMode && heureDebutCombo != null) {
                    heureDebutCombo.setValue(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:00")));
                }
                declencherRechercheDebounce();
            });
        }
        
        if (rechercheRapideField != null) {
            rechercheRapideField.textProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        
        if (capaciteMinSpinner != null) {
            capaciteMinSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1)
            );
            capaciteMinSpinner.valueProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        if (dureeSpinner != null) {
            dureeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 480, 60, 30)
            );
            dureeSpinner.valueProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        
        if (typeSalleCombo != null) {
            if (typeSalleCombo.getItems().isEmpty()) {
                typeSalleCombo.getItems().addAll("Tous", "TD", "TP", "Amphi", "Autre");
                typeSalleCombo.setValue("Tous");
            }
            typeSalleCombo.valueProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        
        if (batimentCombo != null) {
            batimentCombo.valueProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        if (datePicker != null) {
            datePicker.valueProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        if (heureDebutCombo != null) {
            heureDebutCombo.valueProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        if (heureFinCombo != null) {
            heureFinCombo.valueProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        
        if (equipementsList != null) {
            equipementsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            equipementsList.setCellFactory(lv -> new ListCell<Equipement>() {
                @Override
                protected void updateItem(Equipement item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getNom() + (item.getDescription() != null ? " - " + item.getDescription() : ""));
                    }
                }
            });
            equipementsList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> declencherRechercheDebounce());
        }
        
        if (rechercherButton != null) {
            rechercherButton.setOnAction(e -> handleRechercher());
        }
        if (reinitialiserButton != null) {
            reinitialiserButton.setOnAction(e -> handleReinitialiser());
        }
    }
    
    private void declencherRechercheDebounce() {
        if (debounceTimer != null) {
            debounceTimer.playFromStart();
        }
    }
    
    private void configurerTableau() {
        if (numeroColumn == null) return;
        
        if (photoColumn != null) {
            photoColumn.setCellFactory(col -> new TableCell<Salle, String>() {
                private final javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
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
                        Salle s = getTableRow().getItem();
                        String photo = s.getPhotoUrl();
                        if (photo != null && !photo.isBlank()) {
                            javafx.scene.image.Image img = ImageUploadService.chargerImage(photo, 40, 30);
                            if (img != null) {
                                imgView.setImage(img);
                                setGraphic(imgView);
                                return;
                            }
                        }
                        setGraphic(new Label("🏫"));
                    }
                }
            });
        }
        
        numeroColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getNumero()
            )
        );
        
        capaciteColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createIntegerBinding(
                () -> cellData.getValue().getCapacite()
            ).asObject()
        );
        
        typeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getType()
            )
        );
        
        batimentColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomBatiment(cellData.getValue().getBatimentId())
            )
        );
        
        etageColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createIntegerBinding(
                () -> cellData.getValue().getEtage()
            ).asObject()
        );
        
        disponibiliteColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getStatut().equals("disponible") ? "Disponible" : "Indisponible"
            )
        );
        
        disponibiliteColumn.setCellFactory(column -> new TableCell<Salle, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                } else {
                    if (statut.equals("Disponible")) {
                        setText("🟢 Libre");
                        setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: 800;");
                    } else {
                        setText("🔴 Occupée");
                        setStyle("-fx-text-fill: #9E2A2B; -fx-font-weight: 800;");
                    }
                }
            }
        });
        
        configurerActionsColumn();
        
        resultatsTable.setItems(resultatsList);
        resultatsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    private void configurerActionsColumn() {
        if (actionsColumn == null) return;
        
        actionsColumn.setCellFactory(param -> new TableCell<Salle, Void>() {
            private final Button reserverBtn = new Button("Réserver");
            private final Button detailsBtn = new Button("👁️ Détails");
            
            {
                reserverBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                detailsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                
                reserverBtn.setOnAction(event -> {
                    Salle salle = getTableView().getItems().get(getIndex());
                    naviguerVersReservationSalle(salle);
                });
                
                detailsBtn.setOnAction(event -> {
                    Salle salle = getTableView().getItems().get(getIndex());
                    afficherDetailsSalle(salle);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, detailsBtn, reserverBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }
    
    private void configurerHeures() {
        List<String> heures = new ArrayList<>();
        for (int h = 8; h <= 22; h++) {
            heures.add(String.format("%02d:00", h));
        }
        if (heureDebutCombo != null) {
            heureDebutCombo.getItems().addAll(heures);
            heureDebutCombo.setValue("08:00");
        }
        if (heureFinCombo != null) {
            heureFinCombo.getItems().addAll(heures);
            heureFinCombo.setValue("18:00");
        }
    }
    
    private void chargerDonneesInitiales() {
        if (batimentCombo == null) return;
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                List<Batiment> batiments = batimentService.listerTous();
                javafx.application.Platform.runLater(() -> {
                    if (batimentCombo != null) {
                        batimentCombo.getItems().clear();
                        batimentCombo.getItems().addAll(batiments);
                        batimentCombo.setCellFactory(lv -> new ListCell<Batiment>() {
                            @Override
                            protected void updateItem(Batiment item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(empty ? null : item.getNom());
                            }
                        });
                        batimentCombo.setButtonCell(new ListCell<Batiment>() {
                            @Override
                            protected void updateItem(Batiment item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(empty ? null : item.getNom());
                            }
                        });
                    }
                });
                
                List<Equipement> equipements = equipementService.listerTous();
                tousEquipements = equipements;
                
                javafx.application.Platform.runLater(() -> {
                    if (equipementsList != null) {
                        equipementsList.getItems().setAll(equipements);
                    }
                });
                
                return null;
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement données", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Affiche les détails d'une salle.
     * @param salle la salle à afficher
     */
    private void afficherDetailsSalle(Salle salle) {
        if (salle == null) return;
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la salle");
        alert.setHeaderText("Salle " + salle.getNumero());
        
        StringBuilder sb = new StringBuilder();
        sb.append("Capacité: ").append(salle.getCapacite()).append(" places\n");
        sb.append("Type: ").append(salle.getType()).append("\n");
        sb.append("Bâtiment: ").append(getNomBatiment(salle.getBatimentId())).append("\n");
        sb.append("Étage: ").append(salle.getEtage()).append("\n");
        sb.append("Statut: ").append(salle.getStatut()).append("\n");
        sb.append("Occupation actuelle: ").append(getDetailOccupation(salle)).append("\n");
        
        if (salle.getEquipements() != null && !salle.getEquipements().isEmpty()) {
            sb.append("\nÉquipements:\n");
            for (Integer eqId : salle.getEquipements()) {
                for (Equipement e : tousEquipements) {
                    if (e.getId() == eqId) {
                        sb.append("  • ").append(e.getNom()).append("\n");
                        break;
                    }
                }
            }
        }
        
        alert.setContentText(sb.toString());
        
        // Bouton Voir planning
        ButtonType planningBtn = new ButtonType("📅 Voir planning", ButtonBar.ButtonData.OK_DONE);
        ButtonType reserverBtn = new ButtonType("📅 Réserver", ButtonBar.ButtonData.OK_DONE);
        ButtonType fermerBtn = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        boolean peutReserver = utilisateurConnecte != null &&
            (utilisateurConnecte.getRole().equals("admin") ||
             utilisateurConnecte.getRole().equals("gestionnaire") ||
             utilisateurConnecte.getRole().equals("enseignant") ||
             (utilisateurConnecte instanceof Etudiant &&
              "responsable".equals(((Etudiant) utilisateurConnecte).getTypeEtudiant())));
        
        if (peutReserver) {
            alert.getButtonTypes().setAll(planningBtn, reserverBtn, fermerBtn);
        } else {
            alert.getButtonTypes().setAll(planningBtn, fermerBtn);
        }
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == planningBtn) {
            ouvrirPlanningSalle(salle);
        } else if (result.isPresent() && result.get() == reserverBtn && peutReserver) {
            ouvrirReservationDirecte(salle);
        }
    }
    
    /**
     * Obtient la couleur de disponibilité d'une salle pour l'affichage
     */
    private String getCouleurDisponibilite(Salle salle) {
        try {
            String maintenant = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String aujourdhui = LocalDate.now().toString();
            
            // Vérifier si la salle est occupée actuellement
            List<Creneau> creneaux = planningService.getCreneauxParSalle(salle.getId());
            for (Creneau c : creneaux) {
                if (c.getJour().equals(aujourdhui) && !"annule".equals(c.getStatut())) {
                    if (c.getHeureDebut().compareTo(maintenant) <= 0 && 
                        c.getHeureFin().compareTo(maintenant) > 0) {
                        return "#f44336"; // Rouge - occupée
                    }
                    // Vérifier si bientôt (dans 30 min)
                    String[] parts = c.getHeureDebut().split(":");
                    int heure = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    LocalTime debut = LocalTime.of(heure, minute);
                    LocalTime now = LocalTime.now();
                    if (debut.isAfter(now) && debut.minusMinutes(30).isBefore(now)) {
                        return "#FF9800"; // Orange - bientôt occupée
                    }
                }
            }
            
            // Vérifier les réservations
            List<Reservation> reservations = reservationService.getReservationsJour(aujourdhui);
            for (Reservation r : reservations) {
                if (r.getSalleId() == salle.getId()) {
                    if (r.getHeureDebut().compareTo(maintenant) <= 0 && 
                        r.getHeureFin().compareTo(maintenant) > 0) {
                        return "#f44336"; // Rouge - occupée
                    }
                }
            }
            
            if ("indisponible".equals(salle.getStatut())) {
                return "#9E9E9E"; // Gris - indisponible
            }
            return "#4CAF50"; // Vert - libre
            
        } catch (SQLException e) {
            logger.error("Erreur couleur disponibilité", e);
            return "#4CAF50";
        }
    }
    
    /**
     * Obtient le texte d'occupation détaillé
     */
    private String getDetailOccupation(Salle salle) {
        try {
            String maintenant = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String aujourdhui = LocalDate.now().toString();
            
            // Vérifier les cours
            List<Creneau> creneaux = planningService.getCreneauxParSalle(salle.getId());
            for (Creneau c : creneaux) {
                if (c.getJour().equals(aujourdhui) && !"annule".equals(c.getStatut())) {
                    if (c.getHeureDebut().compareTo(maintenant) <= 0 && 
                        c.getHeureFin().compareTo(maintenant) > 0) {
                        Cours cours = coursService.trouverParId(c.getCoursId());
                        if (cours != null) {
                            Matiere m = matiereService.trouverParId(cours.getMatiereId());
                            return "Occupé: " + (m != null ? m.getNom() : "Cours") + 
                                   " jusqu'à " + c.getHeureFin();
                        }
                        return "Occupé jusqu'à " + c.getHeureFin();
                    }
                    // Vérifier si bientôt
                    String[] parts = c.getHeureDebut().split(":");
                    int heure = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    LocalTime debut = LocalTime.of(heure, minute);
                    LocalTime now = LocalTime.now();
                    if (debut.isAfter(now) && debut.minusMinutes(30).isBefore(now)) {
                        return "Libre dans " + (debut.getHour() - now.getHour()) + "h";
                    }
                }
            }
            
            // Vérifier les réservations
            List<Reservation> reservations = reservationService.getReservationsJour(aujourdhui);
            for (Reservation r : reservations) {
                if (r.getSalleId() == salle.getId()) {
                    if (r.getHeureDebut().compareTo(maintenant) <= 0 && 
                        r.getHeureFin().compareTo(maintenant) > 0) {
                        return "Réservé jusqu'à " + r.getHeureFin();
                    }
                }
            }
            
            if ("indisponible".equals(salle.getStatut())) {
                return "Indisponible (travaux)";
            }
            return "Libre";
            
        } catch (SQLException e) {
            logger.error("Erreur détail occupation", e);
            return "Libre";
        }
    }
    
    private void ouvrirPlanningSalle(Salle salle) {
        try {
            Alert planningAlert = new Alert(Alert.AlertType.INFORMATION);
            planningAlert.setTitle("Planning - Salle " + salle.getNumero());
            planningAlert.setHeaderText("Salle " + salle.getNumero() + " - " + salle.getType());
            
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            
            // Période sélectionnable
            HBox periodeBox = new HBox(10);
            DatePicker debutPicker = new DatePicker(LocalDate.now());
            DatePicker finPicker = new DatePicker(LocalDate.now().plusDays(6));
            Button actualiserBtn = new Button("Actualiser");
            periodeBox.getChildren().addAll(new Label("Du:"), debutPicker, new Label("au:"), finPicker, actualiserBtn);
            
            // Tableau des créneaux
            TableView<Map<String, String>> planningTable = new TableView<>();
            TableColumn<Map<String, String>, String> jourCol = new TableColumn<>("Jour");
            TableColumn<Map<String, String>, String> horaireCol = new TableColumn<>("Horaire");
            TableColumn<Map<String, String>, String> occupationCol = new TableColumn<>("Occupation");
            
            jourCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().get("jour")));
            horaireCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().get("horaire")));
            occupationCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().get("occupation")));
            
            planningTable.getColumns().addAll(jourCol, horaireCol, occupationCol);
            planningTable.setPrefHeight(300);
            
            // Charger le planning
            Runnable chargerPlanning = () -> {
                try {
                    List<Map<String, String>> creneaux = new ArrayList<>();
                    LocalDate debut = debutPicker.getValue();
                    LocalDate fin = finPicker.getValue();
                    
                    List<Creneau> coursSalle = planningService.getCreneauxParSalle(salle.getId());
                    List<Reservation> reservations = reservationService.getReservationsSalle(salle.getId());
                    
                    LocalDate current = debut;
                    while (!current.isAfter(fin)) {
                        String dateStr = current.toString();
                        for (int h = 8; h < 18; h++) {
                            String heureDebut = String.format("%02d:00", h);
                            String heureFin = String.format("%02d:00", h + 2);
                            
                            boolean occupe = false;
                            String detail = "";
                            
                            // Vérifier cours
                            for (Creneau c : coursSalle) {
                                if (c.getJour().equals(dateStr) && !"annule".equals(c.getStatut())) {
                                    if (c.getHeureDebut().equals(heureDebut)) {
                                        occupe = true;
                                        try {
                                            Cours cours = coursService.trouverParId(c.getCoursId());
                                            if (cours != null) {
                                                Matiere m = matiereService.trouverParId(cours.getMatiereId());
                                                detail = "Cours: " + (m != null ? m.getNom() : "?");
                                            }
                                        } catch (SQLException ex) {
                                            logger.error("Erreur récupération cours", ex);
                                            detail = "Cours";
                                        }
                                        break;
                                    }
                                }
                            }
                            
                            // Vérifier réservations
                            for (Reservation r : reservations) {
                                if (r.getDateReservation().equals(dateStr) && r.getHeureDebut().equals(heureDebut)) {
                                    occupe = true;
                                    detail = "Réservé: " + r.getMotif();
                                    break;
                                }
                            }
                            
                            Map<String, String> creneau = new HashMap<>();
                            creneau.put("jour", current.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                            creneau.put("horaire", heureDebut + " - " + heureFin);
                            creneau.put("occupation", occupe ? "🔴 " + detail : "🟢 Libre");
                            creneaux.add(creneau);
                        }
                        current = current.plusDays(1);
                    }
                    
                    planningTable.setItems(FXCollections.observableArrayList(creneaux));
                    
                } catch (Exception e) {
                    logger.error("Erreur chargement planning", e);
                }
            };
            
            actualiserBtn.setOnAction(e -> chargerPlanning.run());
            chargerPlanning.run();
            
            content.getChildren().addAll(periodeBox, planningTable);
            
            // Bouton réserver si autorisé
            boolean peutReserver = utilisateurConnecte != null &&
                (utilisateurConnecte.getRole().equals("admin") ||
                 utilisateurConnecte.getRole().equals("gestionnaire") ||
                 utilisateurConnecte.getRole().equals("enseignant") ||
                 (utilisateurConnecte instanceof Etudiant &&
                  "responsable".equals(((Etudiant) utilisateurConnecte).getTypeEtudiant())));
            
            if (peutReserver) {
                Button reserverBtn = new Button("📅 Réserver cette salle");
                reserverBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
                reserverBtn.setOnAction(e -> {
                    planningAlert.close();
                    naviguerVersReservationSalle(salle);
                });
                content.getChildren().add(reserverBtn);
            }
            
            planningAlert.getDialogPane().setContent(content);
            planningAlert.getDialogPane().setPrefWidth(650);
            planningAlert.getDialogPane().setPrefHeight(500);
            planningAlert.show();
            
        } catch (Exception e) {
            logger.error("Erreur ouverture planning", e);
            afficherErreur("Impossible de charger le planning");
        }
    }
    
    private String getNomBatiment(int batimentId) {
        if (batimentCombo == null) return "Inconnu";
        for (Batiment b : batimentCombo.getItems()) {
            if (b.getId() == batimentId) {
                return b.getNom();
            }
        }
        return "Inconnu";
    }
    
    /**
     * Effectue la recherche selon le mode sélectionné.
     */
    @FXML
    private void handleRechercher() {
        if (maintenantRadio.isSelected()) {
            rechercherMaintenant();
        } else {
            rechercherDatePrecise();
        }
    }
    
    private void rechercherMaintenant() {
        Task<List<Salle>> task = new Task<>() {
            @Override
            protected List<Salle> call() throws SQLException {
                List<Salle> salles = rechercheService.rechercherSallesMaintenant();
                return appliquerFiltres(salles);
            }
            
            @Override
            protected void succeeded() {
                resultatsList.setAll(getValue());
                resultatsCountLabel.setText(getValue().size() + " salle(s) trouvée(s)");
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur recherche", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                afficherErreur("Erreur lors de la recherche");
            }
        };
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        new Thread(task).start();
    }
    
    /**
     * Recherche les salles disponibles à une date précise.
     */
    private void rechercherDatePrecise() {
        if (datePicker.getValue() == null || heureDebutCombo.getValue() == null) {
            afficherErreur("Veuillez remplir tous les champs");
            return;
        }
        
        String date = datePicker.getValue().toString();
        String heureDebut = heureDebutCombo.getValue();
        String heureFin = heureFinCombo.getValue() != null ? heureFinCombo.getValue() : 
            calculerHeureFin(heureDebut, dureeSpinner.getValue());
        
        Task<List<Salle>> task = new Task<>() {
            @Override
            protected List<Salle> call() throws SQLException {
                List<Salle> salles = rechercheService.rechercherSallesLibres(date, heureDebut, heureFin);
                return appliquerFiltres(salles);
            }
            
            @Override
            protected void succeeded() {
                resultatsList.setAll(getValue());
                resultatsCountLabel.setText(getValue().size() + " salle(s) trouvée(s)");
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur recherche", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                afficherErreur("Erreur lors de la recherche");
            }
        };
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        new Thread(task).start();
    }
    
    private String calculerHeureFin(String heureDebut, int duree) {
        String[] parts = heureDebut.split(":");
        int heures = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        
        int totalMinutes = heures * 60 + minutes + duree;
        heures = totalMinutes / 60;
        minutes = totalMinutes % 60;
        
        return String.format("%02d:%02d", heures, minutes);
    }
    
    /**
     * Applique les filtres à la liste des salles.
     * @param salles la liste initiale
     * @return la liste filtrée
     */
    private List<Salle> appliquerFiltres(List<Salle> salles) {
        List<Salle> filtrees = new ArrayList<>(salles);
        
        // ✅ Vérifier que le spinner n'est pas null
        if (capaciteMinSpinner != null && capaciteMinSpinner.getValue() != null) {
            int capaciteMin = capaciteMinSpinner.getValue();
            filtrees.removeIf(s -> s.getCapacite() < capaciteMin);
        }
        
        if (typeSalleCombo != null && typeSalleCombo.getValue() != null) {
            String type = typeSalleCombo.getValue();
            if (!"Tous".equals(type)) {
                filtrees.removeIf(s -> !s.getType().equals(type));
            }
        }
        
        if (batimentCombo != null && batimentCombo.getValue() != null) {
            Batiment batiment = batimentCombo.getValue();
            filtrees.removeIf(s -> s.getBatimentId() != batiment.getId());
        }
        
        if (equipementsList != null && equipementsList.getSelectionModel() != null) {
            List<Equipement> equipementsSelectionnes = new ArrayList<>(equipementsList.getSelectionModel().getSelectedItems());
            if (!equipementsSelectionnes.isEmpty()) {
                filtrees.removeIf(s -> {
                    List<Integer> eqIds = s.getEquipements();
                    for (Equipement e : equipementsSelectionnes) {
                        if (!eqIds.contains(e.getId())) {
                            return true;
                        }
                    }
                    return false;
                });
            }
        }
        
        if (rechercheRapideField != null && rechercheRapideField.getText() != null) {
            String query = rechercheRapideField.getText().trim().toLowerCase();
            if (!query.isEmpty()) {
                filtrees.removeIf(s -> {
                    boolean matchNumero = s.getNumero() != null && s.getNumero().toLowerCase().contains(query);
                    boolean matchType = s.getType() != null && s.getType().toLowerCase().contains(query);
                    String batName = getNomBatiment(s.getBatimentId()).toLowerCase();
                    boolean matchBat = batName.contains(query);
                    return !matchNumero && !matchType && !matchBat;
                });
            }
        }
        
        return filtrees;
    }
    
    @FXML
    private void handleReinitialiser() {
        if (rechercheRapideField != null) rechercheRapideField.clear();
        capaciteMinSpinner.getValueFactory().setValue(1);
        typeSalleCombo.setValue("Tous");
        batimentCombo.setValue(null);
        equipementsList.getSelectionModel().clearSelection();
        
        if (maintenantRadio.isSelected()) {
            rechercherMaintenant();
        }
    }
    
    /**
     * Ouvre la réservation directe avec la salle pré-sélectionnée.
     * @param salle la salle à réserver
     */
    private void ouvrirReservationDirecte(Salle salle) {
        try {
            Stage stageAAfficher = primaryStage;
            if (stageAAfficher == null) {
                if (resultatsTable != null && resultatsTable.getScene() != null) {
                    stageAAfficher = (Stage) resultatsTable.getScene().getWindow();
                } else if (rechercherButton != null && rechercherButton.getScene() != null) {
                    stageAAfficher = (Stage) rechercherButton.getScene().getWindow();
                } else if (reinitialiserButton != null && reinitialiserButton.getScene() != null) {
                    stageAAfficher = (Stage) reinitialiserButton.getScene().getWindow();
                } else if (capaciteMinSpinner != null && capaciteMinSpinner.getScene() != null) {
                    stageAAfficher = (Stage) capaciteMinSpinner.getScene().getWindow();
                }
            }
            
            if (stageAAfficher == null) {
                System.err.println("❌ Impossible de récupérer le stage");
                afficherErreur("Erreur de navigation : stage non trouvé");
                return;
            }
            
            if (utilisateurConnecte == null) {
                System.err.println("❌ Utilisateur non connecté");
                afficherErreur("Session expirée. Veuillez vous reconnecter.");
                retourLogin();
                return;
            }
            
            System.out.println("🔍 Navigation vers Réservation depuis Recherche");
            System.out.println("   Stage: " + stageAAfficher);
            System.out.println("   Utilisateur: " + utilisateurConnecte.getEmail());
            System.out.println("   Salle: " + salle.getNumero());
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Reservation.fxml"));
            Parent root = loader.load();
            
            ReservationControleur controleur = loader.getController();
            
            // Initialiser avec utilisateur et stage
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, stageAAfficher);
            
            // Marquer qu'on vient de la recherche pour un retour correct
            controleur.setProvientRecherche(true);
            
            // Pré-sélectionner la salle et éventuellement le créneau
            if (datePreciseRadio != null && datePreciseRadio.isSelected() && datePicker != null && datePicker.getValue() != null) {
                String date = datePicker.getValue().toString();
                String heureDebut = heureDebutCombo != null && heureDebutCombo.getValue() != null ? 
                                    heureDebutCombo.getValue() : "08:00";
                String heureFin = heureFinCombo != null && heureFinCombo.getValue() != null ? 
                                  heureFinCombo.getValue() : "10:00";
                controleur.preselectionnerCreneau(salle, date, heureDebut, heureFin);
            } else {
                controleur.preselectionnerSalle(salle);
            }
            
            Scene scene = new Scene(root);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            
            stageAAfficher.setScene(scene);
            stageAAfficher.setTitle("SCHEDULER - Réservation de salle");
            
        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture réservation: " + e.getMessage());
            e.printStackTrace();
            afficherErreur("Impossible d'ouvrir la page de réservation");
        }
    }
    
    /**
     * Navigue vers la page de réservation avec la salle pré-sélectionnée
     */
    private void naviguerVersReservationSalle(Salle salle) {
        if (utilisateurConnecte == null) {
            afficherErreur("Session expirée. Veuillez vous reconnecter.");
            retourLogin();
            return;
        }
        
        try {
            System.out.println("🔍 Navigation vers Réservation depuis Recherche");
            System.out.println("   Salle: " + salle.getNumero());
            
            // ✅ Récupérer le stage
            Stage stage = getStageFromScene();
            if (stage == null) {
                afficherErreur("Erreur de navigation: stage non trouvé");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Reservation.fxml"));
            Parent root = loader.load();
            
            ReservationControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, stage);
            controleur.setProvientRecherche(true);
            controleur.setProvientCarte(false);
            controleur.preselectionnerSalle(salle);
            
            // ✅ CHARGER DANS LE CONTENU PRINCIPAL
            StackPane contenuPrincipal = getContenuPrincipal();
            if (contenuPrincipal != null) {
                contenuPrincipal.getChildren().clear();
                contenuPrincipal.getChildren().add(root);
                System.out.println("✅ Réservation chargée dans contenuPrincipal");
            } else {
                System.err.println("❌ contenuPrincipal introuvable");
                afficherErreur("Erreur de navigation: conteneur non trouvé");
            }
            
        } catch (IOException e) {
            logger.error("Erreur navigation réservation", e);
            afficherErreur("Impossible d'ouvrir la page de réservation");
        }
    }
    
    protected StackPane getContenuPrincipal() {
        if (resultatsTable != null && resultatsTable.getScene() != null) {
            Parent root = resultatsTable.getScene().getRoot();
            Node found = root.lookup("#contenuPrincipal");
            if (found instanceof StackPane) {
                return (StackPane) found;
            }
        }
        return null;
    }
    
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (resultatsTable != null && resultatsTable.getScene() != null) {
            return (Stage) resultatsTable.getScene().getWindow();
        }
        return null;
    }


    /**
     * Trouve le contenuPrincipal dans la hiérarchie des parents.
     */
    private StackPane trouverContenuPrincipal() {
        // Méthode 1: Chercher par ID dans toute la scène
        if (resultatsTable != null && resultatsTable.getScene() != null) {
            Parent root = resultatsTable.getScene().getRoot();
            Node found = root.lookup("#contenuPrincipal");
            if (found instanceof StackPane) {
                System.out.println("✅ contenuPrincipal trouvé via lookup");
                return (StackPane) found;
            }
            
            // Méthode 2: Parcourir la hiérarchie manuellement
            if (root instanceof BorderPane) {
                BorderPane bp = (BorderPane) root;
                if (bp.getCenter() instanceof StackPane) {
                    System.out.println("✅ contenuPrincipal trouvé via BorderPane center");
                    return (StackPane) bp.getCenter();
                }
                if (bp.getCenter() instanceof SplitPane) {
                    SplitPane sp = (SplitPane) bp.getCenter();
                    if (sp.getItems().size() > 1 && sp.getItems().get(1) instanceof StackPane) {
                        System.out.println("✅ contenuPrincipal trouvé via SplitPane");
                        return (StackPane) sp.getItems().get(1);
                    }
                }
            }
        }
        System.out.println("⚠️ contenuPrincipal non trouvé");
        return null;
    }


    @Override
    protected void handleRetour() {
        if (utilisateurConnecte == null) {
            logger.error("utilisateurConnecte est null dans handleRetour()");
            retourLogin();
            return;
        }
        
        try {
            Stage stageAAfficher = primaryStage;
            if (stageAAfficher == null) {
                if (resultatsTable != null && resultatsTable.getScene() != null) {
                    stageAAfficher = (Stage) resultatsTable.getScene().getWindow();
                }
            }
            
            if (stageAAfficher == null) {
                logger.error("❌ Impossible de récupérer le stage");
                afficherErreur("Erreur de navigation");
                return;
            }
            
            String fxmlFile;
            switch (utilisateurConnecte.getRole()) {
                case "admin":
                    fxmlFile = "/fxml/TableauBordAdmin.fxml";
                    break;
                case "gestionnaire":
                    fxmlFile = "/fxml/TableauBordGestionnaire.fxml";
                    break;
                case "enseignant":
                    fxmlFile = "/fxml/TableauBordEnseignant.fxml";
                    break;
                case "etudiant":
                    fxmlFile = "/fxml/TableauBordEtudiant.fxml";
                    break;
                default:
                    fxmlFile = "/fxml/TableauBordAdmin.fxml";
            }
            
            System.out.println("🔍 Retour vers " + fxmlFile + " pour rôle: " + utilisateurConnecte.getRole());
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            
            Object controleur = loader.getController();
            if (controleur instanceof TableauBordControleur) {
                ((TableauBordControleur) controleur).initialiserAvecUtilisateur(utilisateurConnecte, stageAAfficher);
            }
            
            // ✅ CHARGER DANS LE CONTENU PRINCIPAL
            StackPane contenuPrincipal = trouverContenuPrincipal();
            if (contenuPrincipal != null) {
                contenuPrincipal.getChildren().clear();
                contenuPrincipal.getChildren().add(root);
            } else {
                // Fallback
                Scene scene = new Scene(root);
                String css = getClass().getResource("/css/style.css").toExternalForm();
                if (css != null) scene.getStylesheets().add(css);
                stageAAfficher.setScene(scene);
            }
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner au tableau de bord");
        }
    }
}