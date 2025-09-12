package com.fintech.authservice.dto.message;

public record UserCreationMessage(String userId,
                                  String firstName,
                                  String lastName,
                                  String email,
                                  String phoneNumber,
                                  String address,
                                  String dateOfBirth,
                                  String occupation,
                                  Double initialDeposit
) {
}
