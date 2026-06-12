# LLM-Generated Chat Titles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After the first assistant response in a new chat, automatically generate a concise 4–6 word title using the LLM and persist it via Room.

**Architecture:** `Agent.generateTitle()` builds a single-message prompt and calls `llmClient.complete()`. `ChatViewModel.sendMessage()` detects the first exchange (messages were empty before send) and fires a background coroutine — fire-and-forget, silent on failure — that calls `generateTitle` then `repository.updateChatTitle`. The Room flow (`observeChats`) propagates the new title to the UI automatically.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, MockK, Turbine, Robolectric

---

## File Map

| File | Change |
|---|---|
| `domain/agent/Agent.kt` | Add `generateTitle(userMessage, assistantMessage): String` |
| `domain/repository/ChatRepository.kt` | Add `updateChatTitle(chatId, title)` to interface |
| `data/local/dao/ChatDao.kt` | Add `@Query UPDATE` for title |
| `data/repository/ChatRepositoryImpl.kt` | Implement `updateChatTitle` |
| `ui/chat/ChatViewModel.kt` | Detect first exchange; launch title-gen coroutine |
| `testing/FakeChatRepository.kt` | Implement `updateChatTitle` + add `seedChat` helper |
| `data/local/dao/ChatDaoTest.kt` | New test for `updateTitle` |
| `domain/agent/AgentTest.kt` | Two new tests for `generateTitle` |
| `ui/chat/ChatViewModelTest.kt` | Update setUp + three new title-gen tests |

---

## Task 1: Add `updateChatTitle` to DAO and repository

**Files:**
- Modify: `app/src/main/java/karpiuk/ivan/miniagent/data/local/dao/ChatDao.kt`
- Modify: `app/src/main/java/karpiuk/ivan/miniagent/domain/repository/ChatRepository.kt`
- Modify: `app/src/main/java/karpiuk/ivan/miniagent/data/repository/ChatRepositoryImpl.kt`
- Modify: `app/src/test/java/karpiuk/ivan/miniagent/testing/FakeChatRepository.kt`
- Test: `app/src/test/java/karpiuk/ivan/miniagent/data/local/dao/ChatDaoTest.kt`

- [ ] **Step 1: Write the failing DAO test**

Add to `ChatDaoTest.kt` (inside the existing class body):

```kotlin
@Test
fun `updateTitle changes title for existing chat`() = runTest {
    dao.insert(ChatEntity(id = "c1", title = "New chat", createdAt = 1_000L))

    dao.updateTitle("c1", "Kotlin coroutines explained")

    val result = dao.observeChats().first()
    assertEquals("Kotlin coroutines explained", result.first().title)
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew test --tests "karpiuk.ivan.miniagent.data.local.dao.ChatDaoTest.updateTitle changes title for existing chat"
```

Expected: compilation failure — `updateTitle` does not exist yet.

- [ ] **Step 3: Add `updateTitle` to `ChatDao`**

```kotlin
@Query("UPDATE chats SET title = :title WHERE id = :chatId")
suspend fun updateTitle(chatId: String, title: String)
```

- [ ] **Step 4: Add `updateChatTitle` to `ChatRepository` interface**

```kotlin
suspend fun updateChatTitle(chatId: String, title: String)
```

- [ ] **Step 5: Implement in `ChatRepositoryImpl`**

```kotlin
override suspend fun updateChatTitle(chatId: String, title: String) {
    chatDao.updateTitle(chatId, title)
}
```

- [ ] **Step 6: Implement in `FakeChatRepository` + add `seedChat` helper**

```kotlin
override suspend fun updateChatTitle(chatId: String, title: String) {
    chats.value = chats.value.map { if (it.id == chatId) it.copy(title = title) else it }
}

fun seedChat(chat: Chat) {
    chats.value = chats.value + chat
}
```

- [ ] **Step 7: Run the DAO test to verify it passes**

