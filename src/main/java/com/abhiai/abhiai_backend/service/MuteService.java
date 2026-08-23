package com.abhiai.abhiai_backend.service;
import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import com.abhiai.abhiai_backend.dto.user.*; import com.abhiai.abhiai_backend.entity.*; import com.abhiai.abhiai_backend.exception.*; import com.abhiai.abhiai_backend.repository.*;
@Service public class MuteService{
 private final UserMuteRepository repo; private final UserRepository users;
 public MuteService(UserMuteRepository repo,UserRepository users){this.repo=repo;this.users=users;}
 @Transactional public MuteResponse add(UUID actor,MuteRequest request){User me=user(actor);UserMute mute;
  if(request.type()==MuteType.USER){UUID id=parse(request.userId());if(actor.equals(id))throw new UnauthorizedActionException("You cannot mute yourself");User target=user(id);mute=repo.findByMuterIdAndMutedUserId(actor,id).orElseGet(()->repo.save(new UserMute(me,target)));}
  else{String term=normalize(request.term());mute=repo.findByMuterIdAndMutedTermAndType(actor,term,request.type()).orElseGet(()->repo.save(new UserMute(me,term,request.type())));}return MuteResponse.from(repo.saveAndFlush(mute));}
 @Transactional public void remove(UUID actor,UUID id){UserMute mute=repo.findById(id).orElseThrow(()->new UserNotFoundException());if(!repo.findAllByMuterId(actor).contains(mute))throw new UnauthorizedActionException("Mute does not belong to you");repo.delete(mute);}
 @Transactional(readOnly=true) public List<MuteResponse> list(UUID actor){user(actor);return repo.findAllByMuterId(actor).stream().map(MuteResponse::from).toList();}
 public boolean muted(UUID viewer,UUID author,String text){if(repo.existsByMuterIdAndMutedUserId(viewer,author))return true;String lower=text==null?"":text.toLowerCase(Locale.ROOT);return repo.findAllByMuterId(viewer).stream().filter(m->m.getType()!=MuteType.USER).anyMatch(m->lower.contains(m.getMutedTerm()));}
 private User user(UUID id){return users.findById(id).orElseThrow(UserNotFoundException::new);} private UUID parse(String s){try{return UUID.fromString(s);}catch(Exception e){throw new UnauthorizedActionException("A valid userId is required");}} private String normalize(String s){String t=s==null?"":s.trim().toLowerCase(Locale.ROOT).replaceFirst("^#","");if(t.isBlank())throw new UnauthorizedActionException("A mute term is required");return t;}
}
