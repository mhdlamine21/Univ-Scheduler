package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.stage.Stage;

/**
 * Contrôleur pour l'écran ListeEmploisTemps.
 */
public class ListeEmploisTempsControleur extends TableauBordControleur {
    
    
    @FXML private TableView<EmploiDuTemps> edtTable;
    @FXML private TableColumn<EmploiDuTemps, Integer> edtIdColumn;
    @FXML private TableColumn<EmploiDuTemps, String> edtClasseColumn;
    @FXML private TableColumn<EmploiDuTemps, String> edtPeriodeTypeColumn;
    @FXML private TableColumn<EmploiDuTemps, String> edtPeriodeDebutColumn;
    @FXML private TableColumn<EmploiDuTemps, String> edtPeriodeFinColumn;
    @FXML private TableColumn<EmploiDuTemps, String> edtDateCreationColumn;
    @FXML private TableColumn<EmploiDuTemps, String> edtStatutColumn;
    @FXML private TableColumn<EmploiDuTemps, Void> edtActionsColumn;
    
    @FXML private ComboBox<Classe> classeFilterCombo;
    @FXML private ComboBox<String> periodeFilterCombo;
    @FXML private DatePicker dateDebutFilter;
    @FXML private DatePicker dateFinFilter;
    @FXML private ComboBox<String> statutFilterCombo;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    
    @FXML private Label totalEDTLabel;
    @FXML private Label validesLabel;
    @FXML private Label enCoursLabel;
    @FXML private ProgressIndicator chargementIndicator;
    
    private EmploiDuTempsService edtService;
    private ClasseService classeService;
    private PlanningService planningService;  
    private ExportService exportService;
    private EmailService emailService;
    
    private ObservableList<EmploiDuTemps> edtList;
    private List<EmploiDuTemps> tousEDT;
    
    private String origineRole; 
    
    public void setOrigineRole(String role) {
        this.origineRole = role;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.edtService = new EmploiDuTempsService();
        this.classeService = new ClasseService();
        this.planningService = new PlanningService();  // AJOUTÉ
        this.exportService = new ExportService();
        this.emailService = new EmailService();
        this.edtList = FXCollections.observableArrayList();
        this.tousEDT = new ArrayList<>();
        
        initialiserTableauBord();
    }
    
    @Override
    protected void initialiserTableauBord() {
        if (edtTable == null) return;
        configurerTableau();
        configurerFiltres();
        configurerEvenements();
        chargerClasses();
        chargerEmploisDuTemps();
    }
    
    @Override
    public void initialiserAvecUtilisateur(Utilisateur utilisateur, Stage stage) {
        super.initialiserAvecUtilisateur(utilisateur, stage);
        this.origineRole = utilisateur.getRole();
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerEmploisDuTemps();
    }
    
