# Retro Terminal UI - Technical Specification
**MyWorkoutLog - Mobile-First PWA with Terminal/Retro Aesthetic**

**Date**: 2025-01-31
**Version**: 2.0 (Updated with Retro UI Direction)
**Author**: Claude Code Analysis

---

## Executive Summary

**REVISED RECOMMENDATION: Mobile-First PWA with Terminal/Retro Gaming Aesthetic**

Building on the previous PWA recommendation, this spec incorporates a **unique terminal/retro gaming UI** inspired by:
- **Terminal aesthetics**: Tmux-style layouts, monospace fonts, Dracula color scheme
- **8-bit pixel art**: Kamen Rider Zeztz-style animations, retro gaming sprites
- **Power user design**: Information-dense, keyboard shortcuts (desktop), gesture-optimized (mobile)

**Critical Clarification**: **MOBILE IS PRIMARY** - User brings phone to gym, laptop is for planning/analysis. The terminal aesthetic must be **touch-optimized** while maintaining geeky visual style.

---

## Design Vision

### Aesthetic Pillars

1. **Terminal Aesthetic** (Visual style, not command-line primary)
   - Monospace typography (JetBrains Mono, Fira Code, or similar)
   - Dracula color scheme (purple/pink/cyan accents on dark background)
   - Tmux-inspired split panes
   - Minimal ASCII art accents (not overwhelming)
   - CRT scanline effects (subtle, toggleable)

2. **Retro Gaming Elements**
   - 8-bit pixel art exercise icons/animations
   - Kamen Rider Zeztz-style transformation animations for PRs
   - Retro gaming HUD for stats (health bars → volume bars, XP → progression)
   - Pixel art sprite animations (simple, 32x32 or 64x64)

3. **Power User Optimization**
   - Information-dense layouts (show all relevant data)
   - Keyboard shortcuts for desktop
   - Swipe gestures for mobile
   - Minimal navigation steps

4. **Mobile-First Touch Optimization**
   - No keyboard popup spam (hybrid number pad overlay)
   - Swipe gestures for quick actions
   - Large touch targets (min 44x44px)
   - Haptic feedback for interactions
   - Single-handed operation support

---

## Visual Design System

### Color Palette (Dracula Base)

```css
/* Dracula Theme */
--background: #282a36;
--current-line: #44475a;
--foreground: #f8f8f2;
--comment: #6272a4;
--cyan: #8be9fd;
--green: #50fa7b;
--orange: #ffb86c;
--pink: #ff79c6;
--purple: #bd93f9;
--red: #ff5555;
--yellow: #f1fa8c;

/* Terminal Accents */
--border-color: #6272a4;
--selection-bg: #44475a;
--cursor: #f8f8f2;

/* Pixel Art Highlights */
--pixel-shadow: rgba(0, 0, 0, 0.5);
--pixel-highlight: rgba(255, 255, 255, 0.1);
```

### Alternative Themes (Preset Switcher)

Users can switch between:
1. **Dracula** (default) - Purple/pink/cyan
2. **Gruvbox Dark** - Warm orange/brown tones
3. **Nord** - Cool blue/teal palette
4. **Catppuccin Mocha** - Pastel purple/pink
5. **Matrix Green** - Classic green-on-black terminal
6. **Amber Terminal** - Vintage orange terminal
7. **Cyberpunk Neon** - Bright neon pink/cyan on black
8. **Solarized Dark** - Professional dark mode

### Typography

```css
/* Monospace Primary */
--font-mono: 'JetBrains Mono', 'Fira Code', 'Monaco', 'Consolas', monospace;

/* Font Sizes (Mobile-first, scales up) */
--text-xs: 0.75rem;   /* 12px - labels */
--text-sm: 0.875rem;  /* 14px - body text */
--text-base: 1rem;    /* 16px - primary */
--text-lg: 1.125rem;  /* 18px - headings */
--text-xl: 1.25rem;   /* 20px - section titles */
--text-2xl: 1.5rem;   /* 24px - page titles */

/* Line height for readability */
--leading-tight: 1.25;
--leading-normal: 1.5;
```

### Spacing & Layout

```css
/* Mobile-first spacing (tight on phone, relaxed on desktop) */
--space-1: 0.25rem;  /* 4px */
--space-2: 0.5rem;   /* 8px */
--space-3: 0.75rem;  /* 12px */
--space-4: 1rem;     /* 16px */
--space-6: 1.5rem;   /* 24px */
--space-8: 2rem;     /* 32px */

/* Touch targets (minimum 44x44px per iOS HIG) */
--touch-min: 44px;

/* Border radius (subtle, not rounded) */
--radius-sm: 2px;
--radius-md: 4px;
```

### Pixel Art Specifications

```
Exercise Icons:
- Size: 64x64 pixels (scales to 32x32 on mobile)
- Format: PNG with transparency or GIF for animations
- Style: Simple, high-contrast, recognizable silhouettes
- Animation: 4-8 frames max (looping)
- Color: Match Dracula palette (green/cyan/purple)

PR Celebration Animations:
- Size: 128x128 pixels
- Duration: 1-2 seconds
- Style: Kamen Rider Zeztz transformation-style
- Trigger: New PR detected
- Haptic: Medium impact on mobile
```

---

## User Interface Architecture

### Navigation Structure

```
Mobile (Primary):
┌──────────────────────┐
│ ⚡ MyWorkoutLog      │ ← Top bar (fixed)
├──────────────────────┤
│                      │
│  [Content Area]      │ ← Swipeable views
│                      │
│                      │
├──────────────────────┤
│ 🏠 📊 🗂️ 💪 ⚙️      │ ← Bottom nav (icon + label)
└──────────────────────┘

Desktop (Secondary):
┌────────────────────────────────────────┐
│ ⚡ MyWorkoutLog    [Cmd+K] [Theme] [👤] │
├─────────────┬──────────────────────────┤
│ Sidebar     │ Main Content             │
│             │                          │
│ 🏠 Dashboard│  [Split panes when       │
│ 📊 Analytics│   master-detail needed]  │
│ 🗂️ Cycles   │                          │
│ 💪 Exercises│                          │
│ ⚙️ Settings │                          │
│             │                          │
└─────────────┴──────────────────────────┘
```

