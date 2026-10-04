export interface DosNames {
  /** Under the red king's pillar (left). */
  king: string;
  /** Under the magenta pretender's pillar (right). */
  pretender: string;
}

/**
 * Who is called what on the 1992 screen. The dethroned king keeps his name and record; once the
 * score beats his, the crown moves to the pretender, who is the player and takes the player's name.
 */
export function dosNames(opts: {
  kingName: string;
  crowned: boolean;
  playerName: string;
  pretenderLabel: string;
  defaultPlayerName: string;
}): DosNames {
  // Older versions saved a localised default name; the 1992 screen shows only the English one.
  const typed = opts.playerName.trim();
  const player = typed === '' || typed === 'Игрок' ? opts.defaultPlayerName : typed;
  const king = opts.kingName === 'Игрок' ? opts.defaultPlayerName : opts.kingName;
  return { king, pretender: opts.crowned ? player : opts.pretenderLabel };
}
