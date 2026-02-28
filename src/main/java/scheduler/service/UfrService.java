package scheduler.service;

import scheduler.dao.UfrDAO;
import scheduler.modele.Ufr;
import java.sql.SQLException;
import java.util.List;

/**
 * Service de gestion des UFR (Unités de Formation et de Recherche).
 */
public class UfrService {
    private UfrDAO ufrDAO;
    
    public UfrService() {
        this.ufrDAO = new UfrDAO();
    }
    
    public void ajouter(Ufr ufr) throws SQLException {
        ufrDAO.ajouter(ufr);
    }
    
    public Ufr trouverParId(int id) throws SQLException {
        return ufrDAO.trouverParId(id);
    }
    
    public List<Ufr> listerTous() throws SQLException {
        return ufrDAO.listerTous();
    }
    
    public void modifier(Ufr ufr) throws SQLException {
        ufrDAO.modifier(ufr);
    }
    
    public void supprimer(int id) throws SQLException {
        ufrDAO.supprimer(id);
    }
}