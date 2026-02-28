package scheduler.modele;

/**
 * Représente un gestionnaire d'emplois du temps
 */
public class Gestionnaire extends Utilisateur {
    
    public Gestionnaire() {
        super();
        this.role = "gestionnaire";
    }
    
    public Gestionnaire(String nom, String prenom, String email, String motDePasse) {
        super(nom, prenom, email, motDePasse, "gestionnaire");
    }
}