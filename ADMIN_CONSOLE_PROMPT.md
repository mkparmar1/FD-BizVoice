# BizVoice Admin Console — end-to-end frontend brief

**Status:** backend is built, tested and live-ready. Nothing below is speculative — every
contract was verified by 84 passing feature tests against the running code.
**Audience:** whoever implements the Android side (human or AI coding tool). Paste this
whole file as the task brief.

---

## 1. The idea

Today BizVoice is a single-role app: everyone gets Keypad, Recents, Contacts, Settings.
The company owner has no way to run the business from the phone — user management, number
purchasing, and performance reporting all live in a separate web panel.

**The idea is to put an admin console in the app itself**, visible only to admins, so the
owner can run the whole operation from their phone.

The original requirements, and how each one is answered:

| # | Requirement | How it lands |
|---|---|---|
| 1 | Add a bottom menu for settings | The app already has a Settings tab. Instead we add a **5th tab, "Admin"** |
| 2 | Inside it, a grid of admin menus | `AdminHomeScreen` — a 2-column card grid, 6 cards |
| 3 | Feedback input and answer | `AdminFeedbackScreen` — inbox + reply; user-side submission unchanged |
| 4 | Analytics: who performs well, calls, minutes | `AdminAnalyticsScreen` — org totals + sortable per-user leaderboard |
| 5 | Call history per user | A tab inside `AdminUserDetailScreen`, filterable by date and direction |
| 6 | Only visible for role = admin | Tab hidden client-side **and** every endpoint gated server-side |
| 7 | User active / inactive | A switch on the user row and on the detail screen |
| 8 | Assign and remove numbers | `AdminNumbersScreen` — assign, **unassign** and **release** as distinct actions |
| 9 | Contacts synced to DB, per-user counts | `contacts/sync` + `AdminContactsScreen` |
| 2 | Users list and CRUD, bulk number purchase | `AdminUsersScreen` + bulk purchase flow |

### Decisions already made (do not re-open)

- **Entry point:** a 5th bottom tab, not a section inside Settings.
- **Scope:** `super-admin` sees the whole system; `admin` sees itself plus everyone under
  its `parent_user_id` tree, plus the unassigned number pool.
- **Contacts:** owned per user; an admin can drill into any user's list.
- **Numbers:** *Unassign* (reversible, stays purchased) and *Release* (permanent, deleted
  at Twilio) are two separate actions with two separate endpoints.

---

## 2. Ground rules

1. **Do not change any existing feature.** Dialer, Recents, Contacts, Profile, Settings,
   active/incoming call, Twilio wiring — untouched. Only add.
2. **Same theme.** Reuse `MaterialTheme.colorScheme`, `Type.kt`, `MainTabHeader`,
   `BizAvatar`, `SettingsSectionHeader` and the existing card/row idioms. No new colours,
   no new font, no new design language.
3. **A non-admin must see today's app exactly.** Same 4 tabs, same order, same behaviour.
4. **Client-side hiding is cosmetic.** The server enforces everything; a 403 must degrade
   gracefully, never crash.
5. **Additive Kotlin only.** New DTOs, new Retrofit methods, new screens. The one edit to
   an existing screen is the bottom bar in `MainContainerScreen`.

---

## 3. How it works end to end

### 3.1 Base URL and versioning

```
{base_url}/api/{api_version}/<endpoint>
```

- `api_version = v1.0` → plain JSON. **Use this.**
- `api_version = v2` → the identical payload, AES-encrypted, `text/plain`. `ApiClient`
  already handles the split; nothing new is needed for the admin routes.

### 3.2 Auth

The same JWT the app already uses. `POST admin/login` returns it; `ApiClient`'s
AuthInterceptor already attaches `Authorization: Bearer <jwt>`. **No new auth mechanism,
no second token, no extra header.**

The JWT has no expiry claim — it stays valid until `auth_key` rotates (which `logout`
does). Deactivating a user takes effect on that user's **next request**, not at token
expiry.

### 3.3 Envelope

```json
{ "code": 200, "data": { }, "message": "..." }
```

HTTP status always equals `code`. Model with the existing `GrowfoneCodeEnvelope<T>`.

Paginated `data` is Laravel's paginator — the shape `ContactsPagedDto` already models:

```json
{ "current_page": 1, "data": [ ... ], "last_page": 3, "per_page": 15, "total": 42 }
```

### 3.4 Error shapes — there are two, handle both

