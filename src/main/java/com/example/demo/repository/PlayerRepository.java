package com.example.demo.repository;

import com.example.demo.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    
    Optional<Player> findByPlayerName(String playerName);
    
    boolean existsByPlayerName(String playerName);
    
    @Query("SELECT p FROM Player p ORDER BY p.coins DESC")
    List<Player> findAllOrderByCoinsDesc();
    
    @Query("SELECT p FROM Player p WHERE p.coins >= :minCoins")
    List<Player> findPlayersWithMinCoins(@Param("minCoins") Integer minCoins);
}
