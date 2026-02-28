package scheduler.controleur;

import scheduler.modele.*;
import scheduler.dao.ConnexionBD;
import scheduler.dao.EmploiDuTempsDAO;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * Création et gestion des emplois du temps.
 */
public class PlanningControleur extends TableauBordControleur {
    
    @FXML private ComboBox<Classe> classeFilterCombo;
    @FXML private DatePicker datePicker;
    @FXML private ToggleGroup vueGroup;
    @FXML private RadioButton vueJourRadio;
    @FXML private RadioButton vueSemaineRadio;
    @FXML private RadioButton vueMoisRadio;
    
    @FXML private GridPane planningGrid;
    @FXML private TableView<Creneau> detailsTable;
    @FXML private TableColumn<Creneau, String> jourColumn;
    @FXML private TableColumn<Creneau, String> heureColumn;
    @FXML private TableColumn<Creneau, String> coursColumn;
    @FXML private TableColumn<Creneau, String> salleColumn;
    @FXML private TableColumn<Creneau, String> enseignantColumn;
    @FXML private TableColumn<Creneau, String> matiereColumn;
    @FXML private TableColumn<Creneau, String> classeColumn;
    @FXML private TableColumn<Creneau, Void> actionsColumn;
    
    @FXML private Label periodeLabel;
    @FXML private Label totalCoursLabel;
    @FXML private Label infoLabel;
    @FXML private Label classeTitreLabel;
    @FXML private Label periodeDetailLabel;
    @FXML private HBox legendeBox;
    @FXML private Button precedentButton;
    @FXML private Button suivantButton;
    @FXML private Button aujourdhuiButton;
    @FXML private Button verifierConflitsButton;
    @FXML private Button exporterPDFButton;
    @FXML private Button exporterExcelButton;
    @FXML private Button retourButton;
    @FXML private Button modifierButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private PlanningService planningService;
    private ClasseService classeService;
    private CoursService coursService;
    private ConflitService conflitService;
    private ExportService exportService;
    private SalleService salleService;
    private UtilisateurService utilisateurService;
    private MatiereService matiereService;
    private EnseignantService enseignantService;

    private LocalDate dateCourante;
    private List<Creneau> creneauxActuels;
    private Map<String, String> couleurParTypeCours;
    protected EmploiDuTemps edtCharge;
    protected boolean modeLectureSeule = false;
    protected boolean estGestionnaire = false;
    private String origineRole = "gestionnaire";
    
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.planningService = new PlanningService();
        this.classeService = new ClasseService();
        this.coursService = new CoursService();
        this.conflitService = new ConflitService();
        this.exportService = new ExportService();
        this.salleService = new SalleService();
        this.utilisateurService = new UtilisateurService();
        this.matiereService = new MatiereService();
        this.enseignantService = new EnseignantService();
        
        this.creneauxActuels = new ArrayList<>();
        this.couleurParTypeCours = new HashMap<String, String>();
        