### Bottom Navigation (Mobile)

```typescript
const navItems = [
  { icon: '🏠', label: 'Home', route: '/' },
  { icon: '📊', label: 'Stats', route: '/analytics' },
  { icon: '🗂️', label: 'Cycles', route: '/mesocycles' },
  { icon: '💪', label: 'Log', route: '/workout/log', primary: true },
  { icon: '⚙️', label: 'Settings', route: '/settings' }
];

// Primary action (Log Workout) emphasized with:
// - Larger size
// - Accent color (Dracula purple)
// - Subtle pixel art glow
```

### Master-Detail Layout (Tmux-Inspired)

**Desktop (Z Fold 6 Unfolded):**
```
┌─────────────────────┬──────────────────────────┐
│ ▸ EXERCISES         │ █ BENCH PRESS            │
│                     │                          │
│ █ Bench Press       │ Last Workout: 2025-01-30 │
│   Squat             │ 100kg × 8, 8, 7 @RIR 2   │
│   Deadlift          │                          │
│   Pull-up           │ ▓▓▓▓▓▓▓▓▓░ e1RM: 125kg   │
│   Dip               │                          │
│                     │ ┌─ VOLUME (12 WEEKS) ──┐ │
│ 🔍 Search...        │ │  40 ┤     ╭─╮         │ │
│                     │ │  30 ┤   ╭─╯ ╰╮        │ │
│ [1/50 exercises]    │ │  20 ┤ ╭─╯     ╰─╮     │ │
│                     │ │     └─────────────     │ │
│                     │ └───────────────────────┘ │
│                     │                          │
│                     │ [📊 Analytics] [💪 Log]  │
└─────────────────────┴──────────────────────────┘
       40%                      60%
```

**Mobile (Stacked):**
- Master view = full screen list
- Tap item → Navigate to detail view
- Swipe right to go back

---

## Core Screen Designs

### 1. Dashboard (Home Screen)

**Mobile Layout:**
```
┌────────────────────────┐
│ ⚡ MyWorkoutLog    [⚙️] │
├────────────────────────┤
│                        │
│ ┌──────────────────┐   │
│ │ 💪 QUICK STATS   │   │ ← Pixel art icon
│ ├──────────────────┤   │
│ │ Workouts: 24     │   │
│ │ PRs:      12     │   │
│ │ Streak:   8 days │   │
│ └──────────────────┘   │
│                        │
│ ┌──────────────────┐   │
│ │ 🗓️ NEXT SESSION  │   │
│ ├──────────────────┤   │
│ │ Fri • Leg Day    │   │
│ │ Week 2, Day 3    │   │
│ │ [START WORKOUT]  │   │ ← Large touch target
│ └──────────────────┘   │
│                        │
│ ┌──────────────────┐   │
│ │ 📈 CYCLE PROGRESS│   │
│ ├──────────────────┤   │
│ │ Strength Phase   │   │
│ │ ▓▓▓▓▓▓░░░░ 60%   │   │ ← Retro progress bar
│ │ 9/15 sessions    │   │
│ └──────────────────┘   │
│                        │
│ [View Full History]    │
│                        │
└────────────────────────┘
```

**Information-Dense Mode (Swipe down to expand):**
```
┌────────────────────────┐
│ STATS (LAST 4 WEEKS)   │
├────────────────────────┤
│ Volume:  482 sets (+8%)│
│ PRs:     12 new        │
│ Avg RIR: 1.8           │
│ Top:     Pull-ups (24) │
├────────────────────────┤
│ ACTIVE CYCLE           │
├────────────────────────┤
│ Strength - Week 2/4    │
│ ✓✓✓✓✓✓✓✓✓○○○ 9/15     │
│ Next deload: 6 days    │
└────────────────────────┘
```

---

### 2. Workout Logger (Primary Screen - Mobile Optimized)

**Active Workout View:**
```
┌────────────────────────┐
│ ⏱️ 45:32  [FINISH]     │ ← Timer + end button
├────────────────────────┤
│ BENCH PRESS         [▸]│ ← Expand/collapse
├────────────────────────┤
│ [64px pixel art icon]  │ ← Exercise sprite
│                        │
│ Set 1: 100kg × 8 @2rir │ ← Completed sets (gray)
│ Set 2: 100kg × 8 @2rir │
│ ─────────────────────  │
│ Set 3: [LOGGING]       │ ← Active set (highlighted)
│                        │
│ ┌─ WEIGHT ──────────┐  │
│ │  [-5] 100kg [+5]  │  │ ← Swipe up/down or tap
│ └───────────────────┘  │
│                        │
│ ┌─ REPS ────────────┐  │
│ │  [-1]  8   [+1]   │  │
│ └───────────────────┘  │
│                        │
│ ┌─ RIR ─────────────┐  │
│ │ 0  1 [2] 3  4  5  │  │ ← Tap to select
│ └───────────────────┘  │
│                        │
│ [TAP TO LOG SET] ────  │ ← Large button (haptic on tap)
│                        │
├────────────────────────┤
│ 💬 Notes  ⏱️ Rest  📹  │ ← Optional fields (expand)
└────────────────────────┘
```

**Number Pad Overlay (Tap weight/reps):**
```
┌────────────────────────┐
│    ENTER WEIGHT        │
│                        │
│    [100] kg            │ ← Current value
│                        │
│  ┌──────────────────┐  │
│  │  [7] [8] [9]     │  │
│  │  [4] [5] [6]     │  │ ← Retro calculator style
│  │  [1] [2] [3]     │  │
│  │  [←] [0] [✓]     │  │
│  └──────────────────┘  │
│                        │
│  [CANCEL]              │
└────────────────────────┘
```

**Desktop View (Keyboard Input):**
```
> log bench-press
Bench Press loaded (Last: 100kg × 8,8,7)

> 100 8 2
✓ Set 1: 100kg × 8 @ RIR 2

> 100 8 3
✓ Set 2: 100kg × 8 @ RIR 3

> 100 7
✓ Set 3: 100kg × 7 @ RIR 4 (auto-calculated)

> next
→ Squat loaded
```

