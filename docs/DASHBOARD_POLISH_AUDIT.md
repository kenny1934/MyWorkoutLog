# Dashboard polish audit (2026-04-20)

## Context

Reviewed the Dashboard screen components (`DashboardScreen.kt`, `DashboardWidgetCards.kt`, `DashboardWidgetComponents.kt`, `DashboardHelpers.kt`, `ExpandableWidgetCard.kt`, `DashboardChartCards.kt`) after slices 46–57 and 62 consumed the prior comprehensive audit. This pass focuses on freshly identified polish and consistency issues that do not overlap with landed fixes. Device context: Samsung Z Fold (compact outer + tablet inner), Material 3, `extendedColors` theme tokens in place.

## Findings

### Round C — Consistency & spacing

#### P-1 — Multiple card elevation values used inconsistently
- **Where:** `DashboardHelpers.kt:37` (`EnhancedDashboardWidgetCard`), `:99` (`EnhancedInsightCard`), `:207` (`InteractivePerformanceChart`); `ExpandableWidgetCard.kt:37` (`SimpleExpandableWidgetCard`); `DashboardWidgetCards.kt:51, 96, 295, 389, 638` (widget cards).
- **What's there now:** Card elevations hardcoded to `4.dp`, `6.dp`, or `2.dp` depending on context. No consistent pattern — insight cards use `6.dp` (highest), dashboard helpers use `4.dp`, nested chart cards use `2.dp`.
- **Suggested change:** Define a `Dimens.elevationCardDefault` (use `Dimens.elevationCardRaised`, currently `4.dp`) or `Dimens.elevationCardEmphasized` (`6.dp` for insights) to centralize card elevation choices. Replace all hardcoded values.
- **Size:** S
- **User-visible gain:** Visual cohesion; when a future slice adjusts the card elevation strategy, one change propagates everywhere.
- **Risk / notes:** Purely a token migration. No functional change. Audit already flagged the hardcoded colors in `DashboardWidgetComponents.kt` (difficulty, achievement, muscle-group colors remain hardcoded but are domain-specific palettes, not design system — reasonable to leave as-is).

