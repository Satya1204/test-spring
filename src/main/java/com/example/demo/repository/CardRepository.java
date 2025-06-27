package com.example.demo.repository;

import com.example.demo.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    
    List<Card> findByGameIdAndIsInDeckTrue(Long gameId);
    
    List<Card> findByGameIdAndPlayerIdAndIsInDeckFalseAndIsTopCardFalse(Long gameId, Long playerId);
    
    Optional<Card> findByGameIdAndIsTopCardTrue(Long gameId);
    
    @Query("SELECT c FROM Card c WHERE c.game.id = :gameId AND c.isInDeck = true ORDER BY FUNCTION('RANDOM')")
    List<Card> findDeckCardsRandomOrder(@Param("gameId") Long gameId);
    
    @Query("SELECT COUNT(c) FROM Card c WHERE c.game.id = :gameId AND c.player.id = :playerId AND c.isInDeck = false AND c.isTopCard = false")
    Integer countPlayerCards(@Param("gameId") Long gameId, @Param("playerId") Long playerId);
    
    @Query("SELECT c FROM Card c WHERE c.game.id = :gameId AND c.isInDeck = false AND c.isTopCard = false AND c.player IS NULL")
    List<Card> findDiscardPileCards(@Param("gameId") Long gameId);
}
