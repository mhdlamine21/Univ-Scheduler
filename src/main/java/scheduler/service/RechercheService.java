package scheduler.service;

import scheduler.dao.*;
import scheduler.modele.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de recherche des salles disponibles.
 */
public class RechercheService {
    private static final Logger logger = LoggerFactory.getLogger(RechercheService.class);
    
    private SalleDAO salleDAO;
    private CreneauDAO creneauDAO;
    private BatimentDAO batimentDAO;
    private EquipementDAO equipementDAO;
    
    public RechercheService() {
        this.salleDAO = new SalleDAO();
        this.creneauDAO = new CreneauDAO();
        this.batimentDAO = new BatimentDAO();
        this.equipementDAO = new EquipementDAO();
    }
    
    /**
     * Recherche les salles libres à l'instant présent.
     */
    public List<Salle> rechercherSallesMaintenant() throws SQLException {
        String aujourdhui = java.time.LocalDate.now().toString();
        String heureActuelle = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        
        List<Salle> resultats = rechercherSallesParCriteres(aujourdhui, heureActuelle, null, null, null, null);
        return resultats;
    }
    
    /**
     * Recherche les salles selon des critères précis.
     */
    public List<Salle> rechercherSallesParCriteres(
            String date, 
            String heureDebut, 
            Integer dureeMinutes,
            Integer capaciteMin, 
            List<Integer> equipementsRequis,
            String typeSalle) throws SQLException {
        
        String heureFin = null;
        if (heureDebut != null && dureeMinutes != null) {
            String[] parts = heureDebut.split(":");
            int heures = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int totalMinutes = heures * 60 + minutes + dureeMinutes;
            heures = totalMinutes / 60;
            minutes = totalMinutes % 60;
            heureFin = String.format("%02d:%02d", heures, minutes);
        }
        
        List<Salle> toutesSalles = salleDAO.listerDisponibles();
        
        List<Salle> resultats = new ArrayList<>();
        
        for (Salle salle : toutesSalles) {
            boolean correspond = true;
            
            if (capaciteMin != null && salle.getCapacite() < capaciteMin) {
                correspond = false;
            }
            
            if (correspond && typeSalle != null && !typeSalle.isEmpty() && 
                !salle.getType().equals(typeSalle)) {
                correspond = false;
            }
            
            if (correspond && date != null && heureDebut != null && heureFin != null) {
                boolean estLibre = creneauDAO.verifierDisponibiliteSalle(
                    salle.getId(), date, heureDebut, heureFin
                );
                if (!estLibre) {
                    correspond = false;
                }
            }
            
            if (correspond && equipementsRequis != null && !equipementsRequis.isEmpty()) {
                List<Integer> equipementsSalle = salle.getEquipements();
                if (!equipementsSalle.containsAll(equipementsRequis)) {
                    correspond = false;
                }
            }
            
            if (correspond) {
                resultats.add(salle);
            }
        }
        return resultats;
    }
    
    
    /**
     * Recherche les salles avec détails d'occupation
     */
    public List<Map<String, Object>> rechercherSallesAvecOccupation() throws SQLException {
        List<Map<String, Object>> resultats = new ArrayList<>();
        List<Salle> toutesSalles = salleDAO.listerTous();
        String aujourdhui = LocalDate.now().toString();
        String maintenant = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        
        for (Salle salle : toutesSalles) {
            Map<String, Object> info = new HashMap<>();
            info.put("salle", salle);
            info.put("numero", salle.getNumero());
            info.put("capacite", salle.getCapacite());
            info.put("type", salle.getType());
            info.put("batimentId", salle.getBatimentId());
            info.put("etage", salle.getEtage());
            info.put("statut", salle.getStatut());
            
            String occupation = "libre";
            String detail = "";
            
            List<Creneau> creneaux = creneauDAO.listerParSalle(salle.getId());
            for (Creneau c : creneaux) {
                if (c.getJour().equals(aujourdhui) && !"annule".equals(c.getStatut())) {
                    if (c.getHeureDebut().compareTo(maintenant) <= 0 && 
                        c.getHeureFin().compareTo(maintenant) > 0) {
                        occupation = "occupee";
                        detail = "Cours jusqu'à " + c.getHeureFin();
                        break;
                    }
                    String[] parts = c.getHeureDebut().split(":");
                    int heure = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    LocalTime debut = LocalTime.of(heure, minute);
                    LocalTime now = LocalTime.now();
                    if (debut.isAfter(now) && debut.minusMinutes(30).isBefore(now)) {
                        occupation = "bientot";
                        detail = "Libre dans " + (debut.getHour() - now.getHour()) + "h";
                    }
                }
            }
            
            info.put("occupation", occupation);
            info.put("detailOccupation", detail);
            resultats.add(info);
        }
        
        return resultats;
    }
    
