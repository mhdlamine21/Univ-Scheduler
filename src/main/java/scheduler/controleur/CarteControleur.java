package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.sql.SQLException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import scheduler.service.CoursService;
import scheduler.service.MatiereService;


/**
 * Carte interactive des bâtiments et salles avec filtres date dynamiques.
 */
public class CarteControleur extends TableauBordControleur {

    // COMPOSANTS FILTRES PRINCIPAUX
    @FXML private ComboBox<Ufr>      ufrCombo;
    @FXML private ComboBox<Batiment> batimentCombo;
    @FXML private ComboBox<Integer>  etageCombo;
    @FXML private CheckBox           montreOccupeesCheck;
    @FXML private Button             zoomInButton;
    @FXML private Button             zoomOutButton;
    @FXML private Button             actualiserButton;
    @FXML private Button             itineraireButton;

    // FILTRES DATE
    @FXML private CheckBox    filtreParDateCheck;
    @FXML private Label       infoTempsReelLabel;
    @FXML private Button      rechercherCarteButton;
    @FXML private HBox        panneauDateFiltre;
    @FXML private ToggleGroup typePeriodeGroup;
    @FXML private RadioButton radioJour;
    @FXML private RadioButton radioIntervalle;
    @FXML private DatePicker  dateUniquePicker;
    @FXML private DatePicker  dateDebutPicker;
    @FXML private DatePicker  dateFinPicker;
    @FXML private ComboBox<String> heureDebutCombo;
    @FXML private ComboBox<String> heureFinCombo;

    // RECHERCHE
    @FXML private TextField rechercheField;
    @FXML private Button    rechercherButton;
    @FXML private Label     resultatRechercheLabel;

    // CARTE
    @FXML private Label            infoLabel;
    @FXML private Label            dateOccupationLabel;
    @FXML private HBox             legendeBox;
    @FXML private Label            legendeLabel;
    @FXML private Label            chargementLabel;
    @FXML private ProgressIndicator chargementIndicator;
    @FXML private ScrollPane       carteScroll;
    @FXML private GridPane         carteGrid;

    // SERVICES
    private UfrService        ufrService;
    private BatimentService   batimentService;
    private SalleService      salleService;
    private PlanningService   planningService;
    private ReservationService reservationService;
    private CoursService      coursService;      
    private MatiereService    matiereService;    

    // DONNÉES
    private Map<Integer, StackPane>  sallesPanes     = new HashMap<>();
    private Map<Integer, Rectangle>  sallesRectangles = new HashMap<>();
    private Map<Integer, Salle>      sallesMap        = new HashMap<>();
    private Map<Integer, String>     occupationsActuelles = new HashMap<>();
    private List<Salle>    toutesSalles  = new ArrayList<>();
    private List<Batiment> tousBatiments = new ArrayList<>();
    private List<Ufr>      toutesUfr     = new ArrayList<>();
    private Batiment       batimentActuel;

    private double zoomLevel = 1.0;
    private static final double ZOOM_FACTOR = 0.15;
    private static final double MIN_ZOOM = 0.4;
    private static final double MAX_ZOOM = 2.5;

    @Override
    public void initialize() {
        super.initialize();
        ufrService         = new UfrService();
        batimentService    = new BatimentService();
        salleService       = new SalleService();
        planningService    = new PlanningService();
        reservationService = new ReservationService();
        coursService       = new CoursService();
        matiereService     = new MatiereService();

        configurerComposants();
        configurerFiltresDate();
        configurerLegende();

        demarrerHorlogeOccupation();

        chargerUfr();
        chargerTousBatiments();
    }

    private Timeline horlogeOccupationTimeline;

