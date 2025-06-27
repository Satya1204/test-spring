package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRequest {
    
    @NotBlank(message = "Player name cannot be blank")
    private String playerName;
    
    @NotNull(message = "Coins cannot be null")
    @Min(value = 0, message = "Coins cannot be negative")
    private Integer coins;
}
