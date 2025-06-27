package com.example.demo.util;

import com.example.demo.entity.Card;
import com.example.demo.enums.CardColor;
import com.example.demo.enums.CardType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnoGameRulesTest {

    @Test
    void canPlayCard_SameColor_ShouldReturnTrue() {
        Card topCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        Card cardToPlay = new Card(CardType.NUMBER, CardColor.RED, 3);
        
        assertTrue(UnoGameRules.canPlayCard(cardToPlay, topCard));
    }

    @Test
    void canPlayCard_SameNumber_ShouldReturnTrue() {
        Card topCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        Card cardToPlay = new Card(CardType.NUMBER, CardColor.BLUE, 5);
        
        assertTrue(UnoGameRules.canPlayCard(cardToPlay, topCard));
    }

    @Test
    void canPlayCard_SameActionType_ShouldReturnTrue() {
        Card topCard = new Card(CardType.SKIP, CardColor.RED, null);
        Card cardToPlay = new Card(CardType.SKIP, CardColor.BLUE, null);
        
        assertTrue(UnoGameRules.canPlayCard(cardToPlay, topCard));
    }

    @Test
    void canPlayCard_WildCard_ShouldReturnTrue() {
        Card topCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        Card wildCard = new Card(CardType.WILD, CardColor.WILD, null);
        
        assertTrue(UnoGameRules.canPlayCard(wildCard, topCard));
    }

    @Test
    void canPlayCard_WildDrawFour_ShouldReturnTrue() {
        Card topCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        Card wildDrawFour = new Card(CardType.WILD_DRAW_FOUR, CardColor.WILD, null);
        
        assertTrue(UnoGameRules.canPlayCard(wildDrawFour, topCard));
    }

    @Test
    void canPlayCard_DifferentColorAndNumber_ShouldReturnFalse() {
        Card topCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        Card cardToPlay = new Card(CardType.NUMBER, CardColor.BLUE, 3);
        
        assertFalse(UnoGameRules.canPlayCard(cardToPlay, topCard));
    }

    @Test
    void isValidColorChoice_ValidColors_ShouldReturnTrue() {
        assertTrue(UnoGameRules.isValidColorChoice(CardColor.RED));
        assertTrue(UnoGameRules.isValidColorChoice(CardColor.BLUE));
        assertTrue(UnoGameRules.isValidColorChoice(CardColor.GREEN));
        assertTrue(UnoGameRules.isValidColorChoice(CardColor.YELLOW));
    }

    @Test
    void isValidColorChoice_WildColor_ShouldReturnFalse() {
        assertFalse(UnoGameRules.isValidColorChoice(CardColor.WILD));
    }

    @Test
    void requiresColorChoice_WildCards_ShouldReturnTrue() {
        Card wildCard = new Card(CardType.WILD, CardColor.WILD, null);
        Card wildDrawFour = new Card(CardType.WILD_DRAW_FOUR, CardColor.WILD, null);
        
        assertTrue(UnoGameRules.requiresColorChoice(wildCard));
        assertTrue(UnoGameRules.requiresColorChoice(wildDrawFour));
    }

    @Test
    void requiresColorChoice_RegularCards_ShouldReturnFalse() {
        Card numberCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        Card skipCard = new Card(CardType.SKIP, CardColor.BLUE, null);
        
        assertFalse(UnoGameRules.requiresColorChoice(numberCard));
        assertFalse(UnoGameRules.requiresColorChoice(skipCard));
    }

    @Test
    void getCardDrawCount_ShouldReturnCorrectCounts() {
        Card drawTwo = new Card(CardType.DRAW_TWO, CardColor.RED, null);
        Card wildDrawFour = new Card(CardType.WILD_DRAW_FOUR, CardColor.WILD, null);
        Card numberCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        
        assertEquals(2, UnoGameRules.getCardDrawCount(drawTwo));
        assertEquals(4, UnoGameRules.getCardDrawCount(wildDrawFour));
        assertEquals(0, UnoGameRules.getCardDrawCount(numberCard));
    }

    @Test
    void causesSkip_ShouldReturnTrueForSkipCards() {
        Card skipCard = new Card(CardType.SKIP, CardColor.RED, null);
        Card drawTwo = new Card(CardType.DRAW_TWO, CardColor.RED, null);
        Card wildDrawFour = new Card(CardType.WILD_DRAW_FOUR, CardColor.WILD, null);
        Card numberCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        
        assertTrue(UnoGameRules.causesSkip(skipCard));
        assertTrue(UnoGameRules.causesSkip(drawTwo));
        assertTrue(UnoGameRules.causesSkip(wildDrawFour));
        assertFalse(UnoGameRules.causesSkip(numberCard));
    }

    @Test
    void causesReverse_ShouldReturnTrueForReverseCards() {
        Card reverseCard = new Card(CardType.REVERSE, CardColor.RED, null);
        Card numberCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        
        assertTrue(UnoGameRules.causesReverse(reverseCard));
        assertFalse(UnoGameRules.causesReverse(numberCard));
    }

    @Test
    void getCardPoints_ShouldReturnCorrectPoints() {
        Card numberCard = new Card(CardType.NUMBER, CardColor.RED, 7);
        Card skipCard = new Card(CardType.SKIP, CardColor.RED, null);
        Card wildCard = new Card(CardType.WILD, CardColor.WILD, null);
        
        assertEquals(7, UnoGameRules.getCardPoints(numberCard));
        assertEquals(20, UnoGameRules.getCardPoints(skipCard));
        assertEquals(50, UnoGameRules.getCardPoints(wildCard));
    }

    @Test
    void hasPlayableCard_ShouldReturnTrueWhenPlayableCardExists() {
        Card topCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new Card(CardType.NUMBER, CardColor.BLUE, 3),
            new Card(CardType.NUMBER, CardColor.RED, 7), // Playable (same color)
            new Card(CardType.SKIP, CardColor.GREEN, null)
        );
        
        assertTrue(UnoGameRules.hasPlayableCard(hand, topCard));
    }

    @Test
    void hasPlayableCard_ShouldReturnFalseWhenNoPlayableCard() {
        Card topCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        List<Card> hand = Arrays.asList(
            new Card(CardType.NUMBER, CardColor.BLUE, 3),
            new Card(CardType.NUMBER, CardColor.GREEN, 7),
            new Card(CardType.SKIP, CardColor.YELLOW, null)
        );
        
        assertFalse(UnoGameRules.hasPlayableCard(hand, topCard));
    }

    @Test
    void calculateScore_ShouldReturnCorrectTotal() {
        List<Card> hand = Arrays.asList(
            new Card(CardType.NUMBER, CardColor.RED, 5),    // 5 points
            new Card(CardType.SKIP, CardColor.BLUE, null),  // 20 points
            new Card(CardType.WILD, CardColor.WILD, null)   // 50 points
        );
        
        assertEquals(75, UnoGameRules.calculateScore(hand));
    }

    @Test
    void isValidFirstCard_ShouldReturnTrueForNumberCards() {
        Card numberCard = new Card(CardType.NUMBER, CardColor.RED, 5);
        assertTrue(UnoGameRules.isValidFirstCard(numberCard));
    }

    @Test
    void isValidFirstCard_ShouldReturnFalseForActionCards() {
        Card skipCard = new Card(CardType.SKIP, CardColor.RED, null);
        Card wildCard = new Card(CardType.WILD, CardColor.WILD, null);
        
        assertFalse(UnoGameRules.isValidFirstCard(skipCard));
        assertFalse(UnoGameRules.isValidFirstCard(wildCard));
    }
}
