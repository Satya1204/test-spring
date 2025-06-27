package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameRequest {
    
    @NotNull(message = "Player ID cannot be null")
    private Long playerId;
    
    @Min(value = 2, message = "Minimum players must be at least 2")
    @Max(value = 4, message = "Maximum players cannot exceed 4")
    private Integer maxPlayers = 4;
    
    @Min(value = 2, message = "Minimum players must be at least 2")
    private Integer minPlayers = 2;
}
