# Ihya API — contract

**Status:** draft · **Last updated:** 2026-09-16
**Canonical.** Both repos build to this document.
`ihya-mobile` integrates against it (`docs/before-backend.md` is its planning
view); `ihya-api` implements it. When they disagree, this file wins — update it
first, in a PR of its own.

Derived from `ihya-mobile/docs/before-backend.md` §3, `docs/content-model.md`,
`docs/seed-data-spec.md`, the locked product decisions (2026-09-10), and the
identity module already shipped here.

---

## 0. Conventions

| Thing | Rule |
|---|---|
| **Base path** | `/v1`. Every path below is relative to it (`/v1/auth/login`, `/v1/me`, …), applied via `WebConfig` to every `@RestController` route. |
| **Auth** | `Authorization: Bearer <accessToken>` on everything except `/auth/*` and `/actuator/health`. Missing/invalid/expired → `401` with the standard error body. |
| **Access token** | Signed JWT (HS384), 15 min. `sub` = user id. |
| **Refresh token** | Opaque 64-byte URL-safe string, 30 days, SHA-256-hashed at rest, **rotated** on every `/auth/refresh`, reuse of a revoked token revokes the whole family. Carried in request/response bodies (Android-only for now; no cookie flow). |
| **IDs** | Opaque UUID strings. Clients never parse meaning out of them. The catalogue additionally exposes a stable human `slug`. |
| **Timestamps** | ISO-8601 UTC, e.g. `2026-09-10T18:50:26.301Z`. |
| **Category reference** | Client-facing payloads use `categorySlug` (e.g. `faith-worship`), **never** the category UUID. |
| **Content type** | `application/json` request and response. |
| **Pagination** | Cursor-based for feeds (`/practices`, `/notifications`): `?cursor=<opaque>&limit=<n>`, response carries `nextCursor` (null at end). The catalogue is returned as a full list (small, long-cached). |
| **Errors** | One shape everywhere (already implemented — `ErrorResponse` / `GlobalExceptionHandler` / `RestAuthenticationEntryPoint`): |

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "A user with that email address already exists",
  "timestamp": "2026-09-10T18:50:26.301Z"
}
```

Bean-validation failures return `400` with `message` = the field messages joined
by `"; "`.

---

## 1. Data types (client-facing shapes)

### Me
```
Me {
  id: uuid
  email: string
  name: string | null
  timezone: string          // IANA, e.g. "Asia/Karachi". Defaults to "UTC" until set.
  interests: string[]       // categorySlug[], active categories only. [] = no preference.
  personalizePromptDismissed: boolean
  createdAt: datetime
}
```

### Progress
```
Progress {
  streak: number
  longestStreak: number
  totalPracticed: number
  earnedMilestoneKeys: string[]   // keys mirror ihya-mobile/src/constants/milestones.ts ids;
                                  // only the 5 concrete milestones are evaluated server-side for v1
}
```

### NotificationPreferences
```
NotificationPreferences {
  dailyReminder: boolean     // default true
  streakReminder: boolean    // default true
  weeklySummary: boolean     // default false
  reminderTime: string       // "HH:mm" 24h, user's local time. default "09:00"
}
```

### Category
```
Category {
  slug: string               // stable, unique, e.g. "faith-worship"
  name: string               // "Faith & Worship"
  description: string
  status: "active" | "coming-soon"
  sunnahCount: number        // derived, count of sunnahs in this category
}
```

Active slugs (10): `faith-worship`, `social-manners`, `home-family`,
`food-eating`, `health-cleanliness`, `knowledge-learning`, `work-career`,
`travel-journey`, `nature-creation`, `character-good-deeds`.
Coming-soon (2, no sunnahs): `self-care`, `dua-supplication`.

### Sunnah
```
Sunnah {
  id: uuid                   // opaque
  slug: string               // stable, unique, e.g. "smile-at-people"
  title: string
  categorySlug: string
  source: string             // display citation, e.g. "Sahih al-Bukhari 24"
  description: string        // English translation / explanation
  reflection: string         // "try this today" instruction
  arabicText: string | null  // full tashkeel; only when verified
  prompt: string | null      // short curiosity question
  tags: string[]             // reserved for seasonal targeting; [] for now
}
```

> **Rename from the current entity:** `action` → `reflection`, `reference` →
> `source` (and make it `NOT NULL`). New: `slug`, `arabicText`, `prompt`, `tags`.
> See `ihya-mobile/docs/seed-data-spec.md` for the authoring rules.

### Assignment
```
Assignment {
  sunnah: Sunnah
  replacementAvailable: boolean   // !replacementUsed && !practicedToday
}
```

### Practice
```
Practice {
  id: uuid
  sunnahId: uuid
  practiceDate: string       // "YYYY-MM-DD" in the user's timezone
  feeling: string | null
}
```

### PracticeResult   (POST /practices response)
```
PracticeResult {
  practice: Practice
  streak: number             // authoritative, strict-reset
  longestStreak: number
  totalPracticed: number
  milestoneUnlocked: string | null   // milestone key just earned, else null
}
```

### Notification
```
Notification {
  id: uuid
  type: "streak_reminder" | "milestone_earned" | "daily" | "weekly_summary" | string
  title: string
  body: string
  createdAt: datetime        // real timestamp; client formats "2h ago"
  read: boolean
}
```

---

## 2. Endpoints

Status legend: **✅ built** · **🟡 partial** · **⬜ not started**

### Auth

| Method · Path | Req → Res | Codes | Status |
|---|---|---|---|
| `POST /auth/register` | `{ email, password, timezone? }` → `{ accessToken, expiresIn, refreshToken, userId }` | `201` · `400` · `409` | ✅ (add optional `timezone`) |
| `POST /auth/login` | `{ email, password }` → same | `200` · `400` · `401` | ✅ |
| `POST /auth/refresh` | `{ refreshToken }` → same (rotated) | `200` · `400` · `401` | ✅ |
| `POST /auth/logout` | `{ refreshToken }` → *(empty)* | `204` · `400` | ✅ |
| `POST /auth/forgot-password` | `{ email }` → *(empty)* | `202` always (no account-existence leak) | ✅ (token generated + hashed + stored with a 30-min expiry; email delivery is the only stubbed part — logs the raw token server-side until an email provider is chosen) |
| `POST /auth/reset-password` | `{ token, newPassword }` → *(empty)* | `204` · `400` (invalid/expired/used token, or `@Valid` failure) | ✅ (single-use — consuming the token revokes it; also revokes every active refresh token for the user, forcing re-login everywhere) |

`password`: min 8. `email`: format-validated, normalized (trim + lowercase),
unique. Unknown-email and wrong-password return an identical `401` body.

### Me & preferences

| Method · Path | Req → Res | Codes | Status |
|---|---|---|---|
| `GET /me` | → `Me` | `200` · `401` | ✅ (composes `UserService` + `ProfileService` — id, email, name, timezone, interests, personalizePromptDismissed, createdAt) |
| `PATCH /me` | `{ name?, email?, timezone?, interests?, personalizePromptDismissed? }` → `Me` | `200` · `400` · `401` · `409` (email taken) | ✅ |
| `DELETE /me` | → *(empty)* | `202` · `401` | ✅ (hard delete for v1 — see §5) |
| `GET /me/progress` | → `Progress` | `200` · `401` | ✅ |
| `GET /me/notification-preferences` | → `NotificationPreferences` | `200` · `401` | ✅ |
| `PATCH /me/notification-preferences` | partial `NotificationPreferences` → full | `200` · `400` · `401` | ✅ |
| `POST /me/push-tokens` | `{ expoPushToken, platform: "ios" \| "android" }` → *(empty)* | `204` · `400` · `401` | ✅ (upsert on `(user_id, expo_push_token)`) |

> **`/me` collision resolved (done):** the standalone profile `GET /me` in
> `openapi/profile-api.yaml` was **superseded** by this composite `GET /me` and
> that path has been retired from the spec. The profile module remains the
> owner of `name` / `interests` / `personalizePromptDismissed`, surfaced
> through the composite.

### Catalogue

| Method · Path | Req → Res | Codes | Status |
|---|---|---|---|
| `GET /categories` | → `Category[]` | `200` · `401` | ✅ |
| `GET /sunnahs` | → `Sunnah[]` | `200` · `401` | ✅ |
| `GET /sunnahs/{slug}` | → `Sunnah` | `200` · `401` · `404` | ✅ |
| `POST /categories` | `{ slug, name, description, status }` → `Category` | `201` · `400` · `401` · `403` · `409` | ✅ **ADMIN only** |
| `PATCH /categories/{slug}` | partial → `Category` | `200` · `400` · `401` · `403` · `404` · `409` | ✅ **ADMIN only** |
| `POST /sunnahs` | `Sunnah` sans `id` → `Sunnah` | `201` · `400` · `401` · `403` · `409` | ✅ **ADMIN only** |
| `PATCH /sunnahs/{slug}` | partial → `Sunnah` | `200` · `400` · `401` · `403` · `404` · `409` | ✅ **ADMIN only** |
| `DELETE /sunnahs/{slug}` | → *(empty)* | `204` · `401` · `403` · `404` | ✅ **ADMIN only** |

Writes require `role = ADMIN`, enforced via `@PreAuthorize("hasRole('ADMIN')")`
+ `@EnableMethodSecurity` — `JwtAuthenticationFilter` resolves the caller's
`Role` fresh from the database on every request rather than embedding it in
the JWT, so a role change takes effect immediately rather than after the
15-minute access token expires. Catalogue content was seeded directly against
`ihya-mobile/docs/seed-data-spec.md`'s shape and category reference table
(migration `V10`) — no real spreadsheet export existed yet in `ihya-mobile`,
so this is a one-time authored seed (30 Sunnahs, real cited sources,
`arabicText` left blank pending verification), not an import; a proper
CSV/JSON importer is still open for when the mobile team's spreadsheet is
ready. **Not yet built:** response caching (`ETag` / long `Cache-Control`) on
the catalogue reads — the mobile app fetching once and working offline from
cache still depends on this.

### Daily practice

| Method · Path | Req → Res | Codes | Status |
|---|---|---|---|
| `GET /assignment/today` | → `Assignment` | `200` · `401` | ✅ |
| `POST /assignment/replacement` | `{ reason: string }` → `{ sunnah: Sunnah }` | `200` · `400` · `401` · `409` (already replaced / already practiced today) | ✅ |
| `POST /practices` | `{ sunnahId, feeling? }` → `PracticeResult` | `201` · `400` · `401` · `409` (already practiced today — body carries current state) | ✅ |
| `PATCH /practices/{id}` | `{ feeling }` → `Practice` | `200` · `400` · `401` · `404` | ✅ (feeling only; never re-triggers streak) |
| `GET /practices` | `?cursor=&limit=` → `{ items: (Practice & { sunnah: Sunnah })[], nextCursor }` | `200` · `401` | ✅ |

**Selection logic** (`GET /assignment/today`, `POST /assignment/replacement`)
— **done.** Resolves the user's local date from `timezone`. If a
`daily_assignments` row exists for `(user_id, local_date)`, return it.
Otherwise pick a Sunnah, excluding the last 7 practiced: when `interests` is
non-empty, weighted-random toward those categories (3× the weight of a
non-interest Sunnah); when empty, walk a **curated default order**
(`categories.sort_order` then `sunnahs.created_at`) — deterministic, no
randomness. Persist the row (`daily_assignments_pkey` under `ON CONFLICT DO
NOTHING`, so two simultaneous first-visits-of-the-day for the same user can't
race each other into a 500), return it. `reason` on replacement is stored for
analytics and **never** affects the pick.

**One practice per day** (`POST /practices`) — **done.** `INSERT ... ON
CONFLICT (user_id, practice_date) DO NOTHING`; on conflict return `409` with
the existing practice + current progress, not a 500 — that `409` body
deliberately isn't the shared `ErrorResponse` shape, since it's expected,
routine state, not an error. `practice_date` is a `date` set server-side from
the user's tz. (A first implementation tried to catch the unique-constraint
violation and re-query for the existing row in the same transaction — Postgres
refuses further commands on a transaction after one statement in it has
thrown, and Spring separately marks the whole transaction rollback-only the
instant that exception is thrown, so even isolating the retry in a fresh
transaction still failed at commit. `ON CONFLICT DO NOTHING` avoids the
problem: the conflict is an ordinary return value, not an exception.)

**Streak** (locked decision) — **done.** Strict reset to 0 after a missed
local day. Denormalized on a new `user_progress` row per user (`streak`,
`longest_streak`, `total_practiced`, `last_practice_date`) rather than
recomputed from history on every read, updated in the same transaction as the
practice insert. `longestStreak = max(...)`. Evaluates the 5 concrete
milestones (`streak` 3/7/30, `total` 25/100) and returns `milestoneUnlocked`
(key) when one is newly earned; the 6 `special` milestones are not evaluated
server-side for v1. **The 5 milestone key strings (`streak_3` etc.) are
placeholders** — `ihya-mobile/src/constants/milestones.ts` wasn't available
while building this; confirm the exact strings match before this ships.

### Notifications

| Method · Path | Req → Res | Codes | Status |
|---|---|---|---|
| `GET /notifications` | `?cursor=&limit=` → `{ items: Notification[], nextCursor }` | `200` · `401` | ⬜ |
| `POST /notifications/read` | `{ ids?: uuid[] }` (omit = mark all) → *(empty)* | `204` · `401` | ⬜ |

Push delivery is server → Expo Push service, out of band. Push tokens register
via `POST /me/push-tokens`.

---

## 3. Reconciliation — what changes in `ihya-api`

Ordered as suggested migrations / PRs. Each lands green through CI, `dev` stays
shippable.

1. **`/v1` base path.** Add the prefix to every route (e.g.
   `server.servlet.context-path: /v1`, or a `WebMvcConfigurer` path prefix).
   Update the two OpenAPI files and the identity integration tests. Mobile sets
   `EXPO_PUBLIC_API_URL` to `https://<host>/v1`.
