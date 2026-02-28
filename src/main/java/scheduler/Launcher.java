package scheduler;

/**
 * Lanceur indépendant évitant la vérification stricte du module-path JavaFX.
 * Permet d'exécuter l'application JavaFX via un fat JAR ou classpath direct.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
