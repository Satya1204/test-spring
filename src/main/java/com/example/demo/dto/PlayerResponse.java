package com.example.demo.dto;

import com.example.demo.entity.Player;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerResponse {
    
    private Long id;
    private String playerName;
    private Integer coins;
    
    public static PlayerResponse fromEntity(Player player) {
        return new PlayerResponse(
            player.getId(),
            player.getPlayerName(),
            player.getCoins()
        );
    }
}
