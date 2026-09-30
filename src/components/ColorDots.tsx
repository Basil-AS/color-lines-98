import { EASY_COLORS } from '../modes';
import { ALL_COLORS } from '../engine/models';
import type { BallColor } from '../engine/models';
import type { ModeId } from '../engine/modes';

const HEX: Record<BallColor, string> = {
  red: '#e53935',
  green: '#43a047',
  blue: '#1e88e5',
  cyan: '#00acc1',
  magenta: '#8e24aa',
  yellow: '#fdd835',
  brown: '#6d4c41',
};

/** The colours a mode plays with, as small dots: "7 colours" is something you can see, not just read. */
export function ColorDots({ mode }: { mode: ModeId }) {
  const colors = mode === 'easy' ? EASY_COLORS : ALL_COLORS;
  return (
    <span className="color-dots" aria-hidden="true">
      {colors.map((c) => (
        <span key={c} className="color-dot" style={{ background: HEX[c] }} />
      ))}
    </span>
  );
}