| Situation | HTTP | Body |
|---|---|---|
| Missing / invalid / expired JWT (outer middleware) | 400 or 401 | `{"status": false, "data": [], "message": "Auth token can not be empty."}` |
| Logged in, not an admin | 403 | `{"code": 403, "data": {}, "message": "This area is restricted to administrators."}` |
| Out of scope, or does not exist | 404 | `{"code": 404, "data": {}, "message": "User not found"}` |
| Validation failed | 422 | `{"code": 422, "data": {}, "message": "<first error only>"}` |
| Business rule refused | 400 | `{"code": 400, "data": {}, "message": "<human reason>"}` |

Note the first row uses `status`, not `code` — it comes from the older middleware layer.
A parser that assumes `code` will NPE on an expired session.

### 3.5 Role — how the app knows to show the tab

`POST admin/login` and `GET getProfile` both now return a `role` block. **This is the only
source of truth.**

```json
{
  "code": 200,
  "data": {
    "user": { "...": "unchanged, plus a nested full role row" },
    "role": {
      "id": "7a2b3c4d-...",
      "name": "Admin",
      "slug": "admin",
      "description": "Full access to the admin dashboard",
      "is_custom": false
    },
    "token": "eyJ0eXAi..."
  },
  "message": "Login successful"
}
```

- Switch on **`data.role.slug`** — never `name`, which is editable free text.
- Admin slugs: `admin`, `super-admin`.
- A user with no role gets the same object with every string `""` — never null. No
  null-checking the object itself.
- `getProfile` returns the same block at `data.role`, alongside the flat user fields.

### 3.6 Scope, enforced server-side

`super-admin` → everything. `admin` → itself + its `parent_user_id` descendants + the
unassigned number pool. An id outside the scope answers **404**, never another org's data.
The app does not need to filter anything itself.

---

## 4. Screen-by-screen build spec

### 4.1 Bottom navigation

`MainContainerScreen` currently hard-codes four `NavigationBarItem`s. Replace with a list
built from `isAdmin`:

```
member: [Keypad] [Recents] [Contacts] [Settings]
admin:  [Keypad] [Recents] [Contacts] [Admin] [Settings]
```

- Icon: `Icons.Default.AdminPanelSettings`. Label: `Admin`. Test tag: `nav_item_admin`.
- Keep every existing test tag (`main_bottom_nav`, `nav_item_dialer`, …).
- With five items labels truncate on small screens — drop to icon-only below ~360 dp.
- **Existing UI tests assert a 4-item bar. Update them: 4 for a member, 5 for an admin.**

### 4.2 `AdminHomeScreen` — the grid

`LazyVerticalGrid(GridCells.Fixed(2))`, cards in `surfaceVariant`, icon + label + a
one-line count subtitle where cheap.

| Card | Icon | Goes to | Subtitle source |
|---|---|---|---|
| Users | `Group` | `AdminUsersScreen` | `admin/users` → `total` |
| Numbers | `Dialpad` | `AdminNumbersScreen` | `admin/numbers` → `total` |
| Analytics | `Insights` | `AdminAnalyticsScreen` | `analytics/overview` → `totals.total_calls` |
| Feedback | `Feedback` | `AdminFeedbackScreen` | unanswered count (`answered=false`) |
| Contacts | `Contacts` | `AdminContactsScreen` | `contacts/summary` → `total_assigned` |
| Sync numbers | `Sync` | inline action | super-admin only; hide otherwise |

Above the grid, a **balance tile**: `admin/twilio/balance` → `balance_label`, with a
Sync icon that calls `admin/twilio/balance/sync`. Tint it with the error colour when
`is_low` is true, and show "Balance unavailable" on a 503 rather than `0.00`.

### 4.3 `AdminUsersScreen`

- Search field (debounced 300 ms) → `search`; filter chips for `status` and `role_slug`.
- Row: avatar, name, email, role chip, assigned number, an **active/inactive Switch**.
- Switch → `POST admin/users/{id}/status`. Optimistic flip, revert + snackbar on failure.
- FAB → `AdminUserFormScreen` (create). Row overflow → edit, delete.
- Delete → confirm dialog. Explain in the dialog that the user's number is returned to the
  pool, not released.
- Paging: `current_page` / `last_page`. Pull-to-refresh. Skeleton on first load.

### 4.4 `AdminUserDetailScreen`

Four tabs over one header (avatar, name, email, role chip, status switch):

| Tab | Source |
|---|---|
| Profile | `GET admin/users/{id}` |
| Number | `assigned_numbers[]` from the same call + assign/unassign actions |
| Calls | `GET admin/users/{id}/calls` — date + direction filter, paged; show `counterpart_number` / `contact_name`, not the raw from/to |
| Contacts | `GET admin/contacts?user_id={id}` |

