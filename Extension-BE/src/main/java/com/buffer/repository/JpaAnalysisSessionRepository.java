package com.buffer.repository;

import com.buffer.domain.entity.AnalysisSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository interface for AnalysisSession entity.
 * Extends JpaRepository to provide CRUD operations and custom queries.
 */
@Repository
public interface JpaAnalysisSessionRepository extends JpaRepository<AnalysisSession, String> {
    
    /**
     * Find sessions ordered by creation date (the newest first)
     * @return List of sessions ordered by creation date
     */
    List<AnalysisSession> findAllByOrderByCreatedAtDesc();
}
