'use client';

import { RetroCard } from '@/components/RetroCard';
import { TerminalButton } from '@/components/TerminalButton';
import { RetroProgressBar } from '@/components/RetroProgressBar';

export default function Dashboard() {
  return (
    <div className="p-4 space-y-4 max-w-2xl mx-auto">
      {/* Quick Stats */}
      <RetroCard title="QUICK STATS" icon="💪">
        <div className="grid grid-cols-3 gap-4 text-center">
          <div>
            <div className="text-2xl text-purple font-bold">24</div>
            <div className="text-xs text-comment">Workouts</div>
          </div>
          <div>
            <div className="text-2xl text-green font-bold">12</div>
            <div className="text-xs text-comment">PRs</div>
          </div>
          <div>
            <div className="text-2xl text-cyan font-bold">8</div>
            <div className="text-xs text-comment">Streak</div>
          </div>
        </div>
      </RetroCard>

      {/* Next Session */}
      <RetroCard title="NEXT SESSION" icon="🗓️">
        <div className="space-y-3">
          <div>
            <div className="text-foreground font-bold">Friday • Leg Day</div>
            <div className="text-sm text-comment">Week 2, Day 3 - Strength Phase</div>
          </div>
          <div className="text-sm text-comment space-y-1">
            <div>• Squat 5×5 @ 120kg</div>
            <div>• RDL 4×8 @ 100kg</div>
            <div>• Leg Press 3×12-15</div>
          </div>
          <TerminalButton variant="primary" fullWidth>
            START WORKOUT
          </TerminalButton>
        </div>
      </RetroCard>

      {/* Cycle Progress */}
      <RetroCard title="CYCLE PROGRESS" icon="📈">
        <div className="space-y-3">
          <div>
            <div className="text-foreground font-bold">Strength Phase 2025-01</div>
            <div className="text-sm text-comment">Week 2 of 4</div>
          </div>
          <RetroProgressBar value={9} max={15} label="Sessions" />
          <div className="text-xs text-comment">
            ✓✓✓✓✓✓✓✓✓○○○○○○
          </div>
        </div>
      </RetroCard>

      {/* Recent Workouts */}
      <RetroCard title="RECENT WORKOUTS" icon="📊">
        <div className="space-y-2 text-sm">
          <div className="flex justify-between items-center pb-2 border-b border-comment">
            <span className="text-foreground">Wed • Pull Day</span>
            <span className="text-comment">45:23</span>
          </div>
          <div className="flex justify-between items-center pb-2 border-b border-comment">
            <span className="text-foreground">Mon • Push Day</span>
            <span className="text-comment">52:10</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-foreground">Fri • Leg Day</span>
            <span className="text-comment">61:45</span>
          </div>
        </div>
      </RetroCard>

      {/* Quick Actions */}
      <div className="grid grid-cols-2 gap-3">
        <TerminalButton variant="secondary">
          VIEW HISTORY
        </TerminalButton>
        <TerminalButton variant="secondary">
          ANALYTICS
        </TerminalButton>
      </div>
    </div>
  );
}
