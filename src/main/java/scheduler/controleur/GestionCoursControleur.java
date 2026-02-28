package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.collections.ListChangeListener;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Gestion des cours (création, modification, suppression).
 
 */
public class GestionCoursControleur extends TableauBordControleur {
    
    static {
    }
    
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, Integer> idColumn;
    @FXML private TableColumn<Cours, String> matiereColumn;
    @FXML private TableColumn<Cours, String> enseignantColumn;
    @FXML private TableColumn<Cours, String> classeColumn;
    @FXML private TableColumn<Cours, String> typeColumn;
    @FXML private TableColumn<Cours, String> groupesColumn;
    @FXML private TableColumn<Cours, Integer> volumeColumn;
    @FXML private TableColumn<Cours, Void> actionsColumn;
    
    @FXML private ComboBox<Matiere> matiereCombo;
    @FXML private ComboBox<Enseignant> enseignantCombo;
    @FXML private ComboBox<Classe> classeCombo;
    @FXML private ComboBox<String> typeCoursCombo;
    @FXML private ListView<String> groupesList;
    @FXML private TextField volumeField;
    @FXML private Label infoGroupesLabel;
    @FXML private Label messageLabel;
    @FXML private VBox groupesPane;
    
    @FXML private TextField rechercheCoursField;
    @FXML private ComboBox<String> filtreTypeCoursCombo;
    @FXML private ComboBox<Classe> filtreClasseCombo;
    @FXML private ComboBox<String> filiereMatiereCombo; // AJOUTÉ
    @FXML private Button rechercherCoursButton;
    @FXML private Button reinitialiserCoursButton;
    @FXML private Label totalCoursLabel;
    
    @FXML private RadioButton tousGroupesRadio;
    @FXML private RadioButton groupesSpecifiquesRadio;
    @FXML private ToggleGroup groupesToggle;
    
    @FXML private Button ajouterButton;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button annulerButton;
    @FXML private Button rafraichirButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private MatiereService matiereService;
    private UtilisateurService utilisateurService;
    private ClasseService classeService;
    private CoursService coursService;
    private Cours coursSelectionne;
    private ObservableList<Cours> coursList;
    private List<Cours> tousLesCours; // Pour conserver tous les cours
    private List<String> groupesDisponibles;
    
    @Override
    public void initialize() {
        
        super.initialize();
        
        this.matiereService = new MatiereService();
        this.utilisateurService = new UtilisateurService();
        this.classeService = new ClasseService();
        this.coursService = new CoursService();
        this.coursList = FXCollections.observableArrayList();
        this.tousLesCours = new ArrayList<>();
        this.groupesDisponibles = new ArrayList<>();
        
        try {
            
            List<Matiere> matieres = matiereService.listerToutes();
            
            List<Utilisateur> enseignants = utilisateurService.listerParRole("enseignant");
            
            List<Classe> classes = classeService.listerToutes();
            
        } catch (SQLException e) {
        	
        }
        
        configurerTableau();
        configurerEvenements();
        configurerTypeCours();
        configurerFiltres();
        configurerFiltreFiliere(); 
        
        chargerMatieres();
        chargerEnseignants();
        chargerClasses();
        chargerCours();
        
        groupesPane.setVisible(false);
        groupesPane.setManaged(false);
    }
    
