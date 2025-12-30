import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'MyWorkoutLog - Retro Terminal UI',
  description: 'Fitness tracking with terminal/retro aesthetic',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="scanlines">
        {/* Top Bar */}
        <header className="border-b-2 border-comment px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-purple text-xl">⚡</span>
            <span className="text-foreground font-bold">MyWorkoutLog</span>
          </div>
          <div className="flex items-center gap-4">
            <button className="text-comment hover:text-purple transition-colors">
              [Theme]
            </button>
            <button className="text-comment hover:text-purple transition-colors">
              ⚙️
            </button>
          </div>
        </header>

        {/* Main Content */}
        <main className="min-h-[calc(100vh-120px)]">
          {children}
        </main>

        {/* Bottom Navigation (Mobile) */}
        <nav className="border-t-2 border-comment px-4 py-2 flex justify-around items-center md:hidden">
          <a href="/" className="flex flex-col items-center gap-1 text-purple">
            <span className="text-xl">🏠</span>
            <span className="text-xs">Home</span>
          </a>
          <a href="/analytics" className="flex flex-col items-center gap-1 text-comment hover:text-purple">
            <span className="text-xl">📊</span>
            <span className="text-xs">Stats</span>
          </a>
          <a href="/mesocycles" className="flex flex-col items-center gap-1 text-comment hover:text-purple">
            <span className="text-xl">🗂️</span>
            <span className="text-xs">Cycles</span>
          </a>
          <a href="/workout" className="flex flex-col items-center gap-1 text-green font-bold">
            <span className="text-xl">💪</span>
            <span className="text-xs">Log</span>
          </a>
          <a href="/settings" className="flex flex-col items-center gap-1 text-comment hover:text-purple">
            <span className="text-xl">⚙️</span>
            <span className="text-xs">Settings</span>
          </a>
        </nav>
      </body>
    </html>
  )
}
