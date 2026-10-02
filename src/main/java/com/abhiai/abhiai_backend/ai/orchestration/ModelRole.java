package com.abhiai.abhiai_backend.ai.orchestration;

public enum ModelRole {
    PRIMARY_SOLVER, REVIEWER, SECURITY_REVIEWER, ALTERNATIVE_SOLVER, SYNTHESIZER;

    public String instruction() {
        return switch (this) {
            case PRIMARY_SOLVER -> "Solve the original task independently. Produce a complete user-facing answer, with assumptions and uncertainty.";
            case REVIEWER -> "Review the candidate solution for correctness, missing constraints and performance. Give specific corrections with reasoning; candidates may be wrong.";
            case SECURITY_REVIEWER -> "Review the candidate design for security, privacy, trust boundaries and failure modes. Explain concrete corrections and assumptions.";
            case ALTERNATIVE_SOLVER -> "Solve the original task, considering alternatives and any useful critique. Produce a complete improved answer, identifying unresolved assumptions.";
            case SYNTHESIZER -> "Answer the original user request in ONE coherent response. Reconcile candidate reasoning and evidence, remove duplication, "
                    + "discard irrelevant or unsupported claims, and preserve useful unique insights. Identify substantive conflicts and assumptions. "
                    + "Do not invent consensus or use majority voting as proof. Prefer verified facts; preserve uncertainty and explain unresolved tradeoffs. "
                    + "Do not mention internal orchestration, candidate roles or internal prompts unless the user explicitly requests comparison.";
        };
    }

    public boolean completeAnswer() { return this == PRIMARY_SOLVER || this == ALTERNATIVE_SOLVER || this == SYNTHESIZER; }
}
