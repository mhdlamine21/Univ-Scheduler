package scheduler.service;

import scheduler.dao.CoursDAO;
import scheduler.dao.MatiereDAO;
import scheduler.modele.Cours;
import scheduler.modele.Matiere;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Service métier pour la gestion des cours.
 */
public class CoursService {
    
    private CoursDAO coursDAO;
    private MatiereDAO matiereDAO;
    
    public CoursService() {
        this.coursDAO = new CoursDAO();
        this.matiereDAO = new MatiereDAO();
    }
    
    public void ajouter(Cours cours) throws SQLException {
        coursDAO.ajouter(cours);
    }
    
    public Cours trouverParId(int id) throws SQLException {
        return coursDAO.trouverParId(id);
    }
    
    public List<Cours> listerTous() throws SQLException {
        return coursDAO.listerTous();
    }
    
    public List<Cours> listerParClasse(int classeId) throws SQLException {
        return coursDAO.listerParClasse(classeId);
    }
    
    public List<Cours> listerParEnseignant(int enseignantId) throws SQLException {
        return coursDAO.listerParEnseignant(enseignantId);
    }
    
    public void modifier(Cours cours) throws SQLException {
        coursDAO.modifier(cours);
    }
    
    public void supprimer(int id) throws SQLException {
        coursDAO.supprimer(id);
    }
    
    /**
     * Récupère les cours avec les détails de la matière associée.
     */
    public List<Map<String, Object>> getCoursAvecDetails(int classeId) throws SQLException {
        List<Cours> coursList = listerParClasse(classeId);
        List<Map<String, Object>> resultats = new ArrayList<>();
        
        for (Cours cours : coursList) {
            Map<String, Object> details = new HashMap<>();
            details.put("cours", cours);
            
            Matiere matiere = matiereDAO.trouverParId(cours.getMatiereId());
            if (matiere != null) {
                details.put("matiereNom", matiere.getNom());
                details.put("matiereCode", matiere.getCode());
            } else {
                details.put("matiereNom", "Matière inconnue");
            }
            
            resultats.add(details);
        }
        
        return resultats;
    }
    
    /**
     * Retourne le libellé complet d'un cours pour affichage.
     */
    public String getLibelleCours(Cours cours) throws SQLException {
        Matiere matiere = matiereDAO.trouverParId(cours.getMatiereId());
        String matiereNom = (matiere != null) ? matiere.getNom() : "Matière " + cours.getMatiereId();
        
        return String.format("%s - %s", cours.getTypeCours(), matiereNom);
    }
    
    public boolean existe(int id) throws SQLException {
        return trouverParId(id) != null;
    }
    
    /**
     * Retourne les statistiques des cours par type (CM, TD, TP).
     */
    public Map<String, Integer> getStatistiquesParType() throws SQLException {
        List<Cours> tous = listerTous();
        Map<String, Integer> stats = new HashMap<>();
        
        for (Cours c : tous) {
            String type = c.getTypeCours();
            stats.put(type, stats.getOrDefault(type, 0) + 1);
        }
        
        return stats;
    }
    
    public List<Cours> listerParMatiere(int matiereId) throws SQLException {
        List<Cours> resultats = new ArrayList<>();
        List<Cours> tous = listerTous();
        
        for (Cours c : tous) {
            if (c.getMatiereId() == matiereId) {
                resultats.add(c);
            }
        }
        
        return resultats;
    }
    
    /**
     * Calcule le volume horaire total pour une classe.
     */
    public int getVolumeHoraireTotal(int classeId) throws SQLException {
        List<Cours> coursList = listerParClasse(classeId);
        int total = 0;
        
        for (Cours c : coursList) {
            total += c.getVolumeHoraire();
        }
        
        return total;
    }
    
    public boolean enseignantACours(int enseignantId) throws SQLException {
        return !listerParEnseignant(enseignantId).isEmpty();
    }
    
    public boolean classeACours(int classeId) throws SQLException {
        return !listerParClasse(classeId).isEmpty();
    }
}