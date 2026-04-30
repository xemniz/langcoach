# Proactive Coach — Design Spec

> Feature: make the tutor behave like a real language teacher who remembers you,
> brings up things you care about, and adjusts difficulty based on how you're
> actually doing — not just what level you picked in settings.

## 1. Problem

Today, every session starts cold. The system prompt includes due vocab, weak
error categories, and the last three session summaries, but the tutor has no
sense of *who the user is* beyond their CEFR level. It doesn't know the user
likes cooking, that they struggled with subjunctive last week but nailed it
yesterday, or that they mentioned a trip to Barcelona they're excited about.

A good human tutor would remember all of this and use it. This spec adds the
memory and orchestration to make that happen.


## 2. Goals

1. **Topic memory with engagement scoring.** Track what topics the user engages
   with (cooking, travel, work, football) and how much they light up about each.
2. **Personal facts.** Remember concrete things: user's name, job, pet's name,
   upcoming trip, favorite band. Use them for callbacks.
3. **Proactive conversation starters.** Instead of a generic greeting, the tutor
   opens with something relevant: "Last time you mentioned your trip to
   Barcelona — have you started packing?"
4. **Difficulty ratcheting.** Per-topic and per-grammar-area difficulty that
   moves independently. User might be C1 on food vocabulary but B1 on
   subjunctive mood.
5. **Unfinished threads.** If a conversation got cut short or a topic was left
   hanging, remember it and offer to pick it up.
6. **Engagement signals.** Detect enthusiasm vs. disengagement from the
   transcript and adjust future sessions accordingly.


## 3. Non-goals

- Real-time mid-conversation adaptation (the Realtime API manages its own
  context; we influence it via the system prompt, not by injecting messages
  mid-turn).
- Mood detection or emotional analysis beyond simple engagement signals.
- Social features, sharing, or leaderboards.
- Server-side storage. Everything stays on-device per the existing architecture.


## 4. Architecture overview

The proactive coach is a **pre-session orchestration layer** that enriches the
system prompt, plus a **post-session extraction step** that feeds the new memory
tables. It does not touch the Realtime hot path.

