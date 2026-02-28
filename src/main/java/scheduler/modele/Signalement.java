package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Représente un signalement de problème technique dans une salle
 */
public class Signalement {
    private int id;
    private int utilisateurId;
    private int salleId;
    private String typeProbleme;
    private String description;
    private LocalDateTime dateSignalement;
    private String statut;
    private LocalDateTime dateResolution;
    private String commentaireResolution;
    private String priorite;
    
    public Signalement() {
        this.priorite = "MOYENNE";
    }
    
    public Signalement(int utilisateurId, int salleId, String typeProbleme, String description) {
        this.utilisateurId = utilisateurId;
        this.salleId = salleId;
        this.typeProbleme = typeProbleme;
        this.description = description;
        this.dateSignalement = LocalDateTime.now();
        this.statut = "en_attente";
        this.priorite = "MOYENNE";
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }
    
    public int getSalleId() { return salleId; }
    public void setSalleId(int salleId) { this.salleId = salleId; }
    
    public String getTypeProbleme() { return typeProbleme; }
    public void setTypeProbleme(String typeProbleme) { this.typeProbleme = typeProbleme; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getDateSignalement() { return dateSignalement; }
    public void setDateSignalement(LocalDateTime dateSignalement) { this.dateSignalement = dateSignalement; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    public LocalDateTime getDateResolution() { return dateResolution; }
    public void setDateResolution(LocalDateTime dateResolution) { this.dateResolution = dateResolution; }
    
    public String getCommentaireResolution() { return commentaireResolution; }
    public void setCommentaireResolution(String commentaireResolution) { this.commentaireResolution = commentaireResolution; }
    
    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }
    
    public String getLibellePriorite() {
        switch (priorite) {
            case "HAUTE": return "Haute";
            case "MOYENNE": return "Moyenne";
            case "BASSE": return "Basse";
            default: return priorite;
        }
    }
}