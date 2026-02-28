package scheduler.modele;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Représente un emploi du temps pour une classe sur une période donnée
 */
public class EmploiDuTemps {
    private int id;
    private int classeId;
    private String periodeType;
    private String periodeDebut;
    private String periodeFin;
    private LocalDateTime dateCreation;
    private boolean estValide;
    private LocalDateTime dateValidation;
    private String statut;
    private String heuresConfig;
    private String joursConfig;
    
    public EmploiDuTemps() {
        this.statut = "en_attente";
        this.heuresConfig = "8:2:18";
        this.joursConfig = "Lundi,Mardi,Mercredi,Jeudi,Vendredi";
    }
    
    public EmploiDuTemps(int classeId, String periodeType, String periodeDebut, String periodeFin) {
        this.classeId = classeId;
        this.periodeType = periodeType;
        this.periodeDebut = periodeDebut;
        this.periodeFin = periodeFin;
        this.dateCreation = LocalDateTime.now();
        this.estValide = false;
        this.statut = "en_attente";
        this.heuresConfig = "8:2:18";
        this.joursConfig = "Lundi,Mardi,Mercredi,Jeudi,Vendredi";
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getClasseId() { return classeId; }
    public void setClasseId(int classeId) { this.classeId = classeId; }
    
    public String getPeriodeType() { return periodeType; }
    public void setPeriodeType(String periodeType) { this.periodeType = periodeType; }
    
    public String getPeriodeDebut() { return periodeDebut; }
    public void setPeriodeDebut(String periodeDebut) { this.periodeDebut = periodeDebut; }
    
    public String getPeriodeFin() { return periodeFin; }
    public void setPeriodeFin(String periodeFin) { this.periodeFin = periodeFin; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    
    public boolean isEstValide() { return estValide; }
    public void setEstValide(boolean estValide) { this.estValide = estValide; }
    
    public LocalDateTime getDateValidation() { return dateValidation; }
    public void setDateValidation(LocalDateTime dateValidation) { this.dateValidation = dateValidation; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    public String getHeuresConfig() { return heuresConfig; }
    public void setHeuresConfig(String heuresConfig) { this.heuresConfig = heuresConfig; }
    
    public String getJoursConfig() { return joursConfig; }
    public void setJoursConfig(String joursConfig) { this.joursConfig = joursConfig; }
    
    public int getHeureDebutConfig() {
        try {
            String[] parts = heuresConfig.split(":");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 8;
        }
    }
    
    public int getEcartConfig() {
        try {
            String heuresConfig = this.heuresConfig;
            if (heuresConfig == null || heuresConfig.isEmpty()) {
                return 2;
            }
            String[] parts = heuresConfig.split(":");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
            return 2;
        } catch (Exception e) {
            return 2;
        }
    }
    
    public int getHeureFinConfig() {
        try {
            String[] parts = heuresConfig.split(":");
            return Integer.parseInt(parts[2]);
        } catch (Exception e) {
            return 18;
        }
    }
    
    public List<String> getJoursConfigList() {
        if (joursConfig == null || joursConfig.isEmpty()) {
            return Arrays.asList("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi");
        }
        return Arrays.asList(joursConfig.split(","));
    }
    
    public boolean estTermine() {
        if (periodeFin == null) return false;
        try {
            LocalDate aujourdhui = LocalDate.now();
            LocalDate fin = LocalDate.parse(periodeFin);
            return aujourdhui.isAfter(fin);
        } catch (Exception e) {
            return false;
        }
    }
    
    public void mettreAJourStatut() {
        if (estTermine()) {
            this.statut = "termine";
        } else if (estValide) {
            this.statut = "valide";
        } else {
            this.statut = "en_attente";
        }
    }
    
    @Override
    public String toString() {
        return "EmploiDuTemps{" +
                "id=" + id +
                ", classeId=" + classeId +
                ", periode='" + periodeDebut + " - " + periodeFin + '\'' +
                ", statut='" + statut + '\'' +
                ", config='" + heuresConfig + " | " + joursConfig + '\'' +
                '}';
    }
}