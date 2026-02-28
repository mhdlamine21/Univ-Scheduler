package scheduler.service;

import scheduler.dao.UtilisateurDAO;
import scheduler.dao.DemandeInscriptionDAO;
import scheduler.modele.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Service d'authentification et de gestion des inscriptions.
 */
public class AuthentificationService {
    private UtilisateurDAO utilisateurDAO;
    private DemandeInscriptionDAO demandeDAO;
    
    public AuthentificationService() {
        this.utilisateurDAO = new UtilisateurDAO();
        this.demandeDAO = new DemandeInscriptionDAO();
    }
    
    /**
     * Authentifie un utilisateur par email et mot de passe.
     * @return l'utilisateur si authentifié, null sinon
     */
    public Utilisateur login(String email, String motDePasse) throws SQLException {
        Utilisateur utilisateur = utilisateurDAO.trouverParEmail(email);
        
        if (utilisateur == null) {
            return null;
        }
        
        if (utilisateur != null && utilisateur.isEstValide()) {
            if (utilisateur.getMotDePasse().equals(motDePasse)) {
                return utilisateur;
            }
        }
        return null;
    }
    
    /**
     * Enregistre une demande d'inscription.
     */
    public void inscrire(DemandeInscription demande) throws SQLException {
        if (demandeDAO.emailExiste(demande.getEmail())) {
            throw new SQLException("Cet email est déjà utilisé");
        }
        demandeDAO.ajouter(demande);
    }
    
    /**
     * Valide une demande d'inscription et crée l'utilisateur correspondant.
     * @param demandeId identifiant de la demande
     * @param motDePasseGenere mot de passe généré pour le nouvel utilisateur
     */
    public void validerInscription(int demandeId, String motDePasseGenere) throws SQLException {
        DemandeInscription demande = demandeDAO.trouverParId(demandeId);
        if (demande == null) {
            throw new SQLException("Demande introuvable");
        }
        
        Utilisateur utilisateur = null;
        
        switch (demande.getRoleDemande()) {
            case "etudiant":
                Etudiant etudiant = new Etudiant();
                etudiant.setNom(demande.getNom());
                etudiant.setPrenom(demande.getPrenom());
                etudiant.setEmail(demande.getEmail());
                etudiant.setMotDePasse(motDePasseGenere);
                etudiant.setNumeroEtudiant(demande.getNumeroEtudiant());
                etudiant.setUfrId(demande.getUfrId());
                etudiant.setClasseId(demande.getClasseId());
                etudiant.setTypeEtudiant(demande.getTypeEtudiant() != null ? demande.getTypeEtudiant() : "normal");
                etudiant.setEstValide(true);
                utilisateur = etudiant;
                break;
                
            case "enseignant":
                Enseignant enseignant = new Enseignant();
                enseignant.setNom(demande.getNom());
                enseignant.setPrenom(demande.getPrenom());
                enseignant.setEmail(demande.getEmail());
                enseignant.setMotDePasse(motDePasseGenere);
                enseignant.setMatricule(demande.getMatriculeEnseignant());
                enseignant.setGrade(demande.getGrade());
                enseignant.setEstValide(true);
                utilisateur = enseignant;
                break;
                
            case "gestionnaire":
                Gestionnaire gestionnaire = new Gestionnaire();
                gestionnaire.setNom(demande.getNom());
                gestionnaire.setPrenom(demande.getPrenom());
                gestionnaire.setEmail(demande.getEmail());
                gestionnaire.setMotDePasse(motDePasseGenere);
                gestionnaire.setEstValide(true);
                utilisateur = gestionnaire;
                break;
        }
        
        if (utilisateur != null) {
            utilisateurDAO.ajouter(utilisateur);
            demandeDAO.valider(demandeId);
        }
    }
    
    public void refuserInscription(int demandeId) throws SQLException {
        demandeDAO.refuser(demandeId);
    }
    
    public List<DemandeInscription> getDemandesEnAttente() throws SQLException {
        return demandeDAO.listerEnAttente();
    }
    
    public List<DemandeInscription> getToutesLesDemandes() throws SQLException {
        return demandeDAO.listerToutes();
    }
    
    public boolean emailExiste(String email) throws SQLException {
        return utilisateurDAO.emailExiste(email);
    }
    
    public Utilisateur trouverParEmail(String email) throws SQLException {
        return utilisateurDAO.trouverParEmail(email);
    }
}