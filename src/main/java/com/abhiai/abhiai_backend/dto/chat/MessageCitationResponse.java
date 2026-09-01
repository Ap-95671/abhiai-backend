package com.abhiai.abhiai_backend.dto.chat;

import com.abhiai.abhiai_backend.entity.MessageCitation;

public record MessageCitationResponse(String title, String url, String domain) {

    public static MessageCitationResponse from(MessageCitation citation) {
        return new MessageCitationResponse(citation.getTitle(), citation.getUrl(), citation.getDomain());
    }
}
