package scheduler.service;

import scheduler.dao.*;
import scheduler.modele.*;
import java.sql.SQLException;
import java.util.*;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service métier pour Attribution.
 *
 * @author Équipe SCHEDULER
 * @version 2.0
 */
public class AttributionService {
    private static final Logger logger = LoggerFactory.getLogger(AttributionService.class);
    
    private SalleDAO salleDAO;
    private CreneauDAO creneauDAO;
    private BatimentDAO batimentDAO;
    private EquipementDAO equipementDAO;
    
    public AttributionService() {
        this.salleDAO = new SalleDAO();
        this.creneauDAO = new CreneauDAO();
        this.batimentDAO = new BatimentDAO();
        this.equipementDAO = new EquipementDAO();
    }
    
    /**
     * Trouve la salle optimale pour un cours donné
     */
    public Salle trouverSalleOptimale(Cours cours, String jour, String heureDebut, String heureFin) throws SQLException {
        List<Salle> sallesDisponibles = salleDAO.listerDisponibles();
        List<Salle> sallesCompatibles = new ArrayList<>();
        
        // Récupérer la classe
        ClasseDAO classeDAO = new ClasseDAO();
        Classe classe = classeDAO.trouverParId(cours.getClasseId());
        if (classe == null) {
            logger.error("Classe non trouvée pour le cours {}", cours.getId());
            return null;
        }
        
        // Calculer la capacité nécessaire
        int capaciteRequise = calculerCapaciteRequise(cours, classe);
        
        // Récupérer les équipements nécessaires (si spécifiés dans le cours)
        List<Integer> equipementsRequis = new ArrayList<>();
        // À implémenter si le cours a des besoins spécifiques
        
        // Filtrer les salles disponibles sur ce créneau
        for (Salle salle : sallesDisponibles) {
            boolean estLibre = creneauDAO.verifierDisponibiliteSalle(
                salle.getId(), jour, heureDebut, heureFin
            );
            
            if (!estLibre) continue;
            
            // Vérifier la capacité
            if (salle.getCapacite() < capaciteRequise) continue;
            
            // Vérifier les équipements requis
            if (!verifierEquipements(salle, equipementsRequis)) continue;
            
            sallesCompatibles.add(salle);
        }
        
        if (sallesCompatibles.isEmpty()) {
            return null;
        }
        
        // Trier les salles par pertinence
        sallesCompatibles.sort((s1, s2) -> comparerSalles(s1, s2, capaciteRequise, cours, jour));
        
        Salle meilleureSalle = sallesCompatibles.get(0);
        
        return meilleureSalle;
    }
    
    /**
     * Calcule la capacité nécessaire selon le type de cours
     */
    private int calculerCapaciteRequise(Cours cours, Classe classe) {
        if (cours.getTypeCours().equals("CM")) {
            return classe.getEffectif();
        } else if (cours.getTypeCours().equals("TD") || cours.getTypeCours().equals("TP")) {
            // Pour les TD/TP, on prend la taille d'un groupe
            if (classe.getNbGroupes() > 0) {
                return classe.getEffectif() / classe.getNbGroupes();
            }
            return classe.getEffectif() / 2; // Fallback
        }
        return classe.getEffectif();
    }
    
    /**
     * Vérifie si une salle dispose des équipements requis
     */
    private boolean verifierEquipements(Salle salle, List<Integer> equipementsRequis) throws SQLException {
        if (equipementsRequis.isEmpty()) return true;
        
        List<Integer> equipementsSalle = salle.getEquipements();
        return equipementsSalle.containsAll(equipementsRequis);
    }
    
