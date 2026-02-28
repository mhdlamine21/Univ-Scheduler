package scheduler.controleur;

import scheduler.dao.HistoriqueReservationDAO;
import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * Contrôleur pour l'écran HistoriqueReservation.
 */
public class HistoriqueReservationControleur extends TableauBordControleur {
    
    @FXML private TableView<HistoriqueReservation> historiqueTable;
    @FXML private TableColumn<HistoriqueReservation, Integer> idColumn;
    @FXML private TableColumn<HistoriqueReservation, String> dateColumn;
    @FXML private TableColumn<HistoriqueReservation, String> actionColumn;
    @FXML private TableColumn<HistoriqueReservation, String> detailsColumn;
    @FXML private TableColumn<HistoriqueReservation, String> utilisateurColumn;
    
    @FXML private ComboBox<String> filtreActionCombo;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Button rechercherButton;
    @FXML private Button reinitialiserButton;
    @FXML private Button exporterPDFButton;
    @FXML private Button exporterExcelButton;
    @FXML private Label totalLabel;
    @FXML private ProgressIndicator chargementIndicator;
    
    private HistoriqueReservationDAO historiqueDAO;
    private UtilisateurService utilisateurService;
    private ExportService exportService;
    
    private ObservableList<HistoriqueReservation> historiqueList;
    private List<HistoriqueReservation> tousHistorique;
    
    @Override
    public void initialize() {
        super.initialize();
        
        this.historiqueDAO = new HistoriqueReservationDAO();
        this.utilisateurService = new UtilisateurService();
        this.exportService = new ExportService();
        this.historiqueList = FXCollections.observableArrayList();
        this.tousHistorique = new ArrayList<>();
        
        configurerTableau();
        configurerFiltres();
        configurerBoutons();
        chargerHistorique();
    }
    
    @Override
    protected void initialiserTableauBord() {
        // Déjà fait dans initialize()
    }
    
    @Override
    protected void rafraichirDonnees() {
        chargerHistorique();
    }
    
