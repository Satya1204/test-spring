package com.example.demo.repository;

import com.example.demo.entity.Game;
import com.example.demo.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    Optional<Game> findByGameCode(String gameCode);
    
    boolean existsByGameCode(String gameCode);
    
    List<Game> findByStatus(GameStatus status);
    
    @Query("SELECT g FROM Game g WHERE g.status = :status AND SIZE(g.gamePlayers) < g.maxPlayers")
    List<Game> findAvailableGames(@Param("status") GameStatus status);
    
    @Query("SELECT g FROM Game g JOIN g.gamePlayers gp WHERE gp.player.id = :playerId AND g.status IN :statuses")
    List<Game> findPlayerActiveGames(@Param("playerId") Long playerId, @Param("statuses") List<GameStatus> statuses);
    
    @Query("SELECT g FROM Game g WHERE g.createdBy.id = :playerId")
    List<Game> findGamesByCreator(@Param("playerId") Long playerId);
}
