/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        'bg-base': '#0b1020',
        'bg-2': '#0f162c',
        panel: 'rgba(15,23,42,0.72)',
        ink: '#e2e8f0',
        muted: '#94a3b8',
        brand: '#3b82f6',
        'brand-2': '#22d3ee',
        'brand-warm': '#f59e0b',
        success: '#22c55e',
        danger: '#f43f5e',
        warning: '#fbbf24',
      },
      fontFamily: {
        sans: ['DM Sans', 'Inter', 'system-ui', 'sans-serif'],
        display: ['Space Grotesk', 'DM Sans', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      boxShadow: {
        panel: '0 20px 50px rgba(3,7,18,0.55)',
      },
    },
  },
  plugins: [],
};
