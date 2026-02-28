package scheduler.util;

import scheduler.modele.Utilisateur;
import java.util.prefs.Preferences;

/**
 * Gestionnaire de session utilisateur (Singleton).
 * Maintient l'état de la session de l'utilisateur connecté.
 */
public class SessionUtilisateur {
    
    private static SessionUtilisateur instance;
    private Utilisateur utilisateurConnecte;
    private Preferences preferences;
    
    private SessionUtilisateur() {
        preferences = Preferences.userNodeForPackage(SessionUtilisateur.class);
    }
    
    public static synchronized SessionUtilisateur getInstance() {
        if (instance == null) {
            instance = new SessionUtilisateur();
        }
        return instance;
    }
    
    public void connecter(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
        preferences.put("dernierEmail", utilisateur.getEmail());
    }
    
    public void deconnecter() {
        this.utilisateurConnecte = null;
    }
    
    public boolean estConnecte() {
        return utilisateurConnecte != null;
    }
    
    public Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }
    
    public String getDernierEmail() {
        return preferences.get("dernierEmail", "");
    }
    
    public void setSeSouvenirDeMoi(boolean valeur) {
        preferences.putBoolean("seSouvenirDeMoi", valeur);
    }
    
    public boolean getSeSouvenirDeMoi() {
        return preferences.getBoolean("seSouvenirDeMoi", false);
    }
    
    public boolean aRole(String role) {
        return utilisateurConnecte != null && utilisateurConnecte.getRole().equals(role);
    }
    
    public boolean estAdmin() {
        return aRole(Constantes.ROLE_ADMIN);
    }
    
    public boolean estGestionnaire() {
        return aRole(Constantes.ROLE_GESTIONNAIRE);
    }
    
    public boolean estEnseignant() {
        return aRole(Constantes.ROLE_ENSEIGNANT);
    }
    
    public boolean estEtudiant() {
        return aRole(Constantes.ROLE_ETUDIANT);
    }
    
    public String getNomComplet() {
        if (utilisateurConnecte != null) {
            return utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom();
        }
        return "";
    }
    
    public void nettoyerPreferences() {
        try {
            preferences.clear();
        } catch (Exception e) { }
    }
}