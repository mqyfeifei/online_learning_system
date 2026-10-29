/**
 * 答卷解析展示工具：把答案字母翻译成「字母. 选项文本」的可读形式，
 * 并判断每个选项该标绿（正确答案）还是标红（学生选错的）。
 * Exam.vue 的逐题解析与 Grades.vue 的答卷详情弹窗共用。
 */

export interface QuizOption {
  key: string
  text: string
}

/** 选择题才有选项列表 */
export function optionsOf(detail: any): QuizOption[] {
  return ((detail && detail.options) || []) as QuizOption[]
}

export function isChoice(detail: any): boolean {
  return detail?.type === 1 || detail?.type === 2
}

/** 是不是客观题里「答案本身就是内容」的类型（判断/填空），无需选项翻译 */
function keyToText(detail: any, key: string): string {
  const opts = optionsOf(detail)
  const hit = opts.find((o) => o.key === key)
  return hit ? `${key}. ${hit.text}` : key
}

/**
 * 把答案串翻译成可读文本：
 * - 单选/多选：C → 「C. 选项内容」，多选 A,B → 「A. xx、B. xx」
 * - 判断：T/F → 正确/错误
 * - 填空：多空 | 分隔，展示为空1｜空2
 */
export function answerLabel(detail: any, ans?: string | null): string {
  const a = (ans || '').trim()
  if (!a) return ''
  if (detail?.type === 3) {
    return a.toUpperCase() === 'T' ? '正确' : '错误'
  }
  if (isChoice(detail)) {
    return a
      .split(',')
      .map((k) => keyToText(detail, k.trim()))
      .join('、')
  }
  if (detail?.type === 4) {
    return a.split('|').join(' ｜ ')
  }
  return a
}

/** 该选项是否是正确答案之一 */
export function isRightKey(detail: any, key: string): boolean {
  return (detail?.rightAnswer || '')
    .split(',')
    .map((s: string) => s.trim())
    .includes(key)
}

/** 该选项是否是学生选的（且选错了，选对的话直接标绿即可） */
export function isWrongPick(detail: any, key: string): boolean {
  const yours = (detail?.yourAnswer || '')
    .split(',')
    .map((s: string) => s.trim())
  return yours.includes(key) && !isRightKey(detail, key)
}
