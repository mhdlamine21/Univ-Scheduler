package scheduler.util;

import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import java.util.regex.Pattern;

/**
 * Utilitaire de validation des données.
 * Fournit des méthodes pour valider les entrées utilisateur.
 */
public final class ValidationDonnees {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern TELEPHONE_PATTERN = Pattern.compile("^(\\+?221)?[77|76|75|78|70][0-9]{7}$");
    private static final Pattern NOM_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ\\s-]{2,50}$");
    
    private ValidationDonnees() {
    }
    
    public static boolean validerEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean validerTelephone(String telephone) {
        return telephone != null && TELEPHONE_PATTERN.matcher(telephone).matches();
    }
    
    public static boolean validerNom(String nom) {
        return nom != null && NOM_PATTERN.matcher(nom).matches();
    }
    
    public static boolean validerMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < Constantes.LONGUEUR_MIN_MOT_DE_PASSE) {
            return false;
        }
        
        boolean aMajuscule = motDePasse.matches(".*[A-Z].*");
        boolean aMinuscule = motDePasse.matches(".*[a-z].*");
        boolean aChiffre = motDePasse.matches(".*\\d.*");
        
        return aMajuscule && aMinuscule && aChiffre;
    }
    
    public static int evaluerForceMotDePasse(String motDePasse) {
        if (motDePasse == null) return 0;
        
        int force = 0;
        
        if (motDePasse.length() >= 8) force++;
        if (motDePasse.matches(".*[A-Z].*")) force++;
        if (motDePasse.matches(".*[a-z].*")) force++;
        if (motDePasse.matches(".*\\d.*")) force++;
        if (motDePasse.matches(".*[!@#$%^&*()].*")) force++;
        
        return force;
    }
    
    public static boolean validerDate(String date) {
        try {
            java.time.LocalDate.parse(date);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean validerHeure(String heure) {
        try {
            java.time.LocalTime.parse(heure);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean validerEntierPositif(String valeur) {
        try {
            int n = Integer.parseInt(valeur);
            return n > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean validerCapacite(int capacite) {
        return capacite > 0 && capacite <= 500;
    }
    
    public static void ajouterValidateurEmail(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !validerEmail(newVal)) {
                field.getStyleClass().add("text-field-error");
            } else {
                field.getStyleClass().remove("text-field-error");
            }
        });
    }
    
    public static void ajouterValidateurNonVide(TextField field, String message) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                field.getStyleClass().add("text-field-error");
                field.setTooltip(new Tooltip(message));
            } else {
                field.getStyleClass().remove("text-field-error");
                field.setTooltip(null);
            }
        });
    }
    
    public static void ajouterValidateurSelection(ComboBox<?> comboBox, String message) {
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                comboBox.getStyleClass().add("combo-box-error");
                comboBox.setTooltip(new Tooltip(message));
            } else {
                comboBox.getStyleClass().remove("combo-box-error");
                comboBox.setTooltip(null);
            }
        });
    }
}