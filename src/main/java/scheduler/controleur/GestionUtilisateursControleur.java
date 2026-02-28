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
import javafx.scene.layout.VBox;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import java.sql.SQLException;
import java.io.IOException;
import java.util.*;
import javafx.scene.layout.GridPane;

/**
 * Gestion des comptes utilisateurs.
 */
public class GestionUtilisateursControleur extends TableauBordControleur {
    
    @FXML private TableView<Utilisateur> utilisateurTable;
    @FXML private TableColumn<Utilisateur, Integer> idColumn;
    @FXML private TableColumn<Utilisateur, String> nomColumn;
    @FXML private TableColumn<Utilisateur, String> prenomColumn;
    @FXML private TableColumn<Utilisateur, String> emailColumn;
    @FXML private TableColumn<Utilisateur, String> roleColumn;
    @FXML private TableColumn<Utilisateur, String> statutColumn;
    @FXML private TableColumn<Utilisateur, String> dateColumn;
    @FXML private TableColumn<Utilisateur, String> ufrColumn;
    @FXML private TableColumn<Utilisateur, String> classeColumn;
    @FXML private TableColumn<Utilisateur, String> typeEtudiantColumn; // NOUVEAU
    @FXML private TableColumn<Utilisateur, Void> actionsColumn;
    
    @FXML private ComboBox<String> filtreRoleCombo;
    @FXML private ComboBox<String> filtreStatutCombo;
    @FXML private ComboBox<Ufr> filtreUfrCombo;
    @FXML private TextField rechercheField;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    @FXML private Button retourButton;
    
    @FXML private Label totalUtilisateursLabel;
    @FXML private Label adminsLabel;
    @FXML private Label gestionnairesLabel;
    @FXML private Label enseignantsLabel;
    @FXML private Label etudiantsLabel;
    @FXML private Label etudiantsNormauxLabel; // NOUVEAU
    @FXML private Label etudiantsResponsablesLabel; // NOUVEAU
    @FXML private Label enAttenteLabel;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private UtilisateurService utilisateurService;
    private UfrService ufrService;
    private ClasseService classeService;
    private EmailService emailService;
    private ObservableList<Utilisateur> utilisateurList;
    private List<Utilisateur> tousUtilisateurs;
    private List<Ufr> ufrs;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.utilisateurService = new UtilisateurService();
        this.ufrService = new UfrService();
        this.classeService = new ClasseService();
        this.emailService = new EmailService();
        this.utilisateurList = FXCollections.observableArrayList();
        this.tousUtilisateurs = new ArrayList<>();
        this.ufrs = new ArrayList<>();
        