---

### 3. Mesocycle Management (File System Tree)

**Mobile View:**
```
┌────────────────────────┐
│ 📁 MESOCYCLES          │
├────────────────────────┤
│                        │
│ ▾ 2025-01-strength     │ ← Tap to expand/collapse
│   ▸ ✓ week-01/         │
│   ▾ → week-02/         │ ← Current week
│     ✓ mon-push         │
│     ✓ wed-pull         │
│     ○ fri-legs         │ ← Next workout
│   ▸ ○ week-03/         │
│   ▸ ○ week-04/         │
│                        │
│ ▸ 2024-12-hypertrophy  │
│                        │
│ [+ NEW CYCLE]          │
│                        │
└────────────────────────┘
```

**Tap "fri-legs" to view session:**
```
┌────────────────────────┐
│ ◀ Back   FRI • LEGS    │
├────────────────────────┤
│ Week 2, Day 3          │
│ Strength Phase         │
│                        │
│ ┌──────────────────┐   │
│ │ 🦵 Squat         │   │
│ │ 5 × 5 @ 80%      │   │
│ │ Target: 120kg    │   │
│ └──────────────────┘   │
│                        │
│ ┌──────────────────┐   │
│ │ 🦵 RDL           │   │
│ │ 4 × 8 @ RPE 7    │   │
│ │ Target: 100kg    │   │
│ └──────────────────┘   │
│                        │
│ ┌──────────────────┐   │
│ │ 🦵 Leg Press     │   │
│ │ 3 × 12-15        │   │
│ │ Target: Fatigue  │   │
│ └──────────────────┘   │
│                        │
│ [START WORKOUT] ────   │
│                        │
└────────────────────────┘
```

**Desktop View (Command-line style):**
```
~/mesocycles/2025-01-strength/week-02/

> ls
mon-push.workout  (✓ completed 2025-01-27)
wed-pull.workout  (✓ completed 2025-01-29)
fri-legs.workout  (○ scheduled 2025-01-31)

> cat fri-legs.workout
┌─────────────────────────────────┐
│ FRIDAY • LEG DAY                │
│ Week 2, Day 3 - Strength Phase  │
├─────────────────────────────────┤
│ 1. Squat          5×5  @ 80%    │
│ 2. RDL            4×8  @ RPE 7  │
│ 3. Leg Press      3×12-15       │
│ 4. Calf Raises    3×15-20       │
└─────────────────────────────────┘

> start fri-legs.workout
→ Workout started (timer running)
→ Loaded Squat (Target: 5×5 @ 120kg)
```

---

### 4. Analytics Screen (Information-Dense)

**Mobile View:**
```
┌────────────────────────┐
│ 📊 ANALYTICS      [⚙️] │
├────────────────────────┤
│                        │
│ [Exercise ▼] [12W ▼]  │ ← Filters
│                        │
│ ┌─ BENCH PRESS ──────┐ │
│ │                    │ │
│ │ e1RM PROGRESSION   │ │
│ │ 130 ┤         ●    │ │
│ │ 120 ┤      ●─╯     │ │
│ │ 110 ┤   ●─╯        │ │
│ │ 100 ┤─●╯           │ │
│ │     └──────────    │ │
│ │     W1 W6 W12      │ │
│ └────────────────────┘ │
│                        │
│ ┌─ VOLUME ───────────┐ │
│ │ ▓▓▓▓▓▓▓▓▓▓▓░ 92%   │ │ ← Retro bar
│ │ 46 sets (↑ 8%)     │ │
│ └────────────────────┘ │
│                        │
│ ┌─ RECENT WORKOUTS ──┐ │
│ │ 2025-01-30  8×100  │ │
│ │ 2025-01-27  8×95   │ │
│ │ 2025-01-24  8×95   │ │
│ └────────────────────┘ │
│                        │
│ ┌─ PERSONAL RECORDS ─┐ │
│ │ 1RM:  120kg        │ │
│ │ e1RM: 125kg 🏆NEW  │ │ ← Pixel art trophy
│ │ Vol:  3840kg       │ │
│ └────────────────────┘ │
│                        │
└────────────────────────┘
```

**Swipe right for muscle group view:**
```
┌────────────────────────┐
│ 📊 MUSCLE GROUPS       │
├────────────────────────┤
│                        │
│ Chest   ▓▓▓▓▓▓░░ 48    │
│ Back    ▓▓▓▓▓▓▓░ 52    │
│ Legs    ▓▓▓▓▓▓▓▓ 64    │ ← Tap for detail
│ Arms    ▓▓▓░░░░░ 28    │
│ Shoulders ▓▓▓▓░░ 36    │
│                        │
│ ┌─ DISTRIBUTION ─────┐ │
│ │    Legs 28%        │ │
│ │   ●                │ │
│ │  ● ●  Back 23%     │ │
│ │ ●   ●              │ │
│ │       Chest 21%    │ │
│ └────────────────────┘ │
│                        │
└────────────────────────┘
```

---

### 5. Exercise Library

**Master-Detail (Desktop):**
```
┌──────────────┬─────────────────────────────┐
│ EXERCISES    │ BENCH PRESS                 │
│              │                             │
│ 🔍 Search... │ [64px pixel art animation]  │
│              │                             │
│ ☰ All        │ Category: Weighted          │
│ 💪 Weighted  │ Muscles:  Chest, Triceps    │
│ 🤸 Skill     │ Equipment: Barbell, Bench   │
│ 🏃 Endurance │                             │
│              │ Last: 100kg × 8 (2025-01-30)│
│ █ Bench Press│ PR:   120kg (2025-01-15)    │
│   Squat      │                             │
│   Deadlift   │ 📝 Notes:                   │
│   Pull-up    │ Focus on pause at bottom    │
│   Dip        │                             │
│   ...        │ 🎥 Form Reference:          │
│              │ [Video thumbnail]           │
│ [+] ADD      │                             │
│              │ [📊 Analytics] [✏️ Edit]    │
└──────────────┴─────────────────────────────┘
```

