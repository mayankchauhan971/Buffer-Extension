package com.buffer.database;

import com.buffer.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContentIdeaDatabase {
    private static final Logger logger = LoggerFactory.getLogger(ContentIdeaDatabase.class);
    private static final int MAX_SESSIONS = 50;
    
    // Core storage maps
    private final Map<String, Context> contextSessions = new ConcurrentHashMap<>();
    private final Map<String, Channel> channels = new ConcurrentHashMap<>(); 
    private final Map<String, Idea> ideas = new ConcurrentHashMap<>();
    
    // Session ordering for LRU cleanup
    private final LinkedList<String> sessionOrder = new LinkedList<>();
    
    /**
     * Store a complete context session with channels and ideas
     */
    public synchronized void storeContextSession(Context session) {
        logger.debug("Storing context session: {}", session.getSessionId());
        
        // Manage session limits
        if (sessionOrder.size() >= MAX_SESSIONS) {
            removeOldestSession();
        }
        
        // Store the session
        contextSessions.put(session.getSessionId(), session);
        sessionOrder.addLast(session.getSessionId());
        
        // Store all channels and ideas, to support future chats per idea/channel
        for (Channel channel : session.getChannels()) {
            channels.put(channel.getChannelId(), channel);
            
            for (Idea idea : channel.getIdeas()) {
                ideas.put(idea.getIdeaId(), idea);
            }
        }
        
        logger.info("Stored context session with {} channels and {} total ideas", 
            session.getChannels().size(), 
            session.getChannels().stream().mapToInt(c -> c.getIdeas().size()).sum());
    }
    
    /**
     * Get a context session by ID
     */
    public Context getContextSession(String sessionId) {
        Context session = contextSessions.get(sessionId);
        if (session != null) {
            updateSessionOrder(sessionId);
        }
        return session;
    }

    /**
     * Get all context sessions (for monitoring)
     */
    public List<Context> getAllContextSessions() {
        return new ArrayList<>(contextSessions.values());
    }

    /**
     * Get database statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", contextSessions.size());
        stats.put("maxSessions", MAX_SESSIONS);
        stats.put("totalChannels", channels.size());
        stats.put("totalIdeas", ideas.size());
        
        // Recent activity
        long recentSessions = contextSessions.values().stream()
                .filter(session -> session.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1)))
                .count();
        stats.put("recentlyActiveSessions", recentSessions);
        
        return stats;
    }
    
    /**
     * Remove the oldest session to make room for new ones
     */
    private void removeOldestSession() {
        if (sessionOrder.isEmpty()) return;
        
        String oldestSessionId = sessionOrder.removeFirst();
        Context removedSession = contextSessions.remove(oldestSessionId);
        
        if (removedSession != null) {
            // Clean up associated channels and ideas
            for (Channel channel : removedSession.getChannels()) {
                channels.remove(channel.getChannelId());
                
                for (Idea idea : channel.getIdeas()) {
                    ideas.remove(idea.getIdeaId());
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