        configurerTableau();
        configurerFiltres();
        configurerEvenements();
        chargerUfr();
        chargerUtilisateurs();
        
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
        chargerUtilisateurs();
    }
    
    private void configurerTableau() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        roleColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> traduireRole(cellData.getValue().getRole())
            )
        );
        
        ufrColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() instanceof Etudiant) {
                Etudiant e = (Etudiant) cellData.getValue();
                return javafx.beans.binding.Bindings.createStringBinding(
                    () -> getNomUfr(e.getUfrId())
                );
            }
            return javafx.beans.binding.Bindings.createStringBinding(() -> "-");
        });
        
        classeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() instanceof Etudiant) {
                Etudiant e = (Etudiant) cellData.getValue();
                return javafx.beans.binding.Bindings.createStringBinding(
                    () -> getIntituleClasse(e.getClasseId())
                );
            }
            return javafx.beans.binding.Bindings.createStringBinding(() -> "-");
        });
        
        // NOUVEAU : Colonne type étudiant
        typeEtudiantColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() instanceof Etudiant) {
                Etudiant e = (Etudiant) cellData.getValue();
                return javafx.beans.binding.Bindings.createStringBinding(
                    () -> {
                        String type = e.getTypeEtudiant();
                        if ("responsable".equals(type)) {
                            return "Responsable de classe";
                        }
                        return "Normal";
                    }
                );
            }
            return javafx.beans.binding.Bindings.createStringBinding(() -> "-");
        });
        
        typeEtudiantColumn.setCellFactory(column -> new TableCell<Utilisateur, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null || "-".equals(type)) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(type);
                    if (type.equals("Responsable de classe")) {
                        setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #4A5568;");
                    }
                }
            }
        });
        
        statutColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().isEstValide() ? "Actif" : "En attente"
            )
        );
        
        statutColumn.setCellFactory(column -> new TableCell<Utilisateur, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(statut);
                    if (statut.equals("Actif")) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        dateColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getDateCreation() != null ?
                    cellData.getValue().getDateCreation().toLocalDate().toString() : ""
            )
        );
        
        actionsColumn.setCellFactory(param -> new TableCell<Utilisateur, Void>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final Button resetBtn = new Button("🔄 Reset MDP");
            
            {
                editBtn.setStyle("-fx-background-color: #6B4226; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #9E2A2B; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
                resetBtn.setStyle("-fx-background-color: #D39A43; -fx-text-fill: #3D261A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
                
                editBtn.setOnAction(event -> {
                    Utilisateur user = getTableView().getItems().get(getIndex());
                    ouvrirDialogueModification(user);
                });
                
                deleteBtn.setOnAction(event -> {
                    Utilisateur user = getTableView().getItems().get(getIndex());
                    supprimerUtilisateur(user);
                });
                
                resetBtn.setOnAction(event -> {
                    Utilisateur user = getTableView().getItems().get(getIndex());
                    reinitialiserMotDePasse(user);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Utilisateur user = getTableView().getItems().get(getIndex());
                    HBox box = new HBox(6, editBtn, deleteBtn, resetBtn);
                    box.setAlignment(Pos.CENTER);
                    
                    if (!user.isEstValide()) {
                        Button validerBtn = new Button("✓ Valider");
                        validerBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
                        validerBtn.setOnAction(e -> validerUtilisateur(user));
                        box.getChildren().add(0, validerBtn);
                    }
                    
                    setGraphic(box);
                }
            }
        });
        actionsColumn.setPrefWidth(350);
        
        utilisateurTable.setItems(utilisateurList);
        utilisateurTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }
    
    /**
     * Ouvre le dialogue de modification d'un utilisateur.
     * @param utilisateur l'utilisateur à modifier
     */
    private void ouvrirDialogueModification(Utilisateur utilisateur) {
        Dialog<Utilisateur> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'utilisateur");
        dialog.setHeaderText("Modification de " + utilisateur.getPrenom() + " " + utilisateur.getNom());
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));
        
        TextField nomField = new TextField(utilisateur.getNom());
        TextField prenomField = new TextField(utilisateur.getPrenom());
        TextField emailField = new TextField(utilisateur.getEmail());
        
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Prénom:"), 0, 1);
        grid.add(prenomField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        
        // Champs spécifiques pour étudiants
        if (utilisateur instanceof Etudiant) {
            Etudiant etu = (Etudiant) utilisateur;
            
            TextField numField = new TextField(etu.getNumeroEtudiant());
            ComboBox<Ufr> ufrCombo = new ComboBox<>();
            ufrCombo.getItems().addAll(ufrs);
            
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
            
            for (Ufr u : ufrs) {
                if (u.getId() == etu.getUfrId()) {
                    ufrCombo.setValue(u);
                    break;
                }
            }
            
            ComboBox<Classe> classeCombo = new ComboBox<>();
            try {
                List<Classe> classes = classeService.listerParUfr(etu.getUfrId());
                classeCombo.getItems().addAll(classes);
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
                        setText(empty || item == null ? "Sélectionner une classe" : item.getIntitule() + " (" + item.getAnneeScolaire() + ")");
                    }
                });
                for (Classe c : classes) {
                    if (c.getId() == etu.getClasseId()) {
                        classeCombo.setValue(c);
                        break;
                    }
                }
            } catch (SQLException e) {
                logger.error("Erreur chargement classes", e);
            }
            
            ComboBox<String> typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll("Normal", "Responsable de classe");
            if ("responsable".equals(etu.getTypeEtudiant())) {
                typeCombo.setValue("Responsable de classe");
            } else {
                typeCombo.setValue("Normal");
            }
            
            grid.add(new Label("N° étudiant:"), 0, 3);
            grid.add(numField, 1, 3);
            grid.add(new Label("UFR:"), 0, 4);
            grid.add(ufrCombo, 1, 4);
            grid.add(new Label("Classe:"), 0, 5);
            grid.add(classeCombo, 1, 5);
            grid.add(new Label("Type:"), 0, 6);
            grid.add(typeCombo, 1, 6);
            
            ufrCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        List<Classe> newClasses = classeService.listerParUfr(newVal.getId());
                        classeCombo.getItems().setAll(newClasses);
                    } catch (SQLException e) {
                        logger.error("Erreur", e);
                    }
                }
            });
            
            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    utilisateur.setNom(nomField.getText().trim());
                    utilisateur.setPrenom(prenomField.getText().trim());
                    utilisateur.setEmail(emailField.getText().trim());
                    if (utilisateur instanceof Etudiant) {
                        Etudiant e = (Etudiant) utilisateur;
                        e.setNumeroEtudiant(numField.getText().trim());
                        if (ufrCombo.getValue() != null) {
                            e.setUfrId(ufrCombo.getValue().getId());
                        }
                        if (classeCombo.getValue() != null) {
                            e.setClasseId(classeCombo.getValue().getId());
                        }
                        // NOUVEAU : mettre à jour le type étudiant
                        String typeValue = typeCombo.getValue();
                        if ("Responsable de classe".equals(typeValue)) {
                            e.setTypeEtudiant("responsable");
                        } else {
                            e.setTypeEtudiant("normal");
                        }
                    }
                    return utilisateur;
                }
                return null;
            });
            
        } else {
            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    utilisateur.setNom(nomField.getText().trim());
                    utilisateur.setPrenom(prenomField.getText().trim());
                    utilisateur.setEmail(emailField.getText().trim());
                    return utilisateur;
                }
                return null;
            });
        }
        
        dialog.getDialogPane().setContent(grid);
        
        Optional<Utilisateur> result = dialog.showAndWait();
        result.ifPresent(u -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    utilisateurService.modifier(u);
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    afficherNotification("Succès", "✅ Utilisateur modifié");
                    chargerUtilisateurs();
                }
                
                @Override
                protected void failed() {
                    afficherErreur("❌ Erreur lors de la modification");
                }
            };
            new Thread(task).start();
        });
    }
    
    private Ufr getUfrParId(int ufrId) {
        for (Ufr ufr : ufrs) {
            if (ufr.getId() == ufrId) return ufr;
        }
        return null;
    }
    
    private void configurerFiltres() {
        filtreRoleCombo.getItems().addAll("Tous", "Administrateur", "Gestionnaire", "Enseignant", "Étudiant");
        filtreRoleCombo.setValue("Tous");
        filtreRoleCombo.setPrefWidth(100);
        
        filtreStatutCombo.getItems().addAll("Tous", "Actif", "En attente");
        filtreStatutCombo.setValue("Tous");
        filtreStatutCombo.setPrefWidth(100);
        
        filtreUfrCombo.setPrefWidth(150);
        filtreUfrCombo.setPromptText("Toutes les UFR");
        
        rechercheField.setPromptText("Rechercher...");
        rechercheField.setPrefWidth(200);
        
        rechercherButton.setText("Rechercher");
        rechercherButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        
        reinitialiserButton.setText("Réinitialiser");
        reinitialiserButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white;");
        
        rechercherButton.setOnAction(e -> filtrerUtilisateurs());
        reinitialiserButton.setOnAction(e -> {
            filtreRoleCombo.setValue("Tous");
            filtreStatutCombo.setValue("Tous");
            filtreUfrCombo.setValue(null);
            rechercheField.clear();
            utilisateurList.setAll(tousUtilisateurs);
        });
    }
    
    private void configurerEvenements() {
        filtreRoleCombo.valueProperty().addListener((obs, oldVal, newVal) -> filtrerUtilisateurs());
        filtreStatutCombo.valueProperty().addListener((obs, oldVal, newVal) -> filtrerUtilisateurs());
        filtreUfrCombo.valueProperty().addListener((obs, oldVal, newVal) -> filtrerUtilisateurs());
    }
    
    private void chargerUfr() {
        Task<List<Ufr>> task = new Task<>() {
            @Override
            protected List<Ufr> call() throws SQLException {
                return ufrService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                ufrs = getValue();
                if (ufrs == null) ufrs = new ArrayList<>();
                
                filtreUfrCombo.getItems().clear();
                filtreUfrCombo.getItems().add(null);
                filtreUfrCombo.getItems().addAll(ufrs);
                
                filtreUfrCombo.setCellFactory(lv -> new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Toutes les UFR" : item.getNom());
                    }
                });
                
                filtreUfrCombo.setButtonCell(new ListCell<Ufr>() {
                    @Override
                    protected void updateItem(Ufr item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Toutes les UFR" : item.getNom());
                    }
                });
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement UFR", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    private String getNomUfr(int ufrId) {
        if (ufrs == null || ufrs.isEmpty()) return "Inconnue";
        for (Ufr ufr : ufrs) {
            if (ufr.getId() == ufrId) return ufr.getNom();
        }
        return "Inconnue";
    }
    
    private String getIntituleClasse(int classeId) {
        try {
            Classe classe = classeService.trouverParId(classeId);
            return classe != null ? classe.getIntitule() : "Inconnue";
        } catch (SQLException e) {
            return "Erreur";
        }
    }
    
    private void chargerUtilisateurs() {
        chargementIndicator.setVisible(true);
        
        Task<List<Utilisateur>> task = new Task<>() {
            @Override
            protected List<Utilisateur> call() throws SQLException {
                return utilisateurService.listerTous();
            }
            
            @Override
            protected void succeeded() {
                tousUtilisateurs = getValue();
                utilisateurList.setAll(tousUtilisateurs);
                mettreAJourStatistiques();
                chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement utilisateurs", getException());
                chargementIndicator.setVisible(false);
                afficherErreur("Erreur de chargement des utilisateurs");
            }
        };
        
        new Thread(task).start();
    }
    
    private void mettreAJourStatistiques() {
        int total = tousUtilisateurs.size();
        int admins = 0, gestionnaires = 0, enseignants = 0, etudiants = 0;
        int etudiantsNormaux = 0, etudiantsResponsables = 0;
        int enAttente = 0;
        
        for (Utilisateur u : tousUtilisateurs) {
            switch (u.getRole()) {
                case "admin": admins++; break;
                case "gestionnaire": gestionnaires++; break;
                case "enseignant": enseignants++; break;
                case "etudiant": 
                    etudiants++;
                    if (u instanceof Etudiant) {
                        Etudiant e = (Etudiant) u;
                        if ("responsable".equals(e.getTypeEtudiant())) {
                            etudiantsResponsables++;
                        } else {
                            etudiantsNormaux++;
                        }
                    }
                    break;
            }
            if (!u.isEstValide()) enAttente++;
        }
        
        totalUtilisateursLabel.setText(String.valueOf(total));
        adminsLabel.setText(String.valueOf(admins));
        gestionnairesLabel.setText(String.valueOf(gestionnaires));
        enseignantsLabel.setText(String.valueOf(enseignants));
        etudiantsLabel.setText(String.valueOf(etudiants));
        etudiantsNormauxLabel.setText(String.valueOf(etudiantsNormaux));
        etudiantsResponsablesLabel.setText(String.valueOf(etudiantsResponsables));
        enAttenteLabel.setText(String.valueOf(enAttente));
    }
    
    /**
     * Filtre la liste des utilisateurs selon les critères.
     */
    private void filtrerUtilisateurs() {
        if (tousUtilisateurs == null || tousUtilisateurs.isEmpty()) return;
        
        String recherche = rechercheField.getText().toLowerCase().trim();
        String roleFiltre = filtreRoleCombo.getValue();
        String statutFiltre = filtreStatutCombo.getValue();
        Ufr ufrFiltre = filtreUfrCombo.getValue();
        
        List<Utilisateur> filtres = new ArrayList<>(tousUtilisateurs);
        
        if (!recherche.isEmpty()) {
            filtres.removeIf(u -> 
                !u.getNom().toLowerCase().contains(recherche) &&
                !u.getPrenom().toLowerCase().contains(recherche) &&
                !u.getEmail().toLowerCase().contains(recherche)
            );
        }
        
        if (roleFiltre != null && !"Tous".equals(roleFiltre)) {
            String roleCode = roleVersCode(roleFiltre);
            filtres.removeIf(u -> !u.getRole().equals(roleCode));
        }
        
        if (statutFiltre != null && !"Tous".equals(statutFiltre)) {
            boolean actif = "Actif".equals(statutFiltre);
            filtres.removeIf(u -> u.isEstValide() != actif);
        }
        
        if (ufrFiltre != null) {
            filtres.removeIf(u -> {
                if (u instanceof Etudiant) {
                    return ((Etudiant) u).getUfrId() != ufrFiltre.getId();
                }
                return true;
            });
        }
        
        utilisateurList.setAll(filtres);
    }
    
    /**
     * Traduit un rôle technique en libellé français.
     * @param role le rôle technique (admin, enseignant...)
     * @return le libellé en français
     */
    protected String traduireRole(String role) {
        switch (role) {
            case "admin": return "Administrateur";
            case "gestionnaire": return "Gestionnaire";
            case "enseignant": return "Enseignant";
            case "etudiant": return "Étudiant";
            default: return role;
        }
    }
    
    private String roleVersCode(String roleFr) {
        switch (roleFr) {
            case "Administrateur": return "admin";
            case "Gestionnaire": return "gestionnaire";
            case "Enseignant": return "enseignant";
            case "Étudiant": return "etudiant";
            default: return "";
        }
    }
    
    private void supprimerUtilisateur(Utilisateur utilisateur) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'utilisateur ?");
        alert.setContentText("Supprimer " + utilisateur.getPrenom() + " " + utilisateur.getNom() + " ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        utilisateurService.supprimer(utilisateur.getId());
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherNotification("Succès", "✅ Utilisateur supprimé");
                        chargerUtilisateurs();
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("❌ Erreur suppression");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Valide l'inscription d'un utilisateur et génère un mot de passe.
     * @param utilisateur l'utilisateur à valider
     */
    private void validerUtilisateur(Utilisateur utilisateur) {
        String motDePasse = genererMotDePasse();
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Validation");
        alert.setHeaderText("Valider l'utilisateur ?");
        alert.setContentText("Mot de passe généré: " + motDePasse);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        utilisateur.setEstValide(true);
                        utilisateur.setMotDePasse(motDePasse);
                        utilisateurService.modifier(utilisateur);
                        
                        emailService.envoyerValidationInscription(
                            utilisateur.getEmail(),
                            utilisateur.getNom(),
                            utilisateur.getPrenom(),
                            motDePasse
                        );
                        
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherNotification("Succès", "✅ Utilisateur validé et email envoyé");
                        chargerUtilisateurs();
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("❌ Erreur validation");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Réinitialise le mot de passe d'un utilisateur.
     * @param utilisateur l'utilisateur concerné
     */
    private void reinitialiserMotDePasse(Utilisateur utilisateur) {
        String nouveauMotDePasse = genererMotDePasse();
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Réinitialisation");
        alert.setHeaderText("Réinitialiser le mot de passe ?");
        alert.setContentText("Nouveau mot de passe: " + nouveauMotDePasse + 
                           "\nUn email sera envoyé à " + utilisateur.getEmail());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        utilisateur.setMotDePasse(nouveauMotDePasse);
                        utilisateurService.modifier(utilisateur);
                        
                        emailService.envoyerNouveauMotDePasse(
                            utilisateur.getEmail(),
                            utilisateur.getNom(),
                            utilisateur.getPrenom(),
                            nouveauMotDePasse
                        );
                        
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherNotification("Succès", "✅ Mot de passe réinitialisé et email envoyé");
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("❌ Erreur réinitialisation");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Génère un mot de passe aléatoire.
     * @return mot de passe de 8 caractères
     */
    private String genererMotDePasse() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    @FXML
    private void handleAjouterUtilisateur() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Inscription.fxml"));
            Parent root = loader.load();
            
            InscriptionControleur controleur = loader.getController();
            controleur.setPrimaryStage(primaryStage);
            controleur.setModeAdmin(true);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur ajout utilisateur", e);
        }
    }
    
    @FXML
    private void handleVoirDemandes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DemandesInscription.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur navigation demandes", e);
        }
    }
    
    private void handleRetourGestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordAdmin.fxml"));
            Parent root = loader.load();
            
            TableauBordAdminControleur controleur = loader.getController();
            controleur.initialiserAvecUtilisateur(utilisateurConnecte, primaryStage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner");
        }
    }
}