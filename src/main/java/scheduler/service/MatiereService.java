package scheduler.service;

import scheduler.dao.MatiereDAO;
import scheduler.modele.Matiere;
import java.sql.SQLException;
import java.util.List;

/**
 * Service métier pour Matiere.
 */
public class MatiereService {
    private MatiereDAO matiereDAO;
    
    public MatiereService() {
        this.matiereDAO = new MatiereDAO();
    }
    
    public void ajouter(Matiere matiere) throws SQLException {
        matiereDAO.ajouter(matiere);
    }
    
    public Matiere trouverParId(int id) throws SQLException {
        return matiereDAO.trouverParId(id);
    }
    
    public Matiere trouverParCode(String code) throws SQLException {
        return matiereDAO.trouverParCode(code);
    }
    
    public List<Matiere> listerToutes() throws SQLException {
        return matiereDAO.listerToutes();
    }
    
    public List<Matiere> listerParFiliere(String filiere) throws SQLException {
        return matiereDAO.listerParFiliere(filiere);
    }
    
    public void modifier(Matiere matiere) throws SQLException {
        matiereDAO.modifier(matiere);
    }
    
    public void supprimer(int id) throws SQLException {
        matiereDAO.supprimer(id);
    }
}