package scheduler.dao;

import scheduler.modele.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux comptes utilisateurs.
 */
public class UtilisateurDAO {
    
    public void ajouter(Utilisateur utilisateur) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, est_valide, numero_etudiant, matricule_enseignant, ufr_id, classe_id, type_etudiant) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, utilisateur.getNom());
            stmt.setString(2, utilisateur.getPrenom());
            stmt.setString(3, utilisateur.getEmail());
            stmt.setString(4, utilisateur.getMotDePasse());
            stmt.setString(5, utilisateur.getRole());
            stmt.setBoolean(6, utilisateur.isEstValide());
            
            if (utilisateur instanceof Etudiant) {
                Etudiant etu = (Etudiant) utilisateur;
                stmt.setString(7, etu.getNumeroEtudiant());
                stmt.setNull(8, Types.VARCHAR);
                stmt.setInt(9, etu.getUfrId());
                stmt.setInt(10, etu.getClasseId());
                stmt.setString(11, etu.getTypeEtudiant() != null ? etu.getTypeEtudiant() : "normal");
                
            } else if (utilisateur instanceof Enseignant) {
                Enseignant ens = (Enseignant) utilisateur;
                stmt.setNull(7, Types.VARCHAR);
                stmt.setString(8, ens.getMatricule());
                stmt.setNull(9, Types.INTEGER);
                stmt.setNull(10, Types.INTEGER);
                stmt.setNull(11, Types.VARCHAR);
                
            } else {
                stmt.setNull(7, Types.VARCHAR);
                stmt.setNull(8, Types.VARCHAR);
                stmt.setNull(9, Types.INTEGER);
                stmt.setNull(10, Types.INTEGER);
                stmt.setNull(11, Types.VARCHAR);
            }
            
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                utilisateur.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Utilisateur trouverParEmail(String email) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM utilisateurs WHERE email = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return creerUtilisateurDepuisResultSet(rs);
            }
            return null;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Utilisateur trouverParId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM utilisateurs WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return creerUtilisateurDepuisResultSet(rs);
            }
            return null;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<Utilisateur> listerTous() throws SQLException {
        List<Utilisateur> liste = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM utilisateurs ORDER BY nom, prenom";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerUtilisateurDepuisResultSet(rs));
            }
            return liste;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<Utilisateur> listerParRole(String role) throws SQLException {
        List<Utilisateur> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM utilisateurs WHERE role = ? ORDER BY nom, prenom";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, role);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerUtilisateurDepuisResultSet(rs));
            }
            return liste;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<Utilisateur> listerEnAttente() throws SQLException {
        List<Utilisateur> liste = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM utilisateurs WHERE est_valide = FALSE ORDER BY date_creation";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerUtilisateurDepuisResultSet(rs));
            }
            return liste;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<Etudiant> listerEtudiantsParClasse(int classeId) throws SQLException {
        List<Etudiant> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM utilisateurs WHERE role = 'etudiant' AND classe_id = ? ORDER BY nom, prenom";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, classeId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Utilisateur u = creerUtilisateurDepuisResultSet(rs);
                if (u instanceof Etudiant) {
                    liste.add((Etudiant) u);
                }
            }
            return liste;
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<Etudiant> listerEtudiantsParUfr(int ufrId) throws SQLException {
        List<Etudiant> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM utilisateurs WHERE role = 'etudiant' AND ufr_id = ? ORDER BY nom, prenom";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, ufrId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Utilisateur u = creerUtilisateurDepuisResultSet(rs);
                if (u instanceof Etudiant) {
                    liste.add((Etudiant) u);
                }
            }
            return liste;
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void validerUtilisateur(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE utilisateurs SET est_valide = TRUE, date_validation = CURDATE() WHERE id = ?";
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
    
    public void modifier(Utilisateur utilisateur) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE utilisateurs SET nom = ?, prenom = ?, email = ?, mot_de_passe = ?, role = ?, est_valide = ?, numero_etudiant = ?, matricule_enseignant = ?, ufr_id = ?, classe_id = ?, type_etudiant = ? WHERE id = ?";
            
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, utilisateur.getNom());
            stmt.setString(2, utilisateur.getPrenom());
            stmt.setString(3, utilisateur.getEmail());
            stmt.setString(4, utilisateur.getMotDePasse());
            stmt.setString(5, utilisateur.getRole());
            stmt.setBoolean(6, utilisateur.isEstValide());
            
            if (utilisateur instanceof Etudiant) {
                Etudiant etu = (Etudiant) utilisateur;
                stmt.setString(7, etu.getNumeroEtudiant());
                stmt.setNull(8, Types.VARCHAR);
                stmt.setInt(9, etu.getUfrId());
                stmt.setInt(10, etu.getClasseId());
                stmt.setString(11, etu.getTypeEtudiant() != null ? etu.getTypeEtudiant() : "normal");
            } else if (utilisateur instanceof Enseignant) {
                Enseignant ens = (Enseignant) utilisateur;
                stmt.setNull(7, Types.VARCHAR);
                stmt.setString(8, ens.getMatricule());
                stmt.setNull(9, Types.INTEGER);
                stmt.setNull(10, Types.INTEGER);
                stmt.setNull(11, Types.VARCHAR);
            } else {
                stmt.setNull(7, Types.VARCHAR);
                stmt.setNull(8, Types.VARCHAR);
                stmt.setNull(9, Types.INTEGER);
                stmt.setNull(10, Types.INTEGER);
                stmt.setNull(11, Types.VARCHAR);
            }
            
            stmt.setInt(12, utilisateur.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifierInfosBase(int id, String nom, String prenom, String email) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE utilisateurs SET nom = ?, prenom = ?, email = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nom);
            stmt.setString(2, prenom);
            stmt.setString(3, email);
            stmt.setInt(4, id);
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifierEtudiant(int id, String numeroEtudiant, int ufrId, int classeId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE utilisateurs SET numero_etudiant = ?, ufr_id = ?, classe_id = ? WHERE id = ? AND role = 'etudiant'";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, numeroEtudiant);
            stmt.setInt(2, ufrId);
            stmt.setInt(3, classeId);
            stmt.setInt(4, id);
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifierEnseignant(int id, String matricule) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE utilisateurs SET matricule_enseignant = ? WHERE id = ? AND role = 'enseignant'";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, matricule);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void updateTypeEtudiant(int id, String typeEtudiant) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE utilisateurs SET type_etudiant = ? WHERE id = ? AND role = 'etudiant'";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, typeEtudiant);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void updateMotDePasse(int id, String nouveauMotDePasse) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE utilisateurs SET mot_de_passe = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nouveauMotDePasse);
            stmt.setInt(2, id);
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
            String sql = "DELETE FROM utilisateurs WHERE id = ?";
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
    
    public boolean emailExiste(String email) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public boolean emailExisteAvecIdDifferent(String email, int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ? AND id != ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setInt(2, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private Utilisateur creerUtilisateurDepuisResultSet(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        Utilisateur utilisateur = null;
        
        try {
            switch (role) {
                case "admin":
                    utilisateur = new Administrateur();
                    break;
                case "gestionnaire":
                    utilisateur = new Gestionnaire();
                    break;
                case "enseignant":
                    utilisateur = new Enseignant();
                    ((Enseignant) utilisateur).setMatricule(rs.getString("matricule_enseignant"));
                    try {
                        String gradeEns = rs.getString("grade");
                        if (gradeEns != null && !gradeEns.isEmpty()) {
                            ((Enseignant) utilisateur).setGrade(gradeEns);
                        }
                    } catch (SQLException ignored) { }
                    break;
                case "etudiant":
                    utilisateur = new Etudiant();
                    ((Etudiant) utilisateur).setNumeroEtudiant(rs.getString("numero_etudiant"));
                    ((Etudiant) utilisateur).setUfrId(rs.getInt("ufr_id"));
                    ((Etudiant) utilisateur).setClasseId(rs.getInt("classe_id"));
                    String typeEtudiant = rs.getString("type_etudiant");
                    if (typeEtudiant != null && !typeEtudiant.isEmpty()) {
                        ((Etudiant) utilisateur).setTypeEtudiant(typeEtudiant);
                    } else {
                        ((Etudiant) utilisateur).setTypeEtudiant("normal");
                    }
                    break;
                default:
                    return null;
            }
            
            if (utilisateur != null) {
                utilisateur.setId(rs.getInt("id"));
                utilisateur.setNom(rs.getString("nom"));
                utilisateur.setPrenom(rs.getString("prenom"));
                utilisateur.setEmail(rs.getString("email"));
                utilisateur.setMotDePasse(rs.getString("mot_de_passe"));
                utilisateur.setRole(role);
                utilisateur.setEstValide(rs.getBoolean("est_valide"));
                
                Date dateValidation = rs.getDate("date_validation");
                if (dateValidation != null) {
                    utilisateur.setDateValidation(dateValidation.toLocalDate().atStartOfDay());
                }
                
                Timestamp dateCreation = rs.getTimestamp("date_creation");
                if (dateCreation != null) {
                    utilisateur.setDateCreation(dateCreation.toLocalDateTime());
                }
            }
            
        } catch (SQLException e) {
            throw e;
        }
        
        return utilisateur;
    }
}