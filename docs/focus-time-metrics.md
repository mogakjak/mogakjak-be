# Focus Time Metrics

## Canonical Data

Personal focus metrics use `focus_session.total_duration` as the canonical source.
Todo completion and deletion do not affect accumulated focus time. This preserves
historical focus records even when a Todo is incomplete or later deleted.

The shared group timer stored in `group_focus_session` is not included. Its API,
WebSocket events, scheduler, and stored records remain unchanged.

## Definitions

- Total focus time: all `focus_session` rows for the user.
- Personal focus time: rows where `focus_session.group_id IS NULL`.
- Mogakjak focus time: rows where `focus_session.group_id IS NOT NULL`.
- Period metrics: include rows whose `started_at` is within the requested range.
- Lifetime metrics: include all rows for the user.

The focus dashboard summary, My Page `totalFocusTime`, My Page
`total-study-time`, and character award checks use these definitions.

Hourly and daily charts continue to use completed focus intervals so that break
phases and unfinished intervals are excluded from visualized focus duration.
