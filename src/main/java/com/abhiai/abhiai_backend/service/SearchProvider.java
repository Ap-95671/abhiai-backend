package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.abhiai.abhiai_backend.entity.Hashtag;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.User;

public interface SearchProvider {

    Page<User> searchUsers(String query, Pageable pageable);

    Page<Post> searchPosts(
            UUID actingUserId,
            String query,
            PostSearchCriteria criteria,
            Pageable pageable);

    Page<Hashtag> searchHashtags(String query, Pageable pageable);
}
