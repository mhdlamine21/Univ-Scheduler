package scheduler.controleur;

import scheduler.dao.DemandeInscriptionDAO;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import scheduler.dao.CreneauDAO;
import scheduler.modele.Creneau;
import javafx.scene.layout.Priority;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Random;

public class TableauBordAdminControleur extends TableauBordControleur {
    
    @FXML private TabPane tabPane;
    @FXML private Label totalUtilisateursLabel;
    @FXML private Label totalEnseignantsLabel;
    @FXML private Label totalEtudiantsLabel;
    @FXML private Label totalSallesLabel;
    @FXML private Label totalCoursLabel;
    @FXML private Label demandesEnAttenteLabel;
    @FXML private Button actualiserButton;
    
    @FXML private PieChart repartitionUtilisateursChart;
    @FXML private BarChart<String, Number> occupationChart;
    
    @FXML private TableView<DemandeInscription> demandesTable;
    @FXML private TableColumn<DemandeInscription, String> nomDemandeColumn;
    @FXML private TableColumn<DemandeInscription, String> emailDemandeColumn;
    @FXML private TableColumn<DemandeInscription, String> roleDemandeColumn;
    @FXML private TableColumn<DemandeInscription, String> dateDemandeColumn;
    @FXML private TableColumn<DemandeInscription, Void> actionsDemandeColumn;
    
    @FXML private ProgressIndicator chargementIndicator;
    @FXML private Button notificationButton;
    @FXML private Label notificationBadge;
    @FXML private Label signalementBadge;
    
 // COMPOSANTS SIDEBAR (À AJOUTER)
    @FXML private Button sidebarDashboardBtn;
    @FXML private Button sidebarUtilisateursBtn;
    @FXML private Button sidebarComptesBtn;
    @FXML private Button sidebarDemandesBtn;
    @FXML private Button sidebarSignalementsBtn;
    @FXML private Button sidebarStructureBtn;
    @FXML private Button sidebarUfrBtn;
    @FXML private Button sidebarBatimentsBtn;
    @FXML private Button sidebarSallesBtn;
    @FXML private Button sidebarEquipementsBtn;
    @FXML private Button sidebarClassesBtn;
    @FXML private Button sidebarReservationsBtn;
    @FXML private Button sidebarCoursReportesBtn;
    @FXML private Button sidebarHistoriqueBtn;
    @FXML private Button sidebarCarteBtn;
    @FXML private Button sidebarRapportsBtn;

    @FXML private VBox sousMenuUtilisateurs;
    @FXML private VBox sousMenuStructure;

    // ÉTAT SIDEBAR
    private boolean sousMenuUtilisateursVisible = true;
    private boolean sousMenuStructureVisible = true;

    
    private StatistiqueService statsService;
    private UtilisateurService utilisateurService;
    private UfrService ufrService;
    private SalleService salleService;
    private CoursService coursService;
    private DemandeInscriptionDAO demandeDAO;
    private ClasseService classeService;
    private CreneauDAO creneauDAO;

    
    private int totalNotifications = 0;
    private PauseTransition refreshTimer;
    private int totalSignalementsNonTraites = 0;
    
    @FXML private CategoryAxis occupationXAxis;
    @FXML private NumberAxis occupationYAxis;

    
    @Override
    public void initialize() {
        super.initialize();
        
        this.statsService = new StatistiqueService();
        this.utilisateurService = new UtilisateurService();
        this.ufrService = new UfrService();
        this.salleService = new SalleService();
        this.coursService = new CoursService();
        this.demandeDAO = new DemandeInscriptionDAO();
        this.classeService = new ClasseService();
        this.creneauDAO = new CreneauDAO();
        
        configurerGraphiqueOccupation();
        
        
        
        if (actualiserButton != null) {
            actualiserButton.setOnAction(e -> rafraichirDonnees());
        }
        
        if (notificationButton != null) {
            notificationButton.setOnAction(e -> ouvrirNotifications());
        }
        
        // Démarrer le rafraîchissement automatique
        refreshTimer = new PauseTransition(Duration.seconds(30));
        refreshTimer.setOnFinished(e -> {
            rafraichirNotifications();
            refreshTimer.play();
        });
        refreshTimer.play();
        configurerSidebar();
        configurerEvenementsSidebar();
    }
    
    @Override
    protected void initialiserTableauBord() {
        configurerTableauDemandes();
        chargerStatistiques();
        chargerDemandesEnAttente();
        rafraichirNotifications();
        rafraichirSignalements();
        // Initialiser le contrôleur fils de la liste EDT (onglet Planning)
        if (listeEmploisTempsAdminControleur != null && utilisateurConnecte != null) {
            listeEmploisTempsAdminControleur.setOrigineRole("admin");
            listeEmploisTempsAdminControleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
        }
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerStatistiques();
        chargerDemandesEnAttente();
        rafraichirNotifications();
        rafraichirSignalements();
        afficherNotification("Actualisation", "Données actualisées avec succès");
    }
    
    @FXML private ListeEmploisTempsControleur listeEmploisTempsAdminControleur;