The Calls tab shows `data.stats` as a stat row above the list: total calls, minutes,
answer rate, avg duration.

### 4.5 `AdminNumbersScreen`

- Filter chips: All / Assigned / Unassigned; search by number or friendly name.
- Row: number, friendly name, `status_label` badge, assigned user (or "Unassigned").
  Badge the holder when `assigned_user.is_deleted`, and show "Assigned to a removed
  account" with an Unassign action when `is_orphaned`.
- Actions per row: **Assign** (user picker sheet), **Unassign**, **Release**.
  - Assign 400s if that user already holds an active number — show the server message.
  - **Unassign** is the everyday action: no dialog beyond a confirm snackbar.
  - **Release** is permanent: `AlertDialog`, error-coloured button, the number printed in
    the body, and `confirm: true` in the request.
- Multi-select mode → bulk unassign / bulk release (bulk release keeps the same dialog).
- Purchase flow: reuse the **existing** `findPhoneNumber` search the app already has, let
  the admin tick several results, then `POST admin/numbers/purchase`. Render `purchased`
  and `failed` separately afterwards. **Never auto-retry a purchase — it spends money.**
- "Sync from Twilio" button: super-admin only.
- A balance line at the top of this screen too (`admin/twilio/balance`) makes the
  cost of a purchase obvious before it is made.

### 4.6 `AdminAnalyticsScreen`

- Date range selector (default: last 30 days) → `from` / `to`.
- Top: stat tiles from `analytics/overview` → `totals` (calls, minutes, answer rate) and
  `users` (active / inactive).
- Daily trend: a simple bar or line from `daily_trend[]` (`day`, `calls`, `minutes`).
- Leaderboard: `analytics/users`, sortable header (minutes / calls / answer rate), each row
  tappable through to `AdminUserDetailScreen`.

### 4.7 `AdminFeedbackScreen`

- Filter chips: Unanswered / Pending / Resolved; optional priority filter.
- Row: title, submitter, priority badge, status badge, answered tick.
- Tap → detail sheet with the original message and an answer field →
  `POST admin/feedback/{id}/answer`. Sending marks it `resolved` unless another status is
  picked. Update the row in place afterwards.

### 4.8 `AdminContactsScreen`

- List of users with `contacts_count`, tap through to that user's contacts.
- In the drill-down each contact shows calls out vs calls in from its `calls` block
  (`outbound_calls` = the user rang them, `inbound_calls` = they rang the user), with
  `last_call_at` / `last_direction` as the secondary line.
- Show `unassigned_legacy` as a separate "Unassigned (legacy)" row **only when non-null**
  (super-admin only).
- Empty counts are expected until users have run `contacts/sync`. Say so in the empty
  state rather than showing a bare zero.

### 4.9 `contacts/sync` — wire it into the existing Contacts screen

Not admin-only. It is what makes 4.8 meaningful. Call it after contacts permission is
granted and on pull-to-refresh. Chunk at 500 per request. Store the returned `contact_id`
against the local row. `conflicts` are expected — show "N contacts skipped", never an
error dialog.

---

## 5. API reference — Part A: the admin console (new)

All paths relative to `{base_url}/api/v1.0/`. All require the admin JWT.

### 5.1 Users

| Method | Path | Purpose |
|---|---|---|
| GET | `admin/users` | List / search users |
| GET | `admin/users/{id}` | One user + stats + numbers + contact count |
| POST | `admin/users` | Create |
| POST | `admin/users/{id}` | Update (partial) |
| POST | `admin/users/{id}/status` | Activate / deactivate |
| DELETE | `admin/users/{id}` | Soft delete |
| GET | `admin/users/{id}/calls` | Call history for that user |

**`GET admin/users`** — query `search`, `status`, `role_id`, `role_slug`, `per_page`
(max 100), `page`. Paginator of:

```json
{
  "id": "uuid",
  "name": "Bob Member",
  "email": "bob@example.com",
  "phone_number": "9876543210",
  "status": "active",
  "is_active": true,
  "credits": 250,
  "role": { "id": "uuid", "name": "Team Member", "slug": "team-member" },
  "assigned_numbers": [
    { "id": "uuid", "phone_number": "+15550001", "friendly_name": "Bob line",
      "country_code": "US", "status": 1, "status_label": "Active" }
  ],
  "created_at": "2026-01-14T09:21:33+00:00"
}
```

**`GET admin/users/{id}`** — the row above plus:

