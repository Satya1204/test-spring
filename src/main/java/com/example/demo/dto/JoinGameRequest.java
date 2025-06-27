package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinGameRequest {
    
    @NotBlank(message = "Game code cannot be blank")
    private String gameCode;
    
    @NotNull(message = "Player ID cannot be null")
    private Long playerId;
}
