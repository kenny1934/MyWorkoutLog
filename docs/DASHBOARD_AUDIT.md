# Dashboard UI/UX audit — 2026-04-20

Scope: `ui/DashboardScreen.kt`, `DashboardWidgetCards.kt`, `DashboardHelpers.kt`, `DashboardPlaceholders.kt`, `DashboardWidgetComponents.kt`, `DashboardChartCards.kt`, `ExpandableWidgetCard.kt`. ~4,000 LOC total.

Re-verify every line number and call-site claim against the current tree before editing — premises go stale, as slices 43/44 proved.

## Round A — content redundancy (highest UX value, judgment calls)

### 1. Welcome streak vs QuickStats streak — duplicate first-screen data (S)
`SimpleWelcomeWidgetCard` renders "🔥 N day streak" when streak > 0 (`DashboardWidgetCards.kt:110–136`). `SimpleQuickStatsWidgetCard` renders a `StatCard` labelled "Streak" with the same number (`:207–219`). Both default above the fold. Drop the Welcome badge — QuickStats already owns the metric; Welcome keeps greeting + motivational message.

### 2. Bodyweight widget ⊂ Bodyweight Trend widget (S)
`SimpleBodyweightWidgetCard` (`:320`) = current weight + last-recorded date. `SimpleBodyweightTrendWidgetCard` (`:765`) = current + change + entries + mini chart. Strict superset. Deprecate the simple card or default-hide when the trend widget is enabled.

### 3. NextSessionWidget Start Session button is redundant with NextSessionCtaCard (XS) — near-mirror of slice 38
Slice 36 put the persistent CTA at the top of the dashboard. Slice 38 dropped the redundant Start button from `SimpleCycleProgressWidgetCard`. `SimpleNextSessionWidgetCard` (`:1032`) still has its own "Start Session" button (`:1161`) plus a collapsed-view session name that duplicates the top CTA's main label. The widget's distinct value is the expanded exercise preview. Drop the Start Session button and probably the collapsed session-name restatement; keep the widget as "what does this session look like?"

### 4. CycleProgress + NextSession widgets → "Active Cycle" parent (M, defer)
Both are meaningless without an active cycle. Merging would be a bigger move and depends on Round A #1–3 landing first. Flag only.

## Round B — dead / duplicated composables (safe cleanup)

### 5. `StatCard` defined twice (XS)
Private `DashboardWidgetCards.kt:258` (used by QuickStats) vs public `DashboardWidgetComponents.kt:52` (different signature — value/label, optional trend, onClick). Grep for call sites of the public one; delete if orphaned.

### 6. `InsightCard` (old) vs `EnhancedInsightCard` (current) (XS)
`DashboardWidgetComponents.kt:237` vs `DashboardHelpers.kt:63`. Only Enhanced is used on the dashboard. Verify + delete.

### 7. `QuickActionButton` vs `EnhancedQuickActionButton` (XS)
Tablet path `AdaptiveWidgetGrid` uses the plain one (`DashboardScreen.kt:235`). Compact path uses Enhanced (`:734`). Likely leftover from an incomplete migration. Unify to Enhanced.

### 8. `WorkoutHeatmapGrid` may be dead (XS)
`DashboardWidgetComponents.kt:460`. `ActivityHeatmapGrid` + `EnhancedActivityHeatmap` cover the two live heatmap surfaces. Grep; delete if orphaned.

## Round C — header / shell consistency

### 9. Dashboard doesn't use `ScreenScaffold` (M, cross-cutting)
Slice 26 migrated 6 screens; dashboard was skipped. Hand-rolled header rows in both compact (`DashboardScreen.kt:568`) and tablet (`:411`) paths. Wrap in `ScreenScaffold(title = "Dashboard")`, hoist Edit/Done into `actions`, drop the inline titles. Touches both paths.

### 10. Customization toggle split (S)
Compact: `IconButton` (icon only, `:610`). Tablet: `OutlinedButton` icon + text (`:429`). Unify — tablet version is more discoverable.

### 11. Hidden debug gesture ships in release (S)
Long-press on "Dashboard" title opens AlertDialog that resets dismissed insights (`:425`, `:580`). No visual affordance. `onClick = { /* Normal click does nothing */ }` is misleading. Gate behind `BuildConfig.DEBUG` or move to Settings. Drop the no-op normal click.

## Round D — color tokens (opportunistic, slice-25 pattern)

### 12. Hardcoded `Color(0xFFFF6B35)` streak orange (XS)
Used in Welcome (`DashboardWidgetCards.kt:114, 125`) + QuickStats StatCard (`:209, 239`). Promote to `ExtendedColors.accent` (or a new `streak` token) and migrate.

### 13. Hardcoded `Color(0xFF4CAF50)` / `Color(0xFFF44336)` change colors (XS)
BodyweightTrend widget (`:821–823`), also appears in `TrendIndicator` (`DashboardWidgetComponents.kt:176–178`). Replace with `MaterialTheme.extendedColors.success / warning` or `primary / error` for consistency.

## Round E — insights consolidation

### 14. Two insights regions with different card styling (S)
Urgent/High → inline `EnhancedInsightCard` at top. Low/Medium → wrapped inside an `EnhancedDashboardWidgetCard(title = "Insights")` container at bottom. Same content type, disjoint visuals. Pick one: fold low-priority into the top stream sorted by priority, or wrap all in the "Insights" container. Verify tablet path (`AdaptiveWidgetGrid`) too — it does the priority split without the bottom wrapper.

### 15. Two refresh methods on error (XS)
Error card "Retry" → `refreshDashboard()` (`DashboardScreen.kt:665`). Skeleton-error path "Retry" → `onPullToRefresh()` (`:478`). Verify behavioural equivalence on the VM side; pick one.

## Round F — small polish (defer until Round A–C decisions settle)

### 16. Quick-actions scroll indicator is static (S)
`DashboardScreen.kt:754` — three dots + arrow, not tied to scroll position. Replace with a real page indicator or drop — LazyRow edge fade already signals scroll.

### 17. Widget empty-state copy inconsistent (XS)
"No weight recorded" / "No bodyweight data available" / "No performance data available" / "No volume data available" / "Keep training to unlock achievements!". Unify tone in one pass.

### 18. Welcome widget visual treatment is heavy (defer, design call)
`graphicsLayer(scale)` + `Brush.linearGradient` over greeting text. Flag only — flatten if you want a less "decorated" first impression. Not a slice yet.

## Suggested ordering

1. **Round B (5–8)** — dead code, XS each, no user-visible change. Safe re-entry into the area and confirms nothing is silently broken.
2. **Round A #1 + #3** — highest UX win, and #3 is a near-mirror of slice 38 so the pattern is fresh.
3. **Round A #2** — depends on user decision (drop the simple bodyweight widget entirely, or gate it).
4. **Round C #9** — `ScreenScaffold` migration. M-sized, probably its own session.
5. **Rounds D, E, F** — opportunistic; pick one per session as the area is touched.

Per slice-25 working style: token migration happens as files are touched, not as a dedicated slice. Round D #12 / #13 could ride on top of Round A #1 (Welcome + QuickStats) and Round A #2 (Bodyweight Trend).
