package com.disk91.users.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.CustomField;

import java.util.ArrayList;

public class UserProfile implements CloneableObject<UserProfile> {

    // User first name (encrypted)
    private String firstName;

    // User last name (encrypted)
    private String lastName;

    // User gender (free text, front decides) encrypted
    private String gender;

    // phone number (encrypted) - e164 format, ex: +33601020304
    private String phoneNumber;

    // address (encrypted)
    private String address;

    // city (encrypted)
    private String city;

    // zip code (encrypted)
    private String zipCode;

    // iso country code (encrypted) - expl : FR, US, etc.
    private String country;

    // Custom fields, contains key / value pairs, encrypted
    private ArrayList<CustomField> customFields;

    // === GETTER / SETTER ===

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public ArrayList<CustomField> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(ArrayList<CustomField> customFields) {
        this.customFields = customFields;
    }

    // === CLONE ===

    public UserProfile clone() {
        UserProfile u = new UserProfile();
        u.setFirstName(this.firstName);
        u.setLastName(this.lastName);
        u.setGender(this.gender);
        u.setPhoneNumber(this.phoneNumber);
        u.setAddress(this.address);
        u.setCity(this.city);
        u.setZipCode(this.zipCode);
        u.setCountry(this.country);
        if (this.customFields != null) {
            ArrayList<CustomField> cf = new ArrayList<>();
            for (CustomField c : this.customFields) {
                cf.add(c.clone());
            }
            u.setCustomFields(cf);
        }
        return u;
    }

}