```
┌─────────────────────────────────────────────────┐
│                  Session lifecycle               │
│                                                  │
│  ┌──────────┐    ┌──────────────┐    ┌────────┐ │
│  │ Pre-     │───>│  Realtime    │───>│ Post-  │ │
│  │ session  │    │  session     │    │ session│ │
│  │ (coach)  │    │  (unchanged) │    │ (refl.)│ │
│  └──────────┘    └──────────────┘    └────────┘ │
│       │                                    │     │
│       │ reads                        writes│     │
│       v                                    v     │
│  ┌──────────────────────────────────────────┐   │
│  │          Memory layer (Room 3)            │   │
│  │  existing: Vocab, Errors, Sessions        │   │
│  │  NEW: Topics, PersonalFacts, Threads,     │   │
│  │       EngagementSignals, GranularLevel    │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### Module placement

Following the existing convention (one feature = one package, not one module):

| What | Where | Why |
|------|-------|-----|
| New Room entities + DAOs | `shared/data/db/` | Alongside existing entities |
| New repos | `shared/data/repo/` | Alongside VocabRepo, SessionRepo |
| Engagement extraction | `shared/domain/reflection/` | Extension of ReflectionService |
| Proactive prompt builder | `shared/domain/session/` | Alongside SessionOrchestrator |
| Use cases | `shared/domain/usecase/` | Alongside existing use cases |
| LLM extraction prompts | `shared/llm/reflection/` | Alongside ReflectionServiceImpl |

No new modules. No new libraries.


## 5. Data model

### 5.1 TopicRecord

Tracks a topic the user has discussed, with cumulative engagement data.

```kotlin
@Entity(tableName = "topic_records")
data class TopicRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,              // "cooking", "travel", "work"
    val slug: String,              // normalized key for dedup: "cooking"
    val sessionCount: Int = 1,     // how many sessions mentioned this
    val totalEngagement: Double = 0.0,  // sum of per-session engagement scores
    val avgEngagement: Double = 0.0,    // totalEngagement / sessionCount
    val lastSeenAt: Long,          // epoch millis — last session that mentioned it
    val firstSeenAt: Long,         // epoch millis — discovery session
    val difficultyLevel: Int = 0,  // 0–5 scale, see §7 for ratcheting logic
)
```

**Slug normalization**: lowercase, trim, collapse whitespace. "Cooking at home"
and "cooking" merge under slug `"cooking"`. The reflection LLM is instructed to
return canonical single-word or two-word topic names.

### 5.2 PersonalFact

Concrete facts about the user, extracted from conversation.

```kotlin
@Entity(tableName = "personal_facts")
data class PersonalFact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,       // "name", "job", "pet", "travel", "family", "hobby", "other"
    val fact: String,           // "Has a cat named Misha"
    val sourceSessionId: Long,  // which session this was learned in
    val confidence: Double = 1.0, // 0–1, for facts the LLM is less sure about
    val createdAt: Long,
    val supersededBy: Long? = null, // if a newer fact replaces this one
)
```

Facts can be superseded: if the user says "I got a new job" the old job fact
gets `supersededBy` pointed to the new one. The prompt builder only uses
non-superseded facts.

### 5.3 ConversationThread

Tracks topics or questions that were left unfinished.

```kotlin
@Entity(tableName = "conversation_threads")
data class ConversationThread(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,             // short description: "trip to Barcelona planning"
    val lastContext: String,       // what was said: "User was about to describe..."
    val sessionId: Long,           // session it originated in
    val status: String = "open",   // "open", "resumed", "closed"
    val createdAt: Long,
    val resumedAt: Long? = null,
)
```

### 5.4 SessionEngagement

Per-session engagement metrics, computed during reflection.

```kotlin
@Entity(
    tableName = "session_engagements",
    foreignKeys = [ForeignKey(
        entity = SessionSummary::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId", unique = true)],
)
data class SessionEngagement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val avgUserTurnLength: Double,      // avg words per user turn
    val userTurnCount: Int,             // how many times user spoke
    val followUpRate: Double,           // % of user turns that asked a follow-up or elaborated
    val enthusiasmScore: Double,        // 0–1, LLM-assessed
    val overallEngagement: Double,      // composite: see §6
    val topicSlugs: String,             // comma-separated topic slugs for this session
    val createdAt: Long,
)
```

### 5.5 GranularLevel

Per-area difficulty level, independent of the global ProfileLevel.

```kotlin
@Entity(
    tableName = "granular_levels",
    indices = [Index(value = ["area", "areaType"], unique = true)],
)
data class GranularLevel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val area: String,              // "subjunctive", "food_vocabulary", "ser_vs_estar"
    val areaType: String,          // "grammar" or "topic"
    val currentLevel: Int = 0,     // 0–5 (maps to A1–C2)
    val successStreak: Int = 0,    // consecutive sessions with no/few errors in this area
    val failureCount: Int = 0,     // total error instances in this area
    val lastAssessedAt: Long,
)
```

### 5.6 Database migration

Add the five new entities to `LangCoachDatabase`. This is a schema change
(version 2 → 3). Room 3 handles the migration via `AutoMigration` if the
changes are purely additive (new tables, no column renames). All five tables
are new, so auto-migration should work.

```kotlin
@Database(
    entities = [
        VocabItem::class,
        ErrorCategory::class,
        ErrorInstance::class,
        SessionSummary::class,
        UsageEntry::class,
        // NEW
        TopicRecord::class,
        PersonalFact::class,
        ConversationThread::class,
        SessionEngagement::class,
        GranularLevel::class,
    ],
    version = 3,
    autoMigrations = [AutoMigration(from = 2, to = 3)],
)
```


## 6. Engagement scoring

### 6.1 Signal extraction

During post-session reflection, the existing `ReflectionServiceImpl` is extended
with a fourth structured call that extracts engagement signals from the
transcript.

**Input**: user transcript + assistant transcript (same as existing reflection).

**Output** (new schema `engagement_extraction`):

```kotlin
@Serializable
data class EngagementExtractionValue(
    val topics: List<ExtractedTopic>,
    val personalFacts: List<ExtractedFact>,
    val unfinishedThreads: List<ExtractedThread>,
    val enthusiasmScore: Double,  // 0.0–1.0
    val userTurnCount: Int,
    val avgUserTurnWords: Double,
    val followUpRate: Double,     // 0.0–1.0
)

