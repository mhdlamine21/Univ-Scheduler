package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.concurrent.Task;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.sql.SQLException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReservationControleur extends TableauBordControleur {
    
    @FXML private Label salleInfoLabel;
    @FXML private Label salleCapLabel;
    @FXML private Label salleTypeLabel2;
    @FXML private Label salleEqLabel;
    @FXML private HBox salleDetailsBox;
    @FXML private TextField salleField;
    @FXML private Button rechercherSalleButton;
    
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> heureDebutCombo;
    @FXML private ComboBox<String> heureFinCombo;
    
    @FXML private ToggleGroup typeReservationGroup;
    @FXML private RadioButton typeCoursCM;
    @FXML private RadioButton typeCoursTD;
    @FXML private RadioButton typeCoursTP;
    @FXML private RadioButton typeExamen;
    @FXML private RadioButton typeRattrapage;
    @FXML private RadioButton typeSoutenance;
    @FXML private RadioButton typeReunion;
    @FXML private RadioButton typeConference;
    @FXML private RadioButton typeAutre;
    
    @FXML private TextField motifField;
    @FXML private TextArea descriptionArea;
    
    @FXML private CheckBox prolongationCheck;
    @FXML private Label disponibiliteLabel;
    
    @FXML private Button verifierButton;
    @FXML private Button negocierButton;
    @FXML private Button reserverButton;
    @FXML private Button annulerButton;
    
    @FXML private VBox preferencesPane;
    @FXML private ComboBox<String> typeSalleCombo;
    @FXML private Spinner<Integer> capaciteMinSpinner;
    @FXML private ListView<Equipement> equipementsList;
    @FXML private ComboBox<Classe> classeCombo;
    @FXML private VBox classePane;
    @FXML private VBox enseignantSelectBox;
    @FXML private ComboBox<Utilisateur> enseignantCombo;
    @FXML private Label classeInfoLabel;
    @FXML private Button rechercheAutoButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private ReservationService reservationService;
    private SalleService salleService;
    private NotificationService notificationService;
    private EmailService emailService;
    private UtilisateurService utilisateurService;
    private ClasseService classeService;
    private EquipementService equipementService;
    private EchangeCreneauService echangeCreneauService;
    private CoursService coursService;
    
    private Salle salleSelectionnee;
    private Reservation reservationConflitActuelle;
    private boolean salleDisponible = false;
    private boolean provientRecherche = false;
    private boolean provientCarte = false;
    private String retourFxmlPath = null;
    
    private Timeline timelineRappels;
    private Set<Integer> reservationsNotifiees = new HashSet<>();
    private List<Equipement> tousEquipements;
    
    
    public void setProvientRecherche(boolean provient) {
        this.provientRecherche = provient;
        if (provient) {
            this.retourFxmlPath = "/fxml/Recherche.fxml";
        }
        System.out.println("🔍 ReservationControleur - provientRecherche = " + provient);
    }
    
    public void setProvientCarte(boolean provient) {
        this.provientCarte = provient;
        if (provient) {
            this.retourFxmlPath = "/fxml/Carte.fxml";
        }
        System.out.println("🔍 ReservationControleur - provientCarte = " + provient);
    }
    
    public void initialiserPourReprogrammation(int coursReporteId, String matiere, int classeId) {
        if (motifField != null) {
            motifField.setText("Reprogrammation : " + (matiere != null ? matiere : "Cours"));
        }
        if (typeRattrapage != null) {
            typeRattrapage.setSelected(true);
            if (classePane != null) classePane.setVisible(true);
        }
        if (classeId > 0 && classeCombo != null) {
            for (Classe c : classeCombo.getItems()) {
                if (c.getId() == classeId) {
                    classeCombo.setValue(c);
                    break;
                }
            }
        }
    }

    
    @Override
    public void initialize() {
        super.initialize();
        
        this.reservationService = new ReservationService();
        this.salleService = new SalleService();
        this.notificationService = new NotificationService();
        this.emailService = new EmailService();
        this.utilisateurService = new UtilisateurService();
        this.classeService = new ClasseService();
        this.equipementService = new EquipementService();
        this.echangeCreneauService = new EchangeCreneauService();
        this.coursService = new CoursService();
        this.tousEquipements = new ArrayList<>();
        
        demarrerVerificationRappels();
        
        // Initialisation des composants UI
        initialiserComposantsUI();
    }
    
    private void initialiserComposantsUI() {
        // Initialiser le spinner de capacité
        if (capaciteMinSpinner != null) {
            capaciteMinSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 10));
        }
        
        // Initialiser les combobox d'heures
        if (heureDebutCombo != null) {
            for (int h = 8; h <= 20; h++) {
                heureDebutCombo.getItems().add(String.format("%02d:00", h));
            }
            heureDebutCombo.setValue("08:00");
        }
        
        if (heureFinCombo != null) {
            for (int h = 8; h <= 22; h++) {
                heureFinCombo.getItems().add(String.format("%02d:00", h));
            }
            heureFinCombo.setValue("10:00");
        }
        
        // Initialiser le type de salle
        if (typeSalleCombo != null) {
            typeSalleCombo.getItems().addAll("Tous", "TD", "TP", "Amphi", "Autre");
            typeSalleCombo.setValue("Tous");
        }
        
        // Initialiser la date par défaut
        if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }
        
        // Rien de sélectionné par défaut dans les activités
        if (typeReservationGroup != null) {
            typeReservationGroup.selectToggle(null);
        }
        // Masquer classePane, enseignantSelectBox, negocierButton et salleDetailsBox par défaut
        if (classePane != null) { classePane.setVisible(false); classePane.setManaged(false); }
        if (enseignantSelectBox != null) { enseignantSelectBox.setVisible(false); enseignantSelectBox.setManaged(false); }
        if (negocierButton != null) { negocierButton.setVisible(false); negocierButton.setManaged(false); }
        if (salleDetailsBox != null) { salleDetailsBox.setVisible(false); salleDetailsBox.setManaged(false); }
    }
    
    @Override
    protected void initialiserTableauBord() {
        chargerEquipements();
        chargerClasses();
        chargerEnseignants();
        
        // Vérification des droits de réservation
        if (utilisateurConnecte != null && !utilisateurConnecte.peutReserver()) {
            if (reserverButton != null) reserverButton.setDisable(true);
            if (verifierButton != null) verifierButton.setDisable(true);
            if (disponibiliteLabel != null) {
                disponibiliteLabel.setText("⚠️ Réservation réservée aux enseignants, délégués et administrateurs");
                disponibiliteLabel.setStyle("-fx-text-fill: #9E2A2B; -fx-font-size: 11px;");
            }
        }
        
        // Configuration selon le rôle
        if (utilisateurConnecte != null) {
            boolean estDelegue = utilisateurConnecte.isResponsableClasse();
            boolean estAdminOuGest = "admin".equals(utilisateurConnecte.getRole()) || "gestionnaire".equals(utilisateurConnecte.getRole());
            if (enseignantSelectBox != null) {
                enseignantSelectBox.setVisible(estDelegue || estAdminOuGest);
                enseignantSelectBox.setManaged(estDelegue || estAdminOuGest);
            }
        }
        
        // Réinitialiser vérif si heure début change
        if (heureDebutCombo != null) {
            heureDebutCombo.valueProperty().addListener((obs, oldVal, newVal) -> reinitialiserVerification());
        }
        
        // Initialiser les boutons
        if (rechercherSalleButton != null) {
            rechercherSalleButton.setOnAction(e -> rechercherSalle());
        }
        
        if (verifierButton != null) {
            verifierButton.setOnAction(e -> verifierDisponibilite());
        }
        
        if (negocierButton != null) {
            negocierButton.setOnAction(e -> ouvrirDialogueNegociation());
        }
        
        if (reserverButton != null) {
            reserverButton.setDisable(true);
            reserverButton.setOnAction(e -> effectuerReservation());
        }
        
        if (annulerButton != null) {
            annulerButton.setOnAction(e -> handleRetour());
        }
        
        if (rechercheAutoButton != null) {
            rechercheAutoButton.setOnAction(e -> rechercherSalleAvecPreferences());
        }
        
        if (datePicker != null) {
            datePicker.valueProperty().addListener((obs, oldVal, newVal) -> reinitialiserVerification());
        }
        
        if (heureFinCombo != null) {
            heureFinCombo.valueProperty().addListener((obs, oldVal, newVal) -> reinitialiserVerification());
        }
        
        // Afficher classePane dès qu'une activité est cochée
        if (typeReservationGroup != null) {
            typeReservationGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                reinitialiserVerification();
                if (newVal != null && classePane != null) {
                    classePane.setVisible(true);
                    classePane.setManaged(true);
                }
            });
        }
    }
    
    @Override
    protected void rafraichirDonnees() {
        // Rien à rafraîchir
    }
    
    @Override
    public void handleRetour() {
        if (utilisateurConnecte == null) {
            retourLogin();
            return;
        }
        
        try {
            Stage stage = getStageFromScene();
            if (stage == null) {
                logger.error("Stage null pour retour");
                retourLogin();
                return;
            }
            
            // ✅ Déterminer la page de retour
            String fxmlToLoad;
            if (provientCarte) {
                fxmlToLoad = "/fxml/Carte.fxml";
            } else if (provientRecherche) {
                fxmlToLoad = "/fxml/Recherche.fxml";
            } else {
                fxmlToLoad = getFxmlPath(utilisateurConnecte.getRole());
            }
            
            System.out.println("🔍 Retour vers: " + fxmlToLoad);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlToLoad));
            Parent root = loader.load();
            
            Object controleur = loader.getController();
            if (controleur instanceof TableauBordControleur) {
                ((TableauBordControleur) controleur).initialiserAvecUtilisateur(utilisateurConnecte, stage);
            }
            
            // ✅ CHARGER DANS LE CONTENU PRINCIPAL
            StackPane contenuPrincipal = getContenuPrincipal();
            if (contenuPrincipal != null) {
                contenuPrincipal.getChildren().clear();
                contenuPrincipal.getChildren().add(root);
                System.out.println("✅ Retour chargé dans contenuPrincipal");
            } else {
                // Fallback
                Scene scene = new Scene(root);
                String css = getClass().getResource("/css/style.css").toExternalForm();
                if (css != null) scene.getStylesheets().add(css);
                stage.setScene(scene);
            }
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner");
        }
    }
    
    protected StackPane getContenuPrincipal() {
        if (primaryStage != null && primaryStage.getScene() != null) {
            Parent root = primaryStage.getScene().getRoot();
            Node found = root.lookup("#contenuPrincipal");
            if (found instanceof StackPane) {
                return (StackPane) found;
            }
        }
        return null;
    }

    /**
     * Récupère le stage depuis la scène.
     */
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (salleField != null && salleField.getScene() != null) {
            return (Stage) salleField.getScene().getWindow();
        }
        return null;
    }

    /**
     * Trouve le contenuPrincipal dans la hiérarchie des parents.
     */
    private StackPane trouverContenuPrincipal() {
        if (primaryStage != null && primaryStage.getScene() != null) {
            Parent root = primaryStage.getScene().getRoot();
            
            // Méthode 1: Chercher par ID
            Node found = root.lookup("#contenuPrincipal");
            if (found instanceof StackPane) {
                return (StackPane) found;
            }
            
            // Méthode 2: Parcourir la hiérarchie
            if (root instanceof BorderPane) {
                BorderPane bp = (BorderPane) root;
                if (bp.getCenter() instanceof StackPane) {
                    return (StackPane) bp.getCenter();
                }
                if (bp.getCenter() instanceof SplitPane) {
                    SplitPane sp = (SplitPane) bp.getCenter();
                    if (sp.getItems().size() > 1 && sp.getItems().get(1) instanceof StackPane) {
                        return (StackPane) sp.getItems().get(1);
                    }
                }
            }
        }
        return null;
    }

    
    private void chargerEquipements() {
        Task<List<Equipement>> task = new Task<>() {
            @Override
            protected List<Equipement> call() throws SQLException {
                return equipementService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                tousEquipements = getValue();
                if (equipementsList != null) {
                    equipementsList.getItems().setAll(tousEquipements);
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
                    equipementsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement équipements", getException());
            }
        };
        new Thread(task).start();
    }
    
    private void chargerClasses() {
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() throws SQLException {
                if (utilisateurConnecte != null && "enseignant".equals(utilisateurConnecte.getRole())) {
                    try {
                        List<Cours> coursProf = coursService.listerParEnseignant(utilisateurConnecte.getId());
                        Set<Integer> classeIds = new HashSet<>();
                        for (Cours c : coursProf) classeIds.add(c.getClasseId());
                        if (!classeIds.isEmpty()) {
                            List<Classe> filtered = new ArrayList<>();
                            for (Classe cl : classeService.listerToutes()) {
                                if (classeIds.contains(cl.getId())) filtered.add(cl);
                            }
                            if (!filtered.isEmpty()) return filtered;
                        }
                    } catch (Exception ignored) {}
                }
                return classeService.listerToutes();
            }
            
            @Override
            protected void succeeded() {
                List<Classe> classes = getValue();
                if (classeCombo != null) {
                    classeCombo.getItems().setAll(classes);
                    classeCombo.setCellFactory(lv -> new ListCell<Classe>() {
                        @Override
                        protected void updateItem(Classe item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : 
                                item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                        }
                    });
                    classeCombo.setButtonCell(new ListCell<Classe>() {
                        @Override
                        protected void updateItem(Classe item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? "Sélectionner une classe" : 
                                item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                        }
                    });
                    
                    classeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null && classeInfoLabel != null) {
                            classeInfoLabel.setText("Effectif: " + newVal.getEffectif() + " étudiants");
                        }
                    });

                    // Si c'est un étudiant délégué, sélectionner automatiquement sa classe
                    if (utilisateurConnecte instanceof Etudiant et && et.getClasseId() > 0) {
                        for (Classe cl : classes) {
                            if (cl.getId() == et.getClasseId()) {
                                classeCombo.setValue(cl);
                                break;
                            }
                        }
                    }
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement classes", getException());
            }
        };
        new Thread(task).start();
    }

    private void chargerEnseignants() {
        Task<List<Utilisateur>> task = new Task<>() {
            @Override
            protected List<Utilisateur> call() throws SQLException {
                return utilisateurService.listerParRole("enseignant");
            }

            @Override
            protected void succeeded() {
                List<Utilisateur> profs = getValue();
                if (enseignantCombo != null) {
                    enseignantCombo.getItems().setAll(profs);
                    enseignantCombo.setCellFactory(lv -> new ListCell<>() {
                        @Override
                        protected void updateItem(Utilisateur item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : item.getPrenom() + " " + item.getNom() + " (" + item.getEmail() + ")");
                        }
                    });
                    enseignantCombo.setButtonCell(new ListCell<>() {
                        @Override
                        protected void updateItem(Utilisateur item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? "Sélectionner un enseignant" : item.getPrenom() + " " + item.getNom());
                        }
                    });
                }
            }

            @Override
            protected void failed() {
                logger.warn("Impossible de charger les enseignants: {}", getException().getMessage());
            }
        };
        new Thread(task).start();
    }
    
    private void rechercherSalleAvecPreferences() {
        if (datePicker.getValue() == null || heureDebutCombo.getValue() == null) {
            afficherErreur("Veuillez remplir la date et l'heure");
            return;
        }
        
        if (heureFinCombo.getValue() == null) {
            afficherErreur("Veuillez sélectionner une heure de fin"); return;
        }
        String heureFin = heureFinCombo.getValue();
        
        int capaciteMin = capaciteMinSpinner != null ? capaciteMinSpinner.getValue() : 10;
        String typeSalle = (typeSalleCombo != null && !"Tous".equals(typeSalleCombo.getValue())) ? typeSalleCombo.getValue() : null;
        
        List<Integer> equipementsRequis = new ArrayList<>();
        if (equipementsList != null) {
            for (Equipement e : equipementsList.getSelectionModel().getSelectedItems()) {
                equipementsRequis.add(e.getId());
            }
        }
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws SQLException {
                return reservationService.getSallesDisponiblesAvecFiltres(
                    datePicker.getValue().toString(),
                    heureDebutCombo.getValue(),
                    heureFin,
                    capaciteMin,
                    typeSalle,
                    equipementsRequis
                );
            }
            
            @Override
            protected void succeeded() {
                List<Map<String, Object>> salles = getValue();
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                
                if (salles.isEmpty()) {
                    afficherErreur("Aucune salle disponible correspondant aux critères");
                    return;
                }
                
                List<String> options = new ArrayList<>();
                for (Map<String, Object> info : salles) {
                    Salle s = (Salle) info.get("salle");
                    options.add("Salle " + s.getNumero() + " - Cap: " + s.getCapacite() + 
                               " - Type: " + s.getType());
                }
                
                ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
                dialog.setTitle("Salles disponibles");
                dialog.setHeaderText(salles.size() + " salle(s) trouvée(s)");
                dialog.setContentText("Choisissez une salle :");
                
                dialog.showAndWait().ifPresent(choix -> {
                    String numero = choix.split(" ")[1];
                    for (Map<String, Object> info : salles) {
                        Salle s = (Salle) info.get("salle");
                        if (s.getNumero().equals(numero)) {
                            salleSelectionnee = s;
                            if (salleField != null) salleField.setText(s.getNumero());
                            if (salleInfoLabel != null) {
                                salleInfoLabel.setText(s.getNumero() + " - Cap: " + s.getCapacite() + 
                                                       " - " + s.getType());
                            }
                            verifierDisponibilite();
                            break;
                        }
                    }
                });
            }
            
            @Override
            protected void failed() {
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                afficherErreur("Erreur lors de la recherche");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Recherche une salle par son numéro.
     */
    private void rechercherSalle() {
        String numero = salleField != null ? salleField.getText().trim() : "";
        if (numero.isEmpty()) {
            afficherErreur("Veuillez entrer un numéro de salle");
            return;
        }
        
        Task<Salle> task = new Task<>() {
            @Override
            protected Salle call() throws SQLException {
                List<Salle> salles = salleService.listerTous();
                for (Salle s : salles) {
                    if (s.getNumero().equalsIgnoreCase(numero)) {
                        return s;
                    }
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                Salle salle = getValue();
                if (salle != null) {
                    afficherInfoSalle(salle);
                } else {
                    afficherErreur("Salle non trouvée : " + numero);
                    salleSelectionnee = null;
                    if (salleInfoLabel != null) salleInfoLabel.setText("");
                    cacheSalleDetails(false);
                }
            }
            @Override
            protected void failed() { afficherErreur("Erreur lors de la recherche"); }
        };
        new Thread(task).start();
    }

    /**
     * Affiche les détails d'une salle.
     * @param salle la salle à afficher
     */
    private void afficherInfoSalle(Salle salle) {
        System.out.println("🔍 afficherInfoSalle - Salle: " + salle.getNumero());
        
        salleSelectionnee = salle;
        
        if (salleField != null) {
            salleField.setText(salle.getNumero());
            System.out.println("   salleField.setText: " + salle.getNumero());
        } else {
            System.err.println("❌ salleField est null !");
        }
        
        if (salleInfoLabel != null) {
            salleInfoLabel.setText("✅ " + salle.getNumero());
        }
        
        if (salleCapLabel != null) {
            salleCapLabel.setText("Capacité : " + salle.getCapacite() + " places");
        }
        
        if (salleTypeLabel2 != null) {
            salleTypeLabel2.setText("Type : " + salle.getType());
        }
        
        if (salleEqLabel != null) {
            int nb = salle.getEquipements() != null ? salle.getEquipements().size() : 0;
            if (nb == 0) {
                salleEqLabel.setText("Équipements : aucun");
            } else {
                List<String> noms = new ArrayList<>();
                for (int id : salle.getEquipements()) {
                    for (Equipement eq : tousEquipements) {
                        if (eq.getId() == id) {
                            noms.add(eq.getNom());
                            break;
                        }
                    }
                }
                salleEqLabel.setText("Équipements : " + (noms.isEmpty() ? nb + " équipement(s)" : String.join(", ", noms)));
            }
        }
        
        cacheSalleDetails(true);
        
        // Pré-remplir capacité min et type dans les préférences
        if (capaciteMinSpinner != null && capaciteMinSpinner.getValueFactory() != null) {
            capaciteMinSpinner.getValueFactory().setValue(salle.getCapacite());
        }
        
        if (typeSalleCombo != null) {
            typeSalleCombo.setValue(salle.getType());
        }
        
        // Verrouiller les préférences (salle déjà choisie)
        if (capaciteMinSpinner != null) {
            capaciteMinSpinner.setDisable(true);
        }
        if (typeSalleCombo != null) {
            typeSalleCombo.setDisable(true);
        }
        if (equipementsList != null) {
            equipementsList.setDisable(true);
        }
        
        // Statut
        if (disponibiliteLabel != null && !"disponible".equals(salle.getStatut())) {
            disponibiliteLabel.setText("⚠️ Salle indisponible (travaux)");
            disponibiliteLabel.setStyle("-fx-text-fill: #f44336;");
        }
        
        reinitialiserVerification();
        
        verifierDisponibilite();
    }

    private void cacheSalleDetails(boolean visible) {
        if (salleDetailsBox != null) { salleDetailsBox.setVisible(visible); salleDetailsBox.setManaged(visible); }
    }
    
    /**
     * Vérifie la disponibilité de la salle pour le créneau sélectionné.
     */
    private void verifierDisponibilite() {
        if (utilisateurConnecte != null && !utilisateurConnecte.peutReserver()) {
            afficherErreur("Accès restreint : Seuls l'administrateur, le gestionnaire, les enseignants et les délégués/responsables de classe peuvent réserver.");
            return;
        }

        if (salleSelectionnee == null) {
            afficherErreur("Veuillez d'abord sélectionner une salle");
            return;
        }
        
        if (datePicker.getValue() == null || heureDebutCombo.getValue() == null) {
            afficherErreur("Veuillez remplir tous les champs");
            return;
        }
        
        if (!salleSelectionnee.getStatut().equals("disponible")) {
            if (disponibiliteLabel != null) {
                disponibiliteLabel.setText("❌ Salle indisponible (travaux)");
                disponibiliteLabel.setStyle("-fx-text-fill: #f44336;");
            }
            if (reserverButton != null) reserverButton.setDisable(true);
            if (negocierButton != null) { negocierButton.setVisible(false); negocierButton.setManaged(false); }
            return;
        }
        
        if (heureFinCombo.getValue() == null) {
            afficherErreur("Veuillez sélectionner une heure de fin"); return;
        }
        final String heureDebut = heureDebutCombo.getValue();
        final String heureFin = heureFinCombo.getValue();
        final String dateStr = datePicker.getValue().toString();
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                boolean dispo = reservationService.verifierDisponibilite(
                    salleSelectionnee.getId(),
                    dateStr,
                    heureDebut, heureFin
                );
                Map<String, Object> map = new HashMap<>();
                map.put("disponible", dispo);
                if (!dispo) {
                    Reservation conflit = reservationService.trouverConflit(salleSelectionnee.getId(), dateStr, heureDebut, heureFin);
                    map.put("conflit", conflit);
                    if (conflit != null) {
                        Utilisateur occ = utilisateurService.trouverParId(conflit.getUtilisateurId());
                        map.put("occupant", occ);
                    }
                }
                return map;
            }
            
            @Override
            protected void succeeded() {
                Map<String, Object> res = getValue();
                salleDisponible = (Boolean) res.get("disponible");
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                
                if (salleDisponible) {
                    reservationConflitActuelle = null;
                    if (disponibiliteLabel != null) {
                        disponibiliteLabel.setText("✅ Salle disponible");
                        disponibiliteLabel.setStyle("-fx-text-fill: #4CAF50;");
                    }
                    if (negocierButton != null) {
                        negocierButton.setVisible(false);
                        negocierButton.setManaged(false);
                    }
                    if (reserverButton != null) reserverButton.setDisable(false);
                } else {
                    reservationConflitActuelle = (Reservation) res.get("conflit");
                    Utilisateur occ = (Utilisateur) res.get("occupant");
                    if (disponibiliteLabel != null) {
                        if (reservationConflitActuelle != null && occ != null) {
                            disponibiliteLabel.setText("❌ Occupée par " + occ.getPrenom() + " " + occ.getNom() + " (" + reservationConflitActuelle.getMotif() + ")");
                        } else {
                            disponibiliteLabel.setText("❌ Salle déjà occupée à ce créneau");
                        }
                        disponibiliteLabel.setStyle("-fx-text-fill: #f44336;");
                    }
                    
                    if (negocierButton != null) {
                        boolean peutNegocier = reservationConflitActuelle != null && utilisateurConnecte != null && utilisateurConnecte.peutReserver();
                        negocierButton.setVisible(peutNegocier);
                        negocierButton.setManaged(peutNegocier);
                    }
                    if (reserverButton != null) reserverButton.setDisable(true);
                }
            }
            
            @Override
            protected void failed() {
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                afficherErreur("Erreur lors de la vérification");
            }
        };
        
        new Thread(task).start();
    }
    
    private String getTypeSelectionne() {
        if (typeReservationGroup == null) return "AUTRE";
        
        RadioButton selected = (RadioButton) typeReservationGroup.getSelectedToggle();
        if (selected == null) return null;
        
        if (selected == typeCoursCM) return "CM";
        if (selected == typeCoursTD) return "TD";
        if (selected == typeCoursTP) return "TP";
        if (selected == typeExamen) return "EXAMEN";
        if (selected == typeRattrapage) return "RATTRAPAGE";
        if (selected == typeSoutenance) return "SOUTENANCE";
        if (selected == typeReunion) return "REUNION";
        if (selected == typeConference) return "CONFERENCE";
        return "AUTRE";
    }
    
    private String getLibelleType(String type) {
        if (type == null) return "Événement";
        switch (type) {
            case "CM": return "Cours Magistral";
            case "TD": return "Travaux Dirigés";
            case "TP": return "Travaux Pratiques";
            case "EXAMEN": return "Examen";
            case "RATTRAPAGE": return "Rattrapage";
            case "SOUTENANCE": return "Soutenance";
            case "REUNION": return "Réunion";
            case "CONFERENCE": return "Conférence / Séminaire";
            default: return "Autre";
        }
    }
    
    /**
     * Préselectionne un créneau pour la réservation.
     * @param salle la salle à réserver
     * @param date la date
     * @param heureDebut l'heure de début
     * @param heureFin l'heure de fin
     */
    public void preselectionnerCreneau(Salle salle, String date, String heureDebut, String heureFin) {
        this.provientRecherche = true;
        afficherInfoSalle(salle);
        if (datePicker      != null) datePicker.setValue(LocalDate.parse(date));
        if (heureDebutCombo != null) heureDebutCombo.setValue(heureDebut);
        if (heureFinCombo   != null) heureFinCombo.setValue(heureFin);
        
    }

    /**
     * Préselectionne une salle pour la réservation.
     * @param salle la salle à présélectionner
     */
    public void preselectionnerSalle(Salle salle) {
        this.provientRecherche = true;
        System.out.println("🔍 ReservationControleur.preselectionnerSalle()");
        System.out.println("   Salle reçue: " + (salle != null ? salle.getNumero() : "null"));
        
        if (salle == null) {
            System.err.println("❌ Salle null dans preselectionnerSalle");
            return;
        }
        
        afficherInfoSalle(salle);
    }
    
    private String calculerHeureFin(String heureDebut, int dureeMinutes) {
        if (heureDebut == null) return "";
        String[] parts = heureDebut.split(":");
        if (parts.length < 2) return "";
        
        int heures = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        
        int totalMinutes = heures * 60 + minutes + dureeMinutes;
        heures = totalMinutes / 60;
        minutes = totalMinutes % 60;
        
        return String.format("%02d:%02d", heures, minutes);
    }
    
    /**
     * Effectue la réservation avec validation jours fériés sénégalais, jauge examen 50% et audit log.
     */
    private void effectuerReservation() {
        if (utilisateurConnecte != null && !utilisateurConnecte.peutReserver()) {
            afficherErreur("Accès restreint : Seuls l'administrateur, le gestionnaire, les enseignants et les délégués/responsables de classe peuvent effectuer une réservation.");
            return;
        }

        if (!salleDisponible) {
            afficherErreur("La salle n'est pas disponible");
            return;
        }
        
        if (datePicker.getValue() == null) {
            afficherErreur("Veuillez sélectionner une date");
            return;
        }
        
        String type = getTypeSelectionne();
        if (type == null) {
            afficherErreur("Veuillez sélectionner un type d'activité / événement");
            return;
        }
        
        LocalDate dateRes = datePicker.getValue();
        // 🇸🇳 Alerte Jours fériés sénégalais
        if (JourFerieService.estJourFerie(dateRes)) {
            String fete = JourFerieService.getLibelleFerie(dateRes).orElse("Fête chômée");
            Alert alertFerie = new Alert(Alert.AlertType.CONFIRMATION);
            alertFerie.setTitle("Alerte Calendrier Académique Sénégal");
            alertFerie.setHeaderText("Jour Férié Officiel : " + fete);
            alertFerie.setContentText("Le " + dateRes + " correspond à " + fete + " (jour chômé au Sénégal).\n\n"
                    + "Souhaitez-vous poursuivre la réservation sous dérogation rectorale exceptionnelle ?");
            Optional<ButtonType> rep = alertFerie.showAndWait();
            if (rep.isEmpty() || rep.get() != ButtonType.OK) {
                return;
            }
        } else if (JourFerieService.estDimanche(dateRes)) {
            Alert alertDim = new Alert(Alert.AlertType.CONFIRMATION);
            alertDim.setTitle("Campus Fermé - Dimanche");
            alertDim.setHeaderText("Date dominicale sélectionnée");
            alertDim.setContentText("Le " + dateRes + " est un dimanche. Le campus est habituellement fermé.\nContinuer la réservation ?");
            Optional<ButtonType> rep = alertDim.showAndWait();
            if (rep.isEmpty() || rep.get() != ButtonType.OK) {
                return;
            }
        }
        
        // 🎓 Règle Spéciale Examen : Jauge de sécurité 50% (Distanciation)
        if ("EXAMEN".equals(type) && salleSelectionnee != null && classeCombo != null && classeCombo.getValue() != null) {
            int capExam = salleSelectionnee.getCapacite() / 2;
            int effectif = classeCombo.getValue().getEffectif();
            if (effectif > capExam) {
                Alert alertExam = new Alert(Alert.AlertType.CONFIRMATION);
                alertExam.setTitle("Norme de Distanciation Examen (50%)");
                alertExam.setHeaderText("Capacité d'examen réduite dépassée !");
                alertExam.setContentText("Pour un examen, la capacité de la salle " + salleSelectionnee.getNumero() 
                    + " (" + salleSelectionnee.getCapacite() + " places) est réduite à 50% (" + capExam + " places).\n"
                    + "L'effectif de la classe est de " + effectif + " étudiants.\n\n"
                    + "Confirmez-vous la réservation malgré le risque d'espacement insuffisant ?");
                Optional<ButtonType> rep = alertExam.showAndWait();
                if (rep.isEmpty() || rep.get() != ButtonType.OK) {
                    return;
                }
            }
        }
        
        // Validation classe obligatoire pour les activités académiques
        boolean activiteAcademique = "CM".equals(type) || "TD".equals(type) || "TP".equals(type) || "EXAMEN".equals(type) || "RATTRAPAGE".equals(type);
        if (activiteAcademique && (classeCombo == null || classeCombo.getValue() == null)) {
            afficherErreur("Veuillez sélectionner la classe concernée par ce " + getLibelleType(type).toLowerCase());
            return;
        }

        if (motifField != null && motifField.getText().trim().isEmpty()) {
            afficherErreur("Veuillez indiquer un motif ou intitulé du cours/activité");
            return;
        }
        
        if (heureFinCombo.getValue() == null) {
            afficherErreur("Veuillez sélectionner une heure de fin");
            return;
        }
        
        final String heureFin = heureFinCombo.getValue();
        
        Reservation reservation = new Reservation(
            utilisateurConnecte.getId(),
            salleSelectionnee.getId(),
            type,
            motifField != null ? motifField.getText().trim() : "",
            datePicker.getValue().toString(),
            heureDebutCombo.getValue(),
            heureFin
        );
        
        if (descriptionArea != null) {
            reservation.setDescription(descriptionArea.getText().trim());
        }
        
        // Lier à la classe si sélectionnée
        if (classeCombo != null && classeCombo.getValue() != null) {
            reservation.setClasseId(classeCombo.getValue().getId());
        }

        // Lier à l'enseignant si sélectionné ou connecté
        if (enseignantCombo != null && enseignantCombo.getValue() != null) {
            reservation.setEnseignantId(enseignantCombo.getValue().getId());
        } else if ("enseignant".equals(utilisateurConnecte.getRole())) {
            reservation.setEnseignantId(utilisateurConnecte.getId());
        }
        
        reservation.setStatut("confirmee");
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        if (reserverButton != null) reserverButton.setDisable(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                reservationService.reserver(reservation);
                
                // Envoi des notifications classe et enseignant
                if (reservation.getClasseId() != null) {
                    envoyerNotifClasseReservation(reservation, type);
                }
                
                // 📜 Journalisation légale et immuable dans l'audit_log
                String nomSalle = salleSelectionnee != null ? salleSelectionnee.getNumero() : ("ID " + reservation.getSalleId());
                AuditService.logAsync(
                    utilisateurConnecte.getId(),
                    "RESERVATION",
                    "CREATION",
                    String.format("Salle %s | Date: %s (%s-%s) | Type: %s | Motif: %s | Classe: %s",
                        nomSalle, reservation.getDateReservation(), 
                        reservation.getHeureDebut(), reservation.getHeureFin(),
                        reservation.getType(), reservation.getMotif(),
                        reservation.getClasseId() != null ? "#" + reservation.getClasseId() : "Aucune")
                );
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                if (reserverButton != null) reserverButton.setDisable(false);
                
                afficherNotification("Réservation Réussie", "Votre réservation pour la salle " + 
                    (salleSelectionnee != null ? salleSelectionnee.getNumero() : "") + " a été validée.");
                
                // Proposer prolongation si cochée
                if (prolongationCheck != null && prolongationCheck.isSelected()) {
                    proposerProlongationApresReservation(reservation);
                }
                
                // Réinitialiser les champs
                if (motifField != null) motifField.clear();
                if (descriptionArea != null) descriptionArea.clear();
                reinitialiserVerification();
            }
            
            @Override
            protected void failed() {
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                if (reserverButton != null) reserverButton.setDisable(false);
                afficherErreur("Échec de la réservation : " + 
                    (getException() != null ? getException().getMessage() : "Erreur inconnue"));
            }
        };
        
        new Thread(task).start();
    }

    /**
     * Envoie une notification email et in-app aux étudiants de la classe concernée et à l'enseignant.
     */
    private void envoyerNotifClasseReservation(Reservation reservation, String type) {
        new Thread(() -> {
            try {
                UtilisateurService us = new UtilisateurService();
                List<Utilisateur> tous = us.listerParRole("etudiant");
                String salle = salleSelectionnee != null ? salleSelectionnee.getNumero() : "?";
                String motif = reservation.getMotif() != null ? reservation.getMotif() : "";
                String sujet = "📢 " + getLibelleType(type) + " - " + reservation.getDateReservation();
                String contenu = "Bonjour,\n\n" +
                    "Un " + getLibelleType(type).toLowerCase() + " a été planifié pour votre classe :\n\n" +
                    "📅 Date     : " + reservation.getDateReservation() + "\n" +
                    "⏰ Horaire  : " + reservation.getHeureDebut() + " → " + reservation.getHeureFin() + "\n" +
                    "🏫 Salle    : " + salle + "\n" +
                    "📝 Matière  : " + motif + "\n\n" +
                    "Cordialement,\nL'équipe UNIV-SCHEDULER";
                
                for (Utilisateur u : tous) {
                    if (u instanceof Etudiant e) {
                        if (e.getClasseId() == reservation.getClasseId() && e.isEstValide()) {
                            emailService.envoyerEmail(e.getEmail(), sujet, contenu);
                            notificationService.ajouterNotification(e.getId(), "📢 Nouveau " + getLibelleType(type) + " planifié en salle " + salle + " le " + reservation.getDateReservation());
                        }
                    }
                }

                // Notifier également l'enseignant s'il est spécifié
                if (reservation.getEnseignantId() != null) {
                    Utilisateur ens = us.trouverParId(reservation.getEnseignantId());
                    if (ens != null) {
                        String sujetProf = "📅 Séance programmée - Salle " + salle;
                        String msgProf = "Bonjour M./Mme " + ens.getNom() + ",\n\n"
                                + "Votre cours/séance de " + motif + " a été planifié pour votre classe le " + reservation.getDateReservation()
                                + " de " + reservation.getHeureDebut() + " à " + reservation.getHeureFin() + " en salle " + salle + ".\n\n"
                                + "L'équipe UNIV-SCHEDULER";
                        emailService.envoyerEmail(ens.getEmail(), sujetProf, msgProf);
                        notificationService.ajouterNotification(ens.getId(), "🏫 Séance de " + motif + " fixée en salle " + salle + " le " + reservation.getDateReservation());
                    }
                }
            } catch (Exception ex) {
                logger.warn("Erreur envoi notifs classe réservation: {}", ex.getMessage());
            }
        }).start();
    }

    /**
     * Ouvre la boîte de dialogue pour entamer une procédure de discussion et d'échange urgent.
     */
    private void ouvrirDialogueNegociation() {
        if (salleSelectionnee == null || datePicker.getValue() == null || heureDebutCombo.getValue() == null || heureFinCombo.getValue() == null) {
            afficherErreur("Créneau incomplet pour engager une négociation.");
            return;
        }

        if (reservationConflitActuelle == null) {
            afficherErreur("Impossible d'identifier l'occupant direct de ce créneau.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Négocier / Demander la libération du créneau");
        dialog.setHeaderText("Salle " + salleSelectionnee.getNumero() + " - " + datePicker.getValue() + " (" + heureDebutCombo.getValue() + " - " + heureFinCombo.getValue() + ")");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);

        Label infoOccupant = new Label("Recherche de l'occupant...");
        infoOccupant.setStyle("-fx-font-weight: bold; -fx-text-fill: #3D261A;");

        final int occupantId = reservationConflitActuelle.getUtilisateurId();
        try {
            Utilisateur occ = utilisateurService.trouverParId(occupantId);
            if (occ != null) {
                infoOccupant.setText("👤 Occupant : " + occ.getPrenom() + " " + occ.getNom() + " (" + occ.getEmail() + ")\n"
                        + "📋 Séance : " + reservationConflitActuelle.getMotif() + " (" + getLibelleType(reservationConflitActuelle.getType()) + ")");
            }
        } catch (SQLException ignored) {}

        Label lblUrgence = new Label("Type d'urgence :");
        lblUrgence.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B4226;");
        ComboBox<String> comboUrgence = new ComboBox<>();
        comboUrgence.getItems().addAll(
            "Examen officiel ou rattrapage impératif",
            "Cours de rattrapage avant clôture de semestre",
            "Soutenance de mémoire programmée",
            "Visite rectorale ou inspection",
            "Autre contrainte pédagogique majeure"
        );
        comboUrgence.setValue("Examen officiel ou rattrapage impératif");
        comboUrgence.setMaxWidth(Double.MAX_VALUE);

        Label lblMessage = new Label("Message adressé à l'enseignant occupant :");
        lblMessage.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B4226;");
        TextArea areaMessage = new TextArea();
        areaMessage.setPromptText("Ex: Bonjour cher collègue, nous avons une contrainte impérative d'examen pour la classe. Pourriez-vous s'il vous plaît accepter de différer votre séance ? Merci infiniment.");
        areaMessage.setPrefRowCount(4);
        areaMessage.setWrapText(true);

        Label note = new Label("💡 Si votre collègue accepte, sa séance sera reportée sur la liste de ses cours à reprogrammer, et la salle vous sera immédiatement libérée.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E7D32; -fx-font-style: italic;");
        note.setWrapText(true);

        content.getChildren().addAll(infoOccupant, lblUrgence, comboUrgence, lblMessage, areaMessage, note);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Envoyer la demande");
        okBtn.setStyle("-fx-background-color: #6B4226; -fx-text-fill: white; -fx-font-weight: bold;");

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String urgence = comboUrgence.getValue();
                String msg = areaMessage.getText().trim();
                new Thread(() -> {
                    try {
                        echangeCreneauService.demanderLiberation(
                            utilisateurConnecte.getId(),
                            occupantId,
                            null,
                            reservationConflitActuelle.getId(),
                            urgence,
                            msg
                        );
                        javafx.application.Platform.runLater(() -> {
                            afficherNotification("Demande envoyée", "Votre demande de libération/négociation a été transmise par email et notification à l'enseignant occupant.");
                        });
                    } catch (SQLException ex) {
                        logger.error("Erreur demande libération", ex);
                        javafx.application.Platform.runLater(() -> afficherErreur("Impossible d'envoyer la demande : " + ex.getMessage()));
                    }
                }).start();
            }
        });
    }
    
    private void proposerProlongationApresReservation(Reservation reservation) {
        try {
            Map<String, Object> resultat = reservationService.proposerProlongation(reservation.getId());
            boolean succes = (boolean) resultat.get("succes");
            
            if (succes) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> creneaux = (List<Map<String, Object>>) resultat.get("creneaux");
                if (creneaux != null && !creneaux.isEmpty()) {
                    List<String> options = new ArrayList<>();
                    for (Map<String, Object> c : creneaux) {
                        options.add("Prolonger de " + c.get("duree") + "h (" + 
                                   c.get("heureDebut") + " - " + c.get("heureFin") + ")");
                    }
                    
                    ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
                    dialog.setTitle("Prolonger la réservation");
                    dialog.setHeaderText("Souhaitez-vous prolonger votre réservation ?");
                    dialog.setContentText("La salle est disponible après votre créneau :");
                    
                    dialog.showAndWait().ifPresent(choix -> {
                        int index = options.indexOf(choix);
                        Map<String, Object> c = creneaux.get(index);
                        String heureFin = (String) c.get("heureFin");
                        
                        try {
                            boolean ok = reservationService.prolongerReservation(reservation.getId(), heureFin);
                            if (ok) {
                                afficherNotification("Prolongation réussie",
                                    "Réservation prolongée jusqu'à " + heureFin);
                            }
                        } catch (SQLException e) {
                            afficherErreur("Erreur lors de la prolongation");
                        }
                    });
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la proposition de prolongation", e);
        }
    }
    
    private void reinitialiserVerification() {
        salleDisponible = false;
        reservationConflitActuelle = null;
        if (reserverButton != null) reserverButton.setDisable(true);
        if (disponibiliteLabel != null) disponibiliteLabel.setText("");
        if (negocierButton != null) {
            negocierButton.setVisible(false);
            negocierButton.setManaged(false);
        }
    }
    
    private void demarrerVerificationRappels() {
        timelineRappels = new Timeline(
            new KeyFrame(Duration.minutes(1), event -> {
                verifierRappelsFinReservation();
            })
        );
        timelineRappels.setCycleCount(Timeline.INDEFINITE);
        timelineRappels.play();
    }
    
    private void verifierRappelsFinReservation() {
        new Thread(() -> {
            try {
                String aujourdhui = LocalDate.now().toString();
                LocalTime maintenant = LocalTime.now();
                LocalTime dans5Min = maintenant.plusMinutes(5);
                String heureDans5Min = dans5Min.format(DateTimeFormatter.ofPattern("HH:mm"));
                
                List<Reservation> reservationsDuJour = reservationService.getReservationsJour(aujourdhui);
                
                for (Reservation r : reservationsDuJour) {
                    if (reservationsNotifiees.contains(r.getId())) continue;
                    
                    String heureFin = r.getHeureFin();
                    if (heureFin != null && heureFin.equals(heureDans5Min)) {
                        if (r.getStatut().equals("confirmee") || r.getStatut().equals("en_cours")) {
                            envoyerRappelFinReservation(r);
                            reservationsNotifiees.add(r.getId());
                        }
                    }
                }
            } catch (SQLException e) {
                logger.error("Erreur lors de la vérification des rappels", e);
            }
        }).start();
    }
    
    private void envoyerRappelFinReservation(Reservation reservation) {
        try {
            Utilisateur utilisateur = utilisateurService.trouverParId(reservation.getUtilisateurId());
            if (utilisateur == null) return;
            
            Salle salle = salleService.trouverParId(reservation.getSalleId());
            String salleNom = (salle != null) ? salle.getNumero() : "Salle " + reservation.getSalleId();
            
            String sujet = "⏰ RAPPEL - Fin de réservation dans 5 minutes";
            String contenu = String.format(
                "Bonjour %s %s,\n\n" +
                "Votre réservation se termine dans 5 minutes.\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🏫 Salle      : %s\n" +
                "📅 Date       : %s\n" +
                "⏰ Horaire    : %s - %s\n" +
                "📋 Type       : %s\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Cordialement,\nL'équipe SCHEDULER",
                utilisateur.getPrenom(), utilisateur.getNom(),
                salleNom,
                reservation.getDateReservation(),
                reservation.getHeureDebut(), reservation.getHeureFin(),
                getLibelleType(reservation.getType())
            );
            
            emailService.envoyerEmail(utilisateur.getEmail(), sujet, contenu);
            
            notificationService.ajouterNotification(utilisateur.getId(),
                "⏰ Votre réservation de la salle " + salleNom + " se termine dans 5 minutes");
            
        } catch (SQLException e) {
            logger.error("Erreur lors de l'envoi du rappel", e);
        }
    }
    
    public void arreterRappels() {
        if (timelineRappels != null) {
            timelineRappels.stop();
        }
    }
    
    public void nettoyer() {
        arreterRappels();
    }
}