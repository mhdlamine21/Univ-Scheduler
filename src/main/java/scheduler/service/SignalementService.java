package scheduler.service;

import scheduler.dao.*;
import scheduler.modele.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de gestion des signalements techniques.
 */
public class SignalementService {
    private SignalementDAO signalementDAO;
    private SalleDAO salleDAO;
    private NotificationService notificationService;
    
    public SignalementService() {
        this.signalementDAO = new SignalementDAO();
        this.salleDAO = new SalleDAO();
        this.notificationService = new NotificationService();
    }
    
    public void signaler(Signalement signalement) throws SQLException {
        signalementDAO.ajouter(signalement);
    }
    
    public List<Signalement> listerTous() throws SQLException {
        return signalementDAO.listerTous();
    }
    
    public List<Signalement> listerEnAttente() throws SQLException {
        return signalementDAO.listerParStatut("en_attente");
    }
    
    public List<Signalement> listerParSalle(int salleId) throws SQLException {
        return signalementDAO.listerParSalle(salleId);
    }
    
    public void prendreEnCharge(int id) throws SQLException {
        signalementDAO.mettreEnCours(id);
    }
    
    public void resoudre(int id, String commentaire) throws SQLException {
        signalementDAO.resoudre(id, commentaire);
        
        Signalement signalement = signalementDAO.trouverParId(id);
        if (signalement != null) {
            notificationService.notifierSignalementResolu(signalement);
        }
    }
    
    public void modifier(Signalement signalement) throws SQLException {
        signalementDAO.modifier(signalement);
    }
    
    public void supprimer(int id) throws SQLException {
        signalementDAO.supprimer(id);
    }
    
    /**
     * Compte les signalements non traités.
     */
    public int compterNonTraites() throws SQLException {
        return signalementDAO.compterNonTraites();
    }
    
    public List<Signalement> listerNonTraites() throws SQLException {
        return signalementDAO.listerParStatut("en_attente");
    }
    
    /**
     * Récupère les statistiques des signalements
     */
    public Map<String, Object> getStatistiques() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("nonTraites", compterNonTraites());
        stats.put("parMois", signalementDAO.getStatistiquesParMois());
        stats.put("parType", signalementDAO.getStatistiquesParType());
        stats.put("topSalles", signalementDAO.getTopSallesAvecProblemes());
        stats.put("tempsMoyenResolution", signalementDAO.getTempsMoyenResolution());
        
        return stats;
    }
    
    /**
     * Met à jour la priorité d'un signalement
     */
    public void updatePriorite(int id, String priorite) throws SQLException {
        signalementDAO.updatePriorite(id, priorite);
    }
    
    /**
     * Notifie l'enseignant de la résolution de son signalement.
     */
    public void notifierEnseignantResolution(int signalementId) throws SQLException {
        Signalement signalement = signalementDAO.trouverParId(signalementId);
        if (signalement == null) return;
        
        Utilisateur enseignant = new UtilisateurDAO().trouverParId(signalement.getUtilisateurId());
        if (enseignant == null) return;
        
        Salle salle = new SalleDAO().trouverParId(signalement.getSalleId());
        String salleNom = salle != null ? salle.getNumero() : "Salle " + signalement.getSalleId();
        
        EmailService emailService = new EmailService();
        NotificationService notificationService = new NotificationService();
        
        String sujet = "✅ [SCHEDULER] Votre signalement a été résolu";
        String contenu = "Bonjour " + enseignant.getPrenom() + " " + enseignant.getNom() + ",\n\n" +
            "Votre signalement concernant la salle " + salleNom + " a été résolu.\n\n" +
            "Type: " + signalement.getTypeProbleme() + "\n" +
            "Résolution: " + signalement.getCommentaireResolution() + "\n\n" +
            "Cordialement,\nL'équipe SCHEDULER";
        
        emailService.envoyerEmail(enseignant.getEmail(), sujet, contenu);
        notificationService.ajouterNotification(enseignant.getId(),
            "✅ Votre signalement (Salle " + salleNom + ") a été résolu.");
    }
}