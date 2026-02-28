package scheduler.controleur;

import scheduler.modele.*;
import scheduler.service.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class ResoudreConflitManuelControleur {
    
    @FXML private Label conflitLabel;
    @FXML private Label coursInfoLabel;
    @FXML private ComboBox<String> jourCombo;
    @FXML private ComboBox<String> heureDebutCombo;
    @FXML private Spinner<Integer> dureeSpinner;
    @FXML private ComboBox<String> salleCombo;
    @FXML private Label disponibiliteLabel;
    @FXML private Button verifierButton;
    @FXML private Button appliquerButton;
    @FXML private Button annulerButton;
    @FXML private ProgressIndicator chargementIndicator;
    
    private Creneau creneau;
    private Map<String, Object> conflit;
    private ConflitService conflitService;
    private PlanningService planningService;
    private SalleService salleService;
    private CoursService coursService;
    private UtilisateurService utilisateurService;
    
    private boolean resolu = false;
    
    // ✅ SETTERS
    public void setCreneau(Creneau creneau) {
        this.creneau = creneau;
        chargerInfosCreneau();
    }
    
    public void setConflit(Map<String, Object> conflit) {
        this.conflit = conflit;
    }
    
    public void setConflitService(ConflitService conflitService) {
        this.conflitService = conflitService;
    }
    
    public void setPlanningService(PlanningService planningService) {
        this.planningService = planningService;
    }
    
    public void setSalleService(SalleService salleService) {
        this.salleService = salleService;
    }
    
    public boolean isResolu() {
        return resolu;
    }
    
    @FXML
    private void initialize() {
        // ✅ INITIALISER LES SERVICES
        this.coursService = new CoursService();
        this.utilisateurService = new UtilisateurService();
        if (this.salleService == null) {
            this.salleService = new SalleService();
        }
        if (this.planningService == null) {
            this.planningService = new PlanningService();
        }
        if (this.conflitService == null) {
            this.conflitService = new ConflitService();
        }
        
        configurerHeures();
        configurerJours();
        
        dureeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4, 2));
        
        verifierButton.setOnAction(e -> verifierDisponibilite());
        appliquerButton.setOnAction(e -> appliquerModification());
        annulerButton.setOnAction(e -> fermer(false));
    }

    
    private void chargerInfosCreneau() {
        if (creneau == null) return;
        
        conflitLabel.setText("⚠️ " + conflit.get("description"));
        
        try {
            Cours cours = coursService.trouverParId(creneau.getCoursId());
            if (cours != null) {
                MatiereService matiereService = new MatiereService();
                Matiere matiere = matiereService.trouverParId(cours.getMatiereId());
                String matiereNom = matiere != null ? matiere.getNom() : "Matière " + cours.getMatiereId();
                coursInfoLabel.setText("Cours: " + matiereNom + " (" + cours.getTypeCours() + 
                    ") | Date: " + creneau.getJour() +
                    " | Horaire: " + creneau.getHeureDebut() + " - " + creneau.getHeureFin());
            } else {
                coursInfoLabel.setText("Cours ID: " + creneau.getCoursId() + 
                    " | Date: " + creneau.getJour() +
                    " | Horaire: " + creneau.getHeureDebut() + " - " + creneau.getHeureFin());
            }
        } catch (SQLException e) {
            coursInfoLabel.setText("Cours ID: " + creneau.getCoursId() + 
                " | Date: " + creneau.getJour() +
                " | Horaire: " + creneau.getHeureDebut() + " - " + creneau.getHeureFin());
        }
        
        jourCombo.setValue(creneau.getJour());
        heureDebutCombo.setValue(creneau.getHeureDebut());
        
        int duree = calculerDureeHeures(creneau.getHeureDebut(), creneau.getHeureFin());
        dureeSpinner.getValueFactory().setValue(duree);
        
        if (creneau.getSalleId() != null) {
            try {
                Salle salle = salleService.trouverParId(creneau.getSalleId());
                if (salle != null) {
                    salleCombo.getItems().add(salle.getNumero());
                    salleCombo.setValue(salle.getNumero());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        chargerSallesAlternatives();
    }
    
    private void configurerHeures() {
        for (int h = 8; h <= 17; h++) {
            heureDebutCombo.getItems().add(String.format("%02d:00", h));
        }
    }
    
    private void configurerJours() {
        for (int i = 0; i < 14; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            jourCombo.getItems().add(date.toString());
        }
    }
    
    private void chargerSallesAlternatives() {
        if (creneau == null) return;
        
        try {
            List<Salle> salles = salleService.listerTous();
            for (Salle s : salles) {
                if ("disponible".equals(s.getStatut()) && !salleCombo.getItems().contains(s.getNumero())) {
                    salleCombo.getItems().add(s.getNumero());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void verifierDisponibilite() {
        String date = jourCombo.getValue();
        String heureDebut = heureDebutCombo.getValue();
        int duree = dureeSpinner.getValue();
        String heureFin = calculerHeureFin(heureDebut, duree);
        String salleNumero = salleCombo.getValue();
        
        if (date == null || heureDebut == null || salleNumero == null) {
            disponibiliteLabel.setText("❌ Remplissez tous les champs");
            return;
        }
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Trouver la salle
                    Salle salle = null;
                    List<Salle> salles = salleService.listerTous();
                    for (Salle s : salles) {
                        if (s.getNumero().equals(salleNumero)) {
                            salle = s;
                            break;
                        }
                    }
                    
                    if (salle == null) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                disponibiliteLabel.setText("❌ Salle non trouvée");
                                chargementIndicator.setVisible(false);
                            }
                        });
                        return;
                    }
                    
                    // Vérifier disponibilité de la salle
                    boolean salleLibre = planningService.salleEstDisponible(salle.getId(), date, heureDebut, heureFin);
                    
                    // Vérifier disponibilité de l'enseignant
                    boolean enseignantLibre = true;
                    try {
                        Cours cours = coursService.trouverParId(creneau.getCoursId());
                        if (cours != null) {
                            enseignantLibre = planningService.enseignantEstDisponible(cours.getEnseignantId(), date, heureDebut, heureFin);
                        }
                    } catch (SQLException e) {
                        // Ignorer
                    }
                    
                    // Vérifier disponibilité de la classe
                    boolean classeLibre = true;
                    try {
                        Cours cours = coursService.trouverParId(creneau.getCoursId());
                        if (cours != null) {
                            ClasseService classeService = new ClasseService();
                            Classe classe = classeService.trouverParId(cours.getClasseId());
                            if (classe != null) {
                                // Vérifier si la classe a un autre cours à ce moment
                                List<Creneau> creneauxJour = planningService.getCreneauxParJour(date);
                                for (Creneau c : creneauxJour) {
                                    if (c.getId() == creneau.getId()) continue;
                                    Cours autreCours = coursService.trouverParId(c.getCoursId());
                                    if (autreCours != null && autreCours.getClasseId() == cours.getClasseId()) {
                                        if (seChevauchent(heureDebut, heureFin, c.getHeureDebut(), c.getHeureFin())) {
                                            classeLibre = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        // Ignorer
                    }
                    
                    final boolean salleOk = salleLibre;
                    final boolean enseignantOk = enseignantLibre;
                    final boolean classeOk = classeLibre;
                    
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            chargementIndicator.setVisible(false);
                            if (salleOk && enseignantOk && classeOk) {
                                disponibiliteLabel.setText("✅ Créneau disponible !");
                                disponibiliteLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                                appliquerButton.setDisable(false);
                            } else {
                                StringBuilder msg = new StringBuilder("❌ ");
                                if (!salleOk) msg.append("Salle occupée ");
                                if (!enseignantOk) msg.append("Enseignant occupé ");
                                if (!classeOk) msg.append("Classe occupée ");
                                disponibiliteLabel.setText(msg.toString());
                                disponibiliteLabel.setStyle("-fx-text-fill: #f44336;");
                                appliquerButton.setDisable(true);
                            }
                        }
                    });
                    
                } catch (SQLException e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            chargementIndicator.setVisible(false);
                            disponibiliteLabel.setText("❌ Erreur: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
    
    private boolean seChevauchent(String d1, String f1, String d2, String f2) {
        try {
            String[] debut1 = d1.split(":");
            String[] fin1 = f1.split(":");
            String[] debut2 = d2.split(":");
            String[] fin2 = f2.split(":");
            
            int debut1Min = Integer.parseInt(debut1[0]) * 60 + Integer.parseInt(debut1[1]);
            int fin1Min = Integer.parseInt(fin1[0]) * 60 + Integer.parseInt(fin1[1]);
            int debut2Min = Integer.parseInt(debut2[0]) * 60 + Integer.parseInt(debut2[1]);
            int fin2Min = Integer.parseInt(fin2[0]) * 60 + Integer.parseInt(fin2[1]);
            
            return debut1Min < fin2Min && debut2Min < fin1Min;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void appliquerModification() {
        String date = jourCombo.getValue();
        String heureDebut = heureDebutCombo.getValue();
        int duree = dureeSpinner.getValue();
        String heureFin = calculerHeureFin(heureDebut, duree);
        String salleNumero = salleCombo.getValue();
        
        if (chargementIndicator != null) chargementIndicator.setVisible(true);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Trouver la salle
                    Salle salle = null;
                    List<Salle> salles = salleService.listerTous();
                    for (Salle s : salles) {
                        if (s.getNumero().equals(salleNumero)) {
                            salle = s;
                            break;
                        }
                    }
                    
                    // Modifier le créneau
                    creneau.setJour(date);
                    creneau.setHeureDebut(heureDebut);
                    creneau.setHeureFin(heureFin);
                    if (salle != null) {
                        creneau.setSalleId(salle.getId());
                    }
                    
                    planningService.modifierCreneau(creneau);
                    
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            chargementIndicator.setVisible(false);
                            resolu = true;
                            fermer(true);
                        }
                    });
                    
                } catch (SQLException e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            chargementIndicator.setVisible(false);
                            disponibiliteLabel.setText("❌ Erreur: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
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
    
    private void fermer(boolean ok) {
        Stage stage = (Stage) annulerButton.getScene().getWindow();
        stage.close();
    }
}