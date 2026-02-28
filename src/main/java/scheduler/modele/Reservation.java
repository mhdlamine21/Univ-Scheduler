package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Représente une réservation de salle
 */
public class Reservation {
    private int id;
    private int utilisateurId;
    private int salleId;
    private String type;
    private String motif;
    private String description;
    private String dateReservation;
    private String heureDebut;
    private String heureFin;
    private String statut;
    private Integer classeId;
    private Integer enseignantId;
    private LocalDateTime dateCreation;
    
    public Reservation() {}
    
    public Reservation(int utilisateurId, int salleId, String type, String motif, 
                       String dateReservation, String heureDebut, String heureFin) {
        this.utilisateurId = utilisateurId;
        this.salleId = salleId;
        this.type = type;
        this.motif = motif;
        this.dateReservation = dateReservation;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.statut = "en_cours";
        this.dateCreation = LocalDateTime.now();
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }
    
    public int getSalleId() { return salleId; }
    public void setSalleId(int salleId) { this.salleId = salleId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDateReservation() { return dateReservation; }
    public void setDateReservation(String dateReservation) { this.dateReservation = dateReservation; }
    
    public String getHeureDebut() { return heureDebut; }
    public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }
    
    public String getHeureFin() { return heureFin; }
    public void setHeureFin(String heureFin) { this.heureFin = heureFin; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    
    public Integer getClasseId() { return classeId; }
    public void setClasseId(Integer classeId) { this.classeId = classeId; }

    public Integer getEnseignantId() { return enseignantId; }
    public void setEnseignantId(Integer enseignantId) { this.enseignantId = enseignantId; }
}