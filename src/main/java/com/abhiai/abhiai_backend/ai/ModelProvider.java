package com.abhiai.abhiai_backend.ai;

/** Raw provider contract. ChatService depends on AiProvider; the orchestrator depends on these adapters. */
public interface ModelProvider extends AiProvider {
}
