# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Project: Mini LLM Chat Agent

A minimal Android chat application with an LLM agent, built for a learning challenge (AI Advent, days 6–8). The goal is a working agent with conversation persistence and token accounting — NOT a polished product. Minimal UI, clean code, no bugs.

## Scope by challenge day
- **Day 6 — Agent:** an `Agent` entity that encapsulates the request→response logic to the LLM (not just a raw API call).
- **Day 7 — Context persistence:** store conversation history (messages) in Room; reload on restart; continue the dialog as if never closed. Multiple chats, each with its own history (chat list in a drawer).
- **Day 8 — Token accounting:** count tokens for the current request, full history, and model response; show how tokens/cost grow and what breaks on context overflow.

Build the skeleton to support all three days from the start.

## Build commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "karpiuk.ivan.miniagent.domain.agent.AgentTest"

# Run a single test method (backtick names must be URL-encoded in shell)
./gradlew test --tests "karpiuk.ivan.miniagent.domain.agent.AgentTest.sends full history on each request"

# Lint
./gradlew lint

# Check (compile + test + lint)
./gradlew check

# Install on connected device/emulator
./gradlew installDebug
```

## Tech stack
- **Language:** Kotlin (Java 11 source/target compatibility)
- **UI:** Jetpack Compose (minimal — no theming/animations/polish; function over form, but no bugs)
- **Architecture:** MVVM + Clean Architecture, **single module**
- **DI:** Hilt
- **Networking:** Retrofit + OkHttp, **non-streaming** (full response only)
- **JSON:** **kotlinx.serialization** — requires the `kotlin("plugin.serialization")` Gradle plugin AND a Retrofit kotlinx-serialization converter (e.g. `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter`). Do not forget either the plugin or the converter.
- **Persistence:** Room (entities: Chat, Message)
- **Async:** Coroutines + Kotlin Flow. Repositories expose `Flow` for observable data and `suspend` functions for one-shot operations. No blocking calls.

### Key dependencies not yet in `libs.versions.toml`
Add these to `gradle/libs.versions.toml` when implementing each layer:
- Hilt: `com.google.dagger:hilt-android`, `com.google.dagger:hilt-compiler`, plugin `com.google.dagger.hilt.android`
- Room: `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler` (KSP)
- Retrofit: `com.squareup.retrofit2:retrofit`, `com.squareup.okhttp3:okhttp`, `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter`
- kotlinx.serialization: `org.jetbrains.kotlinx:kotlinx-serialization-json`, plugin `org.jetbrains.kotlin.plugin.serialization`
- KSP plugin: required for Room and Hilt annotation processing
- kotlinx.datetime: `org.jetbrains.kotlinx:kotlinx-datetime`

## Architecture rules (strict)
- Three layers: **domain**, **data**, **ui**. Dependencies point inward: `ui → domain`, `data → domain`. **domain depends on nothing.**
- **domain is pure Kotlin** — NO Android, Room, Retrofit, or framework imports. Only Kotlin + coroutines/Flow.
- The **Agent** lives in domain. It encapsulates the LLM request/response logic and depends ONLY on the repository **interface** (defined in domain), never on a concrete implementation. The Agent is a distinct entity, not a thin wrapper around a single API call.
- Repository **interface** in domain; **implementation** in data.
- Map between domain models and data models (Room entities / network DTOs) with explicit mappers. Domain models never carry Room/Retrofit annotations.
- ViewModels (ui) talk to domain (Agent / use cases / repository interface), never directly to Retrofit or Room.
- The Agent assembles the outgoing message list from stored history through a SINGLE point, so future context-management (truncation/summarization) can be added there without touching UI.

## Package structure

Root package: `karpiuk.ivan.miniagent`

    app/src/main/java/karpiuk/ivan/miniagent/
    │
    ├── data/
    │   ├── remote/        — Retrofit API, request/response DTOs (@Serializable), usage models
    │   ├── local/         — Room: AppDatabase, ChatDao, MessageDao, entities
    │   └── repository/    — ChatRepository implementation, mappers
    │
    ├── domain/
    │   ├── model/         — domain models: Chat, Message
    │   ├── repository/    — ChatRepository interface
    │   └── agent/         — Agent (encapsulates request→response, depends on repository interface)
    │
    ├── di/                — Hilt modules (Network, Database, Repository)
    │
    └── ui/
        ├── chat/          — ChatScreen, ChatViewModel
        ├── chats/         — chat list / drawer
        └── theme/         — minimal theme stub (already scaffolded)

Dependency direction: `ui → domain` and `data → domain`. `domain` depends on nothing.

## Build order
Implement incrementally; the project MUST compile at every step:
1. **domain** — pure Kotlin models + repository interface + Agent
2. **data** — Room entities/DAOs + Retrofit API + ChatRepository implementation + mappers
3. **di** — Hilt modules wiring data implementations to domain interfaces
4. **ui** — ViewModels + Compose screens

Add each layer's dependencies to `libs.versions.toml` only when that layer is being built.

## LLM API details
- Provider: **Anthropic**. Endpoint: `https://api.anthropic.com/v1/messages`.
- Headers: `x-api-key: <key>`, `anthropic-version: 2023-06-01`, `content-type: application/json`.
- Default model: `claude-haiku-4-5-20251001`.
- Request body: `model`, `max_tokens`, `messages` (array of `{role, content}`). The response carries assistant text in `content[0].text` and token usage in `usage` (`input_tokens`, `output_tokens`; some models add `output_tokens_details.thinking_tokens`).
- "Memory" works by resending the full message history on each request — the model itself is stateless. History is stored locally and re-sent each call.

## API key — STRICT security rules
- The Anthropic API key is read from **`local.properties` → `BuildConfig`** only (key name: `ANTHROPIC_API_KEY`).
- `local.properties` is in `.gitignore` — keep it that way.
- **NEVER hardcode the key** in source, NEVER write it to a committed file, NEVER print it in logs.

## Conventions
- Idiomatic Kotlin; explicit and readable over clever.
- Resist over-engineering: this is a learning project. Keep abstractions proportional to need; no speculative generality beyond what days 6–8 require.
- Keep the UI deliberately minimal.
- Be concise in explanations: prioritize showing the plan and code over long prose.
