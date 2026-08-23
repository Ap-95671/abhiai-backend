package com.abhiai.abhiai_backend.repository;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
import com.abhiai.abhiai_backend.entity.*;
public interface UserMuteRepository extends JpaRepository<UserMute,UUID>{
 boolean existsByMuterIdAndMutedUserId(UUID muterId,UUID mutedUserId);
 Optional<UserMute> findByMuterIdAndMutedUserId(UUID muterId,UUID mutedUserId);
 Optional<UserMute> findByMuterIdAndMutedTermAndType(UUID muterId,String term,MuteType type);
 List<UserMute> findAllByMuterId(UUID muterId);
}
