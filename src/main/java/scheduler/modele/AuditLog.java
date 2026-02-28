package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Modèle de données pour la piste d'audit et la traçabilité des opérations sensibles (Audit Trail).
 * 
 * Enregistre chaque événement critique du système :
 * - Connexion, déconnexion et tentatives infructueuses.
 * - Création, modification et suppression de salles, cours, créneaux et réservations.
 * - Validation ou rejet de demandes d'inscription.
 * - Résolution automatique ou manuelle de conflits d'emploi du temps.
 */
public class AuditLog {
    /** Identifiant séquentiel de l'événement dans le journal d'audit. */
    private int id;
    /** Horodatage précis à la seconde de l'action effectuée. */
    private LocalDateTime timestamp;
    /** Adresse courriel du compte utilisateur auteur de l'opération. */
    private String utilisateurEmail;
    /** Rôle fonctionnel exercé lors de l'action (admin, gestionnaire, etc.). */
    private String role;
    /** Type d'action unifié (CREATION, MODIFICATION, SUPPRESSION, VALIDATION, LOGIN). */
    private String action;
    /** Nom de la table ou du domaine métier ciblé (SALLE, CRENEAU, RESERVATION, COMPTE). */
    private String entite;
    /** Identifiant primaire de l'enregistrement impacté. */
    private int entiteId;
    /** Détails contextuels (diff de valeurs, motif du refus, paramètres). */
    private String details;
    /** Adresse IP réseau ou nom d'hôte de la machine cliente. */
    private String adresseIp;

    /**
     * Constructeur par défaut initialisant l'horodatage à l'instant présent.
     */
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructeur paramétré complet pour enregistrer une action d'audit.
     * @param utilisateurEmail courriel de l'opérateur
     * @param role rôle fonctionnel actif
     * @param action code verbe de l'opération
     * @param entite entité métier ciblée
     * @param entiteId identifiant de l'objet impacté
     * @param details précisions textuelles
     */
    public AuditLog(String utilisateurEmail, String role, String action, String entite, int entiteId, String details) {
        this.timestamp = LocalDateTime.now();
        this.utilisateurEmail = utilisateurEmail;
        this.role = role;
        this.action = action;
        this.entite = entite;
        this.entiteId = entiteId;
        this.details = details;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getUtilisateurEmail() { return utilisateurEmail; }
    public void setUtilisateurEmail(String utilisateurEmail) { this.utilisateurEmail = utilisateurEmail; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntite() { return entite; }
    public void setEntite(String entite) { this.entite = entite; }

    public int getEntiteId() { return entiteId; }
    public void setEntiteId(int entiteId) { this.entiteId = entiteId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getAdresseIp() { return adresseIp; }
    public void setAdresseIp(String adresseIp) { this.adresseIp = adresseIp; }
}
