package com.example.demo.repository;

import com.example.demo.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
    
    List<GamePlayer> findByGameIdOrderByPlayerOrder(Long gameId);
    
    Optional<GamePlayer> findByGameIdAndPlayerId(Long gameId, Long playerId);
    
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.game.id = :gameId AND gp.isActive = true ORDER BY gp.playerOrder")
    List<GamePlayer> findActivePlayersByGame(@Param("gameId") Long gameId);
    
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.game.id = :gameId AND gp.isActive = true")
    Integer countActivePlayersByGame(@Param("gameId") Long gameId);
    
    boolean existsByGameIdAndPlayerId(Long gameId, Long playerId);
}
