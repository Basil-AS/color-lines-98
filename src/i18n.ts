import type { BallColor } from './engine/models';

export type Language = 'en' | 'ru';
export const LANGUAGES: readonly Language[] = ['en', 'ru'];

const en = {
  'app.name': 'Basil Lines',
  'app.tagline': 'Classic colour-lines puzzle',
  'app.docTitle': 'Basil Lines — classic colour-lines puzzle',

  'hud.score': 'Score',
  'hud.best': 'Best',
  'hud.next': 'Next',

  'btn.undo': 'Undo move',
  'btn.newGame': 'New game',
  'btn.mute': 'Mute sound',
  'btn.unmute': 'Unmute sound',
  'btn.help': 'Rules and info',
  'btn.stats': 'Statistics',
  'btn.apk': 'Android app',
  'btn.github': 'Source code on GitHub',
  'btn.close': 'Close',

  'theme.group': 'Theme',
  'theme.modern': 'Modern',
  'theme.classic98': 'Win98',
  'theme.retro92': 'DOS 92',

  'lang.label': 'Language',
  'lang.auto': 'Auto',
  'lang.en': 'English',
  'lang.ru': 'Русский',

  'board.label': 'Game board',
  'cell.description': 'Row {row}, column {col}, {content}{state}',
  'cell.ball': '{color} ball',
  'cell.empty': 'empty',
  'cell.selected': ', selected',
  'cell.reachable': ', reachable',
  'next.label': 'Next balls: {colors}',

  'color.red': 'red',
  'color.green': 'green',
  'color.blue': 'blue',
  'color.cyan': 'cyan',
  'color.magenta': 'magenta',
  'color.yellow': 'yellow',
  'color.brown': 'brown',

  'announce.noPath': 'No path to that cell',
  'announce.score': 'Score {score}',
  'announce.lineCleared': 'Line cleared: +{points}. Score {score}',
  'announce.gameOver': 'Game over. Final score {score}',
  'announce.undone': 'Move undone. Score {score}',
  'announce.newGame': 'New game started',

  'gameover.title': 'Game over',
  'gameover.final': 'Final score',
  'gameover.best': 'Best score',
  'gameover.newRecord': 'New record!',
  'gameover.playAgain': 'Play again',

  'stats.title': 'Statistics',
  'stats.gamesPlayed': 'Games played',
  'stats.best': 'Best score',
  'stats.average': 'Average score',
  'stats.lines': 'Lines cleared',
  'stats.moves': 'Total moves',
  'stats.recent': 'Recent games',
  'stats.empty': 'No games yet. Finish a game and it will show up here.',
  'stats.unfinished': 'unfinished',
  'stats.bestMark': 'best',
  'stats.clear': 'Clear history',
  'stats.confirmClear': 'Delete all saved games?',
  'stats.confirmYes': 'Delete',
  'stats.confirmNo': 'Cancel',

  'help.title': 'Rules & scoring',
  'help.objective':
    'Line up 5 or more balls of the same colour horizontally, vertically or diagonally to clear them.',
  'help.scoring': 'Score for a line:',
  'help.scoringLine': '{count}: {points}',
  'help.movement':
    'A ball can move only if there is a clear path of empty cells to the destination.',
  'help.freeTurn': 'Clearing a line is a free turn: no new balls appear.',
  'help.keyboard':
    'Keyboard: arrow keys move between cells, Enter or Space selects a ball or moves it, Escape closes a window.',

  'plural.moves_one': '{n} move',
  'plural.moves_few': '{n} moves',
  'plural.moves_many': '{n} moves',
  'plural.moves_other': '{n} moves',
  'plural.balls_one': '{n} ball',
  'plural.balls_few': '{n} balls',
  'plural.balls_many': '{n} balls',
  'plural.balls_other': '{n} balls',
  'plural.games_one': '{n} game',
  'plural.games_few': '{n} games',
  'plural.games_many': '{n} games',
  'plural.games_other': '{n} games',
} as const;

type Dictionary = { readonly [K in keyof typeof en]: string };