    /**
     * Récupère le planning d'une salle sur une période
     */
    public List<Map<String, String>> getPlanningSalle(int salleId, String dateDebut, String dateFin) throws SQLException {
        List<Map<String, String>> planning = new ArrayList<>();
        LocalDate debut = LocalDate.parse(dateDebut);
        LocalDate fin = LocalDate.parse(dateFin);
        
        List<Creneau> coursSalle = creneauDAO.listerParSalle(salleId);
        ReservationDAO reservationDAO = new ReservationDAO();
        List<Reservation> reservations = reservationDAO.listerParSalle(salleId);
        
        LocalDate current = debut;
        while (!current.isAfter(fin)) {
            String dateStr = current.toString();
            for (int h = 8; h < 18; h++) {
                String heureDebut = String.format("%02d:00", h);
                String heureFin = String.format("%02d:00", h + 2);
                
                boolean occupe = false;
                String detail = "";
                
                for (Creneau c : coursSalle) {
                    if (c.getJour().equals(dateStr) && !"annule".equals(c.getStatut())) {
                        if (c.getHeureDebut().equals(heureDebut)) {
                            occupe = true;
                            Cours cours = new CoursDAO().trouverParId(c.getCoursId());
                            if (cours != null) {
                                Matiere m = new MatiereDAO().trouverParId(cours.getMatiereId());
                                detail = "Cours: " + (m != null ? m.getNom() : "?");
                            }
                            break;
                        }
                    }
                }
                
                for (Reservation r : reservations) {
                    if (r.getDateReservation().equals(dateStr) && r.getHeureDebut().equals(heureDebut)) {
                        occupe = true;
                        detail = "Réservé: " + r.getMotif();
                        break;
                    }
                }
                
                Map<String, String> creneau = new HashMap<>();
                creneau.put("jour", current.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                creneau.put("horaire", heureDebut + " - " + heureFin);
                creneau.put("occupation", occupe ? "🔴 " + detail : "🟢 Libre");
                planning.add(creneau);
            }
            current = current.plusDays(1);
        }
        
        return planning;
    }
    
    /**
     * Recherche les salles libres à une date et heure données.
     */
    public List<Salle> rechercherSallesLibres(String date, String heureDebut, String heureFin) throws SQLException {
        List<Salle> toutesSalles = salleDAO.listerDisponibles();
        List<Salle> resultats = new ArrayList<>();
        
        for (Salle salle : toutesSalles) {
            boolean estLibre = creneauDAO.verifierDisponibiliteSalle(
                salle.getId(), date, heureDebut, heureFin
            );
            
            if (estLibre) {
                resultats.add(salle);
            }
        }
        return resultats;
    }
    
    /**
     * Récupère les détails complets d'une salle.
     */

    public Map<String, Object> getDetailsSalle(int salleId) throws SQLException {
        Map<String, Object> details = new HashMap<>();
        
        Salle salle = salleDAO.trouverParId(salleId);
        if (salle == null) {
            return null;
        }
        
        Batiment batiment = batimentDAO.trouverParId(salle.getBatimentId());
        List<Equipement> equipements = equipementDAO.listerParSalle(salleId);
        List<Creneau> creneaux = creneauDAO.listerParSalle(salleId);
        
        details.put("salle", salle);
        details.put("batiment", batiment);
        details.put("equipements", equipements);
        details.put("creneaux", creneaux.stream()
            .filter(c -> "planifie".equals(c.getStatut()))
            .limit(10)
            .collect(Collectors.toList()));
        
        return details;
    }
}