```bash
./gradlew test --tests "karpiuk.ivan.miniagent.data.local.dao.ChatDaoTest.updateTitle changes title for existing chat"
```

Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/karpiuk/ivan/miniagent/data/local/dao/ChatDao.kt \
        app/src/main/java/karpiuk/ivan/miniagent/domain/repository/ChatRepository.kt \
        app/src/main/java/karpiuk/ivan/miniagent/data/repository/ChatRepositoryImpl.kt \
        app/src/test/java/karpiuk/ivan/miniagent/testing/FakeChatRepository.kt \
        app/src/test/java/karpiuk/ivan/miniagent/data/local/dao/ChatDaoTest.kt
git commit -m "feat: add updateChatTitle to ChatRepository and ChatDao"
```

---

## Task 2: Add `generateTitle` to `Agent`

**Files:**
- Modify: `app/src/main/java/karpiuk/ivan/miniagent/domain/agent/Agent.kt`
- Test: `app/src/test/java/karpiuk/ivan/miniagent/domain/agent/AgentTest.kt`

- [ ] **Step 1: Write failing tests**

Add to `AgentTest.kt` (inside the existing class body):

```kotlin
@Test
fun `generateTitle calls llmClient with prompt containing user and assistant text`() = runTest {
    llmClient.result = LlmResult(assistantText = "Kotlin coroutines explained", inputTokens = 10, outputTokens = 5)

    agent.generateTitle("How do coroutines work?", "Coroutines are lightweight threads...")

    val sent = llmClient.capturedMessages.last()
    assertEquals(1, sent.size)
    assertEquals(Role.USER, sent[0].role)
    assertTrue(sent[0].content.contains("How do coroutines work?"))
    assertTrue(sent[0].content.contains("Coroutines are lightweight threads..."))
}

@Test
fun `generateTitle returns trimmed llm response`() = runTest {
    llmClient.result = LlmResult(assistantText = "  Kotlin coroutines explained  ", inputTokens = 10, outputTokens = 5)

    val title = agent.generateTitle("How do coroutines work?", "Coroutines are lightweight threads...")

    assertEquals("Kotlin coroutines explained", title)
}
```

- [ ] **Step 2: Run to verify they fail**

```bash
./gradlew test --tests "karpiuk.ivan.miniagent.domain.agent.AgentTest.generateTitle*"
```

Expected: compilation failure — `generateTitle` does not exist yet.

- [ ] **Step 3: Implement `generateTitle` in `Agent`**

```kotlin
suspend fun generateTitle(userMessage: String, assistantMessage: String): String {
    val prompt = Message(
        id = UUID.randomUUID().toString(),
        chatId = "",
        role = Role.USER,
        content = "Generate a concise 4–6 word title for a conversation that started with:\n" +
                "User: $userMessage\n" +
                "Assistant: $assistantMessage\n" +
                "Reply with only the title, no punctuation.",
        timestamp = System.currentTimeMillis(),
    )
    return llmClient.complete(listOf(prompt)).assistantText.trim()
}
```

- [ ] **Step 4: Run all Agent tests to verify they pass**

```bash
./gradlew test --tests "karpiuk.ivan.miniagent.domain.agent.AgentTest"
```

Expected: all PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/karpiuk/ivan/miniagent/domain/agent/Agent.kt \
        app/src/test/java/karpiuk/ivan/miniagent/domain/agent/AgentTest.kt
git commit -m "feat: add generateTitle to Agent"
```

---

## Task 3: Wire title generation in `ChatViewModel`

**Files:**
- Modify: `app/src/main/java/karpiuk/ivan/miniagent/ui/chat/ChatViewModel.kt`
- Test: `app/src/test/java/karpiuk/ivan/miniagent/ui/chat/ChatViewModelTest.kt`

- [ ] **Step 1: Write failing tests**

Update `setUp` to mock `generateTitle` (add after existing `coEvery` mock):

```kotlin
coEvery { agent.generateTitle(any(), any()) } returns "Generated Title"
```

Add the following three tests to `ChatViewModelTest.kt`:

