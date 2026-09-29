interface LedNumberProps {
  value: number;
  /** Minimum digits shown (zero padded), like the LED display of Lines 98. */
  digits?: number;
}

const base = () => import.meta.env.BASE_URL.replace(/\/$/, '') + '/originals/lines98/';

/** A number drawn with the red LED digit images of Lines 98. */
export function LedNumber({ value, digits = 5 }: LedNumberProps) {
  const text = String(Math.max(0, Math.floor(value))).padStart(digits, '0');
  return (
    <span className="led-number" role="img" aria-label={String(value)}>
      {[...text].map((d, i) => (
        <img key={i} src={`${base()}digit_${d}.png`} alt="" draggable={false} />
      ))}
    </span>
  );
}
