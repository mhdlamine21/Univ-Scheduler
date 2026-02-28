package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Représente un cours dispensé dans une classe
 */
public class Cours {
    private int id;
    private int matiereId;
    private int enseignantId;
    private int classeId;
    private String typeCours;
    private String groupes;
    private int volumeHoraire;
    private LocalDateTime dateCreation;
    
    public Cours() {}
    
    public Cours(int matiereId, int enseignantId, int classeId, String typeCours, 
                 String groupes, int volumeHoraire) {
        this.matiereId = matiereId;
        this.enseignantId = enseignantId;
        this.classeId = classeId;
        this.typeCours = typeCours;
        this.groupes = groupes;
        this.volumeHoraire = volumeHoraire;
        this.dateCreation = LocalDateTime.now();
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getMatiereId() { return matiereId; }
    public void setMatiereId(int matiereId) { this.matiereId = matiereId; }
    
    public int getEnseignantId() { return enseignantId; }
    public void setEnseignantId(int enseignantId) { this.enseignantId = enseignantId; }
    
    public int getClasseId() { return classeId; }
    public void setClasseId(int classeId) { this.classeId = classeId; }
    
    public String getTypeCours() { return typeCours; }
    public void setTypeCours(String typeCours) { this.typeCours = typeCours; }
    
    public String getGroupes() { return groupes; }
    public void setGroupes(String groupes) { this.groupes = groupes; }
    
    public int getVolumeHoraire() { return volumeHoraire; }
    public void setVolumeHoraire(int volumeHoraire) { this.volumeHoraire = volumeHoraire; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}