@Serializable
data class ExtractedTopic(
    val name: String,      // canonical: "cooking", "travel", "work"
    val engagement: Double, // 0.0–1.0 for this topic in this session
)

@Serializable
data class ExtractedFact(
    val category: String,
    val fact: String,
    val confidence: Double,
    val supersedes: String? = null, // previous fact text this replaces, if any
)

@Serializable
data class ExtractedThread(
    val topic: String,
    val lastContext: String,
)
```

### 6.2 Composite engagement score

```
overallEngagement = 0.3 * enthusiasmScore
                  + 0.3 * clamp(avgUserTurnWords / 30.0, 0, 1)
                  + 0.2 * clamp(followUpRate, 0, 1)
                  + 0.2 * clamp(userTurnCount / 15.0, 0, 1)
```

This is computed in Kotlin, not by the LLM. The LLM provides the raw signals;
we do the math.

### 6.3 Topic engagement update

After each session, for each extracted topic:

1. Find or create `TopicRecord` by slug.
2. Increment `sessionCount`.
3. Add topic engagement score to `totalEngagement`.
4. Recompute `avgEngagement`.
5. Update `lastSeenAt`.

Topics with `avgEngagement > 0.6` are "high-interest." Topics below `0.3` are
"low-interest" and won't be proactively brought up.


## 7. Difficulty ratcheting

### 7.1 Philosophy

The global `ProfileLevel` (A1–C2) is the user's self-reported level and stays
as the baseline. Granular levels track *observed* comfort in specific areas
and deviate from the baseline as evidence accumulates.

The system is conservative: it takes multiple sessions of evidence to change
a granular level. One good session doesn't promote; one bad session doesn't
demote.

### 7.2 Level scale

```
0 = below A1 (never seen / always wrong)
1 = A1 equivalent
2 = A2
3 = B1
4 = B2
5 = C1+
```

Initial granular level for any new area = user's global ProfileLevel mapped to
this scale (A1→1, A2→2, etc.).

### 7.3 Ratchet-up logic

After each session, for each grammar area where errors were tracked:

```
if (errors_in_area == 0 for this session) {
    successStreak++
} else {
    successStreak = 0
    failureCount += errors_in_area
}

if (successStreak >= 3 && currentLevel < 5) {
    currentLevel++
    successStreak = 0
}
```

Three consecutive clean sessions in an area → promote one level.

### 7.4 Ratchet-down logic

```
recent_sessions = last 5 sessions
error_rate = errors_in_area_across_recent / total_user_turns_across_recent

