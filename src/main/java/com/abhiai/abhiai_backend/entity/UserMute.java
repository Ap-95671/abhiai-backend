package com.abhiai.abhiai_backend.entity;
import java.time.Instant; import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
@Entity @Table(name="user_mutes")
public class UserMute {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="muter_id") private User muter;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="muted_user_id") private User mutedUser;
 @Column(name="muted_term",length=100) private String mutedTerm;
 @Enumerated(EnumType.STRING) @Column(name="mute_type",nullable=false,length=16) private MuteType type;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected UserMute(){} public UserMute(User muter,User user){this.muter=muter;this.mutedUser=user;this.type=MuteType.USER;}
 public UserMute(User muter,String term,MuteType type){this.muter=muter;this.mutedTerm=term;this.type=type;}
 public UUID getId(){return id;} public User getMutedUser(){return mutedUser;} public String getMutedTerm(){return mutedTerm;} public MuteType getType(){return type;} public Instant getCreatedAt(){return createdAt;}
}