    /**
     * Navigue vers une page FXML.
     * @param fxmlPath le chemin du fichier FXML
     */
    @FXML private StackPane contenuPrincipal;
    private void naviguerVers(String fxmlPath) {
        try {
            System.out.println("🔍 Navigation vers: " + fxmlPath);
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("❌ Fichier non trouvé: " + fxmlPath);
                afficherErreur("Page introuvable: " + fxmlPath);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            
            Object controleur = loader.getController();
            if (controleur instanceof TableauBordControleur) {
                ((TableauBordControleur) controleur).initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            }
            
            // ✅ Vérifier que contenuPrincipal existe
            if (contenuPrincipal == null) {
                // Fallback: remplacer toute la scène
                Scene scene = new Scene(root);
                String css = getClass().getResource("/css/style.css").toExternalForm();
                if (css != null) scene.getStylesheets().add(css);
                primaryStage.setScene(scene);
            } else {
                contenuPrincipal.getChildren().clear();
                contenuPrincipal.getChildren().add(root);
            }
            
        } catch (IOException e) {
            logger.error("❌ Erreur navigation vers {}", fxmlPath, e);
            afficherErreur("Impossible d'accéder à cette page: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDemandes() {
        naviguerVers("/fxml/DemandesInscription.fxml");
    }
    
    
    @FXML
    private void handleRapports() {
        naviguerVers("/fxml/Rapports.fxml");
    }
    
    @FXML
    private void handleSignalements() {
        naviguerVers("/fxml/Signalement.fxml");
    }
    
 // MÉTHODES DE SÉLECTION D'ONGLET

    public void selectionnerOngletRecherche() {
        if (tabPane != null) {
            for (Tab tab : tabPane.getTabs()) {
                if ("Recherche".equals(tab.getText())) {
                    tabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }

    public void selectionnerOngletCarte() {
        if (tabPane != null) {
            for (Tab tab : tabPane.getTabs()) {
                if ("Carte".equals(tab.getText())) {
                    tabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }
    
    private void configurerTableauDemandes() {
        if (nomDemandeColumn == null) return;
        
        nomDemandeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getPrenom() + " " + cellData.getValue().getNom()
            )
        );
        
        emailDemandeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getEmail()
            )
        );
        
        roleDemandeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> traduireRoleDemande(cellData.getValue().getRoleDemande())
            )
        );
        
        dateDemandeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getDateDemande() != null ?
                    cellData.getValue().getDateDemande().toLocalDate().toString() : ""
            )
        );
        
        actionsDemandeColumn.setCellFactory(param -> new TableCell<DemandeInscription, Void>() {
            private final Button validerBtn = new Button("✓");
            private final Button refuserBtn = new Button("✗");
            private final Button detailsBtn = new Button("👁️");
            
            {
                detailsBtn.setStyle("-fx-background-color: #6B4226; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;");
                validerBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;");
                refuserBtn.setStyle("-fx-background-color: #9E2A2B; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;");
                
                validerBtn.setOnAction(event -> {
                    DemandeInscription demande = getTableView().getItems().get(getIndex());
                    validerDemande(demande);
                });
                
                refuserBtn.setOnAction(event -> {
                    DemandeInscription demande = getTableView().getItems().get(getIndex());
                    refuserDemande(demande);
                });
                
                detailsBtn.setOnAction(event -> {
                    DemandeInscription demande = getTableView().getItems().get(getIndex());
                    afficherDetailsDemande(demande);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, detailsBtn, validerBtn, refuserBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        
        demandesTable.setPlaceholder(new Label("Aucune demande en attente"));
    }
    
    
    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Long) return ((Long) obj).intValue();
        return 0;
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Integer) return ((Integer) obj).doubleValue();
        if (obj instanceof Long) return ((Long) obj).doubleValue();
        return 0.0;
    }
    
    
    private void rafraichirSignalements() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws SQLException {
                SignalementService signalementService = new SignalementService();
                return signalementService.compterNonTraites();
            }
            
            @Override
            protected void succeeded() {
                totalSignalementsNonTraites = getValue();
                if (signalementBadge != null) {
                    if (totalSignalementsNonTraites > 0) {
                        signalementBadge.setText(String.valueOf(totalSignalementsNonTraites));
                        signalementBadge.setVisible(true);
                    } else {
                        signalementBadge.setVisible(false);
                    }
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement signalements", getException());
            }
        };
        new Thread(task).start();
    }
    
    private void chargerStatistiques() {
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws SQLException {
                Map<String, Object> stats = new HashMap<>();
                
                List<Utilisateur> utilisateurs = utilisateurService.listerTous();
                int total = utilisateurs.size();
                int admins = 0, gestionnaires = 0, enseignants = 0, etudiants = 0;
                
                for (Utilisateur u : utilisateurs) {
                    switch (u.getRole()) {
                        case "admin": admins++; break;
                        case "gestionnaire": gestionnaires++; break;
                        case "enseignant": enseignants++; break;
                        case "etudiant": etudiants++; break;
                    }
                }
                
                Map<String, Integer> utilisateursParRole = new HashMap<>();
                utilisateursParRole.put("admin", admins);
                utilisateursParRole.put("gestionnaire", gestionnaires);
                utilisateursParRole.put("enseignant", enseignants);
                utilisateursParRole.put("etudiant", etudiants);
                
                stats.put("totalUtilisateurs", total);
                stats.put("utilisateursParRole", utilisateursParRole);
                
                List<Salle> salles = salleService.listerTous();
                stats.put("totalSalles", salles.size());
                
                List<Cours> tousCours = coursService.listerTous();
                stats.put("totalCours", tousCours.size());
                
                // ✅ OCCUPATION PAR HEURE AVEC creneauDAO
                Map<String, Integer> occupationParHeure = new LinkedHashMap<>();
                occupationParHeure.put("08h-10h", 0);
                occupationParHeure.put("10h-12h", 0);
                occupationParHeure.put("12h-14h", 0);
                occupationParHeure.put("14h-16h", 0);
                occupationParHeure.put("16h-18h", 0);
                
                String aujourdhui = LocalDate.now().toString();
                try {
                    List<Creneau> creneauxJour = creneauDAO.listerParJour(aujourdhui);
                    System.out.println("📅 " + aujourdhui + " - " + creneauxJour.size() + " créneaux trouvés");
                    for (Creneau c : creneauxJour) {
                        if (c.getSalleId() != null && !"annule".equals(c.getStatut())) {
                            int h = Integer.parseInt(c.getHeureDebut().split(":")[0]);
                            if (h >= 8 && h < 10) occupationParHeure.put("08h-10h", occupationParHeure.get("08h-10h") + 1);
                            else if (h >= 10 && h < 12) occupationParHeure.put("10h-12h", occupationParHeure.get("10h-12h") + 1);
                            else if (h >= 12 && h < 14) occupationParHeure.put("12h-14h", occupationParHeure.get("12h-14h") + 1);
                            else if (h >= 14 && h < 16) occupationParHeure.put("14h-16h", occupationParHeure.get("14h-16h") + 1);
                            else if (h >= 16 && h < 18) occupationParHeure.put("16h-18h", occupationParHeure.get("16h-18h") + 1);
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Erreur récupération créneaux", e);
                }
                
                stats.put("occupationParHeure", occupationParHeure);
                
                // Taux d'occupation global
                int totalOccupes = 0;
                for (int val : occupationParHeure.values()) totalOccupes += val;
                int totalPossibles = salles.size() * 5;
                double tauxGlobal = totalPossibles > 0 ? (double) totalOccupes / totalPossibles * 100 : 0;
                stats.put("tauxOccupationGlobal", Math.round(tauxGlobal * 100) / 100.0);
                
                return stats;
            }
            
            @Override
            protected void succeeded() {
                Map<String, Object> stats = getValue();
                mettreAJourStatistiques(stats);
                mettreAJourGraphiqueOccupation(stats);
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement stats", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    
    
    private void configurerGraphiqueOccupation() {
        if (occupationChart == null) return;

        occupationChart.getData().clear();
        occupationChart.setTitle("Occupation des salles par créneau");
        occupationChart.setLegendVisible(false); // une seule série → pas besoin de légende
        occupationChart.setAnimated(false);
        occupationChart.setStyle("-fx-background-color: white;");

        if (occupationXAxis != null) {
            occupationXAxis.setLabel("Créneaux horaires");
            occupationXAxis.setTickLabelFill(javafx.scene.paint.Color.web("#1A202C"));
            occupationXAxis.setTickLabelFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12));
            occupationXAxis.setTickLabelsVisible(true);
        }

        if (occupationYAxis != null) {
            occupationYAxis.setLabel("Nombre de cours");
            occupationYAxis.setAutoRanging(false);
            occupationYAxis.setLowerBound(0);
            occupationYAxis.setUpperBound(5);
            occupationYAxis.setTickUnit(1);
            occupationYAxis.setTickLabelFill(javafx.scene.paint.Color.web("#1A202C"));
            occupationYAxis.setTickLabelFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12));
            occupationYAxis.setTickLabelsVisible(true);
        }
    }
    
    private void configurerEvenementsSidebar() {
        // Dashboard
        sidebarDashboardBtn.setOnAction(e -> afficherDashboard());
        
        // Sous-menu Utilisateurs - toggle
        sidebarUtilisateursBtn.setOnAction(e -> {
            sousMenuUtilisateursVisible = !sousMenuUtilisateursVisible;
            sousMenuUtilisateurs.setVisible(sousMenuUtilisateursVisible);
            sousMenuUtilisateurs.setManaged(sousMenuUtilisateursVisible);
            sidebarUtilisateursBtn.setText(sousMenuUtilisateursVisible ? "👥 Utilisateurs ▼" : "👥 Utilisateurs ▶");
        });
        
        // Navigation Utilisateurs
        sidebarComptesBtn.setOnAction(e -> naviguerVers("/fxml/GestionUtilisateurs.fxml"));
        sidebarDemandesBtn.setOnAction(e -> naviguerVers("/fxml/DemandesInscription.fxml"));
        sidebarSignalementsBtn.setOnAction(e -> naviguerVers("/fxml/Signalement.fxml"));
        
        // Sous-menu Structure - toggle
        sidebarStructureBtn.setOnAction(e -> {
            sousMenuStructureVisible = !sousMenuStructureVisible;
            sousMenuStructure.setVisible(sousMenuStructureVisible);
            sousMenuStructure.setManaged(sousMenuStructureVisible);
            sidebarStructureBtn.setText(sousMenuStructureVisible ? "🏛️ Structure ▼" : "🏛️ Structure ▶");
        });
        
        // Navigation Structure
        sidebarUfrBtn.setOnAction(e -> naviguerVers("/fxml/GestionUfr.fxml"));
        sidebarBatimentsBtn.setOnAction(e -> naviguerVers("/fxml/GestionBatiments.fxml"));
        sidebarSallesBtn.setOnAction(e -> naviguerVers("/fxml/GestionSalles.fxml"));
        sidebarEquipementsBtn.setOnAction(e -> naviguerVers("/fxml/GestionEquipements.fxml"));
        sidebarClassesBtn.setOnAction(e -> naviguerVers("/fxml/GestionClasses.fxml"));
        
        // Navigation autres
        sidebarReservationsBtn.setOnAction(e -> naviguerVers("/fxml/Reservation.fxml"));
        if (sidebarCoursReportesBtn != null) sidebarCoursReportesBtn.setOnAction(e -> ouvrirModalCoursReportes());
        sidebarHistoriqueBtn.setOnAction(e -> naviguerVers("/fxml/HistoriqueReservation.fxml"));
        sidebarCarteBtn.setOnAction(e -> naviguerVers("/fxml/Carte.fxml"));
        sidebarRapportsBtn.setOnAction(e -> naviguerVers("/fxml/Rapports.fxml"));
    }
    
    private void mettreAJourPieChart(Map<String, Integer> utilisateursParRole) {
        if (repartitionUtilisateursChart == null) return;

        int enseignants  = utilisateursParRole.getOrDefault("enseignant", 0);
        int etudiants    = utilisateursParRole.getOrDefault("etudiant", 0);
        int gestionnaires = utilisateursParRole.getOrDefault("gestionnaire", 0);
        int admins       = utilisateursParRole.getOrDefault("admin", 0);

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (enseignants  > 0) pieData.add(new PieChart.Data("Enseignants ("  + enseignants  + ")", enseignants));
        if (etudiants    > 0) pieData.add(new PieChart.Data("Étudiants ("    + etudiants    + ")", etudiants));
        if (gestionnaires > 0) pieData.add(new PieChart.Data("Gestionnaires (" + gestionnaires + ")", gestionnaires));
        if (admins       > 0) pieData.add(new PieChart.Data("Administrateurs (" + admins + ")", admins));

        repartitionUtilisateursChart.setData(pieData);
        repartitionUtilisateursChart.setTitle("Répartition des utilisateurs");
        repartitionUtilisateursChart.setLabelsVisible(true);
        repartitionUtilisateursChart.setLegendVisible(true);
        repartitionUtilisateursChart.setLegendSide(javafx.geometry.Side.RIGHT);
        repartitionUtilisateursChart.setStyle("-fx-background-color: white;");

        // Les couleurs et labels doivent être appliqués APRÈS le layout JavaFX
        javafx.application.Platform.runLater(() -> {
            String[] couleurs = {"#6B4226", "#9E5D32", "#D39A43", "#3D261A"};
            int idx = 0;
            for (PieChart.Data data : repartitionUtilisateursChart.getData()) {
                javafx.scene.Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + couleurs[idx % couleurs.length] + ";");
                }
                idx++;
            }
            // Labels des parts : texte noir lisible
            for (javafx.scene.Node node : repartitionUtilisateursChart.lookupAll(".chart-pie-label")) {
                node.setStyle("-fx-fill: #1A202C; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
            // Légende : texte noir lisible
            for (javafx.scene.Node node : repartitionUtilisateursChart.lookupAll(".chart-legend-item")) {
                node.setStyle("-fx-text-fill: #1A202C; -fx-font-size: 12px;");
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    private void mettreAJourGraphiqueOccupation(Map<String, Object> stats) {
        if (occupationChart == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Integer> occupationParHeure = (Map<String, Integer>) 
                stats.getOrDefault("occupationParHeure", new HashMap<>());

        occupationChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de cours");

        String[] heures = {"08h-10h", "10h-12h", "12h-14h", "14h-16h", "16h-18h"};
        int maxValeur = 0;
        
        for (String heure : heures) {
            int valeur = occupationParHeure.getOrDefault(heure, 0);
            if (valeur > maxValeur) maxValeur = valeur;
            series.getData().add(new XYChart.Data<>(heure, valeur));
        }

        occupationChart.getData().add(series);
        
        // ✅ FORCER L'AFFICHAGE DES LABELS
        occupationChart.setAnimated(false);
        occupationChart.setLegendVisible(false);
        occupationChart.setCategoryGap(15);
        occupationChart.setBarGap(5);
        
        // Ajuster l'axe Y
        if (occupationYAxis != null) {
            occupationYAxis.setAutoRanging(false);
            occupationYAxis.setLowerBound(0);
            occupationYAxis.setUpperBound(Math.max(maxValeur + 2, 8));
            occupationYAxis.setTickUnit(1);
            occupationYAxis.setLabel("Nombre de cours");
        }
        
        if (occupationXAxis != null) {
            occupationXAxis.setLabel("Créneaux horaires");
        }

        // Appliquer les couleurs après chargement
        javafx.application.Platform.runLater(() -> {
            for (XYChart.Series<String, Number> s : occupationChart.getData()) {
                for (XYChart.Data<String, Number> d : s.getData()) {
                    javafx.scene.Node bar = d.getNode();
                    if (bar != null) {
                        bar.setStyle("-fx-bar-fill: #4361EE;");
                    }
                }
            }
            occupationChart.setStyle("-fx-background-color: white;");
        });
    }

    public void rafraichirDashboard() {
        chargerStatistiques();
        chargerDemandesEnAttente();
    }

    
    
    @SuppressWarnings("unchecked")
    private void mettreAJourStatistiques(Map<String, Object> stats) {
        Map<String, Integer> utilisateursParRole = (Map<String, Integer>) stats.get("utilisateursParRole");
        
        int total = toInt(stats.get("totalUtilisateurs"));
        int enseignants = utilisateursParRole != null ? utilisateursParRole.getOrDefault("enseignant", 0) : 0;
        int etudiants = utilisateursParRole != null ? utilisateursParRole.getOrDefault("etudiant", 0) : 0;
        int gestionnaires = utilisateursParRole != null ? utilisateursParRole.getOrDefault("gestionnaire", 0) : 0;
        int admins = utilisateursParRole != null ? utilisateursParRole.getOrDefault("admin", 0) : 0;
        
        int totalSalles = toInt(stats.get("totalSalles"));
        int totalCours = toInt(stats.get("totalCours"));
        
        totalUtilisateursLabel.setText(String.valueOf(total));
        totalEnseignantsLabel.setText(String.valueOf(enseignants));
        totalEtudiantsLabel.setText(String.valueOf(etudiants));
        totalSallesLabel.setText(String.valueOf(totalSalles));
        totalCoursLabel.setText(String.valueOf(totalCours));
        
        if (utilisateursParRole != null) {
            mettreAJourPieChart(utilisateursParRole);
        }
    }
    
    private void chargerDemandesEnAttente() {
        System.out.println("🔍 chargerDemandesEnAttente() appelée");  // ← AJOUTER
        
        if (demandesTable == null) {
            System.out.println("❌ demandesTable est null !");
            return;
        }
        
        Task<List<DemandeInscription>> task = new Task<>() {
            @Override
            protected List<DemandeInscription> call() throws SQLException {
                System.out.println("🔄 Exécution de la requête SQL...");  // ← AJOUTER
                List<DemandeInscription> demandes = demandeDAO.listerEnAttente();
                System.out.println("📋 " + demandes.size() + " demande(s) trouvée(s)");  // ← AJOUTER
                return demandes;
            }
            
            @Override
            protected void succeeded() {
                System.out.println("✅ Chargement réussi");  // ← AJOUTER
                List<DemandeInscription> demandes = getValue();
                demandesTable.setItems(FXCollections.observableArrayList(demandes));
                if (demandesEnAttenteLabel != null) {
                    demandesEnAttenteLabel.setText(String.valueOf(demandes.size()));
                }
                mettreAJourBadgeAvecValeur(demandes.size());
            }
            
            @Override
            protected void failed() {
                System.err.println("❌ Erreur chargement demandes: " + getException().getMessage());  // ← AJOUTER
            }
        };
        
        new Thread(task).start();
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
    
    // CORRECTION 2 : Rafraîchir les notifications (inscriptions + réinitialisations)
    private void rafraichirNotifications() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws SQLException {
                List<DemandeInscription> inscriptions = demandeDAO.listerEnAttente();
                int nbInscriptions = inscriptions.size();
                
                
                
                return nbInscriptions ;
            }
            
            @Override
            protected void succeeded() {
                totalNotifications = getValue();
                javafx.application.Platform.runLater(() -> mettreAJourBadge());
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement notifications", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private void mettreAJourBadge() {
        mettreAJourBadgeAvecValeur(totalNotifications);
    }
    
    private void mettreAJourBadgeAvecValeur(int valeur) {
        if (notificationBadge != null) {
            if (valeur > 0) {
                notificationBadge.setText(String.valueOf(valeur));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        }
    }
    
    /**
     * Ouvre la fenêtre de notifications.
     */
    @FXML
    private void ouvrirNotifications() {
        if (demandeDAO != null) {
            try {
                List<DemandeInscription> inscriptions = demandeDAO.listerEnAttente();
                if (!inscriptions.isEmpty()) {
                    if (tabPane != null) {
                        for (int i = 0; i < tabPane.getTabs().size(); i++) {
                            if ("Utilisateurs".equals(tabPane.getTabs().get(i).getText())) {
                                tabPane.getSelectionModel().select(i);
                                break;
                            }
                        }
                    }
                    handleDemandes();
                    return;
                }
            } catch (SQLException e) {
                logger.error("Erreur", e);
            }
        }
        
        // Vérifier les signalements en attente (nouveau)
        try {
            SignalementService signalementService = new SignalementService();
            int signalementsNonTraites = signalementService.compterNonTraites();
            if (signalementsNonTraites > 0) {
                handleSignalements();
                return;
            }
        } catch (SQLException e) {
            logger.error("Erreur chargement signalements", e);
        }
        
        // Par défaut, aller aux demandes d'inscription
        handleDemandes();
    }
    
    public void selectionnerOnglet(int index) {
        if (tabPane != null && index >= 0 && index < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(index);
        }
    }
    
    private String traduireRoleDemande(String role) {
        switch (role) {
            case "etudiant": return "Étudiant";
            case "enseignant": return "Enseignant";
            case "gestionnaire": return "Gestionnaire";
            default: return role;
        }
    }
    
    /**
     * Valide une demande d'inscription.
     * @param demande la demande à valider
     */
    private void validerDemande(DemandeInscription demande) {
        if (!"en_attente".equals(demande.getStatut())) {
            afficherNotification("Info", "Cette demande n'est plus en attente");
            return;
        }
        
        String motDePasse = genererMotDePasse();
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Validation");
        alert.setHeaderText("Valider l'inscription ?");
        alert.setContentText("Mot de passe généré: " + motDePasse + "\nUn email sera envoyé à " + demande.getEmail());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        AuthentificationService authService = new AuthentificationService();
                        authService.validerInscription(demande.getId(), motDePasse);
                        
                        EmailService emailService = new EmailService();
                        emailService.envoyerValidationInscription(
                            demande.getEmail(), demande.getNom(), demande.getPrenom(), motDePasse
                        );
                        
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherNotification("Succès", "Inscription validée");
                        // CORRECTION : Recharger les demandes, les utilisateurs et les notifications
                        chargerDemandesEnAttente();
                        chargerStatistiques();
                        rafraichirNotifications();
                        // Recharger aussi la liste des utilisateurs (si la fenêtre est ouverte)
                        if (tabPane != null) {
                            for (int i = 0; i < tabPane.getTabs().size(); i++) {
                                Tab tab = tabPane.getTabs().get(i);
                                if ("Utilisateurs".equals(tab.getText())) {
                                    // Déclencher le rafraîchissement de la gestion des utilisateurs
                                    break;
                                }
                            }
                        }
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("Erreur lors de la validation");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Refuse une demande d'inscription.
     * @param demande la demande à refuser
     */
    private void refuserDemande(DemandeInscription demande) {
        if (!"en_attente".equals(demande.getStatut())) {
            afficherNotification("Info", "Cette demande n'est plus en attente");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Refus");
        alert.setHeaderText("Refuser l'inscription ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        AuthentificationService authService = new AuthentificationService();
                        authService.refuserInscription(demande.getId());
                        
                        EmailService emailService = new EmailService();
                        emailService.envoyerEmailRefus(
                            demande.getEmail(), demande.getNom(), demande.getPrenom(), 
                            "Demande refusée par l'administrateur"
                        );
                        
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherNotification("Succès", "Demande refusée");
                        chargerDemandesEnAttente();
                        rafraichirNotifications();
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("Erreur lors du refus");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Affiche les détails d'une demande d'inscription.
     * @param demande la demande à afficher
     */
    private void afficherDetailsDemande(DemandeInscription demande) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la demande");
        alert.setHeaderText(demande.getPrenom() + " " + demande.getNom());
        
        StringBuilder sb = new StringBuilder();
        sb.append("Email: ").append(demande.getEmail()).append("\n");
        sb.append("Rôle: ").append(traduireRoleDemande(demande.getRoleDemande())).append("\n");
        sb.append("Date de demande: ").append(demande.getDateDemande().toLocalDate()).append("\n");
        sb.append("Statut: ").append(demande.getStatut()).append("\n\n");
        
        if (demande.getRoleDemande().equals("etudiant")) {
            sb.append("Numéro étudiant: ").append(demande.getNumeroEtudiant()).append("\n");
            if (demande.getUfrId() != null) {
                try {
                    Ufr ufr = ufrService.trouverParId(demande.getUfrId());
                    if (ufr != null) sb.append("UFR: ").append(ufr.getNom()).append("\n");
                } catch (SQLException e) { /* ignore */ }
            }
            if (demande.getClasseId() != null) {
                try {
                    Classe classe = classeService.trouverParId(demande.getClasseId());
                    if (classe != null) sb.append("Classe: ").append(classe.getIntitule()).append("\n");
                } catch (SQLException e) { /* ignore */ }
            }
        } else if (demande.getRoleDemande().equals("enseignant")) {
            sb.append("Matricule: ").append(demande.getMatriculeEnseignant()).append("\n");
        }
        
        alert.setContentText(sb.toString());
        alert.show();
    }
    
    /**
     * Génère un mot de passe aléatoire.
     * @return mot de passe de 8 caractères
     */
    private String genererMotDePasse() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    
    @FXML
    private void handleGestionUfr() { naviguerVers("/fxml/GestionUfr.fxml"); }
    
    @FXML
    private void handleGestionBatiments() { naviguerVers("/fxml/GestionBatiments.fxml"); }
    
    @FXML
    private void handleGestionSalles() { naviguerVers("/fxml/GestionSalles.fxml"); }
    
    @FXML
    private void handleGestionEquipements() { naviguerVers("/fxml/GestionEquipements.fxml"); }
    
    @FXML
    private void handleGestionClasses() { naviguerVers("/fxml/GestionClasses.fxml"); }
    
    @FXML
    private void handleGestionUtilisateurs() { naviguerVers("/fxml/GestionUtilisateurs.fxml"); }
    
    @FXML
    private void handleListeEmploisTemps() { naviguerVers("/fxml/ListeEmploisTemps.fxml"); }
    
    @FXML
    private void handleHistoriqueReservations() { naviguerVers("/fxml/HistoriqueReservation.fxml"); }
    
    @FXML
    private void handlePlanning() { naviguerVers("/fxml/Planning.fxml"); }
    
    @FXML
    private void handleReservations() { naviguerVers("/fxml/Reservation.fxml"); }
    
    @FXML
    private void handleCarte() {
        try {
            System.out.println("🔍 Navigation vers Carte depuis Admin");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Carte.fxml"));
            Parent root = loader.load();
            
            CarteControleur controleur = loader.getController();
            // IMPORTANT : passer le stage actuel
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            
            Scene scene = new Scene(root);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            
            primaryStage.setScene(scene);
            primaryStage.setTitle("SCHEDULER - Carte interactive");
            
        } catch (IOException e) {
            logger.error("Erreur navigation carte", e);
        }
    }
    
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (tabPane != null && tabPane.getScene() != null && tabPane.getScene().getWindow() instanceof Stage) {
            return (Stage) tabPane.getScene().getWindow();
        }
        if (demandesTable != null && demandesTable.getScene() != null && demandesTable.getScene().getWindow() instanceof Stage) {
            return (Stage) demandesTable.getScene().getWindow();
        }
        return null;
    }
    
    @Override
    protected void retourLogin() {
        Stage stage = getStageFromScene();
        if (stage == null) {
            logger.error("❌ Impossible de récupérer le stage");
            return;
        }
        super.retourLogin();
    }
    
 // MÉTHODES SIDEBAR (À AJOUTER)

    private void configurerSidebar() {
        sousMenuUtilisateurs.setVisible(sousMenuUtilisateursVisible);
        sousMenuUtilisateurs.setManaged(sousMenuUtilisateursVisible);
        sousMenuStructure.setVisible(sousMenuStructureVisible);
        sousMenuStructure.setManaged(sousMenuStructureVisible);
        
        sidebarUtilisateursBtn.setText(sousMenuUtilisateursVisible ? "👥 Utilisateurs ▼" : "👥 Utilisateurs ▶");
        sidebarStructureBtn.setText(sousMenuStructureVisible ? "🏛️ Structure ▼" : "🏛️ Structure ▶");
        
        // Dashboard par défaut
        sidebarDashboardBtn.setOnAction(e -> afficherDashboard());
        
        // Toggle sous-menus
        sidebarUtilisateursBtn.setOnAction(e -> {
            sousMenuUtilisateursVisible = !sousMenuUtilisateursVisible;
            sousMenuUtilisateurs.setVisible(sousMenuUtilisateursVisible);
            sousMenuUtilisateurs.setManaged(sousMenuUtilisateursVisible);
            sidebarUtilisateursBtn.setText(sousMenuUtilisateursVisible ? "👥 Utilisateurs ▼" : "👥 Utilisateurs ▶");
        });
        
        sidebarStructureBtn.setOnAction(e -> {
            sousMenuStructureVisible = !sousMenuStructureVisible;
            sousMenuStructure.setVisible(sousMenuStructureVisible);
            sousMenuStructure.setManaged(sousMenuStructureVisible);
            sidebarStructureBtn.setText(sousMenuStructureVisible ? "🏛️ Structure ▼" : "🏛️ Structure ▶");
        });
        
        // Navigation
        sidebarComptesBtn.setOnAction(e -> naviguerVers("/fxml/GestionUtilisateurs.fxml"));
        sidebarDemandesBtn.setOnAction(e -> naviguerVers("/fxml/DemandesInscription.fxml"));
        sidebarSignalementsBtn.setOnAction(e -> naviguerVers("/fxml/Signalement.fxml"));
        sidebarUfrBtn.setOnAction(e -> naviguerVers("/fxml/GestionUfr.fxml"));
        sidebarBatimentsBtn.setOnAction(e -> naviguerVers("/fxml/GestionBatiments.fxml"));
        sidebarSallesBtn.setOnAction(e -> naviguerVers("/fxml/GestionSalles.fxml"));
        sidebarEquipementsBtn.setOnAction(e -> naviguerVers("/fxml/GestionEquipements.fxml"));
        sidebarClassesBtn.setOnAction(e -> naviguerVers("/fxml/GestionClasses.fxml"));
        sidebarReservationsBtn.setOnAction(e -> naviguerVers("/fxml/Reservation.fxml"));
        if (sidebarCoursReportesBtn != null) sidebarCoursReportesBtn.setOnAction(e -> ouvrirModalCoursReportes());
        sidebarHistoriqueBtn.setOnAction(e -> naviguerVers("/fxml/HistoriqueReservation.fxml"));
        sidebarCarteBtn.setOnAction(e -> naviguerVers("/fxml/Carte.fxml"));
        sidebarRapportsBtn.setOnAction(e -> naviguerVers("/fxml/Rapports.fxml"));
    }

    private void afficherDashboard() {
        contenuPrincipal.getChildren().clear();
        
        VBox dashboardContent = new VBox(20);
        dashboardContent.setStyle("-fx-padding:20;-fx-background-color:#FBF8F3;");
        
        // Titre
        HBox titreBox = new HBox(10);
        titreBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label titre = new Label("Vue d'ensemble");
        titre.setStyle("-fx-font-size:20px;-fx-font-weight:800;-fx-text-fill:#3D261A;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titreBox.getChildren().addAll(titre, spacer);
        dashboardContent.getChildren().add(titreBox);
        
        // Cartes stats
        GridPane cartesGrid = new GridPane();
        cartesGrid.setHgap(14);
        cartesGrid.setVgap(14);
        for (int i = 0; i < 6; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            cartesGrid.getColumnConstraints().add(col);
        }
        
        VBox carteTotal = creerCarteStat("👥 UTILISATEURS", totalUtilisateursLabel, "#6B4226");
        VBox carteEnseignants = creerCarteStat("👨‍🏫 ENSEIGNANTS", totalEnseignantsLabel, "#9E5D32");
        VBox carteEtudiants = creerCarteStat("🎓 ÉTUDIANTS", totalEtudiantsLabel, "#D39A43");
        VBox carteSalles = creerCarteStat("🏛️ SALLES", totalSallesLabel, "#2D6A4F");
        VBox carteCours = creerCarteStat("📚 COURS", totalCoursLabel, "#C89658");
        
        cartesGrid.add(carteTotal, 0, 0);
        cartesGrid.add(carteEnseignants, 1, 0);
        cartesGrid.add(carteEtudiants, 2, 0);
        cartesGrid.add(carteSalles, 3, 0);
        cartesGrid.add(carteCours, 4, 0);
        dashboardContent.getChildren().add(cartesGrid);
        
        // Graphiques
        HBox graphiquesBox = new HBox(16);
        graphiquesBox.setAlignment(javafx.geometry.Pos.CENTER);
        graphiquesBox.setStyle("-fx-background-color:white;-fx-padding:20;-fx-background-radius:12;-fx-border-color:#E6DCCD;-fx-border-width:1;-fx-border-radius:12;");
        
        // ✅ BarChart - OCCUPATION DES SALLES
        VBox barChartBox = new VBox(8);
        barChartBox.setAlignment(javafx.geometry.Pos.CENTER);
        Label barTitle = new Label("📊 OCCUPATION DES SALLES PAR CRÉNEAU");
        barTitle.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:#3D261A;");
        barChartBox.getChildren().add(barTitle);
        
        // Recréer le BarChart s'il est null
        if (occupationChart == null) {
            occupationChart = new BarChart<>(occupationXAxis, occupationYAxis);
            occupationChart.setPrefHeight(320);
            occupationChart.setPrefWidth(580);
            occupationChart.setAnimated(false);
        }
        barChartBox.getChildren().add(occupationChart);
        
        // PieChart
        VBox pieChartBox = new VBox(8);
        pieChartBox.setAlignment(javafx.geometry.Pos.CENTER);
        Label pieTitle = new Label("🥧 RÉPARTITION DES UTILISATEURS");
        pieTitle.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:#3D261A;");
        pieChartBox.getChildren().add(pieTitle);
        if (repartitionUtilisateursChart == null) {
            repartitionUtilisateursChart = new PieChart();
            repartitionUtilisateursChart.setPrefHeight(320);
            repartitionUtilisateursChart.setPrefWidth(460);
        }
        pieChartBox.getChildren().add(repartitionUtilisateursChart);
        
        graphiquesBox.getChildren().addAll(barChartBox, pieChartBox);
        dashboardContent.getChildren().add(graphiquesBox);
        
        // Tableau des demandes
        if (demandesTable != null) {
            VBox demandesBox = new VBox(10);
            demandesBox.setStyle("-fx-background-color:white;-fx-padding:20;-fx-background-radius:12;-fx-border-color:#E6DCCD;-fx-border-width:1;-fx-border-radius:12;");
            
            HBox demandesHeader = new HBox(10);
            demandesHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label demandesTitle = new Label("📝 DEMANDES D'INSCRIPTION EN ATTENTE");
            demandesTitle.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:#3D261A;");
            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);
            Button gererBtn = new Button("Gérer toutes les demandes");
            gererBtn.setStyle("-fx-background-color:#6B4226;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:7 16;-fx-font-size:12px;-fx-font-weight:700;-fx-cursor:hand;");
            gererBtn.setOnAction(e -> handleDemandes());
            demandesHeader.getChildren().addAll(demandesTitle, spacer2, gererBtn);
            
            demandesBox.getChildren().addAll(demandesHeader, demandesTable);
            dashboardContent.getChildren().add(demandesBox);
        }
        
        contenuPrincipal.getChildren().add(dashboardContent);
                chargerStatistiques();
        chargerDemandesEnAttente();
    }

    private VBox creerCarteStat(String label, Label valeurLabel, String couleur) {
        VBox carte = new VBox(8);
        carte.setAlignment(javafx.geometry.Pos.CENTER);
        carte.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-padding:20;" +
                       "-fx-border-color:transparent transparent transparent " + couleur + ";" +
                       "-fx-border-width:0 0 0 5;-fx-border-radius:0 12 12 0;");
        Label titre = new Label(label);
        titre.setStyle("-fx-font-size:11px;-fx-text-fill:#7A665A;-fx-font-weight:700;");
        valeurLabel.setStyle("-fx-font-size:36px;-fx-font-weight:900;-fx-text-fill:" + couleur + ";");
        carte.getChildren().addAll(titre, valeurLabel);
        return carte;
    }
    
    
}