if (error_rate > 0.25 && currentLevel > 0) {
    currentLevel--
    failureCount = 0  // reset after demotion
}
```

Sustained high error rate (>25% of turns have this error) across a 5-session
window → demote one level.

### 7.5 Topic difficulty

Topic difficulty works similarly but is based on engagement + vocabulary
success rather than error counts:

```
if (avgEngagement > 0.7 && vocab_recall_rate_for_topic > 0.8) {
    // user is comfortable — ratchet up
    difficultyLevel = min(difficultyLevel + 1, 5)
}
if (avgEngagement < 0.3 || vocab_recall_rate_for_topic < 0.4) {
    // user is struggling or disengaged — ratchet down
    difficultyLevel = max(difficultyLevel - 1, 0)
}
```

Topic difficulty influences the system prompt: higher difficulty → tutor uses
more complex vocabulary, longer sentences, and introduces nuance. Lower
difficulty → simpler phrasing, more scaffolding.


## 8. Proactive conversation starter

### 8.1 Starter selection algorithm

At session start, before building the system prompt, the coach selects a
conversation opener strategy. Priority order:

1. **Unfinished thread** — if there's an open `ConversationThread` from the
   last 3 sessions, offer to resume it. ("Last time we were talking about your
   Barcelona trip — did you end up booking the hotel?")

2. **Callback to high-engagement topic** — if the user has a topic with
   `avgEngagement > 0.7` and it hasn't been discussed in the last 2 sessions,
   bring it back. ("We haven't talked about cooking in a while — have you tried
   any new recipes?")

3. **Personal fact callback** — if there's a time-relevant personal fact (e.g.,
   upcoming trip, exam, event), reference it. ("Your trip to Barcelona is
   coming up — are you feeling ready to navigate in Spanish?")

4. **Due vocab thematic grouping** — if several due vocab items cluster around
   a topic, use that topic as the opener. ("I have some food words for us to
   practice — let's talk about what you cooked this weekend.")

5. **Default** — generic greeting in target language (current behavior).

### 8.2 Implementation: GetConversationStarter use case

```kotlin
class GetConversationStarter(
    private val threadRepo: ConversationThreadRepo,
    private val topicRepo: TopicRepo,
    private val factRepo: PersonalFactRepo,
    private val getDueVocab: GetDueVocab,
) {
    suspend operator fun invoke(): StarterStrategy {
        // 1. Check unfinished threads
        val openThreads = threadRepo.getOpen(limit = 3)
        if (openThreads.isNotEmpty()) {
            return StarterStrategy.ResumeThread(openThreads.first())
        }

        // 2. High-engagement topic callback
        val staleHighEngagement = topicRepo.getHighEngagementNotRecentlyUsed(
            minEngagement = 0.7,
            notUsedSinceSessions = 2,
        )
        if (staleHighEngagement.isNotEmpty()) {
            return StarterStrategy.TopicCallback(staleHighEngagement.first())
        }

        // 3. Personal fact callback
        val timelyFacts = factRepo.getTimely() // has logic for upcoming events
        if (timelyFacts.isNotEmpty()) {
            return StarterStrategy.FactCallback(timelyFacts.first())
        }

        // 4. Vocab-themed opener
        val dueVocab = getDueVocab()
        val topicCluster = clusterVocabByTopic(dueVocab)
        if (topicCluster != null) {
            return StarterStrategy.VocabTheme(topicCluster)
        }

        // 5. Default
        return StarterStrategy.Default
    }
}

sealed interface StarterStrategy {
    data class ResumeThread(val thread: ConversationThread) : StarterStrategy
    data class TopicCallback(val topic: TopicRecord) : StarterStrategy
    data class FactCallback(val fact: PersonalFact) : StarterStrategy
    data class VocabTheme(val cluster: VocabCluster) : StarterStrategy
    data object Default : StarterStrategy
}

