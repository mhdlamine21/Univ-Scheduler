package scheduler.service;

import scheduler.dao.ConnexionBD;
import scheduler.modele.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service centralisé pour l'audit et l'historique légal des opérations.
 */
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audit-writer");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean tableVerifiee = false;

    private static void initialiserTable(Connection conn) {
        if (tableVerifiee) return;
        String sql = "CREATE TABLE IF NOT EXISTS audit_logs (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "timestamp DATETIME NOT NULL, " +
                "utilisateur_email VARCHAR(150), " +
                "role VARCHAR(50), " +
                "action VARCHAR(80) NOT NULL, " +
                "entite VARCHAR(80) NOT NULL, " +
                "entite_id INT DEFAULT 0, " +
                "details TEXT, " +
                "adresse_ip VARCHAR(50)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            tableVerifiee = true;
        } catch (SQLException e) {
            logger.warn("Vérification table audit_logs : {}", e.getMessage());
        }
    }

    /**
     * Enregistre un événement de façon asynchrone non-bloquante avec email et rôle.
     */
    public static void log(String utilisateurEmail, String role, String action, String entite, int entiteId, String details) {
        executor.submit(() -> {
            Connection conn = null;
            try {
                conn = ConnexionBD.getConnection();
                if (conn == null) return;
                initialiserTable(conn);

                String host = "127.0.0.1";
                try {
                    host = InetAddress.getLocalHost().getHostName();
                } catch (Exception ignored) {}

                String sql = "INSERT INTO audit_logs (timestamp, utilisateur_email, role, action, entite, entite_id, details, adresse_ip) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setString(2, utilisateurEmail != null ? utilisateurEmail : "systeme");
                    ps.setString(3, role != null ? role : "visiteur");
                    ps.setString(4, action);
                    ps.setString(5, entite);
                    ps.setInt(6, entiteId);
                    ps.setString(7, details);
                    ps.setString(8, host);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.warn("Échec d'enregistrement d'audit : {}", e.getMessage());
            } finally {
                if (conn != null) ConnexionBD.libererConnection(conn);
            }
        });
    }

    /**
     * Surcharge asynchrone simplifiée pour journaliser une action d'un utilisateur par ID.
     */
    public static void logAsync(int utilisateurId, String entite, String action, String details) {
        log("user#" + utilisateurId, "utilisateur", action, entite, 0, details);
    }

    /**
     * Récupère les N derniers logs d'audit.
     */
    public static List<AuditLog> listerDerniersLogs(int limite) {
        List<AuditLog> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return list;
            initialiserTable(conn);

            String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AuditLog log = new AuditLog();
                        log.setId(rs.getInt("id"));
                        Timestamp ts = rs.getTimestamp("timestamp");
                        if (ts != null) log.setTimestamp(ts.toLocalDateTime());
                        log.setUtilisateurEmail(rs.getString("utilisateur_email"));
                        log.setRole(rs.getString("role"));
                        log.setAction(rs.getString("action"));
                        log.setEntite(rs.getString("entite"));
                        log.setEntiteId(rs.getInt("entite_id"));
                        log.setDetails(rs.getString("details"));
                        log.setAdresseIp(rs.getString("adresse_ip"));
                        list.add(log);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lecture audit logs : {}", e.getMessage());
        } finally {
            if (conn != null) ConnexionBD.libererConnection(conn);
        }
        return list;
    }
}