```json
{
  "stats": {
    "total_calls": 3, "outbound_calls": 2, "inbound_calls": 1,
    "answered_calls": 2, "unanswered_calls": 1,
    "total_seconds": 420, "total_minutes": 7,
    "avg_duration_seconds": 140, "answer_rate": 66.7,
    "last_call_at": "2026-08-20 11:02:17"
  },
  "contacts_count": 1,
  "parent_user_id": "uuid"
}
```

**`POST admin/users`** — `{ name*, email*, password* (min 8), phone_number?, role_id?,
status?, parent_user_id? }`. `parent_user_id` is honoured for super-admin only; an admin
always creates inside its own tree.

**`POST admin/users/{id}`** — any of `name`, `email`, `password`, `phone_number`,
`friendly_name`, `role_id`, `status`. Send only what changed.

**`POST admin/users/{id}/status`** — `{ "status": "active" | "inactive" }`.
400 `You cannot change your own account status.` for self.

**`DELETE admin/users/{id}`** — soft delete; the held number is **unassigned, not
released**. 400 for self.

**`GET admin/users/{id}/calls`** — query `from`, `to` (`YYYY-MM-DD`), `direction`
(`inbound`|`outbound`), `status`, `search`, `per_page`, `page`. Paginator of call rows,
**plus `data.stats`** (same stats block) and **`data.user`**:

```json
"user": { "id": "uuid", "name": "Bob Member", "email": "bob@example.com",
          "status": "active", "role_name": "Team Member",
          "phone_number": "+15550001" }
```

`data.user` is what lets a screen opened from the analytics leaderboard render its header
without a second call. Each row:

```json
{
  "id": "uuid", "direction": "outbound", "status": "completed",
  "from_phone_number": "+15550001", "to_phone_number": "+919999911111",
  "counterpart_number": "+919999911111", "contact_name": "Ravi Kumar",
  "to_country_code": "IN", "duration": 120, "duration_label": "02:00",
  "is_answered": true, "call_sid": "CA...", "created_at": "2026-08-20T11:02:17+00:00"
}
```

**`counterpart_number` is the number on the other end** — already flipped for you.
`team_calls` stores the two legs by role, so the other party is `to_phone_number` on an
outbound call but `from_phone_number` on an inbound one. `contact_name` resolves it
against that user's contacts (`""` when unknown). Show `counterpart_number`, never the raw
from/to pair.

### 5.2 Numbers

| Method | Path | Purpose |
|---|---|---|
| GET | `admin/numbers` | Org numbers + unassigned pool |
| POST | `admin/numbers/purchase` | Buy 1–20 numbers |
| POST | `admin/numbers/{id}/assign` | Give to a user |
| POST | `admin/numbers/{id}/unassign` | Return to pool (reversible) |
| POST | `admin/numbers/{id}/release` | Delete at Twilio (**permanent**) |
| POST | `admin/numbers/bulk-unassign` | Up to 50 |
| POST | `admin/numbers/bulk-release` | Up to 50, **permanent** |
| POST | `admin/numbers/sync` | Reconcile with Twilio (**super-admin only**) |

Number row:

```json
{
  "id": "uuid", "phone_number": "+15550002", "friendly_name": "Spare",
  "country_code": "US", "status": 1, "status_label": "Active",
  "is_assigned": true, "sid": "PN...",
  "activation_date": "2026-08-01T00:00:00+00:00",
  "assigned_on": "2026-08-01T00:00:00+00:00",
  "is_orphaned": false,
  "assigned_user": {
    "id": "uuid", "name": "Cara Member", "email": "cara@example.com",
    "phone_number": "9876543210", "status": "active", "is_active": true,
    "role_name": "Team Member", "role_slug": "team-member", "is_deleted": false
  },
  "created_at": "..."
}
```

`status`: **0 = Inactive, 1 = Active, 9 = Released.** Display `status_label`.

Three fields matter for the "assigned to whom" row:

- **`assigned_user`** is `null` only when the number is genuinely unassigned, or orphaned.
  Otherwise it carries everything needed to name the holder — no second call.
- **`assigned_user.is_deleted: true`** — the account was soft-deleted but the number was
  never handed back. It is still billable and can be unassigned and reused. Deleting a
  user through the **web panel** does not release their number, so these do occur. Render
  the name with a "deleted account" badge.
- **`is_orphaned: true`** — `user_id` points at an account that no longer exists at all,
  so `assigned_user` is `null` while `is_assigned` is `true`. Show "Assigned to a removed
  account" and offer Unassign to reclaim it.

- **purchase** `{ numbers: [...] (1–20), country_code: "US", friendly_name?, assign_to? }`
  → `{ purchased: [row], failed: [{phone_number, reason}] }`. Partial success is still 200.
