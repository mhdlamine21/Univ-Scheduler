package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Modèle de notification interne persistée en base.
 */
public class Notification {
    private int id;
    private int utilisateurId;
    private String message;
    private boolean estLue;
    private LocalDateTime dateCreation;

    public Notification() {
        this.estLue = false;
        this.dateCreation = LocalDateTime.now();
    }

    public Notification(int utilisateurId, String message) {
        this.utilisateurId = utilisateurId;
        this.message = message;
        this.estLue = false;
        this.dateCreation = LocalDateTime.now();
    }

    public Notification(int id, int utilisateurId, String message, boolean estLue, LocalDateTime dateCreation) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.message = message;
        this.estLue = estLue;
        this.dateCreation = dateCreation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isEstLue() { return estLue; }
    public void setEstLue(boolean estLue) { this.estLue = estLue; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
