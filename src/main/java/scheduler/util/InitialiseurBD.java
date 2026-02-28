package scheduler.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

/**
 * Utilitaire pour initialiser automatiquement la base MySQL si elle n'existe pas encore.
 */
public class InitialiseurBD {
    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            try (InputStream in = InitialiseurBD.class.getClassLoader().getResourceAsStream("config/bd.properties")) {
                if (in != null) props.load(in);
            }

            String user = props.getProperty("mysql.utilisateur", "root");
            String pass = props.getProperty("mysql.motdepasse", "778512692");
            String hostUrl = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("⏳ Connexion au serveur MySQL local...");
            try (Connection conn = DriverManager.getConnection(hostUrl, user, pass);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS univ_scheduler CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
                System.out.println("✅ Base de données 'univ_scheduler' créée ou déjà existante.");
            }

            // Exécution du script SQL si les tables sont vides
            String dbUrl = "jdbc:mysql://localhost:3306/univ_scheduler?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&allowMultiQueries=true";
            try (Connection conn = DriverManager.getConnection(dbUrl, user, pass);
                 Statement stmt = conn.createStatement()) {
                if (Files.exists(Paths.get("script.sql"))) {
                    String sqlContent = Files.readString(Paths.get("script.sql"), StandardCharsets.UTF_8);
                    // Remplacer les délimiteurs pour multi-requêtes simples
                    String[] queries = sqlContent.split(";");
                    int executeCount = 0;
                    for (String q : queries) {
                        String trim = q.trim();
                        if (!trim.isEmpty() && !trim.startsWith("/*") && !trim.startsWith("--") && !trim.toUpperCase().startsWith("DELIMITER")) {
                            try {
                                stmt.execute(trim);
                                executeCount++;
                            } catch (Exception ex) {
                                // ignorer les erreurs sur les tables déjà créées
                            }
                        }
                    }
                    System.out.println("✅ Structure et tables vérifiées (" + executeCount + " déclarations traitées).");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation BD : " + e.getMessage());
        }
    }
}
