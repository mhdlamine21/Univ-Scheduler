package scheduler.controleur;

import scheduler.dao.*;
import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Tableau de bord étudiant.
 */
public class TableauBordEtudiantControleur extends TableauBordControleur {

    // COMPOSANTS INFO
    @FXML private Label bienvenueLabel;
    @FXML private Label classeLabel;
    @FXML private Label anneeLabel;
    @FXML private Label numeroEtudiantLabel;
    @FXML private Label typeEtudiantLabel;
    @FXML private HBox legendContainer;
    @FXML private TabPane tabPane;
    @FXML private Tab ongletRecherche;
    @FXML private Tab ongletCarte;

    // COMPOSANTS EDT PRINCIPAL
    @FXML private VBox edtPrincipalContainer;
    @FXML private Label edtTitreLabel;
    @FXML private Label edtPeriodeLabel;
    @FXML private Label aucunEdtLabel;
    @FXML private GridPane emploiTempsGrid;
    @FXML private VBox listeAutresEdtContainer;

    // BOUTONS EDT
    @FXML private Button actualiserButton;
    @FXML private Button aujourdhuiButton;
    @FXML private Button semaineButton;
    @FXML private Button exporterPDFButton;
    @FXML private Button exporterExcelButton;
    @FXML private Button retourPrincipalButton;

    // PLANNING DÉTAILLÉ
    @FXML private TableView<Creneau> planningTableView;
    @FXML private TableColumn<Creneau, String> dateColumn;
    @FXML private TableColumn<Creneau, String> horaireColumn;
    @FXML private TableColumn<Creneau, String> matiereColumn;
    @FXML private TableColumn<Creneau, String> salleColumn;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Button rechercherButton;
    @FXML private ProgressIndicator chargementIndicator;

    // SERVICES
    private PlanningService planningService;
    private ExportService exportService;
    private CoursService coursService;
    private MatiereService matiereService;
    private SalleService salleService;
    private ClasseService classeService;
    private UtilisateurService utilisateurService;
    private EmploiDuTempsDAO edtDAO;
    private EmploiDuTempsCreneauDAO edtCreneauDAO;

    // DONNÉES
    private Etudiant etudiant;
    private Classe classe;
    private List<EmploiDuTemps> tousEdts;
    private EmploiDuTemps edtActuel;
    private List<Creneau> creneauxActuels;
    private Map<Integer, String> cacheMatieres = new HashMap<>();
    private Map<Integer, Salle> cacheSalles = new HashMap<>();
    private Map<Integer, String> cacheEnseignants = new HashMap<>();

    // CONFIGURATION DE LA GRILLE
    private int grilleHeureDebut = 8;
    private int grilleHeureFin = 18;
    private int grilleEcartHeures = 2;
    private List<String> grilleJours = new ArrayList<>();
    private Map<String, Integer> ordreJours = new HashMap<>();
    private List<String> grilleHeures = new ArrayList<>();

    // NOUVELLES COULEURS (Indigo/Ambre)
    private static final String COULEUR_CM = "#4361EE";      // Indigo
    private static final String COULEUR_TD = "#2DC653";      // Vert
    private static final String COULEUR_TP = "#F4A261";      // Ambre
    private static final String COULEUR_ANNULE = "#E53E3E";  // Rouge
    private static final String COULEUR_DEPLACE = "#F4A261"; // Ambre
    
 // MÉTHODES SIDEBAR

    @FXML private StackPane contenuPrincipal;
    @FXML private Button sidebarEdtBtn;
    @FXML private Button sidebarCoursBtn;
    @FXML private Button sidebarPlanningBtn;
    @FXML private Button sidebarRechercheBtn;
    @FXML private Button sidebarCarteBtn;
    @FXML private Button sidebarSignalementBtn;


    private void configurerSidebar() {
        if (sidebarEdtBtn != null) {
            sidebarEdtBtn.setOnAction(e -> afficherContenuEdt());
        }
        if (sidebarPlanningBtn != null) {
            sidebarPlanningBtn.setOnAction(e -> afficherContenuPlanningDetail());
        }
        if (sidebarRechercheBtn != null) {
            sidebarRechercheBtn.setOnAction(e -> afficherContenuRecherche());
        }
        if (sidebarCarteBtn != null) {
            sidebarCarteBtn.setOnAction(e -> afficherContenuCarte());
        }
        
    }
    
    public void rechargerEdtPrincipal() {
        if (edtActuel != null) {
            afficherEdt(edtActuel);
        } else if (tousEdts != null && !tousEdts.isEmpty()) {
            afficherEdt(tousEdts.get(0));
        } else {
            afficherAucunEdt();
        }
    }
    
    private void afficherContenuEdt() {
        rechargerEdtPrincipal();
    }
    
    private void afficherContenuPlanningDetail() {
        try {
            // ✅ Utiliser le FXML Planning existant (pas PlanningEtudiant)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Planning.fxml"));
            Parent root = loader.load();
            
            PlanningControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            controleur.setOrigineRole("etudiant");
            
            contenuPrincipal.getChildren().clear();
            contenuPrincipal.getChildren().add(root);
            
        } catch (IOException e) {
            logger.error("Erreur chargement Planning", e);
            afficherErreur("Impossible d'ouvrir le planning");
        }
    }
    
