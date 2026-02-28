package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.TreeSet;
import java.util.LinkedHashSet;

/**
 * Tableau de bord de l'enseignant avec grille EDT.
 */
public class TableauBordEnseignantControleur extends TableauBordControleur {
    
    @FXML private Label bienvenueLabel;
    @FXML private Label matriculeLabel;
    
    @FXML private GridPane emploiTempsGrid;
    @FXML private ListView<Cours> mesCoursList;
    @FXML private TableView<Creneau> planningTableView;
    @FXML private TableColumn<Creneau, String> dateColumn;
    @FXML private TableColumn<Creneau, String> horaireColumn;
    @FXML private TableColumn<Creneau, String> coursColumn;
    @FXML private TableColumn<Creneau, String> salleColumn;
    @FXML private TableColumn<Creneau, Void> actionsColumn;
    
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Button rechercherButton;
    @FXML private Button exporterPDFButton;
    @FXML private Button reserverSalleButton;
    @FXML private Tab ongletRecherche;

    
    @FXML private Label totalCoursLabel;
    @FXML private Label prochainCoursLabel;
    
    @FXML private Button exporterExcelButton;
    @FXML private Label heuresTotalesLabel;
    @FXML private TextField rechercheCoursField;
    @FXML private Label totalCoursCountLabel;
    @FXML private DatePicker planningDateDebut;
    @FXML private DatePicker planningDateFin;
    @FXML private Button rechercherPlanningButton;
    @FXML private Button exporterPlanningPDFButton;
    @FXML private TableColumn<Creneau, String> classeColumn;
    @FXML private HBox legendContainer;
    @FXML private Button actualiserCoursButton;
    @FXML private ProgressIndicator chargementIndicator;

    // Nouveaux filtres
    @FXML private ComboBox<Classe> filtreClasseCombo;
    @FXML private ComboBox<Ufr> filtreUfrCombo;
    @FXML private Button reinitFiltresButton;
    @FXML private Label infoFiltreLabel;

    @FXML private Button aujourdhuiButton;
    @FXML private Button semaineButton;
    @FXML private TabPane tabPane;
    
    // NOUVELLES COULEURS (Indigo/Ambre)
    private static final String COULEUR_CM = "#4361EE";      // Indigo
    private static final String COULEUR_TD = "#2DC653";      // Vert
    private static final String COULEUR_TP = "#F4A261";      // Ambre
    private static final String COULEUR_ANNULE = "#E53E3E";  // Rouge
    private static final String COULEUR_DEPLACE = "#F4A261"; // Ambre
    private static final String COULEUR_HEADER = "#1A1F3A";  // Indigo foncé
    private static final String COULEUR_HEURE = "#F8F9FF";   // Lavande clair
    private static final String COULEUR_BORDURE = "#E8EBFF"; // Bordure claire
    
 // MÉTHODES SIDEBAR

    @FXML private Button sidebarEdtBtn;
    @FXML private Button sidebarCoursBtn;
    @FXML private Button sidebarPlanningBtn;
    @FXML private Button sidebarRechercheBtn;
    @FXML private Button sidebarCarteBtn;
    @FXML private Button sidebarCoursReportesBtn;
    @FXML private Button sidebarSignalementBtn;

    @FXML private StackPane contenuPrincipal;
    

    private void configurerSidebar() {
        if (sidebarEdtBtn != null) sidebarEdtBtn.setOnAction(e -> selectionnerOnglet(0));
        if (sidebarCoursBtn != null) sidebarCoursBtn.setOnAction(e -> selectionnerOnglet(1));
        if (sidebarPlanningBtn != null) sidebarPlanningBtn.setOnAction(e -> selectionnerOnglet(2));
        if (sidebarRechercheBtn != null) sidebarRechercheBtn.setOnAction(e -> selectionnerOnglet(3));
        if (sidebarCarteBtn != null) sidebarCarteBtn.setOnAction(e -> selectionnerOnglet(4));
        if (sidebarCoursReportesBtn != null) sidebarCoursReportesBtn.setOnAction(e -> ouvrirModalCoursReportes());
        if (sidebarSignalementBtn != null) sidebarSignalementBtn.setOnAction(e -> selectionnerOnglet(5));
    }

    private void selectionnerOnglet(int index) {
        if (tabPane != null && index >= 0 && index < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(index);
        }
    }
    
    /**
     * Sélectionne l'onglet Recherche et recharge son contenu.
     */
    public void selectionnerOngletRecherche() {
        if (ongletRecherche != null && tabPane != null) {
            tabPane.getSelectionModel().select(ongletRecherche);
        }
    }