**Mobile List:**
```
┌────────────────────────┐
│ EXERCISES         [+]  │
├────────────────────────┤
│ 🔍 Search...           │
│                        │
│ ☰ All  💪 ⚖️  🤸 Skill │ ← Filter chips
│                        │
│ ┌──────────────────┐   │
│ │ [🏋️] Bench Press │   │ ← Pixel art icon
│ │ Chest • 120kg PR │   │
│ └──────────────────┘   │
│                        │
│ ┌──────────────────┐   │
│ │ [🦵] Squat       │   │
│ │ Legs • 140kg PR  │   │
│ └──────────────────┘   │
│                        │
│ ┌──────────────────┐   │
│ │ [🤸] Pull-up     │   │
│ │ Back • BW+20kg   │   │
│ └──────────────────┘   │
│                        │
└────────────────────────┘
```

---

## Component Library

### Core UI Components (shadcn/ui + Custom Styling)

**1. RetroCard**
```tsx
<RetroCard variant="primary" scanlines>
  <CardHeader>
    <PixelIcon name="bench-press" size={64} />
    <CardTitle>Bench Press</CardTitle>
  </CardHeader>
  <CardContent>
    Last: 100kg × 8
  </CardContent>
</RetroCard>

// Styles:
// - Background: Dracula --background
// - Border: 2px solid --border-color
// - Optional scanlines overlay (CSS gradient)
// - Subtle box-shadow with pixel art edge
```

**2. TerminalButton**
```tsx
<TerminalButton
  variant="primary"
  haptic="medium"
  onClick={logSet}
>
  [LOG SET]
</TerminalButton>

// Styles:
// - Monospace font
// - Square brackets around text
// - Hover: slight glow (box-shadow)
// - Active: haptic feedback on mobile
// - Min height: 44px (touch target)
```

**3. RetroProgressBar**
```tsx
<RetroProgressBar
  value={60}
  max={100}
  variant="volume"
  showLabel
/>

// Renders:
// ▓▓▓▓▓▓░░░░ 60%
// - Filled: --green or --purple
// - Empty: --comment (gray)
// - Pixel art style blocks
```

**4. NumberPadOverlay**
```tsx
<NumberPadOverlay
  label="Enter Weight"
  unit="kg"
  initialValue={100}
  onSubmit={(value) => setWeight(value)}
  onCancel={() => setShowPad(false)}
/>

// Features:
// - Calculator-style grid
// - Large touch targets (56px buttons)
// - Haptic feedback on tap
// - Retro button styling
// - Smooth slide-up animation
```

**5. PixelIcon (Animated)**
```tsx
<PixelIcon
  name="bench-press"
  size={64}
  animated={true}
  fps={8}
/>

// Loads pixel art sprite sheet
// Plays 4-8 frame animation on loop
// Fallback to emoji if sprite not found
```

**6. CommandInput (Desktop)**
```tsx
<CommandInput
  placeholder="Type command..."
  onCommand={(cmd) => handleCommand(cmd)}
  autocomplete={exerciseNames}
/>

// Features:
// - Fuzzy search autocomplete
// - Command history (↑/↓ arrows)
// - Syntax highlighting
// - Kbd shortcuts shown
```

**7. FileTreeView**
```tsx
<FileTreeView
  data={mesocycleTree}
  onSelect={(node) => navigate(node.path)}
  expandedKeys={['2025-01-strength', 'week-02']}
/>

// Renders file system tree
// - Folders: ▸/▾ chevrons
// - Files: Icons (✓/○/→)
// - Indent: 16px per level
// - Tap to expand/navigate
```

---

## Technical Implementation

### Tech Stack (Updated)

| Layer | Technology | Justification |
|-------|-----------|---------------|
| **Framework** | Next.js 14 (App Router) | User knows it, SSR for PWA, React Server Components |
| **Language** | TypeScript | Type safety |
| **Styling** | Tailwind CSS | User knows it, rapid styling |
| **UI Base** | shadcn/ui | Accessible components, heavily customizable |
| **Custom Components** | Custom retro components | Built on shadcn/ui, styled for terminal aesthetic |
| **Database** | Dexie.js (IndexedDB) | Client-side storage, offline-first |
| **State** | Zustand | Simple, less boilerplate than Redux |
| **Data Fetching** | TanStack Query | Caching, optimistic updates |
| **Charts** | Recharts | React-native, customizable for retro style |
| **Pixel Art** | PNG sprites + CSS animations | 64x64 icons, GIF for animations |
| **PWA** | next-pwa | Service workers, offline, install |
| **Haptics** | navigator.vibrate() | Mobile haptic feedback |
| **Themes** | CSS variables + localStorage | Theme switcher (Dracula, Gruvbox, etc.) |
| **Gestures** | react-swipeable | Swipe navigation on mobile |
| **Keyboard** | react-hotkeys-hook | Desktop shortcuts |

### Font & Icon Resources

**Fonts:**
```bash
# Install monospace fonts
npm install @fontsource/jetbrains-mono
npm install @fontsource/fira-code

# In layout.tsx
import '@fontsource/jetbrains-mono/400.css';
import '@fontsource/jetbrains-mono/700.css';
```

**Pixel Art Resources:**
```
Free Sprite Libraries:
- OpenGameArt.org (search "fitness", "sports", "exercise")
- Kenney.nl (sports pack, UI pack)
- itch.io/game-assets (pixel art section)
- Pixabay (pixel art category)

Generate Custom:
- Aseprite (pixel art editor - $20 one-time)
- Piskel (free online pixel art tool)
- Lospec (pixel art community, resources)

AI-Generated:
- DALL-E 3: "pixel art icon of [exercise], 64x64, transparent background, Dracula color palette"
- Stable Diffusion: pixel-art-xl model
```

**Icons:**
```bash
# Lucide React for UI icons
npm install lucide-react

# Use for non-exercise icons (settings, navigation, etc.)
import { Settings, ChevronRight, Play } from 'lucide-react';
```

### Theme Switching Implementation

