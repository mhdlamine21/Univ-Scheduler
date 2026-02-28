package scheduler.controleur;

import scheduler.controleur.NouvelEdtControleur.CreneauTemp;
import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.control.SelectionMode;

import java.sql.SQLException;
import java.util.*;

/**
 * Contrôleur du dialogue de placement d'un créneau dans la grille EDT.
 * Gère le choix manuel (UFR → Bâtiment → Salle) et automatique (algorithme).
 */
public class DialogueCreneauControleur {

    private static final Logger logger = LoggerFactory.getLogger(DialogueCreneauControleur.class);
      
    @FXML private Label titreCours;
    @FXML private Label infoJourLabel;
    @FXML private Label infoHeureLabel;
    @FXML private Spinner<Integer> dureeSpinner;
    @FXML private Label heureFinLabel;
    @FXML private ComboBox<String> ecartHeuresCombo;

    @FXML private ToggleGroup modeAttrGroup;
    @FXML private RadioButton modeManuelRadio;
    @FXML private RadioButton modeAutoRadio;

    // Panneau manuel
    @FXML private VBox panneauManuel;
    @FXML private ComboBox<Ufr> ufrComboManuel;
    @FXML private ComboBox<Batiment> batimentComboManuel;
    @FXML private Label nbSallesDispoLabel;
    @FXML private TableView<Salle> sallesManuelTable;
    @FXML private TableColumn<Salle, String> salleNumeroCol;
    @FXML private TableColumn<Salle, Integer> salleCapaciteCol;
    @FXML private TableColumn<Salle, String> salleTypeCol;
    @FXML private TableColumn<Salle, String> salleBatimentCol;
    @FXML private TableColumn<Salle, Integer> salleEtageCol;
    @FXML private TableColumn<Salle, String> salleStatutCol;

    // Panneau automatique
    @FXML private VBox panneauAuto;
    @FXML private CheckBox filtreUfrCheck;
    @FXML private ComboBox<Ufr> ufrComboAuto;
    @FXML private CheckBox filtreBatimentCheck;
    @FXML private ComboBox<Batiment> batimentComboAuto;
    @FXML private CheckBox filtreTypeCheck;
    @FXML private ComboBox<String> typeSalleCombo;
    @FXML private CheckBox filtreEquipCheck;
    @FXML private ListView<Equipement> equipementsList;
    @FXML private Button lancerRechercheAutoButton;
    @FXML private VBox resultAutoPane;
    @FXML private Label salleAutoLabel;
    @FXML private Label salleAutoDetailLabel;
    @FXML private VBox alternativesPane;
    @FXML private ComboBox<String> alternativesCombo;

    @FXML private Label messageLabel;
    @FXML private Button annulerDialogueButton;
    @FXML private Button confirmerCreneauButton;

    @FXML private ComboBox<Cours> coursCombo;
    
    // Données injectées
    private List<Cours> coursDisponibles;
    private String jourNom;
    private String heureDebut;
    private Classe classe;
    private String periodeDebut;
    private Map<Integer, String> cacheMatieres;
    private Map<Integer, String> cacheEnseignants;

    // Services
    private UfrService ufrService;
    private BatimentService batimentService;
    private SalleService salleService;
    private ConflitService conflitService;
    private AttributionService attributionService;
    private EquipementService equipementService;

    // État
    private Cours coursSelectionne;
    private Salle salleSelectionnee;
    private boolean confirme = false;
    private NouvelEdtControleur.CreneauTemp creneauTemp;
    private List<Salle> listeSallesAffichees = new ArrayList<>();
    private Map<Integer, String> cacheBatimentsNoms = new HashMap<>();

    // Configuration grille (injectée depuis NouvelEdtControleur)
    private int configHeureDebut = 8;
    private int configHeureFin = 18;
    private int configEcartHeures = 2;

