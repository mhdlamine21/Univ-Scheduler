package scheduler.service;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service de gestion et persistance des photos (UFR, Salles, Signalements).
 */
public class ImageUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ImageUploadService.class);

    private static final String DIR_UFR = "src/main/resources/images/ufr";
    private static final String DIR_SALLES = "src/main/resources/images/salles";
    private static final String DIR_SIGNALEMENTS = "src/main/resources/images/signalements";

    /**
     * Enregistre une image pour une UFR.
     */
    public static String sauvegarderPhotoUfr(File fichierSource) throws IOException {
        return sauvegarderFichier(fichierSource, DIR_UFR, "ufr_");
    }

    /**
     * Enregistre une image pour une Salle.
     */
    public static String sauvegarderPhotoSalle(File fichierSource) throws IOException {
        return sauvegarderFichier(fichierSource, DIR_SALLES, "salle_");
    }

    /**
     * Enregistre une image de preuve pour un Signalement.
     */
    public static String sauvegarderPhotoSignalement(File fichierSource) throws IOException {
        return sauvegarderFichier(fichierSource, DIR_SIGNALEMENTS, "sign_");
    }

    private static String sauvegarderFichier(File fichierSource, String dossierCible, String prefix) throws IOException {
        if (fichierSource == null || !fichierSource.exists()) {
            return null;
        }

        // Créer les répertoires si nécessaire
        Path targetDir = Paths.get(dossierCible).toAbsolutePath().normalize();
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // Déterminer l'extension
        String originalName = fichierSource.getName();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex).toLowerCase();
        } else {
            extension = ".jpg";
        }

        // Nom unique
        String fileName = prefix + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path destination = targetDir.resolve(fileName);

        // Copier le fichier
        Files.copy(fichierSource.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Photo enregistrée : {}", destination.toAbsolutePath());

        // Retourner le chemin relatif vers resources ou absolu
        return destination.toAbsolutePath().toString();
    }

    /**
     * Charge une image de façon tolérante (fichier disque, ressource classpath, etc.).
     */
    public static Image chargerImage(String chemin, double width, double height) {
        if (chemin == null || chemin.isBlank()) {
            return null;
        }

        try {
            // Essai 1: Fichier disque absolu ou relatif
            File file = new File(chemin);
            if (file.exists()) {
                return new Image(file.toURI().toString(), width, height, true, true);
            }

            // Essai 2: Ressource classpath
            String resPath = chemin.startsWith("/") ? chemin : "/" + chemin;
            InputStream is = ImageUploadService.class.getResourceAsStream(resPath);
            if (is != null) {
                return new Image(is, width, height, true, true);
            }

            // Essai 3: URI standard
            return new Image(chemin, width, height, true, true);
        } catch (Exception e) {
            logger.warn("Impossible de charger l'image depuis {}: {}", chemin, e.getMessage());
            return null;
        }
    }
}
