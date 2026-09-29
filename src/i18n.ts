import type { BallColor } from './engine/models';

export type Language = 'en' | 'ru';
export const LANGUAGES: readonly Language[] = ['en', 'ru'];

const en = {
  'app.name': 'Color Lines',
  'app.tagline': 'The classic puzzle with coloured balls',
  'app.docTitle': 'Color Lines — the classic ball puzzle',

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
  'theme.modern': 'Modern dark',

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
  'cell.incoming': ', a {color} ball will appear here',
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

  'btn.settings': 'Settings',
  'menu.game': 'Game',

  'settings.title': 'Settings',
  'settings.theme': 'Theme',
  'settings.language': 'Language',
  'settings.sound': 'Sound effects',
  'settings.preview': 'Show where new balls will appear',

  'theme.light': 'Modern light',
  'theme.lines98': 'Lines 98 (Windows)',
  'theme.colorlines92': 'Color Lines 1992 (DOS)',

  'level.label': 'Level {level}',
  'level.xp': '{into} / {needed} XP',
  'level.title.novice': 'Novice',
  'level.title.apprentice': 'Apprentice',
  'level.title.skilled': 'Skilled',
  'level.title.expert': 'Expert',
  'level.title.master': 'Master',
  'level.title.grandmaster': 'Grandmaster',
  'level.title.legend': 'Legend',

  'stats.streak': 'Day streak',
  'stats.bestStreak': 'Best streak',
  'stats.playTime': 'Play time',
  'stats.bestLine': 'Longest line',
  'stats.mostLines': 'Most lines in a game',
  'stats.longestGame': 'Longest game',
  'stats.totalScore': 'Total score',
  'stats.trend': 'Last games',
  'stats.achievements': 'Achievements',
  'stats.achievementsCount': '{unlocked} of {total} unlocked',
  'stats.locked': 'locked',
  'stats.records': 'Records',
  'stats.showAll': 'Show all {n}',
  'stats.showLess': 'Show fewer',

  'gameover.xp': '+{xp} XP',
  'gameover.levelUp': 'Level up! You are now level {level}',
  'gameover.unlocked': 'Achievements unlocked',

  'time.hm': '{h} h {m} min',
  'time.m': '{m} min',
  'time.s': '{s} s',

  'ach.first_game.name': 'First steps',
  'ach.first_game.desc': 'Finish your first game.',
  'ach.first_line.name': 'In line',
  'ach.first_line.desc': 'Clear your first line.',
  'ach.long_line_7.name': 'Long shot',
  'ach.long_line_7.desc': 'Clear a line of 7 or more balls.',
  'ach.long_line_9.name': 'Full house',
  'ach.long_line_9.desc': 'Clear a line of 9 balls.',
  'ach.score_100.name': 'Century',
  'ach.score_100.desc': 'Score 100 points in one game.',
  'ach.score_250.name': 'On a roll',
  'ach.score_250.desc': 'Score 250 points in one game.',
  'ach.score_500.name': 'High roller',
  'ach.score_500.desc': 'Score 500 points in one game.',
  'ach.score_1000.name': 'Thousand club',
  'ach.score_1000.desc': 'Score 1000 points in one game.',
  'ach.games_10.name': 'Regular',
  'ach.games_10.desc': 'Play 10 games.',
  'ach.games_50.name': 'Devoted',
  'ach.games_50.desc': 'Play 50 games.',
  'ach.games_100.name': 'Addicted',
  'ach.games_100.desc': 'Play 100 games.',
  'ach.lines_50.name': 'Line cook',
  'ach.lines_50.desc': 'Clear 50 lines in total.',
  'ach.lines_250.name': 'Line master',
  'ach.lines_250.desc': 'Clear 250 lines in total.',
  'ach.streak_3.name': 'Habit',
  'ach.streak_3.desc': 'Play 3 days in a row.',
  'ach.streak_7.name': 'Week warrior',
  'ach.streak_7.desc': 'Play 7 days in a row.',
  'ach.marathon.name': 'Marathon',
  'ach.marathon.desc': 'Make 150 moves in one game.',
  'ach.level_5.name': 'Level 5',
  'ach.level_5.desc': 'Reach level 5.',
  'ach.level_10.name': 'Level 10',
  'ach.level_10.desc': 'Reach level 10.',

  'plural.days_one': '{n} day',
  'plural.days_few': '{n} days',
  'plural.days_many': '{n} days',
  'plural.days_other': '{n} days',

  'dos.help': 'Help',
  'dos.sound': 'Sound',
  'dos.next': 'Show next balls',
  'dos.restart': 'Restart',
  'dos.topTen': 'Top Ten',
  'dos.pretender': 'Pretender',
  'dos.king': 'King',
  'dos.defaultName': 'Player',
  'settings.playerName': 'Your name (Top Ten)',

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
  'app.name': 'Цветные линии',
  'app.tagline': 'Классическая головоломка с цветными шарами',
  'app.docTitle': 'Цветные линии — классическая головоломка с шарами',

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
  'theme.modern': 'Современная тёмная',

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
  'cell.incoming': ', здесь появится {color} шар',
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

  'btn.settings': 'Настройки',
  'menu.game': 'Игра',

  'settings.title': 'Настройки',
  'settings.theme': 'Тема',
  'settings.language': 'Язык',
  'settings.sound': 'Звуковые эффекты',
  'settings.preview': 'Показывать, где появятся новые шары',

  'theme.light': 'Современная светлая',
  'theme.lines98': 'Lines 98 (Windows)',
  'theme.colorlines92': 'Color Lines 1992 (DOS)',

  'level.label': 'Уровень {level}',
  'level.xp': '{into} / {needed} оп.',
  'level.title.novice': 'Новичок',
  'level.title.apprentice': 'Ученик',
  'level.title.skilled': 'Умелый',
  'level.title.expert': 'Знаток',
  'level.title.master': 'Мастер',
  'level.title.grandmaster': 'Гроссмейстер',
  'level.title.legend': 'Легенда',

  'stats.streak': 'Серия дней',
  'stats.bestStreak': 'Лучшая серия',
  'stats.playTime': 'Время в игре',
  'stats.bestLine': 'Самая длинная линия',
  'stats.mostLines': 'Больше всего линий за партию',
  'stats.longestGame': 'Самая длинная партия',
  'stats.totalScore': 'Всего очков',
  'stats.trend': 'Последние партии',
  'stats.achievements': 'Достижения',
  'stats.achievementsCount': 'Открыто {unlocked} из {total}',
  'stats.locked': 'закрыто',
  'stats.records': 'Рекорды',
  'stats.showAll': 'Показать все ({n})',
  'stats.showLess': 'Свернуть',

  'gameover.xp': '+{xp} оп.',
  'gameover.levelUp': 'Новый уровень! Теперь у вас уровень {level}',
  'gameover.unlocked': 'Новые достижения',

  'time.hm': '{h} ч {m} мин',
  'time.m': '{m} мин',
  'time.s': '{s} с',

  'ach.first_game.name': 'Первые шаги',
  'ach.first_game.desc': 'Закончите первую партию.',
  'ach.first_line.name': 'В линию',
  'ach.first_line.desc': 'Соберите первую линию.',
  'ach.long_line_7.name': 'Далёкий прицел',
  'ach.long_line_7.desc': 'Соберите линию из 7 и более шаров.',
  'ach.long_line_9.name': 'Аншлаг',
  'ach.long_line_9.desc': 'Соберите линию из 9 шаров.',
  'ach.score_100.name': 'Сотня',
  'ach.score_100.desc': 'Наберите 100 очков за партию.',
  'ach.score_250.name': 'В ударе',
  'ach.score_250.desc': 'Наберите 250 очков за партию.',
  'ach.score_500.name': 'Крупная ставка',
  'ach.score_500.desc': 'Наберите 500 очков за партию.',
  'ach.score_1000.name': 'Клуб тысячи',
  'ach.score_1000.desc': 'Наберите 1000 очков за партию.',
  'ach.games_10.name': 'Постоянный игрок',
  'ach.games_10.desc': 'Сыграйте 10 партий.',
  'ach.games_50.name': 'Преданный',
  'ach.games_50.desc': 'Сыграйте 50 партий.',
  'ach.games_100.name': 'Завсегдатай',
  'ach.games_100.desc': 'Сыграйте 100 партий.',
  'ach.lines_50.name': 'Линейный повар',
  'ach.lines_50.desc': 'Соберите 50 линий за всё время.',
  'ach.lines_250.name': 'Мастер линий',
  'ach.lines_250.desc': 'Соберите 250 линий за всё время.',
  'ach.streak_3.name': 'Привычка',
  'ach.streak_3.desc': 'Играйте 3 дня подряд.',
  'ach.streak_7.name': 'Неделя без пропусков',
  'ach.streak_7.desc': 'Играйте 7 дней подряд.',
  'ach.marathon.name': 'Марафон',
  'ach.marathon.desc': 'Сделайте 150 ходов за партию.',
  'ach.level_5.name': 'Уровень 5',
  'ach.level_5.desc': 'Достигните 5 уровня.',
  'ach.level_10.name': 'Уровень 10',
  'ach.level_10.desc': 'Достигните 10 уровня.',

  'plural.days_one': '{n} день',
  'plural.days_few': '{n} дня',
  'plural.days_many': '{n} дней',
  'plural.days_other': '{n} дня',

  'dos.help': 'Помощь',
  'dos.sound': 'Звук',
  'dos.next': 'Показывать следующие шары',
  'dos.restart': 'Заново',
  'dos.topTen': 'Десятка лучших',
  'dos.pretender': 'Pretender',
  'dos.king': 'King',
  'dos.defaultName': 'Игрок',
  'settings.playerName': 'Ваше имя (десятка лучших)',

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
export type PluralBase = 'plural.moves' | 'plural.balls' | 'plural.games' | 'plural.days';

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
