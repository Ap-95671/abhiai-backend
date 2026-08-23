package com.abhiai.abhiai_backend.entity;
import java.time.Instant;import java.util.UUID;import org.hibernate.annotations.*;import jakarta.persistence.*;
@Entity @Table(name="follow_requests") public class FollowRequest{
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="requester_id") private User requester;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="target_id") private User target;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private FollowRequestStatus status=FollowRequestStatus.PENDING;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @UpdateTimestamp @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected FollowRequest(){} public FollowRequest(User r,User t){requester=r;target=t;}
 public UUID getId(){return id;}public User getRequester(){return requester;}public User getTarget(){return target;}public FollowRequestStatus getStatus(){return status;}public Instant getCreatedAt(){return createdAt;}
 public void status(FollowRequestStatus s){status=s;}
}