- **assign** `{ user_id }` → 400 if that user already holds an active number.
- **release** `{ confirm: true }` — required, else 400
  `Releasing a number is permanent. Send confirm=true to proceed.`
- **bulk-unassign** `{ ids: [...] }` → `{ unassigned: [ids], failed: [{id, reason}] }`
- **bulk-release** `{ ids: [...], confirm: true }` → `{ released: [...], failed: [...] }`
- **sync** → `{ synced, marked_released }`; 403 for a plain admin.

### 5.3 Twilio account balance

| Method | Path | Purpose |
|---|---|---|
| GET | `admin/twilio/balance` | Live balance for the console tile (cached 60s) |
| POST | `admin/twilio/balance/sync` | The Sync action — always a live read |

```json
{
  "available": true,
  "cached": false,
  "account_sid": "AC...",
  "friendly_name": "Growfone",
  "account_status": "active",
  "account_type": "Full",
  "balance": 123.45,
  "balance_raw": "123.45678",
  "currency": "USD",
  "balance_label": "USD 123.45",
  "is_low": false,
  "low_balance_threshold": 10,
  "numbers": { "total": 3, "active": 3, "assigned": 2, "unassigned": 1 },
  "fetched_at": "2026-08-26T10:00:00+00:00"
}
```

- **Display `balance_label`** — it is already currency-prefixed and rounded. `balance` is
  a float for comparisons; `balance_raw` is Twilio's own unrounded string.
- `is_low` compares against `TWILIO_LOW_BALANCE_THRESHOLD` (server env, default `10`).
  Use it for the warning colour rather than hard-coding a threshold in the app.
- `account_status` is Twilio's own — `active`, `suspended`, `closed`. A suspended account
  explains why calls are failing, so show it.
- `GET` is cached for 60 seconds (`cached: true` means you got the cache). Pass
  `?refresh=true`, or use the sync route, to force a live read.
- **A 503 means Twilio could not be reached.** Render "Balance unavailable" with a retry.
  The server deliberately never returns a 200 with a zero balance.
- Both routes only read. Neither is related to `admin/numbers/sync`, which rewrites number
  rows and is super-admin only.

The balance belongs to the **whole Twilio account**, not to one org — every admin who
reaches the console can read it.

### 5.3b Twilio provider numbers (the Provider tab)

**`GET admin/twilio/numbers`** — what the Twilio account actually holds, matched against
the local table. Query: `status` (default `all`), `search` (number / friendly name / SID),
`assigned` (bool), `page`, `per_page` (default 50, max 200), `refresh` (bypass the 60s
cache).

```json
{
  "summary": {
    "total": 12, "active": 12, "assigned": 8, "unassigned": 4,
    "synced_with_db": 11, "missing_in_db": 1,
    "hidden_by_scope": 0, "db_active_count": 11
  },
  "data": [
    {
      "sid": "PN1a2b3c...", "phone_number": "+15551234567",
      "friendly_name": "Main Support Line", "status": "in-use", "country_code": "US",
      "capabilities": { "voice": true, "sms": true, "mms": false, "fax": false },
      "date_created": "2026-01-15T10:30:00+00:00",
      "in_db": true, "db_id": "9b1f2c34-...", "db_status": 1, "db_status_label": "Active",
      "is_assigned": true,
      "assigned_user": { "id": "...", "name": "Sarah Connor", "email": "...",
                         "status": "active", "is_deleted": false }
    }
  ],
  "total": 12, "current_page": 1, "last_page": 1, "per_page": 50,
  "cached": false, "fetched_at": "2026-08-26T10:00:00+00:00",
  "can_sync": true
}
```

- **`db_id` is a uuid, not an int** — it is the local `phone_number.id`, and it is what
  assign / unassign / release take. `""` when `in_db` is false.
- **`can_sync`** tells you whether this caller may run the reconciliation. Hide the Sync
  button when it is false instead of letting the call 403.
- `summary.missing_in_db` vs `synced_with_db` is the reconciliation banner: a number Twilio
  holds that the database has never seen was bought in the Twilio console.
- **Scope:** super-admin sees the whole account; a scoped admin sees only numbers whose
  local row it owns plus the unassigned pool — anything else would leak another org's line.
  Those are counted in `hidden_by_scope`, and `missing_in_db` is `null` for a scoped admin.
- **503** when Twilio cannot be reached.

**`POST admin/numbers/sync`** now takes an optional body and reports more:

```json
{ "import_missing": true, "reconcile_released": true }
```

```json
{ "synced": 12, "imported": 1, "updated": 11, "marked_released": 0,
  "twilio_active_count": 12, "db_count": 12 }
```

