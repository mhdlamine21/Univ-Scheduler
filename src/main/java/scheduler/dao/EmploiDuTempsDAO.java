package scheduler.dao;

import scheduler.modele.EmploiDuTemps;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux emplois du temps.
 */
public class EmploiDuTempsDAO {
    
    public void ajouter(EmploiDuTemps edt) throws SQLException {
        String sql = "INSERT INTO emplois_du_temps (classe_id, periode_type, periode_debut, periode_fin, " +
                     "est_valide, statut, heures_config, jours_config) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, edt.getClasseId());
            stmt.setString(2, edt.getPeriodeType());
            stmt.setString(3, edt.getPeriodeDebut());
            stmt.setString(4, edt.getPeriodeFin());
            stmt.setBoolean(5, edt.isEstValide());
            stmt.setString(6, edt.getStatut() != null ? edt.getStatut() : "en_attente");
            stmt.setString(7, edt.getHeuresConfig());
            stmt.setString(8, edt.getJoursConfig());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                edt.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public EmploiDuTemps trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM emplois_du_temps WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return creerEmploiDuTempsDepuisResultSet(rs);
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return null;
    }
    
    public List<EmploiDuTemps> listerParClasse(int classeId) throws SQLException {
        List<EmploiDuTemps> liste = new ArrayList<>();
        String sql = "SELECT * FROM emplois_du_temps WHERE classe_id = ? ORDER BY periode_debut DESC";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, classeId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerEmploiDuTempsDepuisResultSet(rs));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public List<EmploiDuTemps> listerTous() throws SQLException {
        List<EmploiDuTemps> liste = new ArrayList<>();
        String sql = "SELECT * FROM emplois_du_temps ORDER BY periode_debut DESC";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerEmploiDuTempsDepuisResultSet(rs));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public List<EmploiDuTemps> listerActifs() throws SQLException {
        List<EmploiDuTemps> liste = new ArrayList<>();
        String sql = "SELECT * FROM emplois_du_temps WHERE statut IN ('en_attente', 'en_cours') ORDER BY periode_debut DESC";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerEmploiDuTempsDepuisResultSet(rs));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public void valider(int id) throws SQLException {
        String sql = "UPDATE emplois_du_temps SET est_valide = TRUE, statut = 'en_cours', date_validation = CURDATE() WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void marquerTermine(int id) throws SQLException {
        String sql = "UPDATE emplois_du_temps SET statut = 'termine' WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifier(EmploiDuTemps edt) throws SQLException {
        String sql = "UPDATE emplois_du_temps SET classe_id = ?, periode_type = ?, periode_debut = ?, " +
                     "periode_fin = ?, est_valide = ?, statut = ?, heures_config = ?, jours_config = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setInt(1, edt.getClasseId());
            stmt.setString(2, edt.getPeriodeType());
            stmt.setString(3, edt.getPeriodeDebut());
            stmt.setString(4, edt.getPeriodeFin());
            stmt.setBoolean(5, edt.isEstValide());
            stmt.setString(6, edt.getStatut());
            stmt.setString(7, edt.getHeuresConfig());
            stmt.setString(8, edt.getJoursConfig());
            stmt.setInt(9, edt.getId());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM emplois_du_temps WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private EmploiDuTemps creerEmploiDuTempsDepuisResultSet(ResultSet rs) throws SQLException {
        EmploiDuTemps edt = new EmploiDuTemps();
        edt.setId(rs.getInt("id"));
        edt.setClasseId(rs.getInt("classe_id"));
        edt.setPeriodeType(rs.getString("periode_type"));
        edt.setPeriodeDebut(rs.getString("periode_debut"));
        edt.setPeriodeFin(rs.getString("periode_fin"));
        edt.setEstValide(rs.getBoolean("est_valide"));
        String statut = rs.getString("statut");
        edt.setStatut(statut != null ? statut : "en_attente");
        
        edt.setHeuresConfig(rs.getString("heures_config"));
        edt.setJoursConfig(rs.getString("jours_config"));
        
        java.sql.Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            edt.setDateCreation(ts.toLocalDateTime());
        }
        return edt;
    }

    public void mettreAJourTitre(int id, String titre) throws SQLException {
        String sql = "UPDATE emplois_du_temps SET titre = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, titre);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            ConnexionBD.libererConnection(conn);
        }
    }

    public String getTitre(int id) throws SQLException {
        String sql = "SELECT titre FROM emplois_du_temps WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String titre = rs.getString("titre");
                return titre != null ? titre : "Emploi du temps #" + id;
            }
        } catch (SQLException e) {
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            ConnexionBD.libererConnection(conn);
        }
        return "Emploi du temps #" + id;
    }
}