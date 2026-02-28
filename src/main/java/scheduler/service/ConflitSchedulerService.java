package scheduler.service;

import scheduler.modele.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Service de détection périodique des conflits.
 * S'exécute toutes les 10 minutes pour vérifier et notifier les nouveaux conflits.
 */
public class ConflitSchedulerService {

    private static final int INTERVALLE_SECONDES = 600;

    private ScheduledExecutorService scheduler;
    private Consumer<Void> conflitListener;
    private boolean running = false;
    private int dernierNombreConflits = -1;

    private final AtomicBoolean enCours = new AtomicBoolean(false);

    private ConflitService conflitService;
    private PlanningService planningService;
    private ReservationService reservationService;
    private NotificationService notificationService;
    private EmailService emailService;

    public ConflitSchedulerService() {
        this.conflitService     = new ConflitService();
        this.planningService    = new PlanningService();
        this.reservationService = new ReservationService();
        this.notificationService = new NotificationService();
        this.emailService        = new EmailService();
    }

    /**
     * Démarre le service de détection périodique des conflits.
     */
    public void start() {
        if (running) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "conflit-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::verifierConflits, 60, INTERVALLE_SECONDES, TimeUnit.SECONDS);
        running = true;
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        running = false;
    }

    public void setConflitListener(Consumer<Void> listener) {
        this.conflitListener = listener;
    }

    public boolean isRunning() { return running; }

    private void verifierConflits() {
        if (!enCours.compareAndSet(false, true)) {
            return;
        }

        try {
            List<Map<String, Object>> conflitsActuels = collecterTousConflits();
            int nombreConflits = conflitsActuels.size();

            if (dernierNombreConflits >= 0 && nombreConflits > dernierNombreConflits) {
                int nouveaux = nombreConflits - dernierNombreConflits;
                notifierNouveauxConflits(conflitsActuels, nouveaux);
            }

            dernierNombreConflits = nombreConflits;

            if (conflitListener != null) {
                conflitListener.accept(null);
            }

        } catch (Exception e) {
        } finally {
            enCours.set(false);
        }
    }

    private List<Map<String, Object>> collecterTousConflits() throws Exception {
        List<Map<String, Object>> conflits = new ArrayList<>();

        List<EmploiDuTemps> edtsActifs = planningService.listerActifs();
        for (EmploiDuTemps edt : edtsActifs) {
            List<Creneau> creneaux = planningService.getCreneauxParEmploiDuTemps(edt.getId());
            for (Creneau creneau : creneaux) {
                try {
                    List<ConflitService.ResultatConflit> resultats =
                            conflitService.verifierConflitsAvecSolutions(creneau);
                    for (ConflitService.ResultatConflit r : resultats) {
                        Map<String, Object> c = new HashMap<>();
                        c.put("type",        r.getTypeConflit());
                        c.put("description", r.getDescription());
                        c.put("source",      "EDT #" + edt.getId());
                        c.put("date",        creneau.getJour());
                        c.put("creneau",     creneau);
                        conflits.add(c);
                    }
                } catch (Exception e) {
                }
            }
        }

        List<Reservation> reservations = reservationService.listerTous();
        for (Reservation r : reservations) {
            if ("annulee".equals(r.getStatut()) || "refusee".equals(r.getStatut())) continue;
            try {
                boolean salleLibre = reservationService.verifierDisponibilite(
                        r.getSalleId(), r.getDateReservation(), r.getHeureDebut(), r.getHeureFin());
                if (!salleLibre) {
                    Map<String, Object> c = new HashMap<>();
                    c.put("type",        "RESERVATION");
                    c.put("description", "Réservation #" + r.getId() + " en conflit");
                    c.put("source",      "Réservation #" + r.getId());
                    c.put("date",        r.getDateReservation());
                    c.put("reservation", r);
                    conflits.add(c);
                }
            } catch (Exception e) {
            }
        }

        Set<String> vus = new HashSet<>();
        List<Map<String, Object>> uniques = new ArrayList<>();
        for (Map<String, Object> c : conflits) {
            String key = c.get("type") + "_" + c.get("description");
            if (vus.add(key)) uniques.add(c);
        }
        return uniques;
    }

    private void notifierNouveauxConflits(List<Map<String, Object>> conflits, int nouveaux) {
        try {
            UtilisateurService utilisateurService = new UtilisateurService();
            List<Gestionnaire> gestionnaires = utilisateurService.getGestionnaires();

            for (Gestionnaire g : gestionnaires) {
                notificationService.ajouterNotification(g.getId(),
                        "⚠️ " + nouveaux + " nouveau(x) conflit(s) détecté(s) ("
                                + conflits.size() + " au total)");

                StringBuilder details = new StringBuilder();
                int max = Math.min(3, conflits.size());
                for (int i = 0; i < max; i++) {
                    Map<String, Object> c = conflits.get(i);
                    details.append("\n• ").append(c.get("type"))
                           .append(": ").append(c.get("description"));
                }
                if (conflits.size() > 3) {
                    details.append("\n• ... et ").append(conflits.size() - 3).append(" autres");
                }

                try {
                    emailService.envoyerConflitGestionnaire(
                            g.getEmail(),
                            "Conflits multiples",
                            details.toString(),
                            "Voir application",
                            LocalDate.now().toString(),
                            "-",
                            "-",
                            g.getPrenom() + " " + g.getNom()
                    );
                } catch (Exception emailEx) {
                }
            }
        } catch (Exception e) {
        }
    }
}