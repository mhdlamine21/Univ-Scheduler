package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import scheduler.dao.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur de création d'un emploi du temps en 5 étapes.
 * Étape 1  : Type + Période
 * Étape 1b : Configuration horaires et jours (NOUVEAU)
 * Étape 2  : Classe + Cours
 * Étape 3  : Grille EDT (placement des créneaux)
 * Étape 4  : Validation + Titre + Envoi
 */
public class NouvelEdtControleur extends TableauBordControleur {

    @FXML private Label etapeTitreLabel;
    @FXML private Button annulerButton;

    @FXML private Circle cercle1, cercle2, cercle3, cercle4, cercle5;
    @FXML private Rectangle ligne1, ligne2, ligne3, ligne4;
    @FXML private Label label2, label3, label4, label5;

    @FXML private ScrollPane etape1Pane;
    @FXML private VBox carteHebdo, carteMensuel, carteSemestriel;
    @FXML private HBox controleHebdo, controleMensuel, controleSemestriel;
    @FXML private ComboBox<String> semaineCombo, anneeCombo, moisCombo, anneeMoisCombo;
    @FXML private DatePicker dateDebutPicker, dateFinPicker;
    @FXML private Label resumePeriodeLabel;
    @FXML private Button etape1SuivantButton;

    @FXML private ScrollPane etape1bPane;
    @FXML private ComboBox<String> heureDebutGrilleCombo;
    @FXML private ComboBox<String> heureFinGrilleCombo;
    @FXML private ComboBox<String> ecartHeuresCombo;
    @FXML private Label apercuCreneauxLabel;
    @FXML private Label apercuJoursLabel;
    @FXML private CheckBox jourLundi, jourMardi, jourMercredi, jourJeudi, jourVendredi, jourSamedi;
    @FXML private Button btnLunVen, btnLunSam, btnToutDesel;
    @FXML private Button etape1bPrecedentButton, etape1bSuivantButton;

    @FXML private ScrollPane etape2Pane;
    @FXML private ComboBox<Classe> classeCombo;
    @FXML private Label classeInfoLabel, nbCoursLabel;
    @FXML private TableView<CoursWrapper> coursTable;
    @FXML private TableColumn<CoursWrapper, Boolean> coursSelectCol;
    @FXML private TableColumn<CoursWrapper, String> coursTypeCol, coursMatiereCol,
            coursEnseignantCol, coursVolumeCol, coursGroupesCol;
    @FXML private Button toutSelectButton, toutDeselButton;
    @FXML private Button etape2PrecedentButton, etape2SuivantButton;

    @FXML private VBox etape3Pane;
    @FXML private Label grilleClasseLabel, grillePeriodeLabel, grilleConfigLabel, nbCreneauxLabel;
    @FXML private ScrollPane grilleScrollPane;
    @FXML private GridPane grilleEDT;
    @FXML private Button etape3PrecedentButton, enregistrerBrouillonButton, etape3SuivantButton;

    @FXML private ScrollPane etape4Pane;
    @FXML private TextField titreEdtField;
    @FXML private Label resumeClasseLabel, resumeTypeLabel, resumePeriodeLabel2,
                        resumeCreneauxLabel, resumeConfigLabel;
    @FXML private Button verifierConflitsButton;
    @FXML private Label conflitsStatusLabel;
    @FXML private TextArea conflitsDetailArea;
    @FXML private CheckBox envoyerEtudiantsCheck, envoyerProfsCheck;
    @FXML private Label destinatairesLabel;
    @FXML private Button etape4PrecedentButton, validerEdtButton;
    @FXML private ProgressIndicator chargementIndicator;

    private ClasseService classeService;
    private CoursService coursService;
    private MatiereService matiereService;
    private UtilisateurService utilisateurService;
    private PlanningService planningService;
    private ConflitService conflitService;
    private NotificationService notificationService;
    private EmailService emailService;
    private UfrService ufrService;
    private BatimentService batimentService;
    private SalleService salleService;
    private ExportService exportService;

    private int etapeActuelle = 1;
    private String typePeriode = "hebdomadaire";
    private String periodeDebut, periodeFin;
    private Classe classeSelectionnee;
    private List<Cours> coursSelectionnes = new ArrayList<>();
    private ObservableList<CoursWrapper> coursListeWrapper = FXCollections.observableArrayList();
    private List<CreneauTemp> creneauxTemp = new ArrayList<>();
    private Map<Integer, String> cacheMatieres = new HashMap<>();
    private Map<Integer, String> cacheEnseignants = new HashMap<>();
    private boolean conflitsVerifies = false;

    private int grilleHeureDebut = 8;
    private int grilleHeureFin = 18;
    private int grilleEcartHeures = 2;
    private List<String> grilleJours = new ArrayList<>(
        Arrays.asList("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi")
    );

    private static final String COULEUR_CM = "#4CAF50";
    private static final String COULEUR_TD = "#2196F3";
    private static final String COULEUR_TP = "#FF9800";

    public static class CoursWrapper {
        private final Cours cours;
        private final SimpleBooleanProperty selectionne;
        private String matiereNom;
        private String enseignantNom;

        public CoursWrapper(Cours cours, boolean selectionne, String matiereNom, String enseignantNom) {
            this.cours = cours;
            this.selectionne = new SimpleBooleanProperty(selectionne);
            this.matiereNom = matiereNom;
            this.enseignantNom = enseignantNom;
        }

        public Cours getCours() { return cours; }
        public boolean isSelectionne() { return selectionne.get(); }
        public void setSelectionne(boolean v) { selectionne.set(v); }
        public SimpleBooleanProperty selectionneProperty() { return selectionne; }
        public String getMatiereNom() { return matiereNom; }
        public String getEnseignantNom() { return enseignantNom; }
    }

    public static class CreneauTemp {
        public Cours cours;
        public String jour;
        public String heureDebut;
        public String heureFin;
        public Salle salle;
        public int ligneGrille;
        public int colonneGrille;
        public int dureeHeures;
        public String matiereNom;
        public String enseignantNom;  

        public CreneauTemp(Cours cours, String jour, String heureDebut,
                           String heureFin, Salle salle, int duree, String matiereNom,
                           String enseignantNom) {
            this.cours = cours;
            this.jour = jour;
            this.heureDebut = heureDebut;
            this.heureFin = heureFin;
            this.salle = salle;
            this.dureeHeures = duree;
            this.matiereNom = matiereNom;
            this.enseignantNom = enseignantNom;  
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        classeService = new ClasseService();
        coursService = new CoursService();
        matiereService = new MatiereService();
        utilisateurService = new UtilisateurService();
        planningService = new PlanningService();
        conflitService = new ConflitService();
        notificationService = new NotificationService();
        emailService = new EmailService();
        ufrService = new UfrService();
        batimentService = new BatimentService();
        salleService = new SalleService();
        exportService = new ExportService();
    }

    @Override
    protected void initialiserTableauBord() {
        configurerEtape1();
        configurerEtape1b();
        configurerEtape2();
        configurerEtape3();
        configurerEtape4();
        configurerBoutonAnnuler();
        afficherEtape(1);
    }

    @Override
    protected void rafraichirDonnees() {}

    
    private void configurerEtape1() {
        carteHebdo.setOnMouseClicked(e -> selectionnerType("hebdomadaire"));
        carteMensuel.setOnMouseClicked(e -> selectionnerType("mensuel"));
        carteSemestriel.setOnMouseClicked(e -> selectionnerType("semestriel"));

        int anneeCourante = LocalDate.now().getYear();
        for (int i = anneeCourante - 1; i <= anneeCourante + 3; i++) {
            anneeCombo.getItems().add(String.valueOf(i));
            anneeMoisCombo.getItems().add(String.valueOf(i));
        }
        anneeCombo.setValue(String.valueOf(anneeCourante));
        anneeMoisCombo.setValue(String.valueOf(anneeCourante));

        chargerSemaines(anneeCourante);
        anneeCombo.setOnAction(e -> {
            if (anneeCombo.getValue() != null)
                chargerSemaines(Integer.parseInt(anneeCombo.getValue()));
        });

        String[] MOIS = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                         "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        moisCombo.getItems().addAll(MOIS);
        moisCombo.setValue(MOIS[LocalDate.now().getMonthValue() - 1]);

        semaineCombo.setOnAction(e -> mettreAJourResumePeriode());
        moisCombo.setOnAction(e -> mettreAJourResumePeriode());
        anneeMoisCombo.setOnAction(e -> mettreAJourResumePeriode());
        if (dateDebutPicker != null) dateDebutPicker.setOnAction(e -> mettreAJourResumePeriode());
        if (dateFinPicker != null) dateFinPicker.setOnAction(e -> mettreAJourResumePeriode());

        selectionnerType("hebdomadaire");

        etape1SuivantButton.setOnAction(e -> {
            if (validerEtape1()) {
                calculerPeriode();
                afficherEtape(2); // → étape 1b
            }
        });
    }