```typescript
// lib/themes.ts
export const themes = {
  dracula: {
    name: 'Dracula',
    colors: {
      bg: '#282a36',
      fg: '#f8f8f2',
      primary: '#bd93f9',
      secondary: '#ff79c6',
      accent: '#8be9fd',
      success: '#50fa7b',
      warning: '#ffb86c',
      error: '#ff5555'
    }
  },
  gruvbox: {
    name: 'Gruvbox Dark',
    colors: {
      bg: '#282828',
      fg: '#ebdbb2',
      primary: '#fe8019',
      secondary: '#fabd2f',
      accent: '#83a598',
      success: '#b8bb26',
      warning: '#fabd2f',
      error: '#fb4934'
    }
  },
  // ... more themes
};

// hooks/useTheme.ts
export function useTheme() {
  const [theme, setTheme] = useState<keyof typeof themes>('dracula');

  useEffect(() => {
    const root = document.documentElement;
    const colors = themes[theme].colors;

    Object.entries(colors).forEach(([key, value]) => {
      root.style.setProperty(`--color-${key}`, value);
    });

    localStorage.setItem('theme', theme);
  }, [theme]);

  return { theme, setTheme, themes };
}
```

### Haptic Feedback (Mobile)

```typescript
// lib/haptics.ts
export const haptic = {
  light: () => {
    if ('vibrate' in navigator) {
      navigator.vibrate(10);
    }
  },
  medium: () => {
    if ('vibrate' in navigator) {
      navigator.vibrate(20);
    }
  },
  heavy: () => {
    if ('vibrate' in navigator) {
      navigator.vibrate([30, 10, 30]);
    }
  },
  success: () => {
    if ('vibrate' in navigator) {
      navigator.vibrate([10, 10, 10]);
    }
  },
  error: () => {
    if ('vibrate' in navigator) {
      navigator.vibrate([50, 30, 50]);
    }
  }
};

// Usage in component
import { haptic } from '@/lib/haptics';

<TerminalButton onClick={() => {
  logSet();
  haptic.medium();
}}>
  [LOG SET]
</TerminalButton>
```

### Gesture Support (Mobile)

```typescript
// hooks/useSwipeGesture.ts
import { useSwipeable } from 'react-swipeable';

export function useSwipeNavigation() {
  const router = useRouter();

  const handlers = useSwipeable({
    onSwipedLeft: () => {
      // Next tab/screen
      haptic.light();
    },
    onSwipedRight: () => {
      // Previous tab/screen or back
      haptic.light();
      router.back();
    },
    trackMouse: false,
    preventScrollOnSwipe: false
  });

  return handlers;
}

// Usage in layout
<div {...useSwipeNavigation()}>
  {children}
</div>
```

### Keyboard Shortcuts (Desktop)

```typescript
// hooks/useKeyboardShortcuts.ts
import { useHotkeys } from 'react-hotkeys-hook';

export function useWorkoutShortcuts() {
  const router = useRouter();

  // Cmd/Ctrl + K: Command palette
  useHotkeys('mod+k', (e) => {
    e.preventDefault();
    openCommandPalette();
  });

  // L: Log workout
  useHotkeys('l', () => router.push('/workout/log'));

  // S: Stats
  useHotkeys('s', () => router.push('/analytics'));

  // /: Search
  useHotkeys('/', (e) => {
    e.preventDefault();
    focusSearch();
  });

  // Escape: Close dialogs
  useHotkeys('esc', () => closeAllDialogs());
}
```

---

## Mobile-First Workflow

### Primary Use Case: Logging Workout at Gym

**User Flow (Mobile):**
1. **Open app** → Dashboard shows "Next Session: Leg Day"
2. **Tap "START WORKOUT"** → Timer starts, loads session template
3. **Exercise auto-loaded** → Squat (5×5 @ 120kg target shown)
4. **Log Set 1:**
   - Weight pre-filled: 120kg (swipe up/down to adjust ±5kg)
   - Reps pre-filled: 5 (tap to open number pad if major change)
   - RIR: Tap "2"
   - Tap "[LOG SET]" → Haptic feedback, set saved, rest timer starts
5. **Rest timer counts down** → Shows "Rest: 3:00" (swipe to skip)
6. **Repeat** for Sets 2-5
7. **Next exercise auto-loaded** → RDL (4×8)
8. **Continue** until all exercises complete
9. **Tap "FINISH WORKOUT"** → Summary shown, timer stopped, saved

**Time per set**: ~10 seconds (vs ~30s with keyboard popup)

### Secondary Use Case: Planning Mesocycle (Desktop)

**User Flow (Desktop):**
1. **Open app on laptop** → Keyboard navigation primary
2. **Type `Cmd+K`** → Command palette opens
3. **Type "new cycle"** → Creates mesocycle template
4. **Fill form:**
   - Name: "Strength Phase 2025-02"
   - Weeks: 4
   - Sessions per week: 3 (PPL)
5. **For each session, type exercises:**
   ```
   > add exercise bench-press
   > sets 5x5 @ 80%
   > add exercise incline-db-press
   > sets 4x8 @ RPE 7
   ```
6. **Tab through** sessions, copy/paste from Week 1 → Week 2 with adjustments
7. **Save template** → Ready to start on phone

---

## Migration Plan (Revised for Retro UI)

### Phase 1: Core PWA + Retro UI Foundation (3-4 weeks)

**Week 1: Setup + Design System**
- [ ] Initialize Next.js 14 project with TypeScript
- [ ] Configure Tailwind CSS with Dracula theme
- [ ] Install shadcn/ui base components
- [ ] Create theme switcher (Dracula, Gruvbox, Nord, etc.)
- [ ] Build core retro components:
  - [ ] RetroCard
  - [ ] TerminalButton
  - [ ] RetroProgressBar
  - [ ] PixelIcon (with placeholder emojis)
- [ ] Set up monospace fonts (JetBrains Mono)
- [ ] Implement haptic feedback utilities
- [ ] Test on Z Fold 6 browser

