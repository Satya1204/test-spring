package com.example.demo.dto;

import com.example.demo.entity.Card;
import com.example.demo.enums.CardColor;
import com.example.demo.enums.CardType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    
    private Long id;
    private CardType cardType;
    private CardColor color;
    private Integer value;
    private Integer positionInHand;
    
    public static CardDto fromEntity(Card card) {
        return new CardDto(
            card.getId(),
            card.getCardType(),
            card.getColor(),
            card.getValue(),
            card.getPositionInHand()
        );
    }
    
    public String getDisplayName() {
        if (cardType == CardType.NUMBER) {
            return color.name() + " " + value;
        } else if (cardType == CardType.WILD || cardType == CardType.WILD_DRAW_FOUR) {
            return cardType.name().replace("_", " ");
        } else {
            return color.name() + " " + cardType.name().replace("_", " ");
        }
    }
}
