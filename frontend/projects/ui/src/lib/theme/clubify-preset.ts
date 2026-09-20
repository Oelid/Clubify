import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/**
 * Thème PrimeNG branché sur les jetons Clubify.
 *
 * <p>Aucune couleur n'est écrite ici en dur : tout renvoie aux variables CSS de
 * `tokens.css`, elles-mêmes surchargeables par club (ADM-01). Changer la marque
 * d'un club ne demande donc aucun changement de code.
 */
export const ClubifyPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{surface.50}',
      100: '{surface.100}',
      200: '{surface.200}',
      300: '{surface.300}',
      400: '{surface.400}',
      500: 'var(--brand-primary)',
      600: 'var(--brand-primary)',
      700: 'var(--brand-primary)',
      800: 'var(--brand-primary)',
      900: 'var(--brand-primary)',
      950: 'var(--brand-primary)',
    },
    formField: {
      paddingX: '0.75rem',
      paddingY: '0.5rem',
      borderRadius: 'var(--radius-md)',
      focusRing: {
        width: '2px',
        style: 'solid',
        color: 'var(--brand-primary)',
        offset: '1px',
      },
    },
    colorScheme: {
      light: {
        surface: {
          0: 'var(--color-surface)',
          50: 'var(--color-surface-sunken)',
          100: '#f0f0ee',
          200: 'var(--color-border)',
          300: 'var(--color-border-strong)',
          400: '#a8a8a2',
          500: '#8a8a84',
          600: 'var(--color-text-muted)',
          700: '#4a4a46',
          800: '#2d2d2a',
          900: 'var(--color-text)',
          950: '#0f0f0e',
        },
        text: {
          color: 'var(--color-text)',
          mutedColor: 'var(--color-text-muted)',
        },
        content: {
          background: 'var(--color-surface)',
          borderColor: 'var(--color-border)',
        },
      },
    },
  },
});
