# AbhiAI Backend

AbhiAI is a Spring Boot 4 modular monolith combining authenticated AI chat with an
incrementally developed social platform.

## Current backend features

- User registration and BCrypt password hashing
- Stateless JWT login and authorization
- PostgreSQL persistence managed by Flyway
- Stored AI conversations and messages
- Pluggable AI providers with streaming support
- Social user profiles with configurable, case-insensitive usernames
- Follow and unfollow relationships with transactional social counters
- Text posts with visibility rules, author ownership, and soft deletion
- Paginated chronological home feed backed by a replaceable feed strategy
- Post likes and paginated replies with transactional counters
- Reposts and private, visibility-aware bookmark collections
- Durable social notifications with per-user read state
- Visibility-aware PostgreSQL full-text post search and profile search

## Social profile API

All current profile endpoints require a bearer access token. The acting user is
always taken from the validated JWT; clients cannot provide a user ID to edit
another account.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /api/v1/users/{username} | Find a profile by username |
| GET | /api/v1/users/me | Read the authenticated user's profile |
| PATCH | /api/v1/users/me/profile | Update the authenticated user's profile |

Profile responses never include password hashes, JWT data, or email addresses.
Verification status and social counters are read-only and cannot be changed by
the profile update API.

## Follow API

All follow operations derive the acting user from the validated JWT. Follower and
following collections are paginated, capped at 100 records per page, and ordered
newest first.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | /api/v1/users/{userId}/follow | Follow another user |
| DELETE | /api/v1/users/{userId}/follow | Unfollow another user |
| GET | /api/v1/users/{userId}/follow-status | Check the current user's relationship |
| GET | /api/v1/users/{userId}/followers | Get a paginated follower list |
| GET | /api/v1/users/{userId}/following | Get a paginated following list |

Self-following and duplicate relationships are rejected. PostgreSQL constraints
provide a second integrity layer, and follower/following counters are updated
atomically in the same transaction as the relationship.

## Post API

Post text is limited by the app.social.posts.max-text-length configuration
property, currently 1000 Unicode characters. Visibility can be PUBLIC,
FOLLOWERS, or PRIVATE.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | /api/v1/posts | Create a text post |
| GET | /api/v1/posts/{postId} | Read a visible active post |
| PATCH | /api/v1/posts/{postId} | Edit an owned post |
| DELETE | /api/v1/posts/{postId} | Soft-delete an owned post |

Only authors can edit or delete posts. Followers-only visibility is checked
against the persisted follow graph. Deleted posts return 404 and remain stored
so future replies, moderation records, and analytics do not lose referential
integrity. Profile post counters are updated atomically.

## Home feed API

The home feed is authenticated, paginated, capped at 100 posts per page, and
ordered by creation time and post ID in descending order for stable pagination.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /api/v1/feed?page=0&size=20 | Get the authenticated user's home feed |

The feed contains the authenticated user's own active posts at every visibility
level, plus active PUBLIC and FOLLOWERS posts from accounts they follow. It
excludes private posts belonging to other users, posts from unrelated accounts,
and soft-deleted posts.

`FeedService` depends on the `FeedStrategy` interface. Phase 4 uses the
chronological implementation; a future recommendation strategy can be selected
without changing the controller or response contract.

## Post interaction API

All interaction endpoints require JWT authentication and reuse the parent post's
visibility rules. Like and reply lists are capped at 100 records per page and use
stable newest-first ordering.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | /api/v1/posts/{postId}/likes | Like a visible post |
| DELETE | /api/v1/posts/{postId}/likes | Remove the current user's like |
| GET | /api/v1/posts/{postId}/likes/status | Get the current user's like status |
| GET | /api/v1/posts/{postId}/likes?page=0&size=20 | List users who liked the post |
| POST | /api/v1/posts/{postId}/replies | Create a reply on a visible post |
| GET | /api/v1/posts/{postId}/replies?page=0&size=20 | List active replies |
| DELETE | /api/v1/posts/{postId}/replies/{replyId} | Soft-delete an owned reply |

The database prevents duplicate likes and the API returns HTTP 409 for a second
like. Reply text uses the same configured Unicode character limit as post text.
Like and reply counters are updated atomically in the same transactions as their
interactions. Replies inherit access from the parent post and deleted replies are
excluded from lists while remaining available for future moderation records.

## Repost and bookmark API

