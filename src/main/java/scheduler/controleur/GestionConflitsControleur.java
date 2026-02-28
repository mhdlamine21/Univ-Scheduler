package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.sql.SQLException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des conflits (gestionnaire).
 * Affiche tous les conflits actifs et permet de les résoudre.
 */
public class GestionConflitsControleur extends TableauBordControleur {

    // COMPOSANTS FXML
    
    @FXML private TableView<Map<String, Object>> conflitsTable;
    @FXML private TableColumn<Map<String, Object>, String> typeColumn;
    @FXML private TableColumn<Map<String, Object>, String> descriptionColumn;
    @FXML private TableColumn<Map<String, Object>, String> sourceColumn;
    @FXML private TableColumn<Map<String, Object>, String> dateColumn;
    @FXML private TableColumn<Map<String, Object>, String> statutColumn;
    @FXML private TableColumn<Map<String, Object>, Void> actionsColumn;
    
    @FXML private Label totalConflitsLabel;
    @FXML private Label conflitsSalleLabel;
    @FXML private Label conflitsEnseignantLabel;
    @FXML private Label conflitsClasseLabel;
    @FXML private Label conflitsReservationLabel;
    @FXML private Button rafraichirButton;
    @FXML private Button toutReglerButton;
    @FXML private ProgressIndicator chargementIndicator;
    @FXML private ComboBox<String> filtreTypeCombo;
    @FXML private ComboBox<String> filtreStatutCombo;
    @FXML private TextField rechercheField;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    
    // SERVICES
    
    private ConflitService conflitService;
    private PlanningService planningService;
    private ReservationService reservationService;
    private SalleService salleService;
    private BatimentService batimentService;
    private CoursService coursService;
    private UtilisateurService utilisateurService;
    private EmailService emailService;
    private NotificationService notificationService;
    
    // DONNÉES
    
    private ObservableList<Map<String, Object>> conflitsList;
    private List<Map<String, Object>> tousConflits;
    private Timeline refreshTimeline;
    
    // CONSTANTES
    
    private static final int REFRESH_INTERVAL_SECONDS = 120; // 2 minutes au lieu de 30s
    
    // INITIALISATION
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.conflitService = new ConflitService();
        this.planningService = new PlanningService();
        this.reservationService = new ReservationService();
        this.salleService = new SalleService();
        this.batimentService = new BatimentService();
        this.coursService = new CoursService();
        this.utilisateurService = new UtilisateurService();
        this.emailService = new EmailService();
        this.notificationService = new NotificationService();
        
        this.conflitsList = FXCollections.observableArrayList();
        this.tousConflits = new ArrayList<>();
        
        configurerTableau();
        configurerFiltres();
        configurerEvenements();
        chargerConflits();

