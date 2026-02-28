package scheduler.modele;

import java.time.LocalDateTime;

/**
 * Représente une demande d'inscription en attente de validation
 */
public class DemandeInscription {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String roleDemande;
    private String numeroEtudiant;
    private Integer ufrId;
    private Integer classeId;
    private String matriculeEnseignant;
    private LocalDateTime dateDemande;
    private String statut;
    private String grade;
    private String typeEtudiant = "normal";

    public DemandeInscription() {}
    
    public DemandeInscription(String nom, String prenom, String email, String roleDemande) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.roleDemande = roleDemande;
        this.dateDemande = LocalDateTime.now();
        this.statut = "en_attente";
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getRoleDemande() { return roleDemande; }
    public void setRoleDemande(String roleDemande) { this.roleDemande = roleDemande; }
    
    public String getNumeroEtudiant() { return numeroEtudiant; }
    public void setNumeroEtudiant(String numeroEtudiant) { this.numeroEtudiant = numeroEtudiant; }
    
    public Integer getUfrId() { return ufrId; }
    public void setUfrId(Integer ufrId) { this.ufrId = ufrId; }
    
    public Integer getClasseId() { return classeId; }
    public void setClasseId(Integer classeId) { this.classeId = classeId; }
    
    public String getMatriculeEnseignant() { return matriculeEnseignant; }
    public void setMatriculeEnseignant(String matriculeEnseignant) { this.matriculeEnseignant = matriculeEnseignant; }
    
    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }
    
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getTypeEtudiant() { return typeEtudiant; }
    public void setTypeEtudiant(String typeEtudiant) { this.typeEtudiant = typeEtudiant; }
    public boolean estResponsableClasse() { return "responsable".equals(typeEtudiant); }
}