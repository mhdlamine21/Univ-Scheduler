package scheduler.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilitaire de conversion et formatage des dates.
 */
public final class ConvertisseurDate {
    
    private static final DateTimeFormatter FORMATTEUR_DATE = DateTimeFormatter.ofPattern(Constantes.FORMAT_DATE);
    private static final DateTimeFormatter FORMATTEUR_DATE_BD = DateTimeFormatter.ofPattern(Constantes.FORMAT_DATE_BD);
    private static final DateTimeFormatter FORMATTEUR_HEURE = DateTimeFormatter.ofPattern(Constantes.FORMAT_HEURE);
    private static final DateTimeFormatter FORMATTEUR_DATE_HEURE = DateTimeFormatter.ofPattern(Constantes.FORMAT_DATE_HEURE);
    
    private ConvertisseurDate() {
    }
    
    public static LocalDate stringToDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, FORMATTEUR_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    public static LocalDate stringToDateBD(String dateStr) {
        try {
            return LocalDate.parse(dateStr, FORMATTEUR_DATE_BD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    public static String dateToString(LocalDate date) {
        if (date == null) return "";
        return date.format(FORMATTEUR_DATE);
    }
    
    public static String dateToStringBD(LocalDate date) {
        if (date == null) return "";
        return date.format(FORMATTEUR_DATE_BD);
    }
    
    public static LocalTime stringToHeure(String heureStr) {
        try {
            return LocalTime.parse(heureStr, FORMATTEUR_HEURE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    public static String heureToString(LocalTime heure) {
        if (heure == null) return "";
        return heure.format(FORMATTEUR_HEURE);
    }
    
    public static LocalDateTime stringToDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, FORMATTEUR_DATE_HEURE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    public static String dateTimeToString(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(FORMATTEUR_DATE_HEURE);
    }
    
    public static LocalTime calculerHeureFin(LocalTime debut, int dureeMinutes) {
        return debut.plusMinutes(dureeMinutes);
    }
    
    public static int calculerDuree(LocalTime debut, LocalTime fin) {
        return (int) java.time.Duration.between(debut, fin).toMinutes();
    }
    
    public static boolean chevauchement(LocalTime debut1, LocalTime fin1, LocalTime debut2, LocalTime fin2) {
        return debut1.isBefore(fin2) && debut2.isBefore(fin1);
    }
    
    public static String formatDateLong(LocalDate date) {
        if (date == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy");
        return date.format(formatter);
    }
    
    public static String getJourSemaine(LocalDate date) {
        if (date == null) return "";
        return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
    }
}