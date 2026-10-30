# CLEAN14 — Universal Horizontal Carousel

## Baseline and scope

- Start from the exact runtime-approved CLEAN13.1P baseline.
- Target Projectivy Launcher 4.72, version code 97.
- Apply one shared carousel policy to every horizontally scrollable home row, including installed-app rows, icon/card rows, Red Light cards, and Android TV channel/program rows.
- Cover both normal single-row and multi-row presentations wherever they use the shared horizontal grid path.
- Do not split the behaviour into app-, Red Light-, or channel-specific implementations.

## Required anchor states

### Start

- The first card is at the natural left edge.
- The focused first card has no artificial leading buffer.
- Moving right from the first card establishes the normal middle anchor without a visible jump.

### Middle

- Keep one complete previous card visible to the left of the focused card.
- The focused card remains at a consistent horizontal anchor while the row advances.
- Translation is smooth and does not create large empty gaps.

### End

- Stop at the row's natural maximum scroll offset.
- The final card reaches the natural right edge; do not leave an artificial trailing gap.
- Once the row reaches this boundary, keep the row stationary while focus traverses the remaining visible cards toward the final card.
- Add only a subtle, few-pixel elastic end bounce. Do not allow a large overscroll or alter the focused item.

### Reverse from end

- When moving left from the end, move focus through the cards already visible while keeping the row fixed.
- Resume translating left only when focus moves past the normal middle anchor.
- Return smoothly and symmetrically to the middle/start geometry; do not snap or leave residual offset.

## Behaviour that must remain unchanged

- Vertical D-pad navigation and selected-row alignment.
- Card focus scale, glow, alpha, animation, and focus ownership.
- Click/launch behaviour and the original item/action models.
- Red Light discovery, refresh, provider rows, card identity, and playback routing.
- Android TV channel/program identity and launch routing.
- Existing row ordering, row selection, and per-row focused-card restoration.
- Touch/scroll settling behaviour unless it directly conflicts with the required carousel anchor.

## Implementation constraints

- Audit the actual Projectivy 4.72 shared row presenter and HorizontalGridView path before selecting the patch point.
- Prefer a shared presenter/grid-level implementation that naturally covers all row content types.
- Preserve CLEAN13.1P's Weather, Show Date, status-bar order, and manual Weather refresh unchanged.
- Do not modify the Red Light engine or card data to implement scrolling.
- Do not change global icon rendering.
- Do not claim success from a build alone; runtime testing on the Shield is required before CLEAN14 is frozen.

## Acceptance matrix

Test at minimum:

1. Empty/one-card rows and rows with only two cards.
2. Content that fits entirely inside the viewport (no unnecessary translation).
3. Long rows with enough cards to exercise start, middle, and end.
4. Rightward movement from start through middle to the final card.
5. Leftward movement from the end through the fixed-visible-card phase and back into middle/start.
6. The small end bounce, including repeated right input at the final card.
7. App/icon rows, Red Light rows, and Android TV channel/program rows.
8. Focus scaling/glow while cards are being translated.
9. Vertical movement between rows while horizontally scrolled.
10. Clicking focused cards at start, middle, and end.

CLEAN14 is not frozen until the full matrix passes on the Shield.
