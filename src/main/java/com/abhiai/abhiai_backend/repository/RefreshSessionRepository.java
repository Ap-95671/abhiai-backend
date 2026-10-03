package com.abhiai.abhiai_backend.repository;

import com.abhiai.abhiai_backend.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> { }