    public void selectionnerOngletCarte() {
        // Chercher l'onglet Carte par son texte
        if (tabPane != null) {
            for (Tab tab : tabPane.getTabs()) {
                if ("Carte".equals(tab.getText())) {
                    tabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }
    
    @FXML
    private void handleCarte() {
        try {
            System.out.println("🔍 Navigation vers Carte depuis Enseignant");
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
    
    
    
    private PlanningService planningService;
    private CoursService coursService;
    private ReservationService reservationService;
    private ExportService exportService;
    private MatiereService matiereService;
    private ClasseService classeService;
    private SalleService salleService;
    private UfrService ufrService;
    private EmailService emailService;
    private UtilisateurService utilisateurService;
    private ConflitService conflitService;
    private Enseignant enseignant;
    
    // Données filtrées
    private List<Creneau> creneauxFiltres;   // Créneaux après application des filtres
    
    private List<Creneau> creneauxEnseignant;
    private List<Cours> tousMesCours;
    
    // Maps de cache
    private Map<Integer, Cours> coursCache = new ConcurrentHashMap<>();
    private Map<Integer, Matiere> matiereCache = new ConcurrentHashMap<>();
    private Map<Integer, Classe> classeCache = new ConcurrentHashMap<>();
    private Map<Integer, Salle> salleCache = new ConcurrentHashMap<>();
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.planningService = new PlanningService();
        this.coursService = new CoursService();
        this.reservationService = new ReservationService();
        this.exportService = new ExportService();
        this.matiereService = new MatiereService();
        this.classeService = new ClasseService();
        this.salleService = new SalleService();
        this.ufrService = new UfrService();
        this.emailService = new EmailService();
        this.utilisateurService = new UtilisateurService();
        this.conflitService = new ConflitService();
        
        this.creneauxEnseignant = new ArrayList<>();
        this.tousMesCours = new ArrayList<>();
    }
    
    @Override
    public void initialiserAvecUtilisateur(Utilisateur utilisateur, Stage stage) {
        super.initialiserAvecUtilisateur(utilisateur, stage);
        
        if (utilisateur instanceof Enseignant) {
            this.enseignant = (Enseignant) utilisateur;
        }
        
        // ✅ IMPORTANT : Mettre à jour primaryStage pour tous les contrôleurs enfants
        this.primaryStage = stage;
        
        initialiserTableauBord();
    }
    
    @Override
    protected void initialiserTableauBord() {
        configurerComposants();
        configurerTableauPlanning();
        configurerFiltresCours();
        configurerSidebar();
        afficherInfosEnseignant();
        creerLegende();
        chargerSemaine();
        chargerMesCours();

        
        if (ongletRecherche != null && ongletRecherche.getContent() != null) {
            Object controller = ongletRecherche.getContent().getProperties().get("fx:controller");
            if (controller instanceof RechercheControleur) {
                ((RechercheControleur) controller).setUtilisateurEtStage(utilisateurConnecte, primaryStage);
                System.out.println("✅ Utilisateur transmis à RechercheControleur");
            }
        }
        
    }
    
    @Override
    protected void rafraichirDonnees() {
        rechercherEmploiDuTemps();
    }
    
    
    /** Charge l'EDT du jour courant uniquement. */
    private void chargerAujourdhui() {
        LocalDate aujourd = LocalDate.now();
        if (dateDebutPicker != null) dateDebutPicker.setValue(aujourd);
        if (dateFinPicker != null) dateFinPicker.setValue(aujourd);
        rechercherEmploiDuTemps();
    }

    /** Charge l'EDT pour afficher TOUS les créneaux de l'enseignant */
    private void chargerSemaine() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate lundi  = aujourdhui.minusDays(aujourdhui.getDayOfWeek().getValue() - 1);
        LocalDate samedi = lundi.plusDays(5);

        if (dateDebutPicker != null) dateDebutPicker.setValue(lundi);
        if (dateFinPicker   != null) dateFinPicker.setValue(samedi);

        System.out.println("📅 Semaine en cours: " + lundi + " → " + samedi);
        rechercherEmploiDuTemps();
    }
    
    
    private void creerLegende() {
        if (legendContainer == null) return;
        
        legendContainer.getChildren().clear();
        legendContainer.setSpacing(20);
        legendContainer.setAlignment(Pos.CENTER_LEFT);
        
        legendContainer.getChildren().addAll(
            creerItemLegende("CM", COULEUR_CM, "Cours Magistral"),
            creerItemLegende("TD", COULEUR_TD, "Travaux Dirigés"),
            creerItemLegende("TP", COULEUR_TP, "Travaux Pratiques"),
            creerItemLegende("Annulé", COULEUR_ANNULE, "Cours annulé")
        );
    }

    private HBox creerItemLegende(String texte, String couleur, String libelle) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Rectangle rect = new Rectangle(16, 16);
        rect.setFill(Color.web(couleur, 0.3));
        rect.setStroke(Color.web(couleur));
        rect.setStrokeWidth(1);
        
        Label label = new Label(texte + " - " + libelle);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #1A1F3A;");
        
        box.getChildren().addAll(rect, label);
        return box;
    }
    
    
    private void configurerComposants() {
        LocalDate today = LocalDate.now();
        
        if (dateDebutPicker != null) dateDebutPicker.setValue(today);
        if (dateFinPicker != null) dateFinPicker.setValue(today.plusDays(6));
        if (planningDateDebut != null) planningDateDebut.setValue(today.minusDays(7));
        if (planningDateFin != null) planningDateFin.setValue(today.plusDays(7));
        
        if (rechercherButton != null) rechercherButton.setOnAction(e -> rechercherEmploiDuTemps());
        if (aujourdhuiButton != null) aujourdhuiButton.setOnAction(e -> chargerAujourdhui());
        if (semaineButton != null) semaineButton.setOnAction(e -> chargerSemaine());
        if (exporterPDFButton != null) exporterPDFButton.setOnAction(e -> exporterEmploiDuTempsPDF());
        if (exporterExcelButton != null) exporterExcelButton.setOnAction(e -> exporterEmploiDuTempsExcel());
        if (reserverSalleButton != null) reserverSalleButton.setOnAction(e -> naviguerVersReservation());
        if (actualiserCoursButton != null) actualiserCoursButton.setOnAction(e -> chargerMesCours());
        if (rechercherPlanningButton != null) rechercherPlanningButton.setOnAction(e -> rechercherPlanningDetail());
        if (exporterPlanningPDFButton != null) exporterPlanningPDFButton.setOnAction(e -> exporterPlanningPDF());

        // Configurer les filtres classe / UFR
        configurerFiltres();
    }

    private void configurerFiltres() {
        if (filtreClasseCombo != null) {
            filtreClasseCombo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Classe c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty || c == null ? null : c.getIntitule());
                }
            });
            filtreClasseCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Classe c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty || c == null ? "Toutes les classes" : c.getIntitule());
                }
            });
            filtreClasseCombo.valueProperty().addListener((obs, o, n) -> appliquerFiltres());
        }

        if (filtreUfrCombo != null) {
            filtreUfrCombo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Ufr u, boolean empty) {
                    super.updateItem(u, empty);
                    setText(empty || u == null ? null : u.getNom());
                }
            });
            filtreUfrCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Ufr u, boolean empty) {
                    super.updateItem(u, empty);
                    setText(empty || u == null ? "Toutes les UFR" : u.getNom());
                }
            });
            filtreUfrCombo.valueProperty().addListener((obs, o, n) -> appliquerFiltres());
        }

        if (reinitFiltresButton != null) {
            reinitFiltresButton.setOnAction(e -> {
                if (filtreClasseCombo != null) filtreClasseCombo.setValue(null);
                if (filtreUfrCombo != null) filtreUfrCombo.setValue(null);
                creneauxFiltres = creneauxEnseignant;
                afficherEmploiTemps();
                if (infoFiltreLabel != null) infoFiltreLabel.setText("");
            });
        }

        chargerDonneesFiltres();
    }

    private void chargerDonneesFiltres() {
        Task<Void> task = new Task<>() {
            List<Classe> classes = new ArrayList<>();
            List<Ufr> ufrs = new ArrayList<>();

            @Override
            protected Void call() throws SQLException {
                if (enseignant != null) {
                    // ✅ Récupérer les classes UNIQUEMENT à partir des cours de l'enseignant
                    List<Cours> mesCours = coursService.listerParEnseignant(enseignant.getId());
                    Set<Integer> classeIds = new HashSet<>();
                    for (Cours c : mesCours) {
                        classeIds.add(c.getClasseId());
                        System.out.println("   Classe trouvée pour ce cours: " + c.getClasseId());
                    }
                    for (int id : classeIds) {
                        Classe cl = classeService.trouverParId(id);
                        if (cl != null && cl.isEstActive()) {
                            classes.add(cl);
                            System.out.println("   ✅ Classe ajoutée au filtre: " + cl.getIntitule());
                        }
                    }
                    
                    // ✅ Récupérer les UFR à partir des classes
                    Set<Integer> ufrIds = new HashSet<>();
                    for (Cours c : mesCours) {
                        Classe cl = classeService.trouverParId(c.getClasseId());
                        if (cl != null) {
                            ufrIds.add(cl.getUfrId());
                            System.out.println("   UFR trouvée: " + cl.getUfrId());
                        }
                    }
                    for (int id : ufrIds) {
                        Ufr ufr = ufrService.trouverParId(id);
                        if (ufr != null) {
                            ufrs.add(ufr);
                            System.out.println("   ✅ UFR ajoutée au filtre: " + ufr.getNom());
                        }
                    }
                }
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // ✅ Configurer ComboBox des classes
                    if (filtreClasseCombo != null) {
                        filtreClasseCombo.getItems().clear();
                        filtreClasseCombo.getItems().add(null); // Option "Toutes les classes"
                        filtreClasseCombo.getItems().addAll(classes);
                        
                        System.out.println("📋 Classes disponibles dans le filtre: " + classes.size());
                    }
                    
                    // ✅ Configurer ComboBox des UFR
                    if (filtreUfrCombo != null) {
                        filtreUfrCombo.getItems().clear();
                        filtreUfrCombo.getItems().add(null); // Option "Toutes les UFR"
                        filtreUfrCombo.getItems().addAll(ufrs);
                        
                        System.out.println("📋 UFR disponibles dans le filtre: " + ufrs.size());
                    }
                });
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement données filtres", getException());
            }
        };
        new Thread(task).start();
    }

    private void appliquerFiltres() {
        if (creneauxEnseignant == null) return;
        creneauxFiltres = new ArrayList<>(creneauxEnseignant);

        Classe classeFiltre = filtreClasseCombo != null ? filtreClasseCombo.getValue() : null;
        Ufr ufrFiltre = filtreUfrCombo != null ? filtreUfrCombo.getValue() : null;

        System.out.println("🔍 Application des filtres - Classe: " + (classeFiltre != null ? classeFiltre.getIntitule() : "Toutes"));
        System.out.println("   UFR: " + (ufrFiltre != null ? ufrFiltre.getNom() : "Toutes"));

        if (classeFiltre != null) {
            creneauxFiltres = creneauxFiltres.stream().filter(c -> {
                Cours cours = getCoursFromCache(c.getCoursId());
                boolean match = cours != null && cours.getClasseId() == classeFiltre.getId();
                if (!match) {
                    System.out.println("   ✗ Créneau filtré (classe): " + c.getJour());
                }
                return match;
            }).collect(Collectors.toList());
        }

        if (ufrFiltre != null) {
            creneauxFiltres = creneauxFiltres.stream().filter(c -> {
                Cours cours = getCoursFromCache(c.getCoursId());
                if (cours == null) return false;
                Classe cl = getClasseFromCache(cours.getClasseId());
                boolean match = cl != null && cl.getUfrId() == ufrFiltre.getId();
                if (!match) {
                    System.out.println("   ✗ Créneau filtré (UFR): " + c.getJour());
                }
                return match;
            }).collect(Collectors.toList());
        }

        System.out.println("   Créneaux après filtres: " + creneauxFiltres.size());

        if (infoFiltreLabel != null) {
            if (classeFiltre != null || ufrFiltre != null) {
                String info = "Filtre actif : ";
                if (classeFiltre != null) info += classeFiltre.getIntitule() + " ";
                if (ufrFiltre != null) info += "| UFR: " + ufrFiltre.getNom();
                info += " (" + creneauxFiltres.size() + " créneau(x))";
                infoFiltreLabel.setText(info);
            } else {
                infoFiltreLabel.setText("");
            }
        }

        // ✅ Sauvegarder les créneaux originaux et afficher les filtrés
        List<Creneau> sauvegardeCreneaux = creneauxEnseignant;
        creneauxEnseignant = creneauxFiltres;
        afficherEmploiTemps();
        creneauxEnseignant = sauvegardeCreneaux;
    }
    
    private void configurerTableauPlanning() {
        if (dateColumn == null) return;
        
        dateColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    if (cellData.getValue() == null) return "";
                    return formaterDate(cellData.getValue().getJour());
                }
            )
        );
        
        horaireColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    if (cellData.getValue() == null) return "";
                    return cellData.getValue().getHeureDebut() + " - " + 
                           cellData.getValue().getHeureFin();
                }
            )
        );
        
        coursColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    Creneau creneau = cellData.getValue();
                    if (creneau == null) return "";
                    
                    Cours cours = getCoursFromCache(creneau.getCoursId());
                    if (cours == null) return "Cours #" + creneau.getCoursId();
                    
                    Matiere m = getMatiereFromCache(cours.getMatiereId());
                    String nomMatiere = m != null ? m.getNom() : "Matière " + cours.getMatiereId();
                    
                    return cours.getTypeCours() + " - " + nomMatiere;
                }
            )
        );
        
        if (classeColumn != null) {
            classeColumn.setCellValueFactory(cellData -> 
                javafx.beans.binding.Bindings.createStringBinding(
                    () -> {
                        Creneau creneau = cellData.getValue();
                        if (creneau == null) return "";
                        
                        Cours cours = getCoursFromCache(creneau.getCoursId());
                        if (cours == null) return "N/A";
                        
                        Classe cl = getClasseFromCache(cours.getClasseId());
                        return cl != null ? cl.getIntitule() : "Classe " + cours.getClasseId();
                    }
                )
            );
        }
        
        salleColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    Creneau creneau = cellData.getValue();
                    if (creneau == null) return "";
                    
                    Integer salleId = creneau.getSalleId();
                    if (salleId == null) return "Non assignée";
                    
                    Salle s = getSalleFromCache(salleId);
                    return s != null ? s.getNumero() : "Salle " + salleId;
                }
            )
        );
        
        actionsColumn.setCellFactory(param -> new TableCell<Creneau, Void>() {
            private final Button detailsBtn = new Button("👁️ Détails");
            private final Button annulerBtn = new Button("❌ Annuler");
            private final Button deplacerBtn = new Button("📅 Déplacer");
            
            {
                detailsBtn.setStyle("-fx-background-color: #4361EE; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 5;");
                annulerBtn.setStyle("-fx-background-color: #E53E3E; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 5;");
                deplacerBtn.setStyle("-fx-background-color: #F4A261; -fx-text-fill: #1A1F3A; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 5;");
                
                detailsBtn.setOnAction(event -> {
                    Creneau creneau = getTableView().getItems().get(getIndex());
                    afficherDetailsCours(creneau);
                });
                
                annulerBtn.setOnAction(event -> {
                    Creneau creneau = getTableView().getItems().get(getIndex());
                    annulerCours(creneau);
                });
                
                deplacerBtn.setOnAction(event -> {
                    Creneau creneau = getTableView().getItems().get(getIndex());
                    deplacerCours(creneau);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().isEmpty() || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, detailsBtn, annulerBtn, deplacerBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        
        planningTableView.setPrefHeight(350);
    }
    
    // GRILLE EDT DYNAMIQUE

    // Config de grille déduite des créneaux
    private final int grilleHeureDebut = 8;
    private final int grilleHeureFin = 18;
    private final int grilleEcartHeures = 1;
    private final List<String> grilleJours = Arrays.asList("Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi");
    
    /**
     * Affiche la grille EDT.
     */
    private void afficherEmploiTemps() {
        if (emploiTempsGrid == null) {
            System.out.println("❌ emploiTempsGrid est null");
            return;
        }

        emploiTempsGrid.getChildren().clear();
        emploiTempsGrid.getColumnConstraints().clear();
        emploiTempsGrid.getRowConstraints().clear();

        // HEURES FIXES (8h à 18h, écart 1h)
        List<String> heures = new ArrayList<>();
        for (int h = grilleHeureDebut; h <= grilleHeureFin; h += grilleEcartHeures) {
            heures.add(String.format("%02d:00", h));
        }
        int nbHeures = heures.size();
        
        LocalDate debutPeriode = dateDebutPicker.getValue();
        LocalDate finPeriode = dateFinPicker.getValue();
        
        if (debutPeriode == null) debutPeriode = LocalDate.now();
        if (finPeriode == null) finPeriode = debutPeriode.plusDays(6);
        
        List<LocalDate> datesJours = new ArrayList<>();
        LocalDate current = debutPeriode;
        while (!current.isAfter(finPeriode)) {
            if (current.getDayOfWeek().getValue() != 7) {
                datesJours.add(current);
            }
            current = current.plusDays(1);
        }
        
        int nbJours = datesJours.size();
        
        List<Creneau> creneauxPeriode = new ArrayList<>();
        if (creneauxEnseignant != null) {
            for (Creneau c : creneauxEnseignant) {
                try {
                    LocalDate jourCreneau = LocalDate.parse(c.getJour());
                    if (!jourCreneau.isBefore(debutPeriode) && !jourCreneau.isAfter(finPeriode)) {
                        creneauxPeriode.add(c);
                    }
                } catch (Exception e) { }
            }
        }
        
        System.out.println("📅 Affichage EDT Professeur");
        System.out.println("   Période sélectionnée: " + debutPeriode + " → " + finPeriode);
        System.out.println("   Jours à afficher: " + nbJours);
        System.out.println("   Jours: " + datesJours);
        System.out.println("   Heures: " + nbHeures);
        System.out.println("   Créneaux dans la période: " + creneauxPeriode.size());

        ColumnConstraints ccHeures = new ColumnConstraints(75);
        ccHeures.setHgrow(Priority.NEVER);
        emploiTempsGrid.getColumnConstraints().add(ccHeures);
        
        for (int j = 0; j < nbJours; j++) {
            ColumnConstraints ccJour = new ColumnConstraints(150);
            ccJour.setHgrow(Priority.ALWAYS);
            emploiTempsGrid.getColumnConstraints().add(ccJour);
        }
        
        RowConstraints rcEntete = new RowConstraints(40);
        emploiTempsGrid.getRowConstraints().add(rcEntete);
        
        for (int h = 0; h < nbHeures; h++) {
            RowConstraints rc = new RowConstraints(55);
            rc.setVgrow(Priority.SOMETIMES);
            emploiTempsGrid.getRowConstraints().add(rc);
        }
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM", java.util.Locale.FRENCH);
        
        // COIN VIDE
        Label coin = new Label();
        coin.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        coin.setStyle("-fx-background-color: " + COULEUR_HEADER + ";");
        emploiTempsGrid.add(coin, 0, 0);
        
        // En-têtes des jours de la semaine (Lundi à Vendredi/Samedi)
        for (int j = 0; j < nbJours; j++) {
            LocalDate jour = datesJours.get(j);
            boolean estAujd = jour.equals(LocalDate.now());
            
            String nomJour = "";
            switch (jour.getDayOfWeek().getValue()) {
                case 1: nomJour = "Lundi"; break;
                case 2: nomJour = "Mardi"; break;
                case 3: nomJour = "Mercredi"; break;
                case 4: nomJour = "Jeudi"; break;
                case 5: nomJour = "Vendredi"; break;
                case 6: nomJour = "Samedi"; break;
                default: nomJour = "Dimanche";
            }
            
            Label entete = new Label(nomJour + " " + jour.format(fmt));
            entete.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            entete.setAlignment(Pos.CENTER);
            entete.setStyle("-fx-background-color: " + (estAujd ? "#0D47A1" : COULEUR_HEADER) + "; " +
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
            emploiTempsGrid.add(entete, j + 1, 0);
        }
        
        for (int h = 0; h < nbHeures; h++) {
            // Colonne heure
            Label lblHeure = new Label(heures.get(h));
            lblHeure.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            lblHeure.setAlignment(Pos.CENTER);
            lblHeure.setStyle("-fx-background-color: " + COULEUR_HEURE + "; -fx-text-fill: #1A1F3A; " +
                              "-fx-font-weight: bold; -fx-font-size: 11px; " +
                              "-fx-border-color: " + COULEUR_BORDURE + "; -fx-border-width: 0 1 1 0;");
            emploiTempsGrid.add(lblHeure, 0, h + 1);
            
            for (int j = 0; j < nbJours; j++) {
                StackPane cellule = new StackPane();
                cellule.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cellule.setStyle("-fx-background-color: white; -fx-border-color: " + COULEUR_BORDURE + "; -fx-border-width: 0 1 1 0;");
                emploiTempsGrid.add(cellule, j + 1, h + 1);
            }
        }
        
        if (!creneauxPeriode.isEmpty()) {
            System.out.println("📌 Placement des créneaux:");
            
            for (Creneau c : creneauxPeriode) {
                try {
                    LocalDate jourCreneau = LocalDate.parse(c.getJour());
                    System.out.println("   - Créneau: " + jourCreneau + " " + c.getHeureDebut() + "-" + c.getHeureFin());
                    
                    // Trouver l'index du jour dans la liste des dates de la période
                    int colIdx = -1;
                    for (int i = 0; i < datesJours.size(); i++) {
                        if (datesJours.get(i).equals(jourCreneau)) {
                            colIdx = i;
                            break;
                        }
                    }
                    if (colIdx < 0) {
                        System.out.println("      ⚠️ Jour non trouvé dans la période");
                        continue;
                    }
                    System.out.println("      Jour index: " + colIdx);
                    
                    // Calcul de la ligne de début
                    int hd = Integer.parseInt(c.getHeureDebut().split(":")[0]);
                    int rowIdx = hd - grilleHeureDebut;
                    System.out.println("      Heure début: " + hd + " → rowIdx: " + rowIdx);
                    
                    if (rowIdx < 0 || rowIdx >= nbHeures) {
                        System.out.println("      ⚠️ Heure hors grille");
                        continue;
                    }
                    
                    // Calcul du nombre de cases
                    int hf = Integer.parseInt(c.getHeureFin().split(":")[0]);
                    int duree = hf - hd;
                    int rowSpan = duree + 1;
                    if (rowIdx + rowSpan > nbHeures) {
                        rowSpan = nbHeures - rowIdx;
                    }
                    System.out.println("      Heure fin: " + hf + " → durée: " + duree + "h, rowSpan: " + rowSpan);
                    
                    // Récupérer la cellule
                    int colPos = colIdx + 1;
                    int rowPos = rowIdx + 1;
                    System.out.println("      Position finale: col=" + colPos + ", row=" + rowPos);
                    
                    StackPane cellule = trouverCellule(colPos, rowPos);
                    if (cellule == null) {
                        System.out.println("      ❌ Cellule non trouvée !");
                        continue;
                    }
                    
                    // Appliquer le rowSpan
                    GridPane.setRowSpan(cellule, rowSpan);
                    
                    // Masquer les cellules en dessous
                    for (int d = 1; d < rowSpan; d++) {
                        StackPane cell2 = trouverCellule(colPos, rowPos + d);
                        if (cell2 != null) {
                            cell2.setVisible(false);
                            cell2.setManaged(false);
                        }
                    }
                    
                    // Remplir la cellule
                    remplirCelluleCreneau(cellule, c, rowSpan);
                    
                } catch (Exception e) {
                    System.err.println("❌ Erreur placement créneau: " + e.getMessage());
                }
            }
        } else {
            System.out.println("⚠️ Aucun créneau à placer dans cette période");
        }
        

    }
    
    
    private int calculerDureeMinutes(String heureDebut, String heureFin) {
        try {
            String[] debut = heureDebut.split(":");
            String[] fin = heureFin.split(":");
            int debutMinutes = Integer.parseInt(debut[0]) * 60 + Integer.parseInt(debut[1]);
            int finMinutes = Integer.parseInt(fin[0]) * 60 + Integer.parseInt(fin[1]);
            return finMinutes - debutMinutes;
        } catch (Exception e) {
            return 60;
        }
    }
    
    private void remplirCelluleCreneau(StackPane cellule, Creneau creneau, int rowSpan) {
        Cours cours = getCoursFromCache(creneau.getCoursId());
        String typeCours = cours != null ? cours.getTypeCours() : "?";
        String matiereNom = "";
        String classeNom = "";
        String salleNom = "";

        if (cours != null) {
            Matiere m = getMatiereFromCache(cours.getMatiereId());
            matiereNom = m != null ? m.getNom() : "Matière " + cours.getMatiereId();
            Classe cl = getClasseFromCache(cours.getClasseId());
            classeNom = cl != null ? cl.getIntitule() : "";
        }
        if (creneau.getSalleId() != null) {
            Salle s = getSalleFromCache(creneau.getSalleId());
            salleNom = s != null ? s.getNumero() : "Salle " + creneau.getSalleId();
        }

        // ✅ NOUVELLES COULEURS
        String couleur = COULEUR_CM;
        if ("TD".equals(typeCours)) couleur = COULEUR_TD;
        else if ("TP".equals(typeCours)) couleur = COULEUR_TP;
        if ("annule".equals(creneau.getStatut())) couleur = COULEUR_ANNULE;
        if ("deplace".equals(creneau.getStatut())) couleur = COULEUR_DEPLACE;

        cellule.setStyle("-fx-background-color: " + couleur + "22; " +
                         "-fx-border-color: " + couleur + "; -fx-border-width: 0 0 0 4;");

        // Ajuster le padding selon le nombre de cases
        int paddingTop = rowSpan >= 3 ? 8 : 12;
        
        VBox content = new VBox(3);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(paddingTop, 6, paddingTop, 8));
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        int fontSize = rowSpan >= 3 ? 11 : 13;

        // Ligne 1 : Type + Matière
        HBox ligne1 = new HBox(5);
        ligne1.setAlignment(Pos.CENTER);
        
        Label typeLbl = new Label("[" + typeCours + "]");
        typeLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";");
        
        Label matLbl = new Label(matiereNom);
        matLbl.setStyle("-fx-font-size:" + (fontSize+1) + "px;-fx-font-weight:bold;-fx-text-fill:#1A202C;");
        matLbl.setWrapText(true);
        
        ligne1.getChildren().addAll(typeLbl, matLbl);
        content.getChildren().add(ligne1);
        
        // Ligne 2 : Salle
        Label salleLbl = new Label("🏫 " + (salleNom.isEmpty() ? "?" : salleNom));
        salleLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-text-fill:#4361EE;");
        content.getChildren().add(salleLbl);
        
        // Ligne 3 : Classe
        Label classeLbl = new Label("👥 " + classeNom);
        classeLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-text-fill:#7B83C4;");
        content.getChildren().add(classeLbl);
        

        // Ligne 4 : Groupe (TD/TP uniquement, non vide, pas "Tous")
        if (cours != null) {
            String groupes = cours.getGroupes();
            String typeCours2 = cours.getTypeCours();
            if (("TD".equals(typeCours2) || "TP".equals(typeCours2))
                    && groupes != null && !groupes.isEmpty() && !"Tous".equalsIgnoreCase(groupes)) {
                Label groupeLbl = new Label("\uD83D\uDCCC " + groupes);
                groupeLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-font-weight:bold;-fx-text-fill:#F4A261;");
                content.getChildren().add(groupeLbl);
            }
        }
        // Ligne 4 : Horaire (si assez de place)
        if (rowSpan >= 3) {
            Label horaireLbl = new Label("⏰ " + creneau.getHeureDebut() + " → " + creneau.getHeureFin());
            horaireLbl.setStyle("-fx-font-size:" + (fontSize-1) + "px;-fx-text-fill:#7B83C4;");
            content.getChildren().add(horaireLbl);
        }

        if ("annule".equals(creneau.getStatut())) {
            Label annuleLbl = new Label("❌ ANNULÉ");
            annuleLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-font-weight:bold;-fx-text-fill:" + COULEUR_ANNULE + ";");
            content.getChildren().add(annuleLbl);
        } else if ("deplace".equals(creneau.getStatut())) {
            Label depLbl = new Label("📍 DÉPLACÉ");
            depLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-font-weight:bold;-fx-text-fill:#F4A261;");
            content.getChildren().add(depLbl);
        }

        cellule.getChildren().add(content);

        cellule.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                afficherMenuContextuelCours(creneau, e);
            } else if (e.getClickCount() == 2) {
                afficherDetailsCours(creneau);
            }
        });

        Tooltip tip = new Tooltip(matiereNom + "\n" + classeNom +
                "\n" + creneau.getHeureDebut() + " - " + creneau.getHeureFin() +
                "\nSalle: " + salleNom);
        Tooltip.install(cellule, tip);
    }

    private StackPane trouverCellule(int col, int row) {
        for (javafx.scene.Node node : emploiTempsGrid.getChildren()) {
            Integer c = GridPane.getColumnIndex(node);
            Integer r = GridPane.getRowIndex(node);
            if (c != null && r != null && c == col && r == row && node instanceof StackPane) {
                return (StackPane) node;
            }
        }
        return null;
    }

    private void afficherCreneauDansGrille(Creneau creneau, StackPane cellule, int rowSpan) {
        Cours cours = getCoursFromCache(creneau.getCoursId());
        String typeCours = cours != null ? cours.getTypeCours() : "?";
        String matiereNom = "";
        String classeNom = "";
        String salleNom = "";

        if (cours != null) {
            Matiere m = getMatiereFromCache(cours.getMatiereId());
            matiereNom = m != null ? m.getNom() : "Matière " + cours.getMatiereId();
            Classe cl = getClasseFromCache(cours.getClasseId());
            classeNom = cl != null ? cl.getIntitule() : "";
        }
        if (creneau.getSalleId() != null) {
            Salle s = getSalleFromCache(creneau.getSalleId());
            salleNom = s != null ? s.getNumero() : "Salle " + creneau.getSalleId();
        }

        // Récupérer la position
        Integer rowCellule = GridPane.getRowIndex(cellule);
        Integer colCellule = GridPane.getColumnIndex(cellule);
        
        if (rowCellule == null || colCellule == null) {
            return;
        }
        
        // ✅ IMPORTANT : Retirer la cellule et la ré-ajouter avec le bon rowSpan
        emploiTempsGrid.getChildren().remove(cellule);
        GridPane.setRowSpan(cellule, rowSpan);
        GridPane.setColumnIndex(cellule, colCellule);
        GridPane.setRowIndex(cellule, rowCellule);
        emploiTempsGrid.add(cellule, colCellule, rowCellule);
        
        // Masquer les cellules en dessous
        for (int dd = 1; dd < rowSpan; dd++) {
            StackPane cell2 = trouverCellule(colCellule, rowCellule + dd);
            if (cell2 != null) {
                cell2.setVisible(false);
                cell2.setManaged(false);
            }
        }

        // ✅ NOUVELLES COULEURS
        String couleur = COULEUR_CM;
        if ("TD".equals(typeCours)) couleur = COULEUR_TD;
        else if ("TP".equals(typeCours)) couleur = COULEUR_TP;
        if ("annule".equals(creneau.getStatut())) couleur = COULEUR_ANNULE;
        if ("deplace".equals(creneau.getStatut())) couleur = COULEUR_DEPLACE;

        cellule.getChildren().clear();
        cellule.setStyle("-fx-background-color: " + couleur + "22; " +
                         "-fx-border-color: " + couleur + "; -fx-border-width: 0 0 0 4;");

        // Ajuster le contenu selon le nombre de cases
        int paddingVertical = rowSpan >= 3 ? 8 : 12;
        
        VBox content = new VBox(3);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(paddingVertical, 6, paddingVertical, 8));
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        int fontSize = rowSpan >= 3 ? 11 : 13;

        // Ligne 1 : Type + Matière
        HBox ligne1 = new HBox(5);
        ligne1.setAlignment(Pos.CENTER);
        
        Label typeLbl = new Label("[" + typeCours + "]");
        typeLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";");
        
        Label matLbl = new Label(matiereNom);
        matLbl.setStyle("-fx-font-size:" + (fontSize+1) + "px;-fx-font-weight:bold;-fx-text-fill:#1A202C;");
        matLbl.setWrapText(true);
        
        ligne1.getChildren().addAll(typeLbl, matLbl);
        content.getChildren().add(ligne1);
        
        // Ligne 2 : Salle
        Label salleLbl = new Label("🏫 " + (salleNom.isEmpty() ? "?" : salleNom));
        salleLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-text-fill:#4361EE;");
        content.getChildren().add(salleLbl);
        
        // Ligne 3 : Classe
        Label classeLbl = new Label("👥 " + classeNom);
        classeLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-text-fill:#7B83C4;");
        content.getChildren().add(classeLbl);
        

        // Ligne 4 : Groupe (TD/TP uniquement, non vide, pas "Tous")
        if (cours != null) {
            String groupes = cours.getGroupes();
            String typeCours2 = cours.getTypeCours();
            if (("TD".equals(typeCours2) || "TP".equals(typeCours2))
                    && groupes != null && !groupes.isEmpty() && !"Tous".equalsIgnoreCase(groupes)) {
                Label groupeLbl = new Label("\uD83D\uDCCC " + groupes);
                groupeLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-font-weight:bold;-fx-text-fill:#F4A261;");
                content.getChildren().add(groupeLbl);
            }
        }
        // Ligne 4 : Horaire (si assez de place)
        if (rowSpan >= 3) {
            Label horaireLbl = new Label("⏰ " + creneau.getHeureDebut() + " → " + creneau.getHeureFin());
            horaireLbl.setStyle("-fx-font-size:" + (fontSize-1) + "px;-fx-text-fill:#7B83C4;");
            content.getChildren().add(horaireLbl);
        }

        if ("annule".equals(creneau.getStatut())) {
            Label annuleLbl = new Label("❌ ANNULÉ");
            annuleLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-font-weight:bold;-fx-text-fill:" + COULEUR_ANNULE + ";");
            content.getChildren().add(annuleLbl);
        } else if ("deplace".equals(creneau.getStatut())) {
            Label depLbl = new Label("📍 DÉPLACÉ");
            depLbl.setStyle("-fx-font-size:" + fontSize + "px;-fx-font-weight:bold;-fx-text-fill:#F4A261;");
            content.getChildren().add(depLbl);
        }

        cellule.getChildren().add(content);

        // ✅ AJOUT DU MENU CONTEXTUEL (clic droit)
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem itemDetails = new MenuItem("👁️ Voir les détails");
        MenuItem itemAnnuler = new MenuItem("❌ Annuler ce cours");
        MenuItem itemDeplacer = new MenuItem("📅 Déplacer ce cours");
        MenuItem itemEmail = new MenuItem("📧 Envoyer un email aux étudiants");
        MenuItem itemConflits = new MenuItem("⚠️ Vérifier les conflits");
        
        itemDetails.setOnAction(e -> afficherDetailsCours(creneau));
        itemAnnuler.setOnAction(e -> annulerCours(creneau));
        itemDeplacer.setOnAction(e -> ouvrirDialogueDeplacement(creneau));
        itemEmail.setOnAction(e -> envoyerEmailAuxEtudiants(creneau));
        itemConflits.setOnAction(e -> verifierConflitsEtudiants(creneau));
        
        contextMenu.getItems().addAll(itemDetails, itemAnnuler, itemDeplacer, itemEmail, itemConflits);
        
        // Clic droit pour ouvrir le menu
        cellule.setOnContextMenuRequested(e -> contextMenu.show(cellule, e.getScreenX(), e.getScreenY()));
        
        // Double-clic pour voir les détails
        cellule.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                afficherDetailsCours(creneau);
            }
        });

        Tooltip tip = new Tooltip(matiereNom + "\n" + classeNom +
                "\n" + creneau.getHeureDebut() + " - " + creneau.getHeureFin() +
                "\nSalle: " + salleNom);
        Tooltip.install(cellule, tip);
    }
    
    private void envoyerEmailAuxEtudiants(Creneau creneau) {
        Cours cours = getCoursFromCache(creneau.getCoursId());
        if (cours == null) {
            afficherErreur("Impossible de trouver les informations du cours");
            return;
        }
        
        Classe classe = getClasseFromCache(cours.getClasseId());
        if (classe == null) {
            afficherErreur("Impossible de trouver la classe");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Envoyer un email");
        dialog.setHeaderText("Envoyer un email aux étudiants de " + classe.getIntitule());
        dialog.setContentText("Message :");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String message = result.get().trim();
            
            new Thread(() -> {
                try {
                    List<Utilisateur> etudiants = utilisateurService.listerParRole("etudiant");
                    int compteur = 0;
                    
                    for (Utilisateur u : etudiants) {
                        if (u instanceof Etudiant) {
                            Etudiant e = (Etudiant) u;
                            if (e.getClasseId() == cours.getClasseId() && e.isEstValide()) {
                                String sujet = "📢 Message de votre professeur - " + classe.getIntitule();
                                String contenu = "Bonjour " + e.getPrenom() + " " + e.getNom() + ",\n\n" +
                                    "Votre professeur vous a envoyé un message :\n\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                    "📚 Matière : " + getNomMatiere(cours.getMatiereId()) + "\n" +
                                    "📅 Date : " + formaterDate(creneau.getJour()) + "\n" +
                                    "⏰ Horaire : " + creneau.getHeureDebut() + " - " + creneau.getHeureFin() + "\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                                    "📝 Message :\n" + message + "\n\n" +
                                    "Cordialement,\n" + enseignant.getPrenom() + " " + enseignant.getNom();
                                
                                emailService.envoyerEmail(e.getEmail(), sujet, contenu);
                                compteur++;
                            }
                        }
                    }
                    
                    final int nbEnvoyes = compteur;
                    Platform.runLater(() -> {
                        afficherNotification("Email envoyé", "Message envoyé à " + nbEnvoyes + " étudiants");
                    });
                    
                } catch (SQLException e) {
                    logger.error("Erreur envoi emails", e);
                    Platform.runLater(() -> afficherErreur("Erreur lors de l'envoi des emails"));
                }
            }).start();
        }
    }
    
    private void verifierConflitsEtudiants(Creneau creneau) {
        Cours cours = getCoursFromCache(creneau.getCoursId());
        if (cours == null) {
            afficherErreur("Impossible de trouver les informations du cours");
            return;
        }
        
        Classe classe = getClasseFromCache(cours.getClasseId());
        if (classe == null) {
            afficherErreur("Impossible de trouver la classe");
            return;
        }
        
        // Vérifier si les étudiants ont d'autres cours au même moment
        List<String> conflits = new ArrayList<>();
        
        try {
            // Récupérer tous les créneaux du même jour
            List<Creneau> creneauxJour = planningService.getCreneauxParJour(creneau.getJour());
            
            // Vérifier pour chaque étudiant (simulation)
            // On vérifie s'il y a d'autres cours pour la même classe
            for (Creneau c : creneauxJour) {
                if (c.getId() == creneau.getId()) continue;
                if ("annule".equals(c.getStatut())) continue;
                
                Cours autreCours = getCoursFromCache(c.getCoursId());
                if (autreCours != null && autreCours.getClasseId() == cours.getClasseId()) {
                    // Vérifier si les horaires se chevauchent
                    if (seChevauchent(creneau.getHeureDebut(), creneau.getHeureFin(),
                                      c.getHeureDebut(), c.getHeureFin())) {
                        String nomAutreMatiere = getNomMatiere(autreCours.getMatiereId());
                        conflits.add("❌ " + nomAutreMatiere + " (" + c.getHeureDebut() + " - " + c.getHeureFin() + ")");
                    }
                }
            }
            
            if (conflits.isEmpty()) {
                afficherNotification("Vérification", "✅ Aucun conflit détecté pour les étudiants de " + classe.getIntitule());
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Conflits détectés");
                alert.setHeaderText(conflits.size() + " conflit(s) pour la classe " + classe.getIntitule());
                alert.setContentText("Les étudiants ont d'autres cours au même moment :\n\n" + String.join("\n", conflits));
                alert.show();
            }
            
        } catch (SQLException e) {
            logger.error("Erreur vérification conflits", e);
            afficherErreur("Erreur lors de la vérification des conflits");
        }
    }

    private boolean seChevauchent(String d1, String f1, String d2, String f2) {
        try {
            int id1 = heureEnInt(d1);
            int if1 = heureEnInt(f1);
            int id2 = heureEnInt(d2);
            int if2 = heureEnInt(f2);
            return id1 < if2 && id2 < if1;
        } catch (Exception e) {
            return false;
        }
    }

    private int heureEnInt(String heure) {
        String[] p = heure.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }
    
    private void ouvrirDialogueDeplacement(Creneau creneau) {
        Cours cours = getCoursFromCache(creneau.getCoursId());
        if (cours == null) {
            afficherErreur("Impossible de trouver les informations du cours");
            return;
        }
        
        Alert choix = new Alert(Alert.AlertType.CONFIRMATION);
        choix.setTitle("Déplacer le cours");
        choix.setHeaderText("Que voulez-vous modifier ?");
        choix.setContentText("Choisissez une option :");
        
        ButtonType btnSalle = new ButtonType("🏫 Changer de salle");
        ButtonType btnHoraire = new ButtonType("📅 Changer de jour/heure");
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        choix.getButtonTypes().setAll(btnSalle, btnHoraire, btnAnnuler);
        
        choix.showAndWait().ifPresent(reponse -> {
            if (reponse == btnSalle) {
                deplacerCoursChangerSalle(creneau, cours, getNomMatiere(cours.getMatiereId()));
            } else if (reponse == btnHoraire) {
                deplacerCoursChangerJour(creneau, cours, getNomMatiere(cours.getMatiereId()));
            }
        });
    }

    /**
     * Menu contextuel clic droit sur un cours dans la grille.
     */
    private void afficherMenuContextuelCours(Creneau creneau, javafx.scene.input.MouseEvent e) {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();

        javafx.scene.control.MenuItem itemDetails = new javafx.scene.control.MenuItem("👁️ Voir les détails");
        itemDetails.setOnAction(ae -> afficherDetailsCours(creneau));

        javafx.scene.control.MenuItem itemAnnuler = new javafx.scene.control.MenuItem("❌ Annuler ce cours");
        itemAnnuler.setOnAction(ae -> annulerCours(creneau));

        javafx.scene.control.MenuItem itemDeplacer = new javafx.scene.control.MenuItem("📅 Déplacer ce cours");
        itemDeplacer.setOnAction(ae -> deplacerCours(creneau));

        menu.getItems().addAll(itemDetails, new SeparatorMenuItem(), itemAnnuler, itemDeplacer);
        menu.show((javafx.scene.Node) e.getSource(), e.getScreenX(), e.getScreenY());
    }
    
    // ACTIONS SUR LES COURS
    
    private void afficherDetailsCours(Creneau creneau) {
        if (creneau == null) return;
        
        Cours cours = getCoursFromCache(creneau.getCoursId());
        if (cours == null) {
            afficherErreur("Impossible de trouver les informations du cours");
            return;
        }
        
        Matiere matiere = getMatiereFromCache(cours.getMatiereId());
        String matiereNom = matiere != null ? matiere.getNom() : "Matière inconnue";
        
        Classe classe = getClasseFromCache(cours.getClasseId());
        String classeNom = classe != null ? classe.getIntitule() : "Classe inconnue";
        
        Salle salle = creneau.getSalleId() != null ? getSalleFromCache(creneau.getSalleId()) : null;
        String salleNom = salle != null ? salle.getNumero() : "Non assignée";
        
        // ✅ Création de la fenêtre de dialogue
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du cours");
        alert.setHeaderText(matiereNom + " (" + cours.getTypeCours() + ")");
        
        // Contenu principal
        StringBuilder sb = new StringBuilder();
        sb.append("📅 Date : ").append(formaterDate(creneau.getJour())).append("\n");
        sb.append("⏰ Horaire : ").append(creneau.getHeureDebut())
          .append(" - ").append(creneau.getHeureFin()).append("\n");
        sb.append("👥 Classe : ").append(classeNom).append("\n");
        sb.append("🏫 Salle : ").append(salleNom).append("\n");
        sb.append("👨‍🏫 Enseignant : ").append(enseignant.getPrenom()).append(" ").append(enseignant.getNom()).append("\n");
        
        if ("annule".equals(creneau.getStatut())) {
            sb.append("\n❌ STATUT : ANNULÉ\n");
            if (creneau.getMotifAnnulation() != null && !creneau.getMotifAnnulation().isEmpty()) {
                sb.append("📝 Motif : ").append(creneau.getMotifAnnulation());
            }
        } else if ("deplace".equals(creneau.getStatut())) {
            sb.append("\n📍 STATUT : DÉPLACÉ\n");
        }
        
        alert.setContentText(sb.toString());
        
        // ✅ AJOUT DES BOUTONS D'ACTION (seulement si le cours n'est pas déjà annulé)
        if (!"annule".equals(creneau.getStatut())) {
            ButtonType annulerBtn = new ButtonType("❌ Annuler ce cours", ButtonBar.ButtonData.OK_DONE);
            ButtonType deplacerBtn = new ButtonType("📅 Déplacer ce cours", ButtonBar.ButtonData.OK_DONE);
            ButtonType fermerBtn = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(annulerBtn, deplacerBtn, fermerBtn);
            
            alert.showAndWait().ifPresent(response -> {
                if (response == annulerBtn) {
                    annulerCours(creneau);
                } else if (response == deplacerBtn) {
                    ouvrirDialogueDeplacement(creneau);
                }
            });
        } else {
            // Cours déjà annulé, juste un bouton fermer
            ButtonType fermerBtn = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(fermerBtn);
            alert.showAndWait();
        }
    }
    
    /**
     * Annule un cours.
     * @param creneau le créneau à annuler
     */
    private void annulerCours(Creneau creneau) {
        if (creneau == null) return;
        
        Cours cours = getCoursFromCache(creneau.getCoursId());
        if (cours == null) {
            afficherErreur("Impossible de trouver les informations du cours");
            return;
        }
        
        Classe classe = getClasseFromCache(cours.getClasseId());
        String classeNom = classe != null ? classe.getIntitule() : "la classe";
        Matiere matiere = getMatiereFromCache(cours.getMatiereId());
        String matiereNom = matiere != null ? matiere.getNom() : "Cours";
        
        TextInputDialog motifDialog = new TextInputDialog();
        motifDialog.setTitle("Annulation de cours");
        motifDialog.setHeaderText("Annuler : " + matiereNom + " (" + cours.getTypeCours() + ")");
        motifDialog.setContentText("Motif de l'annulation (obligatoire) :");
        
        Optional<String> motif = motifDialog.showAndWait();
        if (motif.isPresent() && !motif.get().trim().isEmpty()) {
            String motifTexte = motif.get().trim();
            
            // ✅ Vérifier si c'est un EDT hebdomadaire, mensuel ou semestriel
            // On annule UNIQUEMENT ce créneau, pas les futurs
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Confirmer l'annulation");
            confirm.setContentText("Date: " + formaterDate(creneau.getJour()) + 
                                   "\nHoraire: " + creneau.getHeureDebut() + " - " + creneau.getHeureFin() +
                                   "\nClasse: " + classeNom +
                                   "\nMotif: " + motifTexte +
                                   "\n\n⚠️ Cette annulation ne concerne que ce créneau.\n" +
                                   "Les cours suivants ne sont pas affectés.\n\n" +
                                   "Les étudiants seront notifiés.");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() throws SQLException {
                            // ✅ Annuler UNIQUEMENT ce créneau
                            planningService.annulerCreneau(creneau.getId(), motifTexte);
                            return null;
                        }
                        
                        @Override
                        protected void succeeded() {
                            Platform.runLater(() -> {
                                // Envoyer les emails aux étudiants
                                envoyerMailsAnnulation(creneau, cours, matiereNom, classeNom, motifTexte);
                                afficherNotification("Cours annulé", 
                                    "Le cours a été annulé. Les étudiants ont été notifiés.");
                                // Recharger l'affichage
                                rechercherEmploiDuTemps();
                            });
                        }
                        
                        @Override
                        protected void failed() {
                            Platform.runLater(() -> {
                                afficherErreur("Erreur lors de l'annulation du cours");
                            });
                        }
                    };
                    new Thread(task).start();
                }
            });
        } else {
            afficherErreur("Le motif est obligatoire pour annuler un cours");
        }
    }
    
    /**
     * Envoie un email aux étudiants concernés par une annulation.
     */
    private void envoyerMailsAnnulation(Creneau creneau, Cours cours, String matiereNom, 
                                         String classeNom, String motif) {
        new Thread(() -> {
            try {
                List<Utilisateur> etudiants = utilisateurService.listerParRole("etudiant");
                for (Utilisateur u : etudiants) {
                    if (u instanceof Etudiant) {
                        Etudiant e = (Etudiant) u;
                        if (e.getClasseId() == cours.getClasseId() && e.isEstValide()) {
                            emailService.envoyerAnnulationCours(
                                e.getEmail(),
                                e.getPrenom() + " " + e.getNom(),
                                matiereNom,
                                cours.getTypeCours(),
                                formaterDate(creneau.getJour()),
                                creneau.getHeureDebut(),
                                motif
                            );
                        }
                    }
                }
                
                List<Utilisateur> gestionnaires = utilisateurService.listerParRole("gestionnaire");
                for (Utilisateur g : gestionnaires) {
                    emailService.envoyerConflitGestionnaire(
                        g.getEmail(),
                        "Annulation de cours",
                        "Le professeur " + enseignant.getPrenom() + " " + enseignant.getNom() +
                        " a annulé un cours: " + matiereNom + " le " + creneau.getJour(),
                        "Salle concernée",
                        creneau.getJour(),
                        creneau.getHeureDebut(),
                        creneau.getHeureFin(),
                        enseignant.getPrenom() + " " + enseignant.getNom()
                    );
                }
                
            } catch (SQLException e) {
                logger.error("Erreur envoi mails annulation", e);
            }
        }).start();
    }
    
    /**
     * Déplace un cours.
     * @param creneau le créneau à déplacer
     */
    private void deplacerCours(Creneau creneau) {
        if (creneau == null) return;

        Cours cours = getCoursFromCache(creneau.getCoursId());
        Classe classe = cours != null ? getClasseFromCache(cours.getClasseId()) : null;
        Matiere matiere = cours != null ? getMatiereFromCache(cours.getMatiereId()) : null;
        String matiereNom = matiere != null ? matiere.getNom() : "Cours";

        Alert choix = new Alert(Alert.AlertType.CONFIRMATION);
        choix.setTitle("Déplacer le cours");
        choix.setHeaderText("📚 " + matiereNom + " - " + formaterDate(creneau.getJour()) + " " + creneau.getHeureDebut());
        choix.setContentText("Que voulez-vous modifier ?");

        ButtonType btnJour  = new ButtonType("📅 Changer de jour/heure");
        ButtonType btnSalle = new ButtonType("🏫 Changer de salle");
        ButtonType btnAnnul = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        choix.getButtonTypes().setAll(btnJour, btnSalle, btnAnnul);

        choix.showAndWait().ifPresent(reponse -> {
            if (reponse == btnJour) {
                deplacerCoursChangerJour(creneau, cours, matiereNom);
            } else if (reponse == btnSalle) {
                deplacerCoursChangerSalle(creneau, cours, matiereNom);
            }
        });
    }

    private void deplacerCoursChangerJour(Creneau creneau, Cours cours, String matiereNom) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Changer de jour/heure");
        dialog.setHeaderText("Nouveau créneau pour : " + matiereNom +
                             "\n(Ce changement ne concerne que ce créneau)");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));

        DatePicker nouveauJour = new DatePicker(LocalDate.parse(creneau.getJour()));
        ComboBox<String> nouvelleHeure = new ComboBox<>();
        for (int h = 8; h <= 17; h++) {
            nouvelleHeure.getItems().add(String.format("%02d:00", h));
        }
        nouvelleHeure.setValue(creneau.getHeureDebut());
        
        Spinner<Integer> dureeSpinner = new Spinner<>(1, 5, 1);
        int duree = calculerDureeHeures(creneau.getHeureDebut(), creneau.getHeureFin());
        dureeSpinner.getValueFactory().setValue(duree);
        
        Label finLabel = new Label();
        finLabel.setText(calculerHeureFin(creneau.getHeureDebut(), duree));
        
        nouvelleHeure.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                finLabel.setText(calculerHeureFin(newV, dureeSpinner.getValue()));
            }
        });
        dureeSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (nouvelleHeure.getValue() != null) {
                finLabel.setText(calculerHeureFin(nouvelleHeure.getValue(), newV));
            }
        });

        grid.add(new Label("Nouveau jour :"), 0, 0);
        grid.add(nouveauJour, 1, 0);
        grid.add(new Label("Nouvelle heure :"), 0, 1);
        grid.add(nouvelleHeure, 1, 1);
        grid.add(new Label("Durée (h) :"), 0, 2);
        grid.add(dureeSpinner, 1, 2);
        grid.add(new Label("Fin :"), 0, 3);
        grid.add(finLabel, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK && nouveauJour.getValue() != null && nouvelleHeure.getValue() != null) {
                String ancienJour = creneau.getJour();
                String ancienneHeure = creneau.getHeureDebut();
                String ancSalle = creneau.getSalleId() != null ?
                    "Salle " + creneau.getSalleId() : "Non assignée";
                String nouvelleDate = nouveauJour.getValue().toString();
                String nouvelleH = nouvelleHeure.getValue();
                int nouvelleDuree = dureeSpinner.getValue();
                String nouvelleHeureFin = calculerHeureFin(nouvelleH, nouvelleDuree);
                
                Task<Boolean> verificationTask = new Task<>() {
                    @Override
                    protected Boolean call() throws SQLException {
                        List<String> conflits = planningService.verifierConflitDeplacement(
                            creneau, cours.getEnseignantId(), nouvelleDate, nouvelleH, nouvelleHeureFin);
                        return conflits.isEmpty();
                    }
                    
                    @Override
                    protected void succeeded() {
                        if (getValue()) {
                            Task<Void> task = new Task<>() {
                                @Override
                                protected Void call() throws Exception {
                                    // ✅ Modifier UNIQUEMENT ce créneau
                                    creneau.setJour(nouvelleDate);
                                    creneau.setHeureDebut(nouvelleH);
                                    creneau.setHeureFin(nouvelleHeureFin);
                                    creneau.setStatut("deplace");
                                    planningService.modifierCreneau(creneau);
                                    return null;
                                }
                                @Override
                                protected void succeeded() {
                                    envoyerMailsDeplacement(creneau, cours, matiereNom,
                                        ancienJour, ancienneHeure, ancSalle,
                                        nouvelleDate, nouvelleH, ancSalle);
                                    afficherNotification("Cours déplacé",
                                        matiereNom + " déplacé au " + nouvelleDate + " à " + nouvelleH);
                                    rechercherEmploiDuTemps();
                                }
                                @Override
                                protected void failed() {
                                    afficherErreur("Erreur lors du déplacement");
                                }
                            };
                            new Thread(task).start();
                        } else {
                            afficherErreur("❌ Conflit détecté sur ce créneau. Le gestionnaire a été notifié.");
                            notifierGestionnaireConflit(creneau, cours, matiereNom, nouvelleDate, nouvelleH, nouvelleHeureFin);
                        }
                    }
                };
                new Thread(verificationTask).start();
            }
        });
    }

    private void deplacerCoursChangerSalle(Creneau creneau, Cours cours, String matiereNom) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Changer de salle");
        dialog.setHeaderText("Nouvelle salle pour : " + matiereNom +
            " (" + creneau.getJour() + " " + creneau.getHeureDebut() + ")");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ComboBox<Salle> salleCombo = new ComboBox<>();
        salleCombo.setPromptText("Chargement des salles disponibles...");
        salleCombo.setPrefWidth(400);
        
        Task<List<Salle>> taskSalles = new Task<>() {
            @Override
            protected List<Salle> call() throws Exception {
                return salleService.rechercherSallesDisponibles(
                    creneau.getJour(), creneau.getHeureDebut(), creneau.getHeureFin(), cours.getClasseId());
            }
            @Override
            protected void succeeded() {
                List<Salle> salles = getValue();
                salleCombo.getItems().setAll(salles);
                salleCombo.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(Salle s, boolean empty) {
                        super.updateItem(s, empty);
                        setText(empty || s == null ? null :
                            s.getNumero() + " (Cap:" + s.getCapacite() + " - " + s.getType() + ")");
                    }
                });
                salleCombo.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(Salle s, boolean empty) {
                        super.updateItem(s, empty);
                        setText(empty || s == null ? "Sélectionner une salle" : s.getNumero());
                    }
                });
                if (!salles.isEmpty()) {
                    salleCombo.getSelectionModel().select(0);
                }
            }
        };
        new Thread(taskSalles).start();

        grid.add(new Label("Nouvelle salle :"), 0, 0);
        grid.add(salleCombo, 1, 0);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK && salleCombo.getValue() != null) {
                Salle nouvelleSalle = salleCombo.getValue();
                String ancSalle = creneau.getSalleId() != null ?
                    "Salle " + creneau.getSalleId() : "?";
                
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        creneau.setSalleId(nouvelleSalle.getId());
                        planningService.modifierCreneau(creneau);
                        return null;
                    }
                    @Override
                    protected void succeeded() {
                        envoyerMailsDeplacement(creneau, cours, matiereNom,
                            creneau.getJour(), creneau.getHeureDebut(), ancSalle,
                            creneau.getJour(), creneau.getHeureDebut(), nouvelleSalle.getNumero());
                        afficherNotification("Salle changée",
                            "Cours déplacé vers la salle " + nouvelleSalle.getNumero());
                        rechercherEmploiDuTemps();
                    }
                    @Override
                    protected void failed() {
                        afficherErreur("Erreur lors du changement de salle");
                    }
                };
                new Thread(task).start();
            }
        });
    }
    
    private void notifierGestionnaireConflit(Creneau creneau, Cours cours, String matiereNom,
                                              String nouvelleDate, String nouvelleHeure, String nouvelleHeureFin) {
        new Thread(() -> {
            try {
                List<Utilisateur> gestionnaires = utilisateurService.listerParRole("gestionnaire");
                for (Utilisateur g : gestionnaires) {
                    emailService.envoyerAlerteDeplacementConflit(
                        g.getEmail(),
                        enseignant.getPrenom() + " " + enseignant.getNom(),
                        matiereNom,
                        cours.getTypeCours(),
                        creneau.getJour() + " " + creneau.getHeureDebut() + "-" + creneau.getHeureFin(),
                        nouvelleDate + " " + nouvelleHeure + "-" + nouvelleHeureFin,
                        "Conflit de salle ou d'enseignant sur le nouveau créneau"
                    );
                }
            } catch (SQLException e) {
                logger.error("Erreur notification gestionnaire", e);
            }
        }).start();
    }
    
    private String calculerHeureFin(String heureDebut, int dureeHeures) {
        String[] parts = heureDebut.split(":");
        int heure = Integer.parseInt(parts[0]) + dureeHeures;
        return String.format("%02d:00", heure);
    }
    
    private void envoyerMailsDeplacement(Creneau creneau, Cours cours, String matiereNom,
            String ancienJour, String ancienneHeure, String ancienneSalle,
            String nouveauJour, String nouvelleHeure, String nouvelleSalle) {
        new Thread(() -> {
            try {
                List<Utilisateur> etudiants = utilisateurService.listerParRole("etudiant");
                for (Utilisateur u : etudiants) {
                    if (u instanceof Etudiant) {
                        Etudiant e = (Etudiant) u;
                        if (e.getClasseId() == cours.getClasseId() && e.isEstValide()) {
                            emailService.envoyerDeplacementCours(
                                e.getEmail(),
                                e.getPrenom() + " " + e.getNom(),
                                matiereNom,
                                cours.getTypeCours(),
                                formaterDate(ancienJour),
                                ancienneHeure,
                                ancienneSalle,
                                formaterDate(nouveauJour),
                                nouvelleHeure,
                                nouvelleSalle
                            );
                        }
                    }
                }
            } catch (SQLException e) {
                logger.error("Erreur envoi mails déplacement", e);
            }
        }).start();
    }
    
    // STATISTIQUES EN HAUT
    
    private void calculerStatistiques() {
        if (totalCoursLabel == null) return;
        
        int total = creneauxEnseignant != null ? creneauxEnseignant.size() : 0;
        totalCoursLabel.setText(String.valueOf(total));
        
        int heuresTotales = 0;
        if (creneauxEnseignant != null) {
            for (Creneau c : creneauxEnseignant) {
                heuresTotales += calculerDureeHeures(c.getHeureDebut(), c.getHeureFin());
            }
        }
        
        if (heuresTotalesLabel != null) {
            heuresTotalesLabel.setText(heuresTotales + "h");
        }
        
        if (prochainCoursLabel != null && creneauxEnseignant != null) {
            LocalDate now = LocalDate.now();
            LocalTime nowTime = LocalTime.now();
            Optional<Creneau> prochain = creneauxEnseignant.stream()
                .filter(c -> {
                    try {
                        LocalDate d = LocalDate.parse(c.getJour());
                        return d.isAfter(now) || (d.isEqual(now) && 
                            LocalTime.parse(c.getHeureDebut()).isAfter(nowTime));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .min((c1, c2) -> {
                    try {
                        LocalDate d1 = LocalDate.parse(c1.getJour());
                        LocalDate d2 = LocalDate.parse(c2.getJour());
                        int cmp = d1.compareTo(d2);
                        if (cmp == 0) {
                            return c1.getHeureDebut().compareTo(c2.getHeureDebut());
                        }
                        return cmp;
                    } catch (Exception e) {
                        return 0;
                    }
                });
            
            if (prochain.isPresent()) {
                Creneau c = prochain.get();
                prochainCoursLabel.setText(formaterDate(c.getJour()) + " " + c.getHeureDebut());
            } else {
                prochainCoursLabel.setText("Aucun");
            }
        }
    }
    
    private void allerAujourdhui() { chargerSemaine(); }
    
    // MÉTHODES EXISTANTES
    
    private void afficherInfosEnseignant() {
        if (enseignant != null) {
            if (bienvenueLabel != null) {
                bienvenueLabel.setText("Bienvenue, " + enseignant.getPrenom() + " " + enseignant.getNom());
            }
            if (matriculeLabel != null) {
                matriculeLabel.setText("Matricule: " + enseignant.getMatricule());
            }
        }
    }
    
    private void chargerMesCours() {
        if (enseignant == null) return;
        
        Task<List<Cours>> task = new Task<>() {
            @Override
            protected List<Cours> call() throws SQLException {
                List<Cours> coursList = coursService.listerParEnseignant(enseignant.getId());
                for (Cours c : coursList) {
                    coursCache.put(c.getId(), c);
                }
                return coursList;
            }
            
            @Override
            protected void succeeded() {
                List<Cours> resultat = getValue();
                if (resultat == null) resultat = new ArrayList<>();
                tousMesCours = resultat;
                
                Platform.runLater(() -> {
                    try {
                        mettreAJourListeCours();
                        // Afficher le nb de cours dès le chargement
                        if (totalCoursLabel != null)
                            totalCoursLabel.setText(String.valueOf(tousMesCours.size()));
                        if (totalCoursCountLabel != null)
                            totalCoursCountLabel.setText(tousMesCours.size() + " cours");
                    } catch (Exception e) {
                        logger.error("Erreur mise à jour liste cours", e);
                    }
                });
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement cours", getException());
            }
        };
        new Thread(task).start();
    }
    
    private void mettreAJourListeCours() {
        if (mesCoursList == null) return;
        
        mesCoursList.setCellFactory(lv -> new ListCell<Cours>() {
            @Override
            protected void updateItem(Cours item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        String matiere = getNomMatiere(item.getMatiereId());
                        String classe = getNomClasse(item.getClasseId());
                        setText(matiere + " | " + classe + " | " + item.getTypeCours());
                    } catch (Exception e) {
                        setText("Cours #" + item.getId());
                    }
                }
            }
        });
        
        mesCoursList.setItems(FXCollections.observableArrayList(tousMesCours));
        
        if (totalCoursCountLabel != null) {
            totalCoursCountLabel.setText(tousMesCours.size() + " cours");
        }
    }
    
    
    /**
     * Recharge l'emploi du temps de l'enseignant.
     */
    private void rechercherEmploiDuTemps() {
        if (enseignant == null) return;
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        
        // ✅ Récupérer les dates de la période sélectionnée
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin = dateFinPicker.getValue();
        
        if (dateDebut == null) dateDebut = LocalDate.now();
        if (dateFin == null) dateFin = dateDebut.plusDays(6);
        
        final LocalDate debutFinal = dateDebut;
        final LocalDate finFinal = dateFin;
        
        System.out.println("📅 Recherche EDT pour période: " + debutFinal + " → " + finFinal);
        
        Task<List<Creneau>> task = new Task<>() {
            @Override
            protected List<Creneau> call() throws SQLException {
                // ✅ Récupérer TOUS les créneaux de l'enseignant
                List<Creneau> tousCreneaux = planningService.getCreneauxParEnseignant(enseignant.getId());
                
                System.out.println("🔍 Enseignant ID: " + enseignant.getId());
                System.out.println("   Total créneaux trouvés: " + tousCreneaux.size());
                
                // ✅ FILTRER par période sélectionnée
                List<Creneau> creneauxFiltres = new ArrayList<>();
                for (Creneau c : tousCreneaux) {
                    try {
                        LocalDate jour = LocalDate.parse(c.getJour());
                        if (!jour.isBefore(debutFinal) && !jour.isAfter(finFinal)) {
                            creneauxFiltres.add(c);
                            System.out.println("   ✓ Créneau conservé: " + c.getJour() + " " + c.getHeureDebut() + "-" + c.getHeureFin());
                        }
                    } catch (Exception e) {
                        System.out.println("   ✗ Erreur parsing date: " + c.getJour());
                    }
                }
                
                System.out.println("   Créneaux dans la période: " + creneauxFiltres.size());
                return creneauxFiltres;
            }
            
            @Override
            protected void succeeded() {
                creneauxEnseignant = getValue();
                Platform.runLater(() -> {
                    afficherEmploiTemps();
                    calculerStatistiques();
                    if (chargementIndicator != null) chargementIndicator.setVisible(false);
                });
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement emploi du temps", getException());
                Platform.runLater(() -> {
                    if (chargementIndicator != null) chargementIndicator.setVisible(false);
                    afficherErreur("Erreur: " + getException().getMessage());
                });
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerFilieresEnseignant() {
        Task<List<Ufr>> task = new Task<>() {
            @Override
            protected List<Ufr> call() throws SQLException {
                // Récupérer toutes les classes où l'enseignant intervient
                List<Cours> mesCours = coursService.listerParEnseignant(enseignant.getId());
                Set<Integer> ufrIds = new HashSet<>();
                
                for (Cours c : mesCours) {
                    Classe classe = classeService.trouverParId(c.getClasseId());
                    if (classe != null) {
                        ufrIds.add(classe.getUfrId());
                    }
                }
                
                // Récupérer les UFR correspondantes
                List<Ufr> ufrs = new ArrayList<>();
                for (int id : ufrIds) {
                    Ufr ufr = ufrService.trouverParId(id);
                    if (ufr != null) ufrs.add(ufr);
                }
                
                return ufrs;
            }
            
            @Override
            protected void succeeded() {
                List<Ufr> ufrs = getValue();
                if (filtreUfrCombo != null) {
                    filtreUfrCombo.getItems().clear();
                    filtreUfrCombo.getItems().add(null);
                    filtreUfrCombo.getItems().addAll(ufrs);
                    
                    filtreUfrCombo.setCellFactory(lv -> new ListCell<>() {
                        @Override protected void updateItem(Ufr u, boolean empty) {
                            super.updateItem(u, empty);
                            setText(empty || u == null ? "Toutes les UFR" : u.getNom());
                        }
                    });
                }
            }
        };
        new Thread(task).start();
    }
    
    private void rechercherPlanningDetail() {
        if (planningDateDebut == null || planningDateFin == null || planningTableView == null) return;
        
        LocalDate debut = planningDateDebut.getValue();
        LocalDate fin = planningDateFin.getValue();
        
        if (debut == null || fin == null) {
            afficherErreur("Veuillez sélectionner les dates");
            return;
        }
        
        if (rechercherPlanningButton != null) rechercherPlanningButton.setDisable(true);
        
        Task<List<Creneau>> task = new Task<>() {
            @Override
            protected List<Creneau> call() throws SQLException {
                List<Creneau> resultats = new ArrayList<>();
                LocalDate date = debut;
                while (!date.isAfter(fin)) {
                    resultats.addAll(planningService.getCreneauxParJour(date.toString()));
                    date = date.plusDays(1);
                }
                
                resultats.removeIf(c -> {
                    Cours cours = getCoursFromCache(c.getCoursId());
                    return cours == null || cours.getEnseignantId() != enseignant.getId();
                });
                
                return resultats;
            }
            
            @Override
            protected void succeeded() {
                List<Creneau> resultats = getValue();
                Platform.runLater(() -> {
                    if (planningTableView != null) {
                        planningTableView.setItems(FXCollections.observableArrayList(resultats));
                        planningTableView.refresh();
                    }
                    if (rechercherPlanningButton != null) rechercherPlanningButton.setDisable(false);
                });
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur recherche planning", getException());
                Platform.runLater(() -> {
                    afficherErreur("Erreur lors du chargement du planning détaillé");
                    if (rechercherPlanningButton != null) rechercherPlanningButton.setDisable(false);
                });
            }
        };
        
        new Thread(task).start();
    }
    
    private void configurerFiltresCours() {
        if (rechercheCoursField != null) {
            rechercheCoursField.textProperty().addListener((obs, oldVal, newVal) -> {
                filtrerCours(newVal);
            });
        }
    }
    
    private void filtrerCours(String recherche) {
        if (tousMesCours == null || tousMesCours.isEmpty()) {
            if (mesCoursList != null) {
                mesCoursList.setItems(FXCollections.observableArrayList());
            }
            return;
        }
        
        if (recherche == null || recherche.trim().isEmpty()) {
            mesCoursList.setItems(FXCollections.observableArrayList(tousMesCours));
            if (totalCoursCountLabel != null) {
                totalCoursCountLabel.setText(tousMesCours.size() + " cours");
            }
            return;
        }
        
        String rech = recherche.toLowerCase().trim();
        List<Cours> filtres = new ArrayList<>();
        
        for (Cours c : tousMesCours) {
            try {
                String matiere = getNomMatiere(c.getMatiereId()).toLowerCase();
                String classe = getNomClasse(c.getClasseId()).toLowerCase();
                String type = c.getTypeCours().toLowerCase();
                
                if (matiere.contains(rech) || classe.contains(rech) || type.contains(rech)) {
                    filtres.add(c);
                }
            } catch (Exception e) {
                if (String.valueOf(c.getId()).contains(rech)) {
                    filtres.add(c);
                }
            }
        }
        
        mesCoursList.setItems(FXCollections.observableArrayList(filtres));
        if (totalCoursCountLabel != null) {
            totalCoursCountLabel.setText(filtres.size() + " cours");
        }
    }
    
    private String getNomMatiere(int matiereId) {
        Matiere m = getMatiereFromCache(matiereId);
        return m != null ? m.getNom() : "Matière " + matiereId;
    }
    
    private String getNomClasse(int classeId) {
        Classe c = getClasseFromCache(classeId);
        return c != null ? c.getIntitule() : "Classe " + classeId;
    }
    
    private Cours getCoursFromCache(int coursId) {
        if (!coursCache.containsKey(coursId)) {
            try {
                Cours cours = coursService.trouverParId(coursId);
                if (cours != null) coursCache.put(coursId, cours);
            } catch (SQLException e) {
                logger.error("Erreur chargement cours {} depuis cache", coursId, e);
            }
        }
        return coursCache.get(coursId);
    }
    
    private Matiere getMatiereFromCache(int matiereId) {
        if (!matiereCache.containsKey(matiereId)) {
            try {
                Matiere matiere = matiereService.trouverParId(matiereId);
                if (matiere != null) matiereCache.put(matiereId, matiere);
            } catch (SQLException e) {
                logger.error("Erreur chargement matiere {} depuis cache", matiereId, e);
            }
        }
        return matiereCache.get(matiereId);
    }
    
    private Classe getClasseFromCache(int classeId) {
        if (!classeCache.containsKey(classeId)) {
            try {
                Classe classe = classeService.trouverParId(classeId);
                if (classe != null) classeCache.put(classeId, classe);
            } catch (SQLException e) {
                logger.error("Erreur chargement classe {} depuis cache", classeId, e);
            }
        }
        return classeCache.get(classeId);
    }
    
    private Salle getSalleFromCache(int salleId) {
        if (!salleCache.containsKey(salleId)) {
            try {
                Salle salle = salleService.trouverParId(salleId);
                if (salle != null) salleCache.put(salleId, salle);
            } catch (SQLException e) {
                logger.error("Erreur chargement salle {} depuis cache", salleId, e);
            }
        }
        return salleCache.get(salleId);
    }
    
    private void naviguerVersReservation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Reservation.fxml"));
            Parent root = loader.load();
            
            ReservationControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur navigation réservation", e);
        }
    }
    
    private void exporterEmploiDuTempsPDF() {
        if (creneauxEnseignant == null || creneauxEnseignant.isEmpty()) {
            afficherErreur("Aucun emploi du temps à exporter");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter mon emploi du temps en PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        
        String nomFichier = "MonEDT_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            LocalDate debut = dateDebutPicker.getValue() != null ? dateDebutPicker.getValue() : LocalDate.now();
            LocalDate fin = dateFinPicker.getValue() != null ? dateFinPicker.getValue() : LocalDate.now().plusDays(6);
            String periode = "du " + debut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                             " au " + fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            afficherNotification("Export PDF", "Génération du PDF en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    exportService.exporterEDTGrillePDF(
                        creneauxEnseignant, 
                        null, 
                        periode, 
                        enseignant, 
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
    
    private void exporterEmploiDuTempsExcel() {
        if (creneauxEnseignant == null || creneauxEnseignant.isEmpty()) {
            afficherErreur("Aucun emploi du temps à exporter");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter mon emploi du temps en Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );
        
        String nomFichier = "MonEDT_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            LocalDate debut = dateDebutPicker.getValue() != null ? dateDebutPicker.getValue() : LocalDate.now();
            LocalDate fin = dateFinPicker.getValue() != null ? dateFinPicker.getValue() : LocalDate.now().plusDays(6);
            String periode = "du " + debut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                             " au " + fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            afficherNotification("Export Excel", "Génération du fichier en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    exportService.exporterEDTGrilleExcel(
                        creneauxEnseignant, 
                        null, 
                        periode, 
                        enseignant, 
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
    
    private void exporterPlanningPDF() {
        if (planningTableView.getItems() == null || planningTableView.getItems().isEmpty()) {
            afficherErreur("Aucune donnée de planning à exporter");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le planning détaillé en PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        
        String nomFichier = "Planning_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            List<Creneau> listePlanning = new ArrayList<>(planningTableView.getItems());
            
            afficherNotification("Export PDF", "Génération du PDF en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    exportService.exporterEDTGrillePDF(
                        listePlanning, 
                        null, 
                        "Planning détaillé", 
                        enseignant, 
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
    
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (emploiTempsGrid != null && emploiTempsGrid.getScene() != null && emploiTempsGrid.getScene().getWindow() instanceof Stage) {
            return (Stage) emploiTempsGrid.getScene().getWindow();
        }
        return null;
    }
    
    private String formaterDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    /**
     * Calcule la durée en heures entre deux heures au format HH:mm
     */
    private int calculerDureeHeures(String heureDebut, String heureFin) {
        try {
            String[] debut = heureDebut.split(":");
            String[] fin = heureFin.split(":");
            int debutHeure = Integer.parseInt(debut[0]);
            int finHeure = Integer.parseInt(fin[0]);
            
            if (finHeure < debutHeure) {
                finHeure += 12;
            }
            
            // ✅ Nombre de cases = (heure_fin - heure_debut) + 1
            int duree = (finHeure - debutHeure) + 1;
            return Math.max(1, duree);
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    protected void handleRetour() {
        super.handleRetour();
    }


    @FXML private void handleOngletEdt()        { selectionnerTab(0); }
    @FXML private void handleOngletCours()      { selectionnerTab(1); }
    @FXML private void handleOngletPlanning()   { selectionnerTab(2); }
    @FXML private void handleOngletRecherche()  { selectionnerTab(3); }
    @FXML private void handleOngletCarte()      { selectionnerTab(4); }
    @FXML private void handleOngletSignalement(){ selectionnerTab(5); }

    private void selectionnerTab(int index) {
        if (tabPane != null && index >= 0 && index < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(index);
        }
    }
}