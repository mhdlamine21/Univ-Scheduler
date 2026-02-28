package scheduler.modele;

/**
 * Représente un bâtiment de l'université
 */
public class Batiment {
    private int id;
    private String nom;
    private String localisation;
    private int nbEtages;
    private int ufrId;
    private String statut;
    private String motifIndisponibilite;
    private String dateDebutIndisponibilite;
    private String dateFinIndisponibilite;
    private String photoUrl;
    
    public Batiment() {}
    
    public Batiment(String nom, String localisation, int nbEtages, int ufrId) {
        this.nom = nom;
        this.localisation = localisation;
        this.nbEtages = nbEtages;
        this.ufrId = ufrId;
        this.statut = "disponible";
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
    
    public int getNbEtages() { return nbEtages; }
    public void setNbEtages(int nbEtages) { this.nbEtages = nbEtages; }
    
    public int getUfrId() { return ufrId; }
    public void setUfrId(int ufrId) { this.ufrId = ufrId; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    public String getMotifIndisponibilite() { return motifIndisponibilite; }
    public void setMotifIndisponibilite(String motifIndisponibilite) { this.motifIndisponibilite = motifIndisponibilite; }
    
    public String getDateDebutIndisponibilite() { return dateDebutIndisponibilite; }
    public void setDateDebutIndisponibilite(String dateDebutIndisponibilite) { this.dateDebutIndisponibilite = dateDebutIndisponibilite; }
    
    public String getDateFinIndisponibilite() { return dateFinIndisponibilite; }
    public void setDateFinIndisponibilite(String dateFinIndisponibilite) { this.dateFinIndisponibilite = dateFinIndisponibilite; }
    
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}