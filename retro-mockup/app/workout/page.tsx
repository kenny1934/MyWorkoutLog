'use client';

import { useState } from 'react';
import { RetroCard } from '@/components/RetroCard';
import { TerminalButton } from '@/components/TerminalButton';

export default function WorkoutLogger() {
  const [weight, setWeight] = useState(100);
  const [reps, setReps] = useState(8);
  const [rir, setRir] = useState(2);
  const [sets, setSets] = useState([
    { weight: 100, reps: 8, rir: 2 },
    { weight: 100, reps: 8, rir: 2 },
  ]);

  const logSet = () => {
    setSets([...sets, { weight, reps, rir }]);
  };

  return (
    <div className="p-4 space-y-4 max-w-2xl mx-auto">
      {/* Timer */}
      <div className="flex justify-between items-center px-4 py-3 border-2 border-purple bg-current">
        <div className="flex items-center gap-2">
          <span className="text-purple text-xl">⏱️</span>
          <span className="text-foreground font-bold text-xl">45:32</span>
        </div>
        <TerminalButton variant="danger">
          FINISH
        </TerminalButton>
      </div>

      {/* Exercise Header */}
      <RetroCard title="BENCH PRESS" icon="🏋️">
        <div className="flex items-center justify-center">
          <div className="text-6xl">🏋️</div>
        </div>
        <div className="text-center text-sm text-comment mt-2">
          Chest • Barbell
        </div>
      </RetroCard>

      {/* Completed Sets */}
      <RetroCard title="COMPLETED SETS">
        <div className="space-y-2">
          {sets.map((set, i) => (
            <div
              key={i}
              className="flex justify-between items-center text-comment border-b border-current pb-2"
            >
              <span className="text-sm">Set {i + 1}</span>
              <span className="text-sm">
                {set.weight}kg × {set.reps} @{set.rir}RIR
              </span>
            </div>
          ))}
        </div>
      </RetroCard>

      {/* Set Input */}
      <RetroCard title={`SET ${sets.length + 1}`}>
        <div className="space-y-4">
          {/* Weight */}
          <div>
            <div className="text-xs text-comment mb-2">WEIGHT</div>
            <div className="flex items-center justify-center gap-3">
              <button
                onClick={() => setWeight(Math.max(0, weight - 5))}
                className="w-12 h-12 border-2 border-purple text-purple hover:bg-purple hover:text-background transition-colors text-xl font-bold"
              >
                -5
              </button>
              <div className="flex-1 text-center">
                <div className="text-3xl font-bold text-foreground">{weight}</div>
                <div className="text-sm text-comment">kg</div>
              </div>
              <button
                onClick={() => setWeight(weight + 5)}
                className="w-12 h-12 border-2 border-purple text-purple hover:bg-purple hover:text-background transition-colors text-xl font-bold"
              >
                +5
              </button>
            </div>
          </div>

          {/* Reps */}
          <div>
            <div className="text-xs text-comment mb-2">REPS</div>
            <div className="flex items-center justify-center gap-3">
              <button
                onClick={() => setReps(Math.max(1, reps - 1))}
                className="w-12 h-12 border-2 border-cyan text-cyan hover:bg-cyan hover:text-background transition-colors text-xl font-bold"
              >
                -1
              </button>
              <div className="flex-1 text-center">
                <div className="text-3xl font-bold text-foreground">{reps}</div>
              </div>
              <button
                onClick={() => setReps(reps + 1)}
                className="w-12 h-12 border-2 border-cyan text-cyan hover:bg-cyan hover:text-background transition-colors text-xl font-bold"
              >
                +1
              </button>
            </div>
          </div>

          {/* RIR */}
          <div>
            <div className="text-xs text-comment mb-2">REPS IN RESERVE</div>
            <div className="flex justify-center gap-2">
              {[0, 1, 2, 3, 4, 5].map((r) => (
                <button
                  key={r}
                  onClick={() => setRir(r)}
                  className={`w-10 h-10 border-2 transition-colors ${
                    rir === r
                      ? 'border-green bg-green text-background'
                      : 'border-comment text-comment hover:border-green'
                  }`}
                >
                  {r}
                </button>
              ))}
            </div>
          </div>

          {/* Log Button */}
          <TerminalButton variant="primary" fullWidth onClick={logSet}>
            LOG SET
          </TerminalButton>

          {/* Rest Timer */}
          <div className="text-center text-sm text-comment pt-2 border-t border-current">
            Rest: <span className="text-purple font-bold">3:00</span>
          </div>
        </div>
      </RetroCard>

      {/* Next Exercise */}
      <TerminalButton variant="secondary" fullWidth>
        NEXT EXERCISE →
      </TerminalButton>
    </div>
  );
}
