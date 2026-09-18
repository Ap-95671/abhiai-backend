package com.abhiai.abhiai_backend.assistant;

import java.util.UUID;
import jakarta.persistence.*;
@Entity @Table(name="assistant_preferences")
public class AssistantPreferences {
    @Id public UUID userId;
    public String mode = "STANDARD";
    public boolean pageContext = true;
    public boolean agentActions;
    public boolean proactive;
    public String projectKey = "";
    public boolean fallbackAllowed;
    public AssistantPreferences() {}
    public AssistantPreferences(UUID userId) { this.userId=userId; }
}
