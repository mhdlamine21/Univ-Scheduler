package scheduler.modele;

/**
 * Représente un administrateur du système
 */
public class Administrateur extends Utilisateur {
    
    public Administrateur() {
        super();
        this.role = "admin";
    }
    
    public Administrateur(String nom, String prenom, String email, String motDePasse) {
        super(nom, prenom, email, motDePasse, "admin");
    }
}