```kotlin
@Test
fun `sendMessage generates title and updates chat after first exchange`() = runTest {
    fakeRepository.seedChat(Chat(id = chatId, title = "New chat", createdAt = 0L))

    viewModel.updateInput("How do coroutines work?")
    viewModel.sendMessage()
    advanceUntilIdle()

    coVerify { agent.generateTitle("How do coroutines work?", "reply") }
    assertEquals("Generated Title", viewModel.uiState.value.chatTitle)
}

@Test
fun `sendMessage does not generate title on subsequent exchanges`() = runTest {
    fakeRepository.seedChat(Chat(id = chatId, title = "Existing title", createdAt = 0L))
    fakeRepository.addMessage(Message(id = "m1", chatId = chatId, role = Role.USER, content = "first", timestamp = 1L))
    fakeRepository.addMessage(Message(id = "m2", chatId = chatId, role = Role.ASSISTANT, content = "answer", timestamp = 2L))

    viewModel.updateInput("follow-up question")
    viewModel.sendMessage()
    advanceUntilIdle()

    coVerify(exactly = 0) { agent.generateTitle(any(), any()) }
}

@Test
fun `sendMessage keeps New chat title and does not surface error when generateTitle fails`() = runTest {
    fakeRepository.seedChat(Chat(id = chatId, title = "New chat", createdAt = 0L))
    coEvery { agent.generateTitle(any(), any()) } throws RuntimeException("API error")

    viewModel.updateInput("Hello")
    viewModel.sendMessage()
    advanceUntilIdle()

    assertEquals("New chat", viewModel.uiState.value.chatTitle)
    assertNull(viewModel.uiState.value.error)
}
```

Also add the `Chat` import if not present: `import karpiuk.ivan.miniagent.domain.model.Chat`

- [ ] **Step 2: Run to verify they fail**

```bash
./gradlew test --tests "karpiuk.ivan.miniagent.ui.chat.ChatViewModelTest.sendMessage generates title*" \
               --tests "karpiuk.ivan.miniagent.ui.chat.ChatViewModelTest.sendMessage does not generate title*" \
               --tests "karpiuk.ivan.miniagent.ui.chat.ChatViewModelTest.sendMessage keeps New chat title*"
```

Expected: FAIL — `generateTitle` not called yet from `sendMessage`.

- [ ] **Step 3: Update `ChatViewModel.sendMessage()`**

Replace the existing `sendMessage` function with:

```kotlin
fun sendMessage() {
    val input = _inputText.value.trim()
    if (input.isBlank() || _isSending.value) return
    val isFirstMessage = uiState.value.messages.isEmpty()
    viewModelScope.launch {
        _isSending.value = true
        _inputText.value = ""
        try {
            val response = agent.sendMessage(chatId, input)
            if (isFirstMessage) {
                launch {
                    try {
                        val title = agent.generateTitle(input, response.replyText)
                        repository.updateChatTitle(chatId, title)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // keep "New chat" on title-generation failure
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error"
        } finally {
            _isSending.value = false
        }
    }
}
```

- [ ] **Step 4: Run all ViewModel tests to verify they pass**

```bash
./gradlew test --tests "karpiuk.ivan.miniagent.ui.chat.ChatViewModelTest"
```

Expected: all PASS

- [ ] **Step 5: Run full test suite to check for regressions**

```bash
./gradlew check
```

Expected: BUILD SUCCESSFUL, all tests green

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/karpiuk/ivan/miniagent/ui/chat/ChatViewModel.kt \
        app/src/test/java/karpiuk/ivan/miniagent/ui/chat/ChatViewModelTest.kt
git commit -m "feat: generate LLM chat title after first assistant response"
```

---

## Verification

1. Install on device/emulator: `./gradlew installDebug`
2. Create a new chat, send a message, wait for the reply.
3. Open the drawer — the chat title should change from "New chat" to a generated 4–6 word title.
4. Send a second message and verify the title does **not** change again.
5. Check the top bar of the chat screen — title should reflect the generated name.
