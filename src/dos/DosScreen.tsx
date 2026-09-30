import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import '@fontsource/unifrakturcook';
import '@fontsource/ruslan-display';
import { buildScene, buildWindow } from './scene';
import type { Draw, DosState, Effect } from './scene';
import { BOARD, BUTTONS, BUTTON_X, BUTTON_Y, CELL, SCREEN, WINDOW_POS } from './sprites';
import type { ButtonId } from './sprites';
import type { HallEntry } from './hall';

export type DosWindow = 'none' | 'help' | 'top10';

interface DosScreenProps {
  state: Omit<DosState, 'now'>;
  kingName: string;
  pretenderName: string;
  lang: 'en' | 'ru';
  window: DosWindow;
  hall: readonly HallEntry[];
  labels: Record<ButtonId, string>;
  onButton: (id: ButtonId) => void;
  onCloseWindow: () => void;
  /** The 9x9 grid of accessible cell buttons that sits on top of the board. */
  children: ReactNode;
}

const base = () => import.meta.env.BASE_URL.replace(/\/$/, '') + '/originals/colorlines1992/';

function loadImage(name: string): HTMLImageElement {
  const img = new Image();
  img.src = base() + name;
  return img;
}

const pct = (v: number, total: number) => `${(v / total) * 100}%`;

const FONTS = {
  gothic: '"UnifrakturCook", "Ruslan Display", "Old English Text MT", fantasy',
  mono: '"Courier New", Courier, monospace',
  serif: '"Times New Roman", "Ruslan Display", serif',
} as const;

/** Runs the drawing commands of the scene. Text is squeezed vertically so it looks upright on the 4:3 stretch. */
function paint(ctx: CanvasRenderingContext2D, draws: readonly Draw[], layout: HTMLImageElement, sheet: HTMLImageElement) {
  for (const d of draws) {
    if (d.kind === 'fill') {
      ctx.fillStyle = d.color;
      ctx.fillRect(d.x, d.y, d.w, d.h);
    } else if (d.kind === 'image') {
      ctx.drawImage(d.img === 'layout' ? layout : sheet, d.sx, d.sy, d.sw, d.sh, d.dx, d.dy, d.sw, d.sh);
    } else {
      ctx.save();
      ctx.font = `${d.font === 'gothic' ? '' : 'bold '}${d.size}px ${FONTS[d.font]}`;
      ctx.textAlign = d.align;
      ctx.translate(d.x, d.y);
      ctx.scale(1, 0.73);
      if (d.shadow) {
        ctx.fillStyle = d.shadow;
        ctx.fillText(d.text, 1, 1);
      }
      ctx.fillStyle = d.color;
      ctx.fillText(d.text, 0, 0);
      ctx.restore();
    }
  }
}

export function DosScreen({
  state,
  kingName,
  pretenderName,
  lang,
  window: win,
  hall,
  labels,
  onButton,
  onCloseWindow,
  children,
}: DosScreenProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const images = useRef<{ layout: HTMLImageElement; sheet: HTMLImageElement } | null>(null);
  const latest = useRef({ state, kingName, pretenderName, lang, win, hall });
  useEffect(() => {
    latest.current = { state, kingName, pretenderName, lang, win, hall };
  });

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return; // e.g. tests without a canvas implementation
    ctx.imageSmoothingEnabled = false;
    images.current ??= { layout: loadImage('layout.png'), sheet: loadImage('sheet.png') };
    const { layout, sheet } = images.current;

    const draw = () => {
      if (!layout.complete || !sheet.complete || layout.naturalWidth === 0) return;
      const cur = latest.current;
      const now = performance.now();
      paint(ctx, buildScene({ ...cur.state, now, lang: cur.lang, kingName: cur.kingName, pretenderName: cur.pretenderName }), layout, sheet);
      if (cur.win !== 'none') paint(ctx, buildWindow(cur.win === 'help' ? 'help' : 'top10', cur.lang, cur.hall), layout, sheet);
    };

    layout.addEventListener('load', draw);
    sheet.addEventListener('load', draw);
    void Promise.all([document.fonts?.load('22px UnifrakturCook'), document.fonts?.load('22px "Ruslan Display"')]).then(draw);
    draw();
    const timer = window.setInterval(draw, 70);
    return () => {
      window.clearInterval(timer);
      layout.removeEventListener('load', draw);
      sheet.removeEventListener('load', draw);
    };
  }, []);

  return (
    <div className="dos-screen">
      <canvas ref={canvasRef} className="dos-canvas" width={SCREEN.w} height={SCREEN.h} aria-hidden="true" />
      <div
        className="dos-board-overlay"
        hidden={win !== 'none'}
        style={{
          left: pct(BOARD.x, SCREEN.w),
          top: pct(BOARD.y, SCREEN.h),
          width: pct(CELL.w * 9, SCREEN.w),
          height: pct(CELL.h * 9, SCREEN.h),
        }}
      >
        {children}
      </div>
      {BUTTONS.map((id, i) => (
        <button
          key={id}
          type="button"
          className="dos-button"
          aria-label={`F${i + 1}: ${labels[id]}`}
          style={{
            left: pct(BUTTON_X[id] - 36, SCREEN.w),
            top: pct(BUTTON_Y - 4, SCREEN.h),
            width: pct(109, SCREEN.w),
            height: pct(22, SCREEN.h),
          }}
          onClick={() => onButton(id)}
        />
      ))}
      {win !== 'none' && (
        <button
          type="button"
          className="dos-window-close"
          aria-label={labels.help}
          onClick={onCloseWindow}
          style={{
            left: pct(WINDOW_POS.x, SCREEN.w),
            top: pct(WINDOW_POS.y, SCREEN.h),
            width: pct(238, SCREEN.w),
            height: pct(166, SCREEN.h),
          }}
        />
      )}
    </div>
  );
}

export type { Effect };
