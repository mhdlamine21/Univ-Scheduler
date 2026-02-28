package scheduler.service;

import scheduler.dao.ClasseDAO;
import scheduler.modele.Classe;
import java.sql.SQLException;
import java.util.List;

/**
 * Service métier pour la gestion des classes.
 */
public class ClasseService {
    private ClasseDAO classeDAO;
    
    public ClasseService() {
        this.classeDAO = new ClasseDAO();
    }
    
    public void ajouter(Classe classe) throws SQLException {
        classeDAO.ajouter(classe);
    }
    
    public Classe trouverParId(int id) throws SQLException {
        return classeDAO.trouverParId(id);
    }
    
    public List<Classe> listerToutes() throws SQLException {
        return classeDAO.listerToutes();
    }
    
    public List<Classe> listerParUfr(int ufrId) throws SQLException {
        return classeDAO.listerParUfr(ufrId);
    }
    
    public List<Classe> listerParFiliere(String filiere) throws SQLException {
        return classeDAO.listerParFiliere(filiere);
    }
    
    public List<String> listerFilieres() throws SQLException {
        return classeDAO.listerFilieres();
    }
    
    public void modifier(Classe classe) throws SQLException {
        classeDAO.modifier(classe);
    }
    
    public void archiver(int id) throws SQLException {
        classeDAO.archiver(id);
    }
    
    public void supprimer(int id) throws SQLException {
        classeDAO.supprimer(id);
    }
}