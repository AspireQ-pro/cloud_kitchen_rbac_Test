package com.cloudkitchen.rbac.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Customer profile update request")
public class CustomerUpdateRequest {
    
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    @Schema(description = "First name", example = "John")
    @JsonProperty("firstName")
    private String firstName;
    
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    @Schema(description = "Last name", example = "Doe")
    @JsonProperty("lastName")
    private String lastName;
    
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Schema(description = "Email", example = "john@example.com")
    @JsonProperty("email")
    private String email;
    
    @Schema(description = "Address", example = "123 Main Street")
    @JsonProperty("address")
    private String address;
    
    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Schema(description = "City", example = "Mumbai")
    @JsonProperty("city")
    private String city;
    
    @Size(max = 100, message = "State cannot exceed 100 characters")
    @Schema(description = "State", example = "Maharashtra")
    @JsonProperty("state")
    private String state;
    
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    @Schema(description = "Country", example = "India")
    @JsonProperty("country")
    private String country;
    
    @Size(max = 10, message = "Pincode cannot exceed 10 characters")
    @Schema(description = "Pincode", example = "400001")
    @JsonProperty("pincode")
    private String pincode;
    
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of birth", example = "1990-01-15")
    @JsonProperty("dob")
    private LocalDate dob;
    
    @Size(max = 255, message = "Favorite food cannot exceed 255 characters")
    @Schema(description = "Favorite food", example = "Pizza")
    @JsonProperty("favoriteFood")
    private String favoriteFood;

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { 
        this.firstName = firstName != null ? firstName.trim() : null; 
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { 
        this.lastName = lastName != null ? lastName.trim() : null; 
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getFavoriteFood() { return favoriteFood; }
    public void setFavoriteFood(String favoriteFood) { this.favoriteFood = favoriteFood; }
}