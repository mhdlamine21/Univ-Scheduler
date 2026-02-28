package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Représente une classe ou promotion d'étudiants
 */
public class Classe {
    private int id;
    private String intitule;
    private String filiere;
    private String niveau;
    private String anneeScolaire;
    private int ufrId;
    private int effectif;
    private int nbGroupes;
    private boolean estActive;
    private LocalDateTime dateCreation;
    
    public Classe() {}
    
    public Classe(String intitule, String filiere, String niveau, String anneeScolaire, 
                  int ufrId, int effectif, int nbGroupes) {
        this.intitule = intitule;
        this.filiere = filiere;
        this.niveau = niveau;
        this.anneeScolaire = anneeScolaire;
        this.ufrId = ufrId;
        this.effectif = effectif;
        this.nbGroupes = nbGroupes;
        this.estActive = true;
        this.dateCreation = LocalDateTime.now();
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }
    
    public String getFiliere() { return filiere; }
    public void setFiliere(String filiere) { this.filiere = filiere; }
    
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    
    public String getAnneeScolaire() { return anneeScolaire; }
    public void setAnneeScolaire(String anneeScolaire) { this.anneeScolaire = anneeScolaire; }
    
    public int getUfrId() { return ufrId; }
    public void setUfrId(int ufrId) { this.ufrId = ufrId; }
    
    public int getEffectif() { return effectif; }
    public void setEffectif(int effectif) { this.effectif = effectif; }
    
    public int getNbGroupes() { return nbGroupes; }
    public void setNbGroupes(int nbGroupes) { this.nbGroupes = nbGroupes; }
    
    public boolean isEstActive() { return estActive; }
    public void setEstActive(boolean estActive) { this.estActive = estActive; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}