    /**
     * Sélectionne le type de période (hebdomadaire, mensuel, semestriel).
     * @param type le type sélectionné
     */
    private void selectionnerType(String type) {
        this.typePeriode = type;

        String styleNormal = "-fx-background-color: white; -fx-padding: 25; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.08),6,0,0,2); " +
                "-fx-cursor: hand; -fx-border-color: #E2E8F0; -fx-border-width: 2; -fx-border-radius: 12;";
        String styleActif = "-fx-background-color: #E3F2FD; -fx-padding: 25; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.12),8,0,0,3); " +
                "-fx-cursor: hand; -fx-border-color: #1565C0; -fx-border-width: 3; -fx-border-radius: 12;";

        carteHebdo.setStyle(type.equals("hebdomadaire") ? styleActif : styleNormal);
        carteMensuel.setStyle(type.equals("mensuel") ? styleActif : styleNormal);
        carteSemestriel.setStyle(type.equals("semestriel") ? styleActif : styleNormal);

        controleHebdo.setVisible(type.equals("hebdomadaire"));
        controleHebdo.setManaged(type.equals("hebdomadaire"));
        controleMensuel.setVisible(type.equals("mensuel"));
        controleMensuel.setManaged(type.equals("mensuel"));
        controleSemestriel.setVisible(type.equals("semestriel"));
        controleSemestriel.setManaged(type.equals("semestriel"));

        mettreAJourResumePeriode();
    }

    private void chargerSemaines(int annee) {
        semaineCombo.getItems().clear();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 1; i <= 52; i++) {
            LocalDate debut = getPremierJourSemaine(annee, i);
            LocalDate fin = debut.plusDays(5);
            semaineCombo.getItems().add(String.format("Semaine %d (%s → %s)",
                    i, debut.format(fmt), fin.format(fmt)));
        }
        int semCourante = (LocalDate.now().getDayOfYear() / 7) + 1;
        if (semCourante > 0 && semCourante <= semaineCombo.getItems().size())
            semaineCombo.getSelectionModel().select(semCourante - 1);
    }

    private void mettreAJourResumePeriode() {
        try {
            String resume = "";
            DateTimeFormatter affichage = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            if ("hebdomadaire".equals(typePeriode) && semaineCombo.getValue() != null
                    && anneeCombo.getValue() != null) {
                String sem = semaineCombo.getValue();
                int num = Integer.parseInt(sem.split(" ")[1]);
                LocalDate debut = getPremierJourSemaine(Integer.parseInt(anneeCombo.getValue()), num);
                LocalDate fin = debut.plusDays(5);
                resume = "📅 Du " + debut.format(affichage) + " au " + fin.format(affichage) + " (6 jours)";
                periodeDebut = debut.toString();
                periodeFin = fin.toString();

            } else if ("mensuel".equals(typePeriode) && moisCombo.getValue() != null
                    && anneeMoisCombo.getValue() != null) {
                int moisIdx = moisCombo.getItems().indexOf(moisCombo.getValue()) + 1;
                int annee = Integer.parseInt(anneeMoisCombo.getValue());
                LocalDate debut = LocalDate.of(annee, moisIdx, 1);
                LocalDate fin = debut.plusMonths(1).minusDays(1);
                resume = "🗓️ " + moisCombo.getValue() + " " + annee
                        + " (" + debut.format(affichage) + " → " + fin.format(affichage) + ")";
                periodeDebut = debut.toString();
                periodeFin = fin.toString();

            } else if ("semestriel".equals(typePeriode)
                    && dateDebutPicker != null && dateDebutPicker.getValue() != null
                    && dateFinPicker != null && dateFinPicker.getValue() != null) {
                periodeDebut = dateDebutPicker.getValue().toString();
                periodeFin = dateFinPicker.getValue().toString();
                resume = "📚 Du " + dateDebutPicker.getValue().format(affichage)
                        + " au " + dateFinPicker.getValue().format(affichage);
            }
            if (resumePeriodeLabel != null) {
                resumePeriodeLabel.setText(resume);
                resumePeriodeLabel.setVisible(!resume.isEmpty());
                resumePeriodeLabel.setManaged(!resume.isEmpty());
            }
        } catch (Exception ignored) {}
    }

    /**
     * Valide l'étape 1 (période).
     * @return true si la période est valide
     */
    private boolean validerEtape1() {
        if (periodeDebut == null || periodeFin == null) {
            afficherErreur("Veuillez définir une période complète.");
            return false;
        }
        LocalDate debut = LocalDate.parse(periodeDebut);
        LocalDate fin = LocalDate.parse(periodeFin);
        if (!fin.isAfter(debut)) {
            afficherErreur("La date de fin doit être après la date de début.");
            return false;
        }
        return true;
    }

    private void calculerPeriode() {}

