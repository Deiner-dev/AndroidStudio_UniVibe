package com.gamevault.univibe;

public class User {
    private String uid;
    private String fullName;
    private String institutionalEmail;
    private String university;
    private String career;
    private String semester;
    private String aboutMe;
    private String imgPerfil;

    public User() {
    }

    public User(String fullName, String institutionalEmail, String university) {
        this.fullName = fullName;
        this.institutionalEmail = institutionalEmail;
        this.university = university;
        this.career = "Ingeniería de Sistemas"; // Default value
        this.semester = "4º semestre"; // Default value
        this.aboutMe = "¡Hola! Estoy usando UniVibe."; // Default value
        this.imgPerfil = ""; 
    }

    public String getUid() {
        return uid != null ? uid : "";
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getInstitutionalEmail() {
        return institutionalEmail;
    }

    public void setInstitutionalEmail(String institutionalEmail) {
        this.institutionalEmail = institutionalEmail;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getCareer() {
        return career;
    }

    public void setCareer(String career) {
        this.career = career;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public String getImgPerfil() {
        return imgPerfil;
    }

    public void setImgPerfil(String imgPerfil) {
        this.imgPerfil = imgPerfil;
    }
}
