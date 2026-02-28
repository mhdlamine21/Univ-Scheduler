package scheduler.dao;

import scheduler.modele.Classe;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données des classes.
 */
public class ClasseDAO {
    
    public void ajouter(Classe classe) throws SQLException {
        String sql = "INSERT INTO classes (intitule, filiere, niveau, annee_scolaire, ufr_id, effectif, nb_groupes, est_active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, classe.getIntitule());
            stmt.setString(2, classe.getFiliere());
            stmt.setString(3, classe.getNiveau());
            stmt.setString(4, classe.getAnneeScolaire());
            stmt.setInt(5, classe.getUfrId());
            stmt.setInt(6, classe.getEffectif());
            stmt.setInt(7, classe.getNbGroupes());
            stmt.setBoolean(8, classe.isEstActive());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                classe.setId(rs.getInt(1));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Classe trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM classes WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Classe classe = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                classe = creerClasseDepuisResultSet(rs);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return classe;
    }
    
    public List<Classe> listerToutes() throws SQLException {
        List<Classe> liste = new ArrayList<>();
        String sql = "SELECT * FROM classes WHERE est_active = TRUE ORDER BY annee_scolaire DESC, intitule";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerClasseDepuisResultSet(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public List<Classe> listerParUfr(int ufrId) throws SQLException {
        List<Classe> liste = new ArrayList<>();
        String sql = "SELECT * FROM classes WHERE ufr_id = ? AND est_active = TRUE ORDER BY intitule";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, ufrId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerClasseDepuisResultSet(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public List<Classe> listerParFiliere(String filiere) throws SQLException {
        List<Classe> liste = new ArrayList<>();
        String sql = "SELECT * FROM classes WHERE filiere = ? AND est_active = TRUE ORDER BY niveau";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, filiere);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerClasseDepuisResultSet(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public List<String> listerFilieres() throws SQLException {
        List<String> liste = new ArrayList<>();
        String sql = "SELECT DISTINCT filiere FROM classes WHERE est_active = TRUE ORDER BY filiere";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(rs.getString("filiere"));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public void archiver(int id) throws SQLException {
        String sql = "UPDATE classes SET est_active = FALSE WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifier(Classe classe) throws SQLException {
        String sql = "UPDATE classes SET intitule = ?, filiere = ?, niveau = ?, annee_scolaire = ?, " +
                    "ufr_id = ?, effectif = ?, nb_groupes = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, classe.getIntitule());
            stmt.setString(2, classe.getFiliere());
            stmt.setString(3, classe.getNiveau());
            stmt.setString(4, classe.getAnneeScolaire());
            stmt.setInt(5, classe.getUfrId());
            stmt.setInt(6, classe.getEffectif());
            stmt.setInt(7, classe.getNbGroupes());
            stmt.setInt(8, classe.getId());
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM classes WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private Classe creerClasseDepuisResultSet(ResultSet rs) throws SQLException {
        Classe classe = new Classe();
        classe.setId(rs.getInt("id"));
        classe.setIntitule(rs.getString("intitule"));
        classe.setFiliere(rs.getString("filiere"));
        classe.setNiveau(rs.getString("niveau"));
        classe.setAnneeScolaire(rs.getString("annee_scolaire"));
        classe.setUfrId(rs.getInt("ufr_id"));
        classe.setEffectif(rs.getInt("effectif"));
        classe.setNbGroupes(rs.getInt("nb_groupes"));
        classe.setEstActive(rs.getBoolean("est_active"));
        return classe;
    }
}