data class VocabCluster(val topicHint: String, val words: List<VocabItem>)
```

### 8.3 Vocab clustering

Simple approach — no ML needed. Tag each `VocabItem` with a topic at
extraction time (add an optional `topicSlug: String?` column to `VocabItem`).
During reflection, the LLM already sees the context; ask it to also output a
topic tag per vocab item.

At session start, group due vocab by `topicSlug` and pick the largest cluster.


## 9. Enhanced system prompt

### 9.1 ProactiveSessionOrchestrator

Replaces (or wraps) the existing `SessionOrchestrator`. The existing logic
becomes one section of a richer prompt.

```kotlin
class ProactiveSessionOrchestrator(
    private val profilePrefs: ProfilePrefs,
    private val getDueVocab: GetDueVocab,
    private val getWeakCategories: GetWeakCategories,
    private val getRecentSessions: GetRecentSessions,
    private val getConversationStarter: GetConversationStarter,
    private val factRepo: PersonalFactRepo,
    private val topicRepo: TopicRepo,
    private val granularLevelRepo: GranularLevelRepo,
) {
    suspend fun buildSystemPrompt(): String {
        // ... gather all data ...
        val starter = getConversationStarter()
        val facts = factRepo.getActive(limit = 15)
        val topTopics = topicRepo.getTopByEngagement(limit = 5)
        val granularLevels = granularLevelRepo.getAll()

        return buildString {
            appendCoreIdentity(nativeLang, targetLang, level)
            appendPersonalContext(facts)
            appendTopicPreferences(topTopics)
            appendGranularDifficulty(granularLevels, level)
            appendVocabSection(dueVocab)
            appendErrorSection(weakCategories)
            appendRecentSessions(recentSessions)
            appendConversationStarter(starter)
            appendRules(targetLang)
        }
    }
}
```

### 9.2 Prompt sections

**Core identity** (unchanged from today, plus):

```
You know me personally — use what you know to make our conversation feel
natural and continuous, like talking to a teacher I've been seeing for months.
```

**Personal context**:

```
Things you know about me (reference naturally, don't list them):
- My name is Nikolai
- I work as a software developer
- I have a cat named Misha
- I'm planning a trip to Barcelona in June
```

**Topic preferences**:

```
Topics I enjoy talking about (high engagement — bring these up):
- cooking (difficulty: B1, last discussed 3 sessions ago)
- travel (difficulty: A2, last discussed: last session)
Topics I'm less interested in (low engagement — avoid unless I bring them up):
- sports
```

**Granular difficulty**:

```
My comfort level varies by area. Adjust your language accordingly:
- Subjunctive mood: A2 (use simple cases only, scaffold heavily)
- Food vocabulary: B2 (use varied, specific terms)
- Ser vs estar: B1 (I still mix these up regularly)
- General conversation: B1 (my profile level)
When an area is below my profile level, simplify. When above, challenge me.
```

**Conversation starter instruction**:

```
Start this session by picking up where we left off: last time I was telling
you about my Barcelona hotel search and we didn't finish that thread. Ask me
how it went.
```

Or, for a topic callback:

```
Start by bringing up cooking — we haven't talked about it in a few sessions
and I enjoy it. Ask me if I've tried any new recipes.
```

### 9.3 Prompt budget

The Realtime API has a context limit. The system prompt must stay concise.
Budget:

| Section | Target tokens |
|---------|--------------|
| Core identity + rules | ~150 |
| Personal facts (max 15) | ~100 |
| Topic preferences (max 5+3) | ~80 |
| Granular difficulty (max 8) | ~80 |
| Due vocab (max 10 words) | ~40 |
| Weak errors (max 5) | ~80 |
| Recent sessions (max 3) | ~200 |
| Conversation starter | ~60 |
| **Total** | **~790** |

Well within limits. If it grows, prioritize by recency and engagement score.


## 10. Enhanced reflection

### 10.1 New extraction call

Add a fourth structured LLM call to `ReflectionServiceImpl`:

```kotlin
val engagementResult = chat.structured(
    systemPrompt = """You analyze a language tutoring session transcript to extract:
1. Topics discussed (canonical single/two-word names, with per-topic engagement 0-1)
2. Personal facts about the user (things they revealed about themselves)
3. Unfinished conversation threads (topics that were cut short or left open)
4. Overall enthusiasm (0-1) based on elaboration, questions, expressiveness
5. Basic turn metrics

Be conservative with personal facts — only extract things the user clearly
stated about themselves, not inferences. For topics, use broad canonical names
(e.g. "cooking" not "making pasta carbonara"). For enthusiasm, 0.5 is neutral
conversation; above 0.7 means the user was clearly enjoying themselves; below
0.3 means they seemed disengaged or struggling.""",
    userPrompt = "...",
    schemaName = "engagement_extraction",
    schema = engagementExtractionSchema(),
    deserializer = EngagementExtractionResult.serializer(),
)
```

### 10.2 Post-reflection persistence

New use case: `ProcessEngagement`

```kotlin
class ProcessEngagement(
    private val topicRepo: TopicRepo,
    private val factRepo: PersonalFactRepo,
    private val threadRepo: ConversationThreadRepo,
    private val engagementRepo: SessionEngagementRepo,
    private val granularLevelRepo: GranularLevelRepo,
) {
    suspend operator fun invoke(
        sessionId: Long,
        extraction: EngagementExtractionValue,
        errorsByArea: Map<String, Int>,
        nowMillis: Long,
    ) {
        // 1. Upsert topics
        for (topic in extraction.topics) {
            topicRepo.upsertFromSession(topic.name, topic.engagement, nowMillis)
        }

        // 2. Insert personal facts (with supersession logic)
        for (fact in extraction.personalFacts) {
            if (fact.supersedes != null) {
                factRepo.supersede(fact.supersedes, fact, sessionId, nowMillis)
            } else {
                factRepo.insert(fact, sessionId, nowMillis)
            }
        }

        // 3. Mark old threads as resumed if their topic appeared
        val currentTopicSlugs = extraction.topics.map { it.name.slugify() }
        threadRepo.markResumedIfMatching(currentTopicSlugs, nowMillis)

        // 4. Insert new unfinished threads
        for (thread in extraction.unfinishedThreads) {
            threadRepo.insert(thread, sessionId, nowMillis)
        }

        // 5. Save session engagement record
        engagementRepo.insert(sessionId, extraction, nowMillis)

        // 6. Update granular levels based on errors
        for ((area, errorCount) in errorsByArea) {
            granularLevelRepo.updateAfterSession(area, "grammar", errorCount, nowMillis)
        }

        // 7. Update topic difficulty levels
        for (topic in extraction.topics) {
            granularLevelRepo.updateTopicDifficulty(
                topic.name, topic.engagement, nowMillis
            )
        }
    }
}
```

### 10.3 Integration with existing FinishSession

`ReflectSession` currently calls `ReflectionServiceImpl.reflect()` and then
persists vocab + errors + summary. The new flow:

```
ReflectSession {
    val reflection = reflectionService.reflect(...)        // existing
    val engagement = reflectionService.extractEngagement(...)  // NEW
    processEngagement(sessionId, engagement, ...)          // NEW
    // ... existing vocab/error/summary persistence ...
}
```

The engagement extraction is a separate LLM call that runs concurrently with
(or after) the existing three calls. Cost: ~500–800 tokens per session,
roughly $0.002 at current gpt-4o-mini pricing.


## 11. New repositories

### 11.1 TopicRepo

```kotlin
class TopicRepo(private val dao: TopicDao) {
    suspend fun upsertFromSession(name: String, engagement: Double, nowMillis: Long) {
        val slug = name.slugify()
        val existing = dao.findBySlug(slug)
        if (existing != null) {
            val newCount = existing.sessionCount + 1
            val newTotal = existing.totalEngagement + engagement
            dao.update(existing.copy(
                sessionCount = newCount,
                totalEngagement = newTotal,
                avgEngagement = newTotal / newCount,
                lastSeenAt = nowMillis,
            ))
        } else {
            dao.insert(TopicRecord(
                name = name, slug = slug,
                totalEngagement = engagement, avgEngagement = engagement,
                lastSeenAt = nowMillis, firstSeenAt = nowMillis,
            ))
        }
    }

    suspend fun getTopByEngagement(limit: Int): List<TopicRecord> =
        dao.topByEngagement(limit)

    suspend fun getHighEngagementNotRecentlyUsed(
        minEngagement: Double, notUsedSinceSessions: Int
    ): List<TopicRecord> = dao.highEngagementStale(minEngagement, notUsedSinceSessions)

    suspend fun getLowEngagement(maxEngagement: Double): List<TopicRecord> =
        dao.belowEngagement(maxEngagement)
}
```

### 11.2 PersonalFactRepo

```kotlin
class PersonalFactRepo(private val dao: PersonalFactDao) {
    suspend fun getActive(limit: Int): List<PersonalFact> =
        dao.activeByRecency(limit) // WHERE supersededBy IS NULL ORDER BY createdAt DESC

    suspend fun getTimely(): List<PersonalFact> =
        dao.activeByCategory(listOf("travel", "event"))
        // Additional logic: check if fact text contains dates/timeframes
        // that are upcoming. Simple heuristic, not NLP.

    suspend fun insert(fact: ExtractedFact, sessionId: Long, nowMillis: Long) =
        dao.insert(PersonalFact(
            category = fact.category, fact = fact.fact,
            sourceSessionId = sessionId, confidence = fact.confidence,
            createdAt = nowMillis,
        ))

    suspend fun supersede(oldFactText: String, newFact: ExtractedFact, sessionId: Long, nowMillis: Long) {
        val old = dao.findByText(oldFactText)
        val newId = insert(newFact, sessionId, nowMillis)
        if (old != null) dao.markSuperseded(old.id, newId)
    }
}
```

### 11.3 ConversationThreadRepo

```kotlin
class ConversationThreadRepo(private val dao: ConversationThreadDao) {
    suspend fun getOpen(limit: Int): List<ConversationThread> =
        dao.openThreads(limit) // WHERE status = 'open' ORDER BY createdAt DESC

    suspend fun insert(thread: ExtractedThread, sessionId: Long, nowMillis: Long) =
        dao.insert(ConversationThread(
            topic = thread.topic, lastContext = thread.lastContext,
            sessionId = sessionId, createdAt = nowMillis,
        ))

    suspend fun markResumedIfMatching(slugs: List<String>, nowMillis: Long) {
        // For each open thread, if its topic slug matches any current topic,
        // mark it as "resumed"
        val open = dao.allOpen()
        for (thread in open) {
            if (thread.topic.slugify() in slugs) {
                dao.updateStatus(thread.id, "resumed", nowMillis)
            }
        }
    }
}
```


## 12. Koin wiring

Add to `domainModule`:

```kotlin
factoryOf(::GetConversationStarter)
factoryOf(::ProcessEngagement)
singleOf(::ProactiveSessionOrchestrator)
```

Add to data modules:

```kotlin
singleOf(::TopicRepo)
singleOf(::PersonalFactRepo)
singleOf(::ConversationThreadRepo)
singleOf(::SessionEngagementRepo)
singleOf(::GranularLevelRepo)
```

The existing `SessionOrchestrator` binding is replaced by
`ProactiveSessionOrchestrator`. Any code that depends on `SessionOrchestrator`
needs updating — currently that's just the call screen ViewModel (not yet
implemented).


## 13. Cost impact

| Call | Model | Est. tokens | Est. cost |
|------|-------|------------|-----------|
| Engagement extraction | gpt-4o-mini | ~800 in + ~400 out | ~$0.002 |
| Prompt size increase | realtime | ~400 extra in system | negligible (baked into session) |

Total incremental cost per session: roughly $0.002. Acceptable.

All new LLM calls go through the existing `ChatCompletionsClient` and are
logged to `UsageLedger` via the existing cost tracking path.


## 14. Privacy considerations

All new data stays on-device in Room 3. Personal facts are particularly
sensitive — they contain real information about the user. Considerations:

1. **Export/delete**: the settings screen should let the user view and delete
   personal facts, topics, and threads. Standard Room DAO `deleteAll()` methods.
2. **Transparency**: show the user what the coach knows about them. A "Coach
   Memory" screen listing facts, top topics, and granular levels.
3. **No analytics**: per existing policy, none of this data leaves the device.
4. **Fact confidence threshold**: only persist facts with confidence ≥ 0.6.
   Below that, the LLM wasn't sure, so don't store potentially wrong info.


## 15. UI touchpoints

This spec focuses on the engine, not UI, but the feature implies a few screens:

1. **Coach Memory screen** — shows personal facts, topic interests with
   engagement bars, granular level overrides, and open threads. User can edit
   or delete any item.
2. **Session start toast/card** — briefly shows what the coach is planning
   ("Picking up: Barcelona trip" or "Topic: cooking") so the user isn't
   surprised.
3. **Post-session summary enhancement** — the existing summary card adds
   engagement score and any new facts learned.

These are separate implementation tasks and don't block the engine work.


## 16. Testing strategy

Since the project currently has no tests (per CLAUDE.md — tests start week 3),
this section describes what to test once the test infrastructure is in place:

1. **Engagement scoring math**: unit test the composite score formula with
   known inputs.
2. **Ratchet logic**: unit test promotion (3 clean sessions → level up) and
   demotion (>25% error rate over 5 sessions → level down) with edge cases.
3. **Topic dedup**: test that slug normalization correctly merges "Cooking,"
   "cooking," and "Cooking at home."
4. **Starter selection**: test priority ordering — thread > callback > fact >
   vocab > default.
5. **Fact supersession**: test that updating a fact marks the old one and the
   prompt only uses the new one.
6. **Prompt budget**: integration test that the full proactive prompt stays
   under 1000 tokens with maxed-out data.


## 17. Implementation order

Suggested phasing (each is a committable milestone):

### Phase 1: Data layer
- Add five new entities + DAOs
- Room migration (version 2 → 3)
- New repos with basic CRUD
- Koin wiring

### Phase 2: Engagement extraction
- Add `engagement_extraction` schema to `shared/llm/chat/Schemas.kt`
- Add fourth LLM call to `ReflectionServiceImpl`
- Implement `ProcessEngagement` use case
- Wire into `ReflectSession`

### Phase 3: Proactive prompt
- Implement `GetConversationStarter`
- Implement `ProactiveSessionOrchestrator`
- Replace existing orchestrator binding
- Test with manual sessions

### Phase 4: Difficulty ratcheting
- Implement ratchet-up/down logic in `GranularLevelRepo`
- Wire into `ProcessEngagement`
- Add granular difficulty section to system prompt

### Phase 5: Coach Memory UI
- Memory screen (view/edit/delete facts, topics, levels)
- Session start card
- Enhanced post-session summary


## 18. Open questions

1. **Topic taxonomy**: should we predefine a set of canonical topics, or let
   the LLM generate them freely with slug-based dedup? Free generation is
   simpler but risks fragmentation. Recommendation: free generation with slug
   normalization, plus a periodic "topic merge" pass (could be a Koog agent
   job that runs every N sessions).

2. **Fact staleness**: should personal facts expire? A fact learned 6 months
   ago might be outdated. Options: (a) don't expire, rely on supersession;
   (b) add a `staleAfterDays` per category; (c) periodically ask the user to
   confirm facts in the Coach Memory screen. Recommendation: start with (a),
   add (c) if users report stale facts.

3. **Multi-language**: if the user studies two languages, should topics and
   personal facts be shared across languages? Topics probably yes (the user
   likes cooking regardless of language). Granular grammar levels are obviously
   per-language. Personal facts are language-independent. Recommendation: add a
   `targetLang: String?` column to `GranularLevel` (nullable = shared), and no
   language column on `TopicRecord` or `PersonalFact`.

4. **Cold start**: the first few sessions will have no engagement data. The
   coach should fall back gracefully to current behavior (generic greeting,
   global level) and build up memory organically. No onboarding quiz needed —
   the LLM will naturally discover topics through conversation.
