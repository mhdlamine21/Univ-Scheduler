package scheduler.service;

import scheduler.dao.EquipementDAO;
import scheduler.dao.SalleDAO;
import scheduler.modele.Equipement;
import scheduler.modele.Salle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Service métier pour Equipement.
 */
public class EquipementService {
    private static final Logger logger = LoggerFactory.getLogger(EquipementService.class);
    private EquipementDAO equipementDAO;
    private SalleDAO salleDAO;
    
    public EquipementService() {
        this.equipementDAO = new EquipementDAO();
        this.salleDAO = new SalleDAO();
    }
    
    /**
     * Ajoute un nouvel équipement
     */
    public void ajouter(Equipement equipement) throws SQLException {
        try {
            if (equipement.getQuantite() < 0) {
                throw new SQLException("La quantité ne peut pas être négative");
            }
            
            if (nomExiste(equipement.getNom())) {
                throw new SQLException("Un équipement avec le nom '" + equipement.getNom() + "' existe déjà");
            }
            
            equipementDAO.ajouter(equipement);
                       
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de l'ajout de l'équipement {}", equipement.getNom(), e);
            throw e;
        }
    }
    
    /**
     * Trouve un équipement par son ID
     */
    public Equipement trouverParId(int id) throws SQLException {
        try {
            Equipement equipement = equipementDAO.trouverParId(id);
            if (equipement != null) {
            } else {
            }
            return equipement;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la recherche de l'équipement ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Trouve un équipement par son nom
     */
    public Equipement trouverParNom(String nom) throws SQLException {
        try {
            List<Equipement> tous = listerTous();
            return tous.stream()
                .filter(e -> e.getNom().equalsIgnoreCase(nom))
                .findFirst()
                .orElse(null);
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la recherche de l'équipement par nom {}", nom, e);
            throw e;
        }
    }
    
    /**
     * Vérifie si un nom d'équipement existe déjà
     */
    public boolean nomExiste(String nom) throws SQLException {
        return trouverParNom(nom) != null;
    }
    
    /**
     * Vérifie si un nom d'équipement existe déjà (sauf pour un ID donné)
     */
    public boolean nomExisteSauf(String nom, int id) throws SQLException {
        Equipement existant = trouverParNom(nom);
        return existant != null && existant.getId() != id;
    }
    
    /**
     * Liste tous les équipements
     */
    public List<Equipement> listerTous() throws SQLException {
        try {
            List<Equipement> equipements = equipementDAO.listerTous();
            return equipements;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du chargement des équipements", e);
            throw e;
        }
    }
    
    /**
     * Liste les équipements d'une salle spécifique
     */
    public List<Equipement> listerParSalle(int salleId) throws SQLException {
        try {
            List<Equipement> equipements = equipementDAO.listerParSalle(salleId);
            return equipements;
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du chargement des équipements pour la salle ID {}", salleId, e);
            throw e;
        }
    }
    
    /**
     * Calcule les quantités utilisées pour chaque équipement
     */
    public Map<Integer, Integer> calculerQuantitesUtilisees() throws SQLException {
        Map<Integer, Integer> utilisation = new HashMap<>();
        
        try {
            List<Salle> toutesSalles = salleDAO.listerTous();
            
            for (Salle salle : toutesSalles) {
                for (int equipId : salle.getEquipements()) {
                    utilisation.put(equipId, utilisation.getOrDefault(equipId, 0) + 1);
                }
            }
            return utilisation;
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du calcul des quantités utilisées", e);
            throw e;
        }
    }
    
    /**
     * Calcule la quantité disponible pour un équipement
     */
    public int getQuantiteDisponible(int equipementId) throws SQLException {
        Equipement equipement = trouverParId(equipementId);
        if (equipement == null) return 0;
        
        int utilise = 0;
        List<Salle> toutesSalles = salleDAO.listerTous();
        
        for (Salle salle : toutesSalles) {
            for (int id : salle.getEquipements()) {
                if (id == equipementId) utilise++;
            }
        }
        
        return equipement.getQuantite() - utilise;
    }
    
    /**
     * Calcule la quantité disponible pour tous les équipements
     */
    public Map<Integer, Integer> calculerQuantitesDisponibles() throws SQLException {
        Map<Integer, Integer> disponibles = new HashMap<>();
        Map<Integer, Integer> utilisees = calculerQuantitesUtilisees();
        List<Equipement> tous = listerTous();
        
        for (Equipement e : tous) {
            int utilise = utilisees.getOrDefault(e.getId(), 0);
            disponibles.put(e.getId(), e.getQuantite() - utilise);
        }
        
        return disponibles;
    }
    
    /**
     * Vérifie si un équipement est utilisé dans des salles
     */
    public boolean estUtilise(int equipementId) throws SQLException {
        try {
            List<Salle> toutesSalles = salleDAO.listerTous();
            
            for (Salle salle : toutesSalles) {
                if (salle.getEquipements().contains(equipementId)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la vérification d'utilisation de l'équipement ID {}", equipementId, e);
            throw e;
        }
    }
    
    /**
     * Compte le nombre de salles utilisant un équipement
     */
    public int compterUtilisations(int equipementId) throws SQLException {
        int count = 0;
        
        try {
            List<Salle> toutesSalles = salleDAO.listerTous();
            
            for (Salle salle : toutesSalles) {
                if (salle.getEquipements().contains(equipementId)) {
                    count++;
                }
            }
            
            return count;
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors du comptage des utilisations de l'équipement ID {}", equipementId, e);
            throw e;
        }
    }
    
    /**
     * Modifie un équipement
     */
    public void modifier(Equipement equipement) throws SQLException {
        try {
            if (equipement.getNom() == null || equipement.getNom().trim().isEmpty()) {
                throw new SQLException("Le nom de l'équipement ne peut pas être vide");
            }
            
            if (equipement.getQuantite() < 0) {
                throw new SQLException("La quantité ne peut pas être négative");
            }
            
            if (nomExisteSauf(equipement.getNom(), equipement.getId())) {
                throw new SQLException("Un autre équipement avec le nom '" + equipement.getNom() + "' existe déjà");
            }
            
            int utilisations = compterUtilisations(equipement.getId());
            
            if (equipement.getQuantite() < utilisations) {
                throw new SQLException(
                    String.format("Impossible de réduire la quantité à %d car l'équipement est utilisé dans %d salle(s)",
                    equipement.getQuantite(), utilisations)
                );
            }
            
            equipementDAO.modifier(equipement);
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la modification de l'équipement ID {}", equipement.getId(), e);
            throw e;
        }
    }
    
    /**
     * Supprime un équipement
     */
    public void supprimer(int id) throws SQLException {
        try {
            if (estUtilise(id)) {
                int utilisations = compterUtilisations(id);
                throw new SQLException(
                    String.format("Impossible de supprimer : l'équipement est utilisé dans %d salle(s)", 
                    utilisations)
                );
            }
            
            equipementDAO.supprimer(id);
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la suppression de l'équipement ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Met à jour la quantité d'un équipement
     */
    public void updateQuantite(int id, int nouvelleQuantite) throws SQLException {
        try {
            Equipement equipement = trouverParId(id);
            if (equipement == null) {
                throw new SQLException("Équipement non trouvé");
            }
            
            equipement.setQuantite(nouvelleQuantite);
            modifier(equipement);
            
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la mise à jour de la quantité de l'équipement ID {}", id, e);
            throw e;
        }
    }
    
    /**
     * Augmente la quantité d'un équipement
     */
    public void augmenterQuantite(int id, int increment) throws SQLException {
        Equipement equipement = trouverParId(id);
        if (equipement == null) {
            throw new SQLException("Équipement non trouvé");
        }
        
        updateQuantite(id, equipement.getQuantite() + increment);
    }
    
    /**
     * Diminue la quantité d'un équipement
     */
    public void diminuerQuantite(int id, int decrement) throws SQLException {
        Equipement equipement = trouverParId(id);
        if (equipement == null) {
            throw new SQLException("Équipement non trouvé");
        }
        
        int utilisations = compterUtilisations(id);
        int nouvelleQuantite = equipement.getQuantite() - decrement;
        
        if (nouvelleQuantite < utilisations) {
            throw new SQLException(
                String.format("Impossible de diminuer la quantité à %d (minimum requis: %d)", 
                nouvelleQuantite, utilisations)
            );
        }
        
        updateQuantite(id, nouvelleQuantite);
    }
    
    /**
     * Recherche des équipements par critères
     */
    public List<Equipement> rechercher(String recherche) throws SQLException {
        List<Equipement> tous = listerTous();
        
        if (recherche == null || recherche.trim().isEmpty()) {
            return tous;
        }
        
        String rech = recherche.toLowerCase().trim();
        
        return tous.stream()
            .filter(e -> 
                e.getNom().toLowerCase().contains(rech) ||
                (e.getDescription() != null && e.getDescription().toLowerCase().contains(rech))
            )
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère les statistiques des équipements
     */
    public Map<String, Object> getStatistiques() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        
        List<Equipement> tous = listerTous();
        Map<Integer, Integer> utilisations = calculerQuantitesUtilisees();
        Map<Integer, Integer> disponibles = calculerQuantitesDisponibles();
        
        int totalTypes = tous.size();
        int totalQuantite = tous.stream().mapToInt(Equipement::getQuantite).sum();
        int totalUtilise = utilisations.values().stream().mapToInt(Integer::intValue).sum();
        int totalDisponible = totalQuantite - totalUtilise;
        
        stats.put("totalTypes", totalTypes);
        stats.put("totalQuantite", totalQuantite);
        stats.put("totalUtilise", totalUtilise);
        stats.put("totalDisponible", totalDisponible);
        
        // Équipements en rupture ou presque
        List<Map<String, Object>> enRupture = new ArrayList<>();
        List<Map<String, Object>> stockFaible = new ArrayList<>();
        
        for (Equipement e : tous) {
            int utilise = utilisations.getOrDefault(e.getId(), 0);
            int dispo = e.getQuantite() - utilise;
            
            Map<String, Object> info = new HashMap<>();
            info.put("id", e.getId());
            info.put("nom", e.getNom());
            info.put("quantite", e.getQuantite());
            info.put("utilise", utilise);
            info.put("disponible", dispo);
            
            if (dispo <= 0) {
                enRupture.add(info);
            } else if (dispo <= 2) {
                stockFaible.add(info);
            }
        }
        
        stats.put("enRupture", enRupture);
        stats.put("stockFaible", stockFaible);
        
        // Les 5 équipements les plus utilisés
        List<Map<String, Object>> plusUtilises = utilisations.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5)
            .map(entry -> {
                try {
                    Equipement e = trouverParId(entry.getKey());
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", entry.getKey());
                    info.put("nom", e != null ? e.getNom() : "Inconnu");
                    info.put("utilisations", entry.getValue());
                    return info;
                } catch (SQLException ex) {
                    return null;
                }
            })
            .filter(m -> m != null)
            .collect(Collectors.toList());
        
        stats.put("plusUtilises", plusUtilises);
        
        return stats;
    }
    
    /**
     * Vérifie si un équipement peut être retiré d'une salle
     */
    public boolean peutRetirer(int equipementId, int salleId) throws SQLException {
        List<Equipement> salleEquipements = listerParSalle(salleId);
        
        int count = 0;
        for (Equipement e : salleEquipements) {
            if (e.getId() == equipementId) count++;
        }
        
        return count > 0;
    }
}