Reposts are visible interaction metadata. Bookmarks are private: only the
authenticated user can retrieve their own bookmark collection, and there is no
API for listing another user's bookmarks or the users who bookmarked a post.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | /api/v1/posts/{postId}/reposts | Repost a visible post |
| DELETE | /api/v1/posts/{postId}/reposts | Remove the current user's repost |
| GET | /api/v1/posts/{postId}/reposts/status | Get the current user's repost status |
| GET | /api/v1/posts/{postId}/reposts?page=0&size=20 | List users who reposted the post |
| POST | /api/v1/posts/{postId}/bookmarks | Privately bookmark a visible post |
| DELETE | /api/v1/posts/{postId}/bookmarks | Remove the current user's bookmark |
| GET | /api/v1/posts/{postId}/bookmarks/status | Get the current user's bookmark status |
| GET | /api/v1/bookmarks?page=0&size=20 | Get the current user's private bookmarks |

Both interactions are unique per user and post, return HTTP 409 when duplicated,
and update post counters atomically. Bookmark results exclude deleted posts and
posts the current user can no longer view. Repost events are stored for future
profile and feed-item features; Phase 6 does not change the existing home-feed
`PostResponse` contract.

## Notification API

Notifications are durable PostgreSQL records scoped to their recipient. Follow,
like, reply, and repost actions create notifications for the affected account;
self-actions do not create notifications. Results use stable newest-first
pagination and are capped at 100 records per page.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /api/v1/notifications?page=0&size=20 | List the current user's notifications |
| GET | /api/v1/notifications/unread-count | Get the current user's unread count |
| PATCH | /api/v1/notifications/{notificationId}/read | Mark one owned notification as read |
| PATCH | /api/v1/notifications/read-all | Mark all current notifications as read |

A notification cannot be read through another user's account. The API returns
404 instead of revealing whether another user's notification ID exists.

## Search API

Search requires JWT authentication. Queries are trimmed and must contain between
2 and 100 Unicode characters. User search matches case-insensitive username or
display-name prefixes. Post search uses a generated PostgreSQL `tsvector` column
and GIN index, with relevance followed by newest-first stable ordering.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /api/v1/search/users?q=abhi&page=0&size=20 | Search profiles by username or display name |
| GET | /api/v1/search/posts?q=artificial+intelligence&page=0&size=20 | Search visible active posts |

Post results enforce the same access model as direct post reads: public posts are
searchable by authenticated users, followers-only posts require an active follow,
and private posts are visible only to their author. Soft-deleted posts are never
returned. Search pages are capped at 50 results.

## Run and verify

From the backend directory:

    ./mvnw test
    ./mvnw spring-boot:run

From the AbhiAI project directory:

    docker compose up -d --build

Health is available at http://localhost:8080/actuator/health.

## Social roadmap

1. Social profiles — implemented
2. Follow relationships with pagination — implemented
3. Text posts — implemented
4. Chronological home feed — implemented
5. Likes and replies — implemented
6. Reposts and private bookmarks — implemented
7. Notifications and search foundations — implemented

Media, communities, messaging, recommendations, and AI/social integrations will
be added only after the core social graph is stable.

## Free-tier production deployment

The production target is a Render Free Docker web service connected to `main`.
The versioned `render.yaml` uses Java 21, generates the JWT secret inside
Render, checks `GET /actuator/health`, and deploys every new commit.

The production database is Neon PostgreSQL. Provide its pooled connection in
JDBC form with SSL enabled:

```text
jdbc:postgresql://<pooled-neon-host>/<database>?sslmode=require
```

Required Render variables:

- `SPRING_PROFILES_ACTIVE`
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `ALLOWED_ORIGINS`
- `AI_PROVIDER`
- the selected provider key, such as `GEMINI_API_KEY`

Persistent uploads use Supabase Storage through the existing S3-compatible
storage layer. Set `MEDIA_STORAGE_TYPE=s3` and
`MEDIA_S3_PROVIDER=S3_COMPATIBLE`, then configure:

- `MEDIA_S3_BUCKET`
- `MEDIA_S3_ENDPOINT`
- `MEDIA_S3_REGION`
- `MEDIA_S3_PATH_STYLE=true`
- `MEDIA_S3_CHUNKED_ENCODING=false`
- `MEDIA_S3_ACCESS_KEY`
- `MEDIA_S3_SECRET_KEY`

Credentials remain in Render's secret store and must never be added to GitHub.
Using `MEDIA_STORAGE_TYPE=local` temporarily works, but uploads can be lost when
the free Render instance restarts or redeploys.

Set `ALLOWED_ORIGINS` to the exact Vercel HTTPS origin. Multiple exact origins
can be comma-separated when adding a custom domain. Production intentionally
fails fast if required database, JWT, CORS, selected provider, or enabled S3
configuration is missing. Flyway runs before Hibernate schema validation;
production never uses `ddl-auto=update`.