#### P-2 — Inconsistent badge padding in `NextSessionCtaCard` and `SimpleCycleProgressWidgetCard`
- **Where:** `DashboardWidgetCards.kt:352, 368` (Deload / RIR badges in `NextSessionCtaCard`), `:412, 428` (same badges in `SimpleCycleProgressWidgetCard`).
- **What's there now:** All four use `padding(horizontal = 10.dp, vertical = 4.dp)` — but this is a magic number, not a token.
- **Suggested change:** Promote to `Dimens.badgePaddingHorizontal = 10.dp` and `Dimens.badgePaddingVertical = 4.dp` (or a shorthand `badgePadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)` if Kotlin's scope functions are clear enough). Use consistently across both cards and any other badge patterns.
- **Size:** XS
- **User-visible gain:** None directly — but codifies a pattern seen twice already and likely to be re-used in future polish passes.
- **Risk / notes:** Safe refactor; only changes call sites, no logic.

#### P-3 — Hard-coded `16.dp` padding in card Column wrappers
- **Where:** `DashboardWidgetCards.kt:300` (`NextSessionCtaCard`), `:391` (`SimpleCycleProgressWidgetCard`), `:640` (`SimpleBodyweightTrendWidgetCard`), `:529` (`error state Column`).
- **What's there now:** Card inner padding is hardcoded to `16.dp`. `SimpleWelcomeWidgetCard` and others use `adaptiveContentPadding()` (which returns `16.dp` compact, `24.dp` medium, `32.dp` expanded).
- **Suggested change:** Migrate all `Column(modifier = Modifier.padding(16.dp))` inside cards to `Column(modifier = Modifier.padding(adaptiveContentPadding()))`. Allows cards to breathe proportionally on tablets.
- **Size:** M
- **User-visible gain:** Cards on tablets (Z Fold unfolded) feel less cramped and align with the adaptive theme.
- **Risk / notes:** Non-trivial but low-risk change; only affects padding Modifier, no layout logic. Device-verify on tablet (inner) screen. Affects 4 locations.

#### P-4 — Inconsistent vertical spacers between card title and content
- **Where:** `DashboardWidgetCards.kt:433` (Cycle Progress → `16.dp`), `:658` (Bodyweight Trend → `12.dp`), `ExpandableWidgetCard.kt:81, 96` (Simple Expandable → `8.dp` or `12.dp` conditional).
- **What's there now:** Title-to-content spacing varies: some use fixed `16.dp`, others use `12.dp`, others use adaptive `8.dp`/`12.dp`.
- **Suggested change:** Standardize on a single post-title spacer across all widgets. Suggest `12.dp` (midway between the current range). Use `Dimens.spacingWidgetTitle = 12.dp` to codify.
- **Size:** S
- **User-visible gain:** Vertical rhythm consistency; widgets feel designed as a family.
- **Risk / notes:** Safe change. Affects visual proportions only.

### Round D — Token opportunities

#### P-5 — `DashboardHelpers.kt:99` uses unused `Color` import
- **Where:** `DashboardHelpers.kt:18` (import statement).
- **What's there now:** `import androidx.compose.ui.graphics.Color` is present but no hardcoded `Color()` calls in the file. All colors use theme tokens.
- **Suggested change:** Remove the unused import.
- **Size:** XS
- **User-visible gain:** None — lint noise cleanup.
- **Risk / notes:** Safe; no color values will change.

#### P-6 — Hard-coded `0xFFFF9800` (amber) for INSUFFICIENT_DATA trend
- **Where:** `DashboardWidgetComponents.kt:114` (`TrendIndicator`).
- **What's there now:** The amber color for insufficient-data trends is hardcoded, while improving/declining/stable use theme tokens (success/error/onSurfaceVariant).
- **Suggested change:** Consider promoting to a theme token if the color recurs elsewhere (it appears to be unique to this context). If truly single-use, document why as a comment; if it recurs, create `Dimens.insufficientDataColor` or add to `extendedColors` as `insufficientDataTint`.
- **Size:** XS
- **User-visible gain:** Consistency; easier to theme or adjust the insufficient-data color in future.
- **Risk / notes:** Low-risk if single-use. Grep first to confirm no other call sites.

### Round E — Widget-level polish

#### P-7 — Insight card elevation too high relative to other widgets
- **Where:** `DashboardHelpers.kt:99` (`EnhancedInsightCard`).
- **What's there now:** Insight cards use `elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)`, while all standard widget cards use `Dimens.elevationCardRaised` (4.dp).
- **Suggested change:** Reduce to `Dimens.elevationCardRaised` (4.dp) to maintain visual hierarchy parity with regular widgets. Insight priority is already signaled by color (error container for URGENT, primary container for HIGH); elevation is redundant.
- **Size:** XS
- **User-visible gain:** More cohesive card surface hierarchy; insights feel like part of the dashboard grid, not floating above it.
- **Risk / notes:** Pure visual polish. No functional impact. Verify on Z Fold inner + outer, light + dark.

#### P-8 — Error state card elevation inconsistency
- **Where:** `DashboardScreen.kt:523–529` (error state in compact path), `:391–407` (error state in tablet path).
- **What's there now:** Error card in tablet path uses default Card elevation (implicit, likely inherited). Compact path's error Card also uses default. No explicit elevation token used.
- **Suggested change:** Apply `CardDefaults.cardElevation(defaultElevation = Dimens.elevationCardRaised)` explicitly to both error cards for consistency with other widget cards.
- **Size:** XS
- **User-visible gain:** Consistent visual weight; error state feels like a standard dashboard component, not a special overlay.
- **Risk / notes:** Safe; only affects Card elevation. Both compact + tablet paths should be updated in parallel.

#### P-9 — `NextSessionCtaCard` uses `primaryContainer` background but no visual rhythm cue
- **Where:** `DashboardWidgetCards.kt:291–294` (`NextSessionCtaCard` card colors).
- **What's there now:** Primary CTA card uses `primaryContainer` color + nested primary-colored icon surface (44.dp), but the nested surface's elevation and shadow aren't differentiated from the outer card — both are flat Material surfaces with no shadow hierarchy.
- **Suggested change:** Consider adding a subtle `shadowElevation` or explicit `elevation` to the nested icon surface to create a slight lift. Or flatten the nested surface entirely and use `primary` color inline without the extra Surface wrapper to reduce nesting depth. Keep the wrapping only if the lift improves affordance.
- **Size:** S
- **User-visible gain:** Clearer affordance for the nested play button — does it feel clickable or decorative? Current rendering is ambiguous.
- **Risk / notes:** Design judgment call. Tablet vs compact rendering may differ in perceived depth. Device-verify.

#### P-10 — `EnhancedInsightCard` gradient + scale animation together risk over-decoration
- **Where:** `DashboardHelpers.kt:63–195` (`EnhancedInsightCard` entire composable).
- **What's there now:** Insight cards animate in via `graphicsLayer(scale: animatedScale)` + render with a `Brush.linearGradient()` over containerColor → surface. Together, they create a visually heavy "decorated" first impression that may not align with a minimalist dashboard aesthetic.
- **Suggested change:** Run device verification (Z Fold inner + outer, light + dark) to confirm the combined effect feels balanced. If the gradient feels redundant with the color-coded containers (error/primary/surface), remove the gradient and rely on solid background + animation. Animation alone (scale entrance) is sufficient for polish.
- **Size:** S
- **User-visible gain:** Reduces visual clutter; insights remain scannable and prioritized by color, but feel less "over-designed."
- **Risk / notes:** No functional risk, purely aesthetic. Defer if happy with the current rendering. Slice 57 already addressed the Welcome widget's gradient + scale; consistency suggests insight cards might benefit from similar flattening.

### Round F — Spacing & density

#### P-11 — `EnhancedDashboardWidgetCard` title-to-content spacer is hardcoded
- **Where:** `DashboardHelpers.kt:56` (`Spacer(modifier = Modifier.height(12.dp))`).
- **What's there now:** Post-title spacer is `12.dp` hardcoded. Other widget cards use `adaptiveSpacing()` or conditional values.
- **Suggested change:** Use `Dimens.spacing12` (already exists) or promote to a named token `Dimens.spacingCardHeader` to match the adaptive pattern used elsewhere.
- **Size:** XS
- **User-visible gain:** Consistency; all card headers have equivalent breathing room.
- **Risk / notes:** Safe. No layout reflow risk; purely a spacing token swap.

#### P-12 — Chart cards use `8.dp` padding, widgets use `16.dp`
- **Where:** `DashboardChartCards.kt:67` (`InteractivePerformanceChart` Chart Box → `padding(8.dp)`), vs `DashboardWidgetCards.kt:391` (SimpleCycleProgressWidgetCard Column → `padding(16.dp)`).
- **What's there now:** Chart inner padding is `8.dp` (possibly to maximize chart area), while card Column padding is `16.dp`. No explicit token.
- **Suggested change:** If the `8.dp` is intentional (to maximize chart real estate), document it with a comment. If unintentional, align to `Dimens.spacingMd` (12.dp) or the card's outer padding for visual balance.
- **Size:** XS
- **User-visible gain:** None if intentional. Clarification only.
- **Risk / notes:** Low risk. Verify chart doesn't clip or feel cramped if padding is increased.

### Round G — Accessibility & tap targets

#### P-13 — `NextSessionCtaCard` nested icon Surface is 44.dp but no content description
- **Where:** `DashboardWidgetCards.kt:303–316` (icon Surface wrapper).
- **What's there now:** The play-icon Surface is `44.dp` (meets minimum 48dp touch target), but the outer Row that wraps the entire card has `clickable { ... }` at the parent level. Icon itself has no explicit `contentDescription` — it's wrapped inside a Surface with no semantic role.
- **Suggested change:** Add `contentDescription = "Start next session"` to the Icon inside the nested Surface. The Row's outer clickable is sufficient for the action, but the icon should still carry a label for screen readers.
- **Size:** XS
- **User-visible gain:** Accessibility; screen reader users understand the icon's purpose.
- **Risk / notes:** Non-breaking. Improves a11y. The outer row's touch target (height depends on Row's padding + text) should already exceed 48dp, so tap target is fine.

