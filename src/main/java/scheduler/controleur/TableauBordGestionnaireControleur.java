package scheduler.controleur;

import scheduler.modele.*; 
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.sql.SQLException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.scene.control.ButtonBar;
import javafx.application.Platform;

/**
 * Tableau de bord du gestionnaire pour la planification.
 */
public class TableauBordGestionnaireControleur extends TableauBordControleur {
    
    // COMPOSANTS FXML

    @FXML private GestionMatieresControleur gestionMatieresControleur;
    @FXML private GestionCoursControleur gestionCoursControleur;
    private NouvelEdtControleur nouvelEdtControleur = null;
    @FXML private ListeEmploisTempsControleur listeEdtPlanningControleur;
    @FXML private Tab ongletRecherche;

      
    private Label classeSelectionneeLabel = null;
    private ComboBox<String> semaineCombo = null;
    private ComboBox<String> moisCombo = null;
    private ComboBox<Integer> anneeCombo = null;
    private ComboBox<Integer> anneeMoisCombo = null;
    private DatePicker dateDebutPicker = null;
    private DatePicker dateFinPicker = null;
    private Label periodeLabel = null;
    private HBox hebdoControls = null;
    private HBox mensuelControls = null;
    private HBox semestrielControls = null;
    private ListView<Cours> coursDisponiblesList = null;
    private Label totalCoursLabel = null;
    private TextField rechercheCoursField = null;
    private ComboBox<String> filtreTypeCoursCombo = null;
    private GridPane grilleEDT = null;
    private VBox legendeBox = null;
    private Button creerEDTButton = null;
    private Button validerEDTButton = null;
    private Button exporterPDFButton = null;
    private Button exporterExcelButton = null;
    private Button verifierConflitsButton = null;
    private Label conflitsLabel = null;
    
    @FXML private ProgressIndicator chargementIndicator;
    
 // COMPOSANTS SIDEBAR (À AJOUTER)
    @FXML private Button sidebarPlanningBtn;
    @FXML private Button sidebarCreerEdtBtn;
    @FXML private Button sidebarListeEdtBtn;
    @FXML private Button sidebarGestionBtn;
    @FXML private Button sidebarMatieresBtn;
    @FXML private Button sidebarCoursBtn;
    @FXML private Button sidebarReservationsBtn;
    @FXML private Button sidebarCoursReportesBtn;
    @FXML private Button sidebarHistoriqueBtn;
    @FXML private Button sidebarRechercheBtn;
    @FXML private Button sidebarConflitsBtn;
    @FXML private Button sidebarCarteBtn;

    @FXML private VBox sousMenuPlanning;
    @FXML private VBox sousMenuGestion;
    @FXML private StackPane contenuPrincipal;
    @FXML private TabPane tabPane;

    // ÉTAT SIDEBAR
    private boolean sousMenuPlanningVisible = true;
    private boolean sousMenuGestionVisible = true;
    
    // NOTIFICATIONS AMÉLIORÉES
    @FXML private Label notificationBadge;
    @FXML private Button notificationButton;
    private int totalNotifications = 0;
    private List<Map<String, Object>> conflitsEnCours = new ArrayList<>();
    private Timeline notificationTimeline;
    
    // SERVICES
    private ClasseService classeService;
    private CoursService coursService;
    private PlanningService planningService;
    private AttributionService attributionService;
    private ConflitService conflitService;
    private ReservationService reservationService;
    private ExportService exportService;
    private SalleService salleService;
    private MatiereService matiereService;
    private UtilisateurService utilisateurService;
    private BatimentService batimentService;
    private EmailService emailService;
    
    
    // DONNÉES
    private List<Creneau> creneauxTemp;
    private EmploiDuTemps edtEnCours;
    private List<Cours> tousCoursDisponibles;
    private Map<Integer, Matiere> cacheMatieres;
    private Map<Integer, String> cacheEnseignants;
    private Map<Integer, Salle> cacheSalles;
    private Map<Integer, Batiment> cacheBatiments;
    
    // Couleurs par type de cours
    private static final Color COULEUR_CM = Color.rgb(76, 175, 80, 0.25);
    private static final Color COULEUR_TD = Color.rgb(33, 150, 243, 0.25);
    private static final Color COULEUR_TP = Color.rgb(255, 152, 0, 0.25);
    
    // Champs anciens
    private ComboBox<Classe> classeComboBox = null;
    private ToggleGroup periodeTypeGroup = null;
    private RadioButton hebdoRadio = null;
    private RadioButton mensuelRadio = null;
    private RadioButton semestrielRadio = null;
    private ComboBox<String> periodeComboBox = null;
    
    @Override
    protected void rafraichirDonnees() {
        if (classeComboBox != null && classeComboBox.getValue() != null) {
            chargerCoursDisponibles(classeComboBox.getValue().getId());
        }
        if (creneauxTemp != null && !creneauxTemp.isEmpty()) {
            mettreAJourGrille();
        }
        verifierConflitsEtNotifications();
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.classeService = new ClasseService();
        this.coursService = new CoursService();
        this.planningService = new PlanningService();
        this.attributionService = new AttributionService();
        this.conflitService = new ConflitService();
        this.reservationService = new ReservationService();
        this.exportService = new ExportService();
        this.salleService = new SalleService();
        this.matiereService = new MatiereService();
        this.utilisateurService = new UtilisateurService();
        this.batimentService = new BatimentService();
        this.emailService = new EmailService();
        
        this.creneauxTemp = new ArrayList<>();
        this.cacheMatieres = new HashMap<>();
        this.cacheEnseignants = new HashMap<>();
        this.cacheSalles = new HashMap<>();
        this.cacheBatiments = new HashMap<>();
        this.conflitsEnCours = new ArrayList<>();
        
        chargerTousEnseignants();
        chargerTousBatiments();
        creerLegende();
        initialiserNotifications();
        configurerSidebar();
    }

    private void initialiserControleursFils() {
        if (gestionMatieresControleur != null && utilisateurConnecte != null) {
            gestionMatieresControleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
        }
        if (gestionCoursControleur != null && utilisateurConnecte != null) {
            gestionCoursControleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
        }
        if (listeEdtPlanningControleur != null && utilisateurConnecte != null) {
            listeEdtPlanningControleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
        }
    }
    
    @Override
    protected void initialiserTableauBord() {
        configurerPeriodes();
        configurerComposants();
        configurerFiltresCours();
        chargerClasses();
        chargerAnnees();
        chargerMois();
        configurerGrilleEDT();
        initialiserControleursFils();
        initialiserNotifications();
        
        if (ongletRecherche != null && ongletRecherche.getContent() != null) {
            Object controller = ongletRecherche.getContent().getProperties().get("fx:controller");
            if (controller instanceof RechercheControleur) {
                ((RechercheControleur) controller).setUtilisateurEtStage(utilisateurConnecte, primaryStage);
                System.out.println("✅ Utilisateur transmis à RechercheControleur");
            }
        }
        naviguerVers("/fxml/ListeEmploisTemps.fxml");
    }
    
    
 // MÉTHODES DE SÉLECTION D'ONGLET

