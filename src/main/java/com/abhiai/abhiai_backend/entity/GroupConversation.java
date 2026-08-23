package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "group_conversations")
public class GroupConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupParticipant> participants = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GroupConversation() {
    }

    public GroupConversation(String name, String imageUrl, User owner) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.owner = owner;
        addParticipant(owner, GroupRole.OWNER);
    }

    public GroupParticipant addParticipant(User user, GroupRole role) {
        GroupParticipant participant = new GroupParticipant(this, user, role);
        participants.add(participant);
        touch();
        return participant;
    }

    public void removeParticipant(GroupParticipant participant) {
        participants.remove(participant);
        touch();
    }

    public void updateDetails(String name, String imageUrl, boolean updateImage) {
        if (name != null) this.name = name;
        if (updateImage) this.imageUrl = imageUrl;
        touch();
    }

    public void transferOwnership(User nextOwner) {
        owner = nextOwner;
        touch();
    }

    public void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public User getOwner() { return owner; }
    public List<GroupParticipant> getParticipants() { return List.copyOf(participants); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
