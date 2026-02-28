package scheduler.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service de détection et gestion des jours fériés légaux et chômés au Sénégal.
 * Indispensable pour éviter la planification d'examens ou de cours magistraux
 * lors des fêtes nationales et religieuses sénégalaises.
 */
public class JourFerieService {

    // Registre mémoire des jours fériés mobiles et fixes (clé: LocalDate, valeur: Libellé)
    private static final Map<LocalDate, String> JOURS_FERIES = new HashMap<>();

    static {
        initialiserJoursFeriesSenegal();
    }

    private static void initialiserJoursFeriesSenegal() {
        // Enregistrer pour les années 2024, 2025, 2026, 2027
        for (int annee = 2024; annee <= 2027; annee++) {
            // Jours fériés civils et nationaux fixes
            JOURS_FERIES.put(LocalDate.of(annee, Month.JANUARY, 1), "Jour de l'An");
            JOURS_FERIES.put(LocalDate.of(annee, Month.APRIL, 4), "Fête Nationale de l'Indépendance du Sénégal");
            JOURS_FERIES.put(LocalDate.of(annee, Month.MAY, 1), "Fête du Travail");
            JOURS_FERIES.put(LocalDate.of(annee, Month.MAY, 25), "Journée de l'Afrique");
            JOURS_FERIES.put(LocalDate.of(annee, Month.AUGUST, 15), "Assomption");
            JOURS_FERIES.put(LocalDate.of(annee, Month.NOVEMBER, 1), "Toussaint");
            JOURS_FERIES.put(LocalDate.of(annee, Month.DECEMBER, 25), "Noël");

            // Calcul de Pâques (Algorithme de Meeus/Jones/Butcher)
            LocalDate paques = calculerPaques(annee);
            LocalDate lundiPaques = paques.plusDays(1);
            LocalDate ascension = paques.plusDays(39);
            LocalDate lundiPentecote = paques.plusDays(50);

            JOURS_FERIES.put(lundiPaques, "Lundi de Pâques");
            JOURS_FERIES.put(ascension, "Ascension");
            JOURS_FERIES.put(lundiPentecote, "Lundi de Pentecôte");
        }

        // Fêtes religieuses sénégalaises musulmanes (dates officielles & prévisionnelles)
        // 2024
        JOURS_FERIES.put(LocalDate.of(2024, 4, 10), "Korité (Aïd el-Fitr)");
        JOURS_FERIES.put(LocalDate.of(2024, 6, 17), "Tabaski (Aïd el-Kébir)");
        JOURS_FERIES.put(LocalDate.of(2024, 7, 16), "Tamkharite (Achoura)");
        JOURS_FERIES.put(LocalDate.of(2024, 8, 23), "Grand Magal de Touba");
        JOURS_FERIES.put(LocalDate.of(2024, 9, 15), "Maouloud (Gamou / Naissance du Prophète)");

        // 2025
        JOURS_FERIES.put(LocalDate.of(2025, 3, 31), "Korité (Aïd el-Fitr)");
        JOURS_FERIES.put(LocalDate.of(2025, 6, 7), "Tabaski (Aïd el-Kébir)");
        JOURS_FERIES.put(LocalDate.of(2025, 7, 6), "Tamkharite (Achoura)");
        JOURS_FERIES.put(LocalDate.of(2025, 8, 13), "Grand Magal de Touba");
        JOURS_FERIES.put(LocalDate.of(2025, 9, 5), "Maouloud (Gamou / Naissance du Prophète)");

        // 2026
        JOURS_FERIES.put(LocalDate.of(2026, 3, 20), "Korité (Aïd el-Fitr)");
        JOURS_FERIES.put(LocalDate.of(2026, 5, 27), "Tabaski (Aïd el-Kébir)");
        JOURS_FERIES.put(LocalDate.of(2026, 6, 25), "Tamkharite (Achoura)");
        JOURS_FERIES.put(LocalDate.of(2026, 8, 2), "Grand Magal de Touba");
        JOURS_FERIES.put(LocalDate.of(2026, 8, 25), "Maouloud (Gamou / Naissance du Prophète)");

        // 2027
        JOURS_FERIES.put(LocalDate.of(2027, 3, 10), "Korité (Aïd el-Fitr)");
        JOURS_FERIES.put(LocalDate.of(2027, 5, 17), "Tabaski (Aïd el-Kébir)");
        JOURS_FERIES.put(LocalDate.of(2027, 6, 15), "Tamkharite (Achoura)");
        JOURS_FERIES.put(LocalDate.of(2027, 7, 23), "Grand Magal de Touba");
        JOURS_FERIES.put(LocalDate.of(2027, 8, 14), "Maouloud (Gamou / Naissance du Prophète)");
    }

    /**
     * Vérifie si une date donnée est un jour férié ou chômé officiel au Sénégal.
     */
    public static boolean estJourFerie(LocalDate date) {
        if (date == null) return false;
        return JOURS_FERIES.containsKey(date);
    }

    /**
     * Retourne le libellé du jour férié ou vide si jour ouvré.
     */
    public static Optional<String> getLibelleFerie(LocalDate date) {
        if (date == null) return Optional.empty();
        return Optional.ofNullable(JOURS_FERIES.get(date));
    }

    /**
     * Vérifie si une date correspond à un dimanche (fermeture standard de campus).
     */
    public static boolean estDimanche(LocalDate date) {
        return date != null && date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * Permet à l'administration universitaire d'ajouter dynamiquement un jour chômé décrété.
     */
    public static void ajouterJourChome(LocalDate date, String libelle) {
        if (date != null && libelle != null && !libelle.trim().isEmpty()) {
            JOURS_FERIES.put(date, libelle.trim());
        }
    }

    /**
     * Algorithme standard de calcul du dimanche de Pâques grégorien.
     */
    private static LocalDate calculerPaques(int annee) {
        int a = annee % 19;
        int b = annee / 100;
        int c = annee % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mois = (h + l - 7 * m + 114) / 31;
        int jour = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(annee, mois, jour);
    }
}
