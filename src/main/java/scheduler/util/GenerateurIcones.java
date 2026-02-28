package scheduler.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Générateur d'icônes haute définition pour l'application UNIV-SCHEDULER.
 * Produit des icônes PNG 64x64 antialiasées, parfaitement intégrées à la charte.
 */
public class GenerateurIcones {

    private static final int SIZE = 64;

    public static void main(String[] args) {
        File dir = new File("src/main/resources/images/icons");
        if (!dir.exists()) dir.mkdirs();

        genererToutes(dir);

        // Copier aussi dans target/classes/images/icons
        File targetDir = new File("target/classes/images/icons");
        if (!targetDir.exists()) targetDir.mkdirs();
        genererToutes(targetDir);

        System.out.println("✅ Pack d'icônes complet généré avec succès !");
    }

    private static void genererToutes(File dir) {
        creerIcone(dir, "view.png", Color.decode("#6B4226"), (g, s) -> {
            // Œil
            g.draw(new Ellipse2D.Float(10, 20, 44, 24));
            g.fill(new Ellipse2D.Float(24, 24, 16, 16));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Float(28, 28, 6, 6));
        });

        creerIcone(dir, "edit.png", Color.decode("#D39A43"), (g, s) -> {
            // Crayon
            Path2D.Float p = new Path2D.Float();
            p.moveTo(42, 12);
            p.lineTo(52, 22);
            p.lineTo(24, 50);
            p.lineTo(12, 52);
            p.lineTo(14, 40);
            p.closePath();
            g.fill(p);
        });

        creerIcone(dir, "delete.png", Color.decode("#A93226"), (g, s) -> {
            // Corbeille
            g.fillRoundRect(18, 22, 28, 32, 6, 6);
            g.fillRect(14, 16, 36, 5);
            g.fillRect(26, 10, 12, 6);
            g.setColor(Color.WHITE);
            g.fillRect(25, 28, 3, 20);
            g.fillRect(31, 28, 3, 20);
            g.fillRect(37, 28, 3, 20);
        });

        creerIcone(dir, "check.png", Color.decode("#27AE60"), (g, s) -> {
            // Coche
            Path2D.Float p = new Path2D.Float();
            p.moveTo(14, 32);
            p.lineTo(26, 44);
            p.lineTo(50, 16);
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(p);
        });

        creerIcone(dir, "cancel.png", Color.decode("#C0392B"), (g, s) -> {
            // Croix
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(18, 18, 46, 46);
            g.drawLine(46, 18, 18, 46);
        });

        creerIcone(dir, "add.png", Color.decode("#3D261A"), (g, s) -> {
            // Plus
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(32, 14, 32, 50);
            g.drawLine(14, 32, 50, 32);
        });

        creerIcone(dir, "camera.png", Color.decode("#6B4226"), (g, s) -> {
            // Appareil photo
            g.fillRoundRect(12, 20, 40, 30, 8, 8);
            g.fillRect(24, 14, 16, 6);
            g.setColor(Color.WHITE);
            g.fillOval(23, 27, 18, 18);
            g.setColor(Color.decode("#6B4226"));
            g.fillOval(27, 31, 10, 10);
        });

        creerIcone(dir, "calendar.png", Color.decode("#3D261A"), (g, s) -> {
            // Calendrier
            g.fillRoundRect(12, 16, 40, 38, 8, 8);
            g.setColor(Color.WHITE);
            g.fillRect(16, 26, 32, 24);
            g.setColor(Color.decode("#D39A43"));
            g.fillRect(20, 10, 5, 10);
            g.fillRect(39, 10, 5, 10);
        });

        creerIcone(dir, "room.png", Color.decode("#3D261A"), (g, s) -> {
            // Porte / Salle
            g.fillRoundRect(16, 12, 32, 42, 6, 6);
            g.setColor(Color.WHITE);
            g.fillRect(20, 16, 24, 34);
            g.setColor(Color.decode("#D39A43"));
            g.fillOval(36, 34, 5, 5);
        });

        creerIcone(dir, "swap.png", Color.decode("#D39A43"), (g, s) -> {
            // Flèches d'échange
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(16, 24, 46, 24);
            g.drawLine(40, 18, 46, 24);
            g.drawLine(40, 30, 46, 24);

            g.drawLine(48, 40, 18, 40);
            g.drawLine(24, 34, 18, 40);
            g.drawLine(24, 46, 18, 40);
        });

        creerIcone(dir, "refresh.png", Color.decode("#3D261A"), (g, s) -> {
            // Flèche circulaire
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(16, 16, 32, 32, 45, 270);
            g.fillPolygon(new int[]{42, 50, 42}, new int[]{22, 28, 34}, 3);
        });

        creerIcone(dir, "tool.png", Color.decode("#6B4226"), (g, s) -> {
            // Clé à molette
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(20, 44, 42, 22);
            g.drawArc(36, 14, 16, 16, 30, 290);
        });

        creerIcone(dir, "lock.png", Color.decode("#C0392B"), (g, s) -> {
            // Cadenas fermé
            g.fillRoundRect(18, 26, 28, 24, 6, 6);
            g.setStroke(new BasicStroke(4));
            g.drawArc(24, 14, 16, 20, 0, 180);
        });

        creerIcone(dir, "unlock.png", Color.decode("#27AE60"), (g, s) -> {
            // Cadenas ouvert
            g.fillRoundRect(18, 26, 28, 24, 6, 6);
            g.setStroke(new BasicStroke(4));
            g.drawArc(26, 12, 16, 20, 45, 180);
        });

        creerIcone(dir, "search.png", Color.decode("#6B4226"), (g, s) -> {
            // Loupe
            g.setStroke(new BasicStroke(4));
            g.drawOval(16, 16, 22, 22);
            g.drawLine(34, 34, 48, 48);
        });

        creerIcone(dir, "building.png", Color.decode("#6B4226"), (g, s) -> {
            // Bâtiment
            g.fillRect(16, 14, 32, 38);
            g.setColor(Color.WHITE);
            for (int r = 18; r < 45; r += 8) {
                g.fillRect(22, r, 5, 5);
                g.fillRect(30, r, 5, 5);
                g.fillRect(38, r, 5, 5);
            }
        });

        creerIcone(dir, "user.png", Color.decode("#6B4226"), (g, s) -> {
            // Utilisateur
            g.fillOval(24, 14, 16, 16);
            g.fillArc(14, 32, 36, 30, 0, 180);
        });
    }

    @FunctionalInterface
    interface Painter {
        void paint(Graphics2D g, int size);
    }

    private static void creerIcone(File dir, String nom, Color couleur, Painter painter) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(couleur);
        painter.paint(g, SIZE);
        g.dispose();

        try {
            ImageIO.write(img, "png", new File(dir, nom));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
