package scheduler.dao;

import scheduler.modele.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la persistance des notifications dans la base de données MySQL.
 */
public class NotificationDAO {

    public void ajouter(Notification notif) throws SQLException {
        String sql = "INSERT INTO notifications (utilisateur_id, message, est_lue, date_creation) VALUES (?, ?, ?, NOW())";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, notif.getUtilisateurId());
            stmt.setString(2, notif.getMessage());
            stmt.setBoolean(3, notif.isEstLue());
            stmt.executeUpdate();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                notif.setId(rs.getInt(1));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
    }

    public List<Notification> listerParUtilisateur(int utilisateurId) throws SQLException {
        List<Notification> liste = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE utilisateur_id = ? ORDER BY date_creation DESC";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, utilisateurId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                liste.add(creerDepuisResultSet(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }

    public List<Notification> listerNonLues(int utilisateurId) throws SQLException {
        List<Notification> liste = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE utilisateur_id = ? AND est_lue = FALSE ORDER BY date_creation DESC";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, utilisateurId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                liste.add(creerDepuisResultSet(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }

    public int compterNonLues(int utilisateurId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE utilisateur_id = ? AND est_lue = FALSE";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, utilisateurId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
    }

    public void marquerCommeLue(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET est_lue = TRUE WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, notificationId);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
    }

    public void marquerToutesCommeLues(int utilisateurId) throws SQLException {
        String sql = "UPDATE notifications SET est_lue = TRUE WHERE utilisateur_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, utilisateurId);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
    }

    public void supprimer(int notificationId) throws SQLException {
        String sql = "DELETE FROM notifications WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, notificationId);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
    }

    public void supprimerToutes(int utilisateurId) throws SQLException {
        String sql = "DELETE FROM notifications WHERE utilisateur_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, utilisateurId);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
    }

    private Notification creerDepuisResultSet(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUtilisateurId(rs.getInt("utilisateur_id"));
        n.setMessage(rs.getString("message"));
        n.setEstLue(rs.getBoolean("est_lue"));
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            n.setDateCreation(ts.toLocalDateTime());
        }
        return n;
    }
}
