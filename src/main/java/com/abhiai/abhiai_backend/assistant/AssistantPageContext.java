package com.abhiai.abhiai_backend.assistant;

import jakarta.validation.constraints.*;
/** References, never arbitrary database objects or HTML. Titles are display-only. */
public record AssistantPageContext(
    @NotBlank @Pattern(regexp="news|post|profile|feed|search|conversation|document|other") String pageType,
    @Size(max=240) String route, @Size(max=160) String entityId,
    @Size(max=160) String parentId, @Size(max=240) String title,
    @Size(max=2000) String selectedText, boolean externalProcessingAllowed) {}
