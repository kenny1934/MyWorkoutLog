import React from 'react';

interface TerminalButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
  className?: string;
  fullWidth?: boolean;
}

export function TerminalButton({
  children,
  onClick,
  variant = 'primary',
  className = '',
  fullWidth = false
}: TerminalButtonProps) {
  const variantStyles = {
    primary: 'border-purple text-purple hover:bg-purple hover:text-background',
    secondary: 'border-comment text-comment hover:bg-comment hover:text-background',
    danger: 'border-red text-red hover:bg-red hover:text-background'
  };

  return (
    <button
      onClick={onClick}
      className={`
        retro-button
        ${variantStyles[variant]}
        ${fullWidth ? 'w-full' : ''}
        ${className}
      `}
    >
      [{children}]
    </button>
  );
}