    private void configurerTableau() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);
        idColumn.setStyle("-fx-alignment: CENTER;");
        
        dateColumn.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getDateAction() != null ?
                    cellData.getValue().getDateAction().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
            )
        );
        dateColumn.setPrefWidth(150);
        dateColumn.setStyle("-fx-alignment: CENTER;");
        
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionColumn.setPrefWidth(150);
        actionColumn.setStyle("-fx-alignment: CENTER;");
        
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        detailsColumn.setPrefWidth(300);
        
        utilisateurColumn.setCellValueFactory(cellData -> 
        javafx.beans.binding.Bindings.createStringBinding(
            () -> getNomUtilisateurComplet(cellData.getValue().getUtilisateurId())
        )
    );
        utilisateurColumn.setPrefWidth(150);
        utilisateurColumn.setStyle("-fx-alignment: CENTER;");
        
        historiqueTable.setItems(historiqueList);
        historiqueTable.setPlaceholder(new Label("Aucun historique"));
    }
    
    private void configurerFiltres() {
        filtreActionCombo.getItems().addAll("Tous", "Réservation", "Annulation", "Prolongation", "Validation", "Modification");
        filtreActionCombo.setValue("Tous");
        
        dateDebutPicker.setPromptText("Date début");
        dateFinPicker.setPromptText("Date fin");
    }
    
    private String getNomUtilisateurComplet(int utilisateurId) {
        try {
            Utilisateur u = utilisateurService.trouverParId(utilisateurId);
            if (u != null) {
                return u.getPrenom() + " " + u.getNom() + " (ID: " + u.getId() + ")";
            }
            return "Utilisateur #" + utilisateurId + " (inconnu)";
        } catch (SQLException e) {
            logger.error("Erreur récupération utilisateur", e);
            return "Erreur ID: " + utilisateurId;
        }
    }
    
    private void configurerBoutons() {
        rechercherButton.setOnAction(e -> filtrerHistorique());
        rechercherButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 5;");
        
        reinitialiserButton.setOnAction(e -> {
            filtreActionCombo.setValue("Tous");
            dateDebutPicker.setValue(null);
            dateFinPicker.setValue(null);
            historiqueList.setAll(tousHistorique);
            totalLabel.setText("Total: " + tousHistorique.size());
        });
        reinitialiserButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-background-radius: 5;");
        
        if (exporterPDFButton != null) {
            exporterPDFButton.setOnAction(e -> exporterPDF());
            exporterPDFButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        }
        
        if (exporterExcelButton != null) {
            exporterExcelButton.setOnAction(e -> exporterExcel());
            exporterExcelButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-background-radius: 5;");
        }
    }
    
    private String getNomUtilisateur(int utilisateurId) {
        try {
            Utilisateur u = utilisateurService.trouverParId(utilisateurId);
            return u != null ? u.getPrenom() + " " + u.getNom() : "Inconnu";
        } catch (SQLException e) {
            logger.error("Erreur récupération utilisateur", e);
            return "Erreur";
        }
    }
    
    private void chargerHistorique() {
        chargementIndicator.setVisible(true);
        
        Task<List<HistoriqueReservation>> task = new Task<>() {
            @Override
            protected List<HistoriqueReservation> call() throws SQLException {
                return historiqueDAO.listerTous();
            }
            
            @Override
            protected void succeeded() {
                tousHistorique = getValue();
                historiqueList.setAll(tousHistorique);
                totalLabel.setText("Total: " + tousHistorique.size());
                chargementIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                logger.error("Erreur chargement historique", getException());
                chargementIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Filtre l'historique selon la période et l'action.
     */
    private void filtrerHistorique() {
        String actionFiltre = filtreActionCombo.getValue();
        List<HistoriqueReservation> filtres = new ArrayList<>(tousHistorique);
        
        if (!"Tous".equals(actionFiltre)) {
            filtres.removeIf(h -> !h.getAction().equals(actionFiltre));
        }
        
        if (dateDebutPicker.getValue() != null) {
            java.time.LocalDateTime debut = dateDebutPicker.getValue().atStartOfDay();
            filtres.removeIf(h -> h.getDateAction().isBefore(debut));
        }
        
        if (dateFinPicker.getValue() != null) {
            java.time.LocalDateTime fin = dateFinPicker.getValue().plusDays(1).atStartOfDay();
            filtres.removeIf(h -> h.getDateAction().isAfter(fin));
        }
        
        historiqueList.setAll(filtres);
        totalLabel.setText("Total: " + filtres.size());
    }
    
    /**
     * Exporte l'historique au format PDF.
     */
    private void exporterPDF() {
        if (historiqueList == null || historiqueList.isEmpty()) {
            afficherErreur("Aucune donnée à exporter");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter l'historique en PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        
        String nomFichier = "Historique_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            
            afficherNotification("Export PDF", "Export en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<HistoriqueReservation> donnees = new ArrayList<>(historiqueList);
                    
                    String[] colonnes = {"ID", "Date", "Action", "Détails", "Utilisateur"};
                    Map<String, Function<HistoriqueReservation, String>> extracteurs = new LinkedHashMap<>();
                    
                    extracteurs.put("ID", h -> String.valueOf(h.getId()));
                    extracteurs.put("Date", h -> h.getDateAction() != null ? 
                        h.getDateAction().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
                    extracteurs.put("Action", h -> h.getAction());
                    extracteurs.put("Détails", h -> h.getDetails() != null ? h.getDetails() : "");
                    extracteurs.put("Utilisateur", h -> {
                        try {
                            Utilisateur u = utilisateurService.trouverParId(h.getUtilisateurId());
                            return u != null ? u.getPrenom() + " " + u.getNom() : "Inconnu";
                        } catch (SQLException e) {
                            return "Erreur";
                        }
                    });
                    
                    exportService.exporterListePDF(
                        donnees, 
                        "Historique des réservations - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        colonnes, 
                        extracteurs, 
                        chemin
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
     * Exporte l'historique au format Excel.
     */
    private void exporterExcel() {
        if (historiqueList == null || historiqueList.isEmpty()) {
            afficherErreur("Aucune donnée à exporter");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter l'historique en Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );
        
        String nomFichier = "Historique_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);
        
        File fichier = fileChooser.showSaveDialog(getStageFromScene());
        
        if (fichier != null) {
            String chemin = fichier.getAbsolutePath();
            
            afficherNotification("Export Excel", "Export en cours...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<HistoriqueReservation> donnees = new ArrayList<>(historiqueList);
                    
                    String[] colonnes = {"ID", "Date", "Action", "Détails", "Utilisateur"};
                    Map<String, Function<HistoriqueReservation, String>> extracteurs = new LinkedHashMap<>();
                    
                    extracteurs.put("ID", h -> String.valueOf(h.getId()));
                    extracteurs.put("Date", h -> h.getDateAction() != null ? 
                        h.getDateAction().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
                    extracteurs.put("Action", h -> h.getAction());
                    extracteurs.put("Détails", h -> h.getDetails() != null ? h.getDetails() : "");
                    extracteurs.put("Utilisateur", h -> {
                        try {
                            Utilisateur u = utilisateurService.trouverParId(h.getUtilisateurId());
                            return u != null ? u.getPrenom() + " " + u.getNom() : "Inconnu";
                        } catch (SQLException e) {
                            return "Erreur";
                        }
                    });
                    
                    exportService.exporterListeExcel(
                        donnees, 
                        "Historique des réservations - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        colonnes, 
                        extracteurs, 
                        chemin
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
    * Récupère le stage à partir de la scène.
    * @return le stage principal ou null
    */
    protected Stage getStageFromScene() {
        if (primaryStage != null) return primaryStage;
        if (historiqueTable != null && historiqueTable.getScene() != null && historiqueTable.getScene().getWindow() instanceof Stage) {
            return (Stage) historiqueTable.getScene().getWindow();
        }
        return null;
    }
    
    @FXML
    protected void handleRetour() {
        try {
            Stage stage = getStageFromScene();
            if (stage == null) {
                logger.error("Stage null pour retour");
                return;
            }
            
            String fxmlFile;
            if (utilisateurConnecte == null) {
                retourLogin();
                return;
            }
            
            switch (utilisateurConnecte.getRole()) {
                case "admin":
                    fxmlFile = "/fxml/TableauBordAdmin.fxml";
                    break;
                case "gestionnaire":
                    fxmlFile = "/fxml/TableauBordGestionnaire.fxml";
                    break;
                case "enseignant":
                    fxmlFile = "/fxml/TableauBordEnseignant.fxml";
                    break;
                case "etudiant":
                    fxmlFile = "/fxml/TableauBordEtudiant.fxml";
                    break;
                default:
                    fxmlFile = "/fxml/TableauBordAdmin.fxml";
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
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
            
        } catch (IOException e) {
            logger.error("Erreur retour", e);
            afficherErreur("Impossible de retourner au tableau de bord");
        }
    }
}