    /**
     * Injecte la configuration de la grille pour adapter la durée des créneaux.
     */
    public void setConfigGrille(int heureDebut, int heureFin, int ecartHeures) {
        this.configHeureDebut = heureDebut;
        this.configHeureFin = 18;
        this.configEcartHeures = ecartHeures;
        
        // ✅ Configurer le spinner selon l'écart
        if (dureeSpinner != null) {
            SpinnerValueFactory<Integer> svf;
            
            if (ecartHeures == 2) {
                // Pour écart 2h, les valeurs possibles sont 2h, 4h, 6h
                // Calculer la durée maximale possible (ne pas dépasser 18h)
                int maxDuree = 18 - heureDebut;
                if (maxDuree > 6) maxDuree = 6;
                // Limiter aux valeurs paires
                svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(2, maxDuree, 2);
            } else {
                // Pour écart 1h, on choisit la durée en heures (1, 2, 3...)
                int maxDuree = 18 - heureDebut;
                svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxDuree, 1);
            }
            
            dureeSpinner.setValueFactory(svf);
            mettreAJourHeureFin();
        }
        
        System.out.println("🔧 Configuration grille - Début: " + heureDebut + 
                           "h, Fin: 18h, Écart: " + ecartHeures + "h");
    }

    /**
     * Initialise le dialogue avec les données du créneau.
     * @param cours liste des cours disponibles
     * @param jour nom du jour
     * @param heureDebut heure de début
     * @param classe la classe concernée
     * @param periodeDebut date de début de la période
     * @param cacheMatieres cache des matières
     * @param cacheEnseignants cache des enseignants
     * @param ufrService service UFR
     * @param batimentService service bâtiment
     * @param salleService service salle
     * @param conflitService service conflit
     */
    public void initialiser(List<Cours> cours, String jour, String heureDebut,
                             Classe classe, String periodeDebut,
                             Map<Integer, String> cacheMatieres,
                             Map<Integer, String> cacheEnseignants,
                             UfrService ufrService, BatimentService batimentService,
                             SalleService salleService, ConflitService conflitService) {
        this.coursDisponibles = cours;
        this.jourNom = jour;
        this.heureDebut = heureDebut;
        this.classe = classe;
        this.periodeDebut = periodeDebut;
        this.cacheMatieres = cacheMatieres;
        this.cacheEnseignants = cacheEnseignants;
        this.ufrService = ufrService;
        this.batimentService = batimentService;
        this.salleService = salleService;
        this.conflitService = conflitService;

        infoJourLabel.setText("📅 " + jour);
        infoHeureLabel.setText("🕐 " + heureDebut);
        mettreAJourHeureFin();

        // Configurer le ComboBox de cours
        if (coursCombo != null) {
            coursCombo.setCellFactory(lv -> new ListCell<Cours>() {
                @Override protected void updateItem(Cours c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty || c == null ? null : getTitreCours(c));
                }
            });
            coursCombo.setButtonCell(new ListCell<Cours>() {
                @Override protected void updateItem(Cours c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty || c == null ? null : getTitreCours(c));
                }
            });
            coursCombo.getItems().addAll(cours);
            coursCombo.setOnAction(e -> coursSelectionne = coursCombo.getValue());
            if (!cours.isEmpty()) {
                coursCombo.setValue(cours.get(0));
                coursSelectionne = cours.get(0);
            }
        }
        
        if (titreCours != null) {
            titreCours.setText(cours.size() == 1 ? getTitreCours(cours.get(0)) : "Choisissez un cours :");
        }

        chargerUfrs();
        chargerEquipements();

        typeSalleCombo.getItems().addAll("Tous", "Amphi", "TD", "TP", "Informatique", "Autre");
        typeSalleCombo.setValue("Tous");
    }

    private String getTitreCours(Cours c) {
        String mat = cacheMatieres.getOrDefault(c.getMatiereId(), "Matière " + c.getMatiereId());
        String ens = cacheEnseignants.getOrDefault(c.getEnseignantId(), "Enseignant");
        return "[" + c.getTypeCours() + "] " + mat + " - " + ens;
    }

    private double parseEcart(String valeur) {
        if (valeur == null) return 1;
        switch (valeur) {
            case "1h": return 1;
            case "1h30": return 1.5;
            case "2h": return 2;
            case "3h": return 3;
            default: return 1;
        }
    }

    @FXML
    public void initialize() {
        attributionService = new AttributionService();
        equipementService = new EquipementService();

        SpinnerValueFactory<Integer> svf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 2);
        dureeSpinner.setValueFactory(svf);

        dureeSpinner.valueProperty().addListener((obs, oldV, newV) -> verifierDureeCompatible());

        ToggleGroup modeGroup = new ToggleGroup();
        modeManuelRadio.setToggleGroup(modeGroup);
        modeAutoRadio.setToggleGroup(modeGroup);

        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean estManuel = newVal == modeManuelRadio;
            panneauManuel.setVisible(estManuel);
            panneauManuel.setManaged(estManuel);
            panneauAuto.setVisible(!estManuel);
            panneauAuto.setManaged(!estManuel);
        });

        dureeSpinner.valueProperty().addListener((obs, oldV, newV) -> mettreAJourHeureFin());

        ufrComboManuel.setOnAction(e -> {
            Ufr ufr = ufrComboManuel.getValue();
            if (ufr != null) chargerBatiments(ufr.getId(), batimentComboManuel, false);
        });

        batimentComboManuel.setOnAction(e -> {
            Batiment bat = batimentComboManuel.getValue();
            if (bat != null) chargerSallesDisponibles(bat.getId());
        });

        filtreUfrCheck.setOnAction(e -> {
            ufrComboAuto.setDisable(!filtreUfrCheck.isSelected());
            if (filtreUfrCheck.isSelected()) {
                ufrComboAuto.setOnAction(ev -> {
                    Ufr u = ufrComboAuto.getValue();
                    if (u != null) chargerBatiments(u.getId(), batimentComboAuto, true);
                });
            }
        });
        filtreBatimentCheck.setOnAction(e ->
                batimentComboAuto.setDisable(!filtreBatimentCheck.isSelected()));
        filtreTypeCheck.setOnAction(e ->
                typeSalleCombo.setDisable(!filtreTypeCheck.isSelected()));

        lancerRechercheAutoButton.setOnAction(e -> lancerRechercheAuto());

        configurerTableauSalles();

        sallesManuelTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldV, newV) -> {
                    salleSelectionnee = newV;
                    if (newV != null) masquerMessage();
                });

        annulerDialogueButton.setOnAction(e -> fermerFenetre(false));
        confirmerCreneauButton.setOnAction(e -> confirmerCreneau());
    }

    /**
    * Vérifie si la durée est compatible avec l'écart de la grille.
    */
    private void verifierDureeCompatible() {
        int dureeHeures = dureeSpinner.getValue();  // Directement la durée en heures
        
        // ✅ Pour écart 2h, vérifier que la durée est 2h, 4h ou 6h
        if (configEcartHeures == 2) {
            if (dureeHeures % 2 != 0 || dureeHeures < 2) {
                messageLabel.setText("⚠️ Avec un espacement de 2h, choisissez 2h, 4h ou 6h.");
                messageLabel.setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold; " +
                                      "-fx-background-color: #FFF3E0; -fx-padding: 8 12; -fx-background-radius: 6;");
                messageLabel.setVisible(true);
                confirmerCreneauButton.setDisable(true);
            } else {
                messageLabel.setVisible(false);
                confirmerCreneauButton.setDisable(false);
            }
        } 
        // ✅ Pour écart 1h, toutes les durées sont possibles
        else {
            messageLabel.setVisible(false);
            confirmerCreneauButton.setDisable(false);
        }
    }
    
    /**
     * Met à jour l'affichage de l'heure de fin.
     */
    private void mettreAJourHeureFin() {
        try {
            int dureeHeures = dureeSpinner.getValue();  // Directement la durée en heures
            
            String[] parts = heureDebut.split(":");
            int heure = Integer.parseInt(parts[0]);
            int minutes = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            
            double heureFinDouble = heure + minutes / 60.0 + dureeHeures;
            int hFin = (int) heureFinDouble;
            int mFin = (int) ((heureFinDouble - hFin) * 60);
            
            if (hFin > 18) {
                heureFinLabel.setText("→ ⚠️ Dépasse 18h");
                heureFinLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                confirmerCreneauButton.setDisable(true);
            } else {
                heureFinLabel.setText("→ " + String.format("%02d:%02d", hFin, mFin));
                heureFinLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                confirmerCreneauButton.setDisable(false);
            }
            
        } catch (Exception ignored) {}
    }
    
    private void configurerTableauSalles() {
        salleNumeroCol.setCellValueFactory(
                cd -> javafx.beans.binding.Bindings.createStringBinding(
                        () -> cd.getValue().getNumero()));
        salleCapaciteCol.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleIntegerProperty(
                        cd.getValue().getCapacite()).asObject());
        salleTypeCol.setCellValueFactory(
                cd -> javafx.beans.binding.Bindings.createStringBinding(
                        () -> cd.getValue().getType()));
        salleBatimentCol.setCellValueFactory(
                cd -> javafx.beans.binding.Bindings.createStringBinding(
                        () -> cacheBatimentsNoms.getOrDefault(
                                cd.getValue().getBatimentId(), "Bât. " + cd.getValue().getBatimentId())));
        salleEtageCol.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleIntegerProperty(
                        cd.getValue().getEtage()).asObject());
        salleStatutCol.setCellValueFactory(
                cd -> javafx.beans.binding.Bindings.createStringBinding(
                        () -> cd.getValue().getStatut()));

        salleStatutCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("disponible".equals(item))
                    setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                else
                    setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
            }
        });
    }

    private void chargerUfrs() {
        Task<List<Ufr>> task = new Task<>() {
            @Override protected List<Ufr> call() throws SQLException {
                return ufrService.listerTous();
            }
            @Override protected void succeeded() {
                List<Ufr> ufrs = getValue();
                ufrComboManuel.setCellFactory(lv -> cellUfr());
                ufrComboManuel.setButtonCell(cellUfr());
                ufrComboManuel.getItems().setAll(ufrs);
                ufrComboAuto.setCellFactory(lv -> cellUfr());
                ufrComboAuto.setButtonCell(cellUfr());
                ufrComboAuto.getItems().setAll(ufrs);
            }
        };
        new Thread(task).start();
    }

    private ListCell<Ufr> cellUfr() {
        return new ListCell<>() {
            @Override protected void updateItem(Ufr u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getNom());
            }
        };
    }

    private void chargerBatiments(int ufrId, ComboBox<Batiment> combo, boolean modeAuto) {
        Task<List<Batiment>> task = new Task<>() {
            @Override
            protected List<Batiment> call() throws SQLException {
                List<Batiment> tous = batimentService.listerParUfr(ufrId);
                tous.removeIf(b -> "indisponible".equals(b.getStatut()));
                return tous;
            }
            @Override
            protected void succeeded() {
                List<Batiment> bats = getValue();
                for (Batiment b : bats) cacheBatimentsNoms.put(b.getId(), b.getNom());
                combo.getItems().setAll(bats);
                if (!modeAuto) combo.setDisable(false);
            }
            @Override
            protected void failed() {
                logger.error("Erreur chargement bâtiments", getException());
            }
        };
        new Thread(task).start();
    }

    private void chargerSallesDisponibles(int batimentId) {
        if (coursSelectionne == null) return;
        int duree = dureeSpinner.getValue();
        String[] hParts = heureDebut.split(":");
        String heureFin = String.format("%02d:00", Integer.parseInt(hParts[0]) + duree);
        String date = calculerDateJour(jourNom);

        Task<List<Salle>> task = new Task<>() {
            @Override protected List<Salle> call() throws SQLException {
                List<Salle> sallesBat = salleService.listerParBatiment(batimentId);
                List<Salle> disponibles = new ArrayList<>();
                for (Salle s : sallesBat) {
                    if ("indisponible".equals(s.getStatut())) continue;
                    boolean libre = salleService.estDisponible(s.getId(), date, heureDebut, heureFin);
                    if (libre) disponibles.add(s);
                }
                return disponibles;
            }
            @Override protected void succeeded() {
                listeSallesAffichees = getValue();
                sallesManuelTable.getItems().setAll(listeSallesAffichees);
                nbSallesDispoLabel.setText(listeSallesAffichees.size() + " salle(s) disponible(s)");
                nbSallesDispoLabel.setStyle("-fx-text-fill: " +
                        (listeSallesAffichees.isEmpty() ? "#C62828" : "#2E7D32") +
                        "; -fx-font-weight: bold;");
            }
        };
        new Thread(task).start();
    }

    private void chargerEquipements() {
        Task<List<Equipement>> task = new Task<>() {
            @Override protected List<Equipement> call() throws SQLException {
                return equipementService.listerTous();
            }
            @Override protected void succeeded() {
                equipementsList.getItems().setAll(getValue());
                equipementsList.setCellFactory(lv -> new ListCell<>() {
                    @Override protected void updateItem(Equipement e, boolean empty) {
                        super.updateItem(e, empty);
                        setText(empty || e == null ? null : e.getNom() + " - " + e.getDescription());
                    }
                });
                equipementsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            }
        };
        new Thread(task).start();
    }

    /**
     * Lance la recherche automatique de salle.
     */
    private void lancerRechercheAuto() {
        if (coursSelectionne == null) {
            afficherMessageErreur("Sélectionnez un cours d'abord.");
            return;
        }
        int duree = dureeSpinner.getValue();
        String[] hParts = heureDebut.split(":");
        String heureFin = String.format("%02d:00", Integer.parseInt(hParts[0]) + duree);
        String date = calculerDateJour(jourNom);

        lancerRechercheAutoButton.setDisable(true);
        if (resultAutoPane != null) { resultAutoPane.setVisible(false); resultAutoPane.setManaged(false); }

        Integer ufrId = (filtreUfrCheck.isSelected() && ufrComboAuto.getValue() != null)
                ? ufrComboAuto.getValue().getId() : null;
        Integer batId = (filtreBatimentCheck.isSelected() && batimentComboAuto.getValue() != null)
                ? batimentComboAuto.getValue().getId() : null;
        String typeSalle = (filtreTypeCheck.isSelected() && !"Tous".equals(typeSalleCombo.getValue()))
                ? typeSalleCombo.getValue() : null;
        List<Integer> equipIds = new ArrayList<>();
        if (filtreEquipCheck.isSelected() && equipementsList != null) {
            for (Equipement eq : equipementsList.getSelectionModel().getSelectedItems()) {
                equipIds.add(eq.getId());
            }
        }

        Task<Salle> task = new Task<>() {
            @Override
            protected Salle call() throws SQLException {
                return attributionService.trouverSalleOptimaleAvecFiltres(
                    coursSelectionne, date, heureDebut, heureFin,
                    classe, ufrId, batId, typeSalle, equipIds
                );
            }
            @Override
            protected void succeeded() {
                lancerRechercheAutoButton.setDisable(false);
                Salle salle = getValue();
                if (salle != null) {
                    salleSelectionnee = salle;
                    if (salleAutoLabel != null)
                        salleAutoLabel.setText("✅ Salle trouvée : " + salle.getNumero());
                    if (salleAutoDetailLabel != null)
                        salleAutoDetailLabel.setText("Capacité : " + salle.getCapacite()
                            + " · Type : " + salle.getType()
                            + " · Étage : " + salle.getEtage());
                    if (resultAutoPane != null) {
                        resultAutoPane.setVisible(true);
                        resultAutoPane.setManaged(true);
                    }
                    masquerMessage();
                } else {
                    afficherMessageErreur("❌ Aucune salle disponible avec ces critères.\n" +
                        "Essayez de réduire les filtres.");
                }
            }
            @Override
            protected void failed() {
                lancerRechercheAutoButton.setDisable(false);
                logger.error("Erreur recherche auto salle", getException());
                afficherMessageErreur("Erreur lors de la recherche.");
            }
        };
        new Thread(task).start();
    }

    /**
     * Confirme le créneau sélectionné.
     */
    private void confirmerCreneau() {
        if (coursSelectionne == null) {
            afficherMessageErreur("Sélectionnez un cours.");
            return;
        }

        String[] hParts = heureDebut.split(":");
        int hDebut = Integer.parseInt(hParts[0]);
        int mDebut = hParts.length > 1 ? Integer.parseInt(hParts[1]) : 0;
        
        if (configEcartHeures == 2 && hDebut % 2 != 0) {
            afficherMessageErreur("❌ Avec un écart de 2h, l'heure de début doit être paire.\n" +
                                  "Heures disponibles: " + configHeureDebut + "h, " + 
                                  (configHeureDebut+2) + "h, " + (configHeureDebut+4) + "h...");
            return;
        }

        if (modeManuelRadio.isSelected()) {
            salleSelectionnee = sallesManuelTable.getSelectionModel().getSelectedItem();
            if (salleSelectionnee == null) {
                afficherMessageErreur("Sélectionnez une salle dans le tableau.");
                return;
            }
        } else {
            if (salleSelectionnee == null) {
                afficherMessageErreur("Lancez la recherche automatique et choisissez une salle.");
                return;
            }
        }

        // ✅ Récupérer la durée choisie
        int dureeHeures = dureeSpinner.getValue();
        
        // ✅ Calculer le nombre de cases à occuper
        int nbCasesOccupees;
        if (configEcartHeures == 1) {
            nbCasesOccupees = dureeHeures + 1;
        } else {
            nbCasesOccupees = (dureeHeures / 2) + 1;
        }
        
        System.out.println("📌 Insertion cours: début=" + heureDebut + 
                           ", durée=" + dureeHeures + "h" +
                           ", écart=" + configEcartHeures + "h" +
                           ", cases à occuper=" + nbCasesOccupees);
        
        // ✅ Calculer l'heure de fin
        double heureFinDouble = hDebut + mDebut / 60.0 + dureeHeures;
        int hFin = (int) heureFinDouble;
        int mFin = (int) ((heureFinDouble - hFin) * 60);
        String heureFin = String.format("%02d:%02d", hFin, mFin);
        
        // Vérifier qu'on ne dépasse pas 18h
        if (hFin > 18) {
            afficherMessageErreur("❌ Ce cours dépasse 18h.\n" +
                                  "Début: " + heureDebut + ", Durée: " + dureeHeures + "h → Fin: " + heureFin);
            return;
        }

        String matNom = cacheMatieres.getOrDefault(coursSelectionne.getMatiereId(), "Matière");
        String ensNom = cacheEnseignants.getOrDefault(coursSelectionne.getEnseignantId(), "Enseignant");

        // ✅ Constructeur avec 8 paramètres (ajout de ensNom)
        creneauTemp = new CreneauTemp(
            coursSelectionne, jourNom, heureDebut, heureFin,
            salleSelectionnee, dureeHeures, matNom, ensNom
        );

        confirme = true;
        fermerFenetre(true);
    }
    
    private void fermerFenetre(boolean ok) {
        confirme = ok;
        Stage stage = (Stage) annulerDialogueButton.getScene().getWindow();
        stage.close();
    }

    private void afficherMessageErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold; " +
                              "-fx-background-color: #FFEBEE; -fx-padding: 8 12; -fx-background-radius: 6;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void masquerMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private String calculerDateJour(String nomJour) {
        Map<String, Integer> ordreJours = Map.of(
                "Lundi", 1, "Mardi", 2, "Mercredi", 3,
                "Jeudi", 4, "Vendredi", 5, "Samedi", 6);
        java.time.LocalDate debut = java.time.LocalDate.parse(periodeDebut);
        int jourSemaine = debut.getDayOfWeek().getValue();
        int cible = ordreJours.getOrDefault(nomJour, 1);
        int delta = cible - jourSemaine;
        if (delta < 0) delta += 7;
        return debut.plusDays(delta).toString();
    }

    public boolean isConfirme() { return confirme; }
    public NouvelEdtControleur.CreneauTemp getCreneauTemp() { return creneauTemp; }
}