package com.abhiai.abhiai_backend.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.auth.RegisterUserRequest;
import com.abhiai.abhiai_backend.dto.auth.RegisteredUserResponse;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.EmailAlreadyRegisteredException;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernamePolicy usernamePolicy;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UsernamePolicy usernamePolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.usernamePolicy = usernamePolicy;
    }

    @Transactional
    public RegisteredUserResponse register(RegisterUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        String username = usernamePolicy.generateInitialUsername(
                normalizedEmail,
                userRepository::existsByUsernameIgnoreCase);

        User user = new User(
                username,
                request.displayName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()));

        return RegisteredUserResponse.from(userRepository.saveAndFlush(user));
    }
}
