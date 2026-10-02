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

    @Column(name = "description", length = 1600) private String description;
    @Column(name = "source_date", length = 80) private String sourceDate;
    @Column(name = "retrieved_at") private java.time.Instant retrievedAt;
    public MessageCitation(String title, String url, String domain, String description, String sourceDate, java.time.Instant retrievedAt) {
        this(title, url, domain); this.description=description; this.sourceDate=sourceDate; this.retrievedAt=retrievedAt;
    }
    public String getDescription() { return description; }
    public String getSourceDate() { return sourceDate; }
    public java.time.Instant getRetrievedAt() { return retrievedAt; }

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
