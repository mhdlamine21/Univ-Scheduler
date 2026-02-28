package scheduler.dao;

import scheduler.modele.Matiere;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux matières enseignées.
 */
public class MatiereDAO {
    
    public void ajouter(Matiere matiere) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "INSERT INTO matieres (nom, code, filiere, volume_horaire, description) VALUES (?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, matiere.getNom());
            stmt.setString(2, matiere.getCode());
            stmt.setString(3, matiere.getFiliere());
            stmt.setInt(4, matiere.getVolumeHoraire());
            stmt.setString(5, matiere.getDescription());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                matiere.setId(rs.getInt(1));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Matiere trouverParId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Matiere resultat = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM matieres WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                resultat = creerMatiereDepuisResultSet(rs);
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return resultat;
    }
    
    public Matiere trouverParCode(String code) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Matiere resultat = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM matieres WHERE code = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, code);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                resultat = creerMatiereDepuisResultSet(rs);
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return resultat;
    }
    
    public List<Matiere> listerToutes() throws SQLException {
        List<Matiere> liste = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM matieres ORDER BY filiere, nom";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerMatiereDepuisResultSet(rs));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public List<Matiere> listerParFiliere(String filiere) throws SQLException {
        List<Matiere> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM matieres WHERE filiere = ? ORDER BY nom";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, filiere);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerMatiereDepuisResultSet(rs));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public void modifier(Matiere matiere) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE matieres SET nom = ?, code = ?, filiere = ?, volume_horaire = ?, description = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, matiere.getNom());
            stmt.setString(2, matiere.getCode());
            stmt.setString(3, matiere.getFiliere());
            stmt.setInt(4, matiere.getVolumeHoraire());
            stmt.setString(5, matiere.getDescription());
            stmt.setInt(6, matiere.getId());
            stmt.executeUpdate();
            
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
            String sql = "DELETE FROM matieres WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private Matiere creerMatiereDepuisResultSet(ResultSet rs) throws SQLException {
        Matiere matiere = new Matiere();
        matiere.setId(rs.getInt("id"));
        matiere.setNom(rs.getString("nom"));
        matiere.setCode(rs.getString("code"));
        matiere.setFiliere(rs.getString("filiere"));
        matiere.setVolumeHoraire(rs.getInt("volume_horaire"));
        matiere.setDescription(rs.getString("description"));
        return matiere;
    }
}