2. **Extend `/me` (V7 + code).** `profiles`: keep `name`, add
   `personalize_prompt_dismissed boolean NOT NULL DEFAULT false`. `users`: add
   `timezone varchar(64) NOT NULL DEFAULT 'UTC'`. New table
   `user_interests (user_id uuid REFERENCES users, category_slug varchar,
   PRIMARY KEY (user_id, category_slug))`. Compose `Me` from `UserService` +
   `ProfileService`. Retire `profile-api.yaml`'s `/me`. Add `PATCH /me`,
   `DELETE /me`.
3. **Catalogue schema alignment (V9) — done.** `categories`: added
   `slug varchar UNIQUE NOT NULL`, `status varchar NOT NULL DEFAULT 'active'
   CHECK (status IN ('active','coming-soon'))`, `sort_order int NOT NULL
   DEFAULT 0`. `sunnahs`: added `slug varchar UNIQUE NOT NULL`; renamed
   `action → reflection`, `reference → source` (`source` is now `NOT NULL`);
   added `arabic_text text`, `prompt text`, `tags text[] NOT NULL DEFAULT '{}'`.
   `Category` / `Sunnah` entities and services updated; the old paginated
   `search` query was removed rather than updated, since `GET /sunnahs`
   returns the full catalogue as one list, not a filtered page.
