package com.abhiai.abhiai_backend.ai.pipeline;

import com.abhiai.abhiai_backend.ai.orchestration.*;

/** Persisted internal attribution. Never part of the public message DTO. */
public record AiExecutionMetadata(String requestId, Intent intent, TaskType taskType, RequestComplexity complexity,
                                  ExecutionStrategy strategy, Integer qualityScore) { }
