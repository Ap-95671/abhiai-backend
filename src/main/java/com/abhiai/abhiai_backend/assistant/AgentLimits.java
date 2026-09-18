package com.abhiai.abhiai_backend.assistant;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;
@Component @Validated @ConfigurationProperties(prefix="app.assistant.agent")
public class AgentLimits {
    @Min(2) @Max(20) private int maxSteps=10;
    @Min(1) @Max(20) private int maxToolCalls=8;
    @Min(30) @Max(600) private int maxDurationSeconds=240;
    @Min(4000) @Max(64000) private int maxContextSize=24000;
    @Min(0) @Max(5) private int maxRetries=2;
    public int getMaxSteps(){return maxSteps;} public void setMaxSteps(int v){maxSteps=v;}
    public int getMaxToolCalls(){return maxToolCalls;} public void setMaxToolCalls(int v){maxToolCalls=v;}
    public int getMaxDurationSeconds(){return maxDurationSeconds;} public void setMaxDurationSeconds(int v){maxDurationSeconds=v;}
    public int getMaxContextSize(){return maxContextSize;} public void setMaxContextSize(int v){maxContextSize=v;}
    public int getMaxRetries(){return maxRetries;} public void setMaxRetries(int v){maxRetries=v;}
}
