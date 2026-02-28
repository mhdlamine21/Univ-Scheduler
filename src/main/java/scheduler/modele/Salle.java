package scheduler.modele;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Entité représentant une salle de l'université (amphithéâtre, salle de TD, TP, labo).
 * 
 * Cette classe encapsule :
 * - Les caractéristiques physiques (numéro, bâtiment, étage, capacité d'accueil).
 * - Le type pédagogique (TD, TP, Amphi, Réunion).
 * - L'état de disponibilité opérationnelle et les motifs éventuels d'indisponibilité.
 * - La liste des identifiants des équipements matériels installés.
 * - Le lien visuel vers une photo représentative de la salle.
 */
public class Salle {
    /** Identifiant unique en base de données. */
    private int id;
    /** Numéro ou code unique de la salle (ex: A101, AMPHI-A). */
    private String numero;
    /** Capacité maximale d'accueil d'étudiants en places assises. */
    private int capacite;
    /** Type de salle : 'TD', 'TP', 'Amphi', 'Laboratoire', 'Réunion'. */
    private String type;
    /** Identifiant du bâtiment d'appartenance. */
    private int batimentId;
    /** Numéro d'étage dans le bâtiment (0 = RDC, 1 = 1er étage, etc.). */
    private int etage;
    /** Statut opérationnel : 'disponible', 'indisponible', 'maintenance'. */
    private String statut;
    /** Motif textuel en cas d'indisponibilité (ex: travaux, panne projecteur). */
    private String motifIndisponibilite;
    /** Date de début de la plage d'indisponibilité (format ISO yyyy-MM-dd). */
    private String dateDebutIndisponibilite;
    /** Date de fin de la plage d'indisponibilité (format ISO yyyy-MM-dd). */
    private String dateFinIndisponibilite;
    /** URL ou chemin relatif de la photographie de la salle. */
    private String photoUrl;
    /** Liste des identifiants d'équipements présents dans la salle. */
    private List<Integer> equipements;
    
    /**
     * Constructeur par défaut initialisant une salle disponible sans équipement.
     */
    public Salle() {
        this.equipements = new ArrayList<>();
        this.statut = "disponible";
    }
    
    /**
     * Constructeur avec les caractéristiques principales de la salle.
     * @param numero code identifiant de la salle
     * @param capacite capacité d'accueil en places assises
     * @param type typologie pédagogique (TD, TP, Amphi, etc.)
     * @param batimentId identifiant du bâtiment parent
     * @param etage étage au sein du bâtiment
     */
    public Salle(String numero, int capacite, String type, int batimentId, int etage) {
        this.numero = numero;
        this.capacite = capacite;
        this.type = type;
        this.batimentId = batimentId;
        this.etage = etage;
        this.statut = "disponible";
        this.equipements = new ArrayList<>();
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getBatimentId() { return batimentId; }
    public void setBatimentId(int batimentId) { this.batimentId = batimentId; }
    
    public int getEtage() { return etage; }
    public void setEtage(int etage) { this.etage = etage; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    public String getMotifIndisponibilite() { return motifIndisponibilite; }
    public void setMotifIndisponibilite(String motifIndisponibilite) { this.motifIndisponibilite = motifIndisponibilite; }
    
    public String getDateDebutIndisponibilite() { return dateDebutIndisponibilite; }
    public void setDateDebutIndisponibilite(String dateDebutIndisponibilite) { this.dateDebutIndisponibilite = dateDebutIndisponibilite; }
    
    public String getDateFinIndisponibilite() { return dateFinIndisponibilite; }
    public void setDateFinIndisponibilite(String dateFinIndisponibilite) { this.dateFinIndisponibilite = dateFinIndisponibilite; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    
    public List<Integer> getEquipements() { return equipements; }
    public void setEquipements(List<Integer> equipements) { this.equipements = equipements; }
    
    public void addEquipement(int equipementId) { 
        this.equipements.add(equipementId); 
    }
    
    public void removeEquipement(int equipementId) { 
        this.equipements.remove(Integer.valueOf(equipementId)); 
    }
    
    public boolean aEquipement(int equipementId) {
        return equipements.contains(equipementId);
    }
    
    public int compterEquipement(int equipementId) {
        int count = 0;
        for (int id : equipements) {
            if (id == equipementId) count++;
        }
        return count;
    }
    
    public Map<Integer, Integer> getEquipementsAvecQuantite() {
        Map<Integer, Integer> map = new HashMap<>();
        for (int id : equipements) {
            map.put(id, map.getOrDefault(id, 0) + 1);
        }
        return map;
    }
    
    public int getNbEquipements() {
        return equipements.size();
    }
    
    public int getNbEquipementsUniques() {
        return (int) equipements.stream().distinct().count();
    }
    
    public boolean estDisponible() {
        return "disponible".equals(statut);
    }
    
    public boolean estIndisponible() {
        return "indisponible".equals(statut);
    }
    
    public String getDescription() {
        return numero + " - " + type + " (" + capacite + " places)";
    }
    
    public String getDescriptionComplete() {
        String desc = getDescription();
        if (estIndisponible()) {
            desc += " [INDISPONIBLE]";
            if (motifIndisponibilite != null) {
                desc += " - " + motifIndisponibilite;
            }
        }
        return desc;
    }
    
    public boolean estAdapteePour(int capaciteRequise) {
        return this.capacite >= capaciteRequise;
    }
    
    @Override
    public String toString() {
        return "Salle{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", capacite=" + capacite +
                ", type='" + type + '\'' +
                ", batimentId=" + batimentId +
                ", etage=" + etage +
                ", statut='" + statut + '\'' +
                ", nbEquipements=" + equipements.size() +
                '}';
    }
}