    public void selectionnerOngletRecherche() {
        if (ongletRecherche != null) {
            // Onglet Recherche dans le TabPane principal
            if (tabPane != null) {
                for (Tab tab : tabPane.getTabs()) {
                    if ("Recherche".equals(tab.getText())) {
                        tabPane.getSelectionModel().select(tab);
                        break;
                    }
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
    
    
    
    // NOTIFICATIONS ET CONFLITS AMÉLIORÉS
    
    private void initialiserNotifications() {
        if (notificationButton != null) {
            notificationButton.setOnAction(e -> afficherNotificationsConflits());
            notificationButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        }
        
        if (notificationBadge != null) {
            notificationBadge.setVisible(false);
            notificationBadge.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; " +
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-min-width: 18; -fx-min-height: 18; " +
                "-fx-background-radius: 9; -fx-alignment: center;");
        }
        
        demarrerVerificationConflits();
    }
    
    private void demarrerVerificationConflits() {
        notificationTimeline = new Timeline(
            new KeyFrame(Duration.seconds(300), e -> verifierConflitsEtNotifications())
        );
        notificationTimeline.setCycleCount(Timeline.INDEFINITE);
        notificationTimeline.play();
        
        new Thread(() -> {
            try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
            verifierConflitsEtNotifications();
        }).start();
    }
    
    /**
     * Vérifie les conflits et met à jour les notifications.
     */
    private void verifierConflitsEtNotifications() {
        if (utilisateurConnecte == null) return;
        
        new Thread(() -> {
            try {
                List<Map<String, Object>> nouveauxConflits = new ArrayList<>();
                
                // 1. Vérifier les conflits dans les EDT actifs
                List<EmploiDuTemps> edts = planningService.listerActifs();
                for (EmploiDuTemps edt : edts) {
                    List<Creneau> creneaux = planningService.getCreneauxParEmploiDuTemps(edt.getId());
                    for (Creneau c : creneaux) {
                        List<ConflitService.ResultatConflit> resultats = conflitService.verifierConflitsAvecSolutions(c);
                        for (ConflitService.ResultatConflit r : resultats) {
                            Map<String, Object> conflit = new HashMap<>();
                            conflit.put("type", r.getTypeConflit());
                            conflit.put("description", r.getDescription());
                            conflit.put("creneau", c);
                            conflit.put("solutions", getSolutionsFromResultat(r));
                            conflit.put("edtId", edt.getId());
                            conflit.put("classeId", edt.getClasseId());
                            nouveauxConflits.add(conflit);
                        }
                    }
                }
                
                // 2. Vérifier les réservations en conflit
                List<Reservation> reservations = reservationService.listerTous();
                for (Reservation r : reservations) {
                    if ("confirmee".equals(r.getStatut()) || "en_cours".equals(r.getStatut())) {
                        boolean salleLibre = reservationService.verifierDisponibilite(
                            r.getSalleId(), r.getDateReservation(), 
                            r.getHeureDebut(), r.getHeureFin());
                        boolean pasDeCours = planningService.salleEstDisponible(
                            r.getSalleId(), r.getDateReservation(),
                            r.getHeureDebut(), r.getHeureFin());
                        
                        if (!salleLibre || !pasDeCours) {
                            Map<String, Object> conflit = new HashMap<>();
                            conflit.put("type", "RESERVATION");
                            conflit.put("description", "Réservation en conflit - Salle #" + r.getSalleId() + 
                                " le " + r.getDateReservation() + " de " + r.getHeureDebut() + " à " + r.getHeureFin());
                            conflit.put("reservation", r);
                            conflit.put("solutions", getSolutionsPourReservation(r));
                            nouveauxConflits.add(conflit);
                        }
                    }
                }
                
                final int nbConflits = nouveauxConflits.size();
                
                Platform.runLater(() -> {
                    conflitsEnCours = nouveauxConflits;
                    totalNotifications = nbConflits;
                    if (totalNotifications > 0) {
                        notificationBadge.setText(String.valueOf(totalNotifications));
                        notificationBadge.setVisible(true);
                    } else {
                        notificationBadge.setVisible(false);
                    }
                });
                
            } catch (SQLException e) {
                logger.error("Erreur vérification conflits", e);
            }
        }).start();
    }
    
    private List<Map<String, Object>> getSolutionsFromResultat(ConflitService.ResultatConflit resultat) {
        List<Map<String, Object>> solutions = new ArrayList<>();
        for (ConflitService.SolutionConflit sol : resultat.getSolutions()) {
            Map<String, Object> solution = new HashMap<>();
            solution.put("type", sol.getType());
            solution.put("description", sol.getDescription());
            solution.put("valeur", sol.getValeur());
            solutions.add(solution);
        }
        return solutions;
    }
    

    /**
     * Navigue vers la gestion des conflits.
     */
    @FXML
    private void handleGestionConflits() {
        naviguerVers("/fxml/GestionConflits.fxml");
    }

    private void initialiserNotificationsConflits() {
        Timeline conflitTimeline = new Timeline(
            new KeyFrame(Duration.seconds(30), e -> {
                if (utilisateurConnecte != null && "gestionnaire".equals(utilisateurConnecte.getRole())) {
                    verifierNouveauxConflits();
                }
            })
        );
        conflitTimeline.setCycleCount(Timeline.INDEFINITE);
        conflitTimeline.play();
    }

    private void verifierNouveauxConflits() {
        new Thread(() -> {
            try {
                ConflitService conflitService = new ConflitService();
                PlanningService planningService = new PlanningService();
                
                List<EmploiDuTemps> edtsActifs = planningService.listerActifs();
                int nbConflits = 0;
                
                for (EmploiDuTemps edt : edtsActifs) {
                    List<Creneau> creneaux = planningService.getCreneauxParEmploiDuTemps(edt.getId());
                    for (Creneau c : creneaux) {
                        nbConflits += conflitService.verifierConflits(c).size();
                    }
                }
                
                List<Reservation> reservations = reservationService.listerTous();
                for (Reservation r : reservations) {
                    boolean salleLibre = reservationService.verifierDisponibilite(
                        r.getSalleId(), r.getDateReservation(),
                        r.getHeureDebut(), r.getHeureFin()
                    );
                    if (!salleLibre) nbConflits++;
                }
                
                final int nombreFinal = nbConflits;
                javafx.application.Platform.runLater(() -> {
                    if (notificationBadge != null) {
                        if (nombreFinal > 0) {
                            notificationBadge.setText(String.valueOf(nombreFinal));
                            notificationBadge.setVisible(true);
                        } else {
                            notificationBadge.setVisible(false);
                        }
                    }
                });
                
            } catch (SQLException e) {
                logger.error("Erreur vérification conflits", e);
            }
        }).start();
    }

    
    private List<Map<String, Object>> getSolutionsPourReservation(Reservation r) {
        List<Map<String, Object>> solutions = new ArrayList<>();
        
        Map<String, Object> sol1 = new HashMap<>();
        sol1.put("type", "CHANGER_SALLE");
        sol1.put("description", "🏫 Changer de salle pour une autre disponible");
        solutions.add(sol1);
        
        Map<String, Object> sol2 = new HashMap<>();
        sol2.put("type", "CHANGER_HORAIRE");
        sol2.put("description", "⏰ Décaler l'horaire d'une heure");
        solutions.add(sol2);
        
        Map<String, Object> sol3 = new HashMap<>();
        sol3.put("type", "ANNULER");
        sol3.put("description", "❌ Annuler la réservation");
        solutions.add(sol3);
        
        return solutions;
    }
    
    /**
     * Affiche les notifications de conflits.
     */
    private void afficherNotificationsConflits() {
        if (conflitsEnCours.isEmpty()) {
            afficherNotification("Notifications", "✅ Aucun conflit détecté");
            return;
        }
        
        Dialog<Void> conflitDialog = new Dialog<>();
        conflitDialog.setTitle("Gestion des conflits");
        conflitDialog.setHeaderText(totalNotifications + " conflit(s) détecté(s)");
        // Taille fixe pour éviter la case blanche
        conflitDialog.getDialogPane().setMinWidth(700);
        conflitDialog.getDialogPane().setMinHeight(580);
        conflitDialog.getDialogPane().setPrefWidth(750);
        conflitDialog.getDialogPane().setPrefHeight(620);
        
        VBox content = new VBox(12);
        content.setPadding(new Insets(15));
        
        Label infoLabel = new Label("Cliquez sur un conflit pour voir ses solutions, puis appliquez-en une :");
        infoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-font-weight: 600;");
        
        // Liste des conflits
        ListView<Map<String, Object>> conflitsListView = new ListView<>();
        conflitsListView.setPrefHeight(200);
        conflitsListView.setMinHeight(160);
        
        ObservableList<Map<String, Object>> conflitsObservable = FXCollections.observableArrayList(conflitsEnCours);
        conflitsListView.setItems(conflitsObservable);
        
        conflitsListView.setCellFactory(lv -> new ListCell<Map<String, Object>>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String type = (String) item.get("type");
                    String desc = (String) item.get("description");
                    setText("⚠️ [" + type + "] " + desc);
                    setStyle("-fx-font-size: 12px; -fx-padding: 6 8;");
                }
            }
        });
        
        // Panneau de détails contextuels (toujours visible)
        VBox detailsPane = new VBox(10);
        detailsPane.setStyle("-fx-background-color: #F0F4F8; -fx-padding: 12; -fx-background-radius: 8;" +
            "-fx-border-color: #CBD5E0; -fx-border-width: 1; -fx-border-radius: 8;");
        detailsPane.setVisible(true);
        detailsPane.setManaged(true);
        detailsPane.setMinHeight(200);
        
        Label placeholderLabel = new Label("👆 Sélectionnez un conflit dans la liste ci-dessus");
        placeholderLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 13px; -fx-font-style: italic;");
        
        Label conflitDetailLabel = new Label();
        conflitDetailLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565C0; -fx-font-size: 13px;");
        conflitDetailLabel.setWrapText(true);
        conflitDetailLabel.setVisible(false);
        
        Label solutionsLabel = new Label("💡 Solutions proposées :");
        solutionsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-font-size: 12px;");
        solutionsLabel.setVisible(false);
        
        VBox solutionsBox = new VBox(6);
        solutionsBox.setPadding(new Insets(4, 0, 4, 8));
        
        // ToggleGroup partagé entre le listener et le bouton Appliquer
        ToggleGroup[] solutionsGroupHolder = { new ToggleGroup() };
        
        Button appliquerSolutionBtn = new Button("✅ Appliquer la solution sélectionnée");
        appliquerSolutionBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; " +
            "-fx-background-radius: 7; -fx-padding: 9 18; -fx-cursor: hand; -fx-font-weight: bold;");
        appliquerSolutionBtn.setDisable(true);
        appliquerSolutionBtn.setVisible(false);
        
        detailsPane.getChildren().addAll(
            placeholderLabel, conflitDetailLabel, solutionsLabel, solutionsBox, appliquerSolutionBtn
        );
        
        // Listener sélection conflit
        conflitsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                placeholderLabel.setVisible(false);
                placeholderLabel.setManaged(false);
                conflitDetailLabel.setText("📋 " + (String) newVal.get("description"));
                conflitDetailLabel.setVisible(true);
                conflitDetailLabel.setManaged(true);
                
                solutionsBox.getChildren().clear();
                ToggleGroup tg = new ToggleGroup();
                solutionsGroupHolder[0] = tg;
                
                List<Map<String, Object>> solutions = (List<Map<String, Object>>) newVal.get("solutions");
                
