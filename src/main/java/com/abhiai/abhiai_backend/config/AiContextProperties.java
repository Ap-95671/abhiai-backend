package com.abhiai.abhiai_backend.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="app.ai.context") public class AiContextProperties{
 private int maxMessages=40;private int maxCharacters=60000;
 public int getMaxMessages(){return maxMessages;}public void setMaxMessages(int value){maxMessages=value;}
 public int getMaxCharacters(){return maxCharacters;}public void setMaxCharacters(int value){maxCharacters=value;}
}
