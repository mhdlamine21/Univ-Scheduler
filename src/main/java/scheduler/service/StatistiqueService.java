package scheduler.service;

import scheduler.dao.*;
import scheduler.modele.*;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service analytique responsable du calcul des indicateurs de performance et d'occupation.
 * 
 * Fournit les agrégations pour les graphiques décisionnels :
 * - Répartition démographique des comptes utilisateurs par rôle institutionnel.
 * - Courbes et histogrammes d'occupation des salles par créneau horaire sur 7 jours roulants.
 * - Taux d'utilisation effectif par type de salle (Amphis, TD, TP).
 * - Volumes de réservations et de signalements pour les bilans de direction.
 */
public class StatistiqueService {

    private UtilisateurDAO utilisateurDAO;
    private SalleDAO       salleDAO;
    private CoursDAO       coursDAO;

    /**
     * Instancie le service statistique et initialise l'accès aux tables sources.
     */
    public StatistiqueService() {
        this.utilisateurDAO = new UtilisateurDAO();
        this.salleDAO       = new SalleDAO();
        this.coursDAO       = new CoursDAO();
    }

    /**
     * Calcule l'ensemble des métriques de synthèse affichées sur les tableaux de bord.
     * @return une Map contenant les totaux, ventilations et histogrammes
     * @throws SQLException en cas de rupture de communication avec la base de données
     */
    public Map<String, Object> getStatistiquesGlobales() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        Connection conn = null;
        
