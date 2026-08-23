package com.abhiai.abhiai_backend.service;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.report.*;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.exception.*;
import com.abhiai.abhiai_backend.repository.*;

@Service
public class ContentReportService {
 private final ContentReportRepository reports; private final UserRepository users; private final PostAccessService postAccess;
 private final PostReplyRepository postReplies; private final ArticleCommentRepository articleComments; private final CommunityRepository communities;
 private final DirectMessageRepository directMessages; private final DirectConversationRepository directConversations;
 private final GroupMessageRepository groupMessages; private final GroupParticipantRepository groupParticipants;
 public ContentReportService(ContentReportRepository reports,UserRepository users,PostAccessService postAccess,PostReplyRepository postReplies,ArticleCommentRepository articleComments,CommunityRepository communities,DirectMessageRepository directMessages,DirectConversationRepository directConversations,GroupMessageRepository groupMessages,GroupParticipantRepository groupParticipants){this.reports=reports;this.users=users;this.postAccess=postAccess;this.postReplies=postReplies;this.articleComments=articleComments;this.communities=communities;this.directMessages=directMessages;this.directConversations=directConversations;this.groupMessages=groupMessages;this.groupParticipants=groupParticipants;}

 @Transactional public ContentReportResponse create(UUID reporterId,CreateReportRequest request){
  User reporter=users.findById(reporterId).orElseThrow(UserNotFoundException::new);validateShape(request);User reported=resolveAndAuthorizeTarget(reporterId,request);
  if(reported!=null&&reported.getId().equals(reporterId))throw new InvalidContentReportException("You cannot report your own content");
  if(reports.existsByReporterIdAndTargetTypeAndTargetContextAndTargetId(reporterId,request.targetType(),request.targetContext(),request.targetId()))throw new DuplicateContentReportException();
  String details=request.details()==null||request.details().isBlank()?null:request.details().trim();
  try{return ContentReportResponse.from(reports.saveAndFlush(new ContentReport(reporter,reported,request.targetType(),request.targetContext(),request.targetId(),request.reason(),details)));}catch(DataIntegrityViolationException e){throw new DuplicateContentReportException();}
 }
 @Transactional(readOnly=true) public PageResponse<ContentReportResponse> mine(UUID reporterId,Pageable pageable){if(!users.existsById(reporterId))throw new UserNotFoundException();Pageable normalized=PageRequest.of(Math.max(0,pageable.getPageNumber()),Math.max(1,Math.min(pageable.getPageSize(),50)),Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));return PageResponse.from(reports.findByReporterId(reporterId,normalized),ContentReportResponse::from);}
 private void validateShape(CreateReportRequest request){boolean noContext=request.targetContext()==null;if((request.targetType()==ReportTargetType.POST||request.targetType()==ReportTargetType.USER||request.targetType()==ReportTargetType.COMMUNITY)&&!noContext)throw new InvalidContentReportException("This target type must not include a context");if(request.targetType()==ReportTargetType.COMMENT&&(request.targetContext()!=ReportTargetContext.POST_REPLY&&request.targetContext()!=ReportTargetContext.ARTICLE_COMMENT))throw new InvalidContentReportException("Comment reports require POST_REPLY or ARTICLE_COMMENT context");if(request.targetType()==ReportTargetType.MESSAGE&&(request.targetContext()!=ReportTargetContext.DIRECT_MESSAGE&&request.targetContext()!=ReportTargetContext.GROUP_MESSAGE))throw new InvalidContentReportException("Message reports require DIRECT_MESSAGE or GROUP_MESSAGE context");}
 private User resolveAndAuthorizeTarget(UUID reporterId,CreateReportRequest request){return switch(request.targetType()){
  case USER->users.findById(request.targetId()).orElseThrow(UserNotFoundException::new);
  case POST->{Post post=postAccess.findViewablePost(reporterId,request.targetId());yield post.getAuthor();}
  case COMMUNITY->{Community community=communities.findById(request.targetId()).orElseThrow(CommunityNotFoundException::new);yield community.getOwner();}
  case COMMENT->resolveComment(reporterId,request);
  case MESSAGE->resolveMessage(reporterId,request);
 };}
 private User resolveComment(UUID reporterId,CreateReportRequest request){if(request.targetContext()==ReportTargetContext.POST_REPLY){PostReply reply=postReplies.findByIdAndDeletedAtIsNull(request.targetId()).orElseThrow(()->new InvalidContentReportException("Comment not found"));postAccess.findViewablePost(reporterId,reply.getPost().getId());return reply.getAuthor();}ArticleComment comment=articleComments.findByIdAndDeletedAtIsNull(request.targetId()).orElseThrow(()->new InvalidContentReportException("Comment not found"));return comment.getAuthor();}
 private User resolveMessage(UUID reporterId,CreateReportRequest request){if(request.targetContext()==ReportTargetContext.DIRECT_MESSAGE){DirectMessage message=directMessages.findById(request.targetId()).filter(item->item.getDeletedAt()==null).orElseThrow(()->new InvalidContentReportException("Message not found"));directConversations.findAccessible(message.getConversation().getId(),reporterId).orElseThrow(()->new UnauthorizedActionException("You cannot report a message outside your conversation"));return message.getSender();}GroupMessage message=groupMessages.findById(request.targetId()).filter(item->item.getDeletedAt()==null).orElseThrow(()->new InvalidContentReportException("Message not found"));if(!groupParticipants.existsByConversationIdAndUserId(message.getConversation().getId(),reporterId))throw new UnauthorizedActionException("You cannot report a message outside your group");return message.getSender();}
}
