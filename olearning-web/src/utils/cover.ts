/**
 * 封面与头像配色
 *
 * 课程没有真实封面图时用渐变兜底，按 id 取色：同一门课在任何页面都是同一个颜色。
 * 头像同理，按用户 id 取色。改配色只改这两个数组。
 */

export const COVER_PALETTE = [
  'linear-gradient(135deg,#ff9a62,#ff6b35)',
  'linear-gradient(135deg,#f6b93b,#e58e26)',
  'linear-gradient(135deg,#7c6cf5,#5a4bcf)',
  'linear-gradient(135deg,#3fb984,#2fa36b)',
  'linear-gradient(135deg,#5aa9e6,#3b82f6)',
  'linear-gradient(135deg,#e8737a,#d64550)',
  'linear-gradient(135deg,#c48b5c,#a06a3c)',
  'linear-gradient(135deg,#8e7cc3,#6a5aa8)'
]

export const AVATAR_COLORS = [
  '#ff8a5c',
  '#f0a52c',
  '#7c6cf5',
  '#2fa36b',
  '#3b82f6',
  '#e8737a',
  '#a06a3c'
]

/** 课程封面渐变；id 为空/非法时退回第一个配色，不抛错 */
export function coverOf(id: unknown): string {
  const n = Number(id)
  const i = Number.isFinite(n) ? Math.abs(Math.trunc(n)) % COVER_PALETTE.length : 0
  return COVER_PALETTE[i]
}

/** 头像底色；按用户 id 取，保证同一个人到处一个色 */
export function avatarColor(seed: unknown): string {
  const n = Number(seed)
  const i = Number.isFinite(n) ? Math.abs(Math.trunc(n)) % AVATAR_COLORS.length : 0
  return AVATAR_COLORS[i]
}