const ru: Dictionary = {
  'app.name': 'Basil Lines',
  'app.tagline': 'Классическая головоломка с шарами',
  'app.docTitle': 'Basil Lines — классическая головоломка с шарами',

  'hud.score': 'Счёт',
  'hud.best': 'Рекорд',
  'hud.next': 'Далее',

  'btn.undo': 'Отменить ход',
  'btn.newGame': 'Новая игра',
  'btn.mute': 'Выключить звук',
  'btn.unmute': 'Включить звук',
  'btn.help': 'Правила',
  'btn.stats': 'Статистика',
  'btn.apk': 'Приложение для Android',
  'btn.github': 'Исходный код на GitHub',
  'btn.close': 'Закрыть',

  'theme.group': 'Тема',
  'theme.modern': 'Модерн',
  'theme.classic98': 'Win98',
  'theme.retro92': 'DOS 92',

  'lang.label': 'Язык',
  'lang.auto': 'Авто',
  'lang.en': 'English',
  'lang.ru': 'Русский',

  'board.label': 'Игровое поле',
  'cell.description': 'Ряд {row}, столбец {col}, {content}{state}',
  'cell.ball': '{color} шар',
  'cell.empty': 'пусто',
  'cell.selected': ', выбран',
  'cell.reachable': ', можно дойти',
  'next.label': 'Следующие шары: {colors}',

  'color.red': 'красный',
  'color.green': 'зелёный',
  'color.blue': 'синий',
  'color.cyan': 'голубой',
  'color.magenta': 'пурпурный',
  'color.yellow': 'жёлтый',
  'color.brown': 'коричневый',

  'announce.noPath': 'Туда не пройти',
  'announce.score': 'Счёт {score}',
  'announce.lineCleared': 'Линия собрана: +{points}. Счёт {score}',
  'announce.gameOver': 'Игра окончена. Итоговый счёт {score}',
  'announce.undone': 'Ход отменён. Счёт {score}',
  'announce.newGame': 'Новая игра началась',

  'gameover.title': 'Игра окончена',
  'gameover.final': 'Итоговый счёт',
  'gameover.best': 'Рекорд',
  'gameover.newRecord': 'Новый рекорд!',
  'gameover.playAgain': 'Играть снова',

  'stats.title': 'Статистика',
  'stats.gamesPlayed': 'Сыграно партий',
  'stats.best': 'Лучший счёт',
  'stats.average': 'Средний счёт',
  'stats.lines': 'Собрано линий',
  'stats.moves': 'Всего ходов',
  'stats.recent': 'Последние партии',
  'stats.empty': 'Пока нет партий. Закончите партию, и она появится здесь.',
  'stats.unfinished': 'не закончена',
  'stats.bestMark': 'рекорд',
  'stats.clear': 'Очистить историю',
  'stats.confirmClear': 'Удалить все сохранённые партии?',
  'stats.confirmYes': 'Удалить',
  'stats.confirmNo': 'Отмена',

  'help.title': 'Правила и очки',
  'help.objective':
    'Выстройте 5 и более шаров одного цвета по горизонтали, вертикали или диагонали, чтобы убрать их.',
  'help.scoring': 'Очки за линию:',
  'help.scoringLine': '{count}: {points}',
  'help.movement':
    'Шар можно переместить, только если до нужной клетки есть свободный путь из пустых клеток.',
  'help.freeTurn': 'Собранная линия даёт дополнительный ход: новые шары не появляются.',
  'help.keyboard':
    'Клавиатура: стрелки перемещают между клетками, Enter или пробел выбирает шар или переносит его, Escape закрывает окно.',

  'plural.moves_one': '{n} ход',
  'plural.moves_few': '{n} хода',
  'plural.moves_many': '{n} ходов',
  'plural.moves_other': '{n} хода',
  'plural.balls_one': '{n} шар',
  'plural.balls_few': '{n} шара',
  'plural.balls_many': '{n} шаров',
  'plural.balls_other': '{n} шара',
  'plural.games_one': '{n} партия',
  'plural.games_few': '{n} партии',
  'plural.games_many': '{n} партий',
  'plural.games_other': '{n} партии',
};

export const messages: Record<Language, Dictionary> = { en, ru };

export type MessageKey = keyof typeof en;
export type PluralBase = 'plural.moves' | 'plural.balls' | 'plural.games';

type Params = Record<string, string | number>;

function interpolate(template: string, params: Params): string {
  return template.replace(/\{(\w+)\}/g, (whole, name: string) =>
    name in params ? String(params[name]) : whole
  );
}

export function translate(lang: Language, key: MessageKey, params: Params = {}): string {
  return interpolate(messages[lang][key], params);
}

export function translatePlural(lang: Language, base: PluralBase, n: number): string {
  const category = new Intl.PluralRules(lang).select(n);
  return translate(lang, `${base}_${category}` as MessageKey, { n });
}

export function colorName(lang: Language, color: BallColor): string {
  return translate(lang, `color.${color}` as MessageKey);
}

/** First supported language in the user's preference list wins; English is the fallback. */
export function resolveLanguage(
  pref: Language | 'auto',
  browserLanguages: readonly string[]
): Language {
  if (pref !== 'auto') return pref;
  for (const tag of browserLanguages) {
    const primary = tag.toLowerCase().split('-')[0];
    if ((LANGUAGES as readonly string[]).includes(primary)) return primary as Language;
  }
  return 'en';
}

export function formatDate(lang: Language, epochMs: number): string {
  return new Intl.DateTimeFormat(lang, { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(epochMs)
  );
}
