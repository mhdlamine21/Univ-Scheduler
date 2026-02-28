package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import scheduler.dao.DemandeInscriptionDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.io.IOException;
import java.util.*;

/**
 * Traitement des demandes d'inscription.
 */
public class DemandesInscriptionControleur extends TableauBordControleur {
    
    @FXML private TableView<DemandeInscription> demandeTable;
    @FXML private TableColumn<DemandeInscription, Integer> idColumn;
    @FXML private TableColumn<DemandeInscription, String> nomColumn;
    @FXML private TableColumn<DemandeInscription, String> prenomColumn;
    @FXML private TableColumn<DemandeInscription, String> emailColumn;
    @FXML private TableColumn<DemandeInscription, String> roleColumn;
    @FXML private TableColumn<DemandeInscription, String> detailsColumn;
    @FXML private TableColumn<DemandeInscription, String> dateColumn;
    @FXML private TableColumn<DemandeInscription, String> statutColumn;
    @FXML private TableColumn<DemandeInscription, Void> actionsColumn;
    
    @FXML private Label totalDemandesLabel;
    @FXML private Label etudiantsLabel;
    @FXML private Label enseignantsLabel;
    @FXML private Label gestionnairesLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Button retourButton;
    @FXML private Button toutValiderButton;
    @FXML private Button toutRefuserButton;
    
    @FXML private ComboBox<String> filtreStatutCombo;
    @FXML private TextField rechercheField;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    @FXML private Button rafraichirButton;
    
    @FXML private ProgressIndicator chargementIndicator;
    
    private AuthentificationService authService;
    private ClasseService classeService;
    private UfrService ufrService;
    private DemandeInscriptionDAO demandeDAO;
    private EmailService emailService;
    private ObservableList<DemandeInscription> demandeList;
    private List<DemandeInscription> toutesDemandes;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.authService = new AuthentificationService();
        this.classeService = new ClasseService();
        this.ufrService = new UfrService();
        this.demandeDAO = new DemandeInscriptionDAO();
        this.emailService = new EmailService();
        this.demandeList = FXCollections.observableArrayList();
        this.toutesDemandes = new ArrayList<>();
        
        mettreAJourInterface();
        
        configurerTableau();
        configurerFiltres();
        configurerBoutonsMassifs();
        chargerDemandes();
        
