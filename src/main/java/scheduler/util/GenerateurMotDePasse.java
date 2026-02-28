package scheduler.util;

import java.security.SecureRandom;

/**
 * Générateur de mots de passe aléatoires sécurisés.
 */
public final class GenerateurMotDePasse {
    
    private static final String MAJUSCULES = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULES = "abcdefghijkmnopqrstuvwxyz";
    private static final String CHIFFRES = "23456789";
    private static final String SPECIALS = "@#$%!";
    
    private static final String TOUS_CARACTERES = MAJUSCULES + MINUSCULES + CHIFFRES;
    
    private static final SecureRandom random = new SecureRandom();
    
    private GenerateurMotDePasse() {
    }
    
    public static String generer() {
        return generer(8);
    }
    
    public static String generer(int longueur) {
        StringBuilder motDePasse = new StringBuilder(longueur);
        
        motDePasse.append(MAJUSCULES.charAt(random.nextInt(MAJUSCULES.length())));
        motDePasse.append(MINUSCULES.charAt(random.nextInt(MINUSCULES.length())));
        motDePasse.append(CHIFFRES.charAt(random.nextInt(CHIFFRES.length())));
        
        for (int i = 3; i < longueur; i++) {
            motDePasse.append(TOUS_CARACTERES.charAt(random.nextInt(TOUS_CARACTERES.length())));
        }
        
        return melanger(motDePasse.toString());
    }
    
    public static String genererAvecSpeciaux(int longueur) {
        StringBuilder motDePasse = new StringBuilder(longueur);
        String tous = TOUS_CARACTERES + SPECIALS;
        
        for (int i = 0; i < longueur; i++) {
            motDePasse.append(tous.charAt(random.nextInt(tous.length())));
        }
        
        return motDePasse.toString();
    }
    
    private static String melanger(String input) {
        char[] caracteres = input.toCharArray();
        for (int i = caracteres.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = caracteres[i];
            caracteres[i] = caracteres[j];
            caracteres[j] = temp;
        }
        return new String(caracteres);
    }
    
    public static boolean estSecurise(String motDePasse) {
        return ValidationDonnees.validerMotDePasse(motDePasse);
    }
}