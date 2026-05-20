import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';

interface VisibilityContext {
  hiddenCalendarIds: Set<string>;
  toggleCalendar: (id: string) => void;
}

const CalendarVisibilityContext = createContext<VisibilityContext>({
  hiddenCalendarIds: new Set(),
  toggleCalendar: () => {}
});

export function CalendarVisibilityProvider({ children }: { children: ReactNode }) {
  const [hiddenIds, setHiddenIds] = useState<Set<string>>(new Set());

  const toggleCalendar = useCallback((id: string) => {
    setHiddenIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  return (
    <CalendarVisibilityContext.Provider value={{ hiddenCalendarIds: hiddenIds, toggleCalendar }}>
      {children}
    </CalendarVisibilityContext.Provider>
  );
}

export function useCalendarVisibility() {
  return useContext(CalendarVisibilityContext);
}
