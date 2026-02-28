package scheduler.service;

import scheduler.dao.UtilisateurDAO;
import scheduler.modele.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * Service de gestion des comptes utilisateurs.
 */
public class UtilisateurService {
    private static final Logger logger = LoggerFactory.getLogger(UtilisateurService.class);
    private UtilisateurDAO utilisateurDAO;
    
    public UtilisateurService() {
        this.utilisateurDAO = new UtilisateurDAO();
    }
    
    /**
     * Liste tous les utilisateurs
     */
    public List<Utilisateur> listerTous() throws SQLException {
        try {
            List<Utilisateur> utilisateurs = utilisateurDAO.listerTous();
            return utilisateurs;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du chargement des utilisateurs", e);
            throw e;
        }
    }
    
    /**
     * Liste les utilisateurs par rôle
     */
    public List<Utilisateur> listerParRole(String role) throws SQLException {
        try {
            List<Utilisateur> utilisateurs = utilisateurDAO.listerParRole(role);
            return utilisateurs;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du chargement des utilisateurs par rôle", e);
            throw e;
        }
    }
    
    /**
     * Liste les utilisateurs en attente de validation
     */
    public List<Utilisateur> listerEnAttente() throws SQLException {
        try {
            List<Utilisateur> utilisateurs = utilisateurDAO.listerEnAttente();
            return utilisateurs;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du chargement des utilisateurs en attente", e);
            throw e;
        }
    }
    
    /**
     * Trouve un utilisateur par son ID
     */
    public Utilisateur trouverParId(int id) throws SQLException {
        try {
            Utilisateur utilisateur = utilisateurDAO.trouverParId(id);
            if (utilisateur != null) {
            } else {
            }
            return utilisateur;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la recherche de l'utilisateur ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Trouve un utilisateur par son email
     */
    public Utilisateur trouverParEmail(String email) throws SQLException {
        try {
            Utilisateur utilisateur = utilisateurDAO.trouverParEmail(email);
            if (utilisateur != null) {
            } else {
            }
            return utilisateur;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la recherche de l'utilisateur {}", email, e);
            throw e;
        }
    }
    
    /**
     * Modifie un utilisateur
     */
    public void modifier(Utilisateur utilisateur) throws SQLException {
        try {
            utilisateurDAO.modifier(utilisateur);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la modification de l'utilisateur ID {}", utilisateur.getId(), e);
            throw e;
        }
    }
    
    /**
     * Supprime un utilisateur
     */
    public void supprimer(int id) throws SQLException {
        try {
            utilisateurDAO.supprimer(id);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la suppression de l'utilisateur ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Valide un utilisateur
     */
    public void validerUtilisateur(int id) throws SQLException {
        try {
            utilisateurDAO.validerUtilisateur(id);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la validation de l'utilisateur ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Vérifie si un email existe déjà
     */
    public boolean emailExiste(String email) throws SQLException {
        try {
            boolean existe = utilisateurDAO.emailExiste(email);
            return existe;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la vérification de l'email {}", email, e);
            throw e;
        }
    }
    
    /**
     * Recherche des utilisateurs par critères
     */
    public List<Utilisateur> rechercher(String recherche, String role, Boolean actif) throws SQLException {
        List<Utilisateur> resultats = new ArrayList<>();
        List<Utilisateur> tous = listerTous();
        
        for (Utilisateur u : tous) {
            boolean correspond = true;
            
            if (recherche != null && !recherche.isEmpty()) {
                String rech = recherche.toLowerCase();
                if (!u.getNom().toLowerCase().contains(rech) &&
                    !u.getPrenom().toLowerCase().contains(rech) &&
                    !u.getEmail().toLowerCase().contains(rech)) {
                    correspond = false;
                }
            }
            
            if (role != null && !role.isEmpty() && correspond) {
                if (!u.getRole().equals(role)) {
                    correspond = false;
                }
            }
            
            if (actif != null && correspond) {
                if (u.isEstValide() != actif) {
                    correspond = false;
                }
            }
            
            if (correspond) {
                resultats.add(u);
            }
        }
        
        return resultats;
    }
    
    /**
     * Compte les utilisateurs par rôle
     */
    public int compterParRole(String role) throws SQLException {
        return listerParRole(role).size();
    }
    
    /**
     * Récupère tous les étudiants d'une classe
     */
    public List<Etudiant> getEtudiantsParClasse(int classeId) throws SQLException {
        List<Etudiant> etudiants = new ArrayList<>();
        List<Utilisateur> tous = listerTous();
        
        for (Utilisateur u : tous) {
            if (u instanceof Etudiant) {
                Etudiant e = (Etudiant) u;
                if (e.getClasseId() == classeId) {
                    etudiants.add(e);
                }
            }
        }
        
        return etudiants;
    }
    
    /**
     * Récupère tous les étudiants
     */
    public List<Etudiant> getTousEtudiants() throws SQLException {
        List<Etudiant> etudiants = new ArrayList<>();
        List<Utilisateur> tous = listerParRole("etudiant");
        
        for (Utilisateur u : tous) {
            if (u instanceof Etudiant) {
                etudiants.add((Etudiant) u);
            }
        }
        
        return etudiants;
    }
    
    /**
     * Récupère tous les enseignants
     */
    public List<Enseignant> getEnseignants() throws SQLException {
        List<Enseignant> enseignants = new ArrayList<>();
        List<Utilisateur> tous = listerParRole("enseignant");
        
        for (Utilisateur u : tous) {
            if (u instanceof Enseignant) {
                enseignants.add((Enseignant) u);
            }
        }
        
        return enseignants;
    }
    
    /**
     * Récupère tous les gestionnaires
     */
    public List<Gestionnaire> getGestionnaires() throws SQLException {
        List<Gestionnaire> gestionnaires = new ArrayList<>();
        List<Utilisateur> tous = listerParRole("gestionnaire");
        
        for (Utilisateur u : tous) {
            if (u instanceof Gestionnaire) {
                gestionnaires.add((Gestionnaire) u);
            }
        }
        
        return gestionnaires;
    }
    
    /**
     * Récupère tous les administrateurs
     */
    public List<Administrateur> getAdministrateurs() throws SQLException {
        List<Administrateur> admins = new ArrayList<>();
        List<Utilisateur> tous = listerParRole("admin");
        
        for (Utilisateur u : tous) {
            if (u instanceof Administrateur) {
                admins.add((Administrateur) u);
            }
        }
        
        return admins;
    }
    
    /**
     * Met à jour le mot de passe d'un utilisateur
     */
    public void updateMotDePasse(int id, String nouveauMotDePasse) throws SQLException {
        try {
            // Récupérer l'utilisateur
            Utilisateur utilisateur = trouverParId(id);
            if (utilisateur == null) {
                throw new SQLException("Utilisateur non trouvé");
            }
            
            // Mettre à jour le mot de passe
            utilisateur.setMotDePasse(nouveauMotDePasse);
            modifier(utilisateur);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la mise à jour du mot de passe", e);
            throw e;
        }
    }
    
    /**
     * Active ou désactive un utilisateur
     */
    public void setActif(int id, boolean actif) throws SQLException {
        try {
            Utilisateur utilisateur = trouverParId(id);
            if (utilisateur == null) {
                throw new SQLException("Utilisateur non trouvé");
            }
            
            utilisateur.setEstValide(actif);
            modifier(utilisateur);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la modification du statut", e);
            throw e;
        }
    }
}