package scheduler.service;

import scheduler.dao.SalleDAO;
import scheduler.dao.EquipementDAO;
import scheduler.dao.ReservationDAO;
import scheduler.dao.CreneauDAO;
import scheduler.dao.ClasseDAO; // AJOUT
import scheduler.dao.CoursDAO;
import scheduler.modele.Salle;
import scheduler.modele.Utilisateur;
import scheduler.modele.Equipement;
import scheduler.modele.Reservation; // AJOUT
import scheduler.modele.Creneau; // AJOUT
import scheduler.modele.Classe; // AJOUT
import scheduler.modele.Cours;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Service métier pour la gestion des salles.
 */
public class SalleService {
    private static final Logger logger = LoggerFactory.getLogger(SalleService.class);
    private SalleDAO salleDAO;
    private EquipementDAO equipementDAO;
    private ReservationDAO reservationDAO;
    private CreneauDAO creneauDAO; 
    
    public SalleService() {
        this.salleDAO = new SalleDAO();
        this.equipementDAO = new EquipementDAO();
        this.reservationDAO = new ReservationDAO();
        this.creneauDAO = new CreneauDAO(); 
    }
    
    /**
     * Ajoute une nouvelle salle
     */
    public void ajouter(Salle salle) throws SQLException {
        try {
            if (numeroExiste(salle.getNumero())) {
                throw new SQLException("Une salle avec le numéro " + salle.getNumero() + " existe déjà");
            }
            
            salleDAO.ajouter(salle);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de l'ajout de la salle {}", salle.getNumero(), e);
            throw e;
        }
    }
    
