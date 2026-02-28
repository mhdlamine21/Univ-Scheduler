package scheduler.dao;

import scheduler.modele.Ufr;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux UFR (Unités de Formation et de Recherche).
 */
public class UfrDAO {
    
    private static volatile boolean colonnesVerifiees = false;

    private static void verifierColonnes(Connection conn) {
        if (colonnesVerifiees) return;
        synchronized (UfrDAO.class) {
            if (colonnesVerifiees) return;
            try (Statement stmt = conn.createStatement()) {
                try { stmt.execute("ALTER TABLE ufr ADD COLUMN code VARCHAR(50)"); } catch (Exception ignored) {}
                try { stmt.execute("ALTER TABLE ufr ADD COLUMN photo_url VARCHAR(255)"); } catch (Exception ignored) {}
                try { stmt.execute("ALTER TABLE ufr ADD COLUMN localisation VARCHAR(255)"); } catch (Exception ignored) {}
            } catch (Exception ignored) {}
            colonnesVerifiees = true;
        }
    }
    
    public void ajouter(Ufr ufr) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            verifierColonnes(conn);
            String sql = "INSERT INTO ufr (nom, code, description, photo_url, localisation) VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, ufr.getNom());
            stmt.setString(2, ufr.getCode());
            stmt.setString(3, ufr.getDescription());
            stmt.setString(4, ufr.getPhotoUrl());
            stmt.setString(5, ufr.getLocalisation());
            
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                ufr.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            // Fallback si la table ufr n'a pas encore toutes les colonnes
            try {
                if (stmt != null) stmt.close();
                String fallbackSql = "INSERT INTO ufr (nom, description) VALUES (?, ?)";
                stmt = conn.prepareStatement(fallbackSql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, ufr.getNom());
                stmt.setString(2, ufr.getDescription());
                stmt.executeUpdate();
                rs = stmt.getGeneratedKeys();
                if (rs.next()) ufr.setId(rs.getInt(1));
            } catch (SQLException ex) {
                throw e;
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Ufr trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM ufr WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Ufr ufr = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                ufr = creerUfrDepuisResultSet(rs);
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return ufr;
    }
    
    public List<Ufr> listerTous() throws SQLException {
        List<Ufr> liste = new ArrayList<>();
        String sql = "SELECT * FROM ufr ORDER BY nom";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerUfrDepuisResultSet(rs));
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
    
    public void modifier(Ufr ufr) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            verifierColonnes(conn);
            String sql = "UPDATE ufr SET nom = ?, code = ?, description = ?, photo_url = ?, localisation = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, ufr.getNom());
            stmt.setString(2, ufr.getCode());
            stmt.setString(3, ufr.getDescription());
            stmt.setString(4, ufr.getPhotoUrl());
            stmt.setString(5, ufr.getLocalisation());
            stmt.setInt(6, ufr.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            // Fallback si la colonne n'existe pas encore
            try {
                if (stmt != null) stmt.close();
                String fallbackSql = "UPDATE ufr SET nom = ?, description = ? WHERE id = ?";
                stmt = conn.prepareStatement(fallbackSql);
                stmt.setString(1, ufr.getNom());
                stmt.setString(2, ufr.getDescription());
                stmt.setInt(3, ufr.getId());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                throw e;
            }
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM ufr WHERE id = ?";
        
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
    
    private Ufr creerUfrDepuisResultSet(ResultSet rs) throws SQLException {
        Ufr ufr = new Ufr();
        ufr.setId(rs.getInt("id"));
        ufr.setNom(rs.getString("nom"));
        try { ufr.setDescription(rs.getString("description")); } catch (Exception ignored) {}
        try { ufr.setPhotoUrl(rs.getString("photo_url")); } catch (Exception ignored) {}
        try { ufr.setCode(rs.getString("code")); } catch (Exception ignored) {}
        try { ufr.setLocalisation(rs.getString("localisation")); } catch (Exception ignored) {}
        return ufr;
    }
}