#### P-14 — `ArrowReorderWidgetCard` customization controls use 36.dp `IconButton`, below Material minimum
- **Where:** `DashboardScreen.kt:102–137` (all three IconButtons in the customization chip).
- **What's there now:** Icon buttons are explicitly `modifier = Modifier.size(36.dp)`, with icons inside that are `20.dp`. This results in a touch target smaller than the Material 3 48dp minimum.
- **Suggested change:** Change `size(36.dp)` to `size(Dimens.touchTarget)` (48.dp) or use `modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)` if space is constrained. Alternatively, increase the chip's internal padding to absorb the 48dp requirement.
- **Size:** S
- **User-visible gain:** Accessibility; easier to tap the reorder controls, especially on small screens or for users with large fingers. Reduces miss-taps.
- **Risk / notes:** Will increase the customization chip height. Device-verify on compact outer screen to ensure it doesn't overflow the widget. May need to trim the chip padding or reduce icon size instead.

### Round H — Empty / error states

#### P-15 — Empty-state copy in chart widgets inconsistent with dashboard standard
- **Where:** `DashboardChartCards.kt:38–41` (`InteractivePerformanceChart` → "No performance data yet"), `DashboardWidgetCards.kt:869–872` (`SimplePerformanceTrendWidgetCard` → same), vs `DashboardWidgetCards.kt:729–732` (Bodyweight Trend → "No bodyweight data yet").
- **What's there now:** Both say "No performance data yet" vs "No bodyweight data yet." The phrasing is consistent (adjective + "data yet"), but slices 49–53 unified empty-state copy across the dashboard. Verify no drift remains.
- **Suggested change:** Grep all `"No.*data yet"` strings across the dashboard and confirm they follow the pattern from STATE.md slice 53 ("No performance data yet", "No volume data yet", "No bodyweight data yet", "No achievements yet"). If any outliers remain, unify in a follow-up.
- **Size:** XS
- **User-visible gain:** Copy consistency; dashboard feels more polished and intentional.
- **Risk / notes:** Pure string audit. No logic changes.

## Out of scope / product decisions

None at this time. All findings are polish-focused without requiring feature decisions.

## Convention opportunities

1. **Card elevation tokens** — Consolidate hardcoded `4.dp`, `6.dp`, `2.dp` elevations into named Dimens tokens (`elevationCardDefault`, `elevationCardEmphasized`, `elevationCardSubtle`). Current code has 6 hardcoded elevation values across 4 files.

2. **Badge padding** — The `horizontal = 10.dp, vertical = 4.dp` badge padding pattern repeats 4 times (Deload/RIR badges in two cards). Worth a `Dimens.badgePadding` or `badgePaddingHorizontal` + `badgePaddingVertical` pair.

3. **Post-title spacing** — Widget cards use varied vertical spacers after titles (8dp, 12dp, 16dp). Standardizing to one value (e.g., `Dimens.spacingCardHeader = 12.dp`) would improve visual rhythm consistency.

4. **Adaptive padding** — Some card Columns use `16.dp` hardcoded; others use `adaptiveContentPadding()`. Standard should be adaptive everywhere for tablet responsiveness.

