import type { Point } from './engine/models';
import type { GameEngine } from './engine/gameengine';
import { stepEngine } from './tutorial';
import type { TutorialStep } from './tutorial';

export interface CoachText { title: string; explanation: string; hint: string; success: string; wrong: string }
export interface StrategyLesson {
  id: string;
  setup: NonNullable<TutorialStep['setup']>;
  from: Point;
  to: Point;
  expectedCleared: number;
  acceptedTargets?: readonly Point[];
  text: { en: CoachText; ru: CoachText };
}
/** Hand-authored examples of useful patterns, not a general optimal-move solver. */
const LESSONS: StrategyLesson[] = [
  {
    "id": "open-ends",
    acceptedTargets: [{ x: 1, y: 4 }, { x: 4, y: 4 }],
    "setup": {
      "balls": [
        [
          2,
          4,
          "red"
        ],
        [
          3,
          4,
          "red"
        ],
        [
          7,
          2,
          "red"
        ]
      ],
      "next": [
        "blue",
        "green",
        "yellow"
      ],
      "nextPoints": [
        {
          "x": 8,
          "y": 8
        },
        {
          "x": 0,
          "y": 8
        },
        {
          "x": 8,
          "y": 0
        }
      ]
    },
    "from": {
      "x": 7,
      "y": 2
    },
    "to": {
      "x": 4,
      "y": 4
    },
    "expectedCleared": 0,
    "text": {
      "en": {
        "title": "Keep both ends open",
        "explanation": "Build a three-ball row with empty cells at both ends. It can grow in either direction; this is a useful pattern, not a guarantee of a win.",
        "hint": "Move the red ball at column 8, row 3 to column 5, row 5.",
        "success": "The three red balls now have space at both ends. Plan a fourth while keeping access open.",
        "wrong": "That move misses the open-ended three. Try extending the pair without closing an end."
      },
      "ru": {
        "title": "Оставляйте оба конца открытыми",
        "explanation": "Соберите тройку с пустыми клетками с обоих концов. Её можно продолжить в любую сторону; это полезный приём, а не гарантия победы.",
        "hint": "Переместите красный шар из столбца 8, строки 3 в столбец 5, строку 5.",
        "success": "У красной тройки открыты оба конца. Планируйте четвёртый шар, сохраняя доступ.",
        "wrong": "Этот ход не создаёт открытую тройку. Продолжите пару, не закрывая её конец."
      }
    }
  },
  {
    "id": "gap",
    "setup": {
      "balls": [
        [
          1,
          4,
          "red"
        ],
        [
          2,
          4,
          "red"
        ],
        [
          4,
          4,
          "red"
        ],
        [
          5,
          4,
          "red"
        ],
        [
          7,
          2,
          "red"
        ]
      ],
      "next": [
        "blue",
        "green",
        "yellow"
      ],
      "nextPoints": [
        {
          "x": 8,
          "y": 8
        },
        {
          "x": 0,
          "y": 8
        },
        {
          "x": 8,
          "y": 0
        }
      ]
    },
    "from": {
      "x": 7,
      "y": 2
    },
    "to": {
      "x": 3,
      "y": 4
    },
    "expectedCleared": 5,
    "text": {
      "en": {
        "title": "Fill the gap first",
        "explanation": "Four matching balls are split by one gap. Filling it clears five immediately and avoids a new spawn on this turn.",
        "hint": "Put the distant red ball in the gap at column 4, row 5.",
        "success": "The gap became a five-ball line. An immediate clear also gives you a turn without new balls.",
        "wrong": "Look inside the broken row: its single gap matters more than extending an end."
      },
      "ru": {
        "title": "Сначала заполните разрыв",
        "explanation": "Четыре одинаковых шара разделены одной клеткой. Заполните её: пятёрка исчезнет, и в этот ход новые шары не появятся.",
        "hint": "Поставьте дальний красный шар в разрыв: столбец 4, строка 5.",
        "success": "Разрыв превратился в пятёрку. Немедленная линия также даёт ход без новых шаров.",
        "wrong": "Ищите пустую клетку внутри ряда: сейчас она важнее продолжения с края."
      }
    }
  },
  {
    "id": "diagonal",
    acceptedTargets: [{ x: 5, y: 5 }, { x: 0, y: 0 }],
    "setup": {
      "balls": [
        [
          1,
          1,
          "red"
        ],
        [
          2,
          2,
          "red"
        ],
        [
          3,
          3,
          "red"
        ],
        [
          4,
          4,
          "red"
        ],
        [
          7,
          2,
          "red"
        ]
      ],
      "next": [
        "blue",
        "green",
        "yellow"
      ],
      "nextPoints": [
        {
          "x": 8,
          "y": 8
        },
        {
          "x": 0,
          "y": 8
        },
        {
          "x": 8,
          "y": 0
        }
      ]
    },
    "from": {
      "x": 7,
      "y": 2
    },
    "to": {
      "x": 5,
      "y": 5
    },
    "expectedCleared": 5,
    "text": {
      "en": {
        "title": "Scan the diagonals",
        "explanation": "A diagonal counts just like a row or column. Look along both sloping directions before starting a new pattern.",
        "hint": "Extend the diagonal to column 6, row 6 with the distant red ball.",
        "success": "Five diagonal balls cleared. Include diagonals in your scan for immediate opportunities.",
        "wrong": "The opportunity slopes across the board. Extend that diagonal to five."
      },
      "ru": {
        "title": "Проверяйте диагонали",
        "explanation": "Диагональ считается так же, как строка или столбец. Проверяйте оба наклонных направления, прежде чем начинать новый рисунок.",
        "hint": "Продолжите диагональ дальним красным шаром: столбец 6, строка 6.",
        "success": "Диагональная пятёрка исчезла. Проверяйте диагонали в поиске готовых возможностей.",
        "wrong": "Нужная цепочка идёт наискосок. Продолжите эту диагональ до пяти."
      }
    }
  },
  {
    "id": "cross",
    "setup": {
      "balls": [
        [
          2,
          2,
          "red"
        ],
        [
          3,
          3,
          "red"
        ],
        [
          5,
          5,
          "red"
        ],
        [
          6,
          6,
          "red"
        ],
        [
          2,
          6,
          "red"
        ],
        [
          3,
          5,
          "red"
        ],
        [
          5,
          3,
          "red"
        ],
        [
          6,
          2,
          "red"
        ],
        [
          0,
          0,
          "red"
        ]
      ],
      "next": [
        "blue",
        "green",
        "yellow"
      ],
      "nextPoints": [
        {
          "x": 8,
          "y": 8
        },
        {
          "x": 0,
          "y": 8
        },
        {
          "x": 8,
          "y": 0
        }
      ]
    },
    "from": {
      "x": 0,
      "y": 0
    },
    "to": {
      "x": 4,
      "y": 4
    },
    "expectedCleared": 9,
    "text": {
      "en": {
        "title": "Use a shared intersection",
        "explanation": "One empty cell belongs to two almost-complete lines. Filling the intersection clears both; the shared ball is counted once.",
        "hint": "Move the red corner ball to the central intersection, column 5, row 5.",
        "success": "Both lines cleared: nine distinct balls. Look for cells shared by more than one pattern.",
        "wrong": "The central gap completes two lines together. Aim for their intersection."
      },
      "ru": {
        "title": "Используйте пересечение",
        "explanation": "Одна пустая клетка принадлежит двум почти готовым линиям. Заполнение пересечения уберёт обе; общий шар считается один раз.",
        "hint": "Переместите красный шар из угла в пересечение: столбец 5, строка 5.",
        "success": "Обе линии исчезли: девять разных шаров. Ищите общие клетки нескольких рисунков.",
        "wrong": "Центральный разрыв завершает сразу две линии. Используйте их пересечение."
      }
    }
  },
  {
    "id": "corridor",
    "setup": {
      "balls": [
        [
          4,
          0,
          "red"
        ],
        [
          4,
          1,
          "green"
        ],
        [
          4,
          2,
          "red"
        ],
        [
          4,
          3,
          "green"
        ],
        [
          4,
          5,
          "green"
        ],
        [
          4,
          6,
          "red"
        ],
        [
          4,
          7,
          "green"
        ],
        [
          4,
          8,
          "red"
        ],
        [
          4,
          4,
          "blue"
        ],
        [
          1,
          4,
          "red"
        ]
      ],
      "next": [
        "blue",
        "green",
        "yellow"
      ],
      "nextPoints": [
        {
          "x": 8,
          "y": 8
        },
        {
          "x": 0,
          "y": 8
        },
        {
          "x": 8,
          "y": 0
        }
      ]
    },
    "from": {
      "x": 4,
      "y": 4
    },
    "to": {
      "x": 6,
      "y": 6
    },
    "expectedCleared": 0,
    "text": {
      "en": {
        "title": "Open a passage",
        "explanation": "The occupied central cell seals a wall from top to bottom. Move its blue blocker aside so later balls can cross through the opening.",
        "hint": "Move the blue ball from column 5, row 5 to column 7, row 7.",
        "success": "The passage is open. Keeping routes available can matter even when a move scores nothing.",
        "wrong": "This move leaves the wall sealed. Move the blue blocker away from the central passage."
      },
      "ru": {
        "title": "Освободите проход",
        "explanation": "Занятая центральная клетка замыкает стену сверху донизу. Уберите синий шар, чтобы следующие шары могли пройти через отверстие.",
        "hint": "Уберите синий шар из столбца 5, строки 5 в столбец 7, строку 7.",
        "success": "Проход открыт. Сохранение маршрутов бывает важным даже без очков за ход.",
        "wrong": "Стена осталась закрытой. Уберите синий шар из центрального прохода."
      }
    }
  },
  {
    "id": "preview",
    "setup": {
      "balls": [
        [
          1,
          4,
          "red"
        ],
        [
          2,
          4,
          "red"
        ],
        [
          3,
          4,
          "red"
        ],
        [
          7,
          2,
          "red"
        ]
      ],
      "next": [
        "red",
        "blue",
        "green"
      ],
      "nextPoints": [
        {
          "x": 5,
          "y": 4
        },
        {
          "x": 8,
          "y": 8
        },
        {
          "x": 0,
          "y": 8
        }
      ]
    },
    "from": {
      "x": 7,
      "y": 2
    },
    "to": {
      "x": 4,
      "y": 4
    },
    "expectedCleared": 5,
    "text": {
      "en": {
        "title": "Plan with the preview",
        "explanation": "The marked next red ball will land at column 6, row 5 if that cell stays free. Build a four-ball row beside it so the announced spawn completes five.",
        "hint": "Move the distant red ball to column 5, row 5; leave the next red ball’s cell empty.",
        "success": "The announced red spawn completed your five. Preview planning works when you preserve the marked landing cell.",
        "wrong": "Build four beside the marked red spawn. Do not occupy its landing cell."
      },
      "ru": {
        "title": "Планируйте по прогнозу",
        "explanation": "Следующий красный шар отмечен в столбце 6, строке 5 и появится там, если клетка свободна. Соберите рядом четвёрку, чтобы появление завершило пятёрку.",
        "hint": "Переместите дальний красный шар в столбец 5, строку 5; клетку следующего красного оставьте пустой.",
        "success": "Предсказанный красный шар завершил пятёрку. Такой план работает, пока отмеченная клетка свободна.",
        "wrong": "Соберите четвёрку рядом с будущим красным шаром. Не занимайте его клетку."
      }
    }
  }
];

/** Relocate the blocker without plugging either mouth of the one-cell passage. */
export const STRATEGY_LESSONS: readonly StrategyLesson[] = LESSONS.map(lesson => lesson.id !== 'corridor' ? lesson : {
  ...lesson,
  acceptedTargets: Array.from({ length: 81 }, (_, i) => ({ x: i % 9, y: Math.floor(i / 9) }))
    .filter(p => p.x !== 4 && !(p.y === 4 && (p.x === 3 || p.x === 5)) &&
      !lesson.setup.balls.some(([x, y]) => x === p.x && y === p.y)),
});

export function strategyEngine(lesson: StrategyLesson): GameEngine {
  return stepEngine({ id: 'move', info: false, setup: lesson.setup }, () => 0.5)!;
}

/** Exercises may accept equivalent targets; hints show one example. Screens reset retry boards. */
export function judgeStrategy(lesson: StrategyLesson, from: Point, to: Point, success: boolean, cleared: number): 'done' | 'retry' {
  return success && from.x === lesson.from.x && from.y === lesson.from.y && (lesson.acceptedTargets ?? [lesson.to]).some(target => to.x === target.x && to.y === target.y) && cleared === lesson.expectedCleared ? 'done' : 'retry';
}
