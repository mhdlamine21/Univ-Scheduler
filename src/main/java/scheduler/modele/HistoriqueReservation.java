package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Représente l'historique des actions sur les réservations
 */
public class HistoriqueReservation {
    private int id;
    private int reservationId;
    private String action;
    private LocalDateTime dateAction;
    private int utilisateurId;
    private String details;
    
    public HistoriqueReservation() {}
    
    public HistoriqueReservation(int reservationId, String action, int utilisateurId, String details) {
        this.reservationId = reservationId;
        this.action = action;
        this.utilisateurId = utilisateurId;
        this.details = details;
        this.dateAction = LocalDateTime.now();
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }
    
    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}