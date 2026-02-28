package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import scheduler.dao.HistoriqueReservationDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.ToggleGroup;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ResoudreConflitReservationControleur {
    
    @FXML private Label conflitLabel;
    @FXML private Label reservationInfoLabel;
    @FXML private ComboBox<String> jourCombo;
    @FXML private ComboBox<String> heureDebutCombo;
    @FXML private Spinner<Integer> dureeSpinner;
    @FXML private ComboBox<String> salleCombo;
    @FXML private Label disponibiliteLabel;
    @FXML private RadioButton annulerRadio;
    @FXML private RadioButton modifierRadio;
    @FXML private RadioButton garderRadio;
    @FXML private ToggleGroup actionGroup;  
    @FXML private VBox modificationPane;    
    @FXML private Button appliquerButton;
    @FXML private Button annulerButton;
    @FXML private ProgressIndicator chargementIndicator;
    
    private Reservation reservation;
    private Map<String, Object> conflit;
    private ReservationService reservationService;
    private SalleService salleService;
    private PlanningService planningService;
    private EmailService emailService;
    private UtilisateurService utilisateurService;
    
    private boolean resolu = false;
    
    // SETTERS
    
    // ✅ IMPORTANT : setConflit doit être appelé AVANT setReservation
    public void setConflit(Map<String, Object> conflit) {
        this.conflit = conflit;
    }
    
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        chargerInfosReservation();
    }
    
    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }
    
    public void setSalleService(SalleService salleService) {
        this.salleService = salleService;
    }
    
    public void setPlanningService(PlanningService planningService) {
        this.planningService = planningService;
    }
    
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    public void setUtilisateurService(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }
    
    public boolean isResolu() {
        return resolu;
    }
    
    // INITIALISATION
    
    @FXML
    private void initialize() {
        // Initialiser les services si null
        if (this.salleService == null) {
            this.salleService = new SalleService();
        }
        if (this.planningService == null) {
            this.planningService = new PlanningService();
        }
        if (this.reservationService == null) {
            this.reservationService = new ReservationService();
        }
        if (this.emailService == null) {
            this.emailService = new EmailService();
        }
        if (this.utilisateurService == null) {
            this.utilisateurService = new UtilisateurService();
        }
        
        configurerHeures();
        configurerJours();
        
        dureeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 2));
        
        actionGroup = new ToggleGroup();
        garderRadio.setToggleGroup(actionGroup);
        modifierRadio.setToggleGroup(actionGroup);
        annulerRadio.setToggleGroup(actionGroup);
        garderRadio.setSelected(true);
        
        actionGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            modificationPane.setVisible(newVal == modifierRadio);
            modificationPane.setManaged(newVal == modifierRadio);
        });
        
        appliquerButton.setOnAction(e -> appliquer());
        annulerButton.setOnAction(e -> fermer(false));
        
        chargerSalles();
    }
    
    private void chargerInfosReservation() {
        // ✅ VÉRIFIER QUE conflit N'EST PAS NULL
        if (conflit == null) {
            System.err.println("❌ conflit est null dans chargerInfosReservation()");
            conflitLabel.setText("⚠️ Erreur: impossible de charger les détails du conflit");
            return;
        }
        
        if (reservation == null) {
            System.err.println("❌ reservation est null dans chargerInfosReservation()");
            return;
        }
        
        String description = (String) conflit.get("description");
        conflitLabel.setText("⚠️ " + (description != null ? description : "Conflit non spécifié"));
        
        reservationInfoLabel.setText("Réservation #" + reservation.getId() + 
            " | Salle #" + reservation.getSalleId() +
            " | Date: " + formaterDate(reservation.getDateReservation()) +
            " | Horaire: " + reservation.getHeureDebut() + " - " + reservation.getHeureFin());
        
        jourCombo.setValue(reservation.getDateReservation());
        heureDebutCombo.setValue(reservation.getHeureDebut());
        
        int duree = calculerDureeHeures(reservation.getHeureDebut(), reservation.getHeureFin());
        dureeSpinner.getValueFactory().setValue(duree);
        
        try {
            Salle salle = salleService.trouverParId(reservation.getSalleId());
            if (salle != null) {
                if (!salleCombo.getItems().contains(salle.getNumero())) {
                    salleCombo.getItems().add(salle.getNumero());
                }
                salleCombo.setValue(salle.getNumero());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void configurerHeures() {
        for (int h = 8; h <= 20; h++) {
            heureDebutCombo.getItems().add(String.format("%02d:00", h));
        }
    }
    
    private void configurerJours() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 30; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            jourCombo.getItems().add(date.format(formatter));
        }
    }
    
    private void chargerSalles() {
        if (salleService == null) {
            System.err.println("❌ salleService est null");
            return;
        }
        
        try {
            List<Salle> salles = salleService.listerTous();
            salleCombo.getItems().clear();
            for (Salle s : salles) {
                if ("disponible".equals(s.getStatut())) {
                    salleCombo.getItems().add(s.getNumero());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // RÉSOLUTION
    
    private void appliquer() {
        RadioButton selected = (RadioButton) actionGroup.getSelectedToggle();
        
        if (selected == annulerRadio) {
            chargementIndicator.setVisible(true);
            
            new Thread(() -> {
                try {
                    reservationService.annulerReservation(reservation.getId());
                    
                    HistoriqueReservationDAO historiqueDAO = new HistoriqueReservationDAO();
                    HistoriqueReservation historique = new HistoriqueReservation(
                        reservation.getId(),
                        "CONFLIT_RESOLU_ANNULE",
                        1,
                        "Conflit résolu : Réservation annulée"
                    );
                    historiqueDAO.ajouter(historique);
                    
                    javafx.application.Platform.runLater(() -> {
                        chargementIndicator.setVisible(false);
                        resolu = true;
                        fermer(true);
                    });
                    
                } catch (SQLException e) {
                    javafx.application.Platform.runLater(() -> {
                        chargementIndicator.setVisible(false);
                        disponibiliteLabel.setText("❌ Erreur: " + e.getMessage());
                    });
                }
            }).start();
            
        } else if (selected == modifierRadio) {
            String date = jourCombo.getValue();
            String heureDebut = heureDebutCombo.getValue();
            int duree = dureeSpinner.getValue();
            String heureFin = calculerHeureFin(heureDebut, duree);
            String salleNumero = salleCombo.getValue();
            
            if (date == null || heureDebut == null || salleNumero == null) {
                disponibiliteLabel.setText("❌ Remplissez tous les champs");
                return;
            }
            
            chargementIndicator.setVisible(true);
            
            new Thread(() -> {
                try {
                    Salle salle = null;
                    List<Salle> salles = salleService.listerTous();
                    for (Salle s : salles) {
                        if (s.getNumero().equals(salleNumero)) {
                            salle = s;
                            break;
                        }
                    }
                    
                    if (salle == null) {
                        javafx.application.Platform.runLater(() -> {
                            chargementIndicator.setVisible(false);
                            disponibiliteLabel.setText("❌ Salle non trouvée");
                        });
                        return;
                    }
                    
                    reservation.setDateReservation(date);
                    reservation.setHeureDebut(heureDebut);
                    reservation.setHeureFin(heureFin);
                    reservation.setSalleId(salle.getId());
                    reservationService.modifier(reservation);
                    
                    HistoriqueReservationDAO historiqueDAO = new HistoriqueReservationDAO();
                    HistoriqueReservation historique = new HistoriqueReservation(
                        reservation.getId(),
                        "CONFLIT_RESOLU_MODIFIE",
                        1,
                        "Conflit résolu : " + date + " " + heureDebut + "-" + heureFin + " Salle " + salle.getNumero()
                    );
                    historiqueDAO.ajouter(historique);
                    
                    Utilisateur utilisateur = utilisateurService.trouverParId(reservation.getUtilisateurId());
                    if (utilisateur != null && emailService != null) {
                        emailService.envoyerResolutionConflit(
                            utilisateur.getEmail(),
                            utilisateur.getPrenom() + " " + utilisateur.getNom(),
                            "Réservation",
                            "Créneau modifié",
                            salle.getNumero(),
                            date,
                            heureDebut,
                            heureFin
                        );
                    }
                    
                    javafx.application.Platform.runLater(() -> {
                        chargementIndicator.setVisible(false);
                        resolu = true;
                        fermer(true);
                    });
                    
                } catch (SQLException e) {
                    javafx.application.Platform.runLater(() -> {
                        chargementIndicator.setVisible(false);
                        disponibiliteLabel.setText("❌ Erreur: " + e.getMessage());
                    });
                }
            }).start();
            
        } else {
            resolu = false;
            fermer(false);
        }
    }
    
    private int calculerDureeHeures(String debut, String fin) {
        try {
            String[] d = debut.split(":");
            String[] f = fin.split(":");
            int debutMin = Integer.parseInt(d[0]) * 60 + Integer.parseInt(d[1]);
            int finMin = Integer.parseInt(f[0]) * 60 + Integer.parseInt(f[1]);
            return (finMin - debutMin) / 60;
        } catch (Exception e) {
            return 2;
        }
    }
    
    private String calculerHeureFin(String debut, int duree) {
        String[] parts = debut.split(":");
        int heure = Integer.parseInt(parts[0]) + duree;
        return String.format("%02d:00", heure);
    }
    
    private String formaterDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    private void fermer(boolean ok) {
        Stage stage = (Stage) annulerButton.getScene().getWindow();
        stage.close();
    }
}