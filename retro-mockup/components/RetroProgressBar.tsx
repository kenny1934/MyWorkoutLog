import React from 'react';

interface RetroProgressBarProps {
  value: number;
  max: number;
  label?: string;
  showPercentage?: boolean;
}

export function RetroProgressBar({ value, max, label, showPercentage = true }: RetroProgressBarProps) {
  const percentage = Math.round((value / max) * 100);
  const blocks = 10;
  const filledBlocks = Math.round((value / max) * blocks);

  return (
    <div className="space-y-1">
      {label && <div className="text-xs text-comment">{label}</div>}
      <div className="flex items-center gap-2">
        <div className="pixel-progress flex-1">
          {Array.from({ length: blocks }).map((_, i) => (
            <div
              key={i}
              className={i < filledBlocks ? 'pixel-block' : 'pixel-block-empty'}
            />
          ))}
        </div>
        {showPercentage && (
          <span className="text-sm text-foreground font-bold min-w-[3ch]">
            {percentage}%
          </span>
        )}
      </div>
    </div>
  );
}