**Week 2: Exercise Library + Number Pad**
- [ ] Set up Dexie.js database
- [ ] Create Exercise entity and CRUD
- [ ] Build exercise library UI (list + detail)
- [ ] Implement NumberPadOverlay component
- [ ] Add swipe gestures for navigation
- [ ] Import exercises from Android export
- [ ] Find/generate 10-20 pixel art exercise icons
- [ ] Test touch interactions on phone

**Week 3: Workout Logger (Mobile-First)**
- [ ] Create Workout and LoggedSet entities
- [ ] Build workout logger UI with hybrid input
- [ ] Implement basic timer (non-persistent yet)
- [ ] Add rest time tracking with countdown
- [ ] Swipe gestures for set navigation
- [ ] Haptic feedback on set log
- [ ] Test full workout logging flow on phone

**Week 4: History + Master-Detail**
- [ ] Build workout history list
- [ ] Create workout detail view
- [ ] Implement tmux-style master-detail layout
- [ ] Add edit/delete functionality
- [ ] Responsive breakpoints (mobile/Z Fold/desktop)
- [ ] Performance optimization (virtual scrolling)
- [ ] Polish animations and transitions

**Phase 1 Success Criteria:**
- ✅ Log workout on phone faster than spreadsheet
- ✅ Retro aesthetic looks cool and functional
- ✅ No keyboard popup spam on mobile
- ✅ Works offline
- ✅ Dracula theme + 2 alternative themes working

---

### Phase 2: Mesocycle Management (2-3 weeks)

**Week 5: Program Templates + File Tree**
- [ ] Create ProgramTemplate and SessionTemplate entities
- [ ] Build file tree navigation component
- [ ] Program creation UI (desktop optimized)
- [ ] Session template editor
- [ ] Exercise selection with autocomplete
- [ ] Set/rep target inputs

**Week 6: Active Cycles + Session Planning**
- [ ] ActiveCycle entity and state management
- [ ] Start mesocycle flow
- [ ] Weekly session calendar/tree view
- [ ] Auto-populate workout logger from template
- [ ] Session completion tracking
- [ ] Pixel art icons for workout status (✓/○/→)

**Week 7: Flexibility + Cross-Cycle**
- [ ] Exercise substitution mid-cycle
- [ ] Override set/rep targets per session
- [ ] Session notes and adjustments
- [ ] Mesocycle comparison view
- [ ] Export mesocycle data to spreadsheet

**Phase 2 Success Criteria:**
- ✅ Create 4-week mesocycle in <10 minutes
- ✅ Start cycle and see weekly plan
- ✅ Log workout from template
- ✅ Swap exercises mid-cycle
- ✅ File tree navigation intuitive on mobile

---

### Phase 3: Analytics + Retro Visualizations (2 weeks)

**Week 8: PR Tracking + Retro Charts**
- [ ] PR detection algorithm (port from Android)
- [ ] e1RM calculations
- [ ] Personal records list
- [ ] Restyle Recharts with retro aesthetic:
  - [ ] Dracula color palette
  - [ ] Monospace labels
  - [ ] Pixelated line/bar charts (optional)
- [ ] Volume chart (retro progress bars)
- [ ] Strength progression chart
- [ ] Pixel art celebration animation for new PRs

**Week 9: Cloud Sync + Export**
- [ ] Google OAuth 2.0 integration
- [ ] Encrypt database export (AES-256-GCM)
- [ ] Upload to Google Drive
- [ ] Download and restore from backup
- [ ] Auto-sync on workout completion (debounced)
- [ ] CSV/JSON export for spreadsheets

**Phase 3 Success Criteria:**
- ✅ PR detection works automatically
- ✅ Charts look retro and readable
- ✅ New PR triggers pixel art animation + haptic
- ✅ Google Drive backup works
- ✅ Export to CSV opens correctly in Excel

---

### Phase 4: PWA Features + Polish (1-2 weeks)

**Week 10: PWA + Offline**
- [ ] Configure next-pwa
- [ ] App manifest with pixel art icons
- [ ] Install prompt UI (retro styled)
- [ ] Service Worker caching
- [ ] Offline indicator (terminal-style message)
- [ ] Background sync for workouts

**Week 11: Persistent Timer + Final Polish**
- [ ] Web Worker timer (survives navigation)
- [ ] localStorage timer persistence
- [ ] Timer survives app close/reopen
- [ ] Command palette (desktop)
- [ ] Keyboard shortcuts
- [ ] CRT scanline effects (toggleable)
- [ ] Final Z Fold 6 testing
- [ ] Performance audit
- [ ] Accessibility audit (contrast, touch targets)

**Phase 4 Success Criteria:**
- ✅ PWA installs on phone and desktop
- ✅ Timer persists across navigation
- ✅ Works fully offline
- ✅ Desktop keyboard shortcuts work
- ✅ Retro aesthetic polished and consistent

---

### Phase 5 (Future): Advanced Features (Optional)

**Video Analysis + ML:**
- [ ] Capacitor Camera plugin for video capture
- [ ] TensorFlow.js integration
- [ ] MediaPipe pose estimation
- [ ] Analyze hold duration (front lever, plank)
- [ ] Rep counting
- [ ] Form analysis

**Enhanced Retro Features:**
- [ ] More pixel art sprites (100+ exercises)
- [ ] Animated transformations (Kamen Rider style)
- [ ] Achievement system (8-bit badges)
- [ ] Leaderboard (compete with yourself over time)
- [ ] Easter eggs (Konami code unlocks retro game)

---

## Pixel Art Assets Plan

### Initial Set (Phase 1 - 20 exercises)

**Weighted Exercises:**
- Bench Press (barbell)
- Squat (barbell)
- Deadlift (barbell)
- Overhead Press (barbell)
- Barbell Row

**Bodyweight/Skill:**
- Pull-up
- Dip
- Push-up
- Handstand
- Front Lever

**Endurance:**
- Running
- Cycling
- Rowing
- Jumping Rope
- Burpees

**Calisthenics Specific:**
- Muscle-up
- L-sit
- Pistol Squat
- Planche (progression)
- Back Lever

