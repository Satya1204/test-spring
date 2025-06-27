package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnoCallRequest {
    
    @NotNull(message = "Player ID cannot be null")
    private Long playerId;
    
    @NotNull(message = "Game code cannot be null")
    private String gameCode;
}
