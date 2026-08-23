package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.UserBlockRepository;

@Service
public class BlockPolicyService {
    private final UserBlockRepository blockRepository;

    public BlockPolicyService(UserBlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    public boolean isBlockedEitherDirection(UUID firstId, UUID secondId) {
        return !firstId.equals(secondId) && blockRepository.existsEitherDirection(firstId, secondId);
    }

    public void requireInteractionAllowed(UUID firstId, UUID secondId) {
        if (isBlockedEitherDirection(firstId, secondId)) {
            throw new UnauthorizedActionException("This interaction is unavailable because one of these accounts is blocked");
        }
    }
}
