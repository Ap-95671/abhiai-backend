package com.abhiai.abhiai_backend.assistant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AssistantPreferencesRepository extends JpaRepository<AssistantPreferences,UUID> {}
