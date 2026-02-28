package scheduler.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Liaison entre emplois du temps et créneaux.
 */
public class EmploiDuTempsCreneauDAO {

    public void ajouterCreneau(int emploiId, int creneauId) throws SQLException {
        String sql = "INSERT INTO emploi_du_temps_creneaux (emploi_id, creneau_id) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, emploiId);
            stmt.setInt(2, creneauId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }

    public void supprimerCreneau(int emploiId, int creneauId) throws SQLException {
        String sql = "DELETE FROM emploi_du_temps_creneaux WHERE emploi_id = ? AND creneau_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, emploiId);
            stmt.setInt(2, creneauId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }

    public void supprimerTousCreneaux(int emploiId) throws SQLException {
        String sql = "DELETE FROM emploi_du_temps_creneaux WHERE emploi_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, emploiId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }

    /**
     * Retourne la liste des IDs de créneaux liés à un emploi du temps.
     */
    public List<Integer> listerCreneauxParEdt(int emploiId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT creneau_id FROM emploi_du_temps_creneaux WHERE emploi_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, emploiId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("creneau_id"));
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs   != null) try { rs.close();   } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return ids;
    }

    public boolean existeCreneau(int emploiId, int creneauId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM emploi_du_temps_creneaux WHERE emploi_id = ? AND creneau_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, emploiId);
            stmt.setInt(2, creneauId);
            rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs   != null) try { rs.close();   } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return false;
    }

    public int compterCreneaux(int emploiId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM emploi_du_temps_creneaux WHERE emploi_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, emploiId);
            rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs   != null) try { rs.close();   } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return 0;
    }
}