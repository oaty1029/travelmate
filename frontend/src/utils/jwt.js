/**
 * 轻量 JWT 解码工具 — 仅依赖浏览器原生 atob，无第三方库
 * 用于前端路由守卫：从 token 解析出 userId / role / exp，
 * 避免直接信任 sessionStorage 中的 userInfo.role（可被手工篡改）
 */

/** 从 base64url 解码成字符串 */
function b64urlDecode(str) {
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  while (base64.length % 4) base64 += '='
  // 处理 UTF-8
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return new TextDecoder().decode(bytes)
}

/** 解码 JWT payload 为 Object；失败返回 null */
export function decodeJwt(token) {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    return JSON.parse(b64urlDecode(parts[1]))
  } catch (e) {
    return null
  }
}

/** 判断 token 是否已过期 */
export function isTokenExpired(token) {
  const payload = decodeJwt(token)
  if (!payload) return true
  if (!payload.exp) return false
  return Date.now() >= payload.exp * 1000
}
