package com.fintech.userservice.dto.request;

import com.fintech.userservice.security.annotation.FieldAccessControl;

public class UpdateUserRequest {
    @FieldAccessControl(resourceType = "user", fieldName = "firstName")
    private String firstName;
    @FieldAccessControl(resourceType = "user", fieldName = "lastName")
    private String lastName;
    @FieldAccessControl(resourceType = "user", fieldName = "email")
    private String email;
    @FieldAccessControl(resourceType = "user", fieldName = "phoneNumber")
    private String phoneNumber;
    @FieldAccessControl(resourceType = "user", fieldName = "address")
    private String address;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String firstName, String lastName, String email, String phoneNumber, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }
}
