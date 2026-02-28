package scheduler.modele;

/**
 * Représente un créneau horaire pour un cours
 */
public class Creneau {
    private int id;
    private int coursId;
    private String jour;
    private String heureDebut;
    private String heureFin;
    private Integer salleId;
    private String statut;
    private String motifAnnulation;
    
    public Creneau() {}
    
    public Creneau(int coursId, String jour, String heureDebut, String heureFin, Integer salleId) {
        this.coursId = coursId;
        this.jour = jour;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.salleId = salleId;
        this.statut = "planifie";
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getCoursId() { return coursId; }
    public void setCoursId(int coursId) { this.coursId = coursId; }
    
    public String getJour() { return jour; }
    public void setJour(String jour) { this.jour = jour; }
    
    public String getHeureDebut() { return heureDebut; }
    public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }
    
    public String getHeureFin() { return heureFin; }
    public void setHeureFin(String heureFin) { this.heureFin = heureFin; }
    
    public Integer getSalleId() { return salleId; }
    public void setSalleId(Integer salleId) { this.salleId = salleId; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }
}