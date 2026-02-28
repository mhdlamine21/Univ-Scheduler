package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Représente une matière enseignée
 */
public class Matiere {
    private int id;
    private String nom;
    private String code;
    private String filiere;
    private int volumeHoraire;
    private String description;
    private LocalDateTime dateCreation;
    
    public Matiere() {}
    
    public Matiere(String nom, String code, String filiere, int volumeHoraire, String description) {
        this.nom = nom;
        this.code = code;
        this.filiere = filiere;
        this.volumeHoraire = volumeHoraire;
        this.description = description;
        this.dateCreation = LocalDateTime.now();
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getFiliere() { return filiere; }
    public void setFiliere(String filiere) { this.filiere = filiere; }
    
    public int getVolumeHoraire() { return volumeHoraire; }
    public void setVolumeHoraire(int volumeHoraire) { this.volumeHoraire = volumeHoraire; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}