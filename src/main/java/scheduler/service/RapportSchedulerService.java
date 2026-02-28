package scheduler.service;

import scheduler.modele.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service de génération automatique des rapports hebdomadaires et mensuels.
 */
public class RapportSchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(RapportSchedulerService.class);
    
    private ScheduledExecutorService scheduler;
    private boolean running = false;
    
    private StatistiqueService statsService;
    private EmailService emailService;
    private ExportService exportService;
    private UtilisateurService utilisateurService;
    
    public RapportSchedulerService() {
        this.statsService = new StatistiqueService();
        this.emailService = new EmailService();
        this.exportService = new ExportService();
        this.utilisateurService = new UtilisateurService();
    }
    
    /**
     * Démarre le service de rapports automatiques.
     */
    public void start() {
        if (running) return;
        
        scheduler = Executors.newScheduledThreadPool(2);
        
        long initialDelayHebdo = calculerDelaiJusquauLundi();
        scheduler.scheduleAtFixedRate(this::envoyerRapportHebdomadaire, 
            initialDelayHebdo, 7, TimeUnit.DAYS);
        
        long initialDelayMensuel = calculerDelaiJusquauPremierDuMois();
        scheduler.scheduleAtFixedRate(this::envoyerRapportMensuel, 
            initialDelayMensuel, 30, TimeUnit.DAYS);
        
        scheduler.scheduleAtFixedRate(this::verifierStatutsEDT, 0, 1, TimeUnit.HOURS);
        
        running = true;
        logger.info("✅ Service de rapports automatiques démarré");
    }
    
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        running = false;
        logger.info("🛑 Service de rapports automatiques arrêté");
    }
    
    private void envoyerRapportHebdomadaire() {
        try {
            logger.info("📊 Génération du rapport hebdomadaire...");
            
            LocalDate debutSemaine = LocalDate.now().minusDays(7);
            LocalDate finSemaine = LocalDate.now().minusDays(1);
            String periode = "Semaine du " + debutSemaine + " au " + finSemaine;
            
            Map<String, Object> stats = new HashMap<>();
            Map<String, Object> statsGlobales = statsService.getStatistiquesGlobales();
            
            // ✅ Convertir explicitement les valeurs
            stats.put("totalCours", statsGlobales.getOrDefault("totalCours", 0));
            stats.put("totalSalles", statsGlobales.getOrDefault("totalSalles", 0));
            stats.put("tauxOccupationGlobal", statsGlobales.getOrDefault("tauxOccupationGlobal", 0.0));
            stats.put("utilisateursParRole", statsGlobales.getOrDefault("utilisateursParRole", new HashMap<>()));
            stats.put("signalements", new HashMap<String, Object>());
            stats.put("topSallesPlusUtilisees", new ArrayList<>());
            
            List<Administrateur> admins = utilisateurService.getAdministrateurs();
            String sujet = "📊 [SCHEDULER] Rapport hebdomadaire - " + periode;
            
            for (Administrateur admin : admins) {
                envoyerRapportEmail(admin, sujet, stats, periode, "hebdomadaire");
            }
            
            logger.info("✅ Rapport hebdomadaire envoyé à {} administrateur(s)", admins.size());
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi du rapport hebdomadaire", e);
        }
    }
    
    private void envoyerRapportMensuel() {
        try {
            logger.info("📊 Génération du rapport mensuel...");
            
            LocalDate debutMois = LocalDate.now().withDayOfMonth(1);
            LocalDate finMois = LocalDate.now().minusDays(1);
            String periode = "Mois de " + debutMois.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            
            Map<String, Object> stats = new HashMap<>();
            Map<String, Object> statsGlobales = statsService.getStatistiquesGlobales();
            
            stats.put("totalCours", statsGlobales.getOrDefault("totalCours", 0));
            stats.put("totalSalles", statsGlobales.getOrDefault("totalSalles", 0));
            stats.put("tauxOccupationGlobal", statsGlobales.getOrDefault("tauxOccupationGlobal", 0.0));
            stats.put("utilisateursParRole", statsGlobales.getOrDefault("utilisateursParRole", new HashMap<>()));
            stats.put("signalements", new HashMap<String, Object>());
            stats.put("topSallesPlusUtilisees", new ArrayList<>());
            
            List<Administrateur> admins = utilisateurService.getAdministrateurs();
            String sujet = "📊 [SCHEDULER] Rapport mensuel - " + periode;
            
            for (Administrateur admin : admins) {
                envoyerRapportEmail(admin, sujet, stats, periode, "mensuel");
            }
            
            logger.info("✅ Rapport mensuel envoyé à {} administrateur(s)", admins.size());
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi du rapport mensuel", e);
        }
    }
    
    private void envoyerRapportEmail(Administrateur admin, String sujet, 
            Map<String, Object> stats, String periode, String type) {
        
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='background-color: #1565C0; padding: 20px; text-align: center;'>");
        html.append("<h1 style='color: white;'>SCHEDULER - Rapport ").append(type).append("</h1>");
        html.append("</div>");
        html.append("<div style='padding: 20px;'>");
        html.append("<h2>Période: ").append(periode).append("</h2>");
        
        html.append("<h3>📊 Résumé</h3>");
        html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; width: 100%;'>");
        html.append("<tr><th>Indicateur</th><th>Valeur</th></tr>");
        
        int totalCours = (int) stats.getOrDefault("totalCours", 0);
        int totalSalles = (int) stats.getOrDefault("totalSalles", 0);
        double tauxOccupation = (double) stats.getOrDefault("tauxOccupationGlobal", 0.0);
        
        html.append("<tr><td>📚 Total cours</td><td>").append(totalCours).append("</td></tr>");
        html.append("<tr><td>🏛️ Total salles</td><td>").append(totalSalles).append("</td></tr>");
        html.append("<tr><td>📈 Taux d'occupation</td><td>").append(String.format("%.1f%%", tauxOccupation)).append("</td></tr>");
        
        html.append("</table>");
        
        html.append("<p style='margin-top: 20px; color: #666;'>Ce rapport est généré automatiquement.</p>");
        html.append("<p>Cordialement,<br>L'équipe SCHEDULER</p>");
        html.append("</div></body></html>");
        
        emailService.envoyerEmailHTML(admin.getEmail(), sujet, html.toString(), null);
    }
    
    private void verifierStatutsEDT() {
        try {
            EmploiDuTempsService edtService = new EmploiDuTempsService();
            edtService.mettreAJourTousStatuts();
            logger.debug("✅ Vérification des statuts EDT effectuée");
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la vérification des statuts EDT", e);
        }
    }
    
    private long calculerDelaiJusquauLundi() {
        LocalDate aujourdhui = LocalDate.now();
        int joursJusquaLundi = (8 - aujourdhui.getDayOfWeek().getValue()) % 7;
        if (joursJusquaLundi == 0) joursJusquaLundi = 7;
        
        LocalDate prochainLundi = aujourdhui.plusDays(joursJusquaLundi);
        LocalDateTime cible = prochainLundi.atTime(8, 0);
        LocalDateTime maintenant = LocalDateTime.now();
        
        long millis = java.time.Duration.between(maintenant, cible).toMillis();
        return Math.max(0, millis / 1000);
    }
    
    private long calculerDelaiJusquauPremierDuMois() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate premierProchainMois = aujourdhui.plusMonths(1).withDayOfMonth(1);
        LocalDateTime cible = premierProchainMois.atTime(8, 0);
        LocalDateTime maintenant = LocalDateTime.now();
        
        long millis = java.time.Duration.between(maintenant, cible).toMillis();
        return Math.max(0, millis / 1000);
    }
    
    public boolean isRunning() {
        return running;
    }
}