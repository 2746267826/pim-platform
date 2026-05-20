# Calendar and PC Tracker Visual Refinement Design

## Context

The current visual redesign is functional, but several high-density views still feel like raw engineering controls:

- Calendar timeline and month views are visually harsh, especially the timeline grid.
- PC activity heatmap uses the same small-cell layout across dimensions, so hour view wastes space and month view looks like a partial year view.
- Category timeline displays unreadable labels inside narrow blocks, and its hover tooltip is clipped or hidden.
- Category timeline and keyboard/mouse heatmap are placed side-by-side even though both need wide space.
- Keyboard/mouse heatmap is too simplified and does not represent a standard 108-key keyboard plus a two-side-button mouse.

## Decision

Keep FullCalendar as the calendar interaction engine and rebuild the visual layer around it. This preserves working route state, task/event editing, time selection, and external task drag/drop while removing the raw table aesthetic.

Do not migrate to another calendar library in this pass. TOAST UI Calendar remains a possible later migration target, but it would add integration risk without guaranteeing better support for the existing right-side task drag workflow.

## Calendar Design

The calendar page should feel like a polished planning board rather than a spreadsheet:

- Keep the existing timeline/month segmented view; only one calendar view renders at a time.
- Retain dragging right-side inbox tasks into timeline slots and month days.
- Replace garbled Chinese labels with clean copy: `日历`, `时间轴`, `月视图`, `今天`, `上一段`, `下一段`.
- Add a calmer board shell with soft gradients, rounded lanes, reduced borders, and stronger event cards.
- Render event content with a custom FullCalendar renderer so tasks and events show as compact cards with priority/category color, title, and time.
- Improve timeline legibility with larger row spacing, muted hour labels, and subtle lane backgrounds.
- Improve month view with card-like day cells, clear current-day styling, and reduced grid harshness.

## PC Activity Heatmap Design

Activity heatmap should use a different visual grammar per dimension:

- `hour`: 24 responsive horizontal hour blocks filling the available width, with labels such as `00`, `06`, `12`, `18`, `23`.
- `day`: recent-day grid remains compact and scannable.
- `month`: current-range month calendar matrix, grouped by weekday rows and week columns, visually distinct from year.
- `year`: annual density matrix remains broad and compact.
- Tooltip details should be readable and not clipped by the heatmap panel.

## PC Category Timeline Design

Category timeline should prioritize shape over text when cramped:

- Render the timeline as a full-width card, not side-by-side with keyboard/mouse.
- Blocks narrower than a readable threshold should hide their inline text and rely on hover/focus tooltip.
- Tooltip should have a high z-index and room above/below the track so it is not clipped.
- Keep the 00:00, 06:00, 12:00, 18:00, 24:00 rhythm labels.

## Keyboard and Mouse Heatmap Design

Keyboard/mouse heatmap should become a wide diagnostic diagram:

- Place it below category timeline in a full-width card.
- Render a standard 108-key keyboard layout: function row, alphanumeric cluster, navigation cluster, arrow keys, and numeric keypad.
- Render a mouse diagram with left/right/middle buttons, wheel, and two side buttons.
- Use the same heat color scale as keyboard keys for mouse buttons.
- Preserve shortcut summary, but keep it below the diagrams so it does not compete for horizontal space.

## Validation

Manual/browser validation should verify:

- `/calendar?view=timeline` has one time-grid view, no console errors, and right-side tasks remain draggable.
- `/calendar?view=month` has one month view, no console errors, and right-side tasks remain draggable.
- PC activity heatmap changes visual structure between hour, month, and year dimensions.
- Category timeline no longer shows text in very narrow blocks and tooltip is visible above other content.
- Keyboard/mouse heatmap is full-width and includes 108-key keyboard areas plus a mouse with two side buttons.
