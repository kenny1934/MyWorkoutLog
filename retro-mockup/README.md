# MyWorkoutLog - Retro Terminal UI Mockup

A working mockup of the fitness tracking app with terminal/retro aesthetic.

## 🎮 Features Demonstrated

### Design Elements
- **Dracula Theme**: Purple/pink/cyan color scheme on dark background
- **Monospace Typography**: Terminal-style fonts throughout
- **Scanlines Effect**: Subtle CRT monitor aesthetic
- **Pixel Art Progress Bars**: Retro gaming-style visualization
- **Terminal Buttons**: Square-bracketed button style `[BUTTON]`

### Screens Included

#### 1. Dashboard (`/`)
- Quick Stats widget (workouts, PRs, streak)
- Next Session card with workout preview
- Cycle Progress with pixel art progress bar
- Recent Workouts list
- Quick action buttons

#### 2. Workout Logger (`/workout`)
- Live timer display (45:32)
- Exercise header with icon
- Completed sets history
- Interactive set input:
  - Weight stepper (±5kg buttons)
  - Reps stepper (±1 buttons)
  - RIR selector (tap 0-5)
  - Large "LOG SET" button
- Rest timer countdown
- Next exercise navigation

### Components Built

1. **RetroCard** - Card component with optional title/icon, pixel borders
2. **TerminalButton** - Terminal-style buttons with variants (primary, secondary, danger)
3. **RetroProgressBar** - Pixel art progress visualization with percentage

## 🚀 Running the Mockup

The dev server is already running at:
- **Local**: http://localhost:3000
- **Network**: http://21.0.0.206:3000

### Test on Z Fold 6:
1. Open Chrome on your Z Fold 6
2. Navigate to `http://21.0.0.206:3000`
3. Test folded and unfolded views
4. Interact with the workout logger

### Navigate Between Screens:
- Click bottom navigation icons (mobile)
- Or visit directly:
  - `/` - Dashboard
  - `/workout` - Workout Logger

## 📱 Mobile Optimizations

- **Bottom Navigation**: Icon + label for easy thumb access
- **Large Touch Targets**: All buttons minimum 44x44px
- **Stepper Buttons**: No keyboard popup needed for weight/reps
- **RIR Tap Selection**: Quick 0-5 selection without number pad

## 🎨 Theme Details

### Colors (Dracula)
- Background: `#282a36`
- Foreground: `#f8f8f2`
- Purple: `#bd93f9` (primary actions)
- Pink: `#ff79c6` (accents)
- Cyan: `#8be9fd` (secondary)
- Green: `#50fa7b` (success, RIR selected)
- Red: `#ff5555` (danger, finish button)
- Comment: `#6272a4` (borders, muted text)

### Typography
- Font: Monaco, Consolas, Courier New (monospace)
- All text uses monospace for terminal aesthetic

## 🔧 Next Steps

If you like this direction:
1. **Add more screens**: Analytics, Mesocycles (file tree), Exercise library
2. **Pixel art sprites**: Replace emoji with 64x64 pixel art icons
3. **Animations**: PR celebration, Kamen Rider Zeztz-style transformations
4. **Theme switcher**: Add Gruvbox, Nord, Matrix Green options
5. **Gestures**: Swipe navigation, haptic feedback
6. **IndexedDB**: Add actual data persistence
7. **PWA features**: Service workers, offline mode, install prompt

## 📂 File Structure

```
workout-retro-mockup/
├── app/
│   ├── layout.tsx          # Root layout with navigation
│   ├── page.tsx            # Dashboard
│   ├── workout/
│   │   └── page.tsx        # Workout logger
│   └── globals.css         # Tailwind + retro styles
├── components/
│   ├── RetroCard.tsx       # Card component
│   ├── TerminalButton.tsx  # Button component
│   └── RetroProgressBar.tsx # Progress bar
├── tailwind.config.ts      # Dracula theme config
└── package.json

```

## 🎯 Design Decisions

1. **No keyboard popup spam**: Weight/reps use stepper buttons (tap ±5, ±1)
2. **Information-dense**: Show all relevant data without hiding
3. **Mobile-first**: Bottom nav, large touch targets, vertical scrolling
4. **Retro aesthetic**: Scanlines, pixel borders, monospace fonts
5. **Functional**: Interactive workout logger that actually works

## 💡 Try It Out

Open the workout logger (`/workout`):
- Tap -5/+5 to adjust weight
- Tap -1/+1 to adjust reps
- Tap RIR numbers to select
- Click [LOG SET] to add a set
- See completed sets list update

This demonstrates the hybrid input system (no keyboard popup)!

---

**Built with**: Next.js 16, React 19, TypeScript, Tailwind CSS 4
**Theme**: Dracula
**Status**: Working mockup, ready for expansion
