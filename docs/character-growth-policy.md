# Character Growth Policy 2.0

## Attendance

- Attendance uses completed personal focus intervals in `focus_interval`.
- Only `FOCUS` and `NORMAL` phases count; Pomodoro breaks do not count.
- Interval timestamps are interpreted using the `Asia/Seoul` calendar date.
- An interval crossing midnight is split across both dates.
- Multiple timer and pause/resume intervals on the same date are added together.
- A date counts as one attendance day when its total is at least 1,800 seconds.
- Attendance is cumulative, not consecutive.

## Growth Requirements

| Level | Attendance days | Focus hours |
|---:|---:|---:|
| 1 | 0 | 0 |
| 2 | 15 | 20 |
| 3 | 30 | 50 |
| 4 | 50 | 90 |
| 5 | 75 | 140 |
| 6 | 105 | 200 |
| 7 | 140 | 280 |
| 8 | 180 | 380 |
| 9 | 225 | 500 |
| 10 | 275 | 650 |
| 11 | 330 | 850 |
| 12 | 400 | 1100 |

Both attendance and focus-time requirements must be satisfied. Lifetime focus
time uses the canonical `focus_session.total_duration` metric defined in
`focus-time-metrics.md`.

## Existing Users

Attendance is calculated retroactively from existing completed intervals, so no
separate attendance migration is required. Existing `user_character` ownership
is preserved and characters are never revoked. A check-award request only adds
active characters whose policy requirements are now satisfied.

The level-based policy table is authoritative. Existing character image and name
records remain unchanged, and legacy `unlock_time_in_seconds` values do not drive
award decisions.