                if (solutions != null && !solutions.isEmpty()) {
                    solutionsLabel.setVisible(true);
                    solutionsLabel.setManaged(true);
                    for (Map<String, Object> sol : solutions) {
                        RadioButton rb = new RadioButton((String) sol.get("description"));
                        rb.setToggleGroup(tg);
                        rb.setUserData(sol);
                        rb.setStyle("-fx-font-size: 12px; -fx-padding: 3 0;");
                        solutionsBox.getChildren().add(rb);
                    }
                    appliquerSolutionBtn.setDisable(false);
                    appliquerSolutionBtn.setVisible(true);
                    appliquerSolutionBtn.setManaged(true);
                } else {
                    solutionsLabel.setVisible(false);
                    solutionsLabel.setManaged(false);
                    Label aucune = new Label("ℹ️ Aucune solution automatique disponible pour ce conflit.");
                    aucune.setStyle("-fx-text-fill: #f44336; -fx-font-size: 12px;");
                    solutionsBox.getChildren().add(aucune);
                    appliquerSolutionBtn.setDisable(true);
                    appliquerSolutionBtn.setVisible(false);
                    appliquerSolutionBtn.setManaged(false);
                }
            } else {
                placeholderLabel.setVisible(true);
                placeholderLabel.setManaged(true);
                conflitDetailLabel.setVisible(false);
                conflitDetailLabel.setManaged(false);
                solutionsLabel.setVisible(false);
                solutionsLabel.setManaged(false);
                solutionsBox.getChildren().clear();
                appliquerSolutionBtn.setDisable(true);
                appliquerSolutionBtn.setVisible(false);
                appliquerSolutionBtn.setManaged(false);
            }
        });
        
        // Boutons d'action
        Button toutReglerBtn = new Button("🔧 Tout régler automatiquement");
        toutReglerBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; " +
            "-fx-background-radius: 7; -fx-padding: 9 18; -fx-cursor: hand; -fx-font-weight: bold;");
        toutReglerBtn.setOnAction(e -> reglerTousLesConflits(conflitsObservable, conflitDialog));
        
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; " +
            "-fx-background-radius: 7; -fx-padding: 9 18; -fx-cursor: hand;");
        fermerBtn.setOnAction(e -> conflitDialog.close());
        
        HBox buttonBox = new HBox(10, toutReglerBtn, fermerBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));
        
        content.getChildren().addAll(infoLabel, conflitsListView, detailsPane, buttonBox);
        
        conflitDialog.getDialogPane().setContent(content);
        conflitDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        conflitDialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        
        // Appliquer solution
        appliquerSolutionBtn.setOnAction(e -> {
            Map<String, Object> conflitSelectionne = conflitsListView.getSelectionModel().getSelectedItem();
            if (conflitSelectionne == null) return;
            
            Toggle selectedToggle = solutionsGroupHolder[0].getSelectedToggle();
            if (selectedToggle instanceof RadioButton) {
                Map<String, Object> solution = (Map<String, Object>) ((RadioButton) selectedToggle).getUserData();
                appliquerSolution(conflitSelectionne, solution);
                conflitsObservable.remove(conflitSelectionne);
                conflitsListView.refresh();
                if (conflitsObservable.isEmpty()) {
                    conflitDialog.close();
                    afficherNotification("Succès", "✅ Tous les conflits ont été résolus !");
                } else {
                    conflitsListView.getSelectionModel().clearSelection();
                }
            } else {
                afficherErreur("Veuillez sélectionner une solution dans la liste.");
            }
        });
        
        conflitDialog.showAndWait();
    }
    
    /**
     * Applique une solution à un conflit.
     * @param conflit le conflit à résoudre
     * @param solution la solution à appliquer
     */
    private void appliquerSolution(Map<String, Object> conflit, Map<String, Object> solution) {
        String typeSolution = (String) solution.get("type");
        String description = (String) solution.get("description");
        
        try {
            if ("CHANGER_SALLE".equals(typeSolution)) {
                Object valeur = solution.get("valeur");
                if (valeur instanceof Salle) {
                    Salle nouvelleSalle = (Salle) valeur;
                    Creneau creneau = (Creneau) conflit.get("creneau");
                    if (creneau != null) {
                        creneau.setSalleId(nouvelleSalle.getId());
                        planningService.modifierCreneau(creneau);
                        afficherNotification("Solution appliquée", "Salle changée vers " + nouvelleSalle.getNumero());
                        envoyerEmailResolutionConflit(conflit, description);
                    }
                }
            } else if ("CHANGER_HORAIRE".equals(typeSolution)) {
                Object valeur = solution.get("valeur");
                if (valeur instanceof String[]) {
                    String[] heures = (String[]) valeur;
                    Creneau creneau = (Creneau) conflit.get("creneau");
                    if (creneau != null) {
                        creneau.setHeureDebut(heures[0]);
                        creneau.setHeureFin(heures[1]);
                        planningService.modifierCreneau(creneau);
                        afficherNotification("Solution appliquée", "Horaire changé : " + heures[0] + " - " + heures[1]);
                        envoyerEmailResolutionConflit(conflit, description);
                    }
                }
            } else if ("ANNULER".equals(typeSolution)) {
                Reservation reservation = (Reservation) conflit.get("reservation");
                if (reservation != null) {
                    reservationService.annulerReservation(reservation.getId());
                    afficherNotification("Solution appliquée", "Réservation annulée");
                    envoyerEmailResolutionConflit(conflit, description);
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de l'application de la solution", e);
            afficherErreur("Erreur lors de l'application de la solution");
        }
    }
    
    private void envoyerEmailResolutionConflit(Map<String, Object> conflit, String solution) {
        try {
            if (conflit.containsKey("reservation")) {
                Reservation r = (Reservation) conflit.get("reservation");
                Utilisateur demandeur = utilisateurService.trouverParId(r.getUtilisateurId());
                if (demandeur != null) {
                    emailService.envoyerResolutionConflit(
                        demandeur.getEmail(),
                        demandeur.getPrenom() + " " + demandeur.getNom(),
                        "Réservation",
                        solution,
                        "Salle #" + r.getSalleId(),
                        r.getDateReservation(),
                        r.getHeureDebut(),
                        r.getHeureFin()
                    );
                }
            } else if (conflit.containsKey("creneau")) {
                Creneau c = (Creneau) conflit.get("creneau");
                Cours cours = coursService.trouverParId(c.getCoursId());
                if (cours != null) {
                    Utilisateur enseignant = utilisateurService.trouverParId(cours.getEnseignantId());
                    if (enseignant != null) {
                        emailService.envoyerResolutionConflit(
                            enseignant.getEmail(),
                            enseignant.getPrenom() + " " + enseignant.getNom(),
                            "Cours",
                            solution,
                            "Salle #" + c.getSalleId(),
                            c.getJour(),
                            c.getHeureDebut(),
                            c.getHeureFin()
                        );
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur envoi email résolution conflit", e);
        }
    }
    
    /**
     * Résout tous les conflits automatiquement.
     */
    private void reglerTousLesConflits(ObservableList<Map<String, Object>> conflitsObservable, Dialog<Void> conflitDialog) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Régler tous les conflits");
        confirm.setContentText("Voulez-vous appliquer automatiquement la première solution pour tous les conflits ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    for (Map<String, Object> conflit : conflitsObservable) {
                        List<Map<String, Object>> solutions = (List<Map<String, Object>>) conflit.get("solutions");
                        if (solutions != null && !solutions.isEmpty()) {
                            Map<String, Object> solution = solutions.get(0);
                            appliquerSolution(conflit, solution);
                        }
                    }
                    
                    Platform.runLater(() -> {
                        afficherNotification("Succès", "Tous les conflits ont été traités");
                        conflitsObservable.clear();
                        conflitDialog.close();
                    });
                }).start();
            }
        });
    }
    
    // LÉGENDE
    
    private void creerLegende() {
        if (legendeBox == null) return;
        
        legendeBox.getChildren().clear();
        legendeBox.setSpacing(5);
        legendeBox.setPadding(new Insets(10));
        legendeBox.setStyle("-fx-background-color: #F5F7FA; -fx-background-radius: 5; -fx-border-color: #E0E0E0; -fx-border-width: 1px;");
        
        Label titre = new Label("Légende");
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1976D2;");
        legendeBox.getChildren().add(titre);
        
        legendeBox.getChildren().add(creerItemLegende("CM", COULEUR_CM));
        legendeBox.getChildren().add(creerItemLegende("TD", COULEUR_TD));
        legendeBox.getChildren().add(creerItemLegende("TP", COULEUR_TP));
    }
    
    private HBox creerItemLegende(String texte, Color couleur) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        
        Rectangle rect = new Rectangle(20, 20);
        rect.setFill(couleur);
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(0.5);
        
        Label label = new Label(texte);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #333333;");
        
        item.getChildren().addAll(rect, label);
        return item;
    }
    
    // CHARGEMENT DES DONNÉES DE BASE
    
    private void chargerTousEnseignants() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                List<Utilisateur> enseignants = utilisateurService.listerParRole("enseignant");
                for (Utilisateur u : enseignants) {
                    cacheEnseignants.put(u.getId(), u.getPrenom() + " " + u.getNom());
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    private void chargerTousBatiments() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                List<Batiment> batiments = batimentService.listerTous();
                for (Batiment b : batiments) {
                    cacheBatiments.put(b.getId(), b);
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    // CONFIGURATION
    
    private void configurerPeriodes() {
        if (periodeComboBox != null) {
            periodeComboBox.getItems().addAll("Hebdomadaire", "Mensuel", "Semestriel");
            periodeComboBox.setValue("Hebdomadaire");
        }
    }
    
    private void configurerFiltresCours() {
        if (filtreTypeCoursCombo != null) {
            filtreTypeCoursCombo.getItems().addAll("Tous", "CM", "TD", "TP");
            filtreTypeCoursCombo.setValue("Tous");
            filtreTypeCoursCombo.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        }
        
        if (rechercheCoursField != null) {
            rechercheCoursField.setPromptText("Rechercher un cours...");
            rechercheCoursField.textProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        }
    }
    
    private void filtrerCours() {
        if (tousCoursDisponibles == null || tousCoursDisponibles.isEmpty()) return;
        
        List<Cours> filtres = new ArrayList<>(tousCoursDisponibles);
        
        String recherche = rechercheCoursField != null ? rechercheCoursField.getText().toLowerCase() : "";
        if (!recherche.isEmpty()) {
            filtres = filtres.stream()
                .filter(c -> {
                    try {
                        String nomMatiere = getNomMatiere(c.getMatiereId()).toLowerCase();
                        String nomEnseignant = getNomEnseignant(c.getEnseignantId()).toLowerCase();
                        return nomMatiere.contains(recherche) || 
                               nomEnseignant.contains(recherche) ||
                               c.getTypeCours().toLowerCase().contains(recherche);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        }
        
        String typeFiltre = filtreTypeCoursCombo != null ? filtreTypeCoursCombo.getValue() : "Tous";
        if (!"Tous".equals(typeFiltre)) {
            filtres = filtres.stream()
                .filter(c -> c.getTypeCours().equals(typeFiltre))
                .collect(Collectors.toList());
        }
        
        if (coursDisponiblesList != null) coursDisponiblesList.setItems(FXCollections.observableArrayList(filtres));
        if (totalCoursLabel != null) totalCoursLabel.setText(filtres.size() + " cours disponibles");
    }
    
    private void afficherControlesHebdo() {
        if (hebdoControls != null) {
            hebdoControls.setVisible(true);
            hebdoControls.setManaged(true);
        }
        if (mensuelControls != null) {
            mensuelControls.setVisible(false);
            mensuelControls.setManaged(false);
        }
        if (semestrielControls != null) {
            semestrielControls.setVisible(false);
            semestrielControls.setManaged(false);
        }
        if (anneeCombo != null) chargerSemaines(anneeCombo.getValue() != null ? anneeCombo.getValue() : LocalDate.now().getYear());
    }
    
    private void afficherControlesMensuel() {
        if (hebdoControls != null) {
            hebdoControls.setVisible(false);
            hebdoControls.setManaged(false);
        }
        if (mensuelControls != null) {
            mensuelControls.setVisible(true);
            mensuelControls.setManaged(true);
        }
        if (semestrielControls != null) {
            semestrielControls.setVisible(false);
            semestrielControls.setManaged(false);
        }
        if (moisCombo != null && moisCombo.getItems().isEmpty()) {
            chargerMois();
        }
    }
    
    private void afficherControlesSemestriel() {
        if (hebdoControls != null) {
            hebdoControls.setVisible(false);
            hebdoControls.setManaged(false);
        }
        if (mensuelControls != null) {
            mensuelControls.setVisible(false);
            mensuelControls.setManaged(false);
        }
        if (semestrielControls != null) {
            semestrielControls.setVisible(true);
            semestrielControls.setManaged(true);
        }
        if (dateDebutPicker != null) dateDebutPicker.setValue(LocalDate.now());
        if (dateFinPicker != null) dateFinPicker.setValue(LocalDate.now().plusMonths(6));
    }
    
    private void chargerSemaines(int annee) {
        if (semaineCombo == null) return;
        semaineCombo.getItems().clear();
        
        for (int i = 1; i <= 52; i++) {
            LocalDate debut = getPremierJourSemaine(annee, i);
            LocalDate fin = debut.plusDays(4);
            String item = String.format("Semaine %d (%s - %s)", 
                i, 
                debut.format(DateTimeFormatter.ofPattern("dd/MM")),
                fin.format(DateTimeFormatter.ofPattern("dd/MM")));
            semaineCombo.getItems().add(item);
        }
        
        int semaineCourante = getNumeroSemaine(LocalDate.now());
        for (String item : semaineCombo.getItems()) {
            if (item.startsWith("Semaine " + semaineCourante)) {
                semaineCombo.setValue(item);
                break;
            }
        }
    }
    
    private void chargerMois() {
        if (moisCombo == null) return;
        moisCombo.getItems().clear();
        moisCombo.getItems().addAll(
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
        );
        moisCombo.setValue(moisCombo.getItems().get(LocalDate.now().getMonthValue() - 1));
    }
    
    private LocalDate getPremierJourSemaine(int annee, int numeroSemaine) {
        LocalDate date = LocalDate.of(annee, 1, 1);
        while (date.getDayOfWeek().getValue() != 1) {
            date = date.plusDays(1);
        }
        return date.plusWeeks(numeroSemaine - 1);
    }
    
    private int getNumeroSemaine(LocalDate date) {
        return (date.getDayOfYear() / 7) + 1;
    }
    
    private void chargerAnnees() {
        int anneeCourante = LocalDate.now().getYear();
        
        for (int i = anneeCourante - 2; i <= anneeCourante + 2; i++) {
            if (anneeCombo != null) anneeCombo.getItems().add(i);
            if (anneeMoisCombo != null) anneeMoisCombo.getItems().add(i);
        }
        
        if (anneeCombo != null) anneeCombo.setValue(anneeCourante);
        if (anneeMoisCombo != null) anneeMoisCombo.setValue(anneeCourante);
        
        if (anneeCombo != null) anneeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                chargerSemaines(newVal);
            }
        });
    }
    
    private void configurerComposants() {
        if (classeComboBox != null) classeComboBox.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    if (classeSelectionneeLabel != null) classeSelectionneeLabel.setText(newVal.getIntitule());
                    chargerCoursDisponibles(newVal.getId());
                }
            });
        
        if (creerEDTButton != null) creerEDTButton.setOnAction(e -> handleCreerEDT());
        if (validerEDTButton != null) validerEDTButton.setOnAction(e -> handleValiderEDT());
        if (exporterPDFButton != null) exporterPDFButton.setOnAction(e -> exporterPDF());
        if (exporterExcelButton != null) exporterExcelButton.setOnAction(e -> exporterExcel());
        if (verifierConflitsButton != null) verifierConflitsButton.setOnAction(e -> verifierTousLesConflits());
        
        if (validerEDTButton != null) validerEDTButton.setDisable(true);
        if (exporterPDFButton != null) exporterPDFButton.setDisable(true);
        if (exporterExcelButton != null) exporterExcelButton.setDisable(true);
    }
    
    // CHARGEMENT DES DONNÉES
    
    private void chargerClasses() {
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() throws SQLException {
                return classeService.listerToutes();
            }
            
            @Override
            protected void succeeded() {
                if (classeComboBox != null) classeComboBox.setItems(FXCollections.observableArrayList(getValue()));
                
                if (classeComboBox != null) classeComboBox.setCellFactory(lv -> new ListCell<Classe>() {
                    @Override
                    protected void updateItem(Classe item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : 
                            item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                    }
                });
                
                if (classeComboBox != null) classeComboBox.setButtonCell(new ListCell<Classe>() {
                    @Override
                    protected void updateItem(Classe item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : 
                            item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
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
    
    private String getNomMatiere(int matiereId) {
        if (cacheMatieres.containsKey(matiereId)) {
            return cacheMatieres.get(matiereId).getNom();
        }
        
        try {
            Matiere matiere = matiereService.trouverParId(matiereId);
            if (matiere != null) {
                cacheMatieres.put(matiereId, matiere);
                return matiere.getNom();
            }
        } catch (SQLException e) {
            logger.error("Erreur récupération matière", e);
        }
        return "Matière " + matiereId;
    }
    
    private String getNomEnseignant(int enseignantId) {
        return cacheEnseignants.getOrDefault(enseignantId, "Enseignant inconnu");
    }
    
    private void chargerCoursDisponibles(int classeId) {
        Task<List<Cours>> task = new Task<>() {
            @Override
            protected List<Cours> call() throws SQLException {
                return coursService.listerParClasse(classeId);
            }
            
            @Override
            protected void succeeded() {
                tousCoursDisponibles = getValue();
                
                if (coursDisponiblesList != null) coursDisponiblesList.setCellFactory(lv -> new ListCell<Cours>() {
                    @Override
                    protected void updateItem(Cours item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            String nomMatiere = getNomMatiere(item.getMatiereId());
                            String nomEnseignant = getNomEnseignant(item.getEnseignantId());
                            
                            setText(String.format("%s - %s (Prof: %s)", 
                                item.getTypeCours(), 
                                nomMatiere,
                                nomEnseignant));
                        }
                    }
                });
                
                if (coursDisponiblesList != null) coursDisponiblesList.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        Cours coursSelectionne = coursDisponiblesList.getSelectionModel().getSelectedItem();
                        if (coursSelectionne != null) {
                            ouvrirDialogueAjoutCours(coursSelectionne);
                        }
                    }
                });
                
                filtrerCours();
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement cours", getException());
            }
        };
        new Thread(task).start();
    }
    
    // GRILLE EDT
    
    private void configurerGrilleEDT() {
        String[] heures = {"08:00-10:00", "10:00-12:00", "12:00-14:00", "14:00-16:00", "16:00-18:00"};
        String[] jours = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
        
        if (grilleEDT != null) grilleEDT.getChildren().clear();
        if (grilleEDT != null) grilleEDT.getColumnConstraints().clear();
        if (grilleEDT != null) grilleEDT.getRowConstraints().clear();
        
        for (int i = 0; i <= jours.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            if (i == 0) {
                cc.setMinWidth(100);
                cc.setPrefWidth(120);
            } else {
                cc.setMinWidth(140);
                cc.setPrefWidth(160);
            }
            cc.setHgrow(Priority.NEVER);
            if (grilleEDT != null) grilleEDT.getColumnConstraints().add(cc);
        }
        
        for (int i = 0; i <= heures.length; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(70);
            rc.setPrefHeight(80);
            rc.setVgrow(Priority.NEVER);
            if (grilleEDT != null) grilleEDT.getRowConstraints().add(rc);
        }
        
        for (int j = 0; j < jours.length; j++) {
            Label label = new Label(jours[j]);
            label.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-padding: 8; -fx-alignment: center; -fx-font-weight: bold; -fx-font-size: 13px;");
            label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            if (grilleEDT != null) grilleEDT.add(label, j + 1, 0);
        }
        
        for (int i = 0; i < heures.length; i++) {
            Label label = new Label(heures[i]);
            label.setStyle("-fx-background-color: #E3F2FD; -fx-padding: 8; -fx-alignment: center; -fx-font-weight: bold; -fx-font-size: 12px;");
            label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            if (grilleEDT != null) grilleEDT.add(label, 0, i + 1);
        }
        
        for (int i = 0; i < heures.length; i++) {
            for (int j = 0; j < jours.length; j++) {
                StackPane cell = new StackPane();
                cell.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 1px; -fx-background-color: white;");
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                
                VBox content = new VBox(2);
                content.setAlignment(Pos.CENTER);
                content.setPadding(new Insets(3));
                
                Label matiereLabel = new Label("");
                matiereLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #333333;");
                matiereLabel.setWrapText(true);
                
                Label profLabel = new Label("");
                profLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #666666;");
                profLabel.setWrapText(true);
                
                Label salleLabel = new Label("");
                salleLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #1976D2;");
                
                content.getChildren().addAll(matiereLabel, profLabel, salleLabel);
                
                Button supprimerBtn = new Button("✖");
                supprimerBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f44336; -fx-font-size: 10px; -fx-cursor: hand;");
                supprimerBtn.setMaxSize(15, 15);
                supprimerBtn.setVisible(false);
                
                cell.getChildren().addAll(content, supprimerBtn);
                StackPane.setAlignment(supprimerBtn, Pos.TOP_RIGHT);
                
                final int row = i;
                final int col = j;
                
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        ouvrirDialogueAjoutCours(null, jours[col], heures[row]);
                    }
                });
                
                if (grilleEDT != null) grilleEDT.add(cell, j + 1, i + 1);
            }
        }
    }
    
    // DIALOGUE D'AJOUT DE COURS
    
    private void ouvrirDialogueAjoutCours(Cours cours, String jour, String plageHoraire) {
        if (cours == null) {
            Cours coursSelectionne = coursDisponiblesList.getSelectionModel().getSelectedItem();
            if (coursSelectionne == null) {
                afficherErreur("Veuillez d'abord sélectionner un cours dans la liste");
                return;
            }
            cours = coursSelectionne;
        }
        
        final Cours coursFinal = cours;
        
        Dialog<Creneau> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un cours");
        dialog.setHeaderText(String.format("Cours: %s - %s (Prof: %s)", 
            coursFinal.getTypeCours(), 
            getNomMatiere(coursFinal.getMatiereId()),
            getNomEnseignant(coursFinal.getEnseignantId())));
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<String> jourCombo = new ComboBox<>();
        jourCombo.getItems().addAll("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi");
        jourCombo.setValue(jour);
        
        ComboBox<String> heureDebutCombo = new ComboBox<>();
        for (int h = 8; h <= 16; h+=2) {
            heureDebutCombo.getItems().add(String.format("%02d:00", h));
        }
        heureDebutCombo.setValue(plageHoraire.split("-")[0].trim());
        
        ComboBox<Integer> dureeCombo = new ComboBox<>();
        dureeCombo.getItems().addAll(1, 2, 3, 4, 5);
        dureeCombo.setValue(2);
        dureeCombo.setPrefWidth(80);
        
        ToggleGroup assignGroup = new ToggleGroup();
        RadioButton manuelRadio = new RadioButton("Manuel");
        manuelRadio.setToggleGroup(assignGroup);
        manuelRadio.setSelected(true);
        manuelRadio.setStyle("-fx-text-fill: #333333;");
        
        RadioButton autoRadio = new RadioButton("Automatique");
        autoRadio.setToggleGroup(assignGroup);
        autoRadio.setStyle("-fx-text-fill: #333333;");
        
        HBox assignBox = new HBox(20, manuelRadio, autoRadio);
        
        ComboBox<String> salleCombo = new ComboBox<>();
        salleCombo.setPromptText("Choisir une salle");
        salleCombo.setDisable(false);
        salleCombo.setPrefWidth(400);
        
        chargerSallesDisponibles(salleCombo, coursFinal, jour, heureDebutCombo.getValue(), dureeCombo.getValue());
        
        autoRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                salleCombo.setDisable(true);
                String jourCourant = jourCombo.getValue();
                String heureCourante = heureDebutCombo.getValue();
                int dureeCourante = dureeCombo.getValue();
                proposerSalleAutomatique(salleCombo, coursFinal, jourCourant, heureCourante, dureeCourante);
            } else {
                salleCombo.setDisable(false);
                String jourCourant = jourCombo.getValue();
                String heureCourante = heureDebutCombo.getValue();
                int dureeCourante = dureeCombo.getValue();
                chargerSallesDisponibles(salleCombo, coursFinal, jourCourant, heureCourante, dureeCourante);
            }
        });
        
        heureDebutCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String jourCourant = jourCombo.getValue();
                int dureeCourante = dureeCombo.getValue();
                chargerSallesDisponibles(salleCombo, coursFinal, jourCourant, newVal, dureeCourante);
            }
        });
        
        dureeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String jourCourant = jourCombo.getValue();
                String heureCourante = heureDebutCombo.getValue();
                chargerSallesDisponibles(salleCombo, coursFinal, jourCourant, heureCourante, newVal);
            }
        });
        
        Label conflitLabel = new Label();
        conflitLabel.setStyle("-fx-text-fill: #4CAF50;");
        
        Button verifierBtn = new Button("Vérifier les conflits");
        verifierBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 5 15;");
        verifierBtn.setOnAction(e -> {
            String jourCourant = jourCombo.getValue();
            String heureCourante = heureDebutCombo.getValue();
            int dureeCourante = dureeCombo.getValue();
            verifierConflits(coursFinal, jourCourant, heureCourante, dureeCourante, conflitLabel);
        });
        
        grid.add(new Label("Jour:"), 0, 0);
        grid.add(jourCombo, 1, 0);
        grid.add(new Label("Heure début:"), 0, 1);
        grid.add(heureDebutCombo, 1, 1);
        grid.add(new Label("Durée (heures):"), 0, 2);
        grid.add(dureeCombo, 1, 2);
        grid.add(new Label("Assignation:"), 0, 3);
        grid.add(assignBox, 1, 3);
        grid.add(new Label("Salle:"), 0, 4);
        grid.add(salleCombo, 1, 4);
        grid.add(verifierBtn, 0, 5);
        grid.add(conflitLabel, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Creneau creneau = new Creneau();
                creneau.setCoursId(coursFinal.getId());
                creneau.setJour(calculerDateProchaine(jourCombo.getValue()));
                creneau.setHeureDebut(heureDebutCombo.getValue());
                creneau.setHeureFin(calculerHeureFin(heureDebutCombo.getValue(), dureeCombo.getValue()));
                
                String salleStr = salleCombo.getValue();
                if (salleStr != null && !salleStr.isEmpty() && !salleStr.contains("Aucune") && !salleStr.contains("Erreur")) {
                    String[] parts = salleStr.split(" - ");
                    String numeroSalle = parts[0];
                    
                    for (Salle s : cacheSalles.values()) {
                        if (s.getNumero().equals(numeroSalle)) {
                            creneau.setSalleId(s.getId());
                            break;
                        }
                    }
                }
                
                return creneau;
            }
            return null;
        });
        
        Optional<Creneau> result = dialog.showAndWait();
        result.ifPresent(creneau -> {
            try {
                List<String> conflits = conflitService.verifierConflits(creneau);
                
                if (!conflits.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Conflits détectés");
                    alert.setHeaderText("Des conflits ont été détectés");
                    alert.setContentText(String.join("\n", conflits));
                    
                    ButtonType alternativesBtn = new ButtonType("Voir alternatives");
                    ButtonType ajouterQuandMemeBtn = new ButtonType("Ajouter quand même");
                    alert.getButtonTypes().setAll(alternativesBtn, ajouterQuandMemeBtn, ButtonType.CANCEL);
                    
                    Optional<ButtonType> resultAlert = alert.showAndWait();
                    if (resultAlert.isPresent() && resultAlert.get() == alternativesBtn) {
                        afficherAlternatives(creneau, coursFinal);
                    } else if (resultAlert.isPresent() && resultAlert.get() == ajouterQuandMemeBtn) {
                        creneauxTemp.add(creneau);
                        mettreAJourGrille();
                    }
                } else {
                    creneauxTemp.add(creneau);
                    mettreAJourGrille();
                    if (conflitsLabel != null) conflitsLabel.setText("✓ Aucun conflit");
                }
                
            } catch (SQLException e) {
                logger.error("Erreur vérification conflits", e);
                afficherErreur("Erreur lors de la vérification des conflits");
            }
        });
    }
    
    private void ouvrirDialogueAjoutCours(Cours cours) {
        ouvrirDialogueAjoutCours(cours, "Lundi", "08:00-10:00");
    }
    
    private void chargerSallesDisponibles(ComboBox<String> salleCombo, Cours cours, String jour, String heureDebut, int dureeHeures) {
        Task<List<Salle>> task = new Task<>() {
            @Override
            protected List<Salle> call() throws SQLException {
                String heureFin = calculerHeureFin(heureDebut, dureeHeures);
                String date = calculerDateProchaine(jour);
                return salleService.rechercherSallesDisponibles(date, heureDebut, heureFin, cours.getClasseId());
            }
            
            @Override
            protected void succeeded() {
                List<Salle> salles = getValue();
                salleCombo.getItems().clear();
                
                if (salles.isEmpty()) {
                    salleCombo.getItems().add("Aucune salle disponible pour ce créneau");
                    salleCombo.setDisable(false);
                } else {
                    salleCombo.setDisable(false);
                    for (Salle s : salles) {
                        cacheSalles.put(s.getId(), s);
                        String statut = s.getStatut().equals("disponible") ? "✓" : "❌";
                        Batiment batiment = cacheBatiments.get(s.getBatimentId());
                        String batimentNom = (batiment != null) ? batiment.getNom() : "Bâtiment inconnu";
                        
                        salleCombo.getItems().add(String.format("%s - %s (Cap:%d, Ét:%d, %s) %s",
                            s.getNumero(), batimentNom, s.getCapacite(), s.getEtage(), 
                            s.getType(), statut));
                    }
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement salles", getException());
                salleCombo.getItems().clear();
                salleCombo.getItems().add("Erreur de chargement des salles");
                salleCombo.setDisable(false);
            }
        };
        new Thread(task).start();
    }
    
    private void proposerSalleAutomatique(ComboBox<String> salleCombo, Cours cours, String jour, String heureDebut, int dureeHeures) {
        Task<Salle> task = new Task<>() {
            @Override
            protected Salle call() throws SQLException {
                String heureFin = calculerHeureFin(heureDebut, dureeHeures);
                String date = calculerDateProchaine(jour);
                return attributionService.trouverSalleOptimale(cours, date, heureDebut, heureFin);
            }
            
            @Override
            protected void succeeded() {
                Salle salle = getValue();
                salleCombo.getItems().clear();
                
                if (salle != null) {
                    cacheSalles.put(salle.getId(), salle);
                    Batiment batiment = cacheBatiments.get(salle.getBatimentId());
                    String batimentNom = (batiment != null) ? batiment.getNom() : "Bâtiment inconnu";
                    
                    salleCombo.getItems().add(String.format("%s - %s (Cap:%d, Ét:%d) [OPTIMAL]",
                        salle.getNumero(), batimentNom, salle.getCapacite(), salle.getEtage()));
                    salleCombo.setValue(salleCombo.getItems().get(0));
                    salleCombo.setDisable(true);
                } else {
                    salleCombo.getItems().add("Aucune salle optimale trouvée");
                }
            }
            
            @Override
            protected void failed() {
                afficherErreur("Erreur lors de l'assignation automatique");
            }
        };
        new Thread(task).start();
    }
    
    private void verifierConflits(Cours cours, String jour, String heureDebut, int dureeHeures, Label conflitLabel) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws SQLException {
                String heureFin = calculerHeureFin(heureDebut, dureeHeures);
                String date = calculerDateProchaine(jour);
                Creneau temp = new Creneau();
                temp.setCoursId(cours.getId());
                temp.setJour(date);
                temp.setHeureDebut(heureDebut);
                temp.setHeureFin(heureFin);
                return conflitService.verifierConflits(temp);
            }
            
            @Override
            protected void succeeded() {
                List<String> conflits = getValue();
                if (conflits.isEmpty()) {
                    conflitLabel.setText("✓ Aucun conflit");
                    conflitLabel.setStyle("-fx-text-fill: #4CAF50;");
                } else {
                    conflitLabel.setText("⚠ " + conflits.size() + " conflit(s)");
                    conflitLabel.setStyle("-fx-text-fill: #f44336;");
                    Tooltip tooltip = new Tooltip(String.join("\n", conflits));
                    conflitLabel.setTooltip(tooltip);
                }
            }
        };
        new Thread(task).start();
    }
    
    private void afficherAlternatives(Creneau creneau, Cours cours) {
        try {
            List<Map<String, Object>> alternatives = conflitService.proposerAlternatives(creneau, cours.getClasseId());
            
            if (alternatives.isEmpty()) {
                afficherErreur("Aucune alternative trouvée");
                return;
            }
            
            List<String> descriptions = new ArrayList<>();
            for (Map<String, Object> alt : alternatives) {
                descriptions.add((String) alt.get("message"));
            }
            
            ChoiceDialog<String> dialog = new ChoiceDialog<>(descriptions.get(0), descriptions);
            dialog.setTitle("Alternatives");
            dialog.setHeaderText("Choisissez une alternative");
            dialog.setContentText("Options disponibles:");
            
            Node choiceBox = dialog.getDialogPane().lookup(".choice-box");
            if (choiceBox != null) {
                choiceBox.setStyle("-fx-font-size: 14px;");
            }
            
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(selectedDesc -> {
                for (Map<String, Object> alt : alternatives) {
                    if (alt.get("message").equals(selectedDesc)) {
                        String type = (String) alt.get("type");
                        if (type.equals("salle_alternative")) {
                            Salle salle = (Salle) alt.get("salle");
                            creneau.setSalleId(salle.getId());
                            creneauxTemp.add(creneau);
                            mettreAJourGrille();
                        } else if (type.equals("creneau_alternatif")) {
                            String heure = (String) alt.get("heureDebut");
                            String heureFin = (String) alt.get("heureFin");
                            creneau.setHeureDebut(heure);
                            creneau.setHeureFin(heureFin);
                            creneauxTemp.add(creneau);
                            mettreAJourGrille();
                        } else if (type.equals("jour_alternatif")) {
                            String jour = (String) alt.get("jour");
                            creneau.setJour(jour);
                            creneauxTemp.add(creneau);
                            mettreAJourGrille();
                        }
                        break;
                    }
                }
            });
            
        } catch (SQLException e) {
            logger.error("Erreur chargement alternatives", e);
        }
    }
    
    // GESTION EDT
    
    @FXML
    private void handleCreerEDT() {
        if (classeComboBox == null || classeComboBox.getValue() == null) {
            afficherErreur("Veuillez sélectionner une classe");
            return;
        }
        
        edtEnCours = new EmploiDuTemps();
        edtEnCours.setClasseId(classeComboBox.getValue().getId());
        
        if (hebdoRadio != null && hebdoRadio.isSelected()) {
            edtEnCours.setPeriodeType("hebdomadaire");
            
            if (semaineCombo != null && semaineCombo.getValue() != null && anneeCombo != null && anneeCombo.getValue() != null) {
                String semaineStr = semaineCombo.getValue();
                int annee = anneeCombo.getValue();
                int numSemaine = extraireNumeroSemaine(semaineStr);
                
                LocalDate debut = getPremierJourSemaine(annee, numSemaine);
                LocalDate fin = debut.plusDays(4);
                edtEnCours.setPeriodeDebut(debut.toString());
                edtEnCours.setPeriodeFin(fin.toString());
            } else {
                LocalDate debut = getPremierJourSemaine(LocalDate.now().getYear(), getNumeroSemaine(LocalDate.now()));
                LocalDate fin = debut.plusDays(4);
                edtEnCours.setPeriodeDebut(debut.toString());
                edtEnCours.setPeriodeFin(fin.toString());
            }
            
        } else if (mensuelRadio != null && mensuelRadio.isSelected()) {
            edtEnCours.setPeriodeType("mensuel");
            
            if (moisCombo != null && moisCombo.getValue() != null && anneeMoisCombo != null && anneeMoisCombo.getValue() != null) {
                int annee = anneeMoisCombo.getValue();
                int mois = moisCombo.getSelectionModel().getSelectedIndex() + 1;
                
                LocalDate debut = LocalDate.of(annee, mois, 1);
                LocalDate fin = debut.plusMonths(1).minusDays(1);
                edtEnCours.setPeriodeDebut(debut.toString());
                edtEnCours.setPeriodeFin(fin.toString());
            }
            
        } else {
            edtEnCours.setPeriodeType("semestriel");
            
            if (dateDebutPicker != null && dateDebutPicker.getValue() != null && dateFinPicker != null && dateFinPicker.getValue() != null) {
                edtEnCours.setPeriodeDebut(dateDebutPicker.getValue().toString());
                edtEnCours.setPeriodeFin(dateFinPicker.getValue().toString());
            }
        }
        
        creneauxTemp.clear();
        if (creerEDTButton != null) creerEDTButton.setDisable(true);
        if (validerEDTButton != null) validerEDTButton.setDisable(false);
        
        afficherNotification("Nouvel EDT", 
            "Création d'un emploi du temps " + edtEnCours.getPeriodeType() + 
            " pour " + classeComboBox.getValue().getIntitule());
    }
    
    private int extraireNumeroSemaine(String semaineStr) {
        try {
            String[] parts = semaineStr.split(" ");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return getNumeroSemaine(LocalDate.now());
        }
    }
    
    @FXML
    private void handleValiderEDT() {
        if (creneauxTemp.isEmpty()) {
            afficherErreur("Aucun créneau défini");
            return;
        }
        
        try {
            if (edtEnCours.getPeriodeType() == null) {
                edtEnCours.setPeriodeType("hebdomadaire");
            }
            
            if (edtEnCours.getPeriodeDebut() == null) {
                edtEnCours.setPeriodeDebut(LocalDate.now().toString());
            }
            if (edtEnCours.getPeriodeFin() == null) {
                edtEnCours.setPeriodeFin(LocalDate.now().plusWeeks(1).toString());
            }
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    planningService.creerEmploiDuTemps(edtEnCours, creneauxTemp);
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    afficherNotification("Succès", "Emploi du temps créé avec succès");
                    if (creerEDTButton != null) creerEDTButton.setDisable(false);
                    if (validerEDTButton != null) validerEDTButton.setDisable(true);
                    if (exporterPDFButton != null) exporterPDFButton.setDisable(false);
                    if (exporterExcelButton != null) exporterExcelButton.setDisable(false);
                }
                
                @Override
                protected void failed() {
                    logger.error("Erreur création EDT", getException());
                    afficherErreur("Erreur lors de la création de l'EDT: " + getException().getMessage());
                }
            };
            new Thread(task).start();
            
        } catch (Exception e) {
            logger.error("Erreur validation EDT", e);
            afficherErreur("Erreur lors de la validation");
        }
    }
    
    private void verifierTousLesConflits() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                List<String> tousConflits = new ArrayList<>();
                for (Creneau c : creneauxTemp) {
                    tousConflits.addAll(conflitService.verifierConflits(c));
                }
                return tousConflits;
            }
            
            @Override
            protected void succeeded() {
                List<String> conflits = getValue();
                if (conflits.isEmpty()) {
                    if (conflitsLabel != null) conflitsLabel.setText("✓ Aucun conflit");
                    if (conflitsLabel != null) conflitsLabel.setStyle("-fx-text-fill: #4CAF50;");
                } else {
                    if (conflitsLabel != null) conflitsLabel.setText("⚠ " + conflits.size() + " conflit(s) détecté(s)");
                    if (conflitsLabel != null) conflitsLabel.setStyle("-fx-text-fill: #f44336;");
                    
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Conflits détectés");
                    alert.setHeaderText(conflits.size() + " conflit(s) trouvé(s)");
                    
                    TextArea textArea = new TextArea(String.join("\n\n", conflits));
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    
                    alert.getDialogPane().setContent(textArea);
                    alert.setResizable(true);
                    alert.show();
                }
            }
        };
        new Thread(task).start();
    }
    
    // MISE À JOUR DE LA GRILLE
    
    private void mettreAJourGrille() {
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 6; j++) {
                StackPane cell = (StackPane) getCellFromGridPane(grilleEDT, j, i);
                if (cell == null) continue;
                
                cell.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 1px; -fx-background-color: white;");
                
                for (javafx.scene.Node node : cell.getChildren()) {
                    if (node instanceof VBox) {
                        VBox content = (VBox) node;
                        for (javafx.scene.Node child : content.getChildren()) {
                            if (child instanceof Label) {
                                ((Label) child).setText("");
                            }
                        }
                    } else if (node instanceof Button) {
                        node.setVisible(false);
                    }
                }
            }
        }
        
        for (Creneau c : creneauxTemp) {
            int row = getRowFromHeure(c.getHeureDebut());
            int col = getColFromJour(c.getJour());
            
            StackPane cell = (StackPane) getCellFromGridPane(grilleEDT, col, row);
            if (cell == null) continue;
            
            try {
                Cours cours = coursService.trouverParId(c.getCoursId());
                if (cours != null) {
                    String nomMatiere = getNomMatiere(cours.getMatiereId());
                    String nomEnseignant = getNomEnseignant(cours.getEnseignantId());
                    
                    for (javafx.scene.Node node : cell.getChildren()) {
                        if (node instanceof VBox) {
                            VBox content = (VBox) node;
                            int labelIndex = 0;
                            for (javafx.scene.Node child : content.getChildren()) {
                                if (child instanceof Label) {
                                    Label label = (Label) child;
                                    if (labelIndex == 0) {
                                        label.setText(nomMatiere);
                                    } else if (labelIndex == 1) {
                                        label.setText(nomEnseignant);
                                    } else if (labelIndex == 2) {
                                        if (c.getSalleId() != null) {
                                            Salle salle = cacheSalles.get(c.getSalleId());
                                            label.setText(salle != null ? "Salle " + salle.getNumero() : "Salle " + c.getSalleId());
                                        } else {
                                            label.setText("Salle non assignée");
                                        }
                                    }
                                    labelIndex++;
                                }
                            }
                        } else if (node instanceof Button) {
                            Button supprimerBtn = (Button) node;
                            supprimerBtn.setVisible(false);
                            
                            StackPane parentCell = (StackPane) supprimerBtn.getParent();
                            parentCell.setOnMouseEntered(evt -> supprimerBtn.setVisible(true));
                            parentCell.setOnMouseExited(evt -> supprimerBtn.setVisible(false));
                            
                            final Creneau creneauASupprimer = c;
                            supprimerBtn.setOnAction(e -> {
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Confirmation");
                                alert.setHeaderText("Supprimer ce cours ?");
                                alert.setContentText("Voulez-vous vraiment supprimer ce créneau ?");
                                
                                alert.showAndWait().ifPresent(response -> {
                                    if (response == ButtonType.OK) {
                                        creneauxTemp.remove(creneauASupprimer);
                                        mettreAJourGrille();
                                    }
                                });
                            });
                        }
                    }
                    
                    Color couleur = getCouleurPourType(cours.getTypeCours());
                    String style = String.format("-fx-border-color: #1976D2; -fx-border-width: 2px; -fx-background-color: rgba(%d, %d, %d, 0.2);",
                        (int)(couleur.getRed() * 255),
                        (int)(couleur.getGreen() * 255),
                        (int)(couleur.getBlue() * 255));
                    cell.setStyle(style);
                }
            } catch (SQLException e) {
                logger.error("Erreur affichage cellule", e);
            }
        }
    }
    
    private Color getCouleurPourType(String typeCours) {
        switch (typeCours) {
            case "CM": return COULEUR_CM;
            case "TD": return COULEUR_TD;
            case "TP": return COULEUR_TP;
            default: return Color.rgb(200, 200, 200, 0.3);
        }
    }
    
    private javafx.scene.Node getCellFromGridPane(GridPane grid, int col, int row) {
        for (javafx.scene.Node node : grid.getChildren()) {
            if (GridPane.getColumnIndex(node) != null && GridPane.getRowIndex(node) != null &&
                GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return node;
            }
        }
        return null;
    }
    
    private int getRowFromHeure(String heure) {
        switch (heure) {
            case "08:00": return 1;
            case "10:00": return 2;
            case "12:00": return 3;
            case "14:00": return 4;
            case "16:00": return 5;
            default: return 1;
        }
    }
    
    private int getColFromJour(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return date.getDayOfWeek().getValue();
    }
    
    private String calculerDateProchaine(String jour) {
        Map<String, Integer> joursMap = Map.of(
            "Lundi", 1, "Mardi", 2, "Mercredi", 3,
            "Jeudi", 4, "Vendredi", 5, "Samedi", 6
        );
        
        LocalDate today = LocalDate.now();
        int todayValue = today.getDayOfWeek().getValue();
        Integer targetValue = joursMap.get(jour);
        
        if (targetValue == null) {
            logger.error("❌ Jour invalide: {}", jour);
            return today.toString();
        }
        
        int daysToAdd = targetValue - todayValue;
        if (daysToAdd < 0) {
            daysToAdd += 7;
        }
        
        return today.plusDays(daysToAdd).toString();
    }
    
    private String calculerHeureFin(String heureDebut, int dureeHeures) {
        String[] parts = heureDebut.split(":");
        int heures = Integer.parseInt(parts[0]);
        heures += dureeHeures * 2;
        return String.format("%02d:00", heures);
    }
    
    private void exporterPDF() {
        afficherNotification("Export PDF", "Fonctionnalité à venir");
    }
    
    private void exporterExcel() {
        afficherNotification("Export Excel", "Fonctionnalité à venir");
    }
    
    @FXML
    public void handleRetourGestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordGestionnaire.fxml"));
            Parent root = loader.load();
            TableauBordGestionnaireControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner");
        }
    }
    
    @FXML
    private void handleVoirEDT() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListeEmploisTemps.fxml"));
            Parent root = loader.load();
            ListeEmploisTempsControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur navigation", e);
        }
    }
    
    @FXML
    public void handleGestionMatieres() {
        naviguerVers("/fxml/GestionMatieres.fxml");
    }
    
    @FXML
    public void handleGestionCours() {
        naviguerVers("/fxml/GestionCours.fxml");
    }
    
    @FXML
    public void handleRecherche() {
        naviguerVers("/fxml/Recherche.fxml");
    }
    
    @FXML
    public void handleCarte() {
        try {
            System.out.println("🔍 Navigation vers Carte depuis Gestionnaire");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Carte.fxml"));
            Parent root = loader.load();
            
            CarteControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("SCHEDULER - Carte interactive");
            
        } catch (IOException e) {
            logger.error("Erreur navigation carte", e);
            afficherErreur("Impossible d'ouvrir la carte");
        }
    }
    
    @FXML
    public void handleNouvelEDT() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NouvelEdt.fxml"));
            Parent root = loader.load();
            NouvelEdtControleur ctrl = loader.getController();
            ctrl.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur navigation NouvelEdt", e);
            afficherErreur("Impossible d'ouvrir la création d'emploi du temps");
        }
    }
    
    	private void naviguerVers(String fxmlPath) {
    	    try {
    	        System.out.println("🔍 Navigation vers: " + fxmlPath);
    	        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    	        Parent root = loader.load();
    	        
    	        // ✅ Charger dans contenuPrincipal, pas nouvelle scène
    	        contenuPrincipal.getChildren().clear();
    	        contenuPrincipal.getChildren().add(root);
    	        
    	        // Transmettre l'utilisateur au contrôleur enfant
    	        Object controleur = loader.getController();
    	        if (controleur instanceof TableauBordControleur) {
    	            ((TableauBordControleur) controleur).initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
    	        }
    	        
    	    } catch (IOException e) {
    	        logger.error("❌ Erreur navigation vers {}", fxmlPath, e);
    	        afficherErreur("Impossible d'accéder à cette page");
    	    }
    	}
    
    public void preparerModificationEDT(EmploiDuTemps edt) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NouvelEdt.fxml"));
            Parent root = loader.load();
            NouvelEdtControleur ctrl = loader.getController();
            ctrl.setUtilisateurConnectePublic(utilisateurConnecte);
            ctrl.initialiserModification(edt, primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur préparation modification EDT", e);
            afficherErreur("Impossible d'ouvrir la modification");
        }
    }
    
    private void chargerCreneauxEDT(int edtId) {
        Task<List<Creneau>> task = new Task<>() {
            @Override
            protected List<Creneau> call() throws SQLException {
                return planningService.getCreneauxParEmploiDuTemps(edtId);
            }
            
            @Override
            protected void succeeded() {
                creneauxTemp = getValue();
                mettreAJourGrille();
                if (creerEDTButton != null) creerEDTButton.setDisable(true);
                if (validerEDTButton != null) validerEDTButton.setDisable(false);
                if (exporterPDFButton != null) exporterPDFButton.setDisable(false);
                if (exporterExcelButton != null) exporterExcelButton.setDisable(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement créneaux", getException());
            }
        };
        new Thread(task).start();
    }
    
    @FXML
    private void handleExporterPDF() {
        if (edtEnCours == null) {
            afficherErreur("Aucun emploi du temps a exporter.");
            return;
        }
        try {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Enregistrer le PDF");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fc.setInitialFileName("EDT_" + (classeComboBox != null && classeComboBox.getValue() != null ? classeComboBox.getValue().getIntitule() : "export") + ".pdf");
            java.io.File file = fc.showSaveDialog(primaryStage);
            if (file != null) {
                String titre = (classeComboBox != null && classeComboBox.getValue() != null) ? classeComboBox.getValue().getIntitule() : "Emploi du temps";
                exportService.exporterEDTGrillePDF(creneauxTemp, classeComboBox != null ? classeComboBox.getValue() : null, titre, utilisateurConnecte, file.getAbsolutePath());
                afficherNotification("Export PDF", "Fichier enregistre : " + file.getName());
            }
        } catch (Exception e) {
            logger.error("Erreur export PDF", e);
            afficherErreur("Erreur lors de l'export PDF : " + e.getMessage());
        }
    }
    
    @FXML
    private void handleExporterExcel() {
        if (edtEnCours == null) {
            afficherErreur("Aucun emploi du temps a exporter.");
            return;
        }
        try {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Enregistrer le fichier Excel");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            fc.setInitialFileName("EDT_" + (classeComboBox != null && classeComboBox.getValue() != null ? classeComboBox.getValue().getIntitule() : "export") + ".xlsx");
            java.io.File file = fc.showSaveDialog(primaryStage);
            if (file != null) {
                String titre = (classeComboBox != null && classeComboBox.getValue() != null) ? classeComboBox.getValue().getIntitule() : "Emploi du temps";
                exportService.exporterEDTGrilleExcel(creneauxTemp, classeComboBox != null ? classeComboBox.getValue() : null, titre, utilisateurConnecte, file.getAbsolutePath());
                afficherNotification("Export Excel", "Fichier enregistre : " + file.getName());
            }
        } catch (Exception e) {
            logger.error("Erreur export Excel", e);
            afficherErreur("Erreur lors de l'export Excel : " + e.getMessage());
        }
    }
    
    @FXML
    protected void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordGestionnaire.fxml"));
            Parent root = loader.load();
            TableauBordGestionnaireControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner");
        }
    }
    
    @FXML
    public void handleToutesReservations() {
        Task<List<Reservation>> task = new Task<>() {
            @Override
            protected List<Reservation> call() throws Exception {
                return reservationService.listerTous();
            }
            @Override
            protected void succeeded() {
                afficherDialogueReservations(getValue());
            }
            @Override
            protected void failed() {
                afficherErreur("Erreur lors du chargement des réservations.");
            }
        };
        new Thread(task).start();
    }
    
    private void afficherDialogueReservations(List<Reservation> reservations) {
        Stage popup = new Stage();
        popup.setTitle("Gestion des réservations");
        popup.initOwner(primaryStage);
        popup.initModality(javafx.stage.Modality.WINDOW_MODAL);
        
        TableView<Reservation> table = new TableView<>();
        table.setPrefWidth(900);
        table.setPrefHeight(450);
        
        TableColumn<Reservation, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().getDateReservation()));
        dateCol.setPrefWidth(100);
        
        TableColumn<Reservation, String> heureCol = new TableColumn<>("Horaire");
        heureCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().getHeureDebut() + " → " + cd.getValue().getHeureFin()));
        heureCol.setPrefWidth(110);
        
        TableColumn<Reservation, String> salleCol = new TableColumn<>("Salle");
        salleCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> "Salle #" + cd.getValue().getSalleId()));
        salleCol.setPrefWidth(80);
        
        TableColumn<Reservation, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().getType()));
        typeCol.setPrefWidth(90);
        
        TableColumn<Reservation, String> utilisateurCol = new TableColumn<>("Demandeur");
        utilisateurCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> "Utilisateur #" + cd.getValue().getUtilisateurId()));
        utilisateurCol.setPrefWidth(120);
        
        TableColumn<Reservation, String> statutCol = new TableColumn<>("Statut");
        statutCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().getStatut()));
        statutCol.setPrefWidth(90);
        statutCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                switch (s) {
                    case "confirmee" -> setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    case "en_cours"  -> setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    case "annulee","refusee" -> setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });
        
        TableColumn<Reservation, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(300);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Reservation r = getTableView().getItems().get(getIndex());
                HBox box = new HBox(6);
                
                if ("en_cours".equals(r.getStatut())) {
                    Button valBtn = new Button("✅ Valider");
                    valBtn.setStyle("-fx-background-color:#E8F5E9;-fx-text-fill:#2E7D32;-fx-background-radius:5;-fx-padding:4 8;-fx-cursor:hand;");
                    valBtn.setOnAction(e -> {
                        try { reservationService.validerReservation(r.getId());
                            r.setStatut("confirmee"); table.refresh();
                            afficherNotification("Validée", "Réservation validée");
                        } catch (Exception ex) { afficherErreur("Erreur validation"); }
                    });
                    box.getChildren().add(valBtn);
                    
                    Button refBtn = new Button("❌ Refuser");
                    refBtn.setStyle("-fx-background-color:#FFEBEE;-fx-text-fill:#C62828;-fx-background-radius:5;-fx-padding:4 8;-fx-cursor:hand;");
                    refBtn.setOnAction(e -> {
                        TextInputDialog d = new TextInputDialog();
                        d.setTitle("Motif de refus");
                        d.setHeaderText("Indiquez le motif du refus");
                        d.showAndWait().ifPresent(motif -> {
                            try { reservationService.refuserReservation(r.getId(), motif);
                                r.setStatut("refusee"); table.refresh();
                            } catch (Exception ex) { afficherErreur("Erreur refus"); }
                        });
                    });
                    box.getChildren().add(refBtn);
                }
                
                Button conflitBtn = new Button("⚠️ Gérer conflit");
                conflitBtn.setStyle("-fx-background-color:#FFF3E0;-fx-text-fill:#E65100;-fx-background-radius:5;-fx-padding:4 8;-fx-cursor:hand;");
                conflitBtn.setOnAction(e -> gererConflitReservation(r, popup));
                box.getChildren().add(conflitBtn);
                
                setGraphic(box);
            }
        });
        
        table.getColumns().addAll(dateCol, heureCol, salleCol, typeCol, utilisateurCol, statutCol, actionsCol);
        table.getItems().setAll(reservations);
        
        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 15; -fx-background-color: #F0F4F8;");
        Label titre = new Label("Toutes les réservations - " + reservations.size() + " au total");
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");
        root.getChildren().addAll(titre, table);
        
        Scene scene = new Scene(root, 950, 550);
        popup.setScene(scene);
        popup.show();
    }
    
    private void gererConflitReservation(Reservation reservation, Stage parent) {
        Alert choix = new Alert(Alert.AlertType.CONFIRMATION);
        choix.initOwner(parent);
        choix.setTitle("Gérer le conflit");
        choix.setHeaderText("Réservation du " + reservation.getDateReservation()
            + " - " + reservation.getHeureDebut() + " → " + reservation.getHeureFin());
        choix.setContentText("Comment résoudre ce conflit ?");
        
        ButtonType btnSalle = new ButtonType("🏫 Changer de salle");
        ButtonType btnCreneau = new ButtonType("📅 Changer de créneau");
        ButtonType btnAnnul = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        choix.getButtonTypes().setAll(btnSalle, btnCreneau, btnAnnul);
        
        choix.showAndWait().ifPresent(reponse -> {
            if (reponse == btnSalle) {
                Task<List<Salle>> task = new Task<>() {
                    @Override
                    protected List<Salle> call() throws SQLException {
                        List<Salle> toutesSalles = salleService.listerTous();
                        List<Salle> alternatives = new ArrayList<>();
                        for (Salle s : toutesSalles) {
                            if (s.getId() == reservation.getSalleId()) continue;
                            boolean libre = salleService.estDisponible(
                                s.getId(), 
                                reservation.getDateReservation(),
                                reservation.getHeureDebut(),
                                reservation.getHeureFin()
                            );
                            if (libre && s.getStatut().equals("disponible")) {
                                alternatives.add(s);
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
                            nomsSalles.add("Salle " + s.getNumero() + " (Cap: " + s.getCapacite() + ")");
                        }
                        
                        ChoiceDialog<String> dialog = new ChoiceDialog<>(nomsSalles.get(0), nomsSalles);
                        dialog.setTitle("Changer de salle");
                        dialog.setHeaderText("Choisissez une nouvelle salle");
                        
                        dialog.showAndWait().ifPresent(choixSalle -> {
                            String numero = choixSalle.split(" ")[1];
                            for (Salle s : alternatives) {
                                if (s.getNumero().equals(numero)) {
                                    try {
                                    	reservationService.resoudreConflit(
                                    		    reservation.getId(),
                                    		    s.getId(),
                                    		    null,
                                    		    null,
                                    		    null,
                                    		    "Salle changée vers " + s.getNumero(),
                                    		    emailService,
                                    		    utilisateurService,
                                    		    utilisateurConnecte != null ? utilisateurConnecte.getId() : 1  // ← 9ème argument
                                    		);
                                        afficherNotification("Conflit résolu", 
                                            "Salle changée - email envoyé au concerné");
                                    } catch (SQLException ex) {
                                        afficherErreur("Erreur : " + ex.getMessage());
                                    }
                                    break;
                                }
                            }
                        });
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("Erreur chargement salles alternatives");
                    }
                };
                new Thread(task).start();
                
            } else if (reponse == btnCreneau) {
                TextInputDialog d = new TextInputDialog(reservation.getDateReservation());
                d.setTitle("Nouveau créneau");
                d.setHeaderText("Entrez la nouvelle date (YYYY-MM-DD)");
                d.showAndWait().ifPresent(nouvelleDate -> {
                    try {
                    	reservationService.resoudreConflit(
                    		    reservation.getId(),
                    		    null,
                    		    nouvelleDate.trim(),
                    		    null,
                    		    null,
                    		    "Changement de date : " + nouvelleDate,
                    		    emailService,
                    		    utilisateurService,
                    		    utilisateurConnecte != null ? utilisateurConnecte.getId() : 1  // ← 9ème argument
                    		);
                        afficherNotification("Conflit résolu", 
                            "Date changée - email envoyé au concerné");
                    } catch (SQLException ex) {
                        afficherErreur("Erreur : " + ex.getMessage());
                    }
                });
            }
        });
    }
    
    @FXML
    public void handleHistoriqueReservations() {
        naviguerVers("/fxml/HistoriqueReservation.fxml");
    }
    
 // MÉTHODES SIDEBAR (À AJOUTER)

    private void configurerSidebar() {
        sousMenuPlanning.setVisible(sousMenuPlanningVisible);
        sousMenuPlanning.setManaged(sousMenuPlanningVisible);
        sousMenuGestion.setVisible(sousMenuGestionVisible);
        sousMenuGestion.setManaged(sousMenuGestionVisible);
        
        sidebarPlanningBtn.setText(sousMenuPlanningVisible ? "📅 Planning ▼" : "📅 Planning ▶");
        sidebarGestionBtn.setText(sousMenuGestionVisible ? "⚙️ Gestion ▼" : "⚙️ Gestion ▶");
        
        // Toggle Planning
        sidebarPlanningBtn.setOnAction(e -> {
            sousMenuPlanningVisible = !sousMenuPlanningVisible;
            sousMenuPlanning.setVisible(sousMenuPlanningVisible);
            sousMenuPlanning.setManaged(sousMenuPlanningVisible);
            sidebarPlanningBtn.setText(sousMenuPlanningVisible ? "📅 Planning ▼" : "📅 Planning ▶");
        });
        
        // Toggle Gestion
        sidebarGestionBtn.setOnAction(e -> {
            sousMenuGestionVisible = !sousMenuGestionVisible;
            sousMenuGestion.setVisible(sousMenuGestionVisible);
            sousMenuGestion.setManaged(sousMenuGestionVisible);
            sidebarGestionBtn.setText(sousMenuGestionVisible ? "⚙️ Gestion ▼" : "⚙️ Gestion ▶");
        });
        
        // Navigation Planning
        sidebarCreerEdtBtn.setOnAction(e -> naviguerVers("/fxml/NouvelEdt.fxml"));
        sidebarListeEdtBtn.setOnAction(e -> naviguerVers("/fxml/ListeEmploisTemps.fxml"));
        
        // Navigation Gestion
        sidebarMatieresBtn.setOnAction(e -> naviguerVers("/fxml/GestionMatieres.fxml"));
        sidebarCoursBtn.setOnAction(e -> naviguerVers("/fxml/GestionCours.fxml"));
        
        // Navigation Autres
        sidebarReservationsBtn.setOnAction(e -> naviguerVers("/fxml/Reservation.fxml"));
        if (sidebarCoursReportesBtn != null) sidebarCoursReportesBtn.setOnAction(e -> ouvrirModalCoursReportes());
        sidebarHistoriqueBtn.setOnAction(e -> naviguerVers("/fxml/HistoriqueReservation.fxml"));
        sidebarRechercheBtn.setOnAction(e -> naviguerVers("/fxml/Recherche.fxml"));
        sidebarConflitsBtn.setOnAction(e -> naviguerVers("/fxml/GestionConflits.fxml"));
        sidebarCarteBtn.setOnAction(e -> naviguerVers("/fxml/Carte.fxml"));
    }

   
}