/**
 * 文件下载/预览地址工具
 *
 * 浏览器直接打开链接（window.open / a[href]）带不上 Authorization 头，
 * 后端 JwtInterceptor 支持 ?token= 兜底，这里统一把 token 拼上去，
 * 免得每个页面各写一遍。
 */

export function withToken(url: string): string {
  const token = localStorage.getItem('token')
  if (!token) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}token=${encodeURIComponent(token)}`
}

/** 新窗口打开（下载或预览） */
export function openFile(url: string) {
  window.open(withToken(url), '_blank')
}

/** 用 a 标签触发下载，文件名由后端 Content-Disposition 决定 */
export function triggerDownload(url: string) {
  const a = document.createElement('a')
  a.href = withToken(url)
  a.target = '_blank'
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

/** 非课件文件（作业附件、正文图片等）的预览参数：统一走 url 寻址 */
export function previewParamsOf(url: string, name?: string) {
  return { url, name: name || '' }
}

/** 从 /files/2026/10/xxx.docx 里推定展示名（去掉随机前缀） */
export function fileNameOf(url: string): string {
  if (!url) return '附件'
  const clean = url.split('?')[0].replace(/\\/g, '/')
  const base = clean.substring(clean.lastIndexOf('/') + 1)
  const dot = base.indexOf('.')
  const ext = dot > -1 ? base.substring(base.lastIndexOf('.')) : ''
  // 上传后的文件名是 32 位 uuid + 扩展名
  if (base.length > 33 && base.charAt(32) === '.') {
    return '附件' + ext
  }
  return base
}

/** 是否是可直接用 <img> 渲染的图片 */
export function isImageUrl(url: string): boolean {
  return /\.(png|jpe?g|gif|bmp|webp|svg)$/i.test((url || '').split('?')[0])
}
