package scheduler.service;

import scheduler.dao.ConnexionBD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service de tâches automatiques (univ-scheduler).
 * Exécute périodiquement : mise à jour des statuts, vérification des conflits, rappels.
 */
public class ScheduledTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    
    private ScheduledExecutorService scheduler;
    private boolean running = false;
    
    private EmploiDuTempsService edtService;
    private ConflitSchedulerService conflitScheduler;
    private NotificationService notificationService;
    private ReservationService reservationService;
    
    public ScheduledTaskService() {
        this.edtService = new EmploiDuTempsService();
        this.conflitScheduler = new ConflitSchedulerService();
        this.notificationService = new NotificationService();
        this.reservationService = new ReservationService();
    }
    
    /**
     * Démarre toutes les tâches automatiques
     */
    public void start() {
        if (running) return;
        
        scheduler = Executors.newScheduledThreadPool(3);
        
        // ✅ Mise à jour des statuts EDT : toutes les 6 heures (au lieu de 1 heure)
        scheduler.scheduleAtFixedRate(this::mettreAJourStatutsEDT, 0, 6, TimeUnit.HOURS);
        
        // ✅ Vérification des conflits : toutes les 10 minutes (au lieu de continu)
        // conflitScheduler.start(); // Déjà géré ailleurs
        
        // ✅ Vérification des rappels : toutes les 15 minutes (au lieu de 1 minute)
        scheduler.scheduleAtFixedRate(this::verifierRappelsReservation, 2, 15, TimeUnit.MINUTES);
        
        // ✅ Nettoyage sessions : toutes les 24 heures
        scheduler.scheduleAtFixedRate(this::nettoyerSessions, 0, 24, TimeUnit.HOURS);
        
        running = true;
        logger.info("✅ Service de tâches automatiques démarré");
    }
    
    /**
     * Arrête toutes les tâches automatiques
     */
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
        conflitScheduler.stop();
        running = false;
        logger.info("🛑 Service de tâches automatiques arrêté");
    }
    
    /**
     * Met à jour les statuts des emplois du temps
     */
    private void mettreAJourStatutsEDT() {
        try {
            edtService.mettreAJourTousStatuts();
            logger.info("✅ Mise à jour des statuts EDT effectuée");
        } catch (SQLException e) {
            logger.error("❌ Erreur lors de la mise à jour des statuts EDT", e);
        }
    }
    
    /**
     * Vérifie les rappels de fin de réservation
     */
    private void verifierRappelsReservation() {
        try {
            reservationService.verifierRappelsFinReservation();
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la vérification des rappels", e);
        }
    }
    
    /**
     * Nettoie les sessions expirées (à implémenter selon besoin)
     */
    private void nettoyerSessions() {
        logger.debug("🧹 Nettoyage des sessions effectué");
    }
    
    /**
     * Vérifie si le service est en cours d'exécution
     */
    public boolean isRunning() {
        return running;
    }
}