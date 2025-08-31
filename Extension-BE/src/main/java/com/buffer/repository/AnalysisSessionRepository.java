package com.buffer.repository;

import com.buffer.entity.AnalysisSession;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for managing AnalysisSession persistence.
 * This abstraction allows for multiple storage implementations:
 * - In-memory (current)
 * - Database (JPA/Hibernate)
 * - Redis cache
 * - File-based storage
 */
public interface AnalysisSessionRepository {
    
    /**
     * Store a complete analysis session with all associated channels and ideas
     * @param session The analysis session to store
     */
    void storeSession(AnalysisSession session);
    
    /**
     * Retrieve an analysis session by its ID
     * @param sessionId The unique session identifier
     * @return The analysis session, or null if not found
     */
    AnalysisSession getSession(String sessionId);
    
    /**
     * Get all stored analysis sessions
     * @return List of all sessions (useful for monitoring/admin purposes)
     */
    List<AnalysisSession> getAllSessions();
    
    /**
     * Get repository statistics and metadata
     * @return Map containing stats like total sessions, recent activity, etc.
     */
    Map<String, Object> getRepositoryStats();
    
    /**
     * Remove sessions that exceed storage limits or are too old
     * Implementation-specific cleanup strategy
     */
    void performCleanup();
}
