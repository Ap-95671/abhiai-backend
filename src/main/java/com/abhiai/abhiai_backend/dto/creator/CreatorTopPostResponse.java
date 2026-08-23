package com.abhiai.abhiai_backend.dto.creator;
import java.util.UUID;
public record CreatorTopPostResponse(UUID postId,String textContent,long impressions,long uniqueViewers,long engagements,double engagementRate){}
