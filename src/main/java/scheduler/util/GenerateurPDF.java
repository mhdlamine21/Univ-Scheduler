package scheduler.util;

import scheduler.modele.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Générateur de documents PDF pour l'application.
 */
public final class GenerateurPDF {
    
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    private GenerateurPDF() {
    }
    
    public static void genererEmploiDuTemps(List<Creneau> creneaux, Classe classe, String periode, String cheminFichier) {
        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(cheminFichier));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            Paragraph titre = new Paragraph("EMPLOI DU TEMPS")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20)
                .setBold();
            document.add(titre);
            
            String classeNom = (classe != null && classe.getIntitule() != null) ? classe.getIntitule() : "Classe";
            Paragraph soustitre = new Paragraph(classeNom + " - " + periode)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16);
            document.add(soustitre);
            
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 20, 20, 20, 25}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            table.addHeaderCell(new Cell().add(new Paragraph("Jour")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Horaire")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Matière")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Salle")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Enseignant")).setBold());
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            for (Creneau c : creneaux) {
                if (c == null) continue;
                
                LocalDateTime jour = LocalDateTime.parse(c.getJour() + "T00:00:00");
                table.addCell(new Paragraph(jour.format(dateFormatter)));
                table.addCell(new Paragraph(c.getHeureDebut() + " - " + c.getHeureFin()));
                table.addCell(new Paragraph("Matière #" + c.getCoursId()));
                table.addCell(new Paragraph(c.getSalleId() != null ? "Salle " + c.getSalleId() : "Non assignée"));
                table.addCell(new Paragraph("Enseignant"));
            }
            
            document.add(table);
            
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Généré le " + LocalDateTime.now().format(FORMAT_DATE))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10));
            
            document.close();
            
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
    
    public static void genererRapportOccupation(List<Salle> salles, List<Creneau> creneaux, String periode, String cheminFichier) {
        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(cheminFichier));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            Paragraph titre = new Paragraph("RAPPORT D'OCCUPATION DES SALLES")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20)
                .setBold();
            document.add(titre);
            
            Paragraph soustitre = new Paragraph("Période : " + periode)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16);
            document.add(soustitre);
            
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{20, 15, 15, 25, 25}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            table.addHeaderCell(new Cell().add(new Paragraph("Salle")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Capacité")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Type")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Nb Cours")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Taux Occupation")).setBold());
            
            for (Salle s : salles) {
                long nbCours = creneaux.stream()
                    .filter(c -> c.getSalleId() != null && c.getSalleId() == s.getId())
                    .count();
                
                double taux = (nbCours * 100.0) / 30;
                
                table.addCell(new Paragraph(s.getNumero()));
                table.addCell(new Paragraph(String.valueOf(s.getCapacite())));
                table.addCell(new Paragraph(s.getType()));
                table.addCell(new Paragraph(String.valueOf(nbCours)));
                table.addCell(new Paragraph(String.format("%.1f%%", taux)));
            }
            
            document.add(table);
            document.close();
            
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }
    
    public static void genererListeEtudiants(List<Etudiant> etudiants, Classe classe, String cheminFichier) {
        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(cheminFichier));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            Paragraph titre = new Paragraph("LISTE DES ÉTUDIANTS")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20)
                .setBold();
            document.add(titre);
            
            String classeNom = (classe != null && classe.getIntitule() != null) ? classe.getIntitule() : "Classe";
            Paragraph soustitre = new Paragraph(classeNom)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16);
            document.add(soustitre);
            
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 20, 20, 30, 15}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            table.addHeaderCell(new Cell().add(new Paragraph("N°")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Nom")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Prénom")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Email")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Groupe")).setBold());
            
            int i = 1;
            for (Etudiant e : etudiants) {
                table.addCell(new Paragraph(String.valueOf(i++)));
                table.addCell(new Paragraph(e.getNom()));
                table.addCell(new Paragraph(e.getPrenom()));
                table.addCell(new Paragraph(e.getEmail()));
                table.addCell(new Paragraph("G1"));
            }
            
            document.add(table);
            document.close();
            
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération de la liste", e);
        }
    }
}