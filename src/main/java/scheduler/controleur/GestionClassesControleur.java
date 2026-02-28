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
import java.sql.SQLException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Contrôleur pour l'écran GestionClasses.
 */
public class GestionClassesControleur extends TableauBordControleur {
    
    @FXML private TableView<Classe> classeTable;
    @FXML private TableColumn<Classe, Integer> idColumn;
    @FXML private TableColumn<Classe, String> intituleColumn;
    @FXML private TableColumn<Classe, String> filiereColumn;
    @FXML private TableColumn<Classe, String> niveauColumn;
    @FXML private TableColumn<Classe, String> anneeColumn;
    @FXML private TableColumn<Classe, String> ufrColumn;
    @FXML private TableColumn<Classe, Integer> effectifColumn;
    @FXML private TableColumn<Classe, Integer> groupesColumn;
    @FXML private TableColumn<Classe, String> statutColumn;
    @FXML private TableColumn<Classe, Void> actionsColumn;
    
    // Filtres
    @FXML private TextField rechercheField;
    @FXML private ComboBox<String> filtreNiveauCombo;
    @FXML private ComboBox<String> filtreAnneeCombo;
    @FXML private ComboBox<Ufr> filtreUfrCombo;
    @FXML private CheckBox filtreActifCheck;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    
    @FXML private ComboBox<Ufr> ufrCombo;
    @FXML private TextField intituleField;
    @FXML private TextField filiereField;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private TextField anneeField;
    @FXML private Spinner<Integer> effectifSpinner;
    @FXML private Spinner<Integer> groupesSpinner;
    @FXML private CheckBox activeCheck;
    @FXML private Label messageLabel;
    @FXML private Label totalClassesLabel; // AJOUTÉ
    @FXML private Button retourButton;
    
    @FXML private Button ajouterButton;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button archiverButton;
    @FXML private Button annulerButton;
    @FXML private Button rafraichirButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private ClasseService classeService;
    private UfrService ufrService;
    private Classe classeSelectionne;
    private ObservableList<Classe> classeList;
    private List<Classe> toutesClasses;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.classeService = new ClasseService();
        this.ufrService = new UfrService();
        this.classeList = FXCollections.observableArrayList();
        this.toutesClasses = new ArrayList<>();
        