    @Override
    protected void initialiserTableauBord() {
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerCours();
    }
    
    
    /**
     * Configure le filtre par filière pour les matières et les classes
     */
    private void configurerFiltreFiliere() {
        if (filiereMatiereCombo == null) return;
        
        filiereMatiereCombo.setPromptText("Sélectionner une filière");
        filiereMatiereCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                chargerMatieresParFiliere(newVal);
                filtrerClassesParFiliere(newVal);
            } else {
                chargerMatieres();
                chargerClasses();
            }
        });
        
        chargerFilieresPourCombo();
    }
    
    /**
     * Charge toutes les filières depuis les classes existantes
     */
    private void chargerFilieresPourCombo() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws SQLException {
                return classeService.listerFilieres();
            }
            
            @Override
            protected void succeeded() {
                List<String> filieres = getValue();
                filiereMatiereCombo.getItems().clear();
                filiereMatiereCombo.getItems().add(null);
                if (filieres != null) {
                    filiereMatiereCombo.getItems().addAll(filieres);
                }
                filiereMatiereCombo.setValue(null);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement filières", getException());
            }
        };
        new Thread(task).start();
    }
    
    /**
     * Charge les matières d'une filière spécifique
     */
    private void chargerMatieresParFiliere(String filiere) {
        Task<List<Matiere>> task = new Task<>() {
            @Override
            protected List<Matiere> call() throws SQLException {
                return matiereService.listerParFiliere(filiere);
            }
            
            @Override
            protected void succeeded() {
                List<Matiere> matieres = getValue();
                matiereCombo.getItems().setAll(matieres);
                
                matiereCombo.setCellFactory(lv -> new ListCell<Matiere>() {
                    @Override
                    protected void updateItem(Matiere item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom() + " (" + item.getCode() + ")");
                    }
                });
                
                matiereCombo.setButtonCell(new ListCell<Matiere>() {
                    @Override
                    protected void updateItem(Matiere item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom() + " (" + item.getCode() + ")");
                    }
                });
                
                if (matieres.isEmpty()) {
                    afficherMessageErreur("Aucune matière trouvée pour la filière " + filiere);
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement matières par filière", getException());
                afficherMessageErreur("Erreur de chargement des matières");
            }
        };
        new Thread(task).start();
    }
    
    /**
     * Filtre les classes en fonction de la filière sélectionnée
     */
    private void filtrerClassesParFiliere(String filiere) {
        if (filiere == null || filiere.isEmpty()) {
            chargerClasses();
            return;
        }
        
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() throws SQLException {
                return classeService.listerParFiliere(filiere);
            }
            
            @Override
            protected void succeeded() {
                List<Classe> classes = getValue();
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
                
                filtreClasseCombo.getItems().clear();
                filtreClasseCombo.getItems().add(null);
                if (classes != null) {
                    filtreClasseCombo.getItems().addAll(classes);
                }
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement classes par filière", getException());
            }
        };
        new Thread(task).start();
    }
    
    
    private void configurerFiltres() {
        
        rechercheCoursField.setPromptText("Rechercher par matière, enseignant ou classe...");
        
        filtreTypeCoursCombo.getItems().addAll("Tous", "CM", "TD", "TP");
        filtreTypeCoursCombo.setValue("Tous");
        
        rechercherCoursButton.setOnAction(e -> filtrerCours());
        reinitialiserCoursButton.setOnAction(e -> {
            rechercheCoursField.clear();
            filtreTypeCoursCombo.setValue("Tous");
            filtreClasseCombo.setValue(null);
            coursList.setAll(tousLesCours);
            mettreAJourTotal();
        });
        
        coursList.addListener((ListChangeListener<Cours>) c -> mettreAJourTotal());
    }
    
    /**
     * Filtre la liste des cours selon les critères de recherche.
     */
    private void filtrerCours() {
        String recherche = rechercheCoursField.getText().toLowerCase().trim();
        String typeFiltre = filtreTypeCoursCombo.getValue();
        Classe classeFiltre = filtreClasseCombo.getValue();
        
        List<Cours> resultats = new ArrayList<>();
        
        for (Cours c : tousLesCours) {
            boolean correspond = true;
            
            if (!recherche.isEmpty()) {
                String matiere = getNomMatiere(c.getMatiereId()).toLowerCase();
                String enseignant = getNomEnseignant(c.getEnseignantId()).toLowerCase();
                String classe = getIntituleClasse(c.getClasseId()).toLowerCase();
                
                if (!matiere.contains(recherche) && 
                    !enseignant.contains(recherche) && 
                    !classe.contains(recherche)) {
                    correspond = false;
                }
            }
            
            if (correspond && typeFiltre != null && !"Tous".equals(typeFiltre)) {
                if (!typeFiltre.equals(c.getTypeCours())) {
                    correspond = false;
                }
            }
            
            if (correspond && classeFiltre != null) {
                if (c.getClasseId() != classeFiltre.getId()) {
                    correspond = false;
                }
            }
            
            if (correspond) {
                resultats.add(c);
            }
        }
        
        coursList.setAll(resultats);
    }
    
    private void mettreAJourTotal() {
        if (totalCoursLabel != null) {
            totalCoursLabel.setText(coursList.size() + " cours");
        }
    }
    
    private void configurerTableau() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("typeCours"));
        groupesColumn.setCellValueFactory(new PropertyValueFactory<>("groupes"));
        volumeColumn.setCellValueFactory(new PropertyValueFactory<>("volumeHoraire"));
        
        matiereColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomMatiere(cellData.getValue().getMatiereId())
            )
        );
        
        enseignantColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getNomEnseignant(cellData.getValue().getEnseignantId())
            )
        );
        
        classeColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> getIntituleClasse(cellData.getValue().getClasseId())
            )
        );
        
        actionsColumn.setCellFactory(param -> new TableCell<Cours, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            
            {
                editBtn.setOnAction(event -> {
                    Cours cours = getTableView().getItems().get(getIndex());
                    chargerCoursPourEdition(cours);
                });
                
                deleteBtn.setOnAction(event -> {
                    Cours cours = getTableView().getItems().get(getIndex());
                    supprimerCours(cours);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, editBtn, deleteBtn));
                }
            }
        });
        
        coursTable.setItems(coursList);
        
        coursTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    coursSelectionne = newVal;
                    chargerCoursPourEdition(newVal);
                }
            }
        );
    }
    
    private void configurerEvenements() {
        
        ajouterButton.setOnAction(e -> ajouterCours());
        modifierButton.setOnAction(e -> modifierCours());
        supprimerButton.setOnAction(e -> {
            if (coursSelectionne != null) supprimerCours(coursSelectionne);
        });
        annulerButton.setOnAction(e -> viderFormulaire());
        rafraichirButton.setOnAction(e -> chargerCours());
        
        groupesToggle = new ToggleGroup();
        tousGroupesRadio.setToggleGroup(groupesToggle);
        groupesSpecifiquesRadio.setToggleGroup(groupesToggle);
        tousGroupesRadio.setSelected(true);
        groupesList.setDisable(true);
        
        groupesToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            groupesList.setDisable(newVal == tousGroupesRadio);
        });
        
        classeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                chargerGroupesClasse(newVal);
            }
        });
        
        typeCoursCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isTDouTP = "TD".equals(newVal) || "TP".equals(newVal);
            groupesPane.setVisible(isTDouTP);
            groupesPane.setManaged(isTDouTP);
            // Charger les groupes si une classe est déjà sélectionnée
            if (isTDouTP && classeCombo.getValue() != null
                    && groupesList.getItems().isEmpty()) {
                chargerGroupesClasse(classeCombo.getValue());
            }
        });
    }
    
    private void configurerTypeCours() {
        typeCoursCombo.getItems().addAll("CM", "TD", "TP");
    }
    
    private void chargerMatieres() {
        
        Task<List<Matiere>> task = new Task<>() {
            @Override
            protected List<Matiere> call() throws SQLException {
                List<Matiere> matieres = matiereService.listerToutes();
                return matieres;
            }
            
            @Override
            protected void succeeded() {
                List<Matiere> matieres = getValue();
                
                matiereCombo.getItems().setAll(matieres);
                
                matiereCombo.setCellFactory(lv -> new ListCell<Matiere>() {
                    @Override
                    protected void updateItem(Matiere item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom() + " (" + item.getCode() + ")");
                    }
                });
                
                matiereCombo.setButtonCell(new ListCell<Matiere>() {
                    @Override
                    protected void updateItem(Matiere item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom() + " (" + item.getCode() + ")");
                    }
                });
                
                if (matieres.isEmpty()) {
                } else {
                }
            }
            
            @Override
            protected void failed() {
                getException().printStackTrace();
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerEnseignants() {
        
        Task<List<Utilisateur>> task = new Task<>() {
            @Override
            protected List<Utilisateur> call() throws SQLException {
                List<Utilisateur> utilisateurs = utilisateurService.listerParRole("enseignant");
                return utilisateurs;
            }
            
            @Override
            protected void succeeded() {
                List<Utilisateur> utilisateurs = getValue();
                List<Enseignant> enseignants = new ArrayList<>();
                
                for (Utilisateur u : utilisateurs) {
                    if (u instanceof Enseignant) {
                        enseignants.add((Enseignant) u);
                    }
                }
                
                enseignantCombo.getItems().setAll(enseignants);
                
                enseignantCombo.setCellFactory(lv -> new ListCell<Enseignant>() {
                    @Override
                    protected void updateItem(Enseignant item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getPrenom() + " " + item.getNom());
                    }
                });
                
                enseignantCombo.setButtonCell(new ListCell<Enseignant>() {
                    @Override
                    protected void updateItem(Enseignant item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getPrenom() + " " + item.getNom());
                    }
                });
                
                if (enseignants.isEmpty()) {
                } else {
                }
            }
            
            @Override
            protected void failed() {
                getException().printStackTrace();
            }
        };
        
        new Thread(task).start();
    }
    
    private void chargerClasses() {
        
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() throws SQLException {
                List<Classe> classes = classeService.listerToutes();
                return classes;
            }
            
            @Override
            protected void succeeded() {
                List<Classe> classes = getValue();
                
                classeCombo.getItems().setAll(classes);
                
                filtreClasseCombo.getItems().clear();
                filtreClasseCombo.getItems().add(null);
                filtreClasseCombo.getItems().addAll(classes);
                
                classeCombo.setCellFactory(lv -> new ListCell<Classe>() {
                    @Override
                    protected void updateItem(Classe item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                    }
                });
                
                classeCombo.setButtonCell(new ListCell<Classe>() {
                    @Override
                    protected void updateItem(Classe item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                    }
                });
                
                filtreClasseCombo.setCellFactory(lv -> new ListCell<Classe>() {
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
                
                filtreClasseCombo.setButtonCell(new ListCell<Classe>() {
                    @Override
                    protected void updateItem(Classe item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Toutes les classes" : 
                               item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                    }
                });
                
                if (classes.isEmpty()) {
                } else {
                }
            }
            
            @Override
            protected void failed() {
                getException().printStackTrace();
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Recharge la liste des cours depuis la base de données.
     */
    private void chargerCours() {
        chargementIndicator.setVisible(true);
        
        Task<List<Cours>> task = new Task<>() {
            @Override
            protected List<Cours> call() throws SQLException {
                List<Cours> cours = coursService.listerTous();
                return cours;
            }
            
            @Override
            protected void succeeded() {
                tousLesCours = getValue();
                coursList.setAll(tousLesCours);
                mettreAJourTotal();
                chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                chargementIndicator.setVisible(false);
                afficherMessageErreur("❌ Erreur de chargement des cours: " + getException().getMessage());
            }
        };
        
        chargementIndicator.setVisible(true);
        new Thread(task).start();
    }
    
    private void chargerGroupesClasse(Classe classe) {
        groupesDisponibles.clear();
        for (int i = 1; i <= classe.getNbGroupes(); i++) {
            groupesDisponibles.add("Groupe " + i);
        }
        
        groupesList.getItems().setAll(groupesDisponibles);
        groupesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        infoGroupesLabel.setText(classe.getNbGroupes() + " groupes disponibles");
    }
    
    private String getNomMatiere(int matiereId) {
        for (Matiere m : matiereCombo.getItems()) {
            if (m.getId() == matiereId) {
                return m.getNom();
            }
        }
        return "Inconnue";
    }
    
    private String getNomEnseignant(int enseignantId) {
        for (Enseignant e : enseignantCombo.getItems()) {
            if (e.getId() == enseignantId) {
                return e.getPrenom() + " " + e.getNom();
            }
        }
        return "Inconnu";
    }
    
    private String getIntituleClasse(int classeId) {
        for (Classe c : classeCombo.getItems()) {
            if (c.getId() == classeId) {
                return c.getIntitule();
            }
        }
        return "Inconnue";
    }
    
    private void chargerCoursPourEdition(Cours cours) {
        for (Matiere m : matiereCombo.getItems()) {
            if (m.getId() == cours.getMatiereId()) {
                matiereCombo.setValue(m);
                break;
            }
        }
        
        for (Enseignant e : enseignantCombo.getItems()) {
            if (e.getId() == cours.getEnseignantId()) {
                enseignantCombo.setValue(e);
                break;
            }
        }
        
        for (Classe c : classeCombo.getItems()) {
            if (c.getId() == cours.getClasseId()) {
                classeCombo.setValue(c);
                break;
            }
        }
        
        typeCoursCombo.setValue(cours.getTypeCours());
        
        int volume = cours.getVolumeHoraire();
        volumeField.setText(String.valueOf(volume > 0 ? volume : ""));
        
        if (cours.getGroupes() == null || cours.getGroupes().isEmpty()
                || "Tous".equalsIgnoreCase(cours.getGroupes())) {
            tousGroupesRadio.setSelected(true);
            groupesList.getSelectionModel().clearSelection();
        } else {
            groupesSpecifiquesRadio.setSelected(true);
            String[] groupes = cours.getGroupes().split(",");
            for (String g : groupes) {
                groupesList.getSelectionModel().select(g.trim());
            }
        }
        
        coursSelectionne = cours;
        
        modifierButton.setDisable(false);
        supprimerButton.setDisable(false);
        ajouterButton.setDisable(true);
    }
    
    private void viderFormulaire() {
        matiereCombo.setValue(null);
        enseignantCombo.setValue(null);
        classeCombo.setValue(null);
        typeCoursCombo.setValue(null);
        volumeField.clear();
        tousGroupesRadio.setSelected(true);
        groupesList.getSelectionModel().clearSelection();
        
        coursSelectionne = null;
        
        modifierButton.setDisable(true);
        supprimerButton.setDisable(true);
        ajouterButton.setDisable(false);
        
        coursTable.getSelectionModel().clearSelection();
    }
    
    /**
     * Ajoute un nouveau cours dans la base de données.
     * Valide les données du formulaire avant l'insertion.
     */
    private void ajouterCours() {
        if (!validerFormulaire()) return;
        
        String volumeText = volumeField.getText().trim();
        if (volumeText.isEmpty()) {
            afficherMessageErreur("Le volume horaire est requis");
            return;
        }
        
        int volume;
        try {
            volume = Integer.parseInt(volumeText);
            if (volume <= 0) {
                afficherMessageErreur("Le volume horaire doit être supérieur à 0");
                return;
            }
        } catch (NumberFormatException e) {
            afficherMessageErreur("Le volume horaire doit être un nombre valide");
            return;
        }
        
        String groupes = tousGroupesRadio.isSelected() ? "Tous" :
            String.join(",", groupesList.getSelectionModel().getSelectedItems());
        
        Cours cours = new Cours(
            matiereCombo.getValue().getId(),
            enseignantCombo.getValue().getId(),
            classeCombo.getValue().getId(),
            typeCoursCombo.getValue(),
            groupes,
            volume
        );
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                coursService.ajouter(cours);
                return null;
            }
            
            @Override
            protected void succeeded() {
                afficherMessageSucces("✅ Cours ajouté");
                viderFormulaire();
                chargerCours();
            }
            
            @Override
            protected void failed() {
                String erreur = getException().getMessage();
                afficherMessageErreur("❌ Erreur ajout: " + (erreur != null ? erreur : "Vérifiez les données"));
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Modifie le cours sélectionné.
     * @param cours le cours à modifier
     */
    private void modifierCours() {
        if (coursSelectionne == null) return;
        if (!validerFormulaire()) return;
        
        String volumeText = volumeField.getText().trim();
        if (volumeText.isEmpty()) {
            afficherMessageErreur("Le volume horaire est requis");
            return;
        }
        
        int volume;
        try {
            volume = Integer.parseInt(volumeText);
            if (volume <= 0) {
                afficherMessageErreur("Le volume horaire doit être supérieur à 0");
                return;
            }
        } catch (NumberFormatException e) {
            afficherMessageErreur("Le volume horaire doit être un nombre valide");
            return;
        }
        
        coursSelectionne.setMatiereId(matiereCombo.getValue().getId());
        coursSelectionne.setEnseignantId(enseignantCombo.getValue().getId());
        coursSelectionne.setClasseId(classeCombo.getValue().getId());
        coursSelectionne.setTypeCours(typeCoursCombo.getValue());
        coursSelectionne.setGroupes(tousGroupesRadio.isSelected() ? "Tous" :
            String.join(",", groupesList.getSelectionModel().getSelectedItems()));
        coursSelectionne.setVolumeHoraire(volume);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                coursService.modifier(coursSelectionne);
                return null;
            }
            
            @Override
            protected void succeeded() {
                afficherMessageSucces("✅ Cours modifié");
                viderFormulaire();
                chargerCours();
            }
            
            @Override
            protected void failed() {
                String erreur = getException().getMessage();
                afficherMessageErreur("❌ Erreur modification: " + (erreur != null ? erreur : "Vérifiez les données"));
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Supprime un cours après confirmation.
     * @param cours le cours à supprimer
     */
    private void supprimerCours(Cours cours) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le cours ?");
        alert.setContentText("Supprimer le cours ID " + cours.getId() + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        coursService.supprimer(cours.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherMessageSucces("✅ Cours supprimé");
                        viderFormulaire();
                        chargerCours();
                    }
                    
                    @Override
                    protected void failed() {
                        afficherMessageErreur("❌ Erreur suppression");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    private boolean validerFormulaire() {
        if (matiereCombo.getValue() == null) {
            afficherMessageErreur("Matière requise");
            return false;
        }
        if (enseignantCombo.getValue() == null) {
            afficherMessageErreur("Enseignant requis");
            return false;
        }
        if (classeCombo.getValue() == null) {
            afficherMessageErreur("Classe requise");
            return false;
        }
        if (typeCoursCombo.getValue() == null) {
            afficherMessageErreur("Type de cours requis");
            return false;
        }
        if (groupesSpecifiquesRadio.isSelected() && groupesList.getSelectionModel().getSelectedItems().isEmpty()) {
            afficherMessageErreur("Sélectionnez au moins un groupe");
            return false;
        }
        return true;
    }
    
    
    private void afficherMessageSucces(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #4CAF50;");
            messageLabel.setVisible(true);
            
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> messageLabel.setVisible(false));
            }).start();
        }
    }
    
    private void afficherMessageErreur(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #f44336;");
            messageLabel.setVisible(true);
        }
    }
    
    @FXML
	protected void handleRetour() {
        try {
            Stage stage = (Stage) ((Node) messageLabel).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordGestionnaire.fxml"));
            Parent root = loader.load();
            
            TableauBordGestionnaireControleur controleur = loader.getController();
            if (controleur != null && utilisateurConnecte != null) {
                controleur.initialiserAvecUtilisateur(utilisateurConnecte, stage);
            }
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            
        } catch (IOException e) {
            afficherErreur("Impossible de retourner");
        }
    }
}