    /**
     * Trouve une salle par son ID
     */
    public Salle trouverParId(int id) throws SQLException {
        try {
            Salle salle = salleDAO.trouverParId(id);
            if (salle != null) {
            } else {
            }
            return salle;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la recherche de la salle ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Liste toutes les salles
     */
    public List<Salle> listerTous() throws SQLException {
        try {
            List<Salle> salles = salleDAO.listerTous();
            return salles;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du chargement des salles", e);
            throw e;
        }
    }
    
    /**
     * Liste les salles d'un bâtiment
     */
    public List<Salle> listerParBatiment(int batimentId) throws SQLException {
        try {
            List<Salle> salles = salleDAO.listerParBatiment(batimentId);
            return salles;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du chargement des salles pour le bâtiment ID {}", batimentId, e);
            throw e;
        }
    }
    
    /**
     * Liste les salles disponibles
     */
    public List<Salle> listerDisponibles() throws SQLException {
        try {
            List<Salle> salles = salleDAO.listerDisponibles();
            return salles;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du chargement des salles disponibles", e);
            throw e;
        }
    }
    
    /**
     * Modifie une salle
     */
    public void modifier(Salle salle) throws SQLException {
        try {
            if (numeroExisteSauf(salle.getNumero(), salle.getId())) {
                throw new SQLException("Une autre salle avec le numéro " + salle.getNumero() + " existe déjà");
            }
            
            salleDAO.modifier(salle);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la modification de la salle ID {}", salle.getId(), e);
            throw e;
        }
    }
    
    /**
     * Vérifie les conflits d'indisponibilité d'une salle.
     */
    
    public List<Map<String, Object>> verifierConflitsIndisponibilite(int salleId, 
            String dateDebut, String dateFin) throws SQLException {
        
        List<Map<String, Object>> conflits = new ArrayList<>();
        Salle salle = salleDAO.trouverParId(salleId);
        if (salle == null) return conflits;
        
        List<Creneau> creneaux = creneauDAO.listerParSalle(salleId);
        LocalDate debut = LocalDate.parse(dateDebut);
        LocalDate fin = LocalDate.parse(dateFin);
        
        for (Creneau c : creneaux) {
            LocalDate dateCreneau = LocalDate.parse(c.getJour());
            if (!dateCreneau.isBefore(debut) && !dateCreneau.isAfter(fin)) {
                Map<String, Object> conflit = new HashMap<>();
                conflit.put("type", "SALLE_INDISPONIBLE");
                conflit.put("description", "La salle " + salle.getNumero() + 
                    " sera indisponible du " + dateDebut + " au " + dateFin + 
                    " mais un cours y est planifié le " + c.getJour());
                conflit.put("creneau", c);
                conflit.put("date", c.getJour());
                conflits.add(conflit);
            }
        }
        
        return conflits;
    }
    
    
    
    /**
     * Supprime une salle
     */
    public void supprimer(int id) throws SQLException {
        try {
            if (aReservations(id)) {
                throw new SQLException("Impossible de supprimer : la salle a des réservations");
            }
            
            if (aCreneaux(id)) {
                throw new SQLException("Impossible de supprimer : la salle a des cours planifiés");
            }
            
            salleDAO.supprimer(id);
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la suppression de la salle ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Vérifie si une salle a des réservations
     */
    private boolean aReservations(int salleId) throws SQLException {
        try {
            List<Reservation> reservations = reservationDAO.listerParSalle(salleId);
            return reservations != null && !reservations.isEmpty();
        } catch (SQLException e) {
            logger.error("Erreur lors de la vérification des réservations", e);
            return false;
        }
    }
    
    /**
     * Vérifie si une salle a des créneaux de cours
     */
    private boolean aCreneaux(int salleId) throws SQLException {
        try {
            List<Creneau> creneaux = creneauDAO.listerParSalle(salleId);
            return creneaux != null && !creneaux.isEmpty();
        } catch (SQLException e) {
            logger.error("Erreur lors de la vérification des créneaux", e);
            return false;
        }
    }
    
    /**
     * Rend une salle indisponible
     */
    public void rendreIndisponible(int id, String motif, String dateDebut, String dateFin) throws SQLException {
        try {
            if (dateDebut == null || dateFin == null || motif == null || motif.trim().isEmpty()) {
                throw new SQLException("Le motif et les dates sont obligatoires");
            }
            
            salleDAO.rendreIndisponible(id, motif, dateDebut, dateFin);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du marquage indisponible de la salle ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Rend une salle indisponible avec notification des conflits.
     */

    public void rendreIndisponibleAvecNotification(int id, String motif, 
            String dateDebut, String dateFin, NotificationService notificationService,
            EmailService emailService, UtilisateurService utilisateurService) throws SQLException {
        
        List<Map<String, Object>> conflits = verifierConflitsIndisponibilite(id, dateDebut, dateFin);
        
        rendreIndisponible(id, motif, dateDebut, dateFin);
        
        for (Map<String, Object> conflit : conflits) {
            Creneau c = (Creneau) conflit.get("creneau");
            if (c != null) {
                Cours cours = new CoursDAO().trouverParId(c.getCoursId());
                if (cours != null) {
                    Utilisateur enseignant = utilisateurService.trouverParId(cours.getEnseignantId());
                    if (enseignant != null && notificationService != null) {
                        notificationService.ajouterNotification(enseignant.getId(),
                            "⚠️ La salle de votre cours du " + c.getJour() + 
                            " sera indisponible du " + dateDebut + " au " + dateFin);
                        
                        if (emailService != null) {
                            emailService.envoyerConflitGestionnaire(
                                enseignant.getEmail(),
                                "Salle indisponible",
                                "Votre cours du " + c.getJour() + " est affecté",
                                salleDAO.trouverParId(id).getNumero(),
                                c.getJour(),
                                c.getHeureDebut(),
                                c.getHeureFin(),
                                enseignant.getPrenom() + " " + enseignant.getNom()
                            );
                        }
                    }
                }
            }
        }
        
        logger.info("🔴 Salle #{} rendue indisponible du {} au {} - {} conflit(s) notifié(s)",
            id, dateDebut, dateFin, conflits.size());
    }
    
    /**
     * Rend une salle disponible
     */
    public void rendreDisponible(int id) throws SQLException {
        try {
            salleDAO.rendreDisponible(id);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du marquage disponible de la salle ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Ajoute un équipement à une salle
     */
    public void ajouterEquipement(int salleId, int equipementId) throws SQLException {
        try {
            Equipement equipement = equipementDAO.trouverParId(equipementId);
            if (equipement == null) {
                throw new SQLException("Équipement non trouvé");
            }
            
            int utilise = getQuantiteUtilisee(equipementId);
            int disponible = equipement.getQuantite() - utilise;
            
            if (disponible <= 0) {
                throw new SQLException("Stock insuffisant pour l'équipement " + equipement.getNom() + 
                                      " (disponible: 0, total: " + equipement.getQuantite() + ")");
            }
            
            salleDAO.ajouterEquipement(salleId, equipementId);
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de l'ajout de l'équipement {} à la salle {}", equipementId, salleId, e);
            throw e;
        }
    }
    
    /**
     * Retire un équipement d'une salle
     */
    public void retirerEquipement(int salleId, int equipementId) throws SQLException {
        try {
            salleDAO.retirerEquipement(salleId, equipementId);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du retrait de l'équipement {} de la salle {}", equipementId, salleId, e);
            throw e;
        }
    }
    
    /**
     * Ajoute plusieurs exemplaires d'un équipement à une salle
     */
    public void ajouterPlusieursEquipements(int salleId, int equipementId, int quantite) throws SQLException {
        try {
            Equipement equipement = equipementDAO.trouverParId(equipementId);
            if (equipement == null) {
                throw new SQLException("Équipement non trouvé");
            }
            
            int utilise = getQuantiteUtilisee(equipementId);
            int disponible = equipement.getQuantite() - utilise;
            
            if (disponible < quantite) {
                throw new SQLException("Stock insuffisant. Demandé: " + quantite + 
                                      ", disponible: " + disponible);
            }
            
            for (int i = 0; i < quantite; i++) {
                salleDAO.ajouterEquipement(salleId, equipementId);
            }
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de l'ajout multiple", e);
            throw e;
        }
    }
    
    /**
     * Calcule la quantité utilisée d'un équipement
     */
    public int getQuantiteUtilisee(int equipementId) throws SQLException {
        List<Salle> toutesSalles = listerTous();
        int count = 0;
        for (Salle s : toutesSalles) {
            for (int id : s.getEquipements()) {
                if (id == equipementId) count++;
            }
        }
        return count;
    }
    
    /**
     * Récupère les statistiques des salles
     */
    public Map<String, Object> getStatistiques() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        
        List<Salle> toutes = listerTous();
        List<Salle> disponibles = listerDisponibles();
        
        int total = toutes.size();
        int dispo = disponibles.size();
        int indispo = total - dispo;
        
        stats.put("total", total);
        stats.put("disponibles", dispo);
        stats.put("indisponibles", indispo);
        
        // Capacité totale
        int capaciteTotale = toutes.stream().mapToInt(Salle::getCapacite).sum();
        stats.put("capaciteTotale", capaciteTotale);
        
        // Capacité moyenne
        double capaciteMoyenne = total > 0 ? (double) capaciteTotale / total : 0;
        stats.put("capaciteMoyenne", Math.round(capaciteMoyenne * 10) / 10.0);
        
        // Répartition par type
        Map<String, Integer> parType = new HashMap<>();
        for (Salle s : toutes) {
            parType.put(s.getType(), parType.getOrDefault(s.getType(), 0) + 1);
        }
        stats.put("parType", parType);
        
        // Répartition par étage
        Map<Integer, Integer> parEtage = new HashMap<>();
        for (Salle s : toutes) {
            parEtage.put(s.getEtage(), parEtage.getOrDefault(s.getEtage(), 0) + 1);
        }
        stats.put("parEtage", parEtage);
        
        return stats;
    }
    
    /**
     * Vérifie si un numéro de salle existe déjà
     */
    public boolean numeroExiste(String numero) throws SQLException {
        List<Salle> toutes = listerTous();
        return toutes.stream().anyMatch(s -> s.getNumero().equalsIgnoreCase(numero));
    }
    
    /**
     * Vérifie si un numéro de salle existe déjà (sauf pour une salle donnée)
     */
    public boolean numeroExisteSauf(String numero, int salleId) throws SQLException {
        List<Salle> toutes = listerTous();
        return toutes.stream()
            .filter(s -> s.getId() != salleId)
            .anyMatch(s -> s.getNumero().equalsIgnoreCase(numero));
    }
    
    /**
     * Récupère toutes les salles avec leurs équipements détaillés
     */
    public List<Map<String, Object>> getSallesAvecDetails() throws SQLException {
        List<Salle> salles = listerTous();
        List<Map<String, Object>> resultats = new ArrayList<>();
        
        for (Salle s : salles) {
            Map<String, Object> details = new HashMap<>();
            details.put("salle", s);
            
            // Compter les équipements par type
            Map<Integer, Integer> compteur = new HashMap<>();
            for (int id : s.getEquipements()) {
                compteur.put(id, compteur.getOrDefault(id, 0) + 1);
            }
            
            List<Map<String, Object>> equipements = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : compteur.entrySet()) {
                Equipement e = equipementDAO.trouverParId(entry.getKey());
                if (e != null) {
                    Map<String, Object> eq = new HashMap<>();
                    eq.put("id", e.getId());
                    eq.put("nom", e.getNom());
                    eq.put("quantite", entry.getValue());
                    eq.put("description", e.getDescription());
                    equipements.add(eq);
                }
            }
            
            details.put("equipements", equipements);
            details.put("nbEquipements", s.getEquipements().size());
            resultats.add(details);
        }
        
        return resultats;
    }
    
    /**
     * Recherche des salles par critères
     */
    public List<Salle> rechercher(String recherche, String type, String statut, Integer batimentId) throws SQLException {
        List<Salle> resultats = new ArrayList<>();
        List<Salle> toutes = listerTous();
        
        for (Salle s : toutes) {
            boolean correspond = true;
            
            if (recherche != null && !recherche.isEmpty()) {
                if (!s.getNumero().toLowerCase().contains(recherche.toLowerCase())) {
                    correspond = false;
                }
            }
            
            if (correspond && type != null && !"Tous".equals(type)) {
                if (!s.getType().equals(type)) {
                    correspond = false;
                }
            }
            
            if (correspond && statut != null && !"Tous".equals(statut)) {
                if (!s.getStatut().equals(statut)) {
                    correspond = false;
                }
            }
            
            if (correspond && batimentId != null && batimentId > 0) {
                if (s.getBatimentId() != batimentId) {
                    correspond = false;
                }
            }
            
            if (correspond) {
                resultats.add(s);
            }
        }
        
        return resultats;
    }
    
    
    /**
     * Recherche les salles disponibles pour le planning.
     */
    public List<Salle> rechercherSallesDisponibles(String date, String heureDebut, String heureFin, int classeId) throws SQLException {
        List<Salle> toutesSalles = listerTous();
        List<Salle> disponibles = new ArrayList<>();
        
        ClasseDAO classeDAO = new ClasseDAO();
        Classe classe = classeDAO.trouverParId(classeId);
        int effectif = classe != null ? classe.getEffectif() : 30;
        
        int compteur = 0;
        
        for (Salle salle : toutesSalles) {
            compteur++;
            
            if (salle.getStatut().equals("disponible")) {
                disponibles.add(salle);
            } else {
            }
        }
        
        return disponibles;
    }
    
    /**
     * Recherche avancée avec filtres.
     */
    public List<Salle> rechercherSallesAvecFiltres(String date, String heureDebut, String heureFin, 
                                                     int capaciteMin, List<Integer> equipementsRequis) throws SQLException {
        List<Salle> toutesSalles = listerTous();
        List<Salle> resultats = new ArrayList<>();
        
        for (Salle salle : toutesSalles) {
            if (!salle.getStatut().equals("disponible")) continue;
            
            boolean estLibre = creneauDAO.verifierDisponibiliteSalle(
                salle.getId(), date, heureDebut, heureFin
            );
            
            boolean pasDeReservation = reservationDAO.verifierDisponibilite(
                salle.getId(), date, heureDebut, heureFin
            );
            
            if (!estLibre || !pasDeReservation) continue;
            
            if (salle.getCapacite() < capaciteMin) continue;
            
            if (equipementsRequis != null && !equipementsRequis.isEmpty()) {
                List<Integer> equipementsSalle = salle.getEquipements();
                if (!equipementsSalle.containsAll(equipementsRequis)) continue;
            }
            
            resultats.add(salle);
        }
        
        return resultats;
    }
    
    /**
     * Récupère les salles avec leurs occupations pour une date donnée
     */
    public Map<Salle, List<String>> getSallesAvecOccupation(String date) throws SQLException {
        Map<Salle, List<String>> occupations = new HashMap<>();
        List<Salle> toutesSalles = listerTous();
        
        for (Salle salle : toutesSalles) {
            List<String> creneauxOccupes = new ArrayList<>();
            
            List<Creneau> creneaux = creneauDAO.listerParSalle(salle.getId());
            for (Creneau c : creneaux) {
                if (c.getJour().equals(date)) {
                    creneauxOccupes.add(c.getHeureDebut() + "-" + c.getHeureFin() + " (Cours)");
                }
            }
            
            List<Reservation> reservations = reservationDAO.listerParSalle(salle.getId());
            for (Reservation r : reservations) {
                if (r.getDateReservation().equals(date)) {
                    creneauxOccupes.add(r.getHeureDebut() + "-" + r.getHeureFin() + " (Réservation)");
                }
            }
            
            occupations.put(salle, creneauxOccupes);
        }
        
        return occupations;
    }

    /**
     * Vérifie si une salle est disponible sur un créneau donné.
     */
    public boolean estDisponible(int salleId, String date, String heureDebut, String heureFin) throws SQLException {
        try {
            Salle salle = salleDAO.trouverParId(salleId);
            if (salle == null || "indisponible".equals(salle.getStatut())) return false;
            boolean pasDeCoursNiReservation =
                creneauDAO.verifierDisponibiliteSalle(salleId, date, heureDebut, heureFin) &&
                reservationDAO.verifierDisponibilite(salleId, date, heureDebut, heureFin);
            return pasDeCoursNiReservation;
        } catch (SQLException e) {
            logger.error("Erreur vérification disponibilité salle {}", salleId, e);
            throw e;
        }
    }

    /**
     * Liste les salles disponibles d'un bâtiment.
     */
    public List<Salle> listerDisponiblesParBatiment(int batimentId) throws SQLException {
        List<Salle> salles = listerParBatiment(batimentId);
        salles.removeIf(s -> "indisponible".equals(s.getStatut()));
        return salles;
    }
}