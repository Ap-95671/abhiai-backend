package com.abhiai.abhiai_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MessageCitation {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 255)
    private String domain;

    protected MessageCitation() {
    }

    public MessageCitation(String title, String url, String domain) {
        this.title = title;
        this.url = url;
        this.domain = domain;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getDomain() { return domain; }
}