        configurerTableau();
        configurerEvenements();
        configurerSpinners();
        configurerNiveaux();
        configurerFiltres();
        chargerUfr();
        chargerClasses();
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetourGestion());
        }
    }
    
    @Override
    protected void initialiserTableauBord() {
        // Déjà fait dans initialize()
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerClasses();
    }
    
    private void configurerTableau() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        intituleColumn.setCellValueFactory(new PropertyValueFactory<>("intitule"));
        filiereColumn.setCellValueFactory(new PropertyValueFactory<>("filiere"));
        niveauColumn.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        anneeColumn.setCellValueFactory(new PropertyValueFactory<>("anneeScolaire"));
        effectifColumn.setCellValueFactory(new PropertyValueFactory<>("effectif"));
        groupesColumn.setCellValueFactory(new PropertyValueFactory<>("nbGroupes"));
        
        // Alignement
        idColumn.setStyle("-fx-alignment: CENTER;");
        intituleColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        filiereColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        niveauColumn.setStyle("-fx-alignment: CENTER;");
        anneeColumn.setStyle("-fx-alignment: CENTER;");
        effectifColumn.setStyle("-fx-alignment: CENTER;");
        groupesColumn.setStyle("-fx-alignment: CENTER;");
        
        ufrColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomUfr(cellData.getValue().getUfrId())
            )
        );
        ufrColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        
        statutColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().isEstActive() ? "Active" : "Archivée"
            )
        );
        statutColumn.setStyle("-fx-alignment: CENTER;");
        
        statutColumn.setCellFactory(column -> new TableCell<Classe, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                } else {
                    setText(statut);
                    if (statut.equals("Archivée")) {
                        setStyle("-fx-text-fill: #9E9E9E; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });
        
        actionsColumn.setCellFactory(param -> new TableCell<Classe, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            
            {
                editBtn.setOnAction(event -> {
                    Classe classe = getTableView().getItems().get(getIndex());
                    chargerClassePourEdition(classe);
                });
                
                deleteBtn.setOnAction(event -> {
                    Classe classe = getTableView().getItems().get(getIndex());
                    supprimerClasse(classe);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Classe c = getTableView().getItems().get(getIndex());
                    Button archiveBtn = new Button(c.isEstActive() ? "📦 Archiver" : "🔄 Réactiver");
                    archiveBtn.setOnAction(e -> {
                        if (c.isEstActive()) {
                            archiverClasse(c);
                        } else {
                            reactiverClasse(c);
                        }
                    });
                    
                    HBox box = new HBox(5, editBtn, deleteBtn, archiveBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        
        classeTable.setItems(classeList);
        
        classeTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    classeSelectionne = newVal;
                    chargerClassePourEdition(newVal);
                }
            }
        );
        
        classeTable.setPrefHeight(400);
        classeTable.setMinHeight(350);
    }
    
    private void configurerFiltres() {
        filtreNiveauCombo.getItems().addAll("Tous", "Licence 1", "Licence 2", "Licence 3", "Master 1", "Master 2", "Doctorat");
        filtreNiveauCombo.setValue("Tous");
        
        filtreAnneeCombo.getItems().addAll("Toutes", "2023-2024", "2024-2025", "2025-2026");
        filtreAnneeCombo.setValue("Toutes");
        
        filtreActifCheck.setSelected(true);
        
        rechercherButton.setOnAction(e -> filtrerClasses());
        reinitialiserButton.setOnAction(e -> reinitialiserFiltres());
    }
    
    /**
     * Filtre les classes selon les critères.
     */
    private void filtrerClasses() {
        List<Classe> filtrees = new ArrayList<>(toutesClasses);
        
        String recherche = rechercheField.getText().toLowerCase().trim();
        if (!recherche.isEmpty()) {
            filtrees.removeIf(c -> 
                !c.getIntitule().toLowerCase().contains(recherche) &&
                !c.getFiliere().toLowerCase().contains(recherche)
            );
        }
        
        String niveau = filtreNiveauCombo.getValue();
        if (!"Tous".equals(niveau)) {
            filtrees.removeIf(c -> !c.getNiveau().equals(niveau));
        }
        
        String annee = filtreAnneeCombo.getValue();
        if (!"Toutes".equals(annee)) {
            filtrees.removeIf(c -> !c.getAnneeScolaire().equals(annee));
        }
        
        // ✅ CORRECTION ICI : Vérifier correctement l'UFR
        Ufr ufr = filtreUfrCombo.getValue();
        if (ufr != null) {
            filtrees.removeIf(c -> c.getUfrId() != ufr.getId());
        }
        
        if (filtreActifCheck.isSelected()) {
            filtrees.removeIf(c -> !c.isEstActive());
        }
        
        classeList.setAll(filtrees);
    }
    
    private void reinitialiserFiltres() {
        rechercheField.clear();
        filtreNiveauCombo.setValue("Tous");
        filtreAnneeCombo.setValue("Toutes");
        filtreUfrCombo.setValue(null);
        filtreActifCheck.setSelected(true);
        classeList.setAll(toutesClasses);
    }
    
    private void configurerEvenements() {
        ajouterButton.setOnAction(e -> ajouterClasse());
        modifierButton.setOnAction(e -> modifierClasse());
        supprimerButton.setOnAction(e -> {
            if (classeSelectionne != null) supprimerClasse(classeSelectionne);
        });
        annulerButton.setOnAction(e -> viderFormulaire());
        rafraichirButton.setOnAction(e -> chargerClasses());
    }
    
    private void configurerSpinners() {
        effectifSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 30));
        groupesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
    }
    
    private void configurerNiveaux() {
        niveauCombo.getItems().addAll(
            "Licence 1", "Licence 2", "Licence 3",
            "Master 1", "Master 2", "Doctorat"
        );
    }
    
    private void chargerUfr() {
        Task<List<Ufr>> task = new Task<>() {
            @Override
            protected List<Ufr> call() throws SQLException {
                return ufrService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                List<Ufr> ufrs = getValue();
                
                // ✅ CONFIGURATION POUR ufrCombo (dans le formulaire)
                ufrCombo.setCellFactory(lv -> new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom());
                    }
                });
                ufrCombo.setButtonCell(new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Sélectionner une UFR" : item.getNom());
                    }
                });
                ufrCombo.getItems().setAll(ufrs);
                
                // ✅ CONFIGURATION POUR filtreUfrCombo (dans les filtres)
                filtreUfrCombo.setCellFactory(lv -> new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Toutes les UFR");
                        } else {
                            setText(item.getNom());
                        }
                    }
                });
                
                filtreUfrCombo.setButtonCell(new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Toutes les UFR");
                        } else {
                            setText(item.getNom());
                        }
                    }
                });
                
                // ✅ AJOUTER un élément null au début pour "Toutes les UFR"
                filtreUfrCombo.getItems().clear();
                filtreUfrCombo.getItems().add(null);  // Option "Toutes les UFR"
                filtreUfrCombo.getItems().addAll(ufrs);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement UFR", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerClasses() {
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() throws SQLException {
                return classeService.listerToutes();
            }
            
            @Override
            protected void succeeded() {
                toutesClasses = getValue();
                classeList.setAll(toutesClasses);
                totalClassesLabel.setText(toutesClasses.size() + " classe(s)");
                chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement classes", getException());
                messageLabel.setText("Erreur de chargement");
                chargementIndicator.setVisible(false);
            }
        };
        
        chargementIndicator.setVisible(true);
        new Thread(task).start();
    }
    
    private String getNomUfr(int ufrId) {
        for (Ufr ufr : ufrCombo.getItems()) {
            if (ufr.getId() == ufrId) {
                return ufr.getNom();
            }
        }
        return "Inconnue";
    }
    
    private void chargerClassePourEdition(Classe classe) {
        intituleField.setText(classe.getIntitule());
        filiereField.setText(classe.getFiliere());
        niveauCombo.setValue(classe.getNiveau());
        anneeField.setText(classe.getAnneeScolaire());
        effectifSpinner.getValueFactory().setValue(classe.getEffectif());
        groupesSpinner.getValueFactory().setValue(classe.getNbGroupes());
        activeCheck.setSelected(classe.isEstActive());
        
        for (Ufr ufr : ufrCombo.getItems()) {
            if (ufr.getId() == classe.getUfrId()) {
                ufrCombo.setValue(ufr);
                break;
            }
        }
        
        classeSelectionne = classe;
        
        modifierButton.setDisable(false);
        supprimerButton.setDisable(false);
        ajouterButton.setDisable(true);
    }
    
    private void viderFormulaire() {
        intituleField.clear();
        filiereField.clear();
        niveauCombo.setValue(null);
        anneeField.setText(LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() + 1));
        effectifSpinner.getValueFactory().setValue(30);
        groupesSpinner.getValueFactory().setValue(1);
        activeCheck.setSelected(true);
        ufrCombo.setValue(null);
        
        classeSelectionne = null;
        
        modifierButton.setDisable(true);
        supprimerButton.setDisable(true);
        ajouterButton.setDisable(false);
        
        classeTable.getSelectionModel().clearSelection();
    }
    
    /**
     * Ajoute une nouvelle classe.
     */
    private void ajouterClasse() {
        if (!validerFormulaire()) return;
        
        Classe classe = new Classe(
            intituleField.getText().trim(),
            filiereField.getText().trim(),
            niveauCombo.getValue(),
            anneeField.getText().trim(),
            ufrCombo.getValue().getId(),
            effectifSpinner.getValue(),
            groupesSpinner.getValue()
        );
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                classeService.ajouter(classe);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Classe ajoutée");
                viderFormulaire();
                chargerClasses();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur ajout");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Modifie la classe sélectionnée.
     */
    private void modifierClasse() {
        if (classeSelectionne == null) return;
        if (!validerFormulaire()) return;
        
        classeSelectionne.setIntitule(intituleField.getText().trim());
        classeSelectionne.setFiliere(filiereField.getText().trim());
        classeSelectionne.setNiveau(niveauCombo.getValue());
        classeSelectionne.setAnneeScolaire(anneeField.getText().trim());
        classeSelectionne.setUfrId(ufrCombo.getValue().getId());
        classeSelectionne.setEffectif(effectifSpinner.getValue());
        classeSelectionne.setNbGroupes(groupesSpinner.getValue());
        classeSelectionne.setEstActive(activeCheck.isSelected());
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                classeService.modifier(classeSelectionne);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Classe modifiée");
                viderFormulaire();
                chargerClasses();
            }
            
            @Override
            protected void failed() {
                messageLabel.setText("❌ Erreur modification");
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Supprime une classe après confirmation.
     * @param classe la classe à supprimer
     */
    private void supprimerClasse(Classe classe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la classe ?");
        alert.setContentText("Supprimer " + classe.getIntitule() + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        classeService.supprimer(classe.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        messageLabel.setText("✅ Classe supprimée");
                        viderFormulaire();
                        chargerClasses();
                    }
                    
                    @Override
                    protected void failed() {
                        messageLabel.setText("❌ Erreur suppression");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Archive une classe (la rend inactive).
     * @param classe la classe à archiver
     */
    private void archiverClasse(Classe classe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Archivage");
        alert.setHeaderText("Archiver la classe ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        classeService.archiver(classe.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        messageLabel.setText("✅ Classe archivée");
                        viderFormulaire();
                        chargerClasses();
                    }
                    
                    @Override
                    protected void failed() {
                        messageLabel.setText("❌ Erreur archivage");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Réactive une classe archivée.
     * @param classe la classe à réactiver
     */
    private void reactiverClasse(Classe classe) {
        classe.setEstActive(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                classeService.modifier(classe);
                return null;
            }
            
            @Override
            protected void succeeded() {
                messageLabel.setText("✅ Classe réactivée");
                viderFormulaire();
                chargerClasses();
            }
        };
        
        new Thread(task).start();
    }
    
    private boolean validerFormulaire() {
        if (intituleField.getText().trim().isEmpty()) {
            messageLabel.setText("❌ Intitulé requis");
            return false;
        }
        if (filiereField.getText().trim().isEmpty()) {
            messageLabel.setText("❌ Filière requise");
            return false;
        }
        if (niveauCombo.getValue() == null) {
            messageLabel.setText("❌ Niveau requis");
            return false;
        }
        if (anneeField.getText().trim().isEmpty()) {
            messageLabel.setText("❌ Année requise");
            return false;
        }
        if (ufrCombo.getValue() == null) {
            messageLabel.setText("❌ UFR requise");
            return false;
        }
        return true;
    }
    
    private void handleRetourGestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordAdmin.fxml"));
            Parent root = loader.load();
            
            TableauBordAdminControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            controleur.selectionnerOnglet(1); // Onglet Gestion
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour gestion", e);
            afficherErreur("Impossible de retourner au menu Gestion");
        }
    }
}