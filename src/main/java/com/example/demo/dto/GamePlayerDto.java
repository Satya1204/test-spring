package com.example.demo.dto;

import com.example.demo.entity.GamePlayer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayerDto {
    
    private Long id;
    private PlayerResponse player;
    private Integer playerOrder;
    private LocalDateTime joinedAt;
    private Boolean isActive;
    private Integer cardsCount;
    private Boolean hasCalledUno;
    private List<CardDto> hand; // Only populated for the current player
    
    public static GamePlayerDto fromEntity(GamePlayer gamePlayer, boolean includeHand) {
        GamePlayerDto dto = new GamePlayerDto();
        dto.setId(gamePlayer.getId());
        dto.setPlayer(PlayerResponse.fromEntity(gamePlayer.getPlayer()));
        dto.setPlayerOrder(gamePlayer.getPlayerOrder());
        dto.setJoinedAt(gamePlayer.getJoinedAt());
        dto.setIsActive(gamePlayer.getIsActive());
        dto.setCardsCount(gamePlayer.getCardsCount());
        dto.setHasCalledUno(gamePlayer.getHasCalledUno());
        
        if (includeHand) {
            dto.setHand(gamePlayer.getHand().stream()
                .map(CardDto::fromEntity)
                .toList());
        }
        
        return dto;
    }
}
