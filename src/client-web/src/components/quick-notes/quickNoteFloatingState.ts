export const QUICK_NOTE_DRAFT_KEY = 'pim.quickNotes.floatingDraft';
export const QUICK_NOTE_PANEL_POSITION_KEY = 'pim.quickNotes.panelPosition';

const PANEL_MARGIN = 12;

export interface PanelPoint {
  x: number;
  y: number;
}

export interface PanelSize {
  width: number;
  height: number;
}

export function clampPanelPosition(
  point: PanelPoint,
  viewport: PanelSize,
  panel: PanelSize,
): PanelPoint {
  const maxX = Math.max(PANEL_MARGIN, viewport.width - panel.width - PANEL_MARGIN);
  const maxY = Math.max(PANEL_MARGIN, viewport.height - panel.height - PANEL_MARGIN);

  return {
    x: Math.min(Math.max(point.x, PANEL_MARGIN), maxX),
    y: Math.min(Math.max(point.y, PANEL_MARGIN), maxY),
  };
}

export function loadPanelPosition(viewport: PanelSize, panel: PanelSize): PanelPoint {
  const fallback = clampPanelPosition(
    {
      x: viewport.width - panel.width - PANEL_MARGIN,
      y: viewport.height - panel.height - PANEL_MARGIN,
    },
    viewport,
    panel,
  );

  if (typeof localStorage === 'undefined') {
    return fallback;
  }

  try {
    const stored = localStorage.getItem(QUICK_NOTE_PANEL_POSITION_KEY);
    if (!stored) {
      return fallback;
    }

    const parsed = JSON.parse(stored) as Partial<PanelPoint>;
    if (typeof parsed.x !== 'number' || typeof parsed.y !== 'number') {
      return fallback;
    }

    return clampPanelPosition(parsed as PanelPoint, viewport, panel);
  } catch {
    return fallback;
  }
}

export function savePanelPosition(point: PanelPoint) {
  if (typeof localStorage === 'undefined') {
    return;
  }

  localStorage.setItem(QUICK_NOTE_PANEL_POSITION_KEY, JSON.stringify(point));
}
