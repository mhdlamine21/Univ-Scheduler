package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Classe abstraite représentant un utilisateur du système
 */
public abstract class Utilisateur {
    protected int id;
    protected String nom;
    protected String prenom;
    protected String email;
    protected String motDePasse;
    protected String role;
    protected LocalDateTime dateCreation;
    protected boolean estValide;
    protected LocalDateTime dateValidation;
    
    public Utilisateur() {}
    
    public Utilisateur(String nom, String prenom, String email, String motDePasse, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.dateCreation = LocalDateTime.now();
        this.estValide = false;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    
    public boolean isEstValide() { return estValide; }
    public void setEstValide(boolean estValide) { this.estValide = estValide; }
    
    public LocalDateTime getDateValidation() { return dateValidation; }
    public void setDateValidation(LocalDateTime dateValidation) { this.dateValidation = dateValidation; }
    
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public boolean isResponsableClasse() {
        if (this instanceof Etudiant) {
            return ((Etudiant) this).estResponsable();
        }
        return false;
    }

    /**
     * Seuls l'administrateur, le gestionnaire, l'enseignant et l'étudiant responsable de classe peuvent réserver des salles.
     */
    public boolean peutReserver() {
        if (role == null) return false;
        String r = role.toLowerCase().trim();
        if ("admin".equals(r) || "administrateur".equals(r) || "gestionnaire".equals(r) || "enseignant".equals(r)) {
            return true;
        }
        if ("etudiant".equals(r)) {
            return isResponsableClasse();
        }
        return false;
    }
}