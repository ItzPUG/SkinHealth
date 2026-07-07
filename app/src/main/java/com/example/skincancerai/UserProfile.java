package com.example.skincancerai;

public class UserProfile {

    public String displayName;
    public String avatarBase64;
    public int age;
    public String gender;
    public String phoneNumber;
    public String email;
    public String skinType;
    public String dateOfBirth;

    public boolean isConsent;
    public long consentAt;
    public String termsVersion;

    public UserProfile() {}

    public UserProfile(String displayName, int age, String gender) {
        this.displayName = displayName;
        this.age = age;
        this.gender = gender;
    }

    public UserProfile(String displayName,
                       int age,
                       String gender,
                       String phoneNumber,
                       String email,
                       String skinType) {
        this.displayName = displayName;
        this.age = age;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.skinType = skinType;
    }
}
