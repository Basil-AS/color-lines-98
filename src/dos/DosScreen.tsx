import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import '@fontsource/unifrakturcook';
import { buildScene } from './scene';
import type { DosState, Effect } from './scene';
import { BOARD, BUTTONS, BUTTON_X, BUTTON_Y, CELL, HELP_WINDOW, SCREEN, TOP_TEN_WINDOW, WINDOW_POS, glyphRect } from './sprites';
import type { ButtonId } from './sprites';
import type { HallEntry } from './hall';

export type DosWindow = 'none' | 'help' | 'top10';

interface DosScreenProps {
  state: Omit<DosState, 'now'>;
  kingName: string;
  pretenderName: string;
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

function drawText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, align: CanvasTextAlign) {
  ctx.save();
  ctx.font = '22px UnifrakturCook, "Old English Text MT", fantasy';
  ctx.fillStyle = '#ffff55';
  ctx.textAlign = align;
  ctx.translate(x, y);
  ctx.scale(1, 0.73); // the screen is stretched 1.37x vertically, keep the letters upright
  ctx.fillText(text, 0, 0);
  ctx.restore();
}

function drawPixelText(ctx: CanvasRenderingContext2D, sheet: HTMLImageElement, text: string, x: number, y: number) {
  [...text].forEach((ch, i) => {
    const g = glyphRect(ch);
    if (g) ctx.drawImage(sheet, g[0], g[1], g[2], g[3], x + i * 9, y, g[2], g[3]);
  });
}

export function DosScreen({
  state,
  kingName,
  pretenderName,
  window: win,
  hall,
  labels,
  onButton,
  onCloseWindow,
  children,
}: DosScreenProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const images = useRef<{ layout: HTMLImageElement; sheet: HTMLImageElement } | null>(null);
  const latest = useRef({ state, kingName, pretenderName, win, hall });
  useEffect(() => {
    latest.current = { state, kingName, pretenderName, win, hall };
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
      const pick = (img: 'layout' | 'sheet') => (img === 'layout' ? layout : sheet);
      for (const d of buildScene({ ...cur.state, now })) {
        if (d.kind === 'fill') {
          ctx.fillStyle = d.color;
          ctx.fillRect(d.x, d.y, d.w, d.h);
        } else {
          ctx.drawImage(pick(d.img), d.sx, d.sy, d.sw, d.sh, d.dx, d.dy, d.sw, d.sh);
        }
      }
      drawText(ctx, cur.kingName, 88, 262, 'center');
      drawText(ctx, cur.pretenderName, 541, 262, 'center');

      if (cur.win !== 'none') {
        const r = cur.win === 'help' ? HELP_WINDOW : TOP_TEN_WINDOW;
        ctx.drawImage(sheet, r[0], r[1], r[2], r[3], WINDOW_POS.x, WINDOW_POS.y, r[2], r[3]);
        if (cur.win === 'top10') {
          cur.hall.slice(0, 10).forEach((h, i) => {
            const y = WINDOW_POS.y + 31 + i * 11.6;
            drawPixelText(ctx, sheet, h.name, WINDOW_POS.x + 34, y);
            const s = String(h.score);
            drawPixelText(ctx, sheet, s, WINDOW_POS.x + 210 - s.length * 9, y);
          });
        }
      }
    };

    layout.addEventListener('load', draw);
    sheet.addEventListener('load', draw);
    void document.fonts?.load('22px UnifrakturCook').then(draw);
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
