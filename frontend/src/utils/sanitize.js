/**
 * HTML 消毒 — 使用 DOMPurify 库（npm install dompurify 已装）
 * 解决 BUG-HIGH-01：v-html 存在 XSS 风险
 *
 * DOMPurify 比自定义 DOMParser 方案更全面：
 * - 内置 SVG/MathML/自定义元素支持（按需开启）
 * - 自动剥离事件处理器、危险协议、HTML 注入
 * - 持续维护，覆盖最新 XSS 向量
 */
import DOMPurify from 'dompurify'

const ALLOWED_TAGS = [
  'p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'br', 'hr',
  'ul', 'ol', 'li',
  'a', 'img',
  'strong', 'em', 'b', 'i', 'u', 's', 'del', 'ins', 'mark',
  'span', 'div', 'blockquote', 'pre', 'code',
  'table', 'thead', 'tbody', 'tr', 'td', 'th',
  'figure', 'figcaption',
]

const ALLOWED_ATTRS = ['href', 'title', 'target', 'rel', 'src', 'alt', 'width', 'height', 'colspan', 'rowspan', 'class', 'style']

/** 新窗口链接强制 noopener，防 tabnabbing */
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.tagName === 'A' && node.getAttribute('target') === '_blank') {
    node.setAttribute('rel', 'noopener noreferrer')
  }
})

/** 消毒 HTML 字符串，返回安全的 HTML */
export function sanitizeHtml(html) {
  if (!html) return ''
  if (typeof html !== 'string') return ''

  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR: ALLOWED_ATTRS,
    ALLOWED_ATTR_TAGS: {
      a: ['href', 'title', 'target', 'rel', 'class'],
      img: ['src', 'alt', 'title', 'width', 'height', 'class'],
      td: ['colspan', 'rowspan', 'class'],
      th: ['colspan', 'rowspan', 'class'],
    },
    // DOMPurify 默认已经剥离所有事件处理器和危险协议
  })
}
