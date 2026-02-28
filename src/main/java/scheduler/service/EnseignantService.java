package scheduler.service;

import scheduler.dao.UtilisateurDAO;
import scheduler.modele.Enseignant;
import scheduler.modele.Utilisateur;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service métier pour Enseignant.
 */
public class EnseignantService {
    private UtilisateurDAO utilisateurDAO;
    
    public EnseignantService() {
        this.utilisateurDAO = new UtilisateurDAO();
    }
    
    public Enseignant trouverParId(int id) throws SQLException {
        Utilisateur user = utilisateurDAO.trouverParId(id);
        return (user instanceof Enseignant) ? (Enseignant) user : null;
    }
    
    public List<Enseignant> listerTous() throws SQLException {
        List<Utilisateur> users = utilisateurDAO.listerParRole("enseignant");
        List<Enseignant> enseignants = new ArrayList<>();
        for (Utilisateur u : users) {
            if (u instanceof Enseignant) {
                enseignants.add((Enseignant) u);
            }
        }
        return enseignants;
    }
}