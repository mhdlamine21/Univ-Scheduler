package scheduler.dao;

import scheduler.modele.Cours;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données des cours.
 */
public class CoursDAO {
    
    public void ajouter(Cours cours) throws SQLException {
        String sql = "INSERT INTO cours (matiere_id, enseignant_id, classe_id, type_cours, groupes, volume_horaire) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, cours.getMatiereId());
            stmt.setInt(2, cours.getEnseignantId());
            stmt.setInt(3, cours.getClasseId());
            stmt.setString(4, cours.getTypeCours());
            stmt.setString(5, cours.getGroupes());
            stmt.setInt(6, cours.getVolumeHoraire());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                cours.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Cours trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM cours WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Cours cours = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                cours = creerCoursDepuisResultSet(rs);
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return cours;
    }
    
    public List<Cours> listerTous() throws SQLException {
        List<Cours> liste = new ArrayList<>();
        String sql = "SELECT * FROM cours ORDER BY id";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerCoursDepuisResultSet(rs));
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
    
    public List<Cours> listerParClasse(int classeId) throws SQLException {
        List<Cours> liste = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE classe_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, classeId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerCoursDepuisResultSet(rs));
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
    
    public List<Cours> listerParEnseignant(int enseignantId) throws SQLException {
        List<Cours> liste = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE enseignant_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, enseignantId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerCoursDepuisResultSet(rs));
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
    
    public void modifier(Cours cours) throws SQLException {
        String sql = "UPDATE cours SET matiere_id = ?, enseignant_id = ?, classe_id = ?, " +
                    "type_cours = ?, groupes = ?, volume_horaire = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setInt(1, cours.getMatiereId());
            stmt.setInt(2, cours.getEnseignantId());
            stmt.setInt(3, cours.getClasseId());
            stmt.setString(4, cours.getTypeCours());
            stmt.setString(5, cours.getGroupes());
            stmt.setInt(6, cours.getVolumeHoraire());
            stmt.setInt(7, cours.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            
            String sqlCreneaux = "DELETE FROM creneaux WHERE cours_id = ?";
            try (PreparedStatement stmtCreneaux = conn.prepareStatement(sqlCreneaux)) {
                stmtCreneaux.setInt(1, id);
                stmtCreneaux.executeUpdate();
            }
            
            String sql = "DELETE FROM cours WHERE id = ?";
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
    
    private Cours creerCoursDepuisResultSet(ResultSet rs) throws SQLException {
        Cours cours = new Cours();
        cours.setId(rs.getInt("id"));
        cours.setMatiereId(rs.getInt("matiere_id"));
        cours.setEnseignantId(rs.getInt("enseignant_id"));
        cours.setClasseId(rs.getInt("classe_id"));
        cours.setTypeCours(rs.getString("type_cours"));
        cours.setGroupes(rs.getString("groupes"));
        cours.setVolumeHoraire(rs.getInt("volume_horaire"));
        return cours;
    }
}