    /**
     * Démarre l'horloge dynamique pour le bandeau d'occupation en temps réel.
     */
    private void demarrerHorlogeOccupation() {
        if (horlogeOccupationTimeline != null) {
            horlogeOccupationTimeline.stop();
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd MMM yyyy - HH:mm:ss", java.util.Locale.FRENCH);
        Runnable actualiser = () -> {
            if (dateOccupationLabel != null && (radioJour == null || !radioJour.isSelected() && (radioIntervalle == null || !radioIntervalle.isSelected()))) {
                String texte = LocalDateTime.now().format(fmt);
                dateOccupationLabel.setText("Temps réel : " + Character.toUpperCase(texte.charAt(0)) + texte.substring(1));
            }
        };
        actualiser.run();
        horlogeOccupationTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> actualiser.run()));
        horlogeOccupationTimeline.setCycleCount(Animation.INDEFINITE);
        horlogeOccupationTimeline.play();
    }
        
        @Override
        public void initialiserAvecUtilisateur(Utilisateur utilisateur, Stage stage) {
            super.initialiserAvecUtilisateur(utilisateur, stage);
            this.primaryStage = stage;
            System.out.println("🔍 CarteControleur - primaryStage initialisé: " + stage);
            System.out.println("🔍 CarteControleur - Utilisateur: " + (utilisateur != null ? utilisateur.getEmail() : "null"));
            
            // Recharger les données si nécessaire
            if (batimentActuel != null) {
                chargerSallesBatiment(batimentActuel);
            } else {
                // Si aucun bâtiment n'est encore sélectionné, juste charger les UFR
                if (ufrCombo != null && ufrCombo.getItems().isEmpty()) {
                    chargerUfr();
                }
            }
    }

    @Override
    protected void initialiserTableauBord() {}

    @Override
    protected void rafraichirDonnees() { actualiserCarte(); }

    
    private void configurerFiltresDate() {
        // Heures disponibles
        List<String> heures = new ArrayList<>();
        for (int h = 6; h <= 22; h++) heures.add(String.format("%02d:00", h));
        if (heureDebutCombo != null) { heureDebutCombo.getItems().addAll(heures); heureDebutCombo.setValue("08:00"); }
        if (heureFinCombo   != null) { heureFinCombo.getItems().addAll(heures);   heureFinCombo.setValue("18:00"); }

        // Panneau date invisible par défaut
        if (panneauDateFiltre != null) { panneauDateFiltre.setVisible(false); panneauDateFiltre.setManaged(false); }

        // CheckBox filtre par date
        if (filtreParDateCheck != null) {
            filtreParDateCheck.selectedProperty().addListener((obs, o, n) -> {
                if (panneauDateFiltre != null) { panneauDateFiltre.setVisible(n); panneauDateFiltre.setManaged(n); }
                if (infoTempsReelLabel != null) infoTempsReelLabel.setText(n ? "Mode filtré par date" : "Affichage temps réel - maintenant");
            });
        }

        // Choix jour ou intervalle
        if (radioJour != null && radioIntervalle != null) {
            radioJour.selectedProperty().addListener((obs, o, n) -> {
                if (dateUniquePicker != null) { dateUniquePicker.setDisable(!n); }
                if (dateDebutPicker != null)  { dateDebutPicker.setDisable(n); }
                if (dateFinPicker != null)    { dateFinPicker.setDisable(n); }
            });
        }

        // Date par défaut
        if (dateUniquePicker != null) dateUniquePicker.setValue(LocalDate.now());
        if (dateDebutPicker  != null) dateDebutPicker.setValue(LocalDate.now());
        if (dateFinPicker    != null) dateFinPicker.setValue(LocalDate.now().plusDays(6));

        // Bouton rechercher
        if (rechercherCarteButton != null)
            rechercherCarteButton.setOnAction(e -> rechercherAvecFiltres());
    }

    /**
     * Lance la recherche avec les filtres date.
     */
    private void rechercherAvecFiltres() {
        if (batimentActuel == null) {
            afficherErreur("Veuillez d'abord sélectionner un bâtiment.");
            return;
        }

        boolean filtreActif = filtreParDateCheck != null && filtreParDateCheck.isSelected();

        if (!filtreActif) {
            // Mode temps réel
            chargerOccupationSalles(null, null, null, null);
            return;
        }

        // Mode filtré par date
        String heureDebut = heureDebutCombo != null ? heureDebutCombo.getValue() : null;
        String heureFin   = heureFinCombo   != null ? heureFinCombo.getValue()   : null;

        if (radioJour != null && radioJour.isSelected()) {
            // Un seul jour
            LocalDate jour = dateUniquePicker != null ? dateUniquePicker.getValue() : LocalDate.now();
            if (jour == null) { afficherErreur("Veuillez choisir un jour."); return; }
            chargerOccupationSalles(jour.toString(), jour.toString(), heureDebut, heureFin);
            if (infoTempsReelLabel != null)
                infoTempsReelLabel.setText("Filtrage : " + jour.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " " + (heureDebut != null ? heureDebut + "->" + heureFin : "toute la journée"));
        } else {
            // Intervalle
            LocalDate debut = dateDebutPicker != null ? dateDebutPicker.getValue() : null;
            LocalDate fin   = dateFinPicker   != null ? dateFinPicker.getValue()   : null;
            if (debut == null || fin == null) { afficherErreur("Veuillez définir un intervalle."); return; }
            if (fin.isBefore(debut)) { afficherErreur("La date de fin doit être après la date de début."); return; }
            chargerOccupationSalles(debut.toString(), fin.toString(), heureDebut, heureFin);
            if (infoTempsReelLabel != null)
                infoTempsReelLabel.setText("Filtrage : " + debut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " → " + fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
    }

    
    private void configurerComposants() {
        if (batimentCombo != null) batimentCombo.setDisable(true);
        if (etageCombo    != null) etageCombo.setDisable(true);

        if (ufrCombo != null) {
            ufrCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    if (batimentCombo != null) batimentCombo.setDisable(false);
                    chargerBatiments(n.getId());
                } else {
                    if (batimentCombo != null) { batimentCombo.setDisable(true); batimentCombo.getItems().clear(); }
                    if (etageCombo    != null) { etageCombo.setDisable(true); }
                    if (carteGrid     != null) carteGrid.getChildren().clear();
                }
            });
        }

        if (batimentCombo != null) {
            batimentCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    batimentActuel = n;
                    if (etageCombo != null) etageCombo.setDisable(false);
                    chargerSallesBatiment(n);
                }
            });
        }

        if (etageCombo != null) {
            etageCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                if (batimentActuel != null) filtrerSallesParEtage(n);
            });
        }

        if (montreOccupeesCheck != null) {
            montreOccupeesCheck.selectedProperty().addListener((obs, o, n) -> appliquerFiltresAffichage());
        }

        if (zoomInButton  != null) zoomInButton.setOnAction(e -> zoomIn());
        if (zoomOutButton != null) zoomOutButton.setOnAction(e -> zoomOut());
        if (actualiserButton != null) actualiserButton.setOnAction(e -> actualiserCarte());
        if (itineraireButton != null) itineraireButton.setOnAction(e -> ouvrirCalculItineraire(null));

        if (rechercheField  != null) rechercheField.setOnAction(e -> rechercher());
        if (rechercherButton != null) rechercherButton.setOnAction(e -> rechercher());
    }

    private void configurerLegende() {
        if (legendeBox == null) return;
        legendeBox.getChildren().clear();
        legendeBox.getChildren().addAll(
            creerItemLegende(Color.LIGHTGREEN, "Libre"),
            creerItemLegende(Color.INDIANRED,  "Occupée"),
            creerItemLegende(Color.GOLD,       "Bientôt occupée"),
            creerItemLegende(Color.LIGHTGRAY,  "Indisponible"),
            creerItemLegende(Color.ORCHID,     "Occupée (période)")
        );
    }

    private HBox creerItemLegende(Color color, String texte) {
        HBox box = new HBox(8);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Rectangle rect = new Rectangle(16, 16);
        rect.setFill(color);
        rect.setStroke(Color.web("#555"));
        rect.setStrokeWidth(0.8);
        Label label = new Label(texte);
        label.setStyle("-fx-font-size:11px;-fx-text-fill:#1A1F3A;");
        box.getChildren().addAll(rect, label);
        return box;
    }

    
    private void chargerUfr() {
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        Task<List<Ufr>> task = new Task<>() {
            @Override protected List<Ufr> call() throws SQLException {
                toutesUfr = ufrService.listerTous();
                return toutesUfr;
            }
            @Override protected void succeeded() {
                List<Ufr> ufrs = getValue();
                if (ufrCombo == null) return;
                ufrCombo.setCellFactory(lv -> new ListCell<>() {
                    @Override protected void updateItem(Ufr u, boolean empty) {
                        super.updateItem(u, empty); setText(empty || u == null ? null : u.getNom());
                    }
                });
                ufrCombo.setButtonCell(new ListCell<>() {
                    @Override protected void updateItem(Ufr u, boolean empty) {
                        super.updateItem(u, empty); setText(empty || u == null ? "Toutes les UFR" : u.getNom());
                    }
                });
                ufrCombo.getItems().setAll(ufrs);
                if (!ufrs.isEmpty()) ufrCombo.setValue(ufrs.get(0));
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            @Override protected void failed() {
                logger.error("Erreur UFR", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
        };
        new Thread(task).start();
    }

    private void chargerTousBatiments() {
        new Thread(() -> {
            try { tousBatiments = batimentService.listerTous(); } catch (SQLException e) { logger.error("", e); }
        }).start();
    }

    private void chargerBatiments(int ufrId) {
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        Task<List<Batiment>> task = new Task<>() {
            @Override protected List<Batiment> call() throws SQLException {
                return batimentService.listerParUfr(ufrId);
            }
            @Override protected void succeeded() {
                List<Batiment> bats = getValue();
                if (batimentCombo == null) return;
                batimentCombo.setCellFactory(lv -> new ListCell<>() {
                    @Override protected void updateItem(Batiment b, boolean empty) {
                        super.updateItem(b, empty);
                        if (empty || b == null) { setText(null); return; }
                        setText(b.getNom() + ("indisponible".equals(b.getStatut()) ? " (Indisponible)" : ""));
                    }
                });
                batimentCombo.setButtonCell(new ListCell<>() {
                    @Override protected void updateItem(Batiment b, boolean empty) {
                        super.updateItem(b, empty); setText(empty || b == null ? "Sélectionner un bâtiment" : b.getNom());
                    }
                });
                batimentCombo.getItems().setAll(bats);
                if (!bats.isEmpty()) batimentCombo.setValue(bats.get(0));
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            @Override protected void failed() {
                logger.error("Erreur bâtiments", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
        };
        new Thread(task).start();
    }

    private void chargerSallesBatiment(Batiment batiment) {
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        Task<List<Salle>> task = new Task<>() {
            @Override protected List<Salle> call() throws SQLException {
                return salleService.listerParBatiment(batiment.getId());
            }
            @Override protected void succeeded() {
                toutesSalles = getValue();
                sallesMap.clear();
                sallesPanes.clear();
                sallesRectangles.clear();
                for (Salle s : toutesSalles) sallesMap.put(s.getId(), s);

                Set<Integer> etages = new TreeSet<>();
                for (Salle s : toutesSalles) etages.add(s.getEtage());
                if (etageCombo != null) {
                    etageCombo.getItems().clear();
                    etageCombo.getItems().add(null);
                    etageCombo.getItems().addAll(etages);
                    etageCombo.setValue(null);
                }

                dessinerCarte(batiment, toutesSalles);

                // Occupation selon le mode (temps réel ou filtré)
                boolean filtreActif = filtreParDateCheck != null && filtreParDateCheck.isSelected();
                if (filtreActif) {
                    rechercherAvecFiltres();
                } else {
                    chargerOccupationSalles(null, null, null, null);
                }

                if (infoLabel != null)
                    infoLabel.setText("Bâtiment " + batiment.getNom() + " - " + toutesSalles.size() + " salle(s)");
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            @Override protected void failed() {
                logger.error("Erreur salles", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
        };
        new Thread(task).start();
    }

    /**
     * Dessine la carte du bâtiment.
     * @param batiment le bâtiment à dessiner
     * @param salles les salles du bâtiment
     */
    private void dessinerCarte(Batiment batiment, List<Salle> salles) {
        if (carteGrid == null) return;
        carteGrid.getChildren().clear();
        carteGrid.getColumnConstraints().clear();
        carteGrid.getRowConstraints().clear();

        Map<Integer, List<Salle>> parEtage = new TreeMap<>(Collections.reverseOrder());
        for (Salle s : salles) parEtage.computeIfAbsent(s.getEtage(), k -> new ArrayList<>()).add(s);

        int maxCols = parEtage.values().stream().mapToInt(List::size).max().orElse(0) + 1;
        for (int i = 0; i < maxCols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMinWidth(i == 0 ? 90 : 130);
            cc.setPrefWidth(i == 0 ? 110 : 150);
            cc.setHgrow(i == 0 ? Priority.NEVER : Priority.SOMETIMES);
            carteGrid.getColumnConstraints().add(cc);
        }

        int row = 0;
        for (Map.Entry<Integer, List<Salle>> entry : parEtage.entrySet()) {
            RowConstraints rc = new RowConstraints(160);
            rc.setMinHeight(150);
            carteGrid.getRowConstraints().add(rc);

            int etage = entry.getKey();
            VBox etageBox = new VBox(4);
            etageBox.setAlignment(Pos.CENTER);
            etageBox.setStyle("-fx-background-color:#E3F2FD;-fx-padding:10;-fx-background-radius:8;-fx-border-color:#90CAF9;-fx-border-radius:8;-fx-border-width:1;");
            etageBox.getChildren().addAll(
                labelEtage("Étage " + etage, "-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:#0D47A1;"),
                labelEtage(entry.getValue().size() + " salle(s)", "-fx-font-size:11px;-fx-text-fill:#1565C0;")
            );
            carteGrid.add(etageBox, 0, row);

            List<Salle> sallesEtage = entry.getValue();
            sallesEtage.sort(Comparator.comparing(Salle::getNumero));
            int col = 1;
            for (Salle s : sallesEtage) {
                StackPane pane = creerPaneSalle(s);
                carteGrid.add(pane, col++, row);
                sallesPanes.put(s.getId(), pane);
            }
            row++;
        }
    }

    private Label labelEtage(String t, String style) {
        Label l = new Label(t); l.setStyle(style); return l;
    }

    private StackPane creerPaneSalle(Salle salle) {
        StackPane pane = new StackPane();
        pane.setPrefSize(140, 155);
        pane.setPadding(new Insets(5));

        VBox vbox = new VBox(5);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color:white;-fx-padding:8;-fx-background-radius:8;" +
                      "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.1),5,0,0,2);");

        Rectangle rect = new Rectangle(105, 70);
        rect.setArcWidth(10); rect.setArcHeight(10);
        rect.setStroke(Color.web("#333")); rect.setStrokeWidth(0.8);
        rect.setFill(determinerCouleurInitiale(salle));
        sallesRectangles.put(salle.getId(), rect);

        Label numLbl = new Label(salle.getNumero());
        numLbl.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:#1976D2;");
        Label capLbl = new Label("Cap: " + salle.getCapacite());
        capLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#666;");
        Label typLbl = new Label(salle.getType());
        typLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#1565C0;-fx-font-weight:bold;");
        Label occLbl = new Label("");
        occLbl.setStyle("-fx-font-size:9px;-fx-text-fill:#C62828;");
        occLbl.setId("occ_" + salle.getId());
        occLbl.setWrapText(true);
        occLbl.setMaxWidth(120);

        vbox.getChildren().addAll(rect, numLbl, capLbl, typLbl, occLbl);
        pane.getChildren().add(vbox);
        pane.setCursor(Cursor.HAND);
        pane.setOnMouseClicked(e -> afficherDetailsSalle(salle));

        Tooltip tip = new Tooltip(
            "Salle " + salle.getNumero() + "\nCap: " + salle.getCapacite() +
            " | Type: " + salle.getType() + " | Étage " + salle.getEtage());
        tip.setStyle("-fx-font-size:12px;");
        Tooltip.install(pane, tip);

        return pane;
    }

    private Color determinerCouleurInitiale(Salle s) {
        return "indisponible".equals(s.getStatut()) ? Color.rgb(123, 131, 196) : Color.rgb(45, 198, 83);
    }

    //  OCCUPATION - AVEC OU SANS FILTRES DATE

    /**
     * Charge l'occupation des salles selon le mode (temps réel ou filtré).
     */
    private void chargerOccupationSalles(String dateDebut, String dateFin,
            String heureDebut, String heureFin) {
if (chargementIndicator != null) chargementIndicator.setVisible(true);

final boolean modeTempsReel = (dateDebut == null);
final String hdebut = modeTempsReel ? LocalDate.now().toString() : dateDebut;
final String hfin   = modeTempsReel ? LocalDate.now().toString() : dateFin;
final String hd     = modeTempsReel ? LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) : heureDebut;
final String hfi    = modeTempsReel ? LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) : heureFin;

Task<Map<Integer, String[]>> task = new Task<>() {
@Override
protected Map<Integer, String[]> call() throws SQLException {
Map<Integer, String[]> result = new HashMap<>();
String heureActuelle = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

LocalDate debut = LocalDate.parse(hdebut);
LocalDate fin   = LocalDate.parse(hfin);

LocalDate cur = debut;
while (!cur.isAfter(fin)) {
String dateStr = cur.toString();

// ✅ 1. Récupérer les créneaux des EDT validés
List<Creneau> creneaux = planningService.getCreneauxParJour(dateStr);
for (Creneau c : creneaux) {
if (c.getSalleId() == null) continue;
if ("annule".equals(c.getStatut())) continue;

boolean chevauche = chevauche(c.getHeureDebut(), c.getHeureFin(), hd, hfi, modeTempsReel, heureActuelle);
if (chevauche) {
String detail = dateStr.equals(LocalDate.now().toString()) ? 
  "Cours " + c.getHeureDebut() + "-" + c.getHeureFin() : 
  "Cours le " + dateStr;
result.put(c.getSalleId(), new String[]{"occupe_cours", detail});
} else if (modeTempsReel && estBientot(c.getHeureDebut(), heureActuelle)) {
if (!result.containsKey(c.getSalleId()))
  result.put(c.getSalleId(), new String[]{"bientot", "Cours à " + c.getHeureDebut()});
}
}

// ✅ 2. Récupérer les réservations
List<Reservation> reservations = reservationService.getReservationsJour(dateStr);
for (Reservation r : reservations) {
if (!("confirmee".equals(r.getStatut()) || "en_cours".equals(r.getStatut()))) continue;
boolean chevauche = chevauche(r.getHeureDebut(), r.getHeureFin(), hd, hfi, modeTempsReel, heureActuelle);
if (chevauche) {
result.put(r.getSalleId(), new String[]{"occupe_reservation", "Réservé: " + r.getMotif()});
}
}

cur = cur.plusDays(1);
}
return result;
}

@Override
protected void succeeded() {
Map<Integer, String[]> details = getValue();
appliquerCouleursOccupation(details, modeTempsReel);
appliquerFiltresAffichage();
if (chargementIndicator != null) chargementIndicator.setVisible(false);

String info = modeTempsReel ? 
"Temps réel : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) :
"Filtrage : " + hdebut + " → " + hfin + (hd != null ? " " + hd + "-" + hfi : "");
if (dateOccupationLabel != null) dateOccupationLabel.setText(info);
}

@Override
protected void failed() {
logger.error("Erreur occupation salles", getException());
if (chargementIndicator != null) chargementIndicator.setVisible(false);
}
};
new Thread(task).start();
}

//Ajouter cette méthode pour la légende dynamique
private void mettreAJourLegende() {
if (legendeBox == null) return;
legendeBox.getChildren().clear();
legendeBox.setSpacing(16);
legendeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

legendeBox.getChildren().addAll(
creerItemLegende(Color.LIGHTGREEN, "🟢 Libre"),
creerItemLegende(Color.INDIANRED, "🔴 Occupée (cours)"),
creerItemLegende(Color.ORANGE, "🟠 Occupée (réservation)"),
creerItemLegende(Color.GOLD, "🟡 Bientôt occupée"),
creerItemLegende(Color.LIGHTGRAY, "⚪ Indisponible")
);
}

    private boolean chevauche(String debut, String fin, String hd, String hfi,
                               boolean modeTempsReel, String heureActuelle) {
        if (modeTempsReel) {
            return debut.compareTo(heureActuelle) <= 0 && fin.compareTo(heureActuelle) > 0;
        }
        if (hd == null || hfi == null) return true; // pas de filtre heure → toute la journée
        return debut.compareTo(hfi) < 0 && fin.compareTo(hd) > 0;
    }

    private boolean estBientot(String heureDebut, String heureActuelle) {
        String[] dp = heureDebut.split(":");
        String[] hp = heureActuelle.split(":");
        int dMin = Integer.parseInt(dp[0]) * 60 + Integer.parseInt(dp[1]);
        int hMin = Integer.parseInt(hp[0]) * 60 + Integer.parseInt(hp[1]);
        int diff = dMin - hMin;
        return diff > 0 && diff <= 30;
    }

    private void appliquerCouleursOccupation(Map<Integer, String[]> details, boolean tempsReel) {
        for (Map.Entry<Integer, Rectangle> e : sallesRectangles.entrySet()) {
            int id = e.getKey();
            Rectangle rect = e.getValue();
            Salle salle = sallesMap.get(id);

            if (salle != null && "indisponible".equals(salle.getStatut())) {
                rect.setFill(Color.rgb(123, 131, 196)); // #7B83C4
            } else {
                String[] info = details.get(id);
                if (info == null) {
                    rect.setFill(Color.rgb(45, 198, 83)); // #2DC653 - VERT
                } else if (info[0].equals("occupe_cours") || info[0].equals("occupe_reservation")) {
                    rect.setFill(Color.rgb(229, 62, 62)); // #E53E3E - ROUGE
                } else if (info[0].equals("occupe_periode")) {
                    rect.setFill(Color.rgb(244, 162, 97)); // #F4A261 - ORANGE
                } else if (info[0].equals("bientot")) {
                    rect.setFill(Color.rgb(255, 193, 7)); // JAUNE
                }
            }

            // Mettre à jour le label occupation dans le pane
            StackPane pane = sallesPanes.get(id);
            if (pane != null) {
                pane.getChildren().forEach(node -> {
                    if (node instanceof VBox vbox) {
                        vbox.getChildren().forEach(child -> {
                            if (child instanceof Label lbl && lbl.getId() != null && lbl.getId().startsWith("occ_")) {
                                String[] info2 = details.get(id);
                                if (info2 != null) {
                                    lbl.setText(info2[1]);
                                    lbl.setVisible(true);
                                } else {
                                    lbl.setText(tempsReel ? "Libre" : "");
                                    lbl.setStyle("-fx-font-size:9px;-fx-text-fill:" + (tempsReel ? "#2E7D32" : "#718096") + ";");
                                    lbl.setVisible(true);
                                }
                            }
                        });
                    }
                });
            }
        }
    }

    private void appliquerFiltresAffichage() {
        if (montreOccupeesCheck == null) return;
        boolean montreOccupees = montreOccupeesCheck.isSelected();
        for (Map.Entry<Integer, StackPane> e : sallesPanes.entrySet()) {
            boolean estOccupee = occupationsActuelles.containsKey(e.getKey());
            if (!montreOccupees && estOccupee) {
                e.getValue().setVisible(false);
                e.getValue().setManaged(false);
            } else {
                e.getValue().setVisible(true);
                e.getValue().setManaged(true);
            }
        }
        filtrerSallesParEtage(etageCombo != null ? etageCombo.getValue() : null);
    }

    private void filtrerSallesParEtage(Integer etage) {
        for (Map.Entry<Integer, StackPane> e : sallesPanes.entrySet()) {
            Salle s = sallesMap.get(e.getKey());
            if (s == null) continue;
            boolean visible = (etage == null) || s.getEtage() == etage;
            if (visible) { // Ne pas re-montrer les cachées par le filtre occupées
                boolean occupee = occupationsActuelles.containsKey(e.getKey());
                boolean montreOccupees = montreOccupeesCheck == null || montreOccupeesCheck.isSelected();
                visible = montreOccupees || !occupee;
            }
            e.getValue().setVisible(visible);
            e.getValue().setManaged(visible);
        }
    }

    //  ACTIONS

    private void actualiserCarte() {
        if (batimentActuel != null) chargerSallesBatiment(batimentActuel);
    }

    private void rechercher() {
        if (rechercheField == null) return;
        String rech = rechercheField.getText().trim().toLowerCase();
        if (rech.isEmpty()) { if (resultatRechercheLabel != null) resultatRechercheLabel.setText(""); return; }

        for (Batiment b : tousBatiments) {
            if (b.getNom().toLowerCase().contains(rech)) {
                for (Ufr u : toutesUfr) {
                    if (u.getId() == b.getUfrId()) { if (ufrCombo != null) ufrCombo.setValue(u); break; }
                }
                if (batimentCombo != null) batimentCombo.setValue(b);
                if (resultatRechercheLabel != null)
                    resultatRechercheLabel.setText("Bâtiment trouvé : " + b.getNom());
                return;
            }
        }
        for (Salle s : toutesSalles) {
            if (s.getNumero().toLowerCase().contains(rech)) {
                mettreEnSurbrillance(s.getId());
                if (resultatRechercheLabel != null)
                    resultatRechercheLabel.setText("Salle trouvée : " + s.getNumero());
                return;
            }
        }
        if (resultatRechercheLabel != null)
            resultatRechercheLabel.setText("Aucun résultat pour : " + rech);
    }

    private void mettreEnSurbrillance(int salleId) {
        StackPane pane = sallesPanes.get(salleId);
        if (pane == null) return;
        String oldStyle = pane.getStyle();
        pane.setStyle(oldStyle + "-fx-border-color:#FF9800;-fx-border-width:3;-fx-border-radius:8;");
        new Thread(() -> {
            try { Thread.sleep(3000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            javafx.application.Platform.runLater(() -> pane.setStyle(oldStyle));
        }).start();
    }

    /**
     * Affiche les détails d'une salle.
     * @param salle la salle à afficher
     */
    private void afficherDetailsSalle(Salle salle) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails - Salle " + salle.getNumero());
        alert.setHeaderText(null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        String statut = occupationsActuelles.getOrDefault(salle.getId(), "libre");
        String couleurStatut = statut.startsWith("occupe") ? "#C62828" :
                               "bientot".equals(statut) ? "#E65100" : "#2E7D32";
        String texteStatut = statut.startsWith("occupe_cours") ? "Occupée (cours)" :
                             statut.startsWith("occupe_resa")  ? "Occupée (réservation)" :
                             statut.startsWith("occupe_per")   ? "Occupée sur la période" :
                             "bientot".equals(statut) ? "Bientôt occupée" : "Libre";

        content.getChildren().addAll(
            infoRow("Numéro :",   salle.getNumero()),
            infoRow("Capacité :", salle.getCapacite() + " places"),
            infoRow("Type :",     salle.getType()),
            infoRow("Étage :",    "Étage " + salle.getEtage()),
            infoRow("Bâtiment :", batimentActuel != null ? batimentActuel.getNom() : "?"),
            infoRow("Statut :",   salle.getStatut()),
            infoRowColor("Occupation :", texteStatut, couleurStatut)
        );

        // Bouton Voir planning (pour tous)
        Button btnPlanning = new Button("📅 Voir le planning");
        btnPlanning.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-background-radius: 7; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");
        btnPlanning.setOnAction(e -> {
            alert.close();
            ouvrirPlanningSalle(salle);
        });
        content.getChildren().add(btnPlanning);

        // Bouton Calculer Itinéraire
        Button btnItineraire = new Button("🧭 Calculer l'itinéraire vers cette salle");
        btnItineraire.setStyle("-fx-background-color: #6B4226; -fx-text-fill: white; -fx-background-radius: 7; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");
        btnItineraire.setOnAction(e -> {
            alert.close();
            ouvrirCalculItineraire(salle);
        });
        content.getChildren().add(btnItineraire);

        // Bouton Réserver si autorisé (Admin, Gestionnaire, Enseignant, ou Étudiant Délégué)
        boolean peutReserver = utilisateurConnecte != null && utilisateurConnecte.peutReserver();

        if (peutReserver) {
            Button btnReserver = new Button("📅 Réserver cette salle");
            btnReserver.setStyle("-fx-background-color:#1565C0;-fx-text-fill:white;-fx-background-radius:7;-fx-padding:8 16;-fx-font-weight:bold;-fx-cursor:hand;");
            btnReserver.setOnAction(e -> {
                alert.close();
                naviguerVersReservationSalle(salle);
            });
            content.getChildren().add(btnReserver);
        }

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinWidth(400);
        alert.show();
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

    private HBox infoRow(String label, String valeur) {
        Label l = new Label(label); l.setStyle("-fx-font-weight:bold;-fx-text-fill:#4A5568;-fx-min-width:100;");
        Label v = new Label(valeur); v.setStyle("-fx-text-fill:#1A202C;");
        HBox row = new HBox(10, l, v); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox infoRowColor(String label, String valeur, String couleur) {
        Label l = new Label(label); l.setStyle("-fx-font-weight:bold;-fx-text-fill:#4A5568;-fx-min-width:100;");
        Label v = new Label(valeur); v.setStyle("-fx-text-fill:" + couleur + ";-fx-font-weight:bold;");
        HBox row = new HBox(10, l, v); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * Navigue vers la réservation d'une salle (dans le contenu principal).
     * @param salle la salle à réserver
     */
    private void naviguerVersReservationSalle(Salle salle) {
        if (utilisateurConnecte == null) {
            afficherErreur("Session expirée. Veuillez vous reconnecter.");
            retourLogin();
            return;
        }
        
        try {
            System.out.println("🔍 Navigation vers Réservation depuis Carte");
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
            controleur.setProvientRecherche(false);
            controleur.setProvientCarte(true);
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
        // Méthode 1: Chercher par ID
        if (carteGrid != null && carteGrid.getScene() != null) {
            Parent root = carteGrid.getScene().getRoot();
            Node found = root.lookup("#contenuPrincipal");
            if (found instanceof StackPane) {
                return (StackPane) found;
            }
        }
        return null;
    }


    /**
     * Trouve le contenuPrincipal dans la hiérarchie des parents.
     */
    private StackPane trouverContenuPrincipal() {
        if (carteGrid != null && carteGrid.getScene() != null) {
            Parent root = carteGrid.getScene().getRoot();
            
            // Méthode 1: Chercher par ID
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

     
    /**
     * Récupère le stage depuis la scène courante
     */
    protected Stage getStageFromScene() {
        if (primaryStage != null) {
            return primaryStage;
        }
        // Chercher le stage depuis un composant visible
        if (carteGrid != null && carteGrid.getScene() != null) {
            return (Stage) carteGrid.getScene().getWindow();
        }
        if (ufrCombo != null && ufrCombo.getScene() != null) {
            return (Stage) ufrCombo.getScene().getWindow();
        }
        if (carteScroll != null && carteScroll.getScene() != null) {
            return (Stage) carteScroll.getScene().getWindow();
        }
        return null;
    }
    
    

    //  ZOOM

    private void zoomIn() {
        if (zoomLevel < MAX_ZOOM) {
            zoomLevel = Math.min(MAX_ZOOM, zoomLevel + ZOOM_FACTOR);
            carteGrid.setScaleX(zoomLevel);
            carteGrid.setScaleY(zoomLevel);
        }
    }

    private void zoomOut() {
        if (zoomLevel > MIN_ZOOM) {
            zoomLevel = Math.max(MIN_ZOOM, zoomLevel - ZOOM_FACTOR);
            carteGrid.setScaleX(zoomLevel);
            carteGrid.setScaleY(zoomLevel);
        }
    }

    /**
     * Ouvre la fenêtre interactive de calcul d'itinéraire vers une salle ou un bâtiment.
     * @param salleInitiale la salle de destination ciblée (ou null si sélection manuelle)
     */
    public void ouvrirCalculItineraire(Salle salleInitiale) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        Stage owner = getStageFromScene();
        if (owner != null) {
            modalStage.initOwner(owner);
        }
        modalStage.setTitle("🧭 Calculateur d'Itinéraire Universitaire - UIDT Thiès");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FBF8F3;");
        root.setPrefWidth(680);
        root.setMaxWidth(720);

        // En-tête stylé
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label("🧭");
        iconLbl.setStyle("-fx-font-size: 32px;");
        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Itinéraire & Guidage sur le Campus");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #3D261A;");
        Label subtitleLbl = new Label("Université Iba Der Thiam de Thiès • Navigation dynamique étape par étape");
        subtitleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C6D58;");
        titleBox.getChildren().addAll(titleLbl, subtitleLbl);
        header.getChildren().addAll(iconLbl, titleBox);

        // Sélecteurs Départ / Arrivée
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(14));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E6DCCD; -fx-border-width: 1;");

        Label departLbl = new Label("🚩 Point de départ :");
        departLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #3D261A;");
        ComboBox<String> departCombo = new ComboBox<>();
        departCombo.getItems().addAll(
            "🚪 Entrée Principale / Portail Sud (Avenue Léopold S. Senghor)",
            "🏛️ Rectorat & Services Centraux de l'UIDT",
            "📚 Bibliothèque Centrale Universitaire (BU)",
            "🍽️ Restaurant Universitaire (CROUS Thiès)",
            "🛏️ Pavillons Universitaires & Cité des Étudiants",
            "🏥 Centre Médico-Social Universitaire",
            "⚽ Complexe Sportif Universitaire & Terrain",
            "🚌 Arrêt Navettes & Transports Campus"
        );
        departCombo.setValue(departCombo.getItems().get(0));
        departCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(departCombo, Priority.ALWAYS);

        Label arriveeLbl = new Label("🎯 Destination :");
        arriveeLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #3D261A;");

        // Destination ComboBoxes
        HBox destBox = new HBox(8);
        destBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Ufr> ufrSelect = new ComboBox<>();
        ufrSelect.getItems().addAll(toutesUfr);
        ufrSelect.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Ufr item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getCode() != null ? item.getCode() + " - " : "") + item.getNom());
            }
        });
        ufrSelect.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Ufr item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choisir UFR" : (item.getCode() != null ? item.getCode() + " - " : "") + item.getNom());
            }
        });

        ComboBox<Batiment> batSelect = new ComboBox<>();
        batSelect.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Batiment item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        batSelect.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Batiment item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Bâtiment" : item.getNom());
            }
        });

        ComboBox<Salle> salleSelect = new ComboBox<>();
        salleSelect.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Salle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "Salle " + item.getNumero() + " (" + item.getType() + ")");
            }
        });
        salleSelect.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Salle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Salle" : "Salle " + item.getNumero());
            }
        });

        destBox.getChildren().addAll(ufrSelect, batSelect, salleSelect);

        grid.add(departLbl, 0, 0);
        grid.add(departCombo, 1, 0);
        grid.add(arriveeLbl, 0, 1);
        grid.add(destBox, 1, 1);

        // Cartouche de métriques
        HBox statsBox = new HBox(12);
        statsBox.setAlignment(Pos.CENTER);
        Label dureeBadge = creerBadgeMetrique("⏱️ Durée estimée", "4 min à pied", "#2E7D32");
        Label distanceBadge = creerBadgeMetrique("📏 Distance", "320 mètres", "#1565C0");
        Label accesBadge = creerBadgeMetrique("♿ Accès", "Escalier & Ascenseur", "#8C6D58");
        statsBox.getChildren().addAll(dureeBadge, distanceBadge, accesBadge);

        // Zone déroulante des étapes
        VBox etapesContainer = new VBox(10);
        etapesContainer.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #E6DCCD; -fx-border-width: 1;");
        
        ScrollPane etapesScroll = new ScrollPane(etapesContainer);
        etapesScroll.setFitToWidth(true);
        etapesScroll.setPrefHeight(260);
        etapesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Action de régénération de l'itinéraire
        Runnable regenererItineraire = () -> {
            String depart = departCombo.getValue();
            Salle destSalle = salleSelect.getValue();
            Batiment destBat = batSelect.getValue();
            Ufr destUfr = ufrSelect.getValue();

            etapesContainer.getChildren().clear();

            if (destSalle == null && destBat == null && destUfr == null) {
                Label vide = new Label("Veuillez sélectionner une destination pour afficher l'itinéraire.");
                vide.setStyle("-fx-text-fill: #8C6D58; -fx-font-style: italic;");
                etapesContainer.getChildren().add(vide);
                return;
            }

            int etage = (destSalle != null) ? destSalle.getEtage() : 0;
            String nomSalle = (destSalle != null) ? destSalle.getNumero() : (destBat != null ? "Bâtiment " + destBat.getNom() : "");
            String typeSalle = (destSalle != null) ? destSalle.getType() : "Espace académique";
            int capSalle = (destSalle != null) ? destSalle.getCapacite() : 0;
            String nomBat = (destBat != null) ? destBat.getNom() : "Bâtiment principal";
            String locBat = (destBat != null && destBat.getLocalisation() != null && !destBat.getLocalisation().isBlank()) ? destBat.getLocalisation() : "Axe central";
            String nomUfr = (destUfr != null) ? destUfr.getNom() : "UFR";
            String locUfr = (destUfr != null && destUfr.getLocalisation() != null && !destUfr.getLocalisation().isBlank()) ? destUfr.getLocalisation() : "Campus de Thiès";

            // Calcul distance & durée
            int baseDist = 280;
            if (depart != null) {
                if (depart.contains("Cité")) baseDist = 580;
                else if (depart.contains("CROUS")) baseDist = 420;
                else if (depart.contains("Rectorat")) baseDist = 220;
                else if (depart.contains("BU")) baseDist = 180;
                else if (depart.contains("Complexe")) baseDist = 650;
            }
            int distTotale = baseDist + (etage * 30);
            int minutes = Math.max(2, (int) Math.round(distTotale / 75.0));

            dureeBadge.setText("⏱️ Durée : ~" + minutes + " min");
            distanceBadge.setText("📏 Distance : " + distTotale + " m");
            accesBadge.setText(etage == 0 ? "♿ Accès : Plain-pied (RDC)" : "♿ Étage " + etage + " : Escalier / Ascenseur");

            // Construction des étapes
            etapesContainer.getChildren().addAll(
                creerEtapeRow(1, "🚶 Départ", "Départ depuis " + depart + ". S'engager sur l'allée piétonne principale ombragée."),
                creerEtapeRow(2, "🗺️ Orientation Campus", "Suivre les panneaux directionnels vers " + nomUfr + " (Secteur : " + locUfr + ")."),
                creerEtapeRow(3, "🏛️ Arrivée au Pôle", "Rejoindre le " + nomBat + " (" + locBat + "). Entrer par le hall principal du bâtiment."),
                creerEtapeRow(4, "🪜 Accès Niveau", etage == 0 ? "Rester au Rez-de-chaussée (RDC) dans le hall central." : "Prendre les escaliers ou l'ascenseur pour monter au " + etage + (etage == 1 ? "er" : "ème") + " étage."),
                creerEtapeRow(5, "🚪 Couloir & Signalétique", "Longer le couloir vers " + (etage % 2 == 0 ? "l'aile Ouest" : "l'aile Est") + ". Repérer la porte de la salle " + nomSalle + "."),
                creerEtapeRow(6, "🎯 Destination atteinte !", "Vous êtes arrivé à la Salle " + nomSalle + " (" + typeSalle + (capSalle > 0 ? ", " + capSalle + " places" : "") + ").")
            );
        };

        // Cascading listeners
        ufrSelect.valueProperty().addListener((obs, o, n) -> {
            batSelect.getItems().clear();
            salleSelect.getItems().clear();
            if (n != null) {
                try {
                    List<Batiment> bats = batimentService.listerParUfr(n.getId());
                    batSelect.getItems().addAll(bats);
                    if (!bats.isEmpty()) batSelect.setValue(bats.get(0));
                } catch (SQLException ex) {
                    logger.error("Erreur itinéraire batiments", ex);
                }
            }
            regenererItineraire.run();
        });

        batSelect.valueProperty().addListener((obs, o, n) -> {
            salleSelect.getItems().clear();
            if (n != null) {
                try {
                    List<Salle> sls = salleService.listerParBatiment(n.getId());
                    salleSelect.getItems().addAll(sls);
                    if (!sls.isEmpty()) salleSelect.setValue(sls.get(0));
                } catch (SQLException ex) {
                    logger.error("Erreur itinéraire salles", ex);
                }
            }
            regenererItineraire.run();
        });

        salleSelect.valueProperty().addListener((obs, o, n) -> regenererItineraire.run());
        departCombo.valueProperty().addListener((obs, o, n) -> regenererItineraire.run());

        // Pré-remplissage si salleInitiale fournie
        if (salleInitiale != null) {
            Batiment bCible = null;
            for (Batiment b : tousBatiments) {
                if (b.getId() == salleInitiale.getBatimentId()) {
                    bCible = b;
                    break;
                }
            }
            if (bCible != null) {
                for (Ufr u : toutesUfr) {
                    if (u.getId() == bCible.getUfrId()) {
                        ufrSelect.setValue(u);
                        break;
                    }
                }
                batSelect.setValue(bCible);
            }
            salleSelect.setValue(salleInitiale);
        } else if (!toutesUfr.isEmpty()) {
            if (ufrCombo != null && ufrCombo.getValue() != null) {
                ufrSelect.setValue(ufrCombo.getValue());
            } else {
                ufrSelect.setValue(toutesUfr.get(0));
            }
            if (batimentActuel != null) {
                batSelect.setValue(batimentActuel);
            }
        }

        regenererItineraire.run();

        // Boutons bas
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color: #8C6D58; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 18; -fx-font-weight: 600; -fx-cursor: hand;");
        fermerBtn.setOnAction(e -> modalStage.close());

        Button centrerBtn = new Button("🎯 Voir & Surligner sur la carte");
        centrerBtn.setStyle("-fx-background-color: #4361EE; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20; -fx-font-weight: 800; -fx-cursor: hand;");
        centrerBtn.setOnAction(e -> {
            Salle sCible = salleSelect.getValue();
            Batiment bCible = batSelect.getValue();
            Ufr uCible = ufrSelect.getValue();

            if (uCible != null && ufrCombo != null) ufrCombo.setValue(uCible);
            if (bCible != null && batimentCombo != null) batimentCombo.setValue(bCible);
            
            modalStage.close();

            if (sCible != null) {
                mettreEnSurbrillance(sCible.getId());
            }
        });

        actions.getChildren().addAll(fermerBtn, centrerBtn);

        root.getChildren().addAll(header, grid, statsBox, etapesScroll, actions);

        Scene scene = new Scene(root);
        modalStage.setScene(scene);
        modalStage.show();
    }

    private Label creerBadgeMetrique(String titre, String valeur, String couleurHex) {
        Label lbl = new Label(titre + " : " + valeur);
        lbl.setStyle("-fx-background-color: " + couleurHex + "15; -fx-text-fill: " + couleurHex + "; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 20; -fx-border-color: " + couleurHex + "; -fx-border-radius: 20; -fx-border-width: 1;");
        return lbl;
    }

    private HBox creerEtapeRow(int numero, String titre, String description) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);

        Label numBadge = new Label(String.valueOf(numero));
        numBadge.setMinSize(26, 26);
        numBadge.setMaxSize(26, 26);
        numBadge.setAlignment(Pos.CENTER);
        numBadge.setStyle("-fx-background-color: #D39A43; -fx-text-fill: #3D261A; -fx-font-weight: 900; -fx-font-size: 12px; -fx-background-radius: 13;");

        VBox content = new VBox(2);
        Label tLbl = new Label(titre);
        tLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #3D261A; -fx-font-size: 13px;");
        Label dLbl = new Label(description);
        dLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        dLbl.setWrapText(true);
        dLbl.setMaxWidth(560);
        content.getChildren().addAll(tLbl, dLbl);

        row.getChildren().addAll(numBadge, content);
        return row;
    }
}
