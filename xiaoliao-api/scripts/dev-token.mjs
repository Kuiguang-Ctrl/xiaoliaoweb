#!/usr/bin/env node
/**
 * 本地开发签 JWT 工具 — 配合 dev 鉴权（auth.enabled: true）使用
 *
 * 用法：
 *   node scripts/dev-token.mjs                # 用默认 mock 用户
 *   node scripts/dev-token.mjs u1             # 给指定 userId 签 token
 *   node scripts/dev-token.mjs u1 99999       # 指定过期时长（小时）
 *
 * 密钥默认取 application.yml 的 xiaoliao.jwt.secret 默认值；
 * 若本机用环境变量覆盖了密钥（XIAOLIAO_JWT_SECRET），需传一致的值：
 *   XIAOLIAO_JWT_SECRET=<你的密钥> node scripts/dev-token.mjs u1
 *
 * 生成后打开：http://localhost:3000/?token=<token>（前端会自动消费并抹掉 URL 上的 token）
 */
import { createHmac } from 'node:crypto'

const SECRET = process.env.XIAOLIAO_JWT_SECRET || 'xiaoliao-dev-secret-change-in-production-2026'
const DEFAULT_EXPIRE_HOURS = 168 // 与 xiaoliao.jwt.expire-hours 一致

const userId = process.argv[2] || '7764978f-9019-4b2f-b6b1-11862c1a5528'
const expireHours = Number(process.argv[3] || DEFAULT_EXPIRE_HOURS)

// JWT 三段式：header.payload.signature（HS256）
const b64url = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url')

const header = { alg: 'HS256', typ: 'JWT' }
const now = Math.floor(Date.now() / 1000)
const payload = { sub: userId, iat: now, exp: now + expireHours * 3600 }

const unsigned = `${b64url(header)}.${b64url(payload)}`
const signature = createHmac('sha256', SECRET).update(unsigned).digest('base64url')
const token = `${unsigned}.${signature}`

console.log(`userId    : ${userId}`)
console.log(`过期时间  : ${expireHours} 小时后`)
console.log(`JWT       : ${token}`)
console.log(`测试链接  : http://localhost:3000/?token=${token}`)