    /**
     * Valide l'étape 1b (configuration horaires et jours).
     * @return true si la configuration est valide
     */
    private void configurerEtape1b() {
        for (int h = 6; h <= 18; h++) {
            String label = String.format("%02d:00", h);
            if (heureDebutGrilleCombo != null) heureDebutGrilleCombo.getItems().add(label);
        }
        if (heureDebutGrilleCombo != null) heureDebutGrilleCombo.setValue("08:00");
        
        if (heureFinGrilleCombo != null) {
            heureFinGrilleCombo.setVisible(false);
            heureFinGrilleCombo.setManaged(false);
        }

        if (ecartHeuresCombo != null) {
            ecartHeuresCombo.getItems().addAll("1h", "2h");
            ecartHeuresCombo.setValue("1h");
            
            ecartHeuresCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                mettreAJourApercuCreneaux();
            });
        }

        // Listeners pour mettre à jour l'aperçu
        if (heureDebutGrilleCombo != null)
            heureDebutGrilleCombo.valueProperty().addListener((obs, o, n) -> mettreAJourApercuCreneaux());
        if (ecartHeuresCombo != null)
            ecartHeuresCombo.valueProperty().addListener((obs, o, n) -> mettreAJourApercuCreneaux());

        // Jours listeners
        CheckBox[] joursChecks = {jourLundi, jourMardi, jourMercredi, jourJeudi, jourVendredi, jourSamedi};
        for (CheckBox cb : joursChecks) {
            if (cb != null) cb.selectedProperty().addListener((obs, o, n) -> mettreAJourApercuJours());
        }

        if (btnLunVen != null) btnLunVen.setOnAction(e -> {
            setJours(true, true, true, true, true, false);
        });
        if (btnLunSam != null) btnLunSam.setOnAction(e -> {
            setJours(true, true, true, true, true, true);
        });
        if (btnToutDesel != null) btnToutDesel.setOnAction(e -> {
            setJours(false, false, false, false, false, false);
        });

        if (etape1bPrecedentButton != null) etape1bPrecedentButton.setOnAction(e -> afficherEtape(1));
        if (etape1bSuivantButton != null) etape1bSuivantButton.setOnAction(e -> {
            if (validerEtape1b()) {
                collecterConfigGrille();
                afficherEtape(3); 
            }
        });

        mettreAJourApercuCreneaux();
        mettreAJourApercuJours();
    }

    private void setJours(boolean lun, boolean mar, boolean mer, boolean jeu, boolean ven, boolean sam) {
        if (jourLundi != null) jourLundi.setSelected(lun);
        if (jourMardi != null) jourMardi.setSelected(mar);
        if (jourMercredi != null) jourMercredi.setSelected(mer);
        if (jourJeudi != null) jourJeudi.setSelected(jeu);
        if (jourVendredi != null) jourVendredi.setSelected(ven);
        if (jourSamedi != null) jourSamedi.setSelected(sam);
        mettreAJourApercuJours();
    }

    private void mettreAJourApercuCreneaux() {
        if (apercuCreneauxLabel == null) return;
        try {
            int hDebut = parseHeure(heureDebutGrilleCombo.getValue());
            double ecart = parseEcart(ecartHeuresCombo.getValue());
            
            int hFin = 18;
            
            if (hDebut >= hFin) {
                apercuCreneauxLabel.setText("⚠️ L'heure de début doit être avant 18h.");
                return;
            }

            StringBuilder sb = new StringBuilder("Créneaux : ");
            double h = hDebut;
            List<String> creneaux = new ArrayList<>();
            
            while (h <= hFin - ecart) {
                String heureDebutStr = formatHeure(h);
                String heureFinStr = formatHeure(Math.min(h + ecart, hFin));
                creneaux.add(heureDebutStr + " → " + heureFinStr);
                h += ecart;
            }
            
            sb.append(String.join("  |  ", creneaux));
            sb.append("  (" + creneaux.size() + " créneau(x))");
            apercuCreneauxLabel.setText(sb.toString());
            
            if (ecart == 2.0) {
                Tooltip.install(apercuCreneauxLabel, new Tooltip("Avec un espacement de 2h, les cours doivent avoir une durée paire (2h, 4h, 6h...)"));
            } else {
                Tooltip.install(apercuCreneauxLabel, null);
            }
            
        } catch (Exception e) {
            apercuCreneauxLabel.setText("Configurez les heures ci-dessus.");
        }
    }

    private void mettreAJourApercuJours() {
        if (apercuJoursLabel == null) return;
        List<String> joursSelectionnes = getJoursSelectionnes();
        if (joursSelectionnes.isEmpty()) {
            apercuJoursLabel.setText("⚠️ Aucun jour sélectionné");
        } else {
            apercuJoursLabel.setText("📅 Jours : " + String.join(", ", joursSelectionnes)
                    + "  (" + joursSelectionnes.size() + " jour(s))");
        }
    }

    private List<String> getJoursSelectionnes() {
        List<String> jours = new ArrayList<>();
        if (jourLundi != null && jourLundi.isSelected()) jours.add("Lundi");
        if (jourMardi != null && jourMardi.isSelected()) jours.add("Mardi");
        if (jourMercredi != null && jourMercredi.isSelected()) jours.add("Mercredi");
        if (jourJeudi != null && jourJeudi.isSelected()) jours.add("Jeudi");
        if (jourVendredi != null && jourVendredi.isSelected()) jours.add("Vendredi");
        if (jourSamedi != null && jourSamedi.isSelected()) jours.add("Samedi");
        return jours;
    }

    private boolean validerEtape1b() {
        List<String> jours = getJoursSelectionnes();
        if (jours.isEmpty()) {
            afficherErreur("Veuillez sélectionner au moins un jour.");
            return false;
        }
        try {
            int hDebut = parseHeure(heureDebutGrilleCombo.getValue());
            if (hDebut >= 18) {
                afficherErreur("L'heure de début doit être avant 18h.");
                return false;
            }
        } catch (Exception e) {
            afficherErreur("Configuration des heures invalide.");
            return false;
        }
        return true;
    }

    /**
     * Récupère la configuration de la grille depuis les sélections.
     */
    private void collecterConfigGrille() {
        grilleHeureDebut = parseHeure(heureDebutGrilleCombo.getValue());
        grilleHeureFin = 18; // ✅ FIXE à 18h
        grilleEcartHeures = (int) Math.max(1, parseEcart(ecartHeuresCombo.getValue()));
        grilleJours = getJoursSelectionnes();
        
        System.out.println("📅 Configuration EDT - Début: " + grilleHeureDebut + 
                           "h, Fin: 18h, Écart: " + grilleEcartHeures + "h");
    }

    private int parseHeure(String valeur) {
        if (valeur == null) return 8;
        return Integer.parseInt(valeur.split(":")[0]);
    }

    private double parseEcart(String valeur) {
        if (valeur == null) return 2;
        switch (valeur) {
            case "1h": return 1;
            case "1h30": return 1.5;
            case "2h": return 2;
            case "3h": return 3;
            default: return 2;
        }
    }

    private String formatHeure(double h) {
        int heures = (int) h;
        int minutes = (int) ((h - heures) * 60);
        return String.format("%02d:%02d", heures, minutes);
    }

    
    private void configurerEtape2() {
        Task<List<Classe>> taskClasses = new Task<>() {
            @Override protected List<Classe> call() throws SQLException {
                return classeService.listerToutes();
            }
            @Override protected void succeeded() {
                classeCombo.setCellFactory(lv -> new ListCell<>() {
                    @Override protected void updateItem(Classe c, boolean empty) {
                        super.updateItem(c, empty);
                        setText(empty || c == null ? null :
                                c.getIntitule() + " - " + c.getAnneeScolaire());
                    }
                });
                classeCombo.setButtonCell(new ListCell<>() {
                    @Override protected void updateItem(Classe c, boolean empty) {
                        super.updateItem(c, empty);
                        setText(empty || c == null ? null :
                                c.getIntitule() + " - " + c.getAnneeScolaire());
                    }
                });
                classeCombo.getItems().setAll(getValue());
            }
        };
        new Thread(taskClasses).start();

        classeCombo.setOnAction(e -> {
            Classe c = classeCombo.getValue();
            if (c != null) {
                classeSelectionnee = c;
                classeInfoLabel.setText(c.getEffectif() + " étudiants · " + c.getNbGroupes() + " groupe(s)");
                chargerCoursClasse(c.getId());
            }
        });

        coursSelectCol.setCellValueFactory(cd -> cd.getValue().selectionneProperty());
        coursSelectCol.setCellFactory(col -> new CheckBoxTableCell<>());
        coursSelectCol.setEditable(true);
        coursTable.setEditable(true);

        coursTypeCol.setCellValueFactory(cd ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> cd.getValue().getCours().getTypeCours()));
        coursMatiereCol.setCellValueFactory(cd ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> cd.getValue().getMatiereNom()));
        coursEnseignantCol.setCellValueFactory(cd ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> cd.getValue().getEnseignantNom()));
        coursVolumeCol.setCellValueFactory(cd ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> cd.getValue().getCours().getVolumeHoraire() + "h"));
        coursGroupesCol.setCellValueFactory(cd ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> cd.getValue().getCours().getGroupes() != null ?
                                cd.getValue().getCours().getGroupes() : "Tous"));

        coursTypeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String bg = COULEUR_CM;
                if ("TD".equals(item)) bg = COULEUR_TD;
                else if ("TP".equals(item)) bg = COULEUR_TP;
                setStyle("-fx-background-color: " + bg + "33; -fx-font-weight: bold; " +
                         "-fx-text-fill: " + bg + ";");
            }
        });

        coursTable.setItems(coursListeWrapper);

        toutSelectButton.setOnAction(e -> coursListeWrapper.forEach(w -> w.setSelectionne(true)));
        toutDeselButton.setOnAction(e -> coursListeWrapper.forEach(w -> w.setSelectionne(false)));

        etape2PrecedentButton.setOnAction(e -> afficherEtape(2)); // retour à étape 1b
        etape2SuivantButton.setOnAction(e -> {
            if (validerEtape2()) {
                collecterCoursSelectionnes();
                chargerCachesAsync(() -> {
                    construireGrille();
                    afficherEtape(4); // → étape 3 (grille)
                });
            }
        });
    }

    private void chargerCoursClasse(int classeId) {
        Task<List<Cours>> task = new Task<>() {
            @Override protected List<Cours> call() throws SQLException {
                return coursService.listerParClasse(classeId);
            }
            @Override protected void succeeded() {
                coursListeWrapper.clear();
                List<Cours> liste = getValue();
                nbCoursLabel.setText(liste.size() + " cours");
                for (Cours c : liste) {
                    String matNom = "...";
                    String ensNom = "...";
                    try {
                        Matiere m = matiereService.trouverParId(c.getMatiereId());
                        if (m != null) matNom = m.getNom();
                        cacheMatieres.put(c.getMatiereId(), matNom);
                    } catch (SQLException ignored) {}
                    try {
                    	Utilisateur u = utilisateurService.trouverParId(c.getEnseignantId());
                        if (u != null) {
                            // ✅ CORRECTION : Utiliser seulement grade + nom
                            if (u instanceof Enseignant) {
                                Enseignant e = (Enseignant) u;
                                String grade = e.getGrade();
                                String nom = e.getNom();
                                ensNom = (grade != null && !grade.isEmpty() ? grade + " " : "") + nom;
                            } else {
                                ensNom = u.getNom();  // ← seulement le nom
                            }
                        }
                    } catch (SQLException ignored) {}
                    coursListeWrapper.add(new CoursWrapper(c, true, matNom, ensNom));
                }
            }
        };
        new Thread(task).start();
    }

    private boolean validerEtape2() {
        if (classeSelectionnee == null) {
            afficherErreur("Veuillez sélectionner une classe.");
            return false;
        }
        long nbSel = coursListeWrapper.stream().filter(CoursWrapper::isSelectionne).count();
        if (nbSel == 0) {
            afficherErreur("Sélectionnez au moins un cours.");
            return false;
        }
        return true;
    }

    private void collecterCoursSelectionnes() {
        coursSelectionnes = coursListeWrapper.stream()
                .filter(CoursWrapper::isSelectionne)
                .map(CoursWrapper::getCours)
                .collect(Collectors.toList());
    }

    private void chargerCachesAsync(Runnable apres) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws SQLException {
                for (Cours c : coursSelectionnes) {
                    // Charger la matière
                    if (!cacheMatieres.containsKey(c.getMatiereId())) {
                        Matiere m = matiereService.trouverParId(c.getMatiereId());
                        cacheMatieres.put(c.getMatiereId(), m != null ? m.getNom() : "?");
                    }
                    
                    // ✅ Charger l'enseignant avec grade + nom (ex: "Mr. Fall")
                    if (!cacheEnseignants.containsKey(c.getEnseignantId())) {
                        Utilisateur u = utilisateurService.trouverParId(c.getEnseignantId());
                        if (u instanceof Enseignant) {
                            Enseignant e = (Enseignant) u;
                            String grade = e.getGrade();
                            String nom = e.getNom();  // ← Utilise getNom(), pas getPrenom()
                            
                            if (grade != null && !grade.isEmpty() && nom != null && !nom.isEmpty()) {
                                cacheEnseignants.put(c.getEnseignantId(), grade + " " + nom);
                            } else {
                                cacheEnseignants.put(c.getEnseignantId(), nom != null ? nom : "Enseignant");
                            }
                        } else if (u != null) {
                            cacheEnseignants.put(c.getEnseignantId(), u.getNom());
                        } else {
                            cacheEnseignants.put(c.getEnseignantId(), "Enseignant inconnu");
                        }
                    }
                }
                return null;
            }
            @Override protected void succeeded() {
                javafx.application.Platform.runLater(apres);
            }
        };
        new Thread(task).start();
    }

    
    private void configurerEtape3() {
        etape3PrecedentButton.setOnAction(e -> afficherEtape(3)); // retour étape 2 (classe)
        enregistrerBrouillonButton.setOnAction(e -> enregistrerBrouillon());
        etape3SuivantButton.setOnAction(e -> {
            mettreAJourResumeEtape4();
            afficherEtape(5); // → étape 4 (validation)
        });
    }

    /**
     * Construit la grille EDT avec les paramètres configurés.
     */
    private void construireGrille() {
        grilleEDT.getChildren().clear();
        grilleEDT.getColumnConstraints().clear();
        grilleEDT.getRowConstraints().clear();
        creneauxTemp.clear();

        grilleClasseLabel.setText("Classe : " + classeSelectionnee.getIntitule());
        grillePeriodeLabel.setText("Période : " + periodeDebut + " → " + periodeFin);
        if (grilleConfigLabel != null) {
            grilleConfigLabel.setText("⏰ " + formatHeure(grilleHeureDebut) + "-"
                    + formatHeure(grilleHeureFin) + " | écart " + ecartHeuresCombo.getValue()
                    + " | " + grilleJours.size() + " jours");
        }

        // Générer les créneaux horaires selon la configuration
        List<String> creneauxHoraires = genererCreneauxHoraires();
        int nbJours = grilleJours.size();
        int nbCreneaux = creneauxHoraires.size();

        // Colonne 0 : heures
        ColumnConstraints ccHeure = new ColumnConstraints(80);
        ccHeure.setHgrow(Priority.NEVER);
        grilleEDT.getColumnConstraints().add(ccHeure);

        // Colonnes jours
        for (int j = 0; j < nbJours; j++) {
            ColumnConstraints cc = new ColumnConstraints(165);
            cc.setHgrow(Priority.ALWAYS);
            grilleEDT.getColumnConstraints().add(cc);
        }

        // Ligne 0 : entêtes
        RowConstraints rcEntete = new RowConstraints(42);
        grilleEDT.getRowConstraints().add(rcEntete);

        // Coin vide
        Label coinVide = new Label();
        coinVide.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        coinVide.setStyle("-fx-background-color: #1565C0;");
        grilleEDT.add(coinVide, 0, 0);

        // En-têtes des jours
        for (int j = 0; j < nbJours; j++) {
            Label enteteJour = new Label(grilleJours.get(j));
            enteteJour.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            enteteJour.setAlignment(Pos.CENTER);
            enteteJour.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; " +
                                "-fx-font-weight: bold; -fx-font-size: 13px;");
            grilleEDT.add(enteteJour, j + 1, 0);
        }

        // Lignes des créneaux
        // Hauteur minimale par créneau : 50px de base, plus grand pour les petits écarts
        int hauteurLigne = 55;
        if (grilleEcartHeures <= 1) hauteurLigne = 45;

        for (int h = 0; h < nbCreneaux; h++) {
            RowConstraints rc = new RowConstraints(hauteurLigne);
            rc.setVgrow(Priority.SOMETIMES);
            grilleEDT.getRowConstraints().add(rc);

            String heureLabel = creneauxHoraires.get(h);

            // Label heure
            Label lblHeure = new Label(heureLabel);
            lblHeure.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            lblHeure.setAlignment(Pos.CENTER);
            lblHeure.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0; " +
                              "-fx-font-weight: bold; -fx-font-size: 12px; " +
                              "-fx-border-color: #BBDEFB; -fx-border-width: 0 1 1 0;");
            grilleEDT.add(lblHeure, 0, h + 1);

            // Cellules pour chaque jour
            for (int j = 0; j < nbJours; j++) {
                final int jourIdx = j;
                final int heureIdx = h;
                final String heureDebut = heureLabel;

                StackPane cellule = new StackPane();
                cellule.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cellule.setStyle("-fx-background-color: white; " +
                                 "-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 0;");
                cellule.setUserData(new int[]{jourIdx, heureIdx});

                cellule.setOnMouseEntered(e -> {
                    if (cellule.getChildren().isEmpty())
                        cellule.setStyle("-fx-background-color: #F0F7FF; " +
                                         "-fx-border-color: #BBDEFB; -fx-border-width: 0 1 1 0;");
                });
                cellule.setOnMouseExited(e -> {
                    if (cellule.getChildren().isEmpty())
                        cellule.setStyle("-fx-background-color: white; " +
                                         "-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 0;");
                });

                cellule.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        ouvrirDialogueCreneau(jourIdx, heureIdx, heureDebut, cellule);
                    }
                });

                grilleEDT.add(cellule, j + 1, h + 1);
            }
        }
        mettreAJourNbCreneaux();
    }

    /**
     * Génère la liste des créneaux horaires (heure de début de chaque case).
     */
    private List<String> genererCreneauxHoraires() {
        List<String> creneaux = new ArrayList<>();
        double h = grilleHeureDebut;
        // ✅ S'arrêter à 18h
        while (h <= 18) {
            creneaux.add(formatHeure(h));
            h += grilleEcartHeures;
        }
        return creneaux;
    }
    /**
     * Calcule le nombre de cases occupées par un cours selon sa durée et l'écart configuré.
     */
    private int calculerRowSpan(String heureDebut, String heureFin) {
        try {
            double debut = parseHeureDouble(heureDebut);
            double fin = parseHeureDouble(heureFin);
            double duree = fin - debut;
            // Nombre de cases = durée en heures / écart entre créneaux
            // Pour un cours de 9h à 11h avec écart 1h : duree=2, rowSpan=2 cases (9h,10h)
            // Pour un cours de 9h à 12h avec écart 1h : duree=3, rowSpan=3 cases (9h,10h,11h)
            int rowSpan = (int) Math.round(duree / grilleEcartHeures);
            return Math.max(1, rowSpan);
        } catch (Exception e) {
            return 1;
        }
    }

    private double parseHeureDouble(String heure) {
        if (heure == null) return 0;
        String[] parts = heure.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return h + m / 60.0;
    }
    
    /* Vérifie les conflits avant validation
    */
   private boolean verifierConflitsAvantValidation() {
       if (creneauxTemp.isEmpty()) {
           afficherErreur("Aucun créneau à valider");
           return false;
       }
       
       List<ConflitService.ResultatConflit> tousConflits = new ArrayList<>();
       
       for (CreneauTemp ct : creneauxTemp) {
           Creneau c = new Creneau();
           c.setCoursId(ct.cours.getId());
           c.setJour(calculerDateJour(ct.jour, periodeDebut));
           c.setHeureDebut(ct.heureDebut);
           c.setHeureFin(ct.heureFin);
           c.setSalleId(ct.salle != null ? ct.salle.getId() : null);
           
           try {
               List<ConflitService.ResultatConflit> resultats = conflitService.verifierConflitsAvecSolutions(c);
               tousConflits.addAll(resultats);
           } catch (SQLException e) {
               logger.error("Erreur vérification conflits", e);
           }
       }
       
       if (!tousConflits.isEmpty()) {
           // Afficher les conflits
           StringBuilder sb = new StringBuilder();
           sb.append("⚠️ CONFLITS DÉTECTÉS\n\n");
           for (ConflitService.ResultatConflit r : tousConflits) {
               sb.append(r.toString()).append("\n\n");
           }
           
           Alert alert = new Alert(Alert.AlertType.WARNING);
           alert.setTitle("Conflits détectés");
           alert.setHeaderText(tousConflits.size() + " conflit(s) détecté(s)");
           
           TextArea textArea = new TextArea(sb.toString());
           textArea.setEditable(false);
           textArea.setWrapText(true);
           textArea.setPrefHeight(300);
           
           alert.getDialogPane().setContent(textArea);
           
           ButtonType continuerBtn = new ButtonType("Continuer quand même", ButtonBar.ButtonData.OK_DONE);
           ButtonType annulerBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
           alert.getButtonTypes().setAll(continuerBtn, annulerBtn);
           
           Optional<ButtonType> result = alert.showAndWait();
           return result.isPresent() && result.get() == continuerBtn;
       }
       
       return true;
   }

    /**
     * Calcule l'heure de fin d'un cours selon la durée en heures et l'écart de la grille.
     * La durée est arrondie au multiple d'écart supérieur.
     */
    private String calculerHeureFinSelonDuree(String heureDebut, int nbCases) {
        double debut = parseHeureDouble(heureDebut);
        double fin = debut + nbCases * grilleEcartHeures;
        return formatHeure(fin);
    }

    private void ouvrirDialogueCreneau(int jourIdx, int heureIdx,
                                        String heureDebut, StackPane cellule) {
        if (coursSelectionnes.isEmpty()) {
            afficherErreur("Aucun cours sélectionné.");
            return;
        }

        // Calculer l'heure de fin par défaut (1 case = ecart heures)
        String heureFinDefaut = formatHeure(parseHeureDouble(heureDebut) + grilleEcartHeures);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/DialogueCreneau.fxml"));
            Parent root = loader.load();
            DialogueCreneauControleur ctrl = loader.getController();

            ctrl.initialiser(
                    coursSelectionnes,
                    grilleJours.get(jourIdx),
                    heureDebut,
                    classeSelectionnee,
                    periodeDebut,
                    cacheMatieres,
                    cacheEnseignants,
                    ufrService,
                    batimentService,
                    salleService,
                    conflitService
            );

            // Passer la configuration de la grille
            ctrl.setConfigGrille(grilleHeureDebut, grilleHeureFin, grilleEcartHeures);

            Stage stage = new Stage();
            stage.setTitle("Ajouter un créneau - " + grilleJours.get(jourIdx) + " " + heureDebut);
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            Stage owner = primaryStage;
            if (owner == null && grilleEDT != null && grilleEDT.getScene() != null) {
                owner = (Stage) grilleEDT.getScene().getWindow();
            }
            if (owner != null) stage.initOwner(owner);
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.showAndWait();

            if (ctrl.isConfirme()) {
                CreneauTemp ct = ctrl.getCreneauTemp();
                if (ct != null) {
                    ct.ligneGrille = heureIdx + 1;
                    ct.colonneGrille = jourIdx + 1;
                    creneauxTemp.add(ct);
                    afficherCreneauDansGrille(ct, cellule);
                    mettreAJourNbCreneaux();
                }
            }

        } catch (IOException e) {
            logger.error("Erreur ouverture dialogue créneau", e);
            afficherErreur("Impossible d'ouvrir le dialogue.");
        }
    }
    
    private boolean isDureeCompatible(double duree, double ecart) {
        if (ecart == 2.0) {
            // Avec espacement 2h, la durée doit être paire (multiple de 2)
            return duree % 2 == 0;
        } else if (ecart == 1.5) {
            // Avec espacement 1h30, la durée doit être multiple de 1.5
            return Math.abs(duree % 1.5) < 0.01;
        }
        return true; // écart 1h : toutes les durées sont possibles
    }


    private void afficherCreneauDansGrille(CreneauTemp ct, StackPane cellule) {
        String couleur = COULEUR_CM;
        if ("TD".equals(ct.cours.getTypeCours())) couleur = COULEUR_TD;
        else if ("TP".equals(ct.cours.getTypeCours())) couleur = COULEUR_TP;

        String bgClair = couleur + "30";

        // Calculer le nombre de cases selon l'écart
        int dureeHeures = ct.dureeHeures;
        int nbCases;
        
        if (grilleEcartHeures == 1) {
            nbCases = dureeHeures + 1;  // 1h → 2 cases, 2h → 3 cases
        } else {
            nbCases = (dureeHeures / 2) + 1;  // 2h → 2 cases, 4h → 3 cases, 6h → 4 cases
        }
        
        int rowSpan = Math.max(1, nbCases);
        
        Integer ligneActuelle = GridPane.getRowIndex(cellule);
        if (ligneActuelle != null) {
            int nbLignes = grilleEDT.getRowConstraints().size();
            int maxRowSpan = nbLignes - ligneActuelle;
            rowSpan = Math.min(rowSpan, maxRowSpan);
        }
        
        GridPane.setRowSpan(cellule, rowSpan);

        // Masquer les cellules couvertes
        for (int d = 1; d < rowSpan; d++) {
            int row = ct.ligneGrille + d;
            int col = ct.colonneGrille;
            for (javafx.scene.Node node : new ArrayList<>(grilleEDT.getChildren())) {
                if (node instanceof StackPane) {
                    Integer r = GridPane.getRowIndex(node);
                    Integer c = GridPane.getColumnIndex(node);
                    if (r != null && c != null && r == row && c == col) {
                        node.setVisible(false);
                        node.setManaged(false);
                        break;
                    }
                }
            }
        }

        cellule.getChildren().clear();
        cellule.setStyle(
            "-fx-background-color: " + bgClair + "; " +
            "-fx-border-color: " + couleur + " #E2E8F0 #E2E8F0 " + couleur + "; " +
            "-fx-border-width: 0 1 1 4; " +
            "-fx-cursor: hand;"
        );

        // ✅ Ajuster la taille du texte et le padding selon le nombre de cases
        int fontSize;
        int paddingTop;
        
        if (rowSpan == 1) {
            fontSize = 11;
            paddingTop = 8;
        } else if (rowSpan == 2) {
            fontSize = 10;
            paddingTop = 12;
        } else {
            fontSize = 9;
            paddingTop = 16;
        }

        VBox contenu = new VBox(3);
        contenu.setAlignment(Pos.CENTER);  // ✅ Centrer verticalement
        contenu.setPadding(new Insets(paddingTop, 6, paddingTop, 8));
        contenu.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // ✅ Ligne 1 : Type + Matière
        HBox ligne1 = new HBox(4);
        ligne1.setAlignment(Pos.CENTER);
        
        Label typeLbl = new Label("[" + ct.cours.getTypeCours() + "]");
        typeLbl.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
        
        Label matLbl = new Label(ct.matiereNom);
        matLbl.setStyle("-fx-font-size: " + (fontSize + 1) + "px; -fx-font-weight: bold; -fx-text-fill: #1A202C;");
        matLbl.setWrapText(true);
        
        ligne1.getChildren().addAll(typeLbl, matLbl);
        contenu.getChildren().add(ligne1);

        // ✅ Ligne 2 : Professeur (avec grade)
        Label profLbl = new Label("👨‍🏫 " + ct.enseignantNom);
        profLbl.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: #1976D2;");
        contenu.getChildren().add(profLbl);

        // ✅ Ligne 3 : Salle
        Label salleLbl = new Label("🏫 " + (ct.salle != null ? ct.salle.getNumero() : "?"));
        salleLbl.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: #718096;");
        contenu.getChildren().add(salleLbl);

        // ✅ Ligne 4 : Groupe (uniquement si TD ou TP et groupe non vide / non "Tous")
        String typeCours = ct.cours.getTypeCours();
        String groupes   = ct.cours.getGroupes();
        if (("TD".equals(typeCours) || "TP".equals(typeCours))
                && groupes != null && !groupes.isEmpty() && !"Tous".equalsIgnoreCase(groupes)) {
            Label groupeLbl = new Label("👥 " + groupes);
            groupeLbl.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; -fx-text-fill: #C62828;");
            contenu.getChildren().add(groupeLbl);
        }

        // ✅ Ligne 4 : Horaire (si assez de place)
        if (rowSpan >= 3) {
            Label horaireLbl = new Label("⏰ " + ct.heureDebut + " → " + ct.heureFin);
            horaireLbl.setStyle("-fx-font-size: " + (fontSize - 1) + "px; -fx-text-fill: #718096;");
            contenu.getChildren().add(horaireLbl);
        }

        Button btnSuppr = new Button("✕");
        btnSuppr.setStyle(
            "-fx-background-color: rgba(198,40,40,0.15); -fx-text-fill: #C62828; " +
            "-fx-font-size: " + fontSize + "px; -fx-cursor: hand; -fx-padding: 1 4; -fx-background-radius: 3;"
        );

        final CreneauTemp ctFinal = ct;
        final int rowSpanFinal = rowSpan;

        btnSuppr.setOnAction(e -> {
            creneauxTemp.remove(ctFinal);
            for (int d = 0; d < rowSpanFinal; d++) {
                int row = ctFinal.ligneGrille + d;
                int col = ctFinal.colonneGrille;
                for (javafx.scene.Node node : new ArrayList<>(grilleEDT.getChildren())) {
                    if (node instanceof StackPane) {
                        Integer r = GridPane.getRowIndex(node);
                        Integer c = GridPane.getColumnIndex(node);
                        if (r != null && c != null && r == row && c == col) {
                            node.setVisible(true);
                            node.setManaged(true);
                            node.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 0;");
                            ((StackPane) node).getChildren().clear();
                            break;
                        }
                    }
                }
            }
            GridPane.setRowSpan(cellule, 1);
            cellule.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 0;");
            cellule.getChildren().clear();
            mettreAJourNbCreneaux();
        });

        StackPane.setAlignment(btnSuppr, Pos.TOP_RIGHT);
        cellule.getChildren().addAll(contenu, btnSuppr);
    }

    private void mettreAJourNbCreneaux() {
        if (nbCreneauxLabel == null) return;
        nbCreneauxLabel.setText("✓ " + creneauxTemp.size() + " créneau(x) placé(s)");
        nbCreneauxLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; " +
                                  "-fx-text-fill: " + (creneauxTemp.isEmpty() ? "#718096" : "#2E7D32") + ";");
    }

    private void enregistrerBrouillon() {
        if (classeSelectionnee == null) return;
        afficherNotification("Brouillon", "Brouillon enregistré (" + creneauxTemp.size() + " créneaux)");
    }

    //  ÉTAPE 4 - VALIDATION ET ENVOI

    private void configurerEtape4() {
        if (etape4PrecedentButton != null) etape4PrecedentButton.setOnAction(e -> afficherEtape(4)); // retour grille
        if (verifierConflitsButton != null) verifierConflitsButton.setOnAction(e -> verifierTousConflits());
        if (validerEdtButton != null) validerEdtButton.setOnAction(e -> validerEtEnregistrer());
    }

    private void mettreAJourResumeEtape4() {
        if (classeSelectionnee != null && resumeClasseLabel != null)
            resumeClasseLabel.setText(classeSelectionnee.getIntitule()
                    + " (" + classeSelectionnee.getAnneeScolaire() + ")");
        if (resumeTypeLabel != null)
            resumeTypeLabel.setText(typePeriode.substring(0, 1).toUpperCase() + typePeriode.substring(1));
        if (resumePeriodeLabel2 != null)
            resumePeriodeLabel2.setText(periodeDebut + " → " + periodeFin);
        if (resumeCreneauxLabel != null)
            resumeCreneauxLabel.setText(creneauxTemp.size() + " créneau(x)");
        if (resumeConfigLabel != null)
            resumeConfigLabel.setText(formatHeure(grilleHeureDebut) + "-" + formatHeure(grilleHeureFin)
                    + " | " + ecartHeuresCombo.getValue() + " | " + String.join(", ", grilleJours));

        if (classeSelectionnee != null && titreEdtField != null && titreEdtField.getText().isEmpty()) {
            titreEdtField.setText("Emploi du temps " + classeSelectionnee.getIntitule()
                    + " - " + typePeriode + " " + periodeDebut);
        }

        if (destinatairesLabel != null && classeSelectionnee != null) {
            int nbEtudiants = classeSelectionnee.getEffectif();
            Set<Integer> profs = new HashSet<>();
            for (Cours c : coursSelectionnes) profs.add(c.getEnseignantId());
            destinatairesLabel.setText(nbEtudiants + " étudiant(s) + "
                    + profs.size() + " enseignant(s) concernés");
        }
    }

    private void verifierTousConflits() {
        if (creneauxTemp.isEmpty()) {
            conflitsStatusLabel.setText("⚠️ Aucun créneau à vérifier.");
            return;
        }

        verifierConflitsButton.setDisable(true);
        conflitsStatusLabel.setText("🔍 Vérification en cours...");

        Task<List<ConflitService.ResultatConflit>> task = new Task<>() {
            @Override
            protected List<ConflitService.ResultatConflit> call() throws SQLException {
                List<Creneau> creneauxAVerifier = new ArrayList<>();
                List<String> avertissements = new ArrayList<>();

                for (CreneauTemp ct : creneauxTemp) {
                    if (ct.salle == null) {
                        avertissements.add("⚠️ " + ct.matiereNom
                                + " (" + ct.jour + " " + ct.heureDebut + ") : aucune salle assignée");
                        continue;
                    }
                    Creneau c = new Creneau();
                    c.setCoursId(ct.cours.getId());
                    c.setSalleId(ct.salle.getId());
                    c.setJour(calculerDateJour(ct.jour, periodeDebut));
                    c.setHeureDebut(ct.heureDebut);
                    c.setHeureFin(ct.heureFin);
                    c.setStatut("planifie");
                    creneauxAVerifier.add(c);
                }

                List<ConflitService.ResultatConflit> resultats =
                        conflitService.verifierListeCreneaux(creneauxAVerifier);

                for (String avert : avertissements) {
                    ConflitService.ResultatConflit r =
                            new ConflitService.ResultatConflit("SALLE_MANQUANTE", avert, null);
                    r.ajouterSolution(new ConflitService.SolutionConflit(
                            "CHANGER_SALLE", "Assigner une salle à ce cours", null));
                    resultats.add(0, r);
                }

                return resultats;
            }

            @Override
            protected void succeeded() {
                verifierConflitsButton.setDisable(false);
                List<ConflitService.ResultatConflit> resultats = getValue();
                conflitsVerifies = true;

                if (resultats.isEmpty()) {
                    conflitsStatusLabel.setText("✅ Aucun conflit détecté - vous pouvez valider.");
                    conflitsStatusLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    if (conflitsDetailArea != null) { conflitsDetailArea.setVisible(false); conflitsDetailArea.setManaged(false); }
                } else {
                    long nbConflitsReels = resultats.stream()
                            .filter(r -> !"SALLE_MANQUANTE".equals(r.getTypeConflit())).count();
                    StringBuilder sb = new StringBuilder();
                    for (ConflitService.ResultatConflit r : resultats) {
                        sb.append(r.toString()).append("\n\n");
                    }
                    String labelStatut = nbConflitsReels > 0
                            ? "❌ " + nbConflitsReels + " conflit(s) détecté(s)"
                            : "⚠️ " + resultats.size() + " avertissement(s)";
                    conflitsStatusLabel.setText(labelStatut);
                    conflitsStatusLabel.setStyle(nbConflitsReels > 0
                            ? "-fx-text-fill: #C62828; -fx-font-weight: bold;"
                            : "-fx-text-fill: #E65100; -fx-font-weight: bold;");
                    if (conflitsDetailArea != null) {
                        conflitsDetailArea.setText(sb.toString());
                        conflitsDetailArea.setVisible(true);
                        conflitsDetailArea.setManaged(true);
                    }
                }
            }

            @Override
            protected void failed() {
                verifierConflitsButton.setDisable(false);
                if (conflitsStatusLabel != null)
                    conflitsStatusLabel.setText("❌ Erreur : " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    private byte[] genererPDFEnMemoire(List<Creneau> creneaux, Classe classe, String titre, String periode) {
        try {
            java.io.File tempFile = java.io.File.createTempFile("edt_", ".pdf");
            String cheminTemp = tempFile.getAbsolutePath();
            exportService.exporterEDTGrillePDF(creneaux, classe, periode, utilisateurConnecte, cheminTemp, titre);
            byte[] content = java.nio.file.Files.readAllBytes(tempFile.toPath());
            tempFile.delete();
            return content;
        } catch (Exception e) {
            logger.error("Erreur génération PDF", e);
            return null;
        }
    }

    /**
     * Valide et enregistre l'emploi du temps.
     */
    private void validerEtEnregistrer() {
        String titre = titreEdtField != null ? titreEdtField.getText().trim() : "";
        if (titre.isEmpty()) {
            afficherErreur("Veuillez saisir un titre pour l'emploi du temps.");
            return;
        }
        if (creneauxTemp.isEmpty()) {
            afficherErreur("Aucun créneau placé dans la grille.");
            return;
        }
        if (!conflitsVerifies) {
            boolean continuer = afficherConfirmation("Conflits non vérifiés",
                    "Vous n'avez pas vérifié les conflits. Continuer quand même ?");
            if (!continuer) return;
        }

        if (validerEdtButton != null) validerEdtButton.setDisable(true);
        if (chargementIndicator != null) chargementIndicator.setVisible(true);

        final String titreFinal = titre;
        final boolean envEtu = envoyerEtudiantsCheck != null && envoyerEtudiantsCheck.isSelected();
        final boolean envPro = envoyerProfsCheck != null && envoyerProfsCheck.isSelected();

        System.out.println("📝 Sauvegarde de l'emploi du temps");
        System.out.println("   grilleHeureDebut = " + grilleHeureDebut);
        System.out.println("   grilleEcartHeures = " + grilleEcartHeures);
        System.out.println("   grilleHeureFin = " + grilleHeureFin);
        System.out.println("   grilleJours = " + grilleJours);

        Task<EmploiDuTemps> task = new Task<>() {
            @Override
            protected EmploiDuTemps call() throws Exception {
                List<Creneau> creneauxFinals = new ArrayList<>();
                for (CreneauTemp ct : creneauxTemp) {
                    Creneau c = new Creneau();
                    c.setCoursId(ct.cours.getId());
                    c.setJour(calculerDateJour(ct.jour, periodeDebut));
                    c.setHeureDebut(ct.heureDebut);
                    c.setHeureFin(ct.heureFin);
                    c.setSalleId(ct.salle != null ? ct.salle.getId() : null);
                    c.setStatut("planifie");
                    creneauxFinals.add(c);
                }

                EmploiDuTemps edt = new EmploiDuTemps();
                edt.setClasseId(classeSelectionnee.getId());
                edt.setPeriodeType(typePeriode);
                edt.setPeriodeDebut(periodeDebut);
                edt.setPeriodeFin(periodeFin);
                
                // ✅ SAUVEGARDER LA CONFIGURATION AVEC LES VALEURS CORRECTES
                String heuresConfig = grilleHeureDebut + ":" + grilleEcartHeures + ":" + grilleHeureFin;
                String joursConfig = String.join(",", grilleJours);
                
                edt.setHeuresConfig(heuresConfig);
                edt.setJoursConfig(joursConfig);
                
                System.out.println("   heuresConfig = " + heuresConfig);
                System.out.println("   joursConfig = " + joursConfig);

                planningService.creerEtValiderEmploiDuTemps(edt, creneauxFinals, titreFinal);

                String periodeAffichage = typePeriode + " du " + periodeDebut + " au " + periodeFin;
                byte[] pdfContent = genererPDFEnMemoire(creneauxFinals, classeSelectionnee, titreFinal, periodeAffichage);

                EmploiDuTempsService edtService = new EmploiDuTempsService();
                edtService.validerEmploiDuTemps(edt.getId(), pdfContent, titreFinal, envEtu, envPro);

                return edt;
            }

            @Override
            protected void succeeded() {
                javafx.application.Platform.runLater(() -> {
                    if (chargementIndicator != null) chargementIndicator.setVisible(false);
                    if (validerEdtButton != null) validerEdtButton.setDisable(false);
                    afficherNotification("Succès", "L'emploi du temps \"" + titreFinal + "\" a été créé !" +
                            (envEtu ? "\n✅ Emails envoyés aux étudiants" : "") +
                            (envPro ? "\n✅ Emails envoyés aux enseignants" : ""));
                    retourListeEDT();
                });
            }

            @Override
            protected void failed() {
                javafx.application.Platform.runLater(() -> {
                    if (chargementIndicator != null) chargementIndicator.setVisible(false);
                    if (validerEdtButton != null) validerEdtButton.setDisable(false);
                    afficherErreur("Erreur lors de la création : " + getException().getMessage());
                    logger.error("Erreur création EDT", getException());
                });
            }
        };
        new Thread(task).start();
    }

    //  NAVIGATION ENTRE ÉTAPES

    private void afficherEtape(int numero) {
        etapeActuelle = numero;

        // Masquer tout
        if (etape1Pane != null) { etape1Pane.setVisible(false); etape1Pane.setManaged(false); }
        if (etape1bPane != null) { etape1bPane.setVisible(false); etape1bPane.setManaged(false); }
        if (etape2Pane != null) { etape2Pane.setVisible(false); etape2Pane.setManaged(false); }
        if (etape3Pane != null) { etape3Pane.setVisible(false); etape3Pane.setManaged(false); }
        if (etape4Pane != null) { etape4Pane.setVisible(false); etape4Pane.setManaged(false); }

        // Afficher l'étape courante
        switch (numero) {
            case 1 -> { if (etape1Pane != null) { etape1Pane.setVisible(true); etape1Pane.setManaged(true); } }
            case 2 -> { if (etape1bPane != null) { etape1bPane.setVisible(true); etape1bPane.setManaged(true); } }
            case 3 -> { if (etape2Pane != null) { etape2Pane.setVisible(true); etape2Pane.setManaged(true); } }
            case 4 -> { if (etape3Pane != null) { etape3Pane.setVisible(true); etape3Pane.setManaged(true); } }
            case 5 -> { if (etape4Pane != null) { etape4Pane.setVisible(true); etape4Pane.setManaged(true); } }
        }

        // Mise à jour des indicateurs de progression
        mettreAJourProgression(numero);

        String[] titres = {"", "Étape 1/5 - Type & Période",
                "Étape 2/5 - Configuration horaires",
                "Étape 3/5 - Classe & Cours",
                "Étape 4/5 - Placement des créneaux",
                "Étape 5/5 - Validation & Envoi"};
        if (etapeTitreLabel != null && numero < titres.length)
            etapeTitreLabel.setText("- " + titres[numero]);
    }

    private void mettreAJourProgression(int etapeAffichee) {
        // Mapping étape affichée → numéro logique (1 à 5)
        // étape1=1, étape1b=2, étape2=3, étape3=4, étape4=5
        String ACTIF = "-fx-fill: #1565C0;";
        String FAIT = "-fx-fill: #2E7D32;";
        String INACTIF = "-fx-fill: #B0BEC5;";
        String TXT_ACTIF = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1565C0;";
        String TXT_FAIT = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;";
        String TXT_INACTIF = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #B0BEC5;";

        if (cercle1 != null) cercle1.setStyle(etapeAffichee >= 1 ? (etapeAffichee > 1 ? FAIT : ACTIF) : INACTIF);
        if (cercle2 != null) cercle2.setStyle(etapeAffichee >= 2 ? (etapeAffichee > 2 ? FAIT : ACTIF) : INACTIF);
        if (cercle3 != null) cercle3.setStyle(etapeAffichee >= 3 ? (etapeAffichee > 3 ? FAIT : ACTIF) : INACTIF);
        if (cercle4 != null) cercle4.setStyle(etapeAffichee >= 4 ? (etapeAffichee > 4 ? FAIT : ACTIF) : INACTIF);
        if (cercle5 != null) cercle5.setStyle(etapeAffichee >= 5 ? ACTIF : INACTIF);

        if (label2 != null) label2.setStyle(etapeAffichee >= 2 ? (etapeAffichee > 2 ? TXT_FAIT : TXT_ACTIF) : TXT_INACTIF);
        if (label3 != null) label3.setStyle(etapeAffichee >= 3 ? (etapeAffichee > 3 ? TXT_FAIT : TXT_ACTIF) : TXT_INACTIF);
        if (label4 != null) label4.setStyle(etapeAffichee >= 4 ? (etapeAffichee > 4 ? TXT_FAIT : TXT_ACTIF) : TXT_INACTIF);
        if (label5 != null) label5.setStyle(etapeAffichee >= 5 ? TXT_ACTIF : TXT_INACTIF);

        if (ligne1 != null) ligne1.setStyle(etapeAffichee > 1 ? FAIT : INACTIF);
        if (ligne2 != null) ligne2.setStyle(etapeAffichee > 2 ? FAIT : INACTIF);
        if (ligne3 != null) ligne3.setStyle(etapeAffichee > 3 ? FAIT : INACTIF);
        if (ligne4 != null) ligne4.setStyle(etapeAffichee > 4 ? FAIT : INACTIF);
    }

    
    private String calculerDateJour(String nomJour, String periodeDebut) {
        Map<String, Integer> joursOrdre = Map.of(
                "Lundi", 1, "Mardi", 2, "Mercredi", 3,
                "Jeudi", 4, "Vendredi", 5, "Samedi", 6);
        LocalDate debut = LocalDate.parse(periodeDebut);
        int jourSemaine = debut.getDayOfWeek().getValue();
        int cible = joursOrdre.getOrDefault(nomJour, 1);
        int delta = cible - jourSemaine;
        if (delta < 0) delta += 7;
        return debut.plusDays(delta).toString();
    }

    private LocalDate getPremierJourSemaine(int annee, int numeroSemaine) {
        LocalDate date = LocalDate.of(annee, 1, 1);
        while (date.getDayOfWeek().getValue() != 1) date = date.plusDays(1);
        return date.plusWeeks(numeroSemaine - 1);
    }

    private void configurerBoutonAnnuler() {
        if (annulerButton != null) {
            annulerButton.setOnAction(e -> {
                boolean confirme = afficherConfirmation("Annuler la création",
                        "Abandonner la création de l'emploi du temps ?");
                if (confirme) retourListeEDT();
            });
        }
    }

    private void retourListeEDT() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ListeEmploisTemps.fxml"));
            Parent root = loader.load();
            ListeEmploisTempsControleur ctrl = loader.getController();
            ctrl.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException ex) {
            logger.error("Erreur retour liste EDT", ex);
        }
    }

    /**
     * Initialise la modification d'un EDT existant.
     * @param edt l'emploi du temps à modifier
     * @param stage la fenêtre principale
     */
    public void initialiserModification(EmploiDuTemps edt, Stage stage) {
        initialiserAvecUtilisateur(utilisateurConnecte, stage);
        
        typePeriode = edt.getPeriodeType() != null ? edt.getPeriodeType() : "hebdomadaire";
        periodeDebut = edt.getPeriodeDebut();
        periodeFin = edt.getPeriodeFin();
        selectionnerType(typePeriode);
        mettreAJourResumePeriode();
        
        grilleHeureDebut = edt.getHeureDebutConfig();
        grilleEcartHeures = edt.getEcartConfig();
        grilleHeureFin = edt.getHeureFinConfig();
        grilleJours = edt.getJoursConfigList();
        
        System.out.println("📝 MODIFICATION EDT #" + edt.getId());
        System.out.println("   Heures: début=" + grilleHeureDebut + ", écart=" + grilleEcartHeures + ", fin=" + grilleHeureFin);
        System.out.println("   Jours: " + grilleJours);
        
        // Charger la classe
        Task<Classe> taskClasse = new Task<>() {
            @Override protected Classe call() throws SQLException {
                return classeService.trouverParId(edt.getClasseId());
            }
            @Override protected void succeeded() {
                classeSelectionnee = getValue();
                if (classeSelectionnee != null) {
                    classeCombo.setValue(classeSelectionnee);
                    chargerCoursClasse(classeSelectionnee.getId());
                }
            }
            @Override protected void failed() {
                logger.error("Erreur chargement classe", getException());
            }
        };
        new Thread(taskClasse).start();
        
        Task<List<Creneau>> taskCreneaux = new Task<>() {
            @Override
            protected List<Creneau> call() throws SQLException {
                return planningService.getCreneauxParEmploiDuTemps(edt.getId());
            }
            @Override
            protected void succeeded() {
                List<Creneau> creneaux = getValue();
                System.out.println("   Créneaux à recharger: " + (creneaux != null ? creneaux.size() : 0));
                
                if (creneaux != null && !creneaux.isEmpty()) {
                    for (Creneau c : creneaux) {
                        try {
                            Cours cours = coursService.trouverParId(c.getCoursId());
                            if (cours == null) continue;
                            
                            String matiereNom = cacheMatieres.getOrDefault(cours.getMatiereId(), "Matière");
                            String enseignantNom = cacheEnseignants.getOrDefault(cours.getEnseignantId(), "Enseignant");
                            
                            Salle salle = null;
                            if (c.getSalleId() != null) {
                                salle = salleService.trouverParId(c.getSalleId());
                            }
                            
                            int duree = (Integer.parseInt(c.getHeureFin().split(":")[0]) - 
                                         Integer.parseInt(c.getHeureDebut().split(":")[0]));
                            
                            String nomJour = "";
                            LocalDate date = LocalDate.parse(c.getJour());
                            switch (date.getDayOfWeek().getValue()) {
                                case 1: nomJour = "Lundi"; break;
                                case 2: nomJour = "Mardi"; break;
                                case 3: nomJour = "Mercredi"; break;
                                case 4: nomJour = "Jeudi"; break;
                                case 5: nomJour = "Vendredi"; break;
                                case 6: nomJour = "Samedi"; break;
                                default: nomJour = "Dimanche";
                            }
                            
                            CreneauTemp ct = new CreneauTemp(
                                cours, nomJour, c.getHeureDebut(), c.getHeureFin(),
                                salle, duree, matiereNom, enseignantNom
                            );
                            
                            creneauxTemp.add(ct);
                            System.out.println("   ✅ Créneau chargé: " + nomJour + " " + c.getHeureDebut() + "-" + c.getHeureFin());
                            
                        } catch (Exception e) {
                            logger.error("Erreur chargement créneau", e);
                        }
                    }
                    
                    construireGrille();
                    mettreAJourNbCreneaux();
                }
                
                afficherEtape(4);
            }
            @Override
            protected void failed() {
                logger.error("Erreur chargement créneaux", getException());
                afficherEtape(4);
            }
        };
        new Thread(taskCreneaux).start();
        
        afficherEtape(3);
    }

    public void setUtilisateurConnectePublic(Utilisateur u) {
        this.utilisateurConnecte = u;
    }
}