**Sources:**
1. **Search OpenGameArt.org**: "fitness", "gym", "sports"
2. **Kenney.nl Sports Pack**: Free pixel art pack
3. **Commission Fiverr**: ~$5-10 per sprite (64x64)
4. **AI Generate**: DALL-E 3 with prompt:
   ```
   "pixel art icon of person doing [exercise], 64x64 pixels,
   transparent background, purple and cyan color scheme,
   8-bit retro game style, simple and recognizable"
   ```
5. **Create in Piskel**: If you want to learn (1-2 hours per sprite)

### Expansion (Phase 2+ - 100+ exercises)

- Use Aseprite for batch creation
- Consistent style guide (64x64, 8 colors max, Dracula palette)
- Animated versions (4 frames showing exercise motion)
- Community contributions (open-source sprite library)

---

## Kamen Rider Zeztz Inspiration

Based on the [Kamen Rider Zeztz pixel art transformations](https://tenor.com/view/zeztz-build-pixels-legend-riders-kamen-rider-gif-12169599617546178411), here's how we'll implement PR celebration animations:

### PR Animation Sequence

```typescript
// components/PRCelebration.tsx
export function PRCelebration({ exercise, newPR, oldPR }) {
  return (
    <motion.div
      initial={{ scale: 0, rotate: 0 }}
      animate={{ scale: 1, rotate: 360 }}
      transition={{ duration: 0.5 }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80"
    >
      {/* Pixel art transformation effect */}
      <div className="relative">
        {/* Exercise sprite */}
        <PixelIcon name={exercise} size={128} animated />

        {/* "NEW PR!" text in pixel font */}
        <div className="pixel-font text-4xl text-yellow animate-pulse">
          NEW PR!
        </div>

        {/* PR values */}
        <div className="text-center mt-4">
          <div className="text-gray line-through">{oldPR}kg</div>
          <div className="text-green text-3xl">{newPR}kg</div>
        </div>

        {/* Particle effects (optional) */}
        <PixelParticles count={20} color="yellow" />
      </div>
    </motion.div>
  );
}

// Triggered on new PR detection
useEffect(() => {
  if (newPRDetected) {
    haptic.success();
    showPRCelebration();
    setTimeout(hidePRCelebration, 3000);
  }
}, [newPRDetected]);
```

**Animation Style:**
- Duration: 2-3 seconds
- Pixel art sprite scales up with rotation
- "NEW PR!" text flashes (yellow/white alternating)
- Old PR → New PR number transition
- Optional: Pixel particles burst outward
- Haptic: Triple vibration pattern on mobile
- Auto-dismiss after 3 seconds or tap to close

---

## Accessibility Considerations

Despite the retro aesthetic, maintain accessibility:

**Color Contrast:**
- All text must meet WCAG AA standards (4.5:1 ratio)
- Dracula theme already has good contrast
- Test each theme with contrast checker

**Touch Targets:**
- Minimum 44x44px for all interactive elements
- Adequate spacing between buttons (8px min)
- Number pad buttons: 56px for easier tapping

**Typography:**
- Minimum font size: 14px (--text-sm)
- Line height: 1.5 for readability
- Monospace fonts can be harder to read - use sparingly

**Keyboard Navigation:**
- All interactive elements focusable
- Visible focus indicators (purple outline)
- Logical tab order
- Escape key closes dialogs

**Screen Readers:**
- Semantic HTML (button, nav, main, etc.)
- ARIA labels for icon-only buttons
- Alt text for pixel art (describe exercise)
- Status announcements for set logging

**Motion Sensitivity:**
- Respect `prefers-reduced-motion`
- Disable scanlines/CRT effects if requested
- Skip animations, instant state changes

---

## Performance Optimization

**Mobile-First Performance:**
- Lazy load routes (Next.js dynamic imports)
- Virtual scrolling for long lists (react-window)
- Debounce search input (300ms)
- Optimize pixel art sprites:
  - PNG compressed (TinyPNG)
  - WebP format with PNG fallback
  - Lazy load exercise sprites
  - Preload current workout sprites only

**PWA Optimization:**
- Cache exercise library and sprites
- Background sync for cloud backup
- Precache critical routes
- Runtime cache for API requests

**Render Performance:**
- Memoize expensive components (React.memo)
- Use TanStack Query for smart caching
- Virtualize workout history (100+ workouts)
- Debounce chart re-renders

---

## Testing Strategy

**Manual Testing (Phase 1):**
- [ ] Test on actual Z Fold 6 (folded + unfolded)
- [ ] Test on desktop browser (Chrome, Firefox)
- [ ] Test offline functionality
- [ ] Test touch gestures (swipe, tap, long-press)
- [ ] Test keyboard shortcuts (desktop)
- [ ] Test theme switching
- [ ] Test haptic feedback
- [ ] Test number pad input accuracy

**Performance Testing:**
- [ ] Lighthouse PWA audit (target: 90+ score)
- [ ] Test with 500+ workouts (performance)
- [ ] Test with 100+ exercises (search speed)
- [ ] Monitor IndexedDB size growth
- [ ] Check bundle size (<500KB initial)

**Accessibility Testing:**
- [ ] Keyboard-only navigation
- [ ] Screen reader testing (iOS VoiceOver)
- [ ] Color contrast validation
- [ ] Touch target size validation
- [ ] `prefers-reduced-motion` testing

**Cross-Browser Testing:**
- [ ] Chrome (primary)
- [ ] Samsung Internet (Z Fold 6 default)
- [ ] Firefox
- [ ] Safari (iOS if available)

---

## Deployment & Hosting

**Recommended: Vercel (Free Tier)**
- Next.js native support
- Automatic PWA generation
- Edge functions for API routes (if needed)
- Free SSL certificate
- Automatic deployments from Git

**Alternative: Netlify**
- Similar features to Vercel
- Good PWA support
- Free tier generous

**Self-Hosted (If preferred):**
- Docker container with Next.js
- Nginx reverse proxy
- Let's Encrypt SSL
- Deploy to VPS or home server

---

## Data Migration from Android

### Export from Android App

```kotlin
// One-time export script
fun exportAllData() {
    val exercises = exerciseDao.getAllExercises()
    val workouts = workoutDao.getAllWorkouts()
    val sets = setDao.getAllSets()

    val exportData = ExportData(
        exercises = exercises,
        workouts = workouts,
        sets = sets,
        exportDate = System.currentTimeMillis()
    )

    val json = gson.toJson(exportData)
    File(getExternalFilesDir(null), "export.json").writeText(json)
}
```

### Import to PWA

```typescript
// lib/migration.ts
import { db } from './db';

export async function importFromAndroid(jsonFile: File) {
  const data = JSON.parse(await jsonFile.text());

  // Import exercises
  await db.exercises.bulkAdd(data.exercises.map(e => ({
    name: e.name,
    category: e.category,
    muscleGroups: e.muscleGroups,
    equipment: e.equipment,
    notes: e.notes
  })));

  // Import workouts
  await db.workouts.bulkAdd(data.workouts.map(w => ({
    date: new Date(w.date),
    startTimestamp: w.startTimestamp,
    endTimestamp: w.endTimestamp,
    notes: w.notes
  })));

  // Import sets
  await db.sets.bulkAdd(data.sets.map(s => ({
    workoutId: s.workoutId,
    exerciseId: s.exerciseId,
    setNumber: s.setNumber,
    reps: s.reps,
    weight: s.weight,
    duration: s.duration,
    rir: s.rir
  })));

  console.log('Migration complete!');
}
```

---

## Success Metrics (Revised)

### Phase 1: Core PWA + Retro UI
- [ ] Log workout on mobile in <2 minutes (faster than spreadsheet)
- [ ] No keyboard popup during set logging (hybrid input works)
- [ ] Retro aesthetic looks professional and cohesive
- [ ] Haptic feedback feels responsive
- [ ] Works offline without errors
- [ ] Loads in <2 seconds on 4G

### Phase 2: Mesocycle Management
- [ ] Create mesocycle template in <10 minutes
- [ ] File tree navigation intuitive (no confusion)
- [ ] Start workout from template in <3 taps
- [ ] Exercise swapping works mid-cycle
- [ ] View cross-cycle progress clearly

### Phase 3: Analytics + Cloud
- [ ] PR detection matches Android logic (100% accuracy)
- [ ] Charts render in <1 second
- [ ] New PR animation triggers correctly
- [ ] Google Drive backup <10 seconds
- [ ] CSV export opens in Excel without errors

### Phase 4: PWA Polish
- [ ] PWA installs without issues
- [ ] Timer survives app close/reopen
- [ ] Keyboard shortcuts work (desktop)
- [ ] Theme switching instant (<100ms)
- [ ] Lighthouse PWA score >90

### Overall Success
- [ ] **App replaces spreadsheet 100%**
- [ ] **Mesocycle planning easier than any commercial app**
- [ ] **Retro UI is unique and delightful**
- [ ] **Mobile workout logging is effortless**
- [ ] **User enjoys opening the app (dopamine hit from design)**

---

## Risk Assessment (Retro UI Specific)

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Retro UI too busy/distracting** | Medium | Medium | Start minimal, add flourishes incrementally; user testing |
| **Pixel art delays development** | Low | Low | Use placeholders (emojis) initially, add sprites Phase 2+ |
| **Terminal aesthetic hurts usability** | Low | Medium | Prioritize UX over aesthetics; A/B test with simpler UI |
| **Haptic feedback annoying** | Low | Low | Make toggleable in settings from day 1 |
| **Performance issues with animations** | Low | Low | Use CSS animations (GPU-accelerated), test on older devices |
| **Accessibility compromised** | Medium | High | Follow WCAG guidelines, contrast checker, keyboard nav |
| **Theme switching buggy** | Low | Low | Use CSS variables, test thoroughly before shipping |

---

## Next Steps

1. **✅ Approve Retro UI Direction**
   - Review this spec
   - Confirm design direction
   - Suggest changes/additions

2. **🚀 Set Up Development Environment**
   - Install Node.js, VS Code
   - Initialize Next.js project
   - Configure Tailwind + shadcn/ui
   - Set up Git repository

3. **🎨 Design Validation**
   - Create mockup in Figma/Excalidraw (optional)
   - Find 5-10 pixel art sprites to test aesthetic
   - Test Dracula theme on Z Fold 6 browser

4. **💻 Start Phase 1 Development**
   - Build theme system first
   - Create core retro components
   - Test on mobile browser immediately
   - Iterate based on feel

5. **📦 Export Android Data**
   - Run export script on current Android app
   - Save exercises.json, workouts.json
   - Ready for import once PWA database built

---

## Conclusion

This **mobile-first PWA with terminal/retro aesthetic** combines:
- ✅ **User's skills** (React, Next.js, TypeScript, Tailwind)
- ✅ **Unique design** (Terminal + 8-bit pixel art, Kamen Rider Zeztz inspiration)
- ✅ **Mobile optimization** (Hybrid input, gestures, haptic feedback)
- ✅ **Desktop power user features** (Keyboard shortcuts, command palette)
- ✅ **Fast development** (shadcn/ui base + custom retro styling)
- ✅ **Information-dense** (Show all relevant data, minimal navigation)

The retro aesthetic differentiates this from every commercial fitness app while maintaining excellent usability through thoughtful UX design (large touch targets, hybrid input, gesture support).

**Estimated Timeline**: 8-12 weeks to feature parity + retro polish

**Risk Level**: Low-Medium (familiar stack, design adds ~2 weeks vs plain UI)

**Recommendation**: ✅ **PROCEED WITH RETRO PWA**

---

**Sources:**
- [Kamen Rider Zeztz Transformation GIF](https://tenor.com/view/zeztz-build-pixels-legend-riders-kamen-rider-gif-12169599617546178411)
- [Kamen Rider Zeztz Wiki](https://kamenrider.fandom.com/wiki/Kamen_Rider_Zeztz)
- [Kamen Rider ZEZTZ Wikipedia](https://en.wikipedia.org/wiki/Kamen_Rider_ZEZTZ)

---

**Document Version**: 2.0 - Retro UI
**Last Updated**: 2025-01-31
**Next Review**: After Phase 1 completion
