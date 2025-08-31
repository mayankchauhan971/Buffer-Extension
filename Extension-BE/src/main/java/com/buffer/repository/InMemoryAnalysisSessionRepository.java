package com.buffer.repository;

import com.buffer.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of AnalysisSessionRepository.
 * Stores sessions in ConcurrentHashMap with LRU eviction strategy.
 * 
 * Future implementations could include:
 * - JpaAnalysisSessionRepository (SQL database)
 * - RedisAnalysisSessionRepository (Redis cache)
 * - MongoAnalysisSessionRepository (MongoDB)
 */
@Repository
public class InMemoryAnalysisSessionRepository implements AnalysisSessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryAnalysisSessionRepository.class);
    private static final int MAX_SESSIONS = 50;
    
    // Core storage maps
    private final Map<String, AnalysisSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, SocialMediaChannel> channels = new ConcurrentHashMap<>();
    private final Map<String, ContentIdea> ideas = new ConcurrentHashMap<>();
    
    // Session ordering for LRU cleanup
    private final LinkedList<String> sessionOrder = new LinkedList<>();
    
    @Override
    public synchronized void storeSession(AnalysisSession session) {
        logger.debug("Storing analysis session: {}", session.getSessionId());
        
        // Manage session limits
        if (sessionOrder.size() >= MAX_SESSIONS) {
            performCleanup();
        }
        
        // Store the session
        sessions.put(session.getSessionId(), session);
        sessionOrder.addLast(session.getSessionId());
        
        // Store all channels and ideas, to support future chats per idea/channel
        for (SocialMediaChannel socialMediaChannel : session.getSocialMediaChannels()) {
            channels.put(socialMediaChannel.getChannelId(), socialMediaChannel);
            
            for (ContentIdea contentIdea : socialMediaChannel.getContentIdeas()) {
                ideas.put(contentIdea.getIdeaId(), contentIdea);
            }
        }
        
        logger.info("Stored analysis session with {} channels and {} total ideas", 
            session.getSocialMediaChannels().size(),
            session.getSocialMediaChannels().stream().mapToInt(c -> c.getContentIdeas().size()).sum());
    }
    
    @Override
    public AnalysisSession getSession(String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session != null) {
            updateSessionOrder(sessionId);
        }
        return session;
    }

    @Override
    public List<AnalysisSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    @Override
    public Map<String, Object> getRepositoryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("implementation", "InMemory");
        stats.put("totalSessions", sessions.size());
        stats.put("maxSessions", MAX_SESSIONS);
        stats.put("totalChannels", channels.size());
        stats.put("totalIdeas", ideas.size());
        
        // Recent activity
        long recentSessions = sessions.values().stream()
                .filter(session -> session.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1)))
                .count();
        stats.put("recentlyActiveSessions", recentSessions);
        
        return stats;
    }
    
    @Override
    public void performCleanup() {
        removeOldestSession();
    }
    
    /**
     * Remove the oldest session to make room for new ones
     */
    private void removeOldestSession() {
        if (sessionOrder.isEmpty()) return;
        
        String oldestSessionId = sessionOrder.removeFirst();
        AnalysisSession removedSession = sessions.remove(oldestSessionId);
        
        if (removedSession != null) {
            // Clean up associated channels and ideas
            for (SocialMediaChannel socialMediaChannel : removedSession.getSocialMediaChannels()) {
                channels.remove(socialMediaChannel.getChannelId());
                
                for (ContentIdea contentIdea : socialMediaChannel.getContentIdeas()) {
                    ideas.remove(contentIdea.getIdeaId());
                }
            }
            
            logger.info("Removed oldest session: {} (max limit reached)", oldestSessionId);
        }
    }
    
    /**
     * Update session order for LRU management
     */
    private void updateSessionOrder(String sessionId) {
        if (sessionOrder.contains(sessionId)) {
            sessionOrder.remove(sessionId);
            sessionOrder.addLast(sessionId);
        }
    }
}
