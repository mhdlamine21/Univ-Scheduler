package scheduler.modele;

/**
 * Représente un enseignant de l'université
 */
public class Enseignant extends Utilisateur {

    public static final String[] GRADES = {
        "Mr.",
        "Mme.",
        "Dr."
    };

    private String matricule;
    private String grade;

    public Enseignant() {
        super();
        this.role = "enseignant";
        this.grade = null;
    }

    public Enseignant(String nom, String prenom, String email,
                      String motDePasse, String matricule) {
        super(nom, prenom, email, motDePasse, "enseignant");
        this.matricule = matricule;
        this.grade = null;
    }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getAffichageCourt() {
        if (grade != null && !grade.isEmpty()) {
            return grade + " " + getNom();
        }
        return getNom();
    }

    public String getAffichageComplet() {
        if (grade != null && !grade.isEmpty()) {
            return grade + " " + getPrenom() + " " + getNom();
        }
        return getPrenom() + " " + getNom();
    }

    @Override
    public String toString() {
        return getAffichageCourt();
    }
}