package com.buffer.repository;

import com.buffer.entity.AnalysisSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Database-based implementation of AnalysisSessionRepository.
 * Uses SQLite database for persistent storage via Spring Data JPA.
 */
@Slf4j
@Component
@Primary
public class DatabaseAnalysisSessionRepository implements AnalysisSessionRepository {
    
    private final JpaAnalysisSessionRepository jpaRepository;
    
    @Autowired
    public DatabaseAnalysisSessionRepository(JpaAnalysisSessionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public void storeSession(AnalysisSession session) {
        log.debug("Storing analysis session: {}", session.getSessionId());
        
        try {
            AnalysisSession savedSession = jpaRepository.save(session);
            log.info("Stored analysis session with {} channels and {} total ideas", 
                savedSession.getSocialMediaChannels().size(),
                savedSession.getSocialMediaChannels().stream().mapToInt(c -> c.getContentIdeas().size()).sum());
        } catch (Exception e) {
            log.error("Failed to store analysis session: {}", session.getSessionId(), e);
            throw new RuntimeException("Failed to store session", e);
        }
    }
    
    @Override
    public AnalysisSession getSession(String sessionId) {
        log.debug("Retrieving analysis session: {}", sessionId);
        
        try {
            return jpaRepository.findById(sessionId).orElse(null);
        } catch (Exception e) {
            log.error("Failed to retrieve analysis session: {}", sessionId, e);
            return null;
        }
    }
    
    @Override
    public List<AnalysisSession> getAllSessions() {
        log.debug("Retrieving all analysis sessions");
        
        try {
            return jpaRepository.findAllByOrderByCreatedAtDesc();
        } catch (Exception e) {
            log.error("Failed to retrieve all sessions", e);
            throw new RuntimeException("Failed to retrieve sessions", e);
        }
    }

}
