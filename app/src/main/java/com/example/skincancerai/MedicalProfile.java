package com.example.skincancerai;

public class MedicalProfile {

    public String id;
    public String fullName;
    public int age;
    public String gender;
    public String skinHistory;
    public String note;
    public String dateOfBirth;
    public MedicalProfile() {}

    public MedicalProfile(String id, String fullName, int age,
                          String gender, String skinHistory, String note) {
        this.id = id;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.skinHistory = skinHistory;
        this.note = note;
    }
}