4. **Catalogue controllers + method security — done.** `CategoryController`,
   `SunnahController`. Reads = any authenticated user; writes =
   `@PreAuthorize("hasRole('ADMIN')")`, `@EnableMethodSecurity` enabled on
   `SecurityConfig`, `Role` mapped into a `GrantedAuthority` in
   `JwtAuthenticationFilter` (looked up fresh per request rather than embedded
   in the JWT, so a role change takes effect immediately). A failed check
   renders the same `ErrorResponse` 403 shape as every other error
   (`AuthorizationDeniedException` handled in `GlobalExceptionHandler`, since
   Spring MVC's own exception resolution catches it before it can reach the
   security filter chain's `AccessDeniedHandler`).
5. **Seed the catalogue (V10) — done.** No real spreadsheet export existed yet
   in `ihya-mobile`, so V10 authors 30 real, individually cited Sunnahs
   directly (3 per active category) against `seed-data-spec.md`'s shape,
   rather than importing one. `arabicText` is left blank throughout — the spec
   requires a second source to verify it first, and that review hasn't
   happened. A proper CSV/JSON importer is still open for whenever the real
   spreadsheet lands.
6. **Auth gaps.** `POST /auth/logout`, `POST /auth/forgot-password` +
   `POST /auth/reset-password` — **done** (V8 `password_reset_tokens`).
7. **Notification preferences + push tokens (V11) — done.**
   `notification_preferences (user_id PK, daily_reminder, streak_reminder,
   weekly_summary, reminder_time time)`, `push_tokens (id, user_id,
   expo_push_token, platform, created_at, UNIQUE(user_id, expo_push_token))`.
   New `com.ihya.api.notification` module: `NotificationPreferencesService`
   creates a default-valued preferences row for every user in the same
   transaction as `UserService.register` (mirrors how `ProfileService`
   guarantees a profile row exists — no lazy-create path, `GET` never 404s);
   `PushTokenService.registerToken` upserts on `(user_id, expo_push_token)` by
   attempting an insert and remapping the unique-constraint violation to a
   silent no-op rather than an exception, since re-registering a device token
   is expected, not an error (no `409` in this endpoint's contract).
   `UserService.deleteMe` now also deletes `push_tokens` and
   `notification_preferences` rows before the user row. The three
   `GET/PATCH /me/notification-preferences` + `POST /me/push-tokens` endpoints
   are built; `notification-api.yaml` documents them. **Storage only** — no
   scheduler and no Expo push call yet; see §5.
8. **Daily practice module (V12 + V13) — done.**
   `daily_assignments (user_id, assignment_date date, sunnah_id, replacement_used
   boolean NOT NULL DEFAULT false, replacement_reason text, created_at,
   PRIMARY KEY (user_id, assignment_date))`;
   `practices (id, user_id, sunnah_id, practice_date date, feeling text,
   created_at, UNIQUE (user_id, practice_date))`;
   index `practices (user_id, practice_date DESC)` — all `V12`. A second
   migration, `user_progress (user_id PK, streak, longest_streak,
   total_practiced, last_practice_date)` (`V13`), was added mid-phase once
   denormalized streak storage was chosen over recomputing from practice
   history on every read — not in the original plan, discovered while
   designing the streak requirement. New `com.ihya.api.dailypractice` module:
   `AssignmentService`, `PracticeService`, `UserProgressService` and their
   controllers. `AssignmentService`/`PracticeService` read the caller's
   timezone off `UserRepository` directly rather than through `UserService`,
   the one deliberate exception to "go through the owning module's service" —
   `UserService.deleteMe` depends on these services for cleanup, so the
   reverse dependency would be a circular Spring bean graph. All five
   endpoints, selection logic, streak logic, `GET /me/progress` are built;
   full test suite (entity, unit, and a full-stack integration test against
   real Postgres) green.
9. **Notifications feed (V14, shifted from V13 once step 8 needed
   `user_progress` as an unplanned addition).**
   `notifications (id, user_id, type, title, body, created_at, read_at)`,
   index `(user_id, created_at DESC)`. The two endpoints.

---

## 4. Build status at a glance

| Area | State | Next |
|---|---|---|
| Identity (register / login / refresh / me / delete) | ✅ shipped | — |
| Auth logout / forgot-password / reset-password | ✅ shipped | — |
| Profile (name / interests / personalizePromptDismissed, via composite `Me`) | ✅ shipped | — |
| Notification preferences / push tokens | ✅ shipped (storage/API only) | scheduler + Expo push deferred, see §5 |
| Catalogue | ✅ shipped | response caching (`ETag`/`Cache-Control`) still open |
| Daily practice | ✅ shipped | — |
| Notifications | ⬜ | step 9 |
| Milestones | ✅ shipped (5 concrete milestones, server-evaluated) | key strings are placeholders — confirm against `ihya-mobile/src/constants/milestones.ts` |

---

## 5. Open decisions

- **Auth model:** hand-rolled JWT in this service (already built) — **resolved**,
  not Cognito.
- **Hosting:** AWS. RDS Postgres. Compute shape (App Runner / ECS Fargate /
  Elastic Beanstalk vs Lambda + API Gateway) — **open**.
- **`forgot-password` email provider** (SES, Postmark, …) — **open**; the
  reset-token lifecycle (generate/hash/expire/consume) is built, only the
  actual email send is stubbed (logged server-side) until a provider is chosen.
- **`DELETE /me`** hard delete vs soft-delete + async purge — **resolved for
  v1**: hard delete, built. Revisit soft-delete + async purge if/when this
  needs real user-facing recoverability.
- **Per-user timezone capture:** client sends IANA `timezone` on register and
  syncs it via `PATCH /me`; server default `UTC` — **confirm the mobile side
  sends `Intl.DateTimeFormat().resolvedOptions().timeZone`**.
- **`interests` storage:** `user_interests` join table (chosen here) vs a
  `text[]` column on `profiles` — join table for FK integrity; revisit if it
  adds friction.
- **Reminder delivery:** `notification_preferences` and `push_tokens` storage
  and their three endpoints are built (step 7) — **deferred (decided
  2026-09-15):** the scheduled job that checks each user's preferences +
  timezone against their practice history and actually calls Expo's push API
  is real, separate scope (a background job, not just an endpoint) and is
  intentionally pushed to a future stretch phase, not cut.
- **Milestone key strings:** `streak_3` / `streak_7` / `streak_30` / `total_25`
  / `total_100` (step 8, `MilestoneEvaluator`) are self-describing
  placeholders, not confirmed against `ihya-mobile/src/constants/milestones.ts`
  — that file wasn't available while building step 8. **Open**: verify the
  exact strings before this ships.
