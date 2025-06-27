package com.example.demo.repository;

import com.example.demo.entity.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    
    List<GameEvent> findByGameIdOrderBySequenceNumber(Long gameId);
    
    List<GameEvent> findByGameIdAndEventTypeOrderBySequenceNumber(Long gameId, String eventType);
    
    @Query("SELECT COALESCE(MAX(ge.sequenceNumber), 0) FROM GameEvent ge WHERE ge.game.id = :gameId")
    Integer getLastSequenceNumber(@Param("gameId") Long gameId);
    
    @Query("SELECT ge FROM GameEvent ge WHERE ge.game.id = :gameId AND ge.sequenceNumber > :afterSequence ORDER BY ge.sequenceNumber")
    List<GameEvent> findEventsAfterSequence(@Param("gameId") Long gameId, @Param("afterSequence") Integer afterSequence);
}