    private void afficherContenuRecherche() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Recherche.fxml"));
            Parent root = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof RechercheControleur) {
                ((RechercheControleur) controller).setUtilisateurEtStage(utilisateurConnecte, primaryStage);
            }
            
            contenuPrincipal.getChildren().clear();
            contenuPrincipal.getChildren().add(root);
            
        } catch (IOException e) {
            logger.error("Erreur chargement Recherche", e);
            afficherErreur("Impossible d'ouvrir la recherche");
        }
    }
    
    private void afficherContenuCarte() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Carte.fxml"));
            Parent root = loader.load();
            
            CarteControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            
            contenuPrincipal.getChildren().clear();
            contenuPrincipal.getChildren().add(root);
            
        } catch (IOException e) {
            logger.error("Erreur chargement Carte", e);
            afficherErreur("Impossible d'ouvrir la carte");
        }
    }

    private void selectionnerOnglet(int index) {
        if (tabPane != null && index >= 0 && index < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(index);
            
            // Si on revient à l'onglet EDT (index 0), recharger
            if (index == 0) {
                rechargerEdtPrincipal();
            }
        }
    }

    
 // MÉTHODES DE SÉLECTION D'ONGLET

    public void selectionnerOngletRecherche() {
        if (ongletRecherche != null && tabPane != null) {
            tabPane.getSelectionModel().select(ongletRecherche);
        }
    }

    public void selectionnerOngletCarte() {
        if (ongletCarte != null && tabPane != null) {
            tabPane.getSelectionModel().select(ongletCarte);
        }
    }


    @Override
    public void initialize() {
        super.initialize();
        planningService = new PlanningService();
        exportService = new ExportService();
        coursService = new CoursService();
        matiereService = new MatiereService();
        salleService = new SalleService();
        classeService = new ClasseService();
        utilisateurService = new UtilisateurService();
        edtDAO = new EmploiDuTempsDAO();
        edtCreneauDAO = new EmploiDuTempsCreneauDAO();
        
        ordreJours.put("Lundi", 1);
        ordreJours.put("Mardi", 2);
        ordreJours.put("Mercredi", 3);
        ordreJours.put("Jeudi", 4);
        ordreJours.put("Vendredi", 5);
        ordreJours.put("Samedi", 6);
    }

    @Override
    public void initialiserAvecUtilisateur(Utilisateur utilisateur, Stage stage) {
        super.initialiserAvecUtilisateur(utilisateur, stage);
        if (utilisateur instanceof Etudiant) {
            this.etudiant = (Etudiant) utilisateur;
        }
        initialiserTableauBord();
    }

    @Override
    protected void initialiserTableauBord() {
        configurerBoutons();
        configurerTableauPlanning();
        afficherInfosEtudiant();
        creerLegende();
        configurerSidebar();
        gererOngletRecherche();
        chargerInfosClasseEtEdts();
    }

    @Override
    protected void rafraichirDonnees() {
        chargerInfosClasseEtEdts();
    }

    // AFFICHAGE INFOS ÉTUDIANT
    private void afficherInfosEtudiant() {
        if (etudiant == null) return;
        
        if (bienvenueLabel != null) {
            bienvenueLabel.setText("Bienvenue, " + etudiant.getPrenom() + " " + etudiant.getNom());
        }
        if (numeroEtudiantLabel != null) {
            numeroEtudiantLabel.setText(etudiant.getNumeroEtudiant());
        }
        if (classeLabel != null && classe != null) {
            classeLabel.setText(classe.getIntitule());
        }
        if (anneeLabel != null && classe != null) {
            anneeLabel.setText(classe.getAnneeScolaire());
        }

        if (typeEtudiantLabel != null) {
            if (etudiant.estResponsable()) {
                typeEtudiantLabel.setText("📌 Responsable de classe");
                typeEtudiantLabel.setStyle("-fx-text-fill: #4361EE; -fx-font-weight: bold; -fx-font-size: 12px;");
            } else {
                typeEtudiantLabel.setText("Étudiant");
                typeEtudiantLabel.setStyle("-fx-text-fill: #1A1F3A; -fx-font-size: 12px;");
            }
        }
        }

    // LÉGENDE AVEC NOUVELLES COULEURS
    private void creerLegende() {
        if (legendContainer == null) return;
        legendContainer.getChildren().clear();
        legendContainer.setSpacing(20);
        legendContainer.setAlignment(Pos.CENTER_LEFT);
        legendContainer.getChildren().addAll(
            creerItemLegende("CM", COULEUR_CM, "Cours Magistral"),
            creerItemLegende("TD", COULEUR_TD, "Travaux Dirigés"),
            creerItemLegende("TP", COULEUR_TP, "Travaux Pratiques"),
            creerItemLegende("Annulé", COULEUR_ANNULE, "Cours annulé"),
            creerItemLegende("Déplacé", COULEUR_DEPLACE, "Cours déplacé")
        );
    }

    private HBox creerItemLegende(String texte, String couleur, String libelle) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        Rectangle rect = new Rectangle(14, 14);
        rect.setFill(Color.web(couleur, 0.35));
        rect.setStroke(Color.web(couleur));
        rect.setStrokeWidth(1.5);
        Label label = new Label(texte + " - " + libelle);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #1A1F3A;");
        box.getChildren().addAll(rect, label);
        return box;
    }

    // CONFIGURATION
    private void configurerBoutons() {
        if (actualiserButton != null) actualiserButton.setOnAction(e -> chargerInfosClasseEtEdts());
        if (aujourdhuiButton != null) aujourdhuiButton.setOnAction(e -> afficherJourActuel());
        if (semaineButton != null) semaineButton.setOnAction(e -> afficherSemaineActuelle());
        if (exporterPDFButton != null) exporterPDFButton.setOnAction(e -> exporterEdtPDF(edtActuel));
        if (exporterExcelButton != null) exporterExcelButton.setOnAction(e -> exporterEdtExcel(edtActuel));
        if (rechercherButton != null) rechercherButton.setOnAction(e -> rechercherPlanningDetail());
        if (retourPrincipalButton != null) {
            retourPrincipalButton.setOnAction(e -> retournerEdtPrincipal());
            retourPrincipalButton.setVisible(false);
        }

        if (dateDebutPicker != null) dateDebutPicker.setValue(LocalDate.now());
        if (dateFinPicker != null) dateFinPicker.setValue(LocalDate.now().plusDays(6));
    }

    private void gererOngletRecherche() {
        if (tabPane == null || ongletRecherche == null) return;

        boolean estResponsable = etudiant != null && etudiant.estResponsable();

        if (!estResponsable) {
            tabPane.getTabs().remove(ongletRecherche);  // ✅ Supprime l'onglet
        } else {
            if (ongletRecherche.getContent() != null) {
                Object controller = ongletRecherche.getContent().getProperties().get("fx:controller");
                if (controller instanceof RechercheControleur) {
                    ((RechercheControleur) controller).setUtilisateurEtStage(utilisateurConnecte, primaryStage);
                }
            }
        }
    }


    // CHARGEMENT DES DONNÉES

    private void chargerInfosClasseEtEdts() {
        if (etudiant == null) return;

        Task<Void> task = new Task<>() {
            Classe classeChargee;
            List<EmploiDuTemps> edtsCharges;

            @Override
            protected Void call() throws SQLException {
                classeChargee = classeService.trouverParId(etudiant.getClasseId());
                edtsCharges = planningService.getEdtsParClasse(etudiant.getClasseId());
                return null;
            }

            @Override
            protected void succeeded() {
                classe = classeChargee;
                tousEdts = edtsCharges != null ? edtsCharges : new ArrayList<>();

                if (classeLabel != null && classe != null) {
                    classeLabel.setText(classe.getIntitule());
                }
                if (anneeLabel != null && classe != null) {
                    anneeLabel.setText(classe.getAnneeScolaire());
                }

                gererOngletRecherche();

                chargerCaches(() -> {
                    if (!tousEdts.isEmpty()) {
                        tousEdts.sort((a, b) -> b.getPeriodeDebut().compareTo(a.getPeriodeDebut()));
                        afficherEdt(tousEdts.get(0));
                    } else {
                        afficherAucunEdt();
                    }
                });
            }

            @Override
            protected void failed() {
                logger.error("Erreur chargement EDT", getException());
                afficherAucunEdt();
            }
        };

        new Thread(task).start();
    }

    private void chargerCaches(Runnable apres) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                List<Cours> tousCours = coursService.listerTous();
                for (Cours c : tousCours) {
                    Matiere m = matiereService.trouverParId(c.getMatiereId());
                    if (m != null) cacheMatieres.put(c.getId(), m.getNom());
                }

                List<Salle> salles = salleService.listerTous();
                for (Salle s : salles) cacheSalles.put(s.getId(), s);

                List<Utilisateur> enseignants = utilisateurService.listerParRole("enseignant");
                for (Utilisateur u : enseignants) {
                    if (u instanceof Enseignant) {
                        Enseignant e = (Enseignant) u;
                        String grade = e.getGrade();
                        String nom = e.getNom();
                        cacheEnseignants.put(u.getId(), (grade != null && !grade.isEmpty() ? grade + " " : "") + nom);
                    } else if (u != null) {
                        cacheEnseignants.put(u.getId(), u.getNom());
                    }
                }
                return null;
            }
            @Override
            protected void succeeded() { Platform.runLater(apres); }
        };
        new Thread(task).start();
    }

    // AFFICHAGE D'UN EDT

    private void afficherEdt(EmploiDuTemps edt) {
        if (edt == null) return;

        System.out.println("📅 AFFICHAGE EDT #" + edt.getId());
        System.out.println("📅 Période: " + edt.getPeriodeDebut() + " → " + edt.getPeriodeFin());

        this.edtActuel = edt;

        if (edtTitreLabel != null) {
            String titre = getTitreEdt(edt);
            edtTitreLabel.setText(titre);
        }
        if (edtPeriodeLabel != null) {
            edtPeriodeLabel.setText("Du " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin());
        }
        if (aucunEdtLabel != null) {
            aucunEdtLabel.setVisible(false);
            aucunEdtLabel.setManaged(false);
        }

        // ✅ CORRECTION : Gérer l'affichage du bouton retour
        if (retourPrincipalButton != null) {
            // Le bouton retour est visible seulement si l'EDT affiché N'EST PAS le premier de la liste
            boolean estPrincipal = (tousEdts != null && !tousEdts.isEmpty() && 
                                    edt.getId() == tousEdts.get(0).getId());
            retourPrincipalButton.setVisible(!estPrincipal);
            System.out.println("   Bouton retour visible: " + !estPrincipal + " (estPrincipal=" + estPrincipal + ")");
        }

        grilleHeureDebut = edt.getHeureDebutConfig();
        grilleEcartHeures = edt.getEcartConfig();
        grilleHeureFin = edt.getHeureFinConfig();
        grilleJours = edt.getJoursConfigList();

        genererHeures();

        Task<List<Creneau>> task = new Task<>() {
            @Override
            protected List<Creneau> call() throws SQLException {
                List<Integer> creneauIds = edtCreneauDAO.listerCreneauxParEdt(edt.getId());
                List<Creneau> resultats = new ArrayList<>();
                for (int id : creneauIds) {
                    Creneau c = planningService.trouverCreneauParId(id);
                    if (c != null) resultats.add(c);
                }
                return resultats;
            }

            @Override
            protected void succeeded() {
                creneauxActuels = getValue();
                construireGrille();
                construireListeAutresEdts();
            }

            @Override
            protected void failed() {
                logger.error("Erreur chargement créneaux EDT", getException());
                afficherErreur("Impossible de charger les créneaux de l'emploi du temps");
            }
        };
        new Thread(task).start();
    }

    private void extraireConfigurationEdt() {
        if (creneauxActuels == null || creneauxActuels.isEmpty()) {
            grilleHeureDebut = 8;
            grilleHeureFin = 18;
            grilleEcartHeures = 2;
            grilleJours = Arrays.asList("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi");
            genererHeures();
            return;
        }

        Set<String> joursSet = new LinkedHashSet<>();
        for (Creneau c : creneauxActuels) {
            try {
                LocalDate date = LocalDate.parse(c.getJour());
                String nomJour = getNomJourFrancais(date.getDayOfWeek().getValue());
                joursSet.add(nomJour);
            } catch (Exception e) { }
        }

        List<String> joursTries = new ArrayList<>(joursSet);
        joursTries.sort((a, b) -> Integer.compare(ordreJours.getOrDefault(a, 7), ordreJours.getOrDefault(b, 7)));
        grilleJours = joursTries;

        Set<Integer> heuresDebutSet = new TreeSet<>();
        Set<Integer> heuresFinSet = new TreeSet<>();
        Set<Integer> ecartsSet = new TreeSet<>();

        for (Creneau c : creneauxActuels) {
            try {
                int hd = Integer.parseInt(c.getHeureDebut().split(":")[0]);
                int hf = Integer.parseInt(c.getHeureFin().split(":")[0]);
                heuresDebutSet.add(hd);
                heuresFinSet.add(hf);
                ecartsSet.add(hf - hd);
            } catch (Exception e) { }
        }

        if (!heuresDebutSet.isEmpty()) {
            grilleHeureDebut = heuresDebutSet.iterator().next();
        }
        if (!heuresFinSet.isEmpty()) {
            grilleHeureFin = Collections.max(heuresFinSet);
        }
        if (!ecartsSet.isEmpty()) {
            grilleEcartHeures = ecartsSet.iterator().next();
        }

        genererHeures();
    }

    private void genererHeures() {
        grilleHeures.clear();
        for (int h = grilleHeureDebut; h <= grilleHeureFin; h += grilleEcartHeures) {
            grilleHeures.add(String.format("%02d:00", h));
        }
    }

    private String getNomJourFrancais(int jourNumero) {
        switch (jourNumero) {
            case 1: return "Lundi";
            case 2: return "Mardi";
            case 3: return "Mercredi";
            case 4: return "Jeudi";
            case 5: return "Vendredi";
            case 6: return "Samedi";
            case 7: return "Dimanche";
            default: return "Lundi";
        }
    }

    private void construireGrille() {
        if (emploiTempsGrid == null) return;
        emploiTempsGrid.getChildren().clear();
        emploiTempsGrid.getColumnConstraints().clear();
        emploiTempsGrid.getRowConstraints().clear();

        int nbJours = grilleJours.size();
        int nbHeures = grilleHeures.size();

        if (nbJours == 0 || nbHeures == 0) {
            System.out.println("⚠️ Aucun jour ou heure à afficher");
            return;
        }

        ColumnConstraints ccHeures = new ColumnConstraints(80);
        ccHeures.setHgrow(Priority.NEVER);
        emploiTempsGrid.getColumnConstraints().add(ccHeures);

        for (int j = 0; j < nbJours; j++) {
            ColumnConstraints ccJour = new ColumnConstraints(150);
            ccJour.setHgrow(Priority.ALWAYS);
            emploiTempsGrid.getColumnConstraints().add(ccJour);
        }

        RowConstraints rcEntete = new RowConstraints(42);
        emploiTempsGrid.getRowConstraints().add(rcEntete);

        for (int h = 0; h < nbHeures; h++) {
            RowConstraints rc = new RowConstraints(65);
            rc.setVgrow(Priority.SOMETIMES);
            emploiTempsGrid.getRowConstraints().add(rc);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM", java.util.Locale.FRENCH);
        LocalDate periodeDebut = LocalDate.parse(edtActuel.getPeriodeDebut());

        // Coin vide
        Label coin = new Label();
        coin.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        coin.setStyle("-fx-background-color: #1A1F3A;");
        emploiTempsGrid.add(coin, 0, 0);

        // En-têtes jours
        for (int j = 0; j < nbJours; j++) {
            String nomJour = grilleJours.get(j);

            LocalDate dateJour = periodeDebut;
            while (!getNomJourFrancais(dateJour.getDayOfWeek().getValue()).equals(nomJour) &&
                   !dateJour.isAfter(LocalDate.parse(edtActuel.getPeriodeFin()))) {
                dateJour = dateJour.plusDays(1);
            }

            boolean estAujourdhui = dateJour.equals(LocalDate.now());

            Label entete = new Label(nomJour + " " + dateJour.format(fmt));
            entete.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            entete.setAlignment(Pos.CENTER);
            entete.setStyle("-fx-background-color: " + (estAujourdhui ? "#0D47A1" : "#1A1F3A") + "; " +
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
            emploiTempsGrid.add(entete, j + 1, 0);
        }

        // Lignes d'heures
        for (int h = 0; h < nbHeures; h++) {
            Label lblHeure = new Label(grilleHeures.get(h));
            lblHeure.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            lblHeure.setAlignment(Pos.CENTER);
            lblHeure.setStyle("-fx-background-color: #F8F9FF; -fx-text-fill: #1A1F3A; " +
                              "-fx-font-weight: bold; -fx-font-size: 11px; " +
                              "-fx-border-color: #E8EBFF; -fx-border-width: 0 1 1 0;");
            emploiTempsGrid.add(lblHeure, 0, h + 1);

            for (int j = 0; j < nbJours; j++) {
                StackPane cellule = new StackPane();
                cellule.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cellule.setStyle("-fx-background-color: white; -fx-border-color: #E8EBFF; -fx-border-width: 0 1 1 0;");
                emploiTempsGrid.add(cellule, j + 1, h + 1);
            }
        }

        if (creneauxActuels != null) {
            for (Creneau c : creneauxActuels) {
                placerCreneau(c);
            }
        }
    }

    private void placerCreneau(Creneau creneau) {
        try {
            LocalDate dateCreneau = LocalDate.parse(creneau.getJour());
            String nomJour = getNomJourFrancais(dateCreneau.getDayOfWeek().getValue());

            int jourIndex = grilleJours.indexOf(nomJour);
            if (jourIndex < 0) return;

            int hDebut = Integer.parseInt(creneau.getHeureDebut().split(":")[0]);
            int ligneDebut = (hDebut - grilleHeureDebut) / grilleEcartHeures;
            if (ligneDebut < 0 || ligneDebut >= grilleHeures.size()) return;

            int dureeHeures = calculerDureeHeures(creneau.getHeureDebut(), creneau.getHeureFin());
            int rowSpan = dureeHeures / grilleEcartHeures;
            if (rowSpan < 1) rowSpan = 1;

            if (ligneDebut + rowSpan > grilleHeures.size()) {
                rowSpan = grilleHeures.size() - ligneDebut;
            }

            StackPane cellule = trouverCellule(jourIndex + 1, ligneDebut + 1);
            if (cellule == null) return;

            GridPane.setRowSpan(cellule, rowSpan);

            Integer row = GridPane.getRowIndex(cellule);
            Integer col = GridPane.getColumnIndex(cellule);
            if (row != null && col != null) {
                for (int d = 1; d < rowSpan; d++) {
                    StackPane cell2 = trouverCellule(col, row + d);
                    if (cell2 != null) {
                        cell2.setVisible(false);
                        cell2.setManaged(false);
                    }
                }
            }

            remplirCellule(cellule, creneau, rowSpan);

        } catch (Exception e) {
            logger.warn("Erreur placement créneau: {}", e.getMessage());
        }
    }

    /**
     * REMPLIT UNE CELLULE AVEC LES NOUVELLES COULEURS
     */
    private void remplirCellule(StackPane cellule, Creneau creneau, int rowSpan) {
        try {
            Cours cours = coursService.trouverParId(creneau.getCoursId());
            if (cours == null) return;

            String typeCours = cours.getTypeCours();
            String nomMatiere = cacheMatieres.getOrDefault(creneau.getCoursId(), "Matière");
            String nomEnseignant = cacheEnseignants.getOrDefault(cours.getEnseignantId(), "Enseignant");
            String nomSalle = creneau.getSalleId() != null ?
                (cacheSalles.containsKey(creneau.getSalleId()) ?
                 cacheSalles.get(creneau.getSalleId()).getNumero() : "Salle " + creneau.getSalleId()) : "-";

            String couleur = COULEUR_CM;
            if ("TD".equals(typeCours)) couleur = COULEUR_TD;
            else if ("TP".equals(typeCours)) couleur = COULEUR_TP;

            boolean annule = "annule".equals(creneau.getStatut());
            boolean deplace = "deplace".equals(creneau.getStatut());

            if (annule) couleur = COULEUR_ANNULE;
            if (deplace) couleur = COULEUR_DEPLACE;

            String bgClair = couleur + "22";

            cellule.getChildren().clear();
            cellule.setStyle("-fx-background-color: " + bgClair + "; " +
                             "-fx-border-color: " + couleur + "; -fx-border-width: 0 0 0 4; " +
                             "-fx-cursor: hand;");

            // ✅ AJUSTEMENT DES TAILLES SELON rowSpan
            int fontSize;
            int paddingVertical;
            int spacing;
            
            if (rowSpan >= 4) {
                fontSize = 9;           // Très petit (4h ou plus)
                paddingVertical = 3;
                spacing = 1;
            } else if (rowSpan >= 3) {
                fontSize = 10;          // Petit (3h)
                paddingVertical = 4;
                spacing = 2;
            } else if (rowSpan >= 2) {
                fontSize = 11;          // Normal (2h)
                paddingVertical = 6;
                spacing = 2;
            } else {
                fontSize = 11;          // Normal (1h)
                paddingVertical = 8;
                spacing = 3;
            }

            VBox content = new VBox(spacing);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(paddingVertical, 4, paddingVertical, 6));
            content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            // ✅ Ligne 1 : Type + Matière
            HBox ligne1 = new HBox(3);
            ligne1.setAlignment(Pos.CENTER_LEFT);
            
            Label typeLbl = new Label("[" + typeCours + "]");
            typeLbl.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
            typeLbl.setMaxWidth(35);
            
            Label matLbl = new Label(nomMatiere);
            matLbl.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; -fx-text-fill: #1A1F3A;");
            matLbl.setWrapText(true);
            matLbl.setMaxWidth(100);
            
            ligne1.getChildren().addAll(typeLbl, matLbl);
            content.getChildren().add(ligne1);

            // ✅ Ligne 2 : Enseignant
            Label ensLbl = new Label("👨‍🏫 " + nomEnseignant);
            ensLbl.setStyle("-fx-font-size: " + (fontSize - 1) + "px; -fx-text-fill: #4361EE;");
            ensLbl.setWrapText(true);
            ensLbl.setMaxWidth(120);
            content.getChildren().add(ensLbl);

            // ✅ Ligne 3 : Salle
            Label salleLbl = new Label("🏫 " + nomSalle);
            salleLbl.setStyle("-fx-font-size: " + (fontSize - 1) + "px; -fx-text-fill: #7B83C4;");
            content.getChildren().add(salleLbl);

            // ✅ Ligne 4 : Groupe (si TD/TP)
            String groupes = cours.getGroupes();
            if (("TD".equals(typeCours) || "TP".equals(typeCours))
                    && groupes != null && !groupes.isEmpty() && !"Tous".equalsIgnoreCase(groupes)) {
                Label groupeLbl = new Label("👥 " + groupes);
                groupeLbl.setStyle("-fx-font-size: " + (fontSize - 1) + "px; -fx-font-weight: bold; -fx-text-fill: #F4A261;");
                content.getChildren().add(groupeLbl);
            }

            // ✅ Ligne 5 : Horaire (si assez de place)
            if (rowSpan >= 3) {
                Label horaireLbl = new Label(creneau.getHeureDebut() + "-" + creneau.getHeureFin());
                horaireLbl.setStyle("-fx-font-size: " + (fontSize - 2) + "px; -fx-text-fill: #7B83C4;");
                content.getChildren().add(horaireLbl);
            }

            if (annule) {
                Label annuleLbl = new Label("ANNULÉ");
                annuleLbl.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; -fx-text-fill: " + COULEUR_ANNULE + ";");
                content.getChildren().add(annuleLbl);
            }

            cellule.getChildren().add(content);

            Tooltip tip = new Tooltip(nomMatiere + "\n" + nomEnseignant + "\nSalle: " + nomSalle);
            Tooltip.install(cellule, tip);

        } catch (SQLException e) {
            logger.error("Erreur remplissage cellule", e);
        }
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

    private String getTitreEdt(EmploiDuTemps edt) {
        try {
            String titre = edtDAO.getTitre(edt.getId());
            if (titre != null && !titre.isEmpty()) return titre;
        } catch (Exception e) { /* ignorer */ }

        String type = edt.getPeriodeType();
        if (type != null) {
            type = type.substring(0, 1).toUpperCase() + type.substring(1);
            return type + " - " + edt.getPeriodeDebut() + " → " + edt.getPeriodeFin();
        }
        return "Emploi du temps #" + edt.getId();
    }

    // LISTE DES AUTRES EDT

    private void construireListeAutresEdts() {
        if (listeAutresEdtContainer == null) return;
        listeAutresEdtContainer.getChildren().clear();

        if (tousEdts == null || tousEdts.size() <= 1) {
            Label aucun = new Label("Aucun autre emploi du temps disponible.");
            aucun.setStyle("-fx-text-fill: #7B83C4; -fx-font-size: 12px;");
            listeAutresEdtContainer.getChildren().add(aucun);
            return;
        }

        int edtActuelId = edtActuel != null ? edtActuel.getId() : -1;

        for (EmploiDuTemps edt : tousEdts) {
            if (edt.getId() == edtActuelId) continue;

            HBox carte = creerCarteEdt(edt);
            listeAutresEdtContainer.getChildren().add(carte);
        }
    }

    private HBox creerCarteEdt(EmploiDuTemps edt) {
        HBox carte = new HBox(12);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setStyle("-fx-background-color: #F8F9FF; -fx-padding: 10 14; -fx-background-radius: 8; " +
                       "-fx-border-color: #E8EBFF; -fx-border-width: 1; -fx-border-radius: 8;");

        VBox infos = new VBox(3);
        infos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infos, Priority.ALWAYS);

        String titre = getTitreEdt(edt);
        Label titreLbl = new Label(titre);
        titreLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1A1F3A;");

        Label periodeLbl = new Label("Du " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin());
        periodeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #7B83C4;");

        infos.getChildren().addAll(titreLbl, periodeLbl);

        Button btnVoir = new Button("👁️ Voir");
        btnVoir.setStyle("-fx-background-color: #4361EE; -fx-text-fill: white; -fx-background-radius: 8; " +
                         "-fx-padding: 6 14; -fx-font-weight: bold; -fx-cursor: hand;");

        Button btnPDF = new Button("📄 PDF");
        btnPDF.setStyle("-fx-background-color: #E53E3E; -fx-text-fill: white; -fx-background-radius: 8; " +
                        "-fx-padding: 6 10; -fx-font-weight: bold; -fx-cursor: hand;");

        Button btnExcel = new Button("📊 Excel");
        btnExcel.setStyle("-fx-background-color: #2DC653; -fx-text-fill: white; -fx-background-radius: 8; " +
                          "-fx-padding: 6 10; -fx-font-weight: bold; -fx-cursor: hand;");

        final EmploiDuTemps edtFinal = edt;

        btnVoir.setOnAction(e -> {
            System.out.println("🔍 Voir EDT #" + edtFinal.getId());
            afficherEdt(edtFinal);
        });

        btnPDF.setOnAction(e -> exporterEdtPDF(edtFinal));
        btnExcel.setOnAction(e -> exporterEdtExcel(edtFinal));

        carte.getChildren().addAll(infos, btnVoir, btnPDF, btnExcel);
        return carte;
    }

    private void retournerEdtPrincipal() {
        if (tousEdts != null && !tousEdts.isEmpty()) {
            // Recharger l'EDT principal (le premier de la liste)
            afficherEdt(tousEdts.get(0));
            
            // Recharger la liste des autres EDT
            construireListeAutresEdts();
            
            // Cacher le bouton retour car on est revenu à l'EDT principal
            if (retourPrincipalButton != null) {
                retourPrincipalButton.setVisible(false);
            }
        }
    }

    private void afficherJourActuel() {
        if (edtActuel == null || creneauxActuels == null) return;
        LocalDate today = LocalDate.now();
        List<Creneau> filtre = new ArrayList<>();
        for (Creneau c : creneauxActuels) {
            try {
                if (LocalDate.parse(c.getJour()).equals(today)) {
                    filtre.add(c);
                }
            } catch (Exception e) { }
        }

        List<Creneau> sauvegarde = creneauxActuels;
        creneauxActuels = filtre;
        extraireConfigurationEdt();
        construireGrille();
        creneauxActuels = sauvegarde;
    }

    private void afficherSemaineActuelle() {
        if (edtActuel == null || creneauxActuels == null) return;
        LocalDate lundi = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate samedi = lundi.plusDays(5);

        List<Creneau> filtre = new ArrayList<>();
        for (Creneau c : creneauxActuels) {
            try {
                LocalDate d = LocalDate.parse(c.getJour());
                if (!d.isBefore(lundi) && !d.isAfter(samedi)) {
                    filtre.add(c);
                }
            } catch (Exception e) { }
        }

        List<Creneau> sauvegarde = creneauxActuels;
        creneauxActuels = filtre;
        extraireConfigurationEdt();
        construireGrille();
        creneauxActuels = sauvegarde;
    }

    private void afficherAucunEdt() {
        edtActuel = null;
        if (edtTitreLabel != null) edtTitreLabel.setText("Aucun emploi du temps");
        if (edtPeriodeLabel != null) edtPeriodeLabel.setText("Aucun emploi du temps disponible pour votre classe.");
        if (emploiTempsGrid != null) emploiTempsGrid.getChildren().clear();
        if (aucunEdtLabel != null) {
            aucunEdtLabel.setVisible(true);
            aucunEdtLabel.setManaged(true);
        }
        if (retourPrincipalButton != null) retourPrincipalButton.setVisible(false);
        construireListeAutresEdts();
    }

    // EXPORT

    private void exporterEdtPDF(EmploiDuTemps edt) {
        if (edt == null) {
            afficherErreur("Aucun emploi du temps à exporter");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("EDT_" + edt.getId() + ".pdf");
        File f = fc.showSaveDialog(primaryStage);
        if (f == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Integer> creneauIds = edtCreneauDAO.listerCreneauxParEdt(edt.getId());
                List<Creneau> creneaux = new ArrayList<>();
                for (int id : creneauIds) {
                    Creneau c = planningService.trouverCreneauParId(id);
                    if (c != null) creneaux.add(c);
                }

                String periode = edt.getPeriodeType() + " du " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin();
                exportService.exporterEDTGrillePDF(creneaux, classe, periode, utilisateurConnecte, f.getAbsolutePath(), edt);
                return null;
            }
            @Override
            protected void succeeded() {
                afficherNotification("Export PDF", "Fichier enregistré : " + f.getAbsolutePath());
            }
            @Override
            protected void failed() {
                afficherErreur("Erreur export PDF : " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    private void exporterEdtExcel(EmploiDuTemps edt) {
        if (edt == null) {
            afficherErreur("Aucun emploi du temps à exporter");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("EDT_" + edt.getId() + ".xlsx");
        File f = fc.showSaveDialog(primaryStage);
        if (f == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Integer> creneauIds = edtCreneauDAO.listerCreneauxParEdt(edt.getId());
                List<Creneau> creneaux = new ArrayList<>();
                for (int id : creneauIds) {
                    Creneau c = planningService.trouverCreneauParId(id);
                    if (c != null) creneaux.add(c);
                }

                String periode = edt.getPeriodeType() + " du " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin();
                exportService.exporterEDTGrilleExcel(creneaux, classe, periode, utilisateurConnecte, f.getAbsolutePath(), edt);
                return null;
            }
            @Override
            protected void succeeded() {
                afficherNotification("Export Excel", "Fichier enregistré : " + f.getAbsolutePath());
            }
            @Override
            protected void failed() {
                afficherErreur("Erreur export Excel : " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    // PLANNING DÉTAILLÉ

    private void configurerTableauPlanning() {
        if (planningTableView == null) return;

        // ✅ Vérifier que les colonnes existent avant de les configurer
        if (dateColumn != null) {
            dateColumn.setCellValueFactory(cd -> 
                javafx.beans.binding.Bindings.createStringBinding(
                    () -> formaterDate(cd.getValue().getJour())
                )
            );
        }
        
        if (horaireColumn != null) {
            horaireColumn.setCellValueFactory(cd -> 
                javafx.beans.binding.Bindings.createStringBinding(
                    () -> cd.getValue().getHeureDebut() + " - " + cd.getValue().getHeureFin()
                )
            );
        }
        
        if (matiereColumn != null) {
            matiereColumn.setCellValueFactory(cd -> 
                javafx.beans.binding.Bindings.createStringBinding(() -> {
                    try {
                        Cours cours = coursService.trouverParId(cd.getValue().getCoursId());
                        if (cours != null) {
                            Matiere m = matiereService.trouverParId(cours.getMatiereId());
                            return m != null ? m.getNom() : "Matière";
                        }
                    } catch (SQLException e) {
                        logger.error("Erreur", e);
                    }
                    return "Cours #" + cd.getValue().getCoursId();
                })
            );
        }
        
        if (salleColumn != null) {
            salleColumn.setCellValueFactory(cd -> 
                javafx.beans.binding.Bindings.createStringBinding(() -> {
                    Integer sId = cd.getValue().getSalleId();
                    if (sId == null) return "-";
                    Salle s = cacheSalles.get(sId);
                    return s != null ? s.getNumero() : "Salle " + sId;
                })
            );
        }
        
        planningTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void rechercherPlanningDetail() {
        if (etudiant == null) return;
        if (planningTableView == null) return;
        
        if (chargementIndicator != null) {
            chargementIndicator.setVisible(true);
        }
        
        Task<List<Creneau>> task = new Task<>() {
            @Override
            protected List<Creneau> call() throws SQLException {
                // Récupérer TOUS les créneaux de la classe (sans filtre de période)
                List<Creneau> tousCreneaux = planningService.getCreneauxParClasse(etudiant.getClasseId());
                
                // Filtrer : uniquement les créneaux NON TERMINÉS (date >= aujourd'hui)
                LocalDate aujourdhui = LocalDate.now();
                List<Creneau> resultats = new ArrayList<>();
                
                for (Creneau c : tousCreneaux) {
                    LocalDate dateCreneau = LocalDate.parse(c.getJour());
                    if (!dateCreneau.isBefore(aujourdhui)) {
                        resultats.add(c);
                    }
                }
                
                // Trier par date puis heure
                resultats.sort((c1, c2) -> {
                    int cmp = c1.getJour().compareTo(c2.getJour());
                    if (cmp == 0) {
                        cmp = c1.getHeureDebut().compareTo(c2.getHeureDebut());
                    }
                    return cmp;
                });
                
                return resultats;
            }
            
            @Override
            protected void succeeded() {
                List<Creneau> resultats = getValue();
                planningTableView.setItems(FXCollections.observableArrayList(resultats));
                
                if (chargementIndicator != null) {
                    chargementIndicator.setVisible(false);
                }
                
                if (resultats.isEmpty()) {
                    planningTableView.setPlaceholder(new Label("Aucun cours programmé à venir"));
                }
            }
            
            @Override
            protected void failed() {
                if (chargementIndicator != null) {
                    chargementIndicator.setVisible(false);
                }
                logger.error("Erreur planning détaillé", getException());
                planningTableView.setPlaceholder(new Label("Erreur de chargement"));
            }
        };
        
        new Thread(task).start();
    }

    // UTILITAIRES

    private String formaterDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) { return dateStr; }
    }

    @FXML
    protected void handleCarte() {
        try {
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
}