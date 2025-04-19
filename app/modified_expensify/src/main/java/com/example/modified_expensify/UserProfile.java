package com.example.modified_expensify;

public class UserProfile {
    private String userId;
    private String fullName;
    private String birthDate;
    private String avatar;
    public UserProfile(){}
    public UserProfile(String userId, String fullName, String birthDate, String avatar){
        this.userId = userId;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.avatar = avatar;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getBirthDate() {
        return birthDate;
    }
    public void setBirthDate(String birthDate){
        this.birthDate = birthDate;
    }

    public String getAvatar() {
        return avatar;
    }
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
