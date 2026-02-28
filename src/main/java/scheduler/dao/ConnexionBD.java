package scheduler.dao;

import java.sql.*;
import java.io.*;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestionnaire de connexion à la base de données MySQL.
 * Implémente un pool de connexions pour optimiser les performances.
 */
public class ConnexionBD {
    
    private static String url;
    private static String utilisateur;
    private static String motDePasse;
    private static boolean configurationOk = false;
    
    // ✅ AUGMENTER LE NOMBRE MAX DE CONNEXIONS
    private static final int MAX_CONNECTIONS = 50;
    private static final LinkedBlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>(MAX_CONNECTIONS);
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    
    static {
        try {
            InputStream input = ConnexionBD.class.getClassLoader().getResourceAsStream("config/bd.properties");
            
            if (input == null) {
                System.err.println("❌ Fichier bd.properties non trouvé !");
                configurationOk = false;
            } else {
                Properties props = new Properties();
                props.load(input);
                
                url = props.getProperty("mysql.url");
                utilisateur = props.getProperty("mysql.utilisateur");
                motDePasse = props.getProperty("mysql.motdepasse");
                
                Class.forName("com.mysql.cj.jdbc.Driver");
                configurationOk = true;
                
                // ✅ Créer plus de connexions initiales (10 au lieu de 5)
                for (int i = 0; i < 10; i++) {
                    try {
                        Connection conn = createNewConnection();
                        if (conn != null) {
                            connectionPool.offer(conn);
                        }
                    } catch (SQLException e) {
                        System.err.println("⚠️ Erreur création connexion initiale " + i + ": " + e.getMessage());
                    }
                }
                System.out.println("✅ Pool de connexions initialisé avec " + connectionPool.size() + " connexions");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation ConnexionBD: " + e.getMessage());
            configurationOk = false;
        }
    }
    
    private static synchronized Connection createNewConnection() throws SQLException {
        if (activeConnections.get() >= MAX_CONNECTIONS) {
            throw new SQLException("Nombre maximum de connexions atteint: " + MAX_CONNECTIONS);
        }
        activeConnections.incrementAndGet();
        Connection conn = DriverManager.getConnection(url, utilisateur, motDePasse);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET NAMES utf8mb4");
        } catch (SQLException e) {
            // Ignorer si la base ne supporte pas SET NAMES
        }
        return conn;
    }
    
    /**
     * Obtient une connexion depuis le pool.
     * @return une connexion valide
     * @throws SQLException si aucune connexion n'est disponible
     */
    public static Connection getConnection() throws SQLException {
        if (!configurationOk) {
            throw new SQLException("Configuration de la base de données non initialisée correctement");
        }
        
        Connection conn = null;
        try {
            // ✅ Timeout réduit à 3 secondes pour éviter l'attente trop longue
            conn = connectionPool.poll(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interruption lors de l'attente d'une connexion");
        }
        
        if (conn == null) {
            // ✅ Essayer de créer une nouvelle connexion si possible
            if (activeConnections.get() < MAX_CONNECTIONS) {
                conn = createNewConnection();
                System.out.println("🟢 Nouvelle connexion créée (totales: " + activeConnections.get() + ")");
            } else {
                // ✅ Attendre encore un peu avant d'abandonner
                try {
                    conn = connectionPool.poll(5, TimeUnit.SECONDS);
                    if (conn == null) {
                        // ✅ Afficher l'état du pool pour debug
                        System.err.println("⚠️ Pool saturé - Actives: " + activeConnections.get() + 
                                          ", Pool size: " + connectionPool.size());
                        throw new SQLException("Pool de connexions saturé après 8 secondes d'attente");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interruption lors de l'attente d'une connexion");
                }
            }
        }
        
        // ✅ Vérifier que la connexion est toujours valide
        if (conn != null && !conn.isValid(2)) {
            try {
                conn.close();
            } catch (SQLException e) { }
            conn = createNewConnection();
        }
        
        return conn;
    }

    public static synchronized void fermerToutesConnexions() {
        int fermees = 0;
        for (Connection conn : connectionPool) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    fermees++;
                }
            } catch (SQLException e) { }
        }
        connectionPool.clear();
        activeConnections.set(0);
        System.out.println("✅ Toutes les connexions fermées (" + fermees + " connexions)");
    }
    
    /**
     * Libère une connexion et la remet dans le pool.
     * @param conn la connexion à libérer
     */
    public static void libererConnection(Connection conn) {
        if (conn != null) {
            try {
                // ✅ Vérifier que la connexion est encore valide avant de la remettre
                if (!conn.isClosed() && conn.isValid(1)) {
                    if (!connectionPool.offer(conn)) {
                        conn.close();
                        activeConnections.decrementAndGet();
                    }
                } else {
                    conn.close();
                    activeConnections.decrementAndGet();
                }
            } catch (SQLException e) {
                try {
                    conn.close();
                } catch (SQLException ex) { }
                activeConnections.decrementAndGet();
            }
        }
    }
    
    /**
     * Teste la connexion à la base de données.
     * @return true si la connexion est établie, false sinon
     */
    public static boolean testerConnexion() {
        Connection conn = null;
        try {
            conn = getConnection();
            boolean result = conn != null && !conn.isClosed();
            libererConnection(conn);
            return result;
        } catch (SQLException e) {
            libererConnection(conn);
            return false;
        }
    }
    
    public static void fermerRessources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { }
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException e) { }
        }
        libererConnection(conn);
    }
    
    // ✅ Méthode pour debug - afficher l'état du pool
    public static String getPoolStatus() {
        return "Actives: " + activeConnections.get() + ", Pool: " + connectionPool.size() + "/" + MAX_CONNECTIONS;
    }
}