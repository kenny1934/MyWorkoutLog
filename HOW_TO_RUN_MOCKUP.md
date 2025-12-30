# How to Run the Retro Mockup on Your Laptop

## 📥 **1. Get the Code**

On your laptop, checkout the branch:

```bash
git checkout claude/evaluate-app-rewrite-Eqoyq
git pull
```

## 📂 **2. Navigate to Mockup Directory**

```bash
cd retro-mockup
```

## 📦 **3. Install Dependencies**

```bash
npm install
```

This will install:
- Next.js 16
- React 19
- TypeScript
- Tailwind CSS 4
- All other dependencies

## 🚀 **4. Start Dev Server**

```bash
npm run dev
```

You should see:
```
▲ Next.js 16.1.1 (Turbopack)
- Local:   http://localhost:3000
- Network: http://192.168.x.x:3000

✓ Ready in ~2s
```

## 🌐 **5. Open in Browser**

**On your laptop:**
- Open: **http://localhost:3000**

**On your Z Fold 6** (same WiFi network):
- Open: **http://[your-laptop-ip]:3000**
- Find your laptop IP from the "Network" line in terminal

## 📱 **6. Test on Z Fold 6**

1. **Folded mode** (phone size):
   - Bottom navigation should show
   - Cards stack vertically
   - Touch targets should be easy to hit

2. **Unfolded mode** (tablet size):
   - Same layout (will add master-detail later)
   - More breathing room

3. **Test Workout Logger** (`/workout`):
   - Tap ±5 buttons to change weight
   - Tap ±1 buttons to change reps
   - Tap 0-5 for RIR
   - Click [LOG SET]
   - **Notice**: No keyboard popup!

## 🎨 **7. What to Check**

- **Colors**: Is Dracula theme too dark/bright?
- **Fonts**: Is monospace readable?
- **Touch targets**: Are buttons easy to tap?
- **Spacing**: Too cramped or too spacious?
- **Overall feel**: Geeky enough? Too retro?

## 🛑 **8. Stop Server**

Press `Ctrl+C` in terminal when done.

## 🔧 **Troubleshooting**

**Problem**: `npm install` fails
- **Solution**: Make sure you have Node.js 18+ installed
- Check: `node --version`
- Install from: https://nodejs.org/

**Problem**: Can't access from Z Fold 6
- **Solution**: Make sure both devices on same WiFi
- Check firewall isn't blocking port 3000
- Try: `http://192.168.1.x:3000` (replace with your laptop IP)

**Problem**: Page doesn't load
- **Solution**: Wait for "✓ Ready" message before opening browser
- Try refreshing page
- Check terminal for errors

---

## 📝 **Next Steps After Testing**

Let me know:
1. How it feels on Z Fold 6 (folded vs unfolded)
2. Any visual tweaks needed (colors, spacing, fonts)
3. If hybrid input works well (no keyboard spam)
4. What screens you want to see next (analytics, mesocycles file tree, etc.)

**Branch**: `claude/evaluate-app-rewrite-Eqoyq`
**Location**: `retro-mockup/` directory
