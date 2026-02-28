package scheduler.modele;

/**
 * Représente un équipement disponible dans les salles
 */
public class Equipement {
    private int id;
    private String nom;
    private String description;
    private int quantite;
    
    public Equipement() {
        this.quantite = 1;
    }
    
    public Equipement(String nom, String description) {
        this.nom = nom;
        this.description = description;
        this.quantite = 1;
    }
    
    public Equipement(String nom, String description, int quantite) {
        this.nom = nom;
        this.description = description;
        setQuantite(quantite);
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getQuantite() { return quantite; }
    
    public void setQuantite(int quantite) {
        if (quantite < 0) {
            throw new IllegalArgumentException("La quantité ne peut pas être négative");
        }
        this.quantite = quantite;
    }
    
    public boolean enStock() {
        return quantite > 0;
    }
    
    public boolean enRupture() {
        return quantite <= 0;
    }
    
    public boolean stockFaible() {
        return quantite > 0 && quantite <= 3;
    }
    
    public String getDescriptionComplete() {
        String statut = "";
        if (enRupture()) {
            statut = " (RUPTURE)";
        } else if (stockFaible()) {
            statut = " (Stock faible)";
        }
        return nom + " - " + quantite + " unité(s)" + statut;
    }
    
    @Override
    public String toString() {
        return "Equipement{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", quantite=" + quantite +
                '}';
    }
}