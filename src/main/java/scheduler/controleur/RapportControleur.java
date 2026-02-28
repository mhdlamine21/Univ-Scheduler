package scheduler.controleur;

import scheduler.dao.CreneauDAO;
import scheduler.dao.ReservationDAO;
import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import scheduler.service.CoursService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * Contrôleur pour l'écran Rapports.
 */
public class RapportControleur extends TableauBordControleur {
    
    
    @FXML private ComboBox<String> typeRapportCombo;
    @FXML private ComboBox<Ufr> ufrCombo;
    @FXML private ComboBox<Classe> classeCombo;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Button genererButton;
    @FXML private Button exporterPDFButton;
    @FXML private Button exporterExcelButton;
    @FXML private Button retourButton;
    
    @FXML private DatePicker semaineDebutPicker;
    @FXML private DatePicker semaineFinPicker;
    @FXML private Button aujourdhuiButton;
    @FXML private Button cetteSemaineButton;
    @FXML private Button ceMoisButton;
    
    @FXML private TabPane rapportTabPane;
    
    @FXML private Label totalCoursLabel;
    @FXML private Label totalSallesLabel;
    @FXML private Label tauxOccupationLabel;
    @FXML private Label totalEnseignantsLabel;
    @FXML private Label totalEtudiantsLabel;
    @FXML private Label totalReservationsLabel;
    
    @FXML private BarChart<String, Number> occupationChart;
    @FXML private CategoryAxis occupationXAxis;
    @FXML private NumberAxis occupationYAxis;
    
    @FXML private PieChart repartitionChart;
    
    @FXML private LineChart<String, Number> tendanceChart;
    @FXML private CategoryAxis tendanceXAxis;
    @FXML private NumberAxis tendanceYAxis;
    
    @FXML private TableView<Map<String, Object>> detailsTable;
    @FXML private TableColumn<Map<String, Object>, String> salleColumn;
    @FXML private TableColumn<Map<String, Object>, String> typeColumn;
    @FXML private TableColumn<Map<String, Object>, String> capaciteColumn;
    @FXML private TableColumn<Map<String, Object>, String> tauxColumn;
    @FXML private TableColumn<Map<String, Object>, String> heuresColumn;
    @FXML private TableColumn<Map<String, Object>, String> statutColumn;

    @FXML private Button rafraichirAuditButton;
    @FXML private TableView<AuditLog> auditTable;
    @FXML private TableColumn<AuditLog, String> auditTimestampColumn;
    @FXML private TableColumn<AuditLog, String> auditUserColumn;
    @FXML private TableColumn<AuditLog, String> auditRoleColumn;
    @FXML private TableColumn<AuditLog, String> auditActionColumn;
    @FXML private TableColumn<AuditLog, String> auditEntiteColumn;
    @FXML private TableColumn<AuditLog, String> auditIpColumn;
    @FXML private TableColumn<AuditLog, String> auditDetailsColumn;
    
