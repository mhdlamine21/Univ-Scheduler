package scheduler.service;

import scheduler.dao.BatimentDAO;
import scheduler.dao.CoursDAO;
import scheduler.dao.SalleDAO;
import scheduler.dao.CreneauDAO;
import scheduler.modele.Batiment;
import scheduler.modele.Creneau;
import scheduler.modele.Cours;
import scheduler.modele.Salle;
import scheduler.modele.Utilisateur;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Service métier pour la gestion des bâtiments.
 */
public class BatimentService {
    
    private BatimentDAO batimentDAO;
    
    public BatimentService() {
        this.batimentDAO = new BatimentDAO();
    }
    
    public void ajouter(Batiment batiment) throws SQLException {
        batimentDAO.ajouter(batiment);
    }
    
    public Batiment trouverParId(int id) throws SQLException {
        return batimentDAO.trouverParId(id);
    }
    
    public List<Batiment> listerTous() throws SQLException {
        return batimentDAO.listerTous();
    }
    
    public List<Batiment> listerParUfr(int ufrId) throws SQLException {
        return batimentDAO.listerParUfr(ufrId);
    }
    
    public void modifier(Batiment batiment) throws SQLException {
        batimentDAO.modifier(batiment);
    }
    
    public void supprimer(int id) throws SQLException {
        batimentDAO.supprimer(id);
    }
    
    public void rendreIndisponible(int id, String motif, String dateDebut, String dateFin) throws SQLException {
        batimentDAO.rendreIndisponible(id, motif, dateDebut, dateFin);
    }
    
    public void rendreDisponible(int id) throws SQLException {
        batimentDAO.rendreDisponible(id);
    }
    
    /**
     * Vérifie les conflits liés à l'indisponibilité d'un bâtiment.
     * @return liste des conflits détectés
     */
    public List<Map<String, Object>> verifierConflitsIndisponibilite(int batimentId,
            String dateDebut, String dateFin) throws SQLException {
        
        List<Map<String, Object>> conflits = new ArrayList<>();
        Batiment batiment = batimentDAO.trouverParId(batimentId);
        if (batiment == null) return conflits;
        
        SalleDAO salleDAO = new SalleDAO();
        CreneauDAO creneauDAO = new CreneauDAO();
        List<Salle> salles = salleDAO.listerParBatiment(batimentId);
        LocalDate debut = LocalDate.parse(dateDebut);
        LocalDate fin = LocalDate.parse(dateFin);
        
        for (Salle salle : salles) {
            List<Creneau> creneaux = creneauDAO.listerParSalle(salle.getId());
            for (Creneau c : creneaux) {
                LocalDate dateCreneau = LocalDate.parse(c.getJour());
                if (!dateCreneau.isBefore(debut) && !dateCreneau.isAfter(fin)) {
                    Map<String, Object> conflit = new HashMap<>();
                    conflit.put("type", "BATIMENT_INDISPONIBLE");
                    conflit.put("description", "Le bâtiment " + batiment.getNom() + 
                        " sera indisponible du " + dateDebut + " au " + dateFin + 
                        " mais la salle " + salle.getNumero() + " a un cours le " + c.getJour());
                    conflit.put("creneau", c);
                    conflit.put("salle", salle);
                    conflit.put("date", c.getJour());
                    conflits.add(conflit);
                }
            }
        }
        
        return conflits;
    }
    
    /**
     * Rend un bâtiment indisponible avec notification automatique des conflits.
     */
    public void rendreIndisponibleAvecNotification(int id, String motif,
            String dateDebut, String dateFin, NotificationService notificationService,
            EmailService emailService, UtilisateurService utilisateurService) throws SQLException {
        
        List<Map<String, Object>> conflits = verifierConflitsIndisponibilite(id, dateDebut, dateFin);
        
        rendreIndisponible(id, motif, dateDebut, dateFin);
        
        for (Map<String, Object> conflit : conflits) {
            Creneau c = (Creneau) conflit.get("creneau");
            Salle s = (Salle) conflit.get("salle");
            if (c != null && s != null) {
                CoursDAO coursDAO = new CoursDAO();
                Cours cours = coursDAO.trouverParId(c.getCoursId());
                if (cours != null && notificationService != null) {
                    Utilisateur enseignant = utilisateurService.trouverParId(cours.getEnseignantId());
                    if (enseignant != null) {
                        notificationService.ajouterNotification(enseignant.getId(),
                            "⚠️ Le bâtiment de votre cours du " + c.getJour() + 
                            " (Salle " + s.getNumero() + ") sera indisponible du " + 
                            dateDebut + " au " + dateFin);
                        
                        if (emailService != null) {
                            emailService.envoyerConflitGestionnaire(
                                enseignant.getEmail(),
                                "Bâtiment indisponible",
                                "Votre cours du " + c.getJour() + " est affecté par l'indisponibilité du bâtiment",
                                s.getNumero(),
                                c.getJour(),
                                c.getHeureDebut(),
                                c.getHeureFin(),
                                enseignant.getPrenom() + " " + enseignant.getNom()
                            );
                        }
                    }
                }
            }
        }
    }
}