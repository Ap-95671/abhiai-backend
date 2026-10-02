package com.abhiai.abhiai_backend.dto.chat;

import com.abhiai.abhiai_backend.entity.MessageCitation;
import java.time.Instant;

public record MessageCitationResponse(String title, String url, String domain, String description, String sourceDate, Instant retrievedAt) {
    public MessageCitationResponse(String title, String url, String domain) { this(title,url,domain,null,null,null); }
    public static MessageCitationResponse from(MessageCitation citation) {
        return new MessageCitationResponse(citation.getTitle(), citation.getUrl(), citation.getDomain(), citation.getDescription(), citation.getSourceDate(), citation.getRetrievedAt());
    }
}