    @FXML private ProgressIndicator chargementIndicator;
    @FXML private ScrollPane rapportScrollPane;
    
    
    private StatistiqueService statsService;
    private UfrService ufrService;
    private ClasseService classeService;
    private ExportService exportService;
    private ReservationDAO reservationDAO;
    private CreneauDAO creneauDAO;
    private SalleService salleService;
    private PlanningService planningService;
    private CoursService coursService;
    
    
    private Map<String, Object> dernierRapport;
    private String periodeRapport;
    private List<Map<String, Object>> sallesCritiquesCache;
    private Map<String, Integer> occupationParHeureCache;
    private Map<String, Double> evolutionCache;
    private Map<String, Integer> repartitionParTypeCache;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.statsService = new StatistiqueService();
        this.ufrService = new UfrService();
        this.classeService = new ClasseService();
        this.exportService = new ExportService();
        this.reservationDAO = new ReservationDAO();
        this.creneauDAO = new CreneauDAO();
        this.salleService = new SalleService();
        this.planningService = new PlanningService();
        this.coursService = new CoursService();
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetour());
        }
        
        configurerComposants();
        configurerGraphiques();
        configurerTableauDetails();
        configurerTableauAudit();
        
        chargerUfr();
        chargerClasses();
        
        // Dates par défaut
        dateDebutPicker.setValue(LocalDate.now().minusMonths(1));
        dateFinPicker.setValue(LocalDate.now());
        semaineDebutPicker.setValue(LocalDate.now().with(java.time.DayOfWeek.MONDAY));
        semaineFinPicker.setValue(LocalDate.now().with(java.time.DayOfWeek.SUNDAY));
        
        genererRapport();
        chargerLogsAudit();
    }
    
    @Override
    protected void initialiserTableauBord() {
    }
    
    @Override
    protected void rafraichirDonnees() {
        genererRapport();
        chargerLogsAudit();
    }
    
    
    private void configurerComposants() {
        typeRapportCombo.getItems().addAll(
            "Rapport hebdomadaire",
            "Rapport mensuel",
            "Rapport semestriel",
            "Rapport personnalisé"
        );
        typeRapportCombo.setValue("Rapport mensuel");
        typeRapportCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            ajusterDatesSelonType(newVal);
        });
        
        aujourdhuiButton.setOnAction(e -> {
            dateDebutPicker.setValue(LocalDate.now());
            dateFinPicker.setValue(LocalDate.now());
            genererRapport();
        });
        
        cetteSemaineButton.setOnAction(e -> {
            dateDebutPicker.setValue(LocalDate.now().with(java.time.DayOfWeek.MONDAY));
            dateFinPicker.setValue(LocalDate.now().with(java.time.DayOfWeek.SUNDAY));
            genererRapport();
        });
        
        ceMoisButton.setOnAction(e -> {
            dateDebutPicker.setValue(LocalDate.now().withDayOfMonth(1));
            dateFinPicker.setValue(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
            genererRapport();
        });
        
        // Ajouter les listeners pour les filtres
        ufrCombo.valueProperty().addListener((obs, oldVal, newVal) -> genererRapport());
        classeCombo.valueProperty().addListener((obs, oldVal, newVal) -> genererRapport());
        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> genererRapport());
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> genererRapport());
        
        genererButton.setOnAction(e -> genererRapport());
        exporterPDFButton.setOnAction(e -> exporterPDF());
        exporterExcelButton.setOnAction(e -> exporterExcel());
    }
    
    /**
     * Ajuste les dates selon le type de rapport sélectionné.
     * @param type le type de rapport
     */
    private void ajusterDatesSelonType(String type) {
        LocalDate maintenant = LocalDate.now();
        if (type == null) return;
        
        switch (type) {
            case "Rapport hebdomadaire":
                dateDebutPicker.setValue(maintenant.with(java.time.DayOfWeek.MONDAY));
                dateFinPicker.setValue(maintenant.with(java.time.DayOfWeek.SUNDAY));
                break;
            case "Rapport mensuel":
                dateDebutPicker.setValue(maintenant.withDayOfMonth(1));
                dateFinPicker.setValue(maintenant.withDayOfMonth(maintenant.lengthOfMonth()));
                break;
            case "Rapport semestriel":
                if (maintenant.getMonthValue() <= 6) {
                    dateDebutPicker.setValue(LocalDate.of(maintenant.getYear(), 1, 1));
                    dateFinPicker.setValue(LocalDate.of(maintenant.getYear(), 6, 30));
                } else {
                    dateDebutPicker.setValue(LocalDate.of(maintenant.getYear(), 7, 1));
                    dateFinPicker.setValue(LocalDate.of(maintenant.getYear(), 12, 31));
                }
                break;
            case "Rapport personnalisé":
                // Ne pas modifier les dates
                break;
        }
    }
    
    private void configurerGraphiques() {
        if (occupationChart != null) {
            occupationChart.setTitle("📊 OCCUPATION DES SALLES PAR CRÉNEAU");
            occupationChart.setLegendVisible(true);
            occupationChart.setAnimated(false);
            occupationChart.setPrefHeight(400);  
            occupationChart.setPrefWidth(650);   
            
            if (occupationXAxis != null) {
                occupationXAxis.setLabel("CRÉNEAUX HORAIRES");
                occupationXAxis.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3D261A;");
                occupationXAxis.setTickLabelFont(javafx.scene.text.Font.font("Segoe UI", 12));
                occupationXAxis.setTickLabelFill(javafx.scene.paint.Color.web("#3D261A"));
                occupationXAxis.setTickLabelsVisible(true);
            }
            if (occupationYAxis != null) {
                occupationYAxis.setLabel("NOMBRE DE COURS");
                occupationYAxis.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3D261A;");
                occupationYAxis.setTickLabelFont(javafx.scene.text.Font.font("Segoe UI", 12));
                occupationYAxis.setTickLabelFill(javafx.scene.paint.Color.web("#3D261A"));
                occupationYAxis.setTickLabelsVisible(true);
                occupationYAxis.setTickUnit(1);
            }
        }
        
        if (occupationXAxis != null) {
            occupationXAxis.setTickLabelFill(javafx.scene.paint.Color.web("#3D261A"));
        }
        if (occupationYAxis != null) {
            occupationYAxis.setTickLabelFill(javafx.scene.paint.Color.web("#3D261A"));
        }
        if (tendanceXAxis != null) {
            tendanceXAxis.setTickLabelFill(javafx.scene.paint.Color.web("#3D261A"));
        }
        if (tendanceYAxis != null) {
            tendanceYAxis.setTickLabelFill(javafx.scene.paint.Color.web("#3D261A"));
        }
        
        if (repartitionChart != null) {
            repartitionChart.setTitle("📊 RÉPARTITION PAR TYPE DE COURS");
            repartitionChart.setLabelsVisible(true);
            repartitionChart.setLegendVisible(true);
            repartitionChart.setPrefHeight(400);  
            repartitionChart.setPrefWidth(550);   
            repartitionChart.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        }
        
        if (tendanceChart != null) {
            tendanceChart.setTitle("📈 ÉVOLUTION MENSUELLE DU TAUX D'OCCUPATION (12 derniers mois)");
            tendanceChart.setLegendVisible(true);
            tendanceChart.setAnimated(false);
            tendanceChart.setPrefHeight(400);   
            tendanceChart.setPrefWidth(1100);   
            
            if (tendanceXAxis != null) {
                tendanceXAxis.setLabel("MOIS");
                tendanceXAxis.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3D261A;");
                tendanceXAxis.setTickLabelFont(javafx.scene.text.Font.font("Segoe UI", 11));
                tendanceXAxis.setTickLabelFill(javafx.scene.paint.Color.web("#3D261A"));
                tendanceXAxis.setTickLabelsVisible(true);
            }
            if (tendanceYAxis != null) {
                tendanceYAxis.setLabel("TAUX D'OCCUPATION (%)");
                tendanceYAxis.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3D261A;");
                tendanceYAxis.setTickLabelFont(javafx.scene.text.Font.font("Segoe UI", 11));
                tendanceYAxis.setTickLabelFill(javafx.scene.paint.Color.web("#3D261A"));
                tendanceYAxis.setTickLabelsVisible(true);
            }
        }
    }
    
    private void configurerTableauDetails() {
        salleColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    Object val = cellData.getValue().get("salle");
                    return val != null ? val.toString() : "";
                }
            )
        );
        salleColumn.setPrefWidth(120);
        
        typeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    Object val = cellData.getValue().get("type");
                    return val != null ? val.toString() : "";
                }
            )
        );
        typeColumn.setPrefWidth(100);
        
        capaciteColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    Object val = cellData.getValue().get("capacite");
                    return val != null ? val.toString() : "";
                }
            )
        );
        capaciteColumn.setPrefWidth(80);
        
        tauxColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    Object val = cellData.getValue().get("taux");
                    return val != null ? val.toString() + "%" : "";
                }
            )
        );
        tauxColumn.setPrefWidth(100);
        
        heuresColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    Object val = cellData.getValue().get("heures");
                    return val != null ? val.toString() + "h" : "";
                }
            )
        );
        heuresColumn.setPrefWidth(100);
        
        statutColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    Object val = cellData.getValue().get("statut");
                    if (val == null) return "";
                    String statut = val.toString();
                    if (statut.equals("surchargée")) return "🔴 Surchargée";
                    if (statut.equals("sous-utilisée")) return "🟡 Sous-utilisée";
                    return "🟢 Normale";
                }
            )
        );
        statutColumn.setPrefWidth(110);
        
        statutColumn.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(statut);
                    if (statut.contains("Surchargée")) {
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                    } else if (statut.contains("Sous-utilisée")) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void configurerTableauAudit() {
        if (auditTable == null) return;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        if (auditTimestampColumn != null) {
            auditTimestampColumn.setCellValueFactory(cellData -> 
                javafx.beans.binding.Bindings.createStringBinding(() -> {
                    AuditLog log = cellData.getValue();
                    return (log != null && log.getTimestamp() != null) ? log.getTimestamp().format(dtf) : "";
                })
            );
        }
        if (auditUserColumn != null) {
            auditUserColumn.setCellValueFactory(new PropertyValueFactory<>("utilisateurEmail"));
        }
        if (auditRoleColumn != null) {
            auditRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        }
        if (auditActionColumn != null) {
            auditActionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        }
        if (auditEntiteColumn != null) {
            auditEntiteColumn.setCellValueFactory(new PropertyValueFactory<>("entite"));
        }
        if (auditIpColumn != null) {
            auditIpColumn.setCellValueFactory(new PropertyValueFactory<>("adresseIp"));
        }
        if (auditDetailsColumn != null) {
            auditDetailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        }

        if (rafraichirAuditButton != null) {
            rafraichirAuditButton.setOnAction(e -> chargerLogsAudit());
        }
    }

    private void chargerLogsAudit() {
        Task<List<AuditLog>> task = new Task<>() {
            @Override
            protected List<AuditLog> call() {
                return AuditService.listerDerniersLogs(100);
            }
            @Override
            protected void succeeded() {
                if (auditTable != null && getValue() != null) {
                    auditTable.setItems(FXCollections.observableArrayList(getValue()));
                }
            }
            @Override
            protected void failed() {
                logger.warn("Impossible de charger les logs d'audit");
            }
        };
        new Thread(task).start();
    }
    
    // CHARGEMENT DES DONNÉES
    
    private void chargerUfr() {
        Task<List<Ufr>> task = new Task<>() {
            @Override
            protected List<Ufr> call() throws SQLException {
                return ufrService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                ufrCombo.getItems().clear();
                ufrCombo.getItems().add(null);
                ufrCombo.getItems().addAll(getValue());
                
                ufrCombo.setCellFactory(lv -> new ListCell<Ufr>() {
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
                
                ufrCombo.setButtonCell(new ListCell<Ufr>() {
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
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement UFR", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerClasses() {
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() throws SQLException {
                return classeService.listerToutes();
            }
            
            @Override
            protected void succeeded() {
                classeCombo.getItems().clear();
                classeCombo.getItems().add(null);
                classeCombo.getItems().addAll(getValue());
                
                classeCombo.setCellFactory(lv -> new ListCell<Classe>() {
                    @Override
                    protected void updateItem(Classe item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Toutes les classes");
                        } else {
                            setText(item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                        }
                    }
                });
                
                classeCombo.setButtonCell(new ListCell<Classe>() {
                    @Override
                    protected void updateItem(Classe item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Toutes les classes");
                        } else {
                            setText(item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                        }
                    }
                });
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement classes", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Génère le rapport avec les filtres actuels.
     */
    private void genererRapport() {
        if (chargementIndicator != null) chargementIndicator.setVisible(true);

        LocalDate debut = dateDebutPicker.getValue();
        LocalDate fin   = dateFinPicker.getValue();
        if (debut == null) debut = LocalDate.now().minusMonths(1);
        if (fin   == null) fin   = LocalDate.now();

        final LocalDate debutFinal = debut;
        final LocalDate finFinal   = fin;

        periodeRapport = "du " + debut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                       + " au " + fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        final Integer ufrIdFinal    = ufrCombo.getValue()    != null ? ufrCombo.getValue().getId()    : null;
        final Integer classeIdFinal = classeCombo.getValue() != null ? classeCombo.getValue().getId() : null;

        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws SQLException {
                Map<String, Object> rapport = new HashMap<>();

                // Stats globales (utilisateurs, salles, cours)
                Map<String, Object> globales = statsService.getStatistiquesGlobales();
                rapport.put("totalCours",          globales.get("totalCours"));
                rapport.put("totalSalles",         globales.get("totalSalles"));
                rapport.put("utilisateursParRole", globales.get("utilisateursParRole"));

                // Stats de période (occupation, taux, types, détails salles)
                Map<String, Object> periode = statsService.getRapportStats(
                        debutFinal, finFinal, ufrIdFinal, classeIdFinal);
                rapport.put("occupationParHeure",   periode.get("occupationParHeure"));
                rapport.put("tauxOccupationGlobal", periode.get("tauxOccupationGlobal"));
                rapport.put("coursParType",         periode.get("coursParType"));
                rapport.put("sallesCritiques",      periode.get("sallesCritiques"));

                // Évolution mensuelle
                rapport.put("evolution", statsService.getEvolutionMensuelle());

                // Réservations
                int totalRes = 0;
                try {
                    for (Reservation r : reservationDAO.listerTous()) {
                        if (!"confirmee".equals(r.getStatut()) && !"en_cours".equals(r.getStatut())) continue;
                        LocalDate dr = LocalDate.parse(r.getDateReservation());
                        if (!dr.isBefore(debutFinal) && !dr.isAfter(finFinal)) totalRes++;
                    }
                } catch (Exception ignored) {}
                rapport.put("totalReservations", totalRes);

                return rapport;
            }

            @Override
            protected void succeeded() {
                dernierRapport = getValue();
                afficherRapport(dernierRapport);
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }

            @Override
            protected void failed() {
                logger.error("Erreur génération rapport", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                afficherErreur("Erreur : " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }
    
    /**
     * Calcule l'occupation par créneau horaire avec filtres
     */
    private Map<String, Integer> calculerOccupationParHeure(LocalDate debut, LocalDate fin, 
            Ufr ufr, Classe classe) throws SQLException {
        Map<String, Integer> occupation = new LinkedHashMap<>();
        String[] creneaux = {"08h-10h", "10h-12h", "12h-14h", "14h-16h", "16h-18h"};
        for (String c : creneaux) occupation.put(c, 0);
        
        List<Salle> salles = salleService.listerTous();
        
        // Filtrer les salles par UFR si nécessaire
        if (ufr != null) {
            List<Salle> sallesFiltrees = new ArrayList<>();
            for (Salle s : salles) {
                Batiment batiment = new BatimentService().trouverParId(s.getBatimentId());
                if (batiment != null && batiment.getUfrId() == ufr.getId()) {
                    sallesFiltrees.add(s);
                }
            }
            salles = sallesFiltrees;
        }
        
        final LocalDate debutFinal = debut;
        final LocalDate finFinal = fin;
        
        LocalDate current = debutFinal;
        while (!current.isAfter(finFinal)) {
            String dateStr = current.toString();
            List<Creneau> creneauxJour = planningService.getCreneauxParJour(dateStr);
            
            for (Creneau c : creneauxJour) {
                if (c.getSalleId() == null) continue;
                if ("annule".equals(c.getStatut())) continue;
                
                if (classe != null) {
                    Cours cours = new CoursService().trouverParId(c.getCoursId());
                    if (cours == null || cours.getClasseId() != classe.getId()) continue;
                }
                
                boolean salleOk = false;
                for (Salle s : salles) {
                    if (s.getId() == c.getSalleId()) {
                        salleOk = true;
                        break;
                    }
                }
                if (!salleOk) continue;
                
                int h = Integer.parseInt(c.getHeureDebut().split(":")[0]);
                if (h >= 8 && h < 10) occupation.put("08h-10h", occupation.get("08h-10h") + 1);
                else if (h >= 10 && h < 12) occupation.put("10h-12h", occupation.get("10h-12h") + 1);
                else if (h >= 12 && h < 14) occupation.put("12h-14h", occupation.get("12h-14h") + 1);
                else if (h >= 14 && h < 16) occupation.put("14h-16h", occupation.get("14h-16h") + 1);
                else if (h >= 16 && h < 18) occupation.put("16h-18h", occupation.get("16h-18h") + 1);
            }
            current = current.plusDays(1);
        }
        
        return occupation;
    }
    
    /**
     * Calcule le taux d'occupation global
     */
    private double calculerTauxOccupationGlobal(LocalDate debut, LocalDate fin, 
            Ufr ufr, Classe classe) throws SQLException {
        Map<String, Integer> occupation = calculerOccupationParHeure(debut, fin, ufr, classe);
        int totalCreneaux = 0;
        for (int val : occupation.values()) totalCreneaux += val;
        
        List<Salle> salles = salleService.listerTous();
        if (ufr != null) {
            List<Salle> sallesFiltrees = new ArrayList<>();
            for (Salle s : salles) {
                Batiment batiment = new BatimentService().trouverParId(s.getBatimentId());
                if (batiment != null && batiment.getUfrId() == ufr.getId()) {
                    sallesFiltrees.add(s);
                }
            }
            salles = sallesFiltrees;
        }
        
        final LocalDate debutFinal = debut;
        final LocalDate finFinal = fin;
        
        long jours = java.time.temporal.ChronoUnit.DAYS.between(debutFinal, finFinal) + 1;
        int totalPossibles = salles.size() * 5 * (int) jours;
        
        if (totalPossibles == 0) return 0;
        return (double) totalCreneaux / totalPossibles * 100;
    }
    
    /**
     * Calcule l'évolution mensuelle sur les 12 derniers mois
     */
    private Map<String, Double> calculerEvolutionMensuelle(Ufr ufr, Classe classe) throws SQLException {
        Map<String, Double> evolution = new LinkedHashMap<>();
        LocalDate maintenant = LocalDate.now();
        
        for (int i = 11; i >= 0; i--) {
            final int moisIndex = i;
            final LocalDate debut = maintenant.minusMonths(moisIndex).withDayOfMonth(1);
            final LocalDate fin = debut.plusMonths(1).minusDays(1);
            
            double taux = calculerTauxOccupationGlobal(debut, fin, ufr, classe);
            String mois = debut.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            evolution.put(mois, Math.round(taux * 100) / 100.0);
        }
        
        return evolution;
    }
    
    /**
     * Calcule la répartition par type de cours
     */
    private Map<String, Integer> calculerRepartitionParType(LocalDate debut, LocalDate fin,
            Ufr ufr, Classe classe) throws SQLException {
        Map<String, Integer> repartition = new HashMap<>();
        repartition.put("CM", 0);
        repartition.put("TD", 0);
        repartition.put("TP", 0);
        
        List<Salle> salles = salleService.listerTous();
        if (ufr != null) {
            List<Salle> sallesFiltrees = new ArrayList<>();
            for (Salle s : salles) {
                Batiment batiment = new BatimentService().trouverParId(s.getBatimentId());
                if (batiment != null && batiment.getUfrId() == ufr.getId()) {
                    sallesFiltrees.add(s);
                }
            }
            salles = sallesFiltrees;
        }
        
        final LocalDate debutFinal = debut;
        final LocalDate finFinal = fin;
        
        LocalDate current = debutFinal;
        while (!current.isAfter(finFinal)) {
            String dateStr = current.toString();
            List<Creneau> creneauxJour = planningService.getCreneauxParJour(dateStr);
            
            for (Creneau c : creneauxJour) {
                if (c.getSalleId() == null) continue;
                if ("annule".equals(c.getStatut())) continue;
                
                if (classe != null) {
                    Cours cours = new CoursService().trouverParId(c.getCoursId());
                    if (cours == null || cours.getClasseId() != classe.getId()) continue;
                }
                
                boolean salleOk = false;
                for (Salle s : salles) {
                    if (s.getId() == c.getSalleId()) {
                        salleOk = true;
                        break;
                    }
                }
                if (!salleOk) continue;
                
                Cours cours = new CoursService().trouverParId(c.getCoursId());
                if (cours != null) {
                    String type = cours.getTypeCours();
                    repartition.put(type, repartition.getOrDefault(type, 0) + 1);
                }
            }
            current = current.plusDays(1);
        }
        
        return repartition;
    }
    
    /**
     * Calcule les salles critiques (surchargées ou sous-utilisées)
     */
    private List<Map<String, Object>> calculerSallesCritiques(LocalDate debut, LocalDate fin,
            Ufr ufr, Classe classe) throws SQLException {
        List<Map<String, Object>> resultats = new ArrayList<>();
        List<Salle> salles = salleService.listerTous();
        
        if (ufr != null) {
            List<Salle> sallesFiltrees = new ArrayList<>();
            for (Salle s : salles) {
                Batiment batiment = new BatimentService().trouverParId(s.getBatimentId());
                if (batiment != null && batiment.getUfrId() == ufr.getId()) {
                    sallesFiltrees.add(s);
                }
            }
            salles = sallesFiltrees;
        }
        
        final LocalDate debutFinal = debut;
        final LocalDate finFinal = fin;
        
        long jours = java.time.temporal.ChronoUnit.DAYS.between(debutFinal, finFinal) + 1;
        int totalCreneauxPossibles = 5 * (int) jours;
        
        for (Salle salle : salles) {
            int nbCreneauxOccupes = 0;
            int heuresUtilisees = 0;
            
            LocalDate current = debutFinal;
            while (!current.isAfter(finFinal)) {
                String dateStr = current.toString();
                List<Creneau> creneauxJour = planningService.getCreneauxParSalle(salle.getId());
                
                for (Creneau c : creneauxJour) {
                    if (c.getJour().equals(dateStr) && !"annule".equals(c.getStatut())) {
                        nbCreneauxOccupes++;
                        int duree = calculerDureeHeures(c.getHeureDebut(), c.getHeureFin());
                        heuresUtilisees += duree;
                    }
                }
                current = current.plusDays(1);
            }
            
            double taux = totalCreneauxPossibles > 0 ? (double) nbCreneauxOccupes / totalCreneauxPossibles * 100 : 0;
            
            Map<String, Object> info = new HashMap<>();
            info.put("salle", salle.getNumero());
            info.put("type", salle.getType());
            info.put("capacite", salle.getCapacite());
            info.put("taux", Math.round(taux));
            info.put("heures", heuresUtilisees);
            
            if (taux > 70) {
                info.put("statut", "surchargée");
            } else if (taux < 20) {
                info.put("statut", "sous-utilisée");
            } else {
                info.put("statut", "normale");
            }
            
            resultats.add(info);
        }
        
        resultats.sort((a, b) -> Double.compare(
            (double) b.get("taux"), (double) a.get("taux")
        ));
        
        return resultats;
    }
    
    /**
     * Compte les réservations sur la période
     */
    private int compterReservations(LocalDate debut, LocalDate fin, Ufr ufr, Classe classe) throws SQLException {
        List<Reservation> toutes = reservationDAO.listerTous();
        int compteur = 0;
        
        final LocalDate debutFinal = debut;
        final LocalDate finFinal = fin;
        
        for (Reservation r : toutes) {
            if (!"confirmee".equals(r.getStatut()) && !"en_cours".equals(r.getStatut())) continue;
            
            LocalDate dateRes = LocalDate.parse(r.getDateReservation());
            if (dateRes.isBefore(debutFinal) || dateRes.isAfter(finFinal)) continue;
            
            if (ufr != null) {
                Salle salle = salleService.trouverParId(r.getSalleId());
                if (salle != null) {
                    Batiment batiment = new BatimentService().trouverParId(salle.getBatimentId());
                    if (batiment == null || batiment.getUfrId() != ufr.getId()) continue;
                }
            }
            
            if (classe != null && r.getClasseId() != null && r.getClasseId() != classe.getId()) continue;
            
            compteur++;
        }
        
        return compteur;
    }
    
    private int calculerDureeHeures(String debut, String fin) {
        try {
            String[] d = debut.split(":");
            String[] f = fin.split(":");
            int debutMin = Integer.parseInt(d[0]) * 60 + Integer.parseInt(d[1]);
            int finMin = Integer.parseInt(f[0]) * 60 + Integer.parseInt(f[1]);
            return (finMin - debutMin) / 60;
        } catch (Exception e) {
            return 1;
        }
    }
    
    /**
     * Affiche le rapport dans l'interface.
     * @param rapport les données du rapport
     */
    private void afficherRapport(Map<String, Object> rapport) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> roles = (Map<String, Integer>)
                rapport.getOrDefault("utilisateursParRole", new HashMap<>());

        totalCoursLabel.setText(String.valueOf(convertToInt(rapport.get("totalCours"))));
        totalSallesLabel.setText(String.valueOf(convertToInt(rapport.get("totalSalles"))));
        totalEnseignantsLabel.setText(String.valueOf(roles.getOrDefault("enseignant", 0)));
        totalEtudiantsLabel.setText(String.valueOf(roles.getOrDefault("etudiant", 0)));
        totalReservationsLabel.setText(String.valueOf(convertToInt(rapport.get("totalReservations"))));
        double taux = convertToDouble(rapport.get("tauxOccupationGlobal"));
        tauxOccupationLabel.setText(String.format("%.1f%%", taux));

        @SuppressWarnings("unchecked")
        Map<String, Integer> occupationParHeure = (Map<String, Integer>)
                rapport.getOrDefault("occupationParHeure", new HashMap<>());

        occupationChart.getData().clear();
        XYChart.Series<String, Number> seriesOcc = new XYChart.Series<>();
        seriesOcc.setName("Nombre de cours");
        int maxVal = 0;
        for (String h : new String[]{"08h-10h","10h-12h","12h-14h","14h-16h","16h-18h"}) {
            int v = occupationParHeure.getOrDefault(h, 0);
            if (v > maxVal) maxVal = v;
            seriesOcc.getData().add(new XYChart.Data<>(h, v));
        }
        occupationChart.getData().add(seriesOcc);

        if (occupationYAxis != null) {
            occupationYAxis.setAutoRanging(false);
            occupationYAxis.setLowerBound(0);
            occupationYAxis.setUpperBound(Math.max(maxVal + 1, 5));
            occupationYAxis.setTickUnit(1);
        }

        javafx.application.Platform.runLater(() -> {
            for (XYChart.Series<String, Number> s : occupationChart.getData()) {
                if (s.getNode() != null) s.getNode().setStyle("");
                for (XYChart.Data<String, Number> d : s.getData()) {
                    if (d.getNode() != null)
                        d.getNode().setStyle("-fx-bar-fill: #6B4226; -fx-background-radius: 4 4 0 0;");
                }
            }
        });

        // ── PieChart répartition ──
        @SuppressWarnings("unchecked")
        Map<String, Integer> coursParType = (Map<String, Integer>)
                rapport.getOrDefault("coursParType", new HashMap<>());

        repartitionChart.getData().clear();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        int cm = coursParType.getOrDefault("CM", 0);
        int td = coursParType.getOrDefault("TD", 0);
        int tp = coursParType.getOrDefault("TP", 0);
        if (cm > 0) pieData.add(new PieChart.Data("CM (" + cm + ")", cm));
        if (td > 0) pieData.add(new PieChart.Data("TD (" + td + ")", td));
        if (tp > 0) pieData.add(new PieChart.Data("TP (" + tp + ")", tp));
        if (pieData.isEmpty()) pieData.add(new PieChart.Data("Aucun cours", 1));
        repartitionChart.setData(pieData);
        repartitionChart.setLegendVisible(true);
        repartitionChart.setLegendSide(javafx.geometry.Side.RIGHT);

        javafx.application.Platform.runLater(() -> {
            String[] cols = {"#3D261A", "#6B4226", "#D39A43", "#8C6D58", "#B8860B"};
            int idx = 0;
            for (PieChart.Data d : repartitionChart.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle("-fx-pie-color: " + cols[idx % cols.length] + ";");
                idx++;
            }
            for (javafx.scene.Node n : repartitionChart.lookupAll(".chart-pie-label"))
                n.setStyle("-fx-fill: #3D261A; -fx-font-size: 11px; -fx-font-weight: bold;");
            for (javafx.scene.Node n : repartitionChart.lookupAll(".chart-legend-item"))
                n.setStyle("-fx-text-fill: #3D261A; -fx-font-size: 12px;");
        });

        // ── LineChart évolution ──
        @SuppressWarnings("unchecked")
        Map<String, Double> evolution = (Map<String, Double>)
                rapport.getOrDefault("evolution", new HashMap<>());

        tendanceChart.getData().clear();
        XYChart.Series<String, Number> tendanceSeries = new XYChart.Series<>();
        tendanceSeries.setName("Taux d'occupation (%)");
        double maxTaux = 0;
        for (Map.Entry<String, Double> e : evolution.entrySet()) {
            tendanceSeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            if (e.getValue() > maxTaux) maxTaux = e.getValue();
        }
        tendanceChart.getData().add(tendanceSeries);

        if (tendanceYAxis != null) {
            double borne = Math.max(maxTaux * 1.3, 10);
            tendanceYAxis.setAutoRanging(false);
            tendanceYAxis.setLowerBound(0);
            tendanceYAxis.setUpperBound(Math.round(borne));
            tendanceYAxis.setTickUnit(Math.max(1, Math.round(borne / 8)));
        }

        javafx.application.Platform.runLater(() -> {
            for (XYChart.Series<String, Number> s : tendanceChart.getData()) {
                if (s.getNode() != null)
                    s.getNode().setStyle("-fx-stroke: #D39A43; -fx-stroke-width: 3px;");
                for (XYChart.Data<String, Number> d : s.getData()) {
                    if (d.getNode() != null)
                        d.getNode().setStyle("-fx-background-color: #6B4226, #FBF8F3; -fx-background-radius: 6;");
                }
            }
            for (javafx.scene.Node n : tendanceChart.lookupAll(".chart-legend-item"))
                n.setStyle("-fx-text-fill: #3D261A; -fx-font-size: 12px;");
        });

        // ── Tableau détails salles - données réelles du service ──
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sallesCritiques = (List<Map<String, Object>>)
                rapport.getOrDefault("sallesCritiques", new ArrayList<>());

        if (sallesCritiques.isEmpty()) {
            Map<String, Object> vide = new HashMap<>();
            vide.put("salle", "Aucune donnée"); vide.put("type", ""); vide.put("capacite", "");
            vide.put("taux", 0.0); vide.put("heures", 0); vide.put("statut", "");
            detailsTable.setItems(FXCollections.observableArrayList(vide));
        } else {
            detailsTable.setItems(FXCollections.observableArrayList(sallesCritiques));
        }
    }

    private int convertToInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Double) return ((Double) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Exporte le rapport en PDF.
     */
    private void exporterPDF() {
        if (dernierRapport == null) {
            afficherErreur("Aucun rapport à exporter. Générez d'abord un rapport.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        
        String nomFichier = "Rapport_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            
            afficherNotification("Export PDF", "Génération du rapport en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    exportService.exporterRapportPDF(dernierRapport, periodeRapport, chemin);
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
     * Exporte le rapport en Excel.
     */
    private void exporterExcel() {
        if (dernierRapport == null) {
            afficherErreur("Aucun rapport à exporter. Générez d'abord un rapport.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );
        
        String nomFichier = "Rapport_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            
            afficherNotification("Export Excel", "Génération du rapport en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    exportService.exporterRapportExcel(dernierRapport, periodeRapport, chemin);
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
        if (detailsTable != null && detailsTable.getScene() != null && detailsTable.getScene().getWindow() instanceof Stage) {
            return (Stage) detailsTable.getScene().getWindow();
        }
        return null;
    }
    
    @FXML
    protected void handleRetour() {
        Stage stage = getStageFromScene();
        if (stage == null) {
            logger.error("❌ Stage null pour retour");
            afficherErreur("Erreur de navigation");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordAdmin.fxml"));
            Parent root = loader.load();
            
            TableauBordAdminControleur controleur = loader.getController();
            if (controleur != null) {
                controleur.initialiserAvecUtilisateur(utilisateurConnecte, stage);
            }
            
            Scene scene = new Scene(root);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            stage.setScene(scene);
            stage.setTitle("SCHEDULER - Administrateur");
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner au tableau de bord");
        }
    }
}