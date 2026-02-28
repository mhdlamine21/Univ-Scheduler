package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Représente une Unité de Formation et de Recherche
 */
public class Ufr {
    private int id;
    private String nom;
    private String code;
    private String description;
    private String photoUrl;
    private String doyen;
    private String email;
    private String localisation;
    private LocalDateTime dateCreation;
    
    public Ufr() {}
    
    public Ufr(String nom, String description) {
        this.nom = nom;
        this.description = description;
        this.dateCreation = LocalDateTime.now();
    }

    public Ufr(String nom, String code, String description, String photoUrl) {
        this.nom = nom;
        this.code = code;
        this.description = description;
        this.photoUrl = photoUrl;
        this.dateCreation = LocalDateTime.now();
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getDoyen() { return doyen; }
    public void setDoyen(String doyen) { this.doyen = doyen; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}