    private void configurerTableau() {
        edtIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        edtIdColumn.setPrefWidth(50);
        edtIdColumn.setStyle("-fx-alignment: CENTER;");
        
        edtClasseColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomClasse(cellData.getValue().getClasseId())
            )
        );
        edtClasseColumn.setPrefWidth(150);
        
        edtPeriodeTypeColumn.setCellValueFactory(new PropertyValueFactory<>("periodeType"));
        edtPeriodeTypeColumn.setPrefWidth(100);
        edtPeriodeTypeColumn.setStyle("-fx-alignment: CENTER;");
        
        edtPeriodeDebutColumn.setCellValueFactory(new PropertyValueFactory<>("periodeDebut"));
        edtPeriodeDebutColumn.setPrefWidth(100);
        edtPeriodeDebutColumn.setStyle("-fx-alignment: CENTER;");
        
        edtPeriodeFinColumn.setCellValueFactory(new PropertyValueFactory<>("periodeFin"));
        edtPeriodeFinColumn.setPrefWidth(100);
        edtPeriodeFinColumn.setStyle("-fx-alignment: CENTER;");
        
        edtDateCreationColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getDateCreation() != null ?
                    cellData.getValue().getDateCreation().toLocalDate().toString() : ""
            )
        );
        edtDateCreationColumn.setPrefWidth(100);
        edtDateCreationColumn.setStyle("-fx-alignment: CENTER;");
        
        edtStatutColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    EmploiDuTemps edt = cellData.getValue();
                    String statut = edt.getStatut();
                    if ("en_attente".equals(statut)) return "En attente";
                    if ("en_cours".equals(statut))   return "En cours";
                    if ("termine".equals(statut))    return "Terminé";
                    return edt.isEstValide() ? "En cours" : "En attente";
                }
            )
        );
        edtStatutColumn.setPrefWidth(80);
        edtStatutColumn.setStyle("-fx-alignment: CENTER;");
        
        edtStatutColumn.setCellFactory(column -> new TableCell<EmploiDuTemps, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                } else {
                    setText(statut);
                    switch (statut) {
                        case "En attente" -> setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        case "En cours"   -> setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        case "Terminé"    -> setStyle("-fx-text-fill: #9E9E9E; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        default -> setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });
        
        edtActionsColumn.setCellFactory(param -> new TableCell<EmploiDuTemps, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                EmploiDuTemps edt = getTableView().getItems().get(getIndex());
                boolean estAdmin = "admin".equals(origineRole);

                // ✅ Boutons avec icônes ET texte pour plus de clarté
                Button voirBtn = creerBoutonIcone("👁️", "Voir", "#2196F3");
                Button modBtn = creerBoutonIcone("✏️", "Modifier", "#FF9800");
                Button suppBtn = creerBoutonIcone("🗑️", "Supprimer", "#f44336");
                Button pdfBtn = creerBoutonIcone("📄", "PDF", "#C62828");
                Button excelBtn = creerBoutonIcone("📊", "Excel", "#2E7D32");
                Button mailBtn = creerBoutonIcone("📧", "Email", "#4CAF50");

                if (estAdmin) {
                    // Admin : seulement PDF et Excel
                    HBox box = new HBox(6, pdfBtn, excelBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                } else {
                    // Gestionnaire : toutes les actions
                    voirBtn.setOnAction(e -> voirEmploiDuTemps(edt));
                    modBtn.setOnAction(e -> modifierEmploiDuTemps(edt));
                    suppBtn.setOnAction(e -> supprimerEmploiDuTemps(edt));
                    pdfBtn.setOnAction(e -> exporterPDF(edt));
                    excelBtn.setOnAction(e -> exporterExcel(edt));
                    mailBtn.setOnAction(e -> envoyerParEmail(edt));
                    
                    HBox box = new HBox(4, voirBtn, modBtn, suppBtn, pdfBtn, excelBtn, mailBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        edtActionsColumn.setPrefWidth(260);
        
        edtTable.setItems(edtList);
        edtTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        edtTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                EmploiDuTemps edt = edtTable.getSelectionModel().getSelectedItem();
                if (edt != null) {
                    voirEmploiDuTemps(edt);
                }
            }
        });
    }
    
    private void configurerFiltres() {
        periodeFilterCombo.getItems().addAll("Tous", "Hebdomadaire", "Mensuel", "Semestriel");
        periodeFilterCombo.setValue("Tous");
        periodeFilterCombo.setPrefWidth(120);
        
        statutFilterCombo.getItems().addAll("Tous", "En attente", "En cours", "Terminé");
        statutFilterCombo.setValue("Tous");
        statutFilterCombo.setPrefWidth(100);
        
        dateDebutFilter.setPromptText("Date début");
        dateFinFilter.setPromptText("Date fin");
    }
    
    private void configurerEvenements() {
        rechercherButton.setOnAction(e -> filtrerEmploisDuTemps());
        rechercherButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 5;");
        
        reinitialiserButton.setOnAction(e -> {
            classeFilterCombo.setValue(null);
            periodeFilterCombo.setValue("Tous");
            statutFilterCombo.setValue("Tous");
            dateDebutFilter.setValue(null);
            dateFinFilter.setValue(null);
            edtList.setAll(tousEDT);
        });
        reinitialiserButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-background-radius: 5;");
    }
    
    private void chargerClasses() {
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() throws SQLException {
                return classeService.listerToutes();
            }
            
            @Override
            protected void succeeded() {
                classeFilterCombo.getItems().clear();
                classeFilterCombo.getItems().add(null);
                classeFilterCombo.getItems().addAll(getValue());
                
                classeFilterCombo.setCellFactory(lv -> new ListCell<Classe>() {
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
                
                classeFilterCombo.setButtonCell(new ListCell<Classe>() {
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
                
                chargerEmploisDuTemps();
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement classes", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerEmploisDuTemps() {
        if (edtService == null) return;
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        
        Task<List<EmploiDuTemps>> task = new Task<>() {
            @Override
            protected List<EmploiDuTemps> call() throws SQLException {
                return edtService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                tousEDT = getValue();
                edtList.setAll(tousEDT);
                mettreAJourStatistiques();
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement emplois du temps", getException());
                if (chargementIndicator != null) chargementIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void mettreAJourStatistiques() {
        long total   = tousEDT.size();
        long enCours = tousEDT.stream().filter(e -> "en_cours".equals(e.getStatut())).count();
        long attente = tousEDT.stream().filter(e -> "en_attente".equals(e.getStatut())).count();
        
        if (totalEDTLabel != null) totalEDTLabel.setText(String.valueOf(total));
        if (validesLabel != null)  validesLabel.setText(String.valueOf(enCours));
        if (enCoursLabel != null)  enCoursLabel.setText(String.valueOf(attente));
    }
    
    private String getNomClasse(int classeId) {
        if (classeFilterCombo == null) return "Inconnue";
        for (Classe c : classeFilterCombo.getItems()) {
            if (c != null && c.getId() == classeId) {
                return c.getIntitule();
            }
        }
        return "Inconnue";
    }
    
    /**
     * Filtre la liste des emplois du temps.
     */
    private void filtrerEmploisDuTemps() {
        if (tousEDT.isEmpty()) return;
        
        List<EmploiDuTemps> filtres = new ArrayList<>(tousEDT);
        
        Classe classe = classeFilterCombo.getValue();
        if (classe != null) {
            filtres.removeIf(edt -> edt.getClasseId() != classe.getId());
        }
        
        String periode = periodeFilterCombo.getValue();
        if (!"Tous".equals(periode)) {
            filtres.removeIf(edt -> !edt.getPeriodeType().equalsIgnoreCase(periode));
        }
        
        String statut = statutFilterCombo.getValue();
        if (!"Tous".equals(statut)) {
            String statutBD = switch (statut) {
                case "En attente" -> "en_attente";
                case "En cours"   -> "en_cours";
                case "Terminé"    -> "termine";
                default -> "";
            };
            if (!statutBD.isEmpty()) {
                filtres.removeIf(edt -> !statutBD.equals(edt.getStatut()));
            }
        }
        
        if (dateDebutFilter.getValue() != null) {
            LocalDate debut = dateDebutFilter.getValue();
            filtres.removeIf(edt -> 
                LocalDate.parse(edt.getPeriodeDebut()).isBefore(debut));
        }
        
        if (dateFinFilter.getValue() != null) {
            LocalDate fin = dateFinFilter.getValue();
            filtres.removeIf(edt -> 
                LocalDate.parse(edt.getPeriodeFin()).isAfter(fin));
        }
        
        edtList.setAll(filtres);
    }
    
    private void voirEmploiDuTemps(EmploiDuTemps edt) {
        try {
            Stage stage = getStageOrFromScene();
            if (stage == null) { afficherErreur("Erreur de navigation"); return; }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Planning.fxml"));
            Parent root = loader.load();
            
            PlanningControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(getUtilisateurOrFromSession(), stage);
            controleur.chargerEmploiDuTemps(edt);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur visualisation EDT", e);
            afficherErreur("Impossible de visualiser l'emploi du temps");
        }
    }
    
    /**
     * Modifie un emploi du temps existant.
     * @param edt l'emploi du temps à modifier
     */
    private void modifierEmploiDuTemps(EmploiDuTemps edt) {
        try {
            Stage stage = getStageOrFromScene();
            if (stage == null) { afficherErreur("Erreur de navigation"); return; }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NouvelEdt.fxml"));
            Parent root = loader.load();
            NouvelEdtControleur ctrl = loader.getController();
            ctrl.setUtilisateurConnectePublic(getUtilisateurOrFromSession());
            ctrl.initialiserModification(edt, stage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur modification EDT", e);
            afficherErreur("Impossible de modifier l'emploi du temps");
        }
    }
    
    /**
     * Supprime un emploi du temps après confirmation.
     * @param edt l'emploi du temps à supprimer
     */
    private void supprimerEmploiDuTemps(EmploiDuTemps edt) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'emploi du temps ?");
        alert.setContentText("Voulez-vous vraiment supprimer l'emploi du temps pour la classe " + 
            getNomClasse(edt.getClasseId()) + " ?\nCette action est irréversible.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        edtService.supprimerEmploiDuTemps(edt.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherNotification("Succès", "Emploi du temps supprimé");
                        chargerEmploisDuTemps();
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("Erreur lors de la suppression");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Exporte un emploi du temps en PDF.
     * @param edt l'emploi du temps à exporter
     */
    private void exporterPDF(EmploiDuTemps edt) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter l'emploi du temps en PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        
        String nomFichier = "EDT_" + edt.getId() + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            
            afficherNotification("Export PDF", "Export en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Récupérer les créneaux de cet EDT
                    List<Creneau> creneaux = planningService.getCreneauxParEmploiDuTemps(edt.getId());
                    Classe classe = classeService.trouverParId(edt.getClasseId());
                    
                    exportService.exporterEDTGrillePDF(
                    	    creneaux, 
                    	    classe, 
                    	    edt.getPeriodeType() + " du " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin(),
                    	    getUtilisateurOrFromSession(), 
                    	    chemin,
                    	    edt
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
    
    /**
     * Exporte un emploi du temps en Excel.
     * @param edt l'emploi du temps à exporter
     */
    private void exporterExcel(EmploiDuTemps edt) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter l'emploi du temps en Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );
        
        String nomFichier = "EDT_" + edt.getId() + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            
            afficherNotification("Export Excel", "Export en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<Creneau> creneaux = planningService.getCreneauxParEmploiDuTemps(edt.getId());
                    Classe classe = classeService.trouverParId(edt.getClasseId());
                    
                    exportService.exporterEDTGrilleExcel(
                    	    creneaux, 
                    	    classe, 
                    	    edt.getPeriodeType() + " du " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin(),
                    	    getUtilisateurOrFromSession(), 
                    	    chemin,
                    	    edt
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
    
    /**
     * Envoie les notifications d'un emploi du temps par email.
     * @param edt l'emploi du temps concerné
     */
    private void envoyerParEmail(EmploiDuTemps edt) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Envoi de notification - EDT #" + edt.getId());
        alert.setHeaderText("📧 Envoyer une notification d'emploi du temps");
        alert.setContentText("Choisissez les destinataires :\n\n" +
            "Une notification sera envoyée pour les informer qu'un emploi du temps est disponible.");

        ButtonType enseignantsBtn = new ButtonType("👨‍🏫 Enseignants");
        ButtonType etudiantsBtn   = new ButtonType("👨‍🎓 Étudiants");
        ButtonType tousBtn        = new ButtonType("📢 Tous");
        ButtonType annulerBtn     = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(enseignantsBtn, etudiantsBtn, tousBtn, annulerBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == annulerBtn) return;

            boolean envEtu  = (response == etudiantsBtn || response == tousBtn);
            boolean envProf = (response == enseignantsBtn || response == tousBtn);

            if (chargementIndicator != null) chargementIndicator.setVisible(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String titre = edt.getPeriodeType() + " du " + edt.getPeriodeDebut()
                            + " au " + edt.getPeriodeFin();
                    try {
                        // Récupérer le titre depuis la BD
                        scheduler.dao.EmploiDuTempsDAO dao = new scheduler.dao.EmploiDuTempsDAO();
                        String t = dao.getTitre(edt.getId());
                        if (t != null && !t.isEmpty()) titre = t;
                    } catch (Exception ignore) {}

                    EmploiDuTempsService svc = new EmploiDuTempsService();
                    svc.validerEmploiDuTemps(edt.getId(), null, titre, envEtu, envProf);
                    return null;
                }

                @Override
                protected void succeeded() {
                    if (chargementIndicator != null) chargementIndicator.setVisible(false);
                    String dest = "";
                    if (envEtu && envProf) dest = "étudiants et enseignants";
                    else if (envEtu)       dest = "étudiants";
                    else                   dest = "enseignants";
                    afficherNotification("✅ Notifications envoyées",
                        "Les notifications ont été envoyées aux " + dest + ".");
                }

                @Override
                protected void failed() {
                    if (chargementIndicator != null) chargementIndicator.setVisible(false);
                    afficherErreur("Erreur lors de l'envoi : " + getException().getMessage());
                    logger.error("Erreur envoi emails EDT", getException());
                }
            };
            new Thread(task).start();
        });
    }
    
    private Stage getStageOrFromScene() {
        if (primaryStage != null) return primaryStage;
        if (edtTable != null && edtTable.getScene() != null && edtTable.getScene().getWindow() instanceof Stage) {
            return (Stage) edtTable.getScene().getWindow();
        }
        return null;
    }
    
    private Utilisateur getUtilisateurOrFromSession() {
        if (utilisateurConnecte != null) return utilisateurConnecte;
        if (session != null) return session.getUtilisateurConnecte();
        return null;
    }
    
    private Button creerBouton(String texte, String couleur, String tooltip) {
        Button btn = new Button(texte);
        btn.setStyle("-fx-background-color: " + couleur + "; -fx-text-fill: white; " +
                     "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 10; " +
                     "-fx-background-radius: 5; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }
    
    private Button creerBoutonIcone(String icone, String texte, String couleur) {
        Button btn = new Button(icone);
        btn.setStyle("-fx-background-color: " + couleur + "; -fx-text-fill: white; " +
                     "-fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 32; -fx-min-height: 32; " +
                     "-fx-background-radius: 5; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(texte));
        return btn;
    }

    /**
     * Crée un bouton avec icône (texte) et style.
     */
    private Button creerIconBtn(String icone, String tooltip) {
        Button btn = new Button(icone);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; " +
                     "-fx-font-size: 13px; -fx-padding: 5 8; -fx-text-fill: #1976D2;");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    
    @FXML
    private void retourEnArriere() {
        try {
            Stage stage = getStageOrFromScene();
            if (stage == null) { 
                afficherErreur("Erreur de navigation"); 
                return; 
            }
            
            String fxmlRetour;
            if ("admin".equals(origineRole)) {
                fxmlRetour = "/fxml/TableauBordAdmin.fxml";
            } else {
                fxmlRetour = "/fxml/TableauBordGestionnaire.fxml";
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlRetour));
            Parent root = loader.load();
            
            TableauBordControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(getUtilisateurOrFromSession(), stage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour en arrière", e);
            afficherErreur("Impossible de retourner");
        }
    }
    
    @FXML
    private void handleNouvelEDT() {
        if (!"gestionnaire".equals(origineRole)) {
            afficherErreur("Seul le gestionnaire peut créer un nouvel emploi du temps");
            return;
        }
        
        Stage stage = getStageOrFromScene();
        if (stage == null) {
            afficherErreur("Erreur de navigation");
            return;
        }
        if (getUtilisateurOrFromSession() == null) {
            afficherErreur("Session expirée. Veuillez vous reconnecter.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NouvelEdt.fxml"));
            Parent root = loader.load();
            NouvelEdtControleur ctrl = loader.getController();
            ctrl.initialiserAvecUtilisateur(getUtilisateurOrFromSession(), stage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            logger.error("Erreur création nouvel EDT", e);
            afficherErreur("Impossible de créer un nouvel emploi du temps");
        }
    }
}