import React from 'react';

interface RetroCardProps {
  children: React.ReactNode;
  title?: string;
  icon?: string;
  className?: string;
}

export function RetroCard({ children, title, icon, className = '' }: RetroCardProps) {
  return (
    <div className={`border-2 border-comment bg-background p-4 ${className}`}>
      {title && (
        <div className="flex items-center gap-2 mb-3 pb-2 border-b border-comment">
          {icon && <span className="text-xl">{icon}</span>}
          <h3 className="text-foreground font-bold text-sm uppercase tracking-wider">
            {title}
          </h3>
        </div>
      )}
      {children}
    </div>
  );
}