        couleurParTypeCours.put("CM", "#4361EE");   
        couleurParTypeCours.put("TD", "#2DC653");   
        couleurParTypeCours.put("TP", "#F4A261");   
        couleurParTypeCours.put("default", "#7B83C4");
    }
    
    @Override
    protected void initialiserTableauBord() {
        configurerComposants();
        configurerTableauDetails();
        chargerClasses();
        creerLegende();
        
        dateCourante = LocalDate.now();
        mettreAJourPeriode();
        chargerPlanning();
    }
    
    @Override
    public void initialiserAvecUtilisateur(Utilisateur utilisateur, Stage stage) {
        super.initialiserAvecUtilisateur(utilisateur, stage);
        this.estGestionnaire = "gestionnaire".equals(utilisateur.getRole());
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerPlanning();
    }
    
    private void configurerComposants() {
        vueJourRadio.setSelected(true);
        
        precedentButton.setOnAction(e -> {
            if (vueJourRadio.isSelected()) {
                dateCourante = dateCourante.minusDays(1);
            } else if (vueSemaineRadio.isSelected()) {
                dateCourante = dateCourante.minusWeeks(1);
            } else {
                dateCourante = dateCourante.minusMonths(1);
            }
            mettreAJourPeriode();
            chargerPlanning();
        });
        
        suivantButton.setOnAction(e -> {
            if (vueJourRadio.isSelected()) {
                dateCourante = dateCourante.plusDays(1);
            } else if (vueSemaineRadio.isSelected()) {
                dateCourante = dateCourante.plusWeeks(1);
            } else {
                dateCourante = dateCourante.plusMonths(1);
            }
            mettreAJourPeriode();
            chargerPlanning();
        });
        
        aujourdhuiButton.setOnAction(e -> {
            dateCourante = LocalDate.now();
            mettreAJourPeriode();
            chargerPlanning();
        });
        
        classeFilterCombo.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    mettreAJourTitreClasse(newVal);
                }
                chargerPlanning();
            });
        
        if (datePicker != null) {
            datePicker.setValue(dateCourante);
            datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    dateCourante = newVal;
                    mettreAJourPeriode();
                    chargerPlanning();
                }
            });
        }
        
        vueGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            mettreAJourPeriode();
            chargerPlanning();
        });
        
        verifierConflitsButton.setOnAction(e -> verifierTousLesConflits());
        
        if (exporterPDFButton != null) {
            exporterPDFButton.setOnAction(e -> exporterPDF());
        }
        
        if (exporterExcelButton != null) {
            exporterExcelButton.setOnAction(e -> exporterExcel());
        }
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> retourAListe());
        }
        
        if (modifierButton != null) {
            modifierButton.setOnAction(e -> modifierEmploiDuTemps());
            modifierButton.setVisible(false);
        }
        
        if (modeLectureSeule) {
            verifierConflitsButton.setVisible(false);
            if (modifierButton != null) modifierButton.setVisible(false);
        }
    }
    
    public List<Creneau> getCreneauxParEnseignant(int enseignantId) throws SQLException {
        List<Creneau> resultats = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            
            String sql = "SELECT c.* FROM creneaux c " +
                         "INNER JOIN cours co ON c.cours_id = co.id " +
                         "WHERE co.enseignant_id = ? " +
                         "ORDER BY c.jour, c.heure_debut";
            
            System.out.println("🔍 getCreneauxParEnseignant - Enseignant ID: " + enseignantId);
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, enseignantId);
            rs = stmt.executeQuery();
            
            int compteur = 0;
            while (rs.next()) {
                Creneau creneau = new Creneau();
                creneau.setId(rs.getInt("id"));
                creneau.setCoursId(rs.getInt("cours_id"));
                creneau.setJour(rs.getString("jour"));
                creneau.setHeureDebut(rs.getString("heure_debut"));
                creneau.setHeureFin(rs.getString("heure_fin"));
                creneau.setSalleId(rs.getInt("salle_id"));
                if (rs.wasNull()) creneau.setSalleId(null);
                creneau.setStatut(rs.getString("statut"));
                creneau.setMotifAnnulation(rs.getString("motif_annulation"));
                resultats.add(creneau);
                compteur++;
            }
            
            System.out.println("   ✅ " + compteur + " créneaux trouvés");
            return resultats;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur getCreneauxParEnseignant: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private void creerLegende() {
        if (legendeBox == null) return;

        legendeBox.getChildren().clear();
        legendeBox.setSpacing(25);
        legendeBox.setAlignment(Pos.CENTER_LEFT);
        legendeBox.setPadding(new Insets(8, 0, 8, 0));

        legendeBox.getChildren().addAll(
        	    creerItemLegende("CM", "#4361EE", "Cours Magistral"),
        	    creerItemLegende("TD", "#2DC653", "Travaux Dirigés"),
        	    creerItemLegende("TP", "#F4A261", "Travaux Pratiques")
        	);
    }

    private HBox creerItemLegende(String type, String couleur, String libelle) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Rectangle rect = new Rectangle(18, 18);
        rect.setFill(Color.web(couleur));
        rect.setArcWidth(4); rect.setArcHeight(4);

        Label typeLbl = new Label(type);
        typeLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + couleur + ";");

        Label libLbl = new Label(libelle);
        libLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A5568;");

        box.getChildren().addAll(rect, typeLbl, libLbl);
        return box;
    }
    
    private void mettreAJourTitreClasse(Classe classe) {
        if (classeTitreLabel != null) {
            classeTitreLabel.setText(classe.getIntitule() + " (" + classe.getAnneeScolaire() + ")");
        }
    }
    
    private void chargerClasses() {
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() throws SQLException {
                return classeService.listerToutes();
            }
            
            @Override
            protected void succeeded() {
                classeFilterCombo.setItems(FXCollections.observableArrayList(getValue()));
                classeFilterCombo.getItems().add(0, null);
                classeFilterCombo.setPromptText("Toutes les classes");
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement classes", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private void configurerTableauDetails() {
        jourColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> formaterDate(cellData.getValue().getJour())
            )
        );
        
        heureColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getHeureDebut() + " - " + 
                      cellData.getValue().getHeureFin()
            )
        );
        
        matiereColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomMatiere(cellData.getValue().getCoursId())
            )
        );
        
        enseignantColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomEnseignant(cellData.getValue().getCoursId())
            )
        );
        
        salleColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNumeroSalle(cellData.getValue().getSalleId())
            )
        );
        
        classeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomClasse(cellData.getValue().getCoursId())
            )
        );
        
        coursColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> "Cours #" + cellData.getValue().getCoursId()
            )
        );
        
        actionsColumn.setCellFactory(param -> new TableCell<Creneau, Void>() {
            private final Button detailsBtn = new Button("👁️");
            
            {
                detailsBtn.setOnAction(event -> {
                    Creneau creneau = getTableView().getItems().get(getIndex());
                    afficherDetailsCreneau(creneau);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : detailsBtn);
            }
        });
    }
    
    private String getNomMatiere(int coursId) {
        try {
            Cours cours = coursService.trouverParId(coursId);
            if (cours != null) {
                Matiere matiere = matiereService.trouverParId(cours.getMatiereId());
                return matiere != null ? matiere.getNom() : "Matière inconnue";
            }
        } catch (SQLException e) {
            logger.error("Erreur récupération matière", e);
        }
        return "N/A";
    }
    
    private String getNomEnseignant(int coursId) {
        try {
            Cours cours = coursService.trouverParId(coursId);
            if (cours != null) {
                Utilisateur user = utilisateurService.trouverParId(cours.getEnseignantId());
                if (user instanceof Enseignant) {
                    return user.getPrenom() + " " + user.getNom();
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur récupération enseignant", e);
        }
        return "N/A";
    }
    
    private String getNomClasse(int coursId) {
        try {
            Cours cours = coursService.trouverParId(coursId);
            if (cours != null) {
                Classe classe = classeService.trouverParId(cours.getClasseId());
                return classe != null ? classe.getIntitule() : "Classe inconnue";
            }
        } catch (SQLException e) {
            logger.error("Erreur récupération classe", e);
        }
        return "N/A";
    }
    
    private String getNumeroSalle(Integer salleId) {
        if (salleId == null) return "Non assignée";
        try {
            Salle salle = salleService.trouverParId(salleId);
            return salle != null ? salle.getNumero() : "Salle inconnue";
        } catch (SQLException e) {
            logger.error("Erreur récupération salle", e);
        }
        return "Erreur";
    }
    
    private void mettreAJourPeriode() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        if (vueJourRadio.isSelected()) {
            periodeLabel.setText(dateCourante.format(formatter));
        } else if (vueSemaineRadio.isSelected()) {
            LocalDate debutSemaine = dateCourante.minusDays(dateCourante.getDayOfWeek().getValue() - 1);
            LocalDate finSemaine = debutSemaine.plusDays(4);
            periodeLabel.setText("Semaine du " + debutSemaine.format(formatter) + " au " + finSemaine.format(formatter));
        } else {
            periodeLabel.setText(dateCourante.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        }
    }
    
    private void chargerPlanning() {
        if (modeLectureSeule && edtCharge != null) {
            chargerPlanningEDT();
            return;
        }
        
        final LocalDate dateParam = datePicker != null && datePicker.getValue() != null ? datePicker.getValue() : dateCourante;
        Task<List<Creneau>> task = new Task<>() {
            @Override
            protected List<Creneau> call() throws SQLException {
                if (vueJourRadio.isSelected()) {
                    return planningService.getCreneauxParJour(dateParam.toString());
                } else if (vueSemaineRadio.isSelected()) {
                    return planningService.getCreneauxParSemaine(dateParam);
                } else {
                    return planningService.getCreneauxParMois(dateParam);
                }
            }
            
            @Override
            protected void succeeded() {
                creneauxActuels = filtrerParClasse(getValue());
                afficherPlanning();
                if (detailsTable != null) detailsTable.setItems(FXCollections.observableArrayList(creneauxActuels));
                totalCoursLabel.setText(creneauxActuels.size() + " cours");
                if (infoLabel != null) infoLabel.setText("");
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement planning", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerPlanningEDT() {
        Task<List<Creneau>> task = new Task<>() {
            @Override
            protected List<Creneau> call() throws SQLException {
                return planningService.getCreneauxParEmploiDuTemps(edtCharge.getId());
            }
            
            @Override
            protected void succeeded() {
                creneauxActuels = getValue();
                afficherPlanning();
                if (detailsTable != null) detailsTable.setItems(FXCollections.observableArrayList(creneauxActuels));
                totalCoursLabel.setText(creneauxActuels.size() + " cours");
                if (infoLabel != null) {
                    try {
                        EmploiDuTempsDAO edtDao = new EmploiDuTempsDAO();
                        String titre = edtDao.getTitre(edtCharge.getId());
                        infoLabel.setText("Consultation de l'emploi du temps du " + 
                            edtCharge.getPeriodeDebut() + " au " + edtCharge.getPeriodeFin());
                        if (classeTitreLabel != null) classeTitreLabel.setText(titre);
                    } catch (Exception ex) {
                        infoLabel.setText("Consultation de l'emploi du temps du " + 
                            edtCharge.getPeriodeDebut() + " au " + edtCharge.getPeriodeFin());
                    }
                }
                
                if (modifierButton != null) {
                    modifierButton.setVisible(estGestionnaire);
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement planning EDT", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Affiche le planning sous forme de grille.
     */
    private void afficherPlanning() {
        planningGrid.getChildren().clear();
        planningGrid.getColumnConstraints().clear();
        planningGrid.getRowConstraints().clear();

        final List<String> slots;  
        final List<String> jours;  
        final int ecartMinutes;    

        if (modeLectureSeule && edtCharge != null
                && edtCharge.getHeuresConfig() != null
                && edtCharge.getJoursConfig()  != null) {

            int hDebut = edtCharge.getHeureDebutConfig();
            int hFin   = edtCharge.getHeureFinConfig();
            int ecart  = Math.max(1, edtCharge.getEcartConfig());
            ecartMinutes = ecart * 60;

            List<String> s = new ArrayList<>();
            for (int h = hDebut; h <= hFin; h += ecart) s.add(String.format("%02d:00", h));
            slots = s;
            jours = new ArrayList<>(edtCharge.getJoursConfigList());

        } else {
            // Grille standard pour le mode planning libre
            slots = Arrays.asList(
                "07:00","08:00","09:00","10:00","11:00","12:00",
                "13:00","14:00","15:00","16:00","17:00","18:00");
            jours = Arrays.asList("Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi");
            ecartMinutes = 60;
        }

        // ── Contraintes de colonnes ────────────────────────────────────────────
        ColumnConstraints colHeure = new ColumnConstraints(75);
        colHeure.setHgrow(Priority.NEVER);
        planningGrid.getColumnConstraints().add(colHeure);
        for (int j = 0; j < jours.size(); j++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(90.0 / jours.size());
            cc.setHgrow(Priority.ALWAYS);
            planningGrid.getColumnConstraints().add(cc);
        }

        // ── Contraintes de lignes ──────────────────────────────────────────────
        // Hauteur proportionnelle à l'écart : 1h=50px, 2h=80px
        int hauteurSlot = Math.max(50, (ecartMinutes / 60) * 50);
        RowConstraints rcHeader = new RowConstraints(42);
        planningGrid.getRowConstraints().add(rcHeader);
        for (int i = 0; i < slots.size(); i++) {
            RowConstraints rc = new RowConstraints(hauteurSlot);
            rc.setVgrow(Priority.SOMETIMES);
            planningGrid.getRowConstraints().add(rc);
        }

        // ── Coin vide (0,0) ────────────────────────────────────────────────────
        Label coin = new Label();
        coin.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        coin.setStyle("-fx-background-color: #1A1F3A;");
        planningGrid.add(coin, 0, 0);

        // ── En-têtes des jours ─────────────────────────────────────────────────
        for (int j = 0; j < jours.size(); j++) {
            Label lbl = new Label(jours.get(j));
            lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setStyle("-fx-background-color: #1A1F3A; -fx-text-fill: #EEF0FF; " +
                    "-fx-font-weight: bold; -fx-font-size: 13px;");
            planningGrid.add(lbl, j + 1, 0);
        }

        // ── En-têtes des heures ────────────────────────────────────────────────
        for (int i = 0; i < slots.size(); i++) {
            Label lbl = new Label(slots.get(i));
            lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setStyle("-fx-background-color: #EEF0FF; -fx-text-fill: #4361EE; " +
                    "-fx-font-weight: bold; -fx-font-size: 12px; " +
                    "-fx-border-color: #E8EBFF; -fx-border-width: 0 1 1 0;");
            planningGrid.add(lbl, 0, i + 1);
        }

        // ── Cellules vides ─────────────────────────────────────────────────────
        for (int i = 0; i < slots.size(); i++) {
            for (int j = 0; j < jours.size(); j++) {
                StackPane cell = new StackPane();
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cell.setStyle("-fx-background-color: white; " +
                              "-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 0;");
                planningGrid.add(cell, j + 1, i + 1);
            }
        }

        // ── Placement des cours ────────────────────────────────────────────────
        for (Creneau c : creneauxActuels) {
            int row = trouverLigneExacte(slots, c.getHeureDebut());
            int col = trouverColonneExacte(jours, c.getJour());
            if (row < 0 || col < 0) continue;

            StackPane cell = creerCelluleCours(c);

            // Calculer le rowspan selon la durée du cours / l'écart du slot
            int rowSpan = 1;
            if (ecartMinutes > 0) {
                int dureeMin = heureEnMinutes(c.getHeureFin()) - heureEnMinutes(c.getHeureDebut());
                rowSpan = Math.max(1, (int) Math.round((double) dureeMin / ecartMinutes));
                rowSpan = Math.min(rowSpan, slots.size() - row); // ne pas dépasser la grille
            }

            planningGrid.add(cell, col + 1, row + 1);
            if (rowSpan > 1) GridPane.setRowSpan(cell, rowSpan);
        }
    }

    // ── Utilitaires grille dynamique ───────────────────────────────────────────

    /** Index de la ligne correspondant à l'heure de début d'un créneau. */
    private int trouverLigneExacte(List<String> slots, String heureDebut) {
        int target = heureEnMinutes(heureDebut);
        for (int i = 0; i < slots.size(); i++) {
            if (heureEnMinutes(slots.get(i)) == target) return i;
        }
        // Fallback : slot le plus proche
        int best = 0, bestDiff = Integer.MAX_VALUE;
        for (int i = 0; i < slots.size(); i++) {
            int diff = Math.abs(heureEnMinutes(slots.get(i)) - target);
            if (diff < bestDiff) { bestDiff = diff; best = i; }
        }
        return best;
    }

    /** Index de la colonne correspondant au jour d'un créneau (depuis sa date ISO). */
    private int trouverColonneExacte(List<String> jours, String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            String nom = switch (date.getDayOfWeek().getValue()) {
                case 1 -> "Lundi"; case 2 -> "Mardi"; case 3 -> "Mercredi";
                case 4 -> "Jeudi"; case 5 -> "Vendredi"; case 6 -> "Samedi";
                default -> "";
            };
            return jours.indexOf(nom);
        } catch (Exception e) { return -1; }
    }

    private int heureEnMinutes(String heure) {
        try {
            String[] p = heure.split(":");
            return Integer.parseInt(p[0]) * 60 + (p.length > 1 ? Integer.parseInt(p[1]) : 0);
        } catch (Exception e) { return 0; }
    }
    
    private StackPane creerCelluleCours(Creneau creneau) {
        StackPane cell = new StackPane();
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setPrefHeight(55);

        try {
            Cours cours = coursService.trouverParId(creneau.getCoursId());
            if (cours != null) {
                Matiere matiere   = matiereService.trouverParId(cours.getMatiereId());
                Salle   salle     = creneau.getSalleId() != null ? salleService.trouverParId(creneau.getSalleId()) : null;
                Utilisateur ens   = utilisateurService.trouverParId(cours.getEnseignantId());

                String couleur = couleurParTypeCours.getOrDefault(cours.getTypeCours(), couleurParTypeCours.get("default"));
                String bgClair = couleur + "22";

                VBox vbox = new VBox(2);
                vbox.setAlignment(Pos.CENTER_LEFT);
                vbox.setPadding(new Insets(4, 6, 4, 8));
                vbox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                vbox.setStyle("-fx-background-color: " + bgClair + "; " +
                              "-fx-border-color: " + couleur + "; -fx-border-width: 0 0 0 4; " +
                              "-fx-background-radius: 0;");

                // Ligne 1 : [Type] Matière
                HBox ligne1 = new HBox(4);
                ligne1.setAlignment(Pos.CENTER_LEFT);
                Label typeLbl = new Label("[" + cours.getTypeCours() + "]");
                typeLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
                Label matLbl = new Label(matiere != null ? matiere.getNom() : "?");
                matLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1A202C;");
                matLbl.setWrapText(true);
                ligne1.getChildren().addAll(typeLbl, matLbl);

                // Ligne 2 : Enseignant (Grade + Nom)
                String nomEns = "";
                if (ens instanceof Enseignant) {
                    Enseignant e = (Enseignant) ens;
                    String grade = e.getGrade();
                    nomEns = (grade != null && !grade.isEmpty() ? grade + " " : "") + e.getNom();
                } else if (ens != null) {
                    nomEns = ens.getNom();
                }
                Label ensLbl = new Label("👨‍🏫 " + nomEns);
                ensLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #1976D2;");

                // Ligne 3 : Salle
                Label salleLbl = new Label(salle != null ? "🏫 " + salle.getNumero() : "🏫 -");
                salleLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #4A5568;");

                vbox.getChildren().addAll(ligne1, ensLbl, salleLbl);

                // Ligne 4 : Groupe (TD/TP uniquement, non vide, pas "Tous")
                String groupes = cours.getGroupes();
                String type    = cours.getTypeCours();
                if (("TD".equals(type) || "TP".equals(type))
                        && groupes != null && !groupes.isEmpty() && !"Tous".equalsIgnoreCase(groupes)) {
                    Label groupeLbl = new Label("👥 " + groupes);
                    groupeLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #C62828;");
                    vbox.getChildren().add(groupeLbl);
                }

                cell.getChildren().add(vbox);
            } else {
            	Label erreurLabel = new Label("Cours ?");
            	erreurLabel.setStyle("-fx-text-fill: #7B83C4;");
            	cell.getChildren().add(erreurLabel);
            }
        } catch (SQLException e) {
            cell.getChildren().add(new Label("Erreur"));
        }

        cell.setStyle("-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 0; -fx-cursor: hand;");
        cell.setOnMouseClicked(e -> afficherDetailsCreneau(creneau));
        return cell;
    }
    
    private List<Creneau> filtrerParClasse(List<Creneau> creneaux) {
        Classe classe = classeFilterCombo.getValue();
        if (classe == null) return creneaux;
        List<Creneau> result = new ArrayList<>();
        try {
            for (Creneau c : creneaux) {
                Cours cours = coursService.trouverParId(c.getCoursId());
                if (cours != null && cours.getClasseId() == classe.getId()) result.add(c);
            }
        } catch (SQLException e) { 
            logger.error("Erreur filtrage par classe", e);
            return creneaux; 
        }
        return result;
    }
    
    /**
     * Vérifie tous les conflits pour les créneaux affichés.
     */
    private void verifierTousLesConflits() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                List<String> tousConflits = new ArrayList<>();
                for (Creneau c : creneauxActuels) {
                    tousConflits.addAll(conflitService.verifierConflits(c));
                }
                return tousConflits;
            }
            
            @Override
            protected void succeeded() {
                List<String> conflits = getValue();
                if (conflits.isEmpty()) {
                    afficherNotification("Vérification", "Aucun conflit détecté");
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Conflits détectés");
                    alert.setHeaderText(conflits.size() + " conflit(s) trouvé(s)");
                    
                    ButtonType solutionsBtn = new ButtonType("Voir solutions", ButtonBar.ButtonData.OK_DONE);
                    alert.getButtonTypes().setAll(solutionsBtn, ButtonType.CANCEL);
                    
                    TextArea textArea = new TextArea(String.join("\n\n", conflits));
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    alert.getDialogPane().setContent(textArea);
                    
                    alert.showAndWait().ifPresent(response -> {
                        if (response == solutionsBtn) {
                            proposerSolutionsConflits(conflits);
                        }
                    });
                }
            }
        };
        new Thread(task).start();
    }

    /**
     * Propose des solutions pour résoudre les conflits.
     * @param conflits la liste des messages de conflit
     */
    private void proposerSolutionsConflits(List<String> conflits) {
        List<Map<String, Object>> solutions = new ArrayList<>();
        
        for (String conflit : conflits) {
            if (conflit.contains("salle") || conflit.contains("occupée")) {
                Map<String, Object> solution = new HashMap<>();
                solution.put("type", "salle_alternative");
                solution.put("message", "🔄 Changer de salle pour une autre disponible");
                solution.put("priorite", 1);
                solutions.add(solution);
            }
            
            if (conflit.contains("enseignant") || conflit.contains("prof")) {
                Map<String, Object> solution = new HashMap<>();
                solution.put("type", "creneau_enseignant");
                solution.put("message", "📅 Décaler le créneau de l'enseignant");
                solution.put("priorite", 2);
                solutions.add(solution);
            }
            
            if (conflit.contains("classe")) {
                Map<String, Object> solution = new HashMap<>();
                solution.put("type", "classe");
                solution.put("message", "👥 Modifier le groupe ou la classe");
                solution.put("priorite", 3);
                solutions.add(solution);
            }
            
            Map<String, Object> solutionDecalage = new HashMap<>();
            solutionDecalage.put("type", "decalage_horaire");
            solutionDecalage.put("message", "⏰ Décaler l'horaire d'une heure");
            solutionDecalage.put("priorite", 4);
            solutions.add(solutionDecalage);
        }
        
        List<Map<String, Object>> solutionsUniques = new ArrayList<>();
        Set<String> messagesVus = new HashSet<>();
        for (Map<String, Object> sol : solutions) {
            String msg = (String) sol.get("message");
            if (!messagesVus.contains(msg)) {
                messagesVus.add(msg);
                solutionsUniques.add(sol);
            }
        }
        
        if (solutionsUniques.isEmpty()) {
            afficherErreur("Aucune solution automatique disponible");
            return;
        }
        
        solutionsUniques.sort((a, b) -> {
            int pa = (int) a.getOrDefault("priorite", 99);
            int pb = (int) b.getOrDefault("priorite", 99);
            return Integer.compare(pa, pb);
        });
        
        List<String> messages = new ArrayList<>();
        for (Map<String, Object> sol : solutionsUniques) {
            messages.add((String) sol.get("message"));
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(messages.get(0), messages);
        dialog.setTitle("Solutions proposées");
        dialog.setHeaderText("Choisissez une solution pour résoudre les conflits");
        dialog.setContentText("Solution :");
        
        dialog.showAndWait().ifPresent(solutionChoisie -> {
            Map<String, Object> solutionSelectionnee = null;
            for (Map<String, Object> sol : solutionsUniques) {
                if (sol.get("message").equals(solutionChoisie)) {
                    solutionSelectionnee = sol;
                    break;
                }
            }
            
            if (solutionSelectionnee != null) {
                appliquerSolution(solutionSelectionnee);
            }
        });
    }

    private void appliquerSolution(Map<String, Object> solution) {
        String type = (String) solution.get("type");
        
        switch (type) {
            case "salle_alternative":
                proposerSallesAlternatives();
                break;
            case "creneau_enseignant":
                proposerCreneauxAlternatifsEnseignant();
                break;
            case "classe":
                proposerModificationClasse();
                break;
            case "decalage_horaire":
                proposerDecalageHoraire();
                break;
            default:
                afficherNotification("Info", "Solution en cours de développement");
        }
    }

    /**
     * Propose des salles alternatives pour un créneau.
     */
    private void proposerSallesAlternatives() {
        final Creneau creneauSelectionne = detailsTable.getSelectionModel().getSelectedItem();
        final Creneau creneauFinal = (creneauSelectionne != null && !creneauxActuels.isEmpty()) 
            ? creneauSelectionne : (creneauxActuels.isEmpty() ? null : creneauxActuels.get(0));
        
        if (creneauFinal == null) {
            afficherErreur("Aucun créneau sélectionné");
            return;
        }
        
        Task<List<Salle>> task = new Task<>() {
            @Override
            protected List<Salle> call() throws SQLException {
                Cours cours = coursService.trouverParId(creneauFinal.getCoursId());
                if (cours == null) return new ArrayList<>();
                
                List<Salle> toutesSalles = salleService.listerTous();
                List<Salle> alternatives = new ArrayList<>();
                Classe classe = classeService.trouverParId(cours.getClasseId());
                int capaciteRequise = classe != null ? classe.getEffectif() : 30;
                
                for (Salle s : toutesSalles) {
                    if (s.getStatut().equals("disponible") && s.getCapacite() >= capaciteRequise) {
                        boolean estLibre = planningService.salleEstDisponible(
                            s.getId(), creneauFinal.getJour(),
                            creneauFinal.getHeureDebut(), creneauFinal.getHeureFin()
                        );
                        if (estLibre) {
                            alternatives.add(s);
                        }
                    }
                }
                return alternatives;
            }
            
            @Override
            protected void succeeded() {
                List<Salle> alternatives = getValue();
                if (alternatives.isEmpty()) {
                    afficherErreur("Aucune salle alternative disponible");
                    return;
                }
                
                List<String> nomsSalles = new ArrayList<>();
                for (Salle s : alternatives) {
                    nomsSalles.add("Salle " + s.getNumero() + " (Cap: " + s.getCapacite() + ", Type: " + s.getType() + ")");
                }
                
                ChoiceDialog<String> dialog = new ChoiceDialog<>(nomsSalles.get(0), nomsSalles);
                dialog.setTitle("Salles alternatives");
                dialog.setHeaderText("Choisissez une salle alternative");
                
                dialog.showAndWait().ifPresent(choix -> {
                    String numero = choix.split(" ")[1];
                    for (Salle s : alternatives) {
                        if (s.getNumero().equals(numero)) {
                            try {
                                creneauFinal.setSalleId(s.getId());
                                planningService.modifierCreneau(creneauFinal);
                                afficherNotification("Succès", "Salle changée vers " + s.getNumero());
                                chargerPlanning();
                            } catch (SQLException e) {
                                afficherErreur("Erreur lors du changement de salle");
                            }
                            break;
                        }
                    }
                });
            }
        };
        new Thread(task).start();
    }

    private void proposerCreneauxAlternatifsEnseignant() {
        final Creneau creneauSelectionne = detailsTable.getSelectionModel().getSelectedItem();
        final Creneau creneauFinal = (creneauSelectionne != null && !creneauxActuels.isEmpty()) 
            ? creneauSelectionne : (creneauxActuels.isEmpty() ? null : creneauxActuels.get(0));
        
        if (creneauFinal == null) {
            afficherErreur("Aucun créneau sélectionné");
            return;
        }
        
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                List<String> creneauxAlternatifs = new ArrayList<>();
                Cours cours = coursService.trouverParId(creneauFinal.getCoursId());
                if (cours == null) return creneauxAlternatifs;
                
                LocalDate date = LocalDate.parse(creneauFinal.getJour());
                String[] heuresAlternatives = {"08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00"};
                int duree = calculerDureeHeures(creneauFinal.getHeureDebut(), creneauFinal.getHeureFin());
                
                for (String heureAlt : heuresAlternatives) {
                    if (heureAlt.equals(creneauFinal.getHeureDebut())) continue;
                    
                    String heureFinAlt = calculerHeureFin(heureAlt, duree);
                    boolean salleLibre = true;
                    if (creneauFinal.getSalleId() != null) {
                        salleLibre = planningService.salleEstDisponible(
                            creneauFinal.getSalleId(), date.toString(), heureAlt, heureFinAlt
                        );
                    }
                    boolean enseignantLibre = planningService.enseignantEstDisponible(
                        cours.getEnseignantId(), date.toString(), heureAlt, heureFinAlt
                    );
                    
                    if (salleLibre && enseignantLibre) {
                        creneauxAlternatifs.add(heureAlt + " - " + heureFinAlt);
                    }
                }
                return creneauxAlternatifs;
            }
            
            @Override
            protected void succeeded() {
                List<String> alternatifs = getValue();
                if (alternatifs.isEmpty()) {
                    afficherErreur("Aucun créneau alternatif disponible");
                    return;
                }
                
                ChoiceDialog<String> dialog = new ChoiceDialog<>(alternatifs.get(0), alternatifs);
                dialog.setTitle("Créneaux alternatifs");
                dialog.setHeaderText("Choisissez un nouveau créneau pour l'enseignant");
                
                dialog.showAndWait().ifPresent(choix -> {
                    String[] heures = choix.split(" - ");
                    try {
                        creneauFinal.setHeureDebut(heures[0]);
                        creneauFinal.setHeureFin(heures[1]);
                        planningService.modifierCreneau(creneauFinal);
                        afficherNotification("Succès", "Créneau modifié vers " + choix);
                        chargerPlanning();
                    } catch (SQLException e) {
                        afficherErreur("Erreur lors du changement de créneau");
                    }
                });
            }
        };
        new Thread(task).start();
    }

    private void proposerModificationClasse() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Modification de classe");
        alert.setHeaderText("Gestion des groupes");
        alert.setContentText("Cette fonctionnalité vous permet de modifier les groupes de TD/TP.\n\n" +
                             "1. Allez dans Gestion des cours\n" +
                             "2. Modifiez le cours concerné\n" +
                             "3. Ajustez les groupes (G1, G2, etc.)");
        alert.show();
    }

    private void proposerDecalageHoraire() {
        final Creneau creneauSelectionne = detailsTable.getSelectionModel().getSelectedItem();
        final Creneau creneauFinal = (creneauSelectionne != null && !creneauxActuels.isEmpty()) 
            ? creneauSelectionne : (creneauxActuels.isEmpty() ? null : creneauxActuels.get(0));
        
        if (creneauFinal == null) {
            afficherErreur("Aucun créneau sélectionné");
            return;
        }
        
        Spinner<Integer> decalageSpinner = new Spinner<>(1, 3, 1);
        decalageSpinner.setEditable(true);
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Décaler l'horaire");
        alert.setHeaderText("Décaler le créneau de combien d'heures ?");
        alert.getDialogPane().setContent(decalageSpinner);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int decalage = decalageSpinner.getValue();
                try {
                    String[] hParts = creneauFinal.getHeureDebut().split(":");
                    int newHeure = Integer.parseInt(hParts[0]) + decalage;
                    if (newHeure <= 18) {
                        String nouvelleHeureDebut = String.format("%02d:00", newHeure);
                        int duree = calculerDureeHeures(creneauFinal.getHeureDebut(), creneauFinal.getHeureFin());
                        String nouvelleHeureFin = calculerHeureFin(nouvelleHeureDebut, duree);
                        
                        creneauFinal.setHeureDebut(nouvelleHeureDebut);
                        creneauFinal.setHeureFin(nouvelleHeureFin);
                        planningService.modifierCreneau(creneauFinal);
                        afficherNotification("Succès", "Créneau décalé de " + decalage + " heure(s)");
                        chargerPlanning();
                    } else {
                        afficherErreur("L'horaire dépasse 18h");
                    }
                } catch (SQLException e) {
                    afficherErreur("Erreur lors du décalage");
                }
            }
        });
    }
    
    // MÉTHODES UTILITAIRES
    
    private int calculerDureeHeures(String heureDebut, String heureFin) {
        try {
            String[] debut = heureDebut.split(":");
            String[] fin = heureFin.split(":");
            int debutMinutes = Integer.parseInt(debut[0]) * 60 + Integer.parseInt(debut[1]);
            int finMinutes = Integer.parseInt(fin[0]) * 60 + Integer.parseInt(fin[1]);
            return (finMinutes - debutMinutes) / 60;
        } catch (Exception e) {
            return 1;
        }
    }
    
    private String calculerHeureFin(String heureDebut, int dureeHeures) {
        String[] parts = heureDebut.split(":");
        int heure = Integer.parseInt(parts[0]) + dureeHeures;
        return String.format("%02d:00", heure);
    }
    
    /**
     * Affiche les détails d'un créneau.
     * @param creneau le créneau à afficher
     */
    private void afficherDetailsCreneau(Creneau creneau) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du cours");
        
        StringBuilder sb = new StringBuilder();
        sb.append("📅 Date: ").append(formaterDate(creneau.getJour())).append("\n");
        sb.append("⏰ Horaire: ").append(creneau.getHeureDebut())
          .append(" - ").append(creneau.getHeureFin()).append("\n");
        
        try {
            Cours cours = coursService.trouverParId(creneau.getCoursId());
            if (cours != null) {
                Matiere matiere = matiereService.trouverParId(cours.getMatiereId());
                Utilisateur enseignant = utilisateurService.trouverParId(cours.getEnseignantId());
                Classe classe = classeService.trouverParId(cours.getClasseId());
                Salle salle = creneau.getSalleId() != null ? salleService.trouverParId(creneau.getSalleId()) : null;
                
                sb.append("\n📚 MATIÈRE\n");
                sb.append("   • ").append(matiere != null ? matiere.getNom() : "Inconnue").append("\n");
                sb.append("   • Type: ").append(cours.getTypeCours()).append("\n");
                
                sb.append("\n👨‍🏫 ENSEIGNANT\n");
                if (enseignant != null) {
                    sb.append("   • ").append(enseignant.getPrenom()).append(" ").append(enseignant.getNom()).append("\n");
                } else {
                    sb.append("   • Inconnu\n");
                }
                
                sb.append("\n👥 CLASSE\n");
                sb.append("   • ").append(classe != null ? classe.getIntitule() : "Inconnue").append("\n");
                sb.append("   • Groupes: ").append(cours.getGroupes()).append("\n");
                
                sb.append("\n🏛️ SALLE\n");
                if (salle != null) {
                    sb.append("   • ").append(salle.getNumero()).append("\n");
                    sb.append("   • Capacité: ").append(salle.getCapacite()).append(" places\n");
                    sb.append("   • Type: ").append(salle.getType()).append("\n");
                    sb.append("   • Étage: ").append(salle.getEtage()).append("\n");
                } else {
                    sb.append("   • Non assignée\n");
                }
            }
        } catch (SQLException e) {
            sb.append("\n❌ Erreur chargement détails\n");
        }
        
        sb.append("\n📌 Statut: ").append(creneau.getStatut());
        if (creneau.getMotifAnnulation() != null) {
            sb.append("\n📝 Motif annulation: ").append(creneau.getMotifAnnulation());
        }
        
        alert.setContentText(sb.toString());
        alert.show();
    }
    
    private String formaterDate(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    
    public void setOrigineRole(String role) {
        this.origineRole = role != null ? role : "gestionnaire";
    }

    public void chargerEmploiDuTemps(EmploiDuTemps edt) {
        this.edtCharge = edt;
        this.modeLectureSeule = true;
        
        if (classeFilterCombo != null) classeFilterCombo.setDisable(true);
        if (datePicker != null) datePicker.setDisable(true);
        if (vueJourRadio != null) vueJourRadio.setDisable(true);
        if (vueSemaineRadio != null) vueSemaineRadio.setDisable(true);
        if (precedentButton != null) precedentButton.setDisable(true);
        if (suivantButton != null) suivantButton.setDisable(true);
        if (aujourdhuiButton != null) aujourdhuiButton.setDisable(true);
        if (verifierConflitsButton != null) verifierConflitsButton.setVisible(false);
        
        for (Classe c : classeFilterCombo.getItems()) {
            if (c != null && c.getId() == edt.getClasseId()) {
                classeFilterCombo.setValue(c);
                mettreAJourTitreClasse(c);
                break;
            }
        }
        
        if (periodeDetailLabel != null) {
            periodeDetailLabel.setText("Période: " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin());
        }
        
        periodeLabel.setText(edt.getPeriodeType() + " du " + 
            edt.getPeriodeDebut() + " au " + edt.getPeriodeFin());
        
        chargerPlanning();
    }
    
    private void modifierEmploiDuTemps() {
        if (!estGestionnaire) {
            afficherErreur("Seul le gestionnaire peut modifier l'emploi du temps");
            return;
        }
        if (edtCharge == null) {
            afficherErreur("Aucun emploi du temps chargé");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NouvelEdt.fxml"));
            Parent root = loader.load();
            NouvelEdtControleur ctrl = loader.getController();
            ctrl.setUtilisateurConnectePublic(utilisateurConnecte);
            ctrl.initialiserModification(edtCharge, primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur navigation modification EDT", e);
            afficherErreur("Impossible d'accéder à la modification");
        }
    }
    
    public void preparerNouvelEDT(int classeId) {
        this.modeLectureSeule = false;
        this.edtCharge = null;
        
        for (Classe c : classeFilterCombo.getItems()) {
            if (c != null && c.getId() == classeId) {
                classeFilterCombo.setValue(c);
                mettreAJourTitreClasse(c);
                break;
            }
        }
        
        if (classeFilterCombo != null) classeFilterCombo.setDisable(false);
        if (datePicker != null) datePicker.setDisable(false);
        if (vueJourRadio != null) vueJourRadio.setDisable(false);
        if (vueSemaineRadio != null) vueSemaineRadio.setDisable(false);
        if (precedentButton != null) precedentButton.setDisable(false);
        if (suivantButton != null) suivantButton.setDisable(false);
        if (aujourdhuiButton != null) aujourdhuiButton.setDisable(false);
        if (verifierConflitsButton != null) verifierConflitsButton.setVisible(true);
        if (modifierButton != null) modifierButton.setVisible(false);
        
        dateCourante = LocalDate.now();
        mettreAJourPeriode();
        chargerPlanning();
    }
    
    /**
     * Exporte le planning en PDF.
     */
    private void exporterPDF() {
        if (creneauxActuels == null || creneauxActuels.isEmpty()) {
            afficherErreur("Aucune donnée à exporter");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le planning en PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        
        String nomFichier = "Planning_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Classe classe = classeFilterCombo.getValue();
                    exportService.exporterEDTGrillePDF(
                        creneauxActuels, 
                        classe, 
                        periodeLabel.getText(), 
                        utilisateurConnecte, 
                        chemin
                    );
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    afficherNotification("Export PDF réussi", "Fichier enregistré : " + chemin);
                }
                
                @Override
                protected void failed() {
                    afficherErreur("Erreur lors de l'export PDF : " + getException().getMessage());
                }
            };
            
            new Thread(task).start();
        }
    }
    
    /**
     * Exporte le planning en Excel.
     */
    private void exporterExcel() {
        if (creneauxActuels == null || creneauxActuels.isEmpty()) {
            afficherErreur("Aucune donnée à exporter");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le planning en Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );
        
        String nomFichier = "Planning_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Classe classe = classeFilterCombo.getValue();
                    exportService.exporterEDTGrilleExcel(
                        creneauxActuels, 
                        classe, 
                        periodeLabel.getText(), 
                        utilisateurConnecte, 
                        chemin
                    );
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    afficherNotification("Export Excel réussi", "Fichier enregistré : " + chemin);
                }
                
                @Override
                protected void failed() {
                    afficherErreur("Erreur lors de l'export Excel : " + getException().getMessage());
                }
            };
            
            new Thread(task).start();
        }
    }
    
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (planningGrid != null && planningGrid.getScene() != null && planningGrid.getScene().getWindow() instanceof Stage) {
            return (Stage) planningGrid.getScene().getWindow();
        }
        return null;
    }
    
    private void retourAListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListeEmploisTemps.fxml"));
            Parent root = loader.load();
            ListeEmploisTempsControleur ctrl = (ListeEmploisTempsControleur) loader.getController();
            ctrl.setOrigineRole(origineRole);
            ctrl.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur retour liste EDT", e);
        }
    }
    
    public void preselectionnerCours(Cours cours) {
    }
}