# Performance editorial

Approved direction: a dark-only sports aesthetic with editorial typography, near-black warm surfaces, high contrast text, and a restrained lime accent. Visual polish is the primary goal; existing navigation, button placement, workout flows, and numeric-entry gestures remain the reference.

## Applied treatment

- Shared color, typography, and shape tokens carry the identity into Sessions, Exercises, Stats, Settings, templates, and detail screens.
- Sessions adds an actual weekly progress ring in the history area without relocating Start/Resume or the template suggestion. Its target defaults to four completed sessions and can be changed from the ring section. The calendar week runs Monday through Sunday in the device timezone; a session belongs to the week it started. Drafts and unfinished workouts do not count.
- Weight remains tap-to-edit. Reps retain tap-to-edit, plus/minus taps, and press-and-hold scrubbing. Completion retains tap to log and long press to skip.
- Set completion uses a dark check on lime. Volume chart categories retain their independent semantic colors.
- Numeric entry retains live updates, first-digit replacement, decimal limits, haptics, and swipe/outside dismissal. The visual treatment adds a centered value, quieter keys, and a labeled Done action.

The browser concepts were visual exploration, not a replacement specification for native workflows. Decorative slashes and bars were rejected in favor of meaningful circular progress.
