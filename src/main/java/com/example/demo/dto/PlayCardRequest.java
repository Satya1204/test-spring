package com.example.demo.dto;

import com.example.demo.enums.CardColor;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayCardRequest {
    
    @NotNull(message = "Card ID cannot be null")
    private Long cardId;
    
    @NotNull(message = "Player ID cannot be null")
    private Long playerId;
    
    @NotNull(message = "Game code cannot be null")
    private String gameCode;
    
    // For wild cards - the color the player chooses
    private CardColor chosenColor;
}