`synced` = `imported` + `updated`. Running it clears the Provider tab cache. Still
**super-admin only** — it rewrites rows across every org.

### 5.4 Analytics

**`GET admin/analytics/overview`** — query `from`, `to`.

```json
{
  "range": { "from": "", "to": "" },
  "totals": { "total_calls": 4, "outbound_calls": 3, "inbound_calls": 1,
              "answered_calls": 3, "unanswered_calls": 1, "total_seconds": 480,
              "total_minutes": 8, "avg_duration_seconds": 120, "answer_rate": 75 },
  "users": { "total": 3, "active": 3, "inactive": 0 },
  "daily_trend": [ { "day": "2026-08-20", "calls": 2, "minutes": 3.5 } ],
  "top_performers": [ /* up to 5 user-metric rows */ ]
}
```

**`GET admin/analytics/users`** — query `from`, `to`, `sort`, `order`, `per_page`, `page`.
`sort` ∈ `total_calls`, `total_minutes`, `total_seconds`, `answered_calls`,
`unanswered_calls`, `inbound_calls`, `outbound_calls`, `answer_rate`, `name`.

```json
{ "user_id": "uuid", "name": "Bob Member", "email": "...", "status": "active",
  "role_name": "Team Member",
  "phone_number": "+15550001", "friendly_name": "Bob line", "number_status": 1,
  "total_calls": 3, "outbound_calls": 2, "inbound_calls": 1,
  "answered_calls": 2, "unanswered_calls": 1, "total_seconds": 420,
  "total_minutes": 7, "avg_duration_seconds": 140, "answer_rate": 66.7 }
```

`phone_number` / `friendly_name` / `number_status` are the line that user works from (an
active line wins when they have held more than one). A user with no number reports `""`,
never a missing key. `user_id` is what you pass to `admin/users/{id}/calls` when a row is
tapped.

A call is attributed via `from_user_id` (outbound) **or** `to_user_id` (inbound), so
internal calls are never double counted.

### 5.5 Feedback

**`GET admin/feedback`** — query `status` (`pending`|`in_progress`|`resolved`|`closed`),
`priority` (`low`|`medium`|`high`|`critical`), `type`, `user_id`, `answered` (bool),
`search`, `per_page`, `page`.

```json
{ "id": "uuid", "title": "App crashes", "message": "It crashed",
  "description": "On dial", "type": "bug", "priority": "high", "status": "pending",
  "answer": "", "is_answered": false, "answered_by": "", "answered_at": "",
  "user": { "id": "uuid", "name": "Bob", "email": "bob@example.com" },
  "created_at": "..." }
```

**`POST admin/feedback/{id}/answer`** — `{ answer* (≤5000), status? }` → the updated row.
Defaults `status` to `resolved`.

### 5.6 Contacts

**`GET admin/contacts/summary`**

```json
{
  "users": [ { "user_id": "uuid", "name": "Bob", "email": "...", "status": "active",
               "contacts_count": 3 } ],
  "total_assigned": 3,
  "unassigned_legacy": null
}
```

**`GET admin/contacts`** — query `user_id`, `search`, `from`, `to`, `per_page`, `page`.
**`id` is an Int**, not a UUID. Each row:

```json
{
  "id": 12, "user_id": "uuid", "first_name": "Ravi", "last_name": "Kumar",
  "number": "+919999911111", "email": "", "company_name": "",
  "is_dnd": false, "is_blacklisted": false,
  "has_calls": true,
  "calls": {
    "total_calls": 2, "inbound_calls": 0, "outbound_calls": 2, "answered_calls": 1,
    "total_seconds": 120, "total_minutes": 2,
    "last_call_at": "2026-08-20 11:02:17", "last_direction": "outbound"
  },
  "created_at": "..."
}
```

**`calls` answers "called or received", per contact, for that contact's owner.**
`outbound_calls` = the user rang this contact; `inbound_calls` = the contact rang the
user. `from` / `to` narrow it to a date range. Numbers are matched on their **last 10
digits**, so a contact saved as `9999911111` still matches a call logged as
`+919999911111`.

**`POST contacts/sync`** — *not* admin-only. Max 500 per call.

```json
{ "contacts": [ { "first_name": "Fresh", "last_name": "?", "number": "+15553333",
                  "email": "?", "company_name": "?", "extension": "?" } ] }
```

