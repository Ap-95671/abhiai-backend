package com.abhiai.abhiai_backend.ai.orchestration;

import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.exception.AiProviderFailureKind;

public record StageResult(ExecutionPlan.Assignment assignment, Status status, AiCompletion completion,
                          AiProviderFailureKind failure, long latencyMs) {
    public enum Status { SUCCESS, TIMEOUT, RATE_LIMITED, FAILED, SKIPPED }
    public boolean successful() { return status == Status.SUCCESS && completion != null; }
}