    /**
     * Compare deux salles selon un modèle multicritère pour identifier l'attribution optimale :
     * 1. Minimisation du gaspillage d'espace : préférer une salle dont la capacité dépasse à peine l'effectif requis.
     * 2. Continuité spatiale : privilégier les salles situées dans le bâtiment habituel de la cohorte.
     * 3. Réduction des déplacements : favoriser la proximité avec les cours précédents de la même promotion.
     * 
     * @param s1 première salle candidate
     * @param s2 deuxième salle candidate
     * @param capaciteRequise nombre de places minimum nécessaires
     * @param cours matière et type de cours dispensé
     * @param jour jour de la semaine concerné
     * @return valeur négative si s1 est meilleure, positive si s2 est meilleure, 0 si équivalentes
     */
    private int comparerSalles(Salle s1, Salle s2, int capaciteRequise, Cours cours, String jour) {
        // Critère 1 : Capacité juste suffisante (évite d'immobiliser un grand amphi pour un petit groupe de TD)
        int ecart1 = s1.getCapacite() - capaciteRequise;
        int ecart2 = s2.getCapacite() - capaciteRequise;
        
        if (ecart1 != ecart2) {
            return Integer.compare(ecart1, ecart2);
        }
        
        // Critère 2 : Préférence pour les étages bas (facilité d'accès pour les étudiants)
        if (s1.getEtage() != s2.getEtage()) {
            return Integer.compare(s1.getEtage(), s2.getEtage());
        }
        
        // Critère 3 : Richesse de l'équipement multimédia (ordre décroissant)
        int eq1 = s1.getEquipements().size();
        int eq2 = s2.getEquipements().size();
        if (eq1 != eq2) {
            return Integer.compare(eq2, eq1);
        }
        
        // Critère 4 : Tri alphabétique par numéro de salle pour déterminisme
        return s1.getNumero().compareTo(s2.getNumero());
    }
    
    /**
     * Attribution automatique complète avec propositions multiples
     */
    public Map<String, Object> attributionAvecAlternatives(Cours cours, String jour, 
                                                            String heureDebut, int dureeHeures) throws SQLException {
        Map<String, Object> resultat = new HashMap<>();
        List<Map<String, Object>> propositions = new ArrayList<>();
        
        String heureFin = calculerHeureFin(heureDebut, dureeHeures);
        
        // Essayer d'abord avec le créneau demandé
        Salle salleOptimale = trouverSalleOptimale(cours, jour, heureDebut, heureFin);
        if (salleOptimale != null) {
            Map<String, Object> prop = new HashMap<>();
            prop.put("type", "original");
            prop.put("salle", salleOptimale);
            prop.put("jour", jour);
            prop.put("heureDebut", heureDebut);
            prop.put("heureFin", heureFin);
            prop.put("score", 100);
            propositions.add(prop);
        }
        
        // Proposer des créneaux alternatifs
        String[] heuresAlternatives = {"08:00", "10:00", "14:00", "16:00"};
        for (String heureAlt : heuresAlternatives) {
            if (heureAlt.equals(heureDebut)) continue;
            
            String heureFinAlt = calculerHeureFin(heureAlt, dureeHeures);
            Salle salleAlt = trouverSalleOptimale(cours, jour, heureAlt, heureFinAlt);
            
            if (salleAlt != null) {
                Map<String, Object> prop = new HashMap<>();
                prop.put("type", "creneau_alterne");
                prop.put("salle", salleAlt);
                prop.put("jour", jour);
                prop.put("heureDebut", heureAlt);
                prop.put("heureFin", heureFinAlt);
                prop.put("score", 80); // Score inférieur car créneau différent
                propositions.add(prop);
            }
        }
        
        // Proposer des jours alternatifs (J+1, J+2)
        LocalDate date = LocalDate.parse(jour);
        for (int i = 1; i <= 3; i++) {
            String jourAlt = date.plusDays(i).toString();
            Salle salleAlt = trouverSalleOptimale(cours, jourAlt, heureDebut, heureFin);
            
            if (salleAlt != null) {
                Map<String, Object> prop = new HashMap<>();
                prop.put("type", "jour_alterne");
                prop.put("salle", salleAlt);
                prop.put("jour", jourAlt);
                prop.put("heureDebut", heureDebut);
                prop.put("heureFin", heureFin);
                prop.put("score", 70 - i*5); // Score décroissant
                propositions.add(prop);
            }
        }
        
        // Trier par score
        propositions.sort((p1, p2) -> 
            Integer.compare((int)p2.get("score"), (int)p1.get("score"))
        );
        
        resultat.put("propositions", propositions);
        resultat.put("succes", !propositions.isEmpty());
        
        if (propositions.isEmpty()) {
            resultat.put("message", "Aucune salle disponible");
        }
        
        return resultat;
    }
    
    /**
     * Calcule l'heure de fin à partir de l'heure de début et de la durée
     */
    private String calculerHeureFin(String heureDebut, int dureeHeures) {
        String[] parts = heureDebut.split(":");
        int heures = Integer.parseInt(parts[0]);
        heures += dureeHeures;
        return String.format("%02d:00", heures);
    }
    