        try {
            conn = ConnexionBD.getConnection();

            // Statistiques utilisateurs (inchangées)
            Map<String, Integer> roles = new LinkedHashMap<>();
            roles.put("admin", 0); roles.put("gestionnaire", 0);
            roles.put("enseignant", 0); roles.put("etudiant", 0);
            try (PreparedStatement ps = conn.prepareStatement("SELECT role, COUNT(*) FROM utilisateurs GROUP BY role")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) roles.put(rs.getString("role"), rs.getInt(2));
                }
            }
            stats.put("utilisateursParRole", roles);
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM utilisateurs")) {
                try (ResultSet rs = ps.executeQuery()) { stats.put("totalUtilisateurs", rs.next() ? rs.getInt(1) : 0); }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM salles")) {
                try (ResultSet rs = ps.executeQuery()) { stats.put("totalSalles", rs.next() ? rs.getInt(1) : 0); }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM cours")) {
                try (ResultSet rs = ps.executeQuery()) { stats.put("totalCours", rs.next() ? rs.getInt(1) : 0); }
            }

            // ✅ CORRECTION : Occupation sur les 7 derniers jours (pas seulement aujourd'hui)
            Map<String, Integer> occupation = new LinkedHashMap<>();
            occupation.put("08h-10h", 0); occupation.put("10h-12h", 0);
            occupation.put("12h-14h", 0); occupation.put("14h-16h", 0);
            occupation.put("16h-18h", 0);
            
            // Prendre les 7 derniers jours
            String debutSemaine = LocalDate.now().minusDays(7).toString();
            String aujourdhui = LocalDate.now().toString();
            
            String sqlCreneaux = "SELECT heure_debut, COUNT(*) as nb FROM creneaux " +
                                 "WHERE jour BETWEEN ? AND ? " +
                                 "AND salle_id IS NOT NULL AND statut != 'annule' " +
                                 "GROUP BY heure_debut";
            
            try (PreparedStatement ps = conn.prepareStatement(sqlCreneaux)) {
                ps.setString(1, debutSemaine);
                ps.setString(2, aujourdhui);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String hd = rs.getString("heure_debut");
                        int nb = rs.getInt("nb");
                        int h = parseHeure(hd);
                        if (h >= 8 && h < 10) occupation.merge("08h-10h", nb, Integer::sum);
                        else if (h >= 10 && h < 12) occupation.merge("10h-12h", nb, Integer::sum);
                        else if (h >= 12 && h < 14) occupation.merge("12h-14h", nb, Integer::sum);
                        else if (h >= 14 && h < 16) occupation.merge("14h-16h", nb, Integer::sum);
                        else if (h >= 16 && h < 18) occupation.merge("16h-18h", nb, Integer::sum);
                    }
                }
            }
            stats.put("occupationParHeure", occupation);
            
            // Calcul du taux d'occupation global (7 jours)
            List<Salle> salles = salleDAO.listerTous();
            int totalOccupes = occupation.values().stream().mapToInt(v -> v).sum();
            int totalPossibles = salles.size() * 5 * 7; // 5 créneaux par jour × 7 jours
            double taux = totalPossibles > 0 ? (double) totalOccupes / totalPossibles * 100 : 0;
            stats.put("tauxOccupationGlobal", Math.round(taux * 100) / 100.0);

        } finally {
            ConnexionBD.libererConnection(conn);
        }
        return stats;
    }
    /**
     * Récupère les statistiques pour une période donnée.
     */
    public Map<String, Object> getRapportStats(LocalDate debut, LocalDate fin,
            Integer ufrId, Integer classeId) throws SQLException {

        Map<String, Object> stats = new HashMap<>();
        String dDebut = debut.toString();
        String dFin   = fin.toString();

        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();

            // Construire la condition SQL des filtres
            StringBuilder whereExtra = new StringBuilder();
            List<Object> params = new ArrayList<>();
            params.add(dDebut);
            params.add(dFin);

            if (ufrId != null) {
                whereExtra.append(
                    " AND cr.salle_id IN (" +
                    "  SELECT s.id FROM salles s " +
                    "  JOIN batiments b ON s.batiment_id = b.id " +
                    "  WHERE b.ufr_id = ?)");
                params.add(ufrId);
            }
            if (classeId != null) {
                whereExtra.append(
                    " AND cr.cours_id IN (" +
                    "  SELECT id FROM cours WHERE classe_id = ?)");
                params.add(classeId);
            }

            String baseWhere = "cr.jour BETWEEN ? AND ? " +
                               "AND cr.salle_id IS NOT NULL " +
                               "AND cr.statut != 'annule'" + whereExtra;

            Map<String, Integer> occupation = new LinkedHashMap<>();
            occupation.put("08h-10h", 0);
            occupation.put("10h-12h", 0);
            occupation.put("12h-14h", 0);
            occupation.put("14h-16h", 0);
            occupation.put("16h-18h", 0);

            String sqlOcc = "SELECT cr.heure_debut, COUNT(*) AS nb FROM creneaux cr " +
                            "WHERE " + baseWhere + " GROUP BY cr.heure_debut";
            try (PreparedStatement ps = conn.prepareStatement(sqlOcc)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int h = parseHeure(rs.getString("heure_debut"));
                        int nb = rs.getInt("nb");
                        if      (h >= 8  && h < 10) occupation.merge("08h-10h", nb, Integer::sum);
                        else if (h >= 10 && h < 12) occupation.merge("10h-12h", nb, Integer::sum);
                        else if (h >= 12 && h < 14) occupation.merge("12h-14h", nb, Integer::sum);
                        else if (h >= 14 && h < 16) occupation.merge("14h-16h", nb, Integer::sum);
                        else if (h >= 16 && h < 18) occupation.merge("16h-18h", nb, Integer::sum);
                    }
                }
            }
            stats.put("occupationParHeure", occupation);

            int totalOccupes = occupation.values().stream().mapToInt(v -> v).sum();
            int totalSalles  = compterSalles(conn, ufrId);
            long jours = java.time.temporal.ChronoUnit.DAYS.between(debut, fin) + 1;
            int totalPossibles = totalSalles * 5 * (int) jours;
            double taux = totalPossibles > 0 ? (double) totalOccupes / totalPossibles * 100 : 0;
            stats.put("tauxOccupationGlobal", Math.round(taux * 100) / 100.0);
            stats.put("totalCreneaux", totalOccupes);

            Map<String, Integer> repartition = new LinkedHashMap<>();
            repartition.put("CM", 0);
            repartition.put("TD", 0);
            repartition.put("TP", 0);

            String sqlType = "SELECT co.type_cours, COUNT(*) AS nb " +
                             "FROM creneaux cr " +
                             "JOIN cours co ON cr.cours_id = co.id " +
                             "WHERE " + baseWhere + " GROUP BY co.type_cours";
            try (PreparedStatement ps = conn.prepareStatement(sqlType)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("type_cours");
                        repartition.put(type, repartition.getOrDefault(type, 0) + rs.getInt("nb"));
                    }
                }
            }
            stats.put("coursParType", repartition);

            List<Map<String, Object>> detailsSalles = new ArrayList<>();
            String sqlSalles =
                "SELECT s.numero, s.type, s.capacite, s.statut, " +
                "       COUNT(cr.id) AS nb_creneaux " +
                "FROM salles s " +
                "LEFT JOIN creneaux cr ON cr.salle_id = s.id " +
                "   AND cr.jour BETWEEN ? AND ? " +
                "   AND cr.statut != 'annule' " +
                (classeId != null ?
                    "   AND cr.cours_id IN (SELECT id FROM cours WHERE classe_id = " + classeId + ") " : "") +
                (ufrId != null ?
                    "   AND s.batiment_id IN (SELECT id FROM batiments WHERE ufr_id = " + ufrId + ") " : "") +
                "GROUP BY s.id, s.numero, s.type, s.capacite, s.statut " +
                "ORDER BY nb_creneaux DESC";

            try (PreparedStatement ps = conn.prepareStatement(sqlSalles)) {
                ps.setString(1, dDebut);
                ps.setString(2, dFin);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if ("indisponible".equals(rs.getString("statut"))) continue;
                        int nb = rs.getInt("nb_creneaux");
                        double tauxSalle = totalPossibles > 0
                                ? (double) nb / (5.0 * jours) * 100 : 0;

                        Map<String, Object> info = new HashMap<>();
                        info.put("salle",    rs.getString("numero"));
                        info.put("type",     rs.getString("type"));
                        info.put("capacite", rs.getInt("capacite"));
                        info.put("taux",     Math.round(tauxSalle * 10) / 10.0);
                        info.put("heures",   nb * 2);

                        if (tauxSalle > 70)      info.put("statut", "surchargée");
                        else if (tauxSalle < 20) info.put("statut", "sous-utilisée");
                        else                     info.put("statut", "normale");

                        detailsSalles.add(info);
                    }
                }
            }
            stats.put("sallesCritiques", detailsSalles);

        } finally {
            ConnexionBD.libererConnection(conn);
        }

        return stats;
    }

    
    /**
     * Récupère l'évolution mensuelle des taux d'occupation.
     */
    public Map<String, Double> getEvolutionMensuelle() throws SQLException {
        Map<String, Double> evolution = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.FRENCH);

        // Initialiser les 12 mois avec 0
        for (int i = 11; i >= 0; i--) {
            LocalDate m = now.minusMonths(i).withDayOfMonth(1);
            evolution.put(m.format(fmt), 0.0);
        }

        LocalDate debut12 = now.minusMonths(11).withDayOfMonth(1);
        String dDebut = debut12.toString();
        String dFin = now.toString();

        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();

            int totalSalles = compterSalles(conn, null);
            if (totalSalles == 0) return evolution;

            // ✅ CORRECTION : Utiliser SUBSTR au lieu de DATE_FORMAT pour compatibilité
            String sql = "SELECT SUBSTR(jour, 1, 7) AS mois, COUNT(*) AS nb " +
                         "FROM creneaux " +
                         "WHERE jour BETWEEN ? AND ? " +
                         "  AND salle_id IS NOT NULL " +
                         "  AND statut != 'annule' " +
                         "GROUP BY SUBSTR(jour, 1, 7) " +
                         "ORDER BY mois";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, dDebut);
                ps.setString(2, dFin);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String moisKey = rs.getString("mois"); // "2024-09"
                        int nb = rs.getInt("nb");

                        String[] parts = moisKey.split("-");
                        int annee = Integer.parseInt(parts[0]);
                        int moisNum = Integer.parseInt(parts[1]);
                        LocalDate debutMois = LocalDate.of(annee, moisNum, 1);
                        LocalDate finMois = debutMois.plusMonths(1).minusDays(1);
                        int joursOuvres = (int) java.time.temporal.ChronoUnit.DAYS.between(debutMois, finMois) + 1;

                        int possible = totalSalles * 5 * joursOuvres;
                        double tauxMois = possible > 0 ? (double) nb / possible * 100 : 0;

                        String cle = debutMois.format(fmt);
                        if (evolution.containsKey(cle)) {
                            evolution.put(cle, Math.round(tauxMois * 100) / 100.0);
                        }
                    }
                }
            }
        } finally {
            ConnexionBD.libererConnection(conn);
        }
        return evolution;
    }

    
    private int compterSalles(Connection conn, Integer ufrId) throws SQLException {
        String sql = ufrId != null
            ? "SELECT COUNT(*) FROM salles s JOIN batiments b ON s.batiment_id = b.id WHERE b.ufr_id = ?"
            : "SELECT COUNT(*) FROM salles";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (ufrId != null) ps.setInt(1, ufrId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int parseHeure(String heureDebut) {
        try { return Integer.parseInt(heureDebut.split(":")[0]); }
        catch (Exception e) { return 0; }
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof String)  ps.setString(i + 1, (String) p);
            else if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
        }
    }
}