        // Rafraîchissement périodique SILENCIEUX (pas de fenêtre d'erreur)
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> {
                // Vérifier que l'écran est encore visible avant de rafraîchir
                boolean ecranVisible = conflitsTable != null
                        && conflitsTable.getScene() != null
                        && conflitsTable.getScene().getWindow() != null
                        && conflitsTable.getScene().getWindow().isShowing();
                if (!ecranVisible) {
                    // L'écran a été fermé, stopper le Timer proprement
                    if (refreshTimeline != null) refreshTimeline.stop();
                    return;
                }
                if (utilisateurConnecte != null && "gestionnaire".equals(utilisateurConnecte.getRole())) {
                    chargerConflitsSilencieux(); // version sans Alert
                }
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    @Override
    protected void initialiserTableauBord() {
        // Déjà fait dans initialize()
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerConflits();
    }
    
    @Override
    public void handleRetour() {
        try {
            Stage stage = getStageFromScene();
            if (stage == null) {
                logger.error("Stage null pour retour");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordGestionnaire.fxml"));
            Parent root = loader.load();
            
            TableauBordGestionnaireControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, stage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner au tableau de bord");
        }
    }
    
    
    
    // CONFIGURATION
    
    private void configurerTableau() {
        typeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> traduireType((String) cellData.getValue().get("type"))
            )
        );
        typeColumn.setPrefWidth(120);
        typeColumn.setStyle("-fx-alignment: CENTER;");
        
        descriptionColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> (String) cellData.getValue().get("description")
            )
        );
        descriptionColumn.setPrefWidth(450);
        descriptionColumn.setStyle("-fx-alignment: CENTER-LEFT; -fx-wrap-text: true;");
        
        sourceColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> (String) cellData.getValue().get("source")
            )
        );
        sourceColumn.setPrefWidth(150);
        sourceColumn.setStyle("-fx-alignment: CENTER;");
        
        dateColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> (String) cellData.getValue().get("date")
            )
        );
        dateColumn.setPrefWidth(120);
        dateColumn.setStyle("-fx-alignment: CENTER;");
        
        statutColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> (String) cellData.getValue().get("statut")
            )
        );
        statutColumn.setPrefWidth(100);
        statutColumn.setStyle("-fx-alignment: CENTER;");
        
        statutColumn.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(statut);
                    if ("non_resolu".equals(statut)) {
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if ("en_cours".equals(statut)) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if ("resolu".equals(statut)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });
        
        actionsColumn.setCellFactory(param -> new TableCell<Map<String, Object>, Void>() {
            private final Button autoBtn = new Button("🤖 Auto");
            private final Button manuelBtn = new Button("✏️ Manuel");
            private final Button detailsBtn = new Button("👁️");
            
            {
                autoBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 5; -fx-cursor: hand;");
                manuelBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 5; -fx-cursor: hand;");
                detailsBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 5; -fx-cursor: hand;");
                
                autoBtn.setOnAction(event -> {
                    Map<String, Object> conflit = getTableView().getItems().get(getIndex());
                    resoudreAutomatiquement(conflit);
                });
                
                manuelBtn.setOnAction(event -> {
                    Map<String, Object> conflit = getTableView().getItems().get(getIndex());
                    resoudreManuellement(conflit);
                });
                
                detailsBtn.setOnAction(event -> {
                    Map<String, Object> conflit = getTableView().getItems().get(getIndex());
                    afficherDetailsConflit(conflit);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Map<String, Object> conflit = getTableView().getItems().get(getIndex());
                    String statut = (String) conflit.get("statut");
                    HBox box = new HBox(5, detailsBtn, autoBtn, manuelBtn);
                    box.setAlignment(Pos.CENTER);
                    
                    if ("resolu".equals(statut)) {
                        autoBtn.setDisable(true);
                        manuelBtn.setDisable(true);
                    } else {
                        autoBtn.setDisable(false);
                        manuelBtn.setDisable(false);
                    }
                    
                    setGraphic(box);
                }
            }
        });
        actionsColumn.setPrefWidth(200);
        
        conflitsTable.setItems(conflitsList);
        conflitsTable.setPlaceholder(new Label("✅ Aucun conflit actif"));
        conflitsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Double-clic pour voir les détails
        conflitsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Map<String, Object> conflit = conflitsTable.getSelectionModel().getSelectedItem();
                if (conflit != null) {
                    afficherDetailsConflit(conflit);
                }
            }
        });
    }
    
    private void configurerFiltres() {
        filtreTypeCombo.getItems().addAll("Tous", "Salle", "Enseignant", "Classe", "Réservation", "Indisponibilité");
        filtreTypeCombo.setValue("Tous");
        
        filtreStatutCombo.getItems().addAll("Tous", "non_resolu", "en_cours", "resolu");
        filtreStatutCombo.setValue("Tous");
        
        rechercheField.setPromptText("Rechercher par description...");
        
        rechercherButton.setOnAction(e -> filtrerConflits());
        reinitialiserButton.setOnAction(e -> {
            filtreTypeCombo.setValue("Tous");
            filtreStatutCombo.setValue("Tous");
            rechercheField.clear();
            conflitsList.setAll(tousConflits);
        });
    }
    
    private void configurerEvenements() {
        rafraichirButton.setOnAction(e -> chargerConflits());
        toutReglerButton.setOnAction(e -> reglerTousLesConflits());
    }
    
    // CHARGEMENT DES CONFLITS
    
    private void chargerConflits() {
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return chargerConflitsDepuisBD();
            }
            
            @Override
            protected void succeeded() {
                tousConflits = getValue();
                conflitsList.setAll(tousConflits);
                mettreAJourStatistiques();
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement conflits", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
                // NE PAS appeler afficherErreur() ici - déclenché par le Timer toutes les 30s
                // → afficher juste dans le label de statut sans ouvrir de fenêtre
                if (totalConflitsLabel != null) {
                    totalConflitsLabel.setText("⚠️ Erreur");
                    totalConflitsLabel.setStyle("-fx-text-fill: #FF9800;");
                }
            }
        };

        new Thread(task).start();
    }

    /**
     * Rafraîchissement silencieux déclenché automatiquement par le Timer.
     * N'ouvre JAMAIS de fenêtre d'erreur - logge seulement en console.
     */

    /**
     * Charge tous les conflits depuis la base de données.
     * Méthode extraite pour être réutilisée par le refresh manuel et automatique.
     */
    private List<Map<String, Object>> chargerConflitsDepuisBD() throws Exception {
        List<Map<String, Object>> conflits = new ArrayList<>();
        
        List<EmploiDuTemps> edtsActifs = planningService.listerActifs();
        for (EmploiDuTemps edt : edtsActifs) {
            List<Creneau> creneaux = planningService.getCreneauxParEmploiDuTemps(edt.getId());
            for (Creneau creneau : creneaux) {
                List<ConflitService.ResultatConflit> resultats = conflitService.verifierConflitsAvecSolutions(creneau);
                for (ConflitService.ResultatConflit r : resultats) {
                    Map<String, Object> conflit = new HashMap<>();
                    conflit.put("type", r.getTypeConflit());
                    conflit.put("description", r.getDescription());
                    conflit.put("source", "EDT #" + edt.getId());
                    conflit.put("date", creneau.getJour());
                    conflit.put("statut", "non_resolu");
                    conflit.put("creneau", creneau);
                    conflit.put("solutions", r.getSolutions());
                    conflit.put("edtId", edt.getId());
                    conflits.add(conflit);
                    
                    List<Map<String, Object>> solutionsSerialisables = new ArrayList<>();
                    for (ConflitService.SolutionConflit sol : r.getSolutions()) {
                        Map<String, Object> solMap = new HashMap<>();
                        solMap.put("type", sol.getType());
                        solMap.put("description", sol.getDescription());
                        
                        // Gérer la valeur (peut être Salle, String[], etc.)
                        Object valeur = sol.getValeur();
                        if (valeur instanceof Salle) {
                            Salle s = (Salle) valeur;
                            Map<String, Object> salleMap = new HashMap<>();
                            salleMap.put("id", s.getId());
                            salleMap.put("numero", s.getNumero());
                            salleMap.put("capacite", s.getCapacite());
                            solMap.put("valeur", salleMap);
                        } else {
                            solMap.put("valeur", valeur);
                        }
                        
                        solutionsSerialisables.add(solMap);
                    }
                    conflit.put("solutions", solutionsSerialisables);
                    
                    conflits.add(conflit);
                }
            }
        }
        
        // 2. Conflits dans les réservations
        List<Reservation> reservations = reservationService.listerTous();
        for (Reservation r : reservations) {
            if ("annulee".equals(r.getStatut()) || "refusee".equals(r.getStatut())) continue;
            
            boolean salleLibre = reservationService.verifierDisponibilite(
                r.getSalleId(), r.getDateReservation(), 
                r.getHeureDebut(), r.getHeureFin()
            );
            
            boolean pasDeCours = planningService.salleEstDisponible(
                r.getSalleId(), r.getDateReservation(),
                r.getHeureDebut(), r.getHeureFin()
            );
            
            if (!salleLibre || !pasDeCours) {
                Map<String, Object> conflit = new HashMap<>();
                conflit.put("type", "RESERVATION");
                conflit.put("description", "Réservation en conflit - Salle #" + r.getSalleId() + 
                    " le " + r.getDateReservation() + " de " + r.getHeureDebut() + " à " + r.getHeureFin());
                conflit.put("source", "Réservation #" + r.getId());
                conflit.put("date", r.getDateReservation());
                conflit.put("statut", "non_resolu");
                conflit.put("reservation", r);
                conflits.add(conflit);
            }
        }
        
        // 3. Salles indisponibles avec cours planifiés
        List<Salle> sallesIndisponibles = salleService.listerTous().stream()
            .filter(s -> "indisponible".equals(s.getStatut()))
            .collect(Collectors.toList());
        
        for (Salle s : sallesIndisponibles) {
            List<Creneau> creneaux = planningService.getCreneauxParSalle(s.getId());
            LocalDate aujourdhui = LocalDate.now();
            
            for (Creneau c : creneaux) {
                LocalDate dateCreneau = LocalDate.parse(c.getJour());
                if (!dateCreneau.isBefore(aujourdhui)) {
                    Map<String, Object> conflit = new HashMap<>();
                    conflit.put("type", "SALLE_INDISPONIBLE");
                    conflit.put("description", "La salle " + s.getNumero() + " est indisponible mais un cours y est planifié le " + c.getJour());
                    conflit.put("source", "Salle #" + s.getId());
                    conflit.put("date", c.getJour());
                    conflit.put("statut", "non_resolu");
                    conflit.put("salle", s);
                    conflit.put("creneau", c);
                    conflits.add(conflit);
                }
            }
        }
        
        // 4. Bâtiments indisponibles avec salles utilisées
        List<Batiment> batimentsIndisponibles = batimentService.listerTous().stream()
            .filter(b -> "indisponible".equals(b.getStatut()))
            .collect(Collectors.toList());
        
        for (Batiment b : batimentsIndisponibles) {
            List<Salle> sallesBatiment = salleService.listerParBatiment(b.getId());
            for (Salle s : sallesBatiment) {
                List<Creneau> creneaux = planningService.getCreneauxParSalle(s.getId());
                LocalDate aujourdhui = LocalDate.now();
                
                for (Creneau c : creneaux) {
                    LocalDate dateCreneau = LocalDate.parse(c.getJour());
                    if (!dateCreneau.isBefore(aujourdhui)) {
                        Map<String, Object> conflit = new HashMap<>();
                        conflit.put("type", "BATIMENT_INDISPONIBLE");
                        conflit.put("description", "Le bâtiment " + b.getNom() + " est indisponible mais la salle " + s.getNumero() + " y a un cours le " + c.getJour());
                        conflit.put("source", "Bâtiment #" + b.getId());
                        conflit.put("date", c.getJour());
                        conflit.put("statut", "non_resolu");
                        conflit.put("batiment", b);
                        conflit.put("salle", s);
                        conflit.put("creneau", c);
                        conflits.add(conflit);
                    }
                }
            }
        }
        
        // Éliminer les doublons
        Set<String> ids = new HashSet<>();
        List<Map<String, Object>> uniques = new ArrayList<>();
        for (Map<String, Object> c : conflits) {
            String id = c.get("type") + "_" + c.get("description");
            if (!ids.contains(id)) {
                ids.add(id);
                uniques.add(c);
            }
        }
        
        return uniques;
    }

    private void chargerConflitsSilencieux() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return chargerConflitsDepuisBD();
            }

            @Override
            protected void succeeded() {
                tousConflits = getValue();
                conflitsList.setAll(tousConflits);
                mettreAJourStatistiques();
            }

            @Override
            protected void failed() {
                // Silencieux : juste un log, pas de fenêtre
                logger.warn("Rafraîchissement auto des conflits échoué : {}",
                        getException() != null ? getException().getMessage() : "?");
            }
        };
        new Thread(task).start();
    }
    
    
    private void mettreAJourStatistiques() {
        int total = tousConflits.size();
        int salle = 0, enseignant = 0, classe = 0, reservation = 0, indispo = 0;
        
        for (Map<String, Object> c : tousConflits) {
            String type = (String) c.get("type");
            switch (type) {
                case "SALLE": salle++; break;
                case "ENSEIGNANT": enseignant++; break;
                case "CLASSE": classe++; break;
                case "RESERVATION": reservation++; break;
                case "SALLE_INDISPONIBLE": indispo++; break;
                case "BATIMENT_INDISPONIBLE": indispo++; break;
            }
        }
        
        totalConflitsLabel.setText(String.valueOf(total));
        conflitsSalleLabel.setText(String.valueOf(salle + indispo));
        conflitsEnseignantLabel.setText(String.valueOf(enseignant));
        conflitsClasseLabel.setText(String.valueOf(classe));
        conflitsReservationLabel.setText(String.valueOf(reservation));
    }
    
    private void filtrerConflits() {
        if (tousConflits == null || tousConflits.isEmpty()) return;
        
        String typeFiltre = filtreTypeCombo.getValue();
        String statutFiltre = filtreStatutCombo.getValue();
        String recherche = rechercheField.getText().toLowerCase().trim();
        
        List<Map<String, Object>> filtres = new ArrayList<>(tousConflits);
        
        if (!"Tous".equals(typeFiltre)) {
            String typeCode = typeFiltre.equals("Salle") ? "SALLE" :
                              typeFiltre.equals("Enseignant") ? "ENSEIGNANT" :
                              typeFiltre.equals("Classe") ? "CLASSE" :
                              typeFiltre.equals("Réservation") ? "RESERVATION" : 
                              "SALLE_INDISPONIBLE";
            filtres.removeIf(c -> !((String) c.get("type")).equals(typeCode));
        }
        
        if (!"Tous".equals(statutFiltre)) {
            filtres.removeIf(c -> !((String) c.get("statut")).equals(statutFiltre));
        }
        
        if (!recherche.isEmpty()) {
            filtres.removeIf(c -> !((String) c.get("description")).toLowerCase().contains(recherche));
        }
        
        conflitsList.setAll(filtres);
    }
    
    // RÉSOLUTION DES CONFLITS
    
    private void resoudreAutomatiquement(Map<String, Object> conflit) {
        // ✅ RÉCUPÉRER LES SOLUTIONS
        List<Map<String, Object>> solutions = null;
        
        Object solutionsObj = conflit.get("solutions");
        if (solutionsObj instanceof List) {
            solutions = (List<Map<String, Object>>) solutionsObj;
        }
        
        if (solutions == null || solutions.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Aucune solution automatique");
            alert.setHeaderText("Aucune solution automatique disponible pour ce conflit");
            alert.setContentText("Ce conflit ne peut pas être résolu automatiquement.\n\n" +
                                "Veuillez utiliser la résolution manuelle pour :\n" +
                                "• Changer la salle manuellement\n" +
                                "• Modifier l'horaire du cours\n" +
                                "• Contacter l'enseignant concerné");
            alert.show();
            return;
        }
        
        // ✅ CRÉER UNE COPIE FINALE POUR LA LAMBDA
        final List<Map<String, Object>> solutionsFinales = new ArrayList<>(solutions);
        
        // Créer la liste des options
        List<String> options = new ArrayList<>();
        for (Map<String, Object> sol : solutionsFinales) {
            String desc = (String) sol.get("description");
            options.add(desc != null ? desc : "Solution");
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Résolution automatique");
        dialog.setHeaderText("Choisissez une solution pour résoudre le conflit");
        dialog.setContentText("Solution proposée :");
        
        dialog.showAndWait().ifPresent(choix -> {
            int index = options.indexOf(choix);
            Map<String, Object> solutionMap = solutionsFinales.get(index);
            
            // ✅ RECONSTRUIRE LA SOLUTION
            String type = (String) solutionMap.get("type");
            String description = (String) solutionMap.get("description");
            Object valeur = solutionMap.get("valeur");
            
            // Si la valeur est une Map (salle sérialisée), reconstruire l'objet Salle
            if (valeur instanceof Map) {
                Map<String, Object> salleMap = (Map<String, Object>) valeur;
                Integer salleId = (Integer) salleMap.get("id");
                if (salleId != null) {
                    try {
                        valeur = salleService.trouverParId(salleId);
                    } catch (SQLException e) {
                        logger.error("Erreur récupération salle", e);
                    }
                }
            }
            
            ConflitService.SolutionConflit solution = new ConflitService.SolutionConflit(type, description, valeur);
            
            // Appliquer la solution
            try {
                boolean applique = appliquerSolution(conflit, solution);
                if (applique) {
                    conflit.put("statut", "resolu");
                    afficherNotification("Succès", "Conflit résolu avec succès");
                    chargerConflits();
                } else {
                    afficherErreur("Impossible d'appliquer la solution");
                }
            } catch (SQLException e) {
                logger.error("Erreur lors de la résolution", e);
                afficherErreur("Erreur lors de la résolution : " + e.getMessage());
            }
        });
    }    
    private boolean appliquerSolution(Map<String, Object> conflit, ConflitService.SolutionConflit solution) throws SQLException {
        String typeSolution = solution.getType();
        Creneau creneau = (Creneau) conflit.get("creneau");
        Reservation reservation = (Reservation) conflit.get("reservation");
        Object valeur = solution.getValeur();
        
        System.out.println("🔧 Application de la solution : " + typeSolution);
        System.out.println("   Description : " + solution.getDescription());
        System.out.println("   Valeur type : " + (valeur != null ? valeur.getClass().getSimpleName() : "null"));
        
        if ("CHANGER_SALLE".equals(typeSolution)) {
            Salle nouvelleSalle = null;
            
            // Gérer différents types de valeurs possibles
            if (valeur instanceof Salle) {
                nouvelleSalle = (Salle) valeur;
            } else if (valeur instanceof Map) {
                // Si la valeur est une Map (cas de sérialisation)
                Map<String, Object> salleMap = (Map<String, Object>) valeur;
                Integer salleId = (Integer) salleMap.get("id");
                if (salleId != null) {
                    nouvelleSalle = salleService.trouverParId(salleId);
                }
            }
            
            if (nouvelleSalle == null) {
                System.err.println("❌ Nouvelle salle est null");
                return false;
            }
            
            System.out.println("   Nouvelle salle : " + nouvelleSalle.getNumero());
            
            if (creneau != null) {
                creneau.setSalleId(nouvelleSalle.getId());
                planningService.modifierCreneau(creneau);
                System.out.println("✅ Créneau #" + creneau.getId() + " modifié avec salle #" + nouvelleSalle.getId());
                return true;
            } else if (reservation != null) {
                reservation.setSalleId(nouvelleSalle.getId());
                reservationService.modifier(reservation);
                System.out.println("✅ Réservation #" + reservation.getId() + " modifiée avec salle #" + nouvelleSalle.getId());
                return true;
            }
            
        } else if ("CHANGER_HORAIRE".equals(typeSolution)) {
            String[] heures = null;
            
            if (valeur instanceof String[]) {
                heures = (String[]) valeur;
            } else if (valeur instanceof List) {
                List<?> list = (List<?>) valeur;
                heures = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    heures[i] = String.valueOf(list.get(i));
                }
            }
            
            if (heures == null || heures.length < 2) {
                System.err.println("❌ Heures invalides");
                return false;
            }
            
            System.out.println("   Nouvel horaire : " + heures[0] + " - " + heures[1]);
            
            if (creneau != null) {
                creneau.setHeureDebut(heures[0]);
                creneau.setHeureFin(heures[1]);
                planningService.modifierCreneau(creneau);
                System.out.println("✅ Créneau #" + creneau.getId() + " horaire modifié");
                return true;
            } else if (reservation != null) {
                reservation.setHeureDebut(heures[0]);
                reservation.setHeureFin(heures[1]);
                reservationService.modifier(reservation);
                System.out.println("✅ Réservation #" + reservation.getId() + " horaire modifié");
                return true;
            }
            
        } else if ("CHANGER_JOUR".equals(typeSolution)) {
            String nouveauJour = null;
            
            if (valeur instanceof String) {
                nouveauJour = (String) valeur;
            }
            
            if (nouveauJour == null) {
                System.err.println("❌ Nouveau jour est null");
                return false;
            }
            
            System.out.println("   Nouveau jour : " + nouveauJour);
            
            if (creneau != null) {
                creneau.setJour(nouveauJour);
                planningService.modifierCreneau(creneau);
                System.out.println("✅ Créneau #" + creneau.getId() + " jour modifié");
                return true;
            } else if (reservation != null) {
                reservation.setDateReservation(nouveauJour);
                reservationService.modifier(reservation);
                System.out.println("✅ Réservation #" + reservation.getId() + " jour modifié");
                return true;
            }
        }
        
        System.err.println("❌ Type de solution non reconnu ou échec : " + typeSolution);
        return false;
    }
    
    private void resoudreManuellement(Map<String, Object> conflit) {
        Creneau creneau = (Creneau) conflit.get("creneau");
        Reservation reservation = (Reservation) conflit.get("reservation");
        
        if (creneau != null) {
            ouvrirDialogueResoudreCreneau(creneau, conflit);
        } else if (reservation != null) {
            ouvrirDialogueResoudreReservation(reservation, conflit);
        }
    }
    
    private void ouvrirDialogueResoudreCreneau(Creneau creneau, Map<String, Object> conflit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResoudreConflitManuel.fxml"));
            Parent root = loader.load();
            
            ResoudreConflitManuelControleur controleur = loader.getController();
            controleur.setCreneau(creneau);
            controleur.setConflit(conflit);
            controleur.setConflitService(conflitService);
            controleur.setPlanningService(planningService);
            controleur.setSalleService(salleService);
            
            Stage dialog = new Stage();
            dialog.setTitle("Résoudre manuellement le conflit");
            dialog.setScene(new Scene(root));
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.initOwner(getStageFromScene());
            dialog.showAndWait();
            
            if (controleur.isResolu()) {
                conflit.put("statut", "resolu");
                chargerConflits();
            }
            
        } catch (IOException e) {
            logger.error("Erreur ouverture dialogue", e);
            afficherErreur("Impossible d'ouvrir la résolution manuelle");
        }
    }
    
    private void ouvrirDialogueResoudreReservation(Reservation reservation, Map<String, Object> conflit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResoudreConflitReservation.fxml"));
            Parent root = loader.load();
            
            ResoudreConflitReservationControleur controleur = loader.getController();
            
            // ✅ ORDRE IMPORTANT : setConflit AVANT setReservation
            controleur.setConflit(conflit);
            controleur.setReservation(reservation);
            
            controleur.setReservationService(reservationService);
            controleur.setSalleService(salleService);
            controleur.setPlanningService(planningService);
            controleur.setEmailService(emailService);
            controleur.setUtilisateurService(utilisateurService);
            
            Stage dialog = new Stage();
            dialog.setTitle("Résoudre manuellement le conflit de réservation");
            dialog.setScene(new Scene(root));
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.initOwner(getStageFromScene());
            dialog.showAndWait();
            
            if (controleur.isResolu()) {
                conflit.put("statut", "resolu");
                chargerConflits();
                afficherNotification("Succès", "Conflit résolu avec succès");
            }
            
        } catch (IOException e) {
            logger.error("Erreur ouverture dialogue", e);
            afficherErreur("Impossible d'ouvrir la résolution manuelle");
        }
    }
    
    private void reglerTousLesConflits() {
        if (tousConflits.isEmpty()) {
            afficherNotification("Info", "Aucun conflit à résoudre");
            return;
        }
        
        long nonResolus = tousConflits.stream()
            .filter(c -> !"resolu".equals(c.get("statut")))
            .count();
        
        if (nonResolus == 0) {
            afficherNotification("Info", "Aucun conflit non résolu");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Résolution automatique de tous les conflits");
        confirm.setHeaderText("Résoudre tous les conflits automatiquement ?");
        confirm.setContentText("L'algorithme tentera de résoudre les " + nonResolus + " conflit(s) non résolu(s).");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                chargementIndicator.setVisible(true);
                
                Task<Integer> task = new Task<>() {
                    @Override
                    protected Integer call() throws Exception {
                        int resolus = 0;
                        for (Map<String, Object> conflit : tousConflits) {
                            if ("resolu".equals(conflit.get("statut"))) continue;
                            
                            @SuppressWarnings("unchecked")
                            List<ConflitService.SolutionConflit> solutions = 
                                (List<ConflitService.SolutionConflit>) conflit.get("solutions");
                            
                            if (solutions != null && !solutions.isEmpty()) {
                                try {
                                    if (appliquerSolution(conflit, solutions.get(0))) {
                                        conflit.put("statut", "resolu");
                                        resolus++;
                                    }
                                } catch (SQLException e) {
                                    logger.error("Erreur résolution conflit", e);
                                }
                            }
                            Thread.sleep(50);
                        }
                        return resolus;
                    }
                    
                    @Override
                    protected void succeeded() {
                        chargementIndicator.setVisible(false);
                        afficherNotification("Résolution terminée", getValue() + " conflit(s) résolu(s)");
                        chargerConflits();
                    }
                    
                    @Override
                    protected void failed() {
                        chargementIndicator.setVisible(false);
                        afficherErreur("Erreur lors de la résolution automatique");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    private void afficherDetailsConflit(Map<String, Object> conflit) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du conflit");
        alert.setHeaderText(traduireType((String) conflit.get("type")) + " - " + conflit.get("source"));
        
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Description:\n");
        sb.append(conflit.get("description")).append("\n\n");
        sb.append("📅 Date: ").append(conflit.get("date")).append("\n");
        sb.append("📌 Statut: ").append(conflit.get("statut")).append("\n\n");
        
        @SuppressWarnings("unchecked")
        List<ConflitService.SolutionConflit> solutions = 
            (List<ConflitService.SolutionConflit>) conflit.get("solutions");
        
        if (solutions != null && !solutions.isEmpty()) {
            sb.append("💡 Solutions proposées:\n");
            for (int i = 0; i < Math.min(5, solutions.size()); i++) {
                sb.append("   ").append(i + 1).append(". ").append(solutions.get(i).getDescription()).append("\n");
            }
            if (solutions.size() > 5) {
                sb.append("   ... et ").append(solutions.size() - 5).append(" autre(s) solution(s)");
            }
        } else {
            sb.append("💡 Aucune solution automatique proposée.\n");
            sb.append("   Veuillez résoudre manuellement ce conflit.");
        }
        
        alert.setContentText(sb.toString());
        alert.getDialogPane().setMinWidth(500);
        alert.show();
    }
    
    private String traduireType(String type) {
        switch (type) {
            case "SALLE": return "🏛️ Conflit de salle";
            case "ENSEIGNANT": return "👨‍🏫 Conflit d'enseignant";
            case "CLASSE": return "👥 Conflit de classe";
            case "RESERVATION": return "📅 Conflit de réservation";
            case "SALLE_INDISPONIBLE": return "🔴 Salle indisponible";
            case "BATIMENT_INDISPONIBLE": return "🏢 Bâtiment indisponible";
            default: return type;
        }
    }
    
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (conflitsTable != null && conflitsTable.getScene() != null && conflitsTable.getScene().getWindow() instanceof Stage) {
            return (Stage) conflitsTable.getScene().getWindow();
        }
        return null;
    }
    
    public void arreter() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}