```json
{
  "created":   [ { "number": "+15553333", "contact_id": 3, "action": "created"  } ],
  "claimed":   [ { "number": "+15552222", "contact_id": 2, "action": "claimed"  } ],
  "existing":  [ { "number": "+15551111", "contact_id": 1, "action": "existing" } ],
  "conflicts": [ { "number": "+1555....", "contact_id": null, "action": "conflict",
                   "reason": "This number is already held by another user." } ],
  "failed":    [ { "number": "+1555....", "reason": "Could not be saved." } ],
  "summary": { "created": 1, "claimed": 1, "existing": 1, "conflicts": 0,
               "failed": 0, "total_owned": 3 }
}
```

- **claimed** — a legacy row nobody owned; the first user to sync it takes it.
- **conflicts** — contact numbers are still globally unique, so a number held by another
  user cannot be duplicated. Expected, not an error.

---

## 6. API reference — Part B: everything else the app uses

Unchanged by this work; listed so the console can reuse instead of duplicating. 95 routes
exist under `api/v1.0`; these are the ones the app calls.

### Auth & session
| Method | Path | Notes |
|---|---|---|
| POST | `admin/login` | **now also returns `data.role`** |
| POST | `SignIn` | device session |
| POST | `createProfile` | |
| POST | `logout` | rotates `auth_key`, invalidating the JWT |
| POST | `password/forgot`, `password/reset` | |

### Profile & account
| Method | Path | Notes |
|---|---|---|
| GET | `getProfile` | **now also returns `data.role`** |
| POST | `updateProfile`, `changePassword` | |
| GET | `getCredit`, `user-permissions` | |

### Phone numbers & telephony
| Method | Path | Notes |
|---|---|---|
| POST | `findPhoneNumber` | **reused by the admin purchase flow** |
| POST | `holdNumber`, `purchaseNumbers` | user-side single purchase |
| GET | `getNumberDetails`, `getAllNumberDetails` | |
| GET | `getCapabilityToken`, `getDialingCountries` | |
| POST | `makeOutBoundCall`, `makeInboundCall`, `callStatusUpdate`, `receivedCallStatusUpdate` | |
| POST | `getCallLogs` | the caller's own log (device-auth) |
| POST | `areaList` | |

### Contacts
| Method | Path | Notes |
|---|---|---|
| GET | `contacts/list`, `contacts/query`, `contacts/get/{id}` | **still unscoped — unchanged** |
| POST | `contacts/new`, `contacts/update/{id}` | |
| DELETE | `contacts/delete/{id}` | |
| POST | `contacts/toggle_dnd`, `contacts/toggle_blacklist` | |
| POST | `contacts/sync` | **new** |

### Team, roles, dashboard, feedback, billing
| Method | Path | Notes |
|---|---|---|
| GET/POST/PUT/DELETE | `team-members`, `getAllTeamMembers`, `createTeamMember`, `updateTeamMember`, `deleteTeamMember/{id}` | permission-gated, org-scoped |
| GET/POST/PUT/DELETE | `roles`, `roles/{id}`, `roles/assign`, `roles/remove`, `roles/user/update`, `roles/{roleId}/users`, `users/{userId}/role`, `users/assign-role` | |
| GET | `dashboard` | legacy, not org-scoped — **use `admin/analytics/*` instead** |
| GET/POST/PUT/DELETE | `getUserFeedback`, `createFeedback`, `updateFeedback`, `deleteFeedback/{id}`, `feedback` | user side, unchanged |
| GET/POST | `getAllPlans`, `getTeamSubscriptions`, `getAllInvoices`, `generatePaymentLink`, `checkPaymentStatus`, `getPaymentDetails`, `getAllInvoicesForUser` | |
| GET/POST | `companies`, `companyUpdate` | |

---

## 7. Kotlin work list

**`data/model/Models.kt`** — add `RoleBlockDto`, `AdminUserDto`, `AdminUserDetailDto`,
`UserStatsDto`, `AdminCallDto`, `AdminNumberDto`, `PurchaseResultDto`, `BulkResultDto`,
`AnalyticsOverviewDto`, `UserMetricDto`, `DailyTrendDto`, `AdminFeedbackDto`,
`ContactSummaryDto`, `AdminContactDto`, `ContactSyncResultDto` + paged wrappers.
Add `role: RoleBlockDto?` to `AdminLoginDataDto` and the profile DTO. Line ~953 currently
fakes the role as `"Team Member"` — replace with the real slug.
**Every new field nullable with a default** so a backend addition can never crash the app.

**`data/remote/LaravelApiService.kt`** — add a `13 · ADMIN` section mirroring the existing
style: `Response<GrowfoneCodeEnvelope<T>>`, `@Query` for filters, `@Body` for posts.

**`data/local/SessionManager.kt`** — persist `role_slug`; add
`fun isAdmin() = roleSlug in setOf("admin", "super-admin")`. Refresh it on login and on
every profile fetch.

