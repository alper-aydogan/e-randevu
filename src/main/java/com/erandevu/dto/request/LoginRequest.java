package com.erandevu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User login request")
public class LoginRequest {
    
    @NotBlank(message = "Username is required")
    @Schema(example = "johndoe")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Schema(example = "Password123")
    private String password;

    // Manual getters (since Lombok not working)
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