        if (retourButton != null) {
            retourButton.setOnAction(e -> handleRetour());
        }
    }
    
    @Override
    protected void initialiserTableauBord() {
        // Déjà fait dans initialize()
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerDemandes();
    }
    
    @Override
    protected void mettreAJourInterface() {
        super.mettreAJourInterface();
    }
    
    private void configurerBoutonsMassifs() {
        if (toutValiderButton != null) {
            toutValiderButton.setOnAction(e -> toutValider());
            toutValiderButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 15;");
        }
        
        if (toutRefuserButton != null) {
            toutRefuserButton.setOnAction(e -> toutRefuser());
            toutRefuserButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 8 15;");
        }
    }
    
    /**
     * Valide toutes les demandes en attente.
     */
    private void toutValider() {
        List<DemandeInscription> enAttente = new ArrayList<>();
        for (DemandeInscription d : toutesDemandes) {
            if ("en_attente".equals(d.getStatut())) {
                enAttente.add(d);
            }
        }
        
        if (enAttente.isEmpty()) {
            afficherNotification("Info", "Aucune demande en attente");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Validation massive");
        alert.setHeaderText("Valider toutes les demandes ?");
        alert.setContentText("Vous allez valider " + enAttente.size() + " demande(s).");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        for (DemandeInscription d : enAttente) {
                            String motDePasse = genererMotDePasse();
                            authService.validerInscription(d.getId(), motDePasse);
                            
                            try {
                                emailService.envoyerValidationInscription(
                                    d.getEmail(), d.getNom(), d.getPrenom(), motDePasse
                                );
                            } catch (Exception e) {
                                logger.error("Erreur envoi email à " + d.getEmail(), e);
                            }
                            
                            Thread.sleep(100);
                        }
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherNotification("Succès", enAttente.size() + " demande(s) validée(s)");
                        chargerDemandes();
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("Erreur lors de la validation massive");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Refuse toutes les demandes en attente avec un motif.
     */
    private void toutRefuser() {
        List<DemandeInscription> enAttente = new ArrayList<>();
        for (DemandeInscription d : toutesDemandes) {
            if ("en_attente".equals(d.getStatut())) {
                enAttente.add(d);
            }
        }
        
        if (enAttente.isEmpty()) {
            afficherNotification("Info", "Aucune demande en attente");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refus massif");
        dialog.setHeaderText("Motif du refus pour toutes les demandes");
        dialog.setContentText("Raison :");
        
        dialog.showAndWait().ifPresent(motif -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Refuser toutes les demandes ?");
            confirm.setContentText("Vous allez refuser " + enAttente.size() + " demande(s).");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            for (DemandeInscription d : enAttente) {
                                authService.refuserInscription(d.getId());
                                
                                try {
                                    emailService.envoyerEmailRefus(
                                        d.getEmail(), d.getNom(), d.getPrenom(), motif
                                    );
                                } catch (Exception e) {
                                    logger.error("Erreur envoi email à " + d.getEmail(), e);
                                }
                                
                                Thread.sleep(100);
                            }
                            return null;
                        }
                        
                        @Override
                        protected void succeeded() {
                            afficherNotification("Succès", enAttente.size() + " demande(s) refusée(s)");
                            chargerDemandes();
                        }
                        
                        @Override
                        protected void failed() {
                            afficherErreur("Erreur lors du refus massif");
                        }
                    };
                    
                    new Thread(task).start();
                }
            });
        });
    }
    
    private void configurerTableau() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);
        idColumn.setStyle("-fx-alignment: CENTER;");
        
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        nomColumn.setPrefWidth(100);
        
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        prenomColumn.setPrefWidth(100);
        
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailColumn.setPrefWidth(200);
        
        roleColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> traduireRole(cellData.getValue().getRoleDemande())
            )
        );
        roleColumn.setPrefWidth(100);
        roleColumn.setStyle("-fx-alignment: CENTER;");
        
        detailsColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    DemandeInscription d = cellData.getValue();
                    if (d.getRoleDemande().equals("etudiant")) {
                        return "N°: " + d.getNumeroEtudiant();
                    } else if (d.getRoleDemande().equals("enseignant")) {
                        String grade = d.getGrade() != null ? d.getGrade() + " " : "";
                        return grade + "Matricule: " + d.getMatriculeEnseignant();
                    }
                    return "";
                }
            )
        );
        detailsColumn.setPrefWidth(150);
        
        dateColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getDateDemande() != null ?
                    cellData.getValue().getDateDemande().toLocalDate().toString() : ""
            )
        );
        dateColumn.setPrefWidth(100);
        dateColumn.setStyle("-fx-alignment: CENTER;");
        
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        statutColumn.setPrefWidth(100);
        statutColumn.setStyle("-fx-alignment: CENTER;");
        
        statutColumn.setCellFactory(column -> new TableCell<DemandeInscription, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                } else {
                    setText(statut);
                    switch (statut) {
                        case "en_attente":
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-alignment: CENTER;");
                            break;
                        case "validee":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-alignment: CENTER;");
                            break;
                        case "refusee":
                            setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold; -fx-alignment: CENTER;");
                            break;
                    }
                }
            }
        });
        
        actionsColumn.setCellFactory(param -> new TableCell<DemandeInscription, Void>() {
            private final Button validerBtn = new Button("✓");
            private final Button refuserBtn = new Button("✗");
            private final Button detailsBtn = new Button("👁️");
            
            {
                validerBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px; -fx-min-width: 30px;");
                refuserBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 12px; -fx-min-width: 30px;");
                detailsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 12px; -fx-min-width: 30px;");
                
                validerBtn.setOnAction(event -> {
                    DemandeInscription demande = getTableView().getItems().get(getIndex());
                    validerDemande(demande);
                });
                
                refuserBtn.setOnAction(event -> {
                    DemandeInscription demande = getTableView().getItems().get(getIndex());
                    refuserDemande(demande);
                });
                
                detailsBtn.setOnAction(event -> {
                    DemandeInscription demande = getTableView().getItems().get(getIndex());
                    afficherDetails(demande);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, detailsBtn, validerBtn, refuserBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        actionsColumn.setPrefWidth(120);
        
        demandeTable.setItems(demandeList);
        demandeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    private void configurerFiltres() {
        filtreStatutCombo.getItems().addAll("Tous", "en_attente", "validee", "refusee");
        filtreStatutCombo.setValue("Tous");
        filtreStatutCombo.setPrefWidth(150);
        
        rechercheField.setPromptText("Rechercher par nom, prénom ou email...");
        rechercheField.setPrefWidth(250);
        
        rechercherButton.setText("Rechercher");
        rechercherButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8 15;");
        
        reinitialiserButton.setText("Réinitialiser");
        reinitialiserButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-padding: 8 15;");
        
        rafraichirButton.setText("Rafraîchir");
        rafraichirButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 15;");
        
        rechercherButton.setOnAction(e -> filtrerDemandes());
        reinitialiserButton.setOnAction(e -> {
            filtreStatutCombo.setValue("Tous");
            rechercheField.clear();
            demandeList.setAll(toutesDemandes);
        });
    }
    
    /**
     * Filtre les demandes selon les critères.
     */
    private void filtrerDemandes() {
        if (toutesDemandes == null || toutesDemandes.isEmpty()) return;
        
        String recherche = rechercheField.getText().toLowerCase().trim();
        String statut = filtreStatutCombo.getValue();
        
        List<DemandeInscription> filtrees = new ArrayList<>(toutesDemandes);
        
        if (!recherche.isEmpty()) {
            filtrees.removeIf(d -> 
                !d.getNom().toLowerCase().contains(recherche) &&
                !d.getPrenom().toLowerCase().contains(recherche) &&
                !d.getEmail().toLowerCase().contains(recherche)
            );
        }
        
        if (statut != null && !"Tous".equals(statut)) {
            filtrees.removeIf(d -> !d.getStatut().equals(statut));
        }
        
        demandeList.setAll(filtrees);
    }
    
    private void chargerDemandes() {
        chargementIndicator.setVisible(true);
        
        Task<List<DemandeInscription>> task = new Task<>() {
            @Override
            protected List<DemandeInscription> call() throws SQLException {
                return demandeDAO.listerEnAttente();
            }
            
            @Override
            protected void succeeded() {
                toutesDemandes = getValue();
                demandeList.setAll(toutesDemandes);
                mettreAJourStatistiques();
                chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement demandes", getException());
                chargementIndicator.setVisible(false);
                afficherErreur("Erreur de chargement des demandes");
            }
        };
        
        new Thread(task).start();
    }
    
    private void mettreAJourStatistiques() {
        int total = demandeList.size();
        int etudiants = 0, enseignants = 0, gestionnaires = 0;
        int enAttente = 0;
        
        for (DemandeInscription d : demandeList) {
            switch (d.getRoleDemande()) {
                case "etudiant": etudiants++; break;
                case "enseignant": enseignants++; break;
                case "gestionnaire": gestionnaires++; break;
            }
            if ("en_attente".equals(d.getStatut())) {
                enAttente++;
            }
        }
        
        if (totalDemandesLabel != null) totalDemandesLabel.setText(String.valueOf(total));
        if (etudiantsLabel != null) etudiantsLabel.setText(String.valueOf(etudiants));
        if (enseignantsLabel != null) enseignantsLabel.setText(String.valueOf(enseignants));
        if (gestionnairesLabel != null) gestionnairesLabel.setText(String.valueOf(gestionnaires));
        if (enAttenteLabel != null) enAttenteLabel.setText(String.valueOf(enAttente));
    }
    
    protected String traduireRole(String role) {
        switch (role) {
            case "etudiant": return "Étudiant";
            case "enseignant": return "Enseignant";
            case "gestionnaire": return "Gestionnaire";
            case "admin": return "Administrateur";
            default: return role;
        }
    }
    
    private String getNomUfr(int ufrId) {
        try {
            Ufr ufr = ufrService.trouverParId(ufrId);
            return ufr != null ? ufr.getNom() : "Inconnue";
        } catch (SQLException e) {
            return "Inconnue";
        }
    }
    
    /**
     * Valide une demande d'inscription.
     * @param demande la demande à valider
     */
    private void validerDemande(DemandeInscription demande) {
        if (!"en_attente".equals(demande.getStatut())) {
            afficherNotification("Info", "Cette demande n'est plus en attente");
            chargerDemandes();
            return;
        }
        
        String motDePasse = genererMotDePasse();
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Validation");
        alert.setHeaderText("Valider l'inscription ?");
        alert.setContentText("Un email sera envoyé à " + demande.getEmail());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        authService.validerInscription(demande.getId(), motDePasse);
                        
                        emailService.envoyerValidationInscription(
                            demande.getEmail(), 
                            demande.getNom(), 
                            demande.getPrenom(), 
                            motDePasse
                        );
                        
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        afficherNotification("Succès", "Inscription validée");
                        chargerDemandes();
                    }
                    
                    @Override
                    protected void failed() {
                        afficherErreur("Erreur lors de la validation");
                    }
                };
                
                new Thread(task).start();
            }
        });
    }
    
    /**
     * Refuse une demande d'inscription.
     * @param demande la demande à refuser
     */
    private void refuserDemande(DemandeInscription demande) {
        if (!"en_attente".equals(demande.getStatut())) {
            afficherNotification("Info", "Cette demande n'est plus en attente");
            chargerDemandes();
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refus");
        dialog.setHeaderText("Motif du refus");
        dialog.setContentText("Veuillez indiquer la raison du refus:");
        
        dialog.showAndWait().ifPresent(motif -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    authService.refuserInscription(demande.getId());
                    
                    emailService.envoyerEmailRefus(
                        demande.getEmail(), 
                        demande.getNom(), 
                        demande.getPrenom(), 
                        motif
                    );
                    
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    afficherNotification("Succès", "Demande refusée");
                    chargerDemandes();
                }
                
                @Override
                protected void failed() {
                    afficherErreur("Erreur lors du refus");
                }
            };
            
            new Thread(task).start();
        });
    }
    
    /**
     * Affiche les détails d'une demande.
     * @param demande la demande à afficher
     */
    private void afficherDetails(DemandeInscription demande) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la demande");
        alert.setHeaderText(demande.getPrenom() + " " + demande.getNom());
        
        StringBuilder sb = new StringBuilder();
        sb.append("Email: ").append(demande.getEmail()).append("\n");
        sb.append("Rôle: ").append(traduireRole(demande.getRoleDemande())).append("\n");
        sb.append("Date de demande: ").append(demande.getDateDemande().toLocalDate()).append("\n");
        sb.append("Statut: ").append(demande.getStatut()).append("\n\n");
        
        if (demande.getRoleDemande().equals("etudiant")) {
            sb.append("Numéro étudiant: ").append(demande.getNumeroEtudiant()).append("\n");
            if (demande.getUfrId() != null) {
                sb.append("UFR: ").append(getNomUfr(demande.getUfrId())).append("\n");
            }
            if (demande.getClasseId() != null) {
                sb.append("Classe: ").append(getIntituleClasse(demande.getClasseId())).append("\n");
            }
        } else if (demande.getRoleDemande().equals("enseignant")) {
            String grade = demande.getGrade() != null ? demande.getGrade() + " " : "";
            sb.append("Grade: ").append(grade).append("\n");
            sb.append("Matricule: ").append(demande.getMatriculeEnseignant()).append("\n");
        }
        
        alert.setContentText(sb.toString());
        alert.show();
    }
    
    private String getIntituleClasse(int classeId) {
        try {
            Classe classe = classeService.trouverParId(classeId);
            return classe != null ? classe.getIntitule() : "Inconnue";
        } catch (SQLException e) {
            return "Inconnue";
        }
    }
    
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
    private void handleRafraichir() {
        chargerDemandes();
    }
    
    @FXML
    public void handleRetour() {
        try {
            Stage stage = getStageFromScene();
            if (stage == null) {
                logger.error("Stage null pour retour");
                if (primaryStage != null) {
                    stage = primaryStage;
                } else {
                    afficherErreur("Erreur de navigation");
                    retourLogin();
                    return;
                }
            }
            
            if (utilisateurConnecte == null) {
                logger.error("utilisateurConnecte est null");
                if (session != null) {
                    utilisateurConnecte = session.getUtilisateurConnecte();
                }
                if (utilisateurConnecte == null) {
                    afficherErreur("Session expirée");
                    retourLogin();
                    return;
                }
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableauBordAdmin.fxml"));
            Parent root = loader.load();
            
            Object controleur = loader.getController();
            if (controleur instanceof TableauBordControleur) {
                ((TableauBordControleur) controleur).initialiserAvecUtilisateur(utilisateurConnecte, stage);
            }
            
            Scene scene = new Scene(root);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
            
            stage.setScene(scene);
            stage.setTitle("SCHEDULER - Administrateur");
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner au tableau de bord");
        }
    }
    
    
    
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (demandeTable != null && demandeTable.getScene() != null && demandeTable.getScene().getWindow() instanceof Stage) {
            return (Stage) demandeTable.getScene().getWindow();
        }
        return null;
    }
}