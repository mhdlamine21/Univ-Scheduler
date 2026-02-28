package scheduler.service;

import scheduler.modele.*;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.HorizontalAlignment;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'export des emplois du temps et rapports en PDF et Excel.
 */
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter FMT_DATETIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CoursService       coursService;
    private final UtilisateurService utilisateurService;
    private final SalleService       salleService;
    private final MatiereService     matiereService;
    private final ClasseService      classeService;

    // Palette chromatique institutionnelle (indigo et ambre)
    private static final DeviceRgb C_HEADER = new DeviceRgb(26,  31,  58);  // #1A1F3A (indigo foncé)
    private static final DeviceRgb C_HEURE  = new DeviceRgb(238, 240, 255); // #EEF0FF (lavande clair)
    private static final DeviceRgb C_CM     = new DeviceRgb(67,  97,  238); // #4361EE (bleu indigo)
    private static final DeviceRgb C_TD     = new DeviceRgb(45,  198, 83);  // #2DC653 (vert)
    private static final DeviceRgb C_TP     = new DeviceRgb(244, 162, 97);  // #F4A261 (ambre)
    private static final DeviceRgb C_ROUGE  = new DeviceRgb(229, 62,  62);  // #E53E3E (rouge)
    private static final DeviceRgb C_VERT   = new DeviceRgb(45,  198, 83);  // #2DC653 (vert)
    private static final DeviceRgb C_ORANGE = new DeviceRgb(244, 162, 97);  // #F4A261 (ambre)
    private static final DeviceRgb C_GRIS   = new DeviceRgb(123, 131, 196); // #7B83C4 (gris indigo)

    public ExportService() {
        this.coursService       = new CoursService();
        this.utilisateurService = new UtilisateurService();
        this.salleService       = new SalleService();
        this.matiereService     = new MatiereService();
        this.classeService      = new ClasseService();
    }

    
    private static class GrilleConfig {
        int          heureDebut = 8;
        int          heureFin   = 18;
        int          ecart      = 2;
        List<String> jours      = new ArrayList<>();
        List<String> creneauxHoraires = new ArrayList<>();

        void generer() {
            creneauxHoraires.clear();
            for (int h = heureDebut; h < heureFin; h += ecart)
                creneauxHoraires.add(String.format("%02d:00", h));
        }
    }

    private static class InfoCreneau {
        String texte = "";
        String type = "";
        String heureDebut = "";
        String heureFin = "";
    }

    /** Config exacte depuis l'EDT (gestionnaire / étudiant). */
    private GrilleConfig configEDT(EmploiDuTemps edt) {
        GrilleConfig c = new GrilleConfig();
        c.heureDebut = edt.getHeureDebutConfig();
        c.heureFin   = edt.getHeureFinConfig();
        c.ecart      = edt.getEcartConfig();
        c.jours      = new ArrayList<>(edt.getJoursConfigList());
        if (c.jours.isEmpty())
            c.jours = Arrays.asList("Lundi","Mardi","Mercredi","Jeudi","Vendredi");
        c.generer();
        return c;
    }

    /** Config déduite des créneaux (enseignant - ce qui est affiché). */
    private GrilleConfig configCreneaux(List<Creneau> creneaux) {
        GrilleConfig c = new GrilleConfig();
        if (creneaux == null || creneaux.isEmpty()) {
            c.jours = Arrays.asList("Lundi","Mardi","Mercredi","Jeudi","Vendredi");
            c.generer();
            return c;
        }
        int hMin = 23, hMax = 0;
        TreeSet<Integer> ecarts = new TreeSet<>();
        LinkedHashSet<String> joursVus = new LinkedHashSet<>();
        String[] NOMS = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"};

        for (Creneau cr : creneaux) {
            try {
                int hd = Integer.parseInt(cr.getHeureDebut().split(":")[0]);
                int hf = Integer.parseInt(cr.getHeureFin().split(":")[0]);
                hMin = Math.min(hMin, hd);
                hMax = Math.max(hMax, hf);
                if (hf > hd) ecarts.add(hf - hd);
            } catch (Exception ignored) {}
            try {
                LocalDate d = LocalDate.parse(cr.getJour());
                joursVus.add(NOMS[d.getDayOfWeek().getValue() - 1]);
            } catch (Exception ignored) {}
        }
        c.heureDebut = (hMin < 23) ? hMin : 8;
        c.heureFin   = (hMax > 0)  ? hMax : 18;
        c.ecart      = ecarts.isEmpty() ? 1 : ecarts.first();

        List<String> ordre = Arrays.asList("Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi");
        c.jours = ordre.stream().filter(joursVus::contains).collect(Collectors.toList());
        if (c.jours.isEmpty())
            c.jours = Arrays.asList("Lundi","Mardi","Mercredi","Jeudi","Vendredi");
        c.generer();
        return c;
    }

    /**
     * Exporte un emploi du temps en PDF (sans EDT).
     */
    public void exporterEDTGrillePDF(List<Creneau> creneaux, Classe classe, String periode,
                                      Utilisateur utilisateur, String chemin) throws Exception {
        ecrirePDFGrille(creneaux, classe, periode, utilisateur, chemin, configCreneaux(creneaux));
    }

    public void exporterEDTGrillePDF(List<Creneau> creneaux, Classe classe, String periode,
                                      Utilisateur utilisateur, String chemin,
                                      String titrePersonnalise) throws Exception {
        exporterEDTGrillePDF(creneaux, classe, periode, utilisateur, chemin);
    }

    /**
     * Exporte un emploi du temps en PDF avec configuration EDT.
     */
    public void exporterEDTGrillePDF(List<Creneau> creneaux, Classe classe, String periode,
                                      Utilisateur utilisateur, String chemin,
                                      EmploiDuTemps edt) throws Exception {
        GrilleConfig cfg = (edt != null) ? configEDT(edt) : configCreneaux(creneaux);
        ecrirePDFGrille(creneaux, classe, periode, utilisateur, chemin, cfg);
    }

    private void ecrirePDFGrille(List<Creneau> creneaux, Classe classe, String periode,
                                  Utilisateur utilisateur, String chemin,
                                  GrilleConfig cfg) throws Exception {
        try (PdfWriter writer = new PdfWriter(chemin);
             PdfDocument pdf  = new PdfDocument(writer);
             Document doc     = new Document(pdf)) {

            // TITRE
            Paragraph titre = new Paragraph(construireTitreEDT(utilisateur, classe, periode))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16).setBold();
            doc.add(titre);

            Paragraph sousTitre = new Paragraph(
                    periode != null && !periode.isEmpty() ? periode : "")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12);
            doc.add(sousTitre);

            doc.add(new Paragraph("\n").setFontSize(4));

            int nbJours = cfg.jours.size();
            float[] largeurs = new float[nbJours + 1];
            largeurs[0] = 1.5f;
            Arrays.fill(largeurs, 1, largeurs.length, 3f);

            Table table = new Table(UnitValue.createPercentArray(largeurs))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell coin = new Cell().add(new Paragraph("Horaire"))
                    .setBackgroundColor(C_HEADER)
                    .setFontColor(ColorConstants.WHITE)
                    .setBold().setTextAlignment(TextAlignment.CENTER);
            table.addCell(coin);

            for (String jour : cfg.jours) {
                table.addCell(new Cell()
                        .add(new Paragraph(jour))
                        .setBackgroundColor(C_HEADER)
                        .setFontColor(ColorConstants.WHITE)
                        .setBold().setTextAlignment(TextAlignment.CENTER));
            }

            int nbHeures = cfg.creneauxHoraires.size();
            int[] rowsOccupied = new int[cfg.jours.size()];

            for (int i = 0; i < nbHeures; i++) {
                String heure = cfg.creneauxHoraires.get(i);

                table.addCell(new Cell()
                        .add(new Paragraph(heure).setFontSize(9).setBold())
                        .setBackgroundColor(C_HEURE)
                        .setTextAlignment(TextAlignment.CENTER));

                for (int j = 0; j < cfg.jours.size(); j++) {
                    if (rowsOccupied[j] > 0) {
                        rowsOccupied[j]--;
                        continue;
                    }

                    InfoCreneau info = trouver(creneaux, cfg.jours.get(j), heure, cfg.ecart, utilisateur);

                    if (info != null && !info.texte.isEmpty()) {
                        int dureeH = calculerDureeHeures(info.heureDebut, info.heureFin);
                        int rowSpan = Math.max(1, dureeH / cfg.ecart);
                        rowSpan = Math.min(rowSpan, nbHeures - i); // ne pas dépasser

                        DeviceRgb col = couleurPDF(info.type);
                        Cell celluleCours = new Cell(rowSpan, 1);
                        celluleCours.add(new Paragraph(info.texte).setFontSize(9));
                        celluleCours.setBackgroundColor(col, 0.18f);
                        celluleCours.setTextAlignment(TextAlignment.CENTER);
                        table.addCell(celluleCours);

                        if (rowSpan > 1) {
                            rowsOccupied[j] = rowSpan - 1;
                        }
                    } else {
                        table.addCell(new Cell().add(new Paragraph("")));
                    }
                }
            }
            doc.add(table);

            doc.add(new Paragraph("\n").setFontSize(4));
            Table leg = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2}))
                    .setWidth(UnitValue.createPercentValue(55))
                    .setHorizontalAlignment(HorizontalAlignment.LEFT);
            leg.addCell(new Cell().add(new Paragraph("■ CM - Cours Magistral").setFontSize(9))
                    .setBackgroundColor(C_CM, 0.2f).setTextAlignment(TextAlignment.CENTER));
            leg.addCell(new Cell().add(new Paragraph("■ TD - Travaux Dirigés").setFontSize(9))
                    .setBackgroundColor(C_TD, 0.2f).setTextAlignment(TextAlignment.CENTER));
            leg.addCell(new Cell().add(new Paragraph("■ TP - Travaux Pratiques").setFontSize(9))
                    .setBackgroundColor(C_TP, 0.2f).setTextAlignment(TextAlignment.CENTER));
            doc.add(leg);

            // PIED
            doc.add(piedPage());
        }
    }

    
    /**
     * Exporte un emploi du temps en Excel (sans EDT).
     */
    public void exporterEDTGrilleExcel(List<Creneau> creneaux, Classe classe, String periode,
                                        Utilisateur utilisateur, String chemin) throws Exception {
        ecrireExcelGrille(creneaux, classe, periode, utilisateur, chemin, configCreneaux(creneaux));
    }

    /**
     * Exporte un emploi du temps en Excel avec configuration EDT.
     */
    public void exporterEDTGrilleExcel(List<Creneau> creneaux, Classe classe, String periode,
                                        Utilisateur utilisateur, String chemin,
                                        EmploiDuTemps edt) throws Exception {
        GrilleConfig cfg = (edt != null) ? configEDT(edt) : configCreneaux(creneaux);
        ecrireExcelGrille(creneaux, classe, periode, utilisateur, chemin, cfg);
    }

    private void ecrireExcelGrille(List<Creneau> creneaux, Classe classe, String periode,
                                    Utilisateur utilisateur, String chemin,
                                    GrilleConfig cfg) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Emploi du temps");

            CellStyle sHeader = makeHeader(wb);
            CellStyle sHeure  = makeHeure(wb);
            CellStyle sCM     = makeCouleurIndigo(wb);
            CellStyle sTD     = makeCouleurVerte(wb);
            CellStyle sTP     = makeCouleurAmbre(wb);
            CellStyle sVide   = makeVide(wb);

            int nbJours = cfg.jours.size();
            String titreComplet = construireTitreEDT(utilisateur, classe, periode);

            Row rTitre = sheet.createRow(0);
            rTitre.setHeightInPoints(24);
            org.apache.poi.ss.usermodel.Cell cTitre = rTitre.createCell(0);
            cTitre.setCellValue(titreComplet);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, nbJours));

            Row rH = sheet.createRow(2);
            rH.setHeightInPoints(20);
            org.apache.poi.ss.usermodel.Cell cv = rH.createCell(0);
            cv.setCellValue("Horaire"); cv.setCellStyle(sHeader);
            for (int j = 0; j < cfg.jours.size(); j++) {
                org.apache.poi.ss.usermodel.Cell c = rH.createCell(j + 1);
                c.setCellValue(cfg.jours.get(j)); c.setCellStyle(sHeader);
            }

            int rowN = 3;
            int ht = Math.max(28, cfg.ecart * 18);
            
            for (int i = 0; i < cfg.creneauxHoraires.size(); i++) {
                String heure = cfg.creneauxHoraires.get(i);
                Row row = sheet.createRow(rowN++);
                row.setHeightInPoints(ht);

                org.apache.poi.ss.usermodel.Cell ch = row.createCell(0);
                ch.setCellValue(heure);
                ch.setCellStyle(sHeure);

                for (int j = 0; j < cfg.jours.size(); j++) {
                    InfoCreneau info = trouver(creneaux, cfg.jours.get(j), heure, cfg.ecart, utilisateur);
                    
                    if (info != null && !info.texte.isEmpty()) {
                        org.apache.poi.ss.usermodel.Cell cell = row.createCell(j + 1);
                        cell.setCellValue(info.texte);
                        
                        switch (info.type) {
                            case "CM": cell.setCellStyle(sCM); break;
                            case "TD": cell.setCellStyle(sTD); break;
                            case "TP": cell.setCellStyle(sTP); break;
                            default:   cell.setCellStyle(sVide);
                        }
                        
                        // Gestion du rowSpan pour Excel
                        int dureeHeures = calculerDureeHeures(info.heureDebut, info.heureFin);
                        int rowSpan = (dureeHeures / cfg.ecart) + 1;
                        
                        if (rowSpan > 1 && i + rowSpan <= cfg.creneauxHoraires.size()) {
                            sheet.addMergedRegion(new CellRangeAddress(rowN - 1, rowN - 1 + rowSpan - 1, j + 1, j + 1));
                        }
                    } else {
                        row.createCell(j + 1).setCellStyle(sVide);
                    }
                }
            }

            Row rLeg = sheet.createRow(rowN + 1);
            org.apache.poi.ss.usermodel.Cell lCM = rLeg.createCell(0);
            lCM.setCellValue("■ CM - Cours Magistral"); lCM.setCellStyle(sCM);
            org.apache.poi.ss.usermodel.Cell lTD = rLeg.createCell(1);
            lTD.setCellValue("■ TD - Travaux Dirigés"); lTD.setCellStyle(sTD);
            org.apache.poi.ss.usermodel.Cell lTP = rLeg.createCell(2);
            lTP.setCellValue("■ TP - Travaux Pratiques"); lTP.setCellStyle(sTP);

            sheet.setColumnWidth(0, 4200);
            for (int j = 1; j <= nbJours; j++) sheet.setColumnWidth(j, 7000);

            try (FileOutputStream fos = new FileOutputStream(chemin)) { wb.write(fos); }
        }
    }

    
    /**
     * Exporte un rapport statistique en PDF.
     */
    public void exporterRapportPDF(Map<String, Object> rapport, String periode,
                                    String chemin) throws Exception {
        try (PdfWriter writer = new PdfWriter(chemin);
             PdfDocument pdf  = new PdfDocument(writer);
             Document doc     = new Document(pdf)) {

            doc.add(new Paragraph("RAPPORT STATISTIQUE")
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(20).setBold());
            doc.add(new Paragraph(periode)
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(14));
            doc.add(new Paragraph("\n").setFontSize(4));

            // 1. Résumé
            doc.add(secTitle("1.  Résumé général"));
            doc.add(tblResume(rapport));

            // 2. Occupation par heure
            doc.add(secTitle("2.  Occupation des salles par créneau"));
            doc.add(tblOccupation(rapport));

            // 3. Répartition par type
            doc.add(secTitle("3.  Répartition par type de cours"));
            doc.add(tblTypeCours(rapport));

            // 4. Évolution mensuelle
            doc.add(secTitle("4.  Évolution mensuelle (12 derniers mois)"));
            doc.add(tblEvolution(rapport));

            // 5. Détail salles
            doc.add(secTitle("5.  Détail par salle"));
            doc.add(tblSalles(rapport));

            doc.add(piedPage());
        }
    }

    /**
     * Exporte un rapport statistique en Excel (5 feuilles).
     */
    public void exporterRapportExcel(Map<String, Object> rapport, String periode,
                                      String chemin) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            feuilleResume(wb, rapport, periode);
            feuilleOccupation(wb, rapport);
            feuilleTypes(wb, rapport);
            feuilleEvolution(wb, rapport);
            feuilleSalles(wb, rapport);
            try (FileOutputStream fos = new FileOutputStream(chemin)) { wb.write(fos); }
        }
    }

    
    public <T> void exporterListePDF(List<T> items, String titre, String[] colonnes,
                                      Map<String, java.util.function.Function<T, String>> exts,
                                      String chemin) throws Exception {
        try (PdfWriter w = new PdfWriter(chemin);
             PdfDocument pdf = new PdfDocument(w);
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph(titre).setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18).setBold());
            doc.add(new Paragraph("Généré le " + LocalDateTime.now().format(FMT_DATETIME))
                    .setTextAlignment(TextAlignment.RIGHT).setFontSize(9));
            doc.add(new Paragraph("\n"));
            Table t = new Table(UnitValue.createPercentArray(colonnes.length))
                    .setWidth(UnitValue.createPercentValue(100));
            for (String col : colonnes) t.addHeaderCell(new Cell().add(new Paragraph(col)).setBold());
            List<java.util.function.Function<T,String>> fl = new ArrayList<>(exts.values());
            for (T item : items)
                for (java.util.function.Function<T,String> ex : fl)
                    t.addCell(new Cell().add(new Paragraph(ex.apply(item))));
            doc.add(t);
        }
    }

    public <T> void exporterListeExcel(List<T> items, String titre, String[] colonnes,
                                        Map<String, java.util.function.Function<T, String>> exts,
                                        String chemin) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Données");
            Row r0 = s.createRow(0); r0.createCell(0).setCellValue(titre);
            s.addMergedRegion(new CellRangeAddress(0,0,0,colonnes.length-1));
            CellStyle hs = makeHeader(wb);
            Row rh = s.createRow(2);
            for (int i=0;i<colonnes.length;i++) {
                org.apache.poi.ss.usermodel.Cell c = rh.createCell(i);
                c.setCellValue(colonnes[i]); c.setCellStyle(hs);
            }
            List<java.util.function.Function<T,String>> fl = new ArrayList<>(exts.values());
            int rn=3;
            for (T item : items) {
                Row row = s.createRow(rn++); int cn=0;
                for (java.util.function.Function<T,String> ex : fl)
                    row.createCell(cn++).setCellValue(ex.apply(item));
            }
            for (int i=0;i<colonnes.length;i++) s.autoSizeColumn(i);
            try (FileOutputStream fos=new FileOutputStream(chemin)) { wb.write(fos); }
        }
    }

    //  RECHERCHE CRÉNEAU DANS LA GRILLE

    private InfoCreneau trouver(List<Creneau> creneaux, String jourNom, String heureCase,
                                 int ecart, Utilisateur utilisateur) {
        if (creneaux == null) return null;
        int hCase = Integer.parseInt(heureCase.split(":")[0]);
        for (Creneau c : creneaux) {
            try {
                LocalDate d = LocalDate.parse(c.getJour());
                if (!nomJour(d.getDayOfWeek().getValue()).equals(jourNom)) continue;
                int hd = Integer.parseInt(c.getHeureDebut().split(":")[0]);
                if (hd != hCase) continue;
                // Filtre enseignant : ne montrer que ses cours
                if (utilisateur instanceof Enseignant) {
                    Cours cours = coursService.trouverParId(c.getCoursId());
                    if (cours != null && cours.getEnseignantId() != utilisateur.getId()) continue;
                }
                return buildInfo(c, utilisateur);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private InfoCreneau buildInfo(Creneau c, Utilisateur utilisateur) {
        InfoCreneau info = new InfoCreneau();
        info.heureDebut = c.getHeureDebut();
        info.heureFin = c.getHeureFin();
        try {
            Cours cours = coursService.trouverParId(c.getCoursId());
            info.type = cours != null ? cours.getTypeCours() : "";

            String mat = "";
            if (cours != null) {
                Matiere m = matiereService.trouverParId(cours.getMatiereId());
                mat = m != null ? m.getNom() : "Matière";
            }

            StringBuilder sb = new StringBuilder("[").append(info.type).append("] ").append(mat);

            // Étudiant → afficher le prof
            if (!(utilisateur instanceof Enseignant) && cours != null) {
                Utilisateur prof = utilisateurService.trouverParId(cours.getEnseignantId());
                if (prof != null)
                    sb.append("\n").append(prof.getPrenom()).append(" ").append(prof.getNom());
            }
            // Enseignant → afficher la classe
            if (utilisateur instanceof Enseignant && cours != null) {
                Classe cl = classeService.trouverParId(cours.getClasseId());
                if (cl != null) sb.append("\n").append(cl.getIntitule());
            }
            // Salle toujours
            if (c.getSalleId() != null) {
                Salle sal = salleService.trouverParId(c.getSalleId());
                if (sal != null) sb.append("\nSalle ").append(sal.getNumero());
            }
            info.texte = sb.toString();
        } catch (Exception e) { info.texte = "Cours"; }
        return info;
    }

    //  TABLEAUX PDF - RAPPORT

    @SuppressWarnings("unchecked")
    private Table tblResume(Map<String,Object> r) {
        Map<String,Integer> roles = (Map<String,Integer>) r.getOrDefault("utilisateursParRole", new HashMap<>());
        double taux = dbl(r.getOrDefault("tauxOccupationGlobal", 0));
        String[][] data = {
            {"Total cours",        s(r.getOrDefault("totalCours", 0))},
            {"Total salles",       s(r.getOrDefault("totalSalles", 0))},
            {"Enseignants",        s(roles.getOrDefault("enseignant", 0))},
            {"Étudiants",          s(roles.getOrDefault("etudiant", 0))},
            {"Gestionnaires",      s(roles.getOrDefault("gestionnaire", 0))},
            {"Réservations",       s(r.getOrDefault("totalReservations", 0))},
            {"Taux occupation",    String.format("%.2f%%", taux)}
        };
        Table t = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
                .setWidth(UnitValue.createPercentValue(60));
        t.addHeaderCell(new Cell().add(new Paragraph("Indicateur")).setBold());
        t.addHeaderCell(new Cell().add(new Paragraph("Valeur")).setBold());
        for (String[] l : data) {
            t.addCell(new Cell().add(new Paragraph(l[0]).setFontSize(10)));
            t.addCell(new Cell().add(new Paragraph(l[1]).setFontSize(10).setBold())
                    .setTextAlignment(TextAlignment.CENTER));
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private Table tblOccupation(Map<String,Object> r) {
        Map<String,Integer> occ = (Map<String,Integer>) r.getOrDefault("occupationParHeure", new LinkedHashMap<>());
        int max = occ.values().stream().mapToInt(v -> v).max().orElse(1);
        Table t = new Table(UnitValue.createPercentArray(new float[]{2, 2}))
                .setWidth(UnitValue.createPercentValue(60));
        t.addHeaderCell(new Cell().add(new Paragraph("Créneau")).setBold());
        t.addHeaderCell(new Cell().add(new Paragraph("Nb cours")).setBold());
        for (Map.Entry<String,Integer> e : occ.entrySet()) {
            t.addCell(new Cell().add(new Paragraph(e.getKey()).setFontSize(10)));
            int v = e.getValue();
            float pct = max > 0 ? (float) v / max : 0;
            DeviceRgb col = pct > 0.7f ? C_ROUGE : pct > 0.4f ? C_TP : C_TD;
            t.addCell(new Cell().add(new Paragraph(s(v) + " cours").setFontSize(10))
                    .setBackgroundColor(col, 0.18f)
                    .setTextAlignment(TextAlignment.CENTER));
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private Table tblTypeCours(Map<String,Object> r) {
        Map<String,Integer> types = (Map<String,Integer>) r.getOrDefault("coursParType", new LinkedHashMap<>());
        int total = types.values().stream().mapToInt(v -> v).sum();
        Table t = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 1.5f}))
                .setWidth(UnitValue.createPercentValue(60));
        t.addHeaderCell(new Cell().add(new Paragraph("Type")).setBold());
        t.addHeaderCell(new Cell().add(new Paragraph("Nb cours")).setBold());
        t.addHeaderCell(new Cell().add(new Paragraph("%")).setBold());
        Map<String,DeviceRgb> cols = new LinkedHashMap<>();
        cols.put("CM", C_CM); cols.put("TD", C_TD); cols.put("TP", C_TP);
        for (Map.Entry<String,Integer> e : types.entrySet()) {
            int v = e.getValue();
            double pct = total > 0 ? (double) v / total * 100 : 0;
            DeviceRgb col = cols.getOrDefault(e.getKey(), C_GRIS);
            t.addCell(new Cell().add(new Paragraph(e.getKey()).setFontSize(10).setBold())
                    .setBackgroundColor(col, 0.22f).setTextAlignment(TextAlignment.CENTER));
            t.addCell(new Cell().add(new Paragraph(s(v)).setFontSize(10))
                    .setTextAlignment(TextAlignment.CENTER));
            t.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", pct)).setFontSize(10))
                    .setTextAlignment(TextAlignment.CENTER));
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private Table tblEvolution(Map<String,Object> r) {
        Map<String,Double> evo = (Map<String,Double>) r.getOrDefault("evolution", new LinkedHashMap<>());
        Table t = new Table(UnitValue.createPercentArray(new float[]{2, 2}))
                .setWidth(UnitValue.createPercentValue(60));
        t.addHeaderCell(new Cell().add(new Paragraph("Mois")).setBold());
        t.addHeaderCell(new Cell().add(new Paragraph("Taux (%)")).setBold());
        for (Map.Entry<String,Double> e : evo.entrySet()) {
            double v = e.getValue();
            DeviceRgb col = v > 70 ? C_ROUGE : v > 40 ? C_TP : C_TD;
            t.addCell(new Cell().add(new Paragraph(e.getKey()).setFontSize(9)));
            t.addCell(new Cell().add(new Paragraph(String.format("%.2f%%", v)).setFontSize(9))
                    .setBackgroundColor(col, 0.18f).setTextAlignment(TextAlignment.CENTER));
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private Table tblSalles(Map<String,Object> r) {
        List<Map<String,Object>> salles = (List<Map<String,Object>>)
                r.getOrDefault("sallesCritiques", new ArrayList<>());
        Table t = new Table(UnitValue.createPercentArray(new float[]{2, 1.5f, 1.5f, 1.5f, 2}))
                .setWidth(UnitValue.createPercentValue(100));
        for (String h : new String[]{"Salle","Type","Capacité","Taux (%)","Statut"})
            t.addHeaderCell(new Cell().add(new Paragraph(h)).setBold());
        for (Map<String,Object> sal : salles) {
            double taux = dbl(sal.getOrDefault("taux", 0));
            String statut = s(sal.getOrDefault("statut", "normale"));
            DeviceRgb col = "surchargée".equals(statut) ? C_ROUGE
                          : "sous-utilisée".equals(statut) ? C_TP : C_TD;
            t.addCell(new Cell().add(new Paragraph(s(sal.getOrDefault("salle",""))).setFontSize(9)));
            t.addCell(new Cell().add(new Paragraph(s(sal.getOrDefault("type",""))).setFontSize(9)));
            t.addCell(new Cell().add(new Paragraph(s(sal.getOrDefault("capacite",""))).setFontSize(9))
                    .setTextAlignment(TextAlignment.CENTER));
            t.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", taux)).setFontSize(9))
                    .setTextAlignment(TextAlignment.CENTER));
            t.addCell(new Cell().add(new Paragraph(statut).setFontSize(9).setBold())
                    .setBackgroundColor(col, 0.2f).setTextAlignment(TextAlignment.CENTER));
        }
        return t;
    }

    private Paragraph secTitle(String txt) {
        return new Paragraph(txt).setBold().setFontSize(13)
                .setFontColor(C_HEADER).setMarginTop(10).setMarginBottom(5);
    }

    private Paragraph piedPage() {
        return new Paragraph("Généré le " + LocalDateTime.now().format(FMT_DATETIME))
                .setTextAlignment(TextAlignment.RIGHT).setFontSize(8)
                .setFontColor(ColorConstants.GRAY).setMarginTop(8);
    }

    //  FEUILLES EXCEL - RAPPORT

    @SuppressWarnings("unchecked")
    private void feuilleResume(Workbook wb, Map<String,Object> r, String periode) {
        Sheet s = wb.createSheet("Résumé");
        CellStyle hs = makeHeader(wb);
        Row rt = s.createRow(0); rt.createCell(0).setCellValue("RAPPORT - " + periode);
        s.addMergedRegion(new CellRangeAddress(0,0,0,3));
        Map<String,Integer> roles = (Map<String,Integer>) r.getOrDefault("utilisateursParRole", new HashMap<>());
        String[][] data = {
            {"Indicateur","Valeur"},
            {"Total cours",       s(r.getOrDefault("totalCours",0))},
            {"Total salles",      s(r.getOrDefault("totalSalles",0))},
            {"Enseignants",       s(roles.getOrDefault("enseignant",0))},
            {"Étudiants",         s(roles.getOrDefault("etudiant",0))},
            {"Gestionnaires",     s(roles.getOrDefault("gestionnaire",0))},
            {"Réservations",      s(r.getOrDefault("totalReservations",0))},
            {"Taux occupation",   String.format("%.2f%%",dbl(r.getOrDefault("tauxOccupationGlobal",0)))}
        };
        for (int i=0;i<data.length;i++) {
            Row row = s.createRow(i+2);
            org.apache.poi.ss.usermodel.Cell c0 = row.createCell(0); c0.setCellValue(data[i][0]);
            org.apache.poi.ss.usermodel.Cell c1 = row.createCell(1); c1.setCellValue(data[i][1]);
            if (i==0) { c0.setCellStyle(hs); c1.setCellStyle(hs); }
        }
        s.autoSizeColumn(0); s.autoSizeColumn(1);
    }

    @SuppressWarnings("unchecked")
    private void feuilleOccupation(Workbook wb, Map<String,Object> r) {
        Sheet s = wb.createSheet("Occupation par heure");
        CellStyle hs = makeHeader(wb);
        Row rh = s.createRow(0);
        for (String h : new String[]{"Créneau","Nb cours","% relatif"}) {
            int n = rh.getPhysicalNumberOfCells();
            org.apache.poi.ss.usermodel.Cell c = rh.createCell(n);
            c.setCellValue(h); c.setCellStyle(hs);
        }
        Map<String,Integer> occ = (Map<String,Integer>) r.getOrDefault("occupationParHeure", new LinkedHashMap<>());
        int max = occ.values().stream().mapToInt(v->v).max().orElse(1);
        int rn=1;
        for (Map.Entry<String,Integer> e : occ.entrySet()) {
            Row row = s.createRow(rn++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
            row.createCell(2).setCellValue(max>0 ? String.format("%.1f%%",(double)e.getValue()/max*100) : "0%");
        }
        for (int i=0;i<3;i++) s.autoSizeColumn(i);
    }

    @SuppressWarnings("unchecked")
    private void feuilleTypes(Workbook wb, Map<String,Object> r) {
        Sheet s = wb.createSheet("Types de cours");
        CellStyle hs = makeHeader(wb);
        Row rh = s.createRow(0);
        for (String h : new String[]{"Type","Nb cours","%"}) {
            int n = rh.getPhysicalNumberOfCells();
            org.apache.poi.ss.usermodel.Cell c = rh.createCell(n);
            c.setCellValue(h); c.setCellStyle(hs);
        }
        Map<String,Integer> types = (Map<String,Integer>) r.getOrDefault("coursParType", new LinkedHashMap<>());
        int total = types.values().stream().mapToInt(v->v).sum();
        int rn=1;
        for (Map.Entry<String,Integer> e : types.entrySet()) {
            Row row = s.createRow(rn++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
            row.createCell(2).setCellValue(total>0 ? String.format("%.1f%%",(double)e.getValue()/total*100) : "0%");
        }
        for (int i=0;i<3;i++) s.autoSizeColumn(i);
    }

    @SuppressWarnings("unchecked")
    private void feuilleEvolution(Workbook wb, Map<String,Object> r) {
        Sheet s = wb.createSheet("Evolution mensuelle");
        CellStyle hs = makeHeader(wb);
        Row rh = s.createRow(0);
        org.apache.poi.ss.usermodel.Cell c0=rh.createCell(0); c0.setCellValue("Mois"); c0.setCellStyle(hs);
        org.apache.poi.ss.usermodel.Cell c1=rh.createCell(1); c1.setCellValue("Taux (%)"); c1.setCellStyle(hs);
        Map<String,Double> evo = (Map<String,Double>) r.getOrDefault("evolution", new LinkedHashMap<>());
        int rn=1;
        for (Map.Entry<String,Double> e : evo.entrySet()) {
            Row row = s.createRow(rn++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
        s.autoSizeColumn(0); s.autoSizeColumn(1);
    }

    @SuppressWarnings("unchecked")
    private void feuilleSalles(Workbook wb, Map<String,Object> r) {
        Sheet s = wb.createSheet("Detail salles");
        CellStyle hs = makeHeader(wb);
        String[] heads = {"Salle","Type","Capacité","Taux (%)","Heures","Statut"};
        Row rh = s.createRow(0);
        for (int i=0;i<heads.length;i++) {
            org.apache.poi.ss.usermodel.Cell c = rh.createCell(i);
            c.setCellValue(heads[i]); c.setCellStyle(hs);
        }
        List<Map<String,Object>> salles = (List<Map<String,Object>>) r.getOrDefault("sallesCritiques", new ArrayList<>());
        int rn=1;
        for (Map<String,Object> sal : salles) {
            Row row = s.createRow(rn++);
            row.createCell(0).setCellValue(s(sal.getOrDefault("salle","")));
            row.createCell(1).setCellValue(s(sal.getOrDefault("type","")));
            row.createCell(2).setCellValue(it(sal.getOrDefault("capacite",0)));
            row.createCell(3).setCellValue(dbl(sal.getOrDefault("taux",0)));
            row.createCell(4).setCellValue(it(sal.getOrDefault("heures",0)));
            row.createCell(5).setCellValue(s(sal.getOrDefault("statut","")));
        }
        for (int i=0;i<heads.length;i++) s.autoSizeColumn(i);
    }

    //  STYLES EXCEL - COULEURS PERSONNALISÉES

    private CellStyle makeHeader(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        st.setFont(f);
        
        // Couleur indigo foncé #1A1F3A
        XSSFCellStyle xssfStyle = (XSSFCellStyle) st;
        XSSFColor indigoFonce = new XSSFColor(new java.awt.Color(26, 31, 58), null);
        xssfStyle.setFillForegroundColor(indigoFonce);
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        brd(st); 
        return st;
    }

    private CellStyle makeHeure(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); 
        st.setFont(f);
        
        // Couleur lavande clair #EEF0FF
        XSSFCellStyle xssfStyle = (XSSFCellStyle) st;
        XSSFColor lavande = new XSSFColor(new java.awt.Color(238, 240, 255), null);
        xssfStyle.setFillForegroundColor(lavande);
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setWrapText(true); 
        brd(st); 
        return st;
    }

    private CellStyle makeCouleurIndigo(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        XSSFCellStyle xssfStyle = (XSSFCellStyle) st;
        XSSFColor indigo = new XSSFColor(new java.awt.Color(67, 97, 238), null);
        xssfStyle.setFillForegroundColor(indigo);
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setWrapText(true);
        brd(st);
        return st;
    }

    private CellStyle makeCouleurVerte(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        XSSFCellStyle xssfStyle = (XSSFCellStyle) st;
        XSSFColor vert = new XSSFColor(new java.awt.Color(45, 198, 83), null);
        xssfStyle.setFillForegroundColor(vert);
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setWrapText(true);
        brd(st);
        return st;
    }

    private CellStyle makeCouleurAmbre(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        XSSFCellStyle xssfStyle = (XSSFCellStyle) st;
        XSSFColor ambre = new XSSFColor(new java.awt.Color(244, 162, 97), null);
        xssfStyle.setFillForegroundColor(ambre);
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setWrapText(true);
        brd(st);
        return st;
    }

    private CellStyle makeVide(Workbook wb) {
        CellStyle st = wb.createCellStyle(); 
        brd(st); 
        return st;
    }

    private void brd(CellStyle st) {
        st.setBorderBottom(BorderStyle.THIN); 
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);   
        st.setBorderRight(BorderStyle.THIN);
    }

    //  UTILITAIRES

    private String construireTitreEDT(Utilisateur u, Classe classe, String periode) {
        StringBuilder sb = new StringBuilder("EMPLOI DU TEMPS");
        if (u instanceof Enseignant)
            sb.append(" - ").append(u.getPrenom()).append(" ").append(u.getNom());
        else if (classe != null)
            sb.append(" - ").append(classe.getIntitule());
        if (periode != null && !periode.isEmpty())
            sb.append("  |  ").append(periode);
        return sb.toString();
    }

    private String heureFin(String debut, int ecart) {
        try { return String.format("%02d:00", Integer.parseInt(debut.split(":")[0]) + ecart); }
        catch (Exception e) { return "?"; }
    }

    private int calculerDureeHeures(String debut, String fin) {
        try {
            String[] d = debut.split(":");
            String[] f = fin.split(":");
            int debutMin = Integer.parseInt(d[0]) * 60 + Integer.parseInt(d[1]);
            int finMin = Integer.parseInt(f[0]) * 60 + Integer.parseInt(f[1]);
            return (finMin - debutMin) / 60;
        } catch (Exception e) {
            return 1;
        }
    }

    private DeviceRgb couleurPDF(String type) {
        switch (type) {
            case "CM": return C_CM;   // #4361EE - indigo
            case "TD": return C_TD;   // #2DC653 - vert
            case "TP": return C_TP;   // #F4A261 - ambre
            default: return C_GRIS;   // #7B83C4 - gris indigo
        }
    }

    private String nomJour(int dow) {
        switch (dow) {
            case 1: return "Lundi";    case 2: return "Mardi";
            case 3: return "Mercredi"; case 4: return "Jeudi";
            case 5: return "Vendredi"; case 6: return "Samedi";
            default: return "Dimanche";
        }
    }

    private String s(Object o)   { return o == null ? "" : String.valueOf(o); }
    private int    it(Object o)  { try { return ((Number)o).intValue();    } catch (Exception e) { return 0; } }
    private double dbl(Object o) { try { return ((Number)o).doubleValue(); } catch (Exception e) { return 0.0; } }
}