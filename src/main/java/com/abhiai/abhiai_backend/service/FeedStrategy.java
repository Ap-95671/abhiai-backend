package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.abhiai.abhiai_backend.entity.Post;

public interface FeedStrategy {

    Page<Post> load(UUID userId, Pageable pageable);
}