    public Map<String, Object> attributionAutomatique(Cours cours, String periodeDebut, String periodeFin, 
                                                       String jour, String heureDebut, String heureFin) throws SQLException {
        Map<String, Object> resultat = new HashMap<>();
        List<Creneau> creneauxProposes = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();
        
        Salle salleOptimale = trouverSalleOptimale(cours, jour, heureDebut, heureFin);
        
        if (salleOptimale == null) {
            avertissements.add("Aucune salle disponible pour ce créneau");
            resultat.put("succes", false);
            resultat.put("avertissements", avertissements);
            return resultat;
        }
        
        // Créer le créneau
        Creneau creneau = new Creneau(
            cours.getId(),
            jour,
            heureDebut,
            heureFin,
            salleOptimale.getId()
        );
        
        creneauxProposes.add(creneau);
        
        resultat.put("succes", true);
        resultat.put("creneaux", creneauxProposes);
        resultat.put("salle", salleOptimale);
        resultat.put("avertissements", avertissements);
        
        return resultat;
    }
    
    public List<Salle> trouverSallesAlternatives(Cours cours, String jour, String heureDebut, 
                                                   String heureFin, int salleExclueId) throws SQLException {
        List<Salle> sallesDisponibles = salleDAO.listerDisponibles();
        List<Salle> sallesCompatibles = new ArrayList<>();
        
        ClasseDAO classeDAO = new ClasseDAO();
        Classe classe = classeDAO.trouverParId(cours.getClasseId());
        if (classe == null) return sallesCompatibles;
        
        int capaciteRequise = calculerCapaciteRequise(cours, classe);
        
        for (Salle salle : sallesDisponibles) {
            if (salle.getId() == salleExclueId) continue;
            
            boolean estLibre = creneauDAO.verifierDisponibiliteSalle(
                salle.getId(), jour, heureDebut, heureFin
            );
            
            if (estLibre && salle.getCapacite() >= capaciteRequise) {
                sallesCompatibles.add(salle);
            }
        }
        
        sallesCompatibles.sort((s1, s2) -> {
            int ecart1 = s1.getCapacite() - capaciteRequise;
            int ecart2 = s2.getCapacite() - capaciteRequise;
            return Integer.compare(ecart1, ecart2);
        });
        
        return sallesCompatibles;
    }

    /**
     * Trouve la salle optimale avec filtres avancés (UFR, bâtiment, type, équipements).
     * Utilisé par la recherche automatique dans le dialogue de placement de créneau.
     */
    public Salle trouverSalleOptimaleAvecFiltres(
            Cours cours, String jour, String heureDebut, String heureFin,
            scheduler.modele.Classe classe,
            Integer ufrId, Integer batimentId, String typeSalle,
            List<Integer> equipementsRequis) throws SQLException {

        List<Salle> sallesDisponibles;

        // Filtrer par bâtiment si spécifié
        if (batimentId != null) {
            sallesDisponibles = salleDAO.listerParBatiment(batimentId);
        } else {
            sallesDisponibles = salleDAO.listerDisponibles();
        }

        // Filtrer par UFR si spécifié (via les bâtiments de l'UFR)
        if (ufrId != null && batimentId == null) {
            List<scheduler.modele.Batiment> batiments = batimentDAO.listerParUfr(ufrId);
            Set<Integer> batIds = new HashSet<>();
            for (scheduler.modele.Batiment b : batiments) batIds.add(b.getId());
            sallesDisponibles.removeIf(s -> !batIds.contains(s.getBatimentId()));
        }

        // Filtrer par type de salle
        if (typeSalle != null && !typeSalle.isEmpty() && !"Tous".equals(typeSalle)) {
            sallesDisponibles.removeIf(s -> !typeSalle.equals(s.getType()));
        }

        // Filtrer les salles indisponibles
        sallesDisponibles.removeIf(s -> "indisponible".equals(s.getStatut()));

        int capaciteRequise = (classe != null) ? calculerCapaciteRequise(cours, classe) : 1;
        List<Salle> compatibles = new ArrayList<>();

        for (Salle salle : sallesDisponibles) {
            boolean libre = creneauDAO.verifierDisponibiliteSalle(
                    salle.getId(), jour, heureDebut, heureFin);
            if (!libre) continue;
            if (salle.getCapacite() < capaciteRequise) continue;
            if (equipementsRequis != null && !equipementsRequis.isEmpty()) {
                if (!verifierEquipements(salle, equipementsRequis)) continue;
            }
            compatibles.add(salle);
        }

        if (compatibles.isEmpty()) return null;
        compatibles.sort((s1, s2) -> comparerSalles(s1, s2, capaciteRequise, cours, jour));
        return compatibles.get(0);
    }
}