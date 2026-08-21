import { useEffect } from 'react';

export interface ShellShareDetail { text?: string; url?: string }

export function useShellShare(onShare: (detail: ShellShareDetail) => void) {
  useEffect(() => {
    const handler = (e: Event) => {
      const detail = (e as CustomEvent<ShellShareDetail>).detail;
      if (detail) onShare(detail);
    };
    window.addEventListener('pim-shell:share', handler as EventListener);
    return () => window.removeEventListener('pim-shell:share', handler as EventListener);
  }, [onShare]);
}
