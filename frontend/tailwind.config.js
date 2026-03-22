/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Aeroport', 'sans-serif'],
      },
      colors: {
        'tiny-gray': '#F9F9F9'
      },
      borderRadius: {
        'sm': '2px',
        DEFAULT: '4px',
        'md': '4px',
        'lg': '4px',
        'xl': '4px',
        '2xl': '4px',
        '3xl': '4px'
      },
      boxShadow: {
        sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        DEFAULT: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
        md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
        lg: 'none', // Removed heavy shadows as per strict guidelines
        xl: 'none',
        '2xl': 'none',
        inner: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.06)',
      }
    },
  },
  plugins: [],
}