**`data/repository/BizVoiceRepository.kt`** — one suspend function per endpoint, using the
repo's existing result wrapper. Expose `isAdminFlow`; on any 403 from an admin call, set it
false so the tab disappears instead of showing errors.

**`ui/navigation/MainTab.kt`** — add `ADMIN`.

**`ui/screens/main/MainContainerScreen.kt`** — role-filtered tab list (the only edit to an
existing screen).

**`ui/screens/admin/`** — `AdminHomeScreen`, `AdminUsersScreen`, `AdminUserFormScreen`,
`AdminUserDetailScreen`, `AdminNumbersScreen`, `AdminNumberPurchaseScreen`,
`AdminAnalyticsScreen`, `AdminFeedbackScreen`, `AdminContactsScreen`.

---

## 8. Gotchas that will bite

1. **`total_minutes` and `answer_rate` serialise as a whole number** when they have no
   fractional part (`7`, not `7.0`). Type them `Double` in Kotlin — Moshi's Double adapter
   accepts both.
2. **Two error envelopes** (§3.4). `status` vs `code`.
3. **Contact `id` is an Int**, everything else is a UUID string.
4. **`assigned_user` is nullable** on a number row; `assigned_numbers` can be an empty list.
5. **A user can hold only one active number.** Assign returns 400 otherwise — surface the
   server's message verbatim, it names the number to unassign.
6. **`unassigned_legacy` is null for normal admins.** Do not render "null" or "0".
7. **Deactivation is immediate** on the target's next request. If an admin deactivates
   themselves — they can't, the server refuses (400).
8. **Release spends nothing but destroys an asset.** Purchase spends money. Neither should
   ever be retried automatically.
9. **The existing `dashboard` endpoint is not org-scoped** and counts the whole system. Do
   not use it for the admin screens; use `admin/analytics/*`.
10. **Never render a call's raw `from`/`to` as "the other party".** Which column holds the
    outside number flips with direction. Use `counterpart_number`.
11. **A number can read as assigned with `assigned_user: null`** — that is `is_orphaned`,
    a real state, not a bug. Handle it rather than showing a blank name.
12. **`contacts.calls` is per owner.** When `user_id` is omitted the list spans users and
    each row's figures belong to that row's own owner.
13. **The balance can 503.** That is Twilio being unreachable, not an empty account —
    never fall back to rendering `0.00`. The Provider tab does the same.
14. **`db_id` on a provider row is a uuid**, and is empty for a number not yet in the
    database. Assign / unassign / release need that id, so those actions are unavailable
    on a `in_db: false` row until a sync imports it.
15. **Gate the Sync button on `can_sync`**, not on your own role guess.

---

## 9. Acceptance criteria

1. A `team-member` account sees no Admin tab; the app is byte-for-byte the same experience.
2. An `admin` account sees the 5th tab and can list, search, create, edit, deactivate and
   delete users, and open any user's call history.
3. Assign / unassign / release all work; release demands an explicit confirmation; sync is
   hidden for non-super-admins.
4. Bulk purchase reports purchased and failed numbers separately and never auto-retries.
5. Analytics shows per-user minutes, calls and answer rate, sortable, over a date range.
6. Feedback can be read and answered; the answered row flips to `resolved` in place.
7. Contacts summary shows per-user counts; `contacts/sync` runs from the Contacts screen
   and reports conflicts without an error dialog. Selecting a user shows, per contact,
   how many calls were made to and received from that number.
8. The numbers list names every holder, including one whose account was deleted (badged),
   and an orphaned number offers Unassign.
8. Every admin screen renders correctly in light and dark theme with no new colours.
9. Change a logged-in admin's role server-side: the app hides the tab on the next profile
   refresh rather than crashing.

---

## 10. Backend reference

| Thing | Where |
|---|---|
| Routes | `routes/api.php` — the `Route::prefix('admin')` block |
| Gate | `app/Http/Middleware/AdminApiGate.php` |
| Scope rules | `app/Services/Admin/AdminScope.php` |
| Controllers | `app/Http/Controllers/Api/Admin/` |
| Contact sync | `app/Http/Controllers/Api/ContactSyncController.php` |
| Postman | collection folder **14 · Admin Console** (22 requests, with examples) |
| Tests | `tests/Feature/AdminConsoleApiTest.php`, `ExistingApiContractTest.php` |

Run the backend tests:

```bash
php artisan test --filter="AdminConsoleApiTest|ExistingApiContractTest"
```

Seed the optional permissions (the gate works on role slug alone without this):

```bash
php artisan db:seed --class=AdminApiPermissionSeeder
```
