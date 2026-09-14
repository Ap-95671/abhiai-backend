package com.abhiai.abhiai_backend.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;

@Component
@Validated
@ConfigurationProperties(prefix = "app.assistant")
public class AssistantProperties {
    private boolean enabled = true;
    private boolean voiceEnabled = true;
    @NotBlank private String model = "gemini-3.1-flash-live-preview";
    @NotBlank private String voice = "Kore";
    @Min(1) @Max(50) private int sessionsPerHour = 12;
    @Min(1) @Max(5) private int concurrentSessions = 1;
    @Min(60) @Max(3600) private int maxSessionSeconds = 900;
    @Min(30) @Max(600) private int idleSeconds = 120;
    @Min(1) @Max(120) private int requestsPerMinute = 30;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public boolean isVoiceEnabled() { return voiceEnabled; }
    public void setVoiceEnabled(boolean v) { voiceEnabled = v; }
    public String getModel() { return model; }
    public void setModel(String v) { model = v; }
    public String getVoice() { return voice; }
    public void setVoice(String v) { voice = v; }
    public int getSessionsPerHour() { return sessionsPerHour; }
    public void setSessionsPerHour(int v) { sessionsPerHour = v; }
    public int getConcurrentSessions() { return concurrentSessions; }
    public void setConcurrentSessions(int v) { concurrentSessions = v; }
    public int getMaxSessionSeconds() { return maxSessionSeconds; }
    public void setMaxSessionSeconds(int v) { maxSessionSeconds = v; }
    public int getIdleSeconds() { return idleSeconds; }
    public void setIdleSeconds(int v) { idleSeconds = v; }
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int v) { requestsPerMinute = v; }
}
