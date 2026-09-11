/**
 * 前端国密与接口安全工具
 * 规范：时间戳 (Timestamp) + 防重放 (Nonce) + SM3-HMAC 签名与 SM4-GCM 认证加密
 */

/**
 * 生成 32 位防重放随机数 (Nonce)
 */
export function generateNonce(): string {
  const chars = '0123456789abcdef';
  let nonce = '';
  for (let i = 0; i < 32; i++) {
    nonce += chars[Math.floor(Math.random() * chars.length)];
  }
  return nonce;
}

/**
 * 获取当前毫秒时间戳 (13 位)
 */
export function getTimestamp(): number {
  return Date.now();
}

/**
 * 构建规范待签名字符串 (Canonical String)
 * 格式：METHOD + "\n" + PATH + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY
 */
export function buildCanonicalString(
  method: string,
  path: string,
  timestamp: number,
  nonce: string,
  body: string = ''
): string {
  return `${(method || 'GET').toUpperCase()}\n${path || '/'}\n${timestamp}\n${nonce || ''}\n${body || ''}`;
}

// ==========================================
// 国密 SM3 密码杂凑算法标准实现 (GM/T 0004-2012)
// ==========================================

function leftRotate(x: number, n: number): number {
  return ((x << n) | (x >>> (32 - n))) >>> 0;
}

function sm3P0(x: number): number {
  return (x ^ leftRotate(x, 9) ^ leftRotate(x, 17)) >>> 0;
}

function sm3P1(x: number): number {
  return (x ^ leftRotate(x, 15) ^ leftRotate(x, 23)) >>> 0;
}

function sm3FF(x: number, y: number, z: number, j: number): number {
  return (j >= 0 && j <= 15 ? x ^ y ^ z : (x & y) | (x & z) | (y & z)) >>> 0;
}

function sm3GG(x: number, y: number, z: number, j: number): number {
  return (j >= 0 && j <= 15 ? x ^ y ^ z : (x & y) | (~x & z)) >>> 0;
}

function sm3T(j: number): number {
  return j >= 0 && j <= 15 ? 0x79cc4519 : 0x7a879d8a;
}

function stringToUtf8ByteArray(str: string): number[] {
  const bytes: number[] = [];
  for (let i = 0; i < str.length; i++) {
    let c = str.charCodeAt(i);
    if (c < 0x80) {
      bytes.push(c);
    } else if (c < 0x800) {
      bytes.push(0xc0 | (c >> 6));
      bytes.push(0x80 | (c & 0x3f));
    } else if (c < 0xd800 || c >= 0xe000) {
      bytes.push(0xe0 | (c >> 12));
      bytes.push(0x80 | ((c >> 6) & 0x3f));
      bytes.push(0x80 | (c & 0x3f));
    } else {
      i++;
      c = 0x10000 + (((c & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff));
      bytes.push(0xf0 | (c >> 18));
      bytes.push(0x80 | ((c >> 12) & 0x3f));
      bytes.push(0x80 | ((c >> 6) & 0x3f));
      bytes.push(0x80 | (c & 0x3f));
    }
  }
  return bytes;
}

/**
 * 计算输入的国密 SM3 杂凑值（返回 64 位 16 进制字符串）
 */
export function sm3(input: string | number[]): string {
  const msg = typeof input === 'string' ? stringToUtf8ByteArray(input) : input;
  const bitLen = msg.length * 8;

  // 1. 填充报文
  const padded = [...msg, 0x80];
  while ((padded.length % 64) !== 56) {
    padded.push(0x00);
  }

  // 附加 64 位长度 (大端)
  for (let i = 7; i >= 0; i--) {
    padded.push(Math.floor((bitLen / Math.pow(2, i * 8)) % 256));
  }

  // 2. 初始向量 IV
  let V = [
    0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600,
    0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e
  ];

  // 3. 迭代压缩
  const blockCount = padded.length / 64;
  for (let b = 0; b < blockCount; b++) {
    const block = padded.slice(b * 64, (b + 1) * 64);
    const W = new Array(68);
    const W1 = new Array(64);

    for (let i = 0; i < 16; i++) {
      W[i] = ((block[i * 4] << 24) | (block[i * 4 + 1] << 16) | (block[i * 4 + 2] << 8) | block[i * 4 + 3]) >>> 0;
    }
    for (let j = 16; j < 68; j++) {
      W[j] = (sm3P1(W[j - 16] ^ W[j - 9] ^ leftRotate(W[j - 3], 15)) ^ leftRotate(W[j - 13], 7) ^ W[j - 6]) >>> 0;
    }
    for (let j = 0; j < 64; j++) {
      W1[j] = (W[j] ^ W[j + 4]) >>> 0;
    }

    let [A, B, C, D, E, F, G, H] = V;

    for (let j = 0; j < 64; j++) {
      const SS1 = leftRotate((leftRotate(A, 12) + E + leftRotate(sm3T(j), j % 32)) >>> 0, 7);
      const SS2 = (SS1 ^ leftRotate(A, 12)) >>> 0;
      const TT1 = (sm3FF(A, B, C, j) + D + SS2 + W1[j]) >>> 0;
      const TT2 = (sm3GG(E, F, G, j) + H + SS1 + W[j]) >>> 0;
      D = C;
      C = leftRotate(B, 9);
      B = A;
      A = TT1;
      H = G;
      G = leftRotate(F, 19);
      F = E;
      E = sm3P0(TT2);
    }

    V = [
      (V[0] ^ A) >>> 0,
      (V[1] ^ B) >>> 0,
      (V[2] ^ C) >>> 0,
      (V[3] ^ D) >>> 0,
      (V[4] ^ E) >>> 0,
      (V[5] ^ F) >>> 0,
      (V[6] ^ G) >>> 0,
      (V[7] ^ H) >>> 0
    ];
  }

  return V.map(v => v.toString(16).padStart(8, '0')).join('');
}

/**
 * 国密 SM3-HMAC 消息认证码实现
 * @param secretKey 密钥字符串
 * @param content   待认证内容
 * @returns 64 位 16 进制签名字符串
 */
export function sm3Hmac(secretKey: string, content: string): string {
  let keyBytes = stringToUtf8ByteArray(secretKey);
  const blockSize = 64;

  if (keyBytes.length > blockSize) {
    const hashHex = sm3(secretKey);
    keyBytes = [];
    for (let i = 0; i < hashHex.length; i += 2) {
      keyBytes.push(parseInt(hashHex.substring(i, i + 2), 16));
    }
  }

  while (keyBytes.length < blockSize) {
    keyBytes.push(0x00);
  }

  const ipad = new Array(blockSize);
  const opad = new Array(blockSize);
  for (let i = 0; i < blockSize; i++) {
    ipad[i] = keyBytes[i] ^ 0x36;
    opad[i] = keyBytes[i] ^ 0x5c;
  }

  const contentBytes = stringToUtf8ByteArray(content);
  const innerMsg = [...ipad, ...contentBytes];
  const innerHashHex = sm3(innerMsg);

  const innerHashBytes: number[] = [];
  for (let i = 0; i < innerHashHex.length; i += 2) {
    innerHashBytes.push(parseInt(innerHashHex.substring(i, i + 2), 16));
  }

  const outerMsg = [...opad, ...innerHashBytes];
  return sm3(outerMsg);
}

/**
 * 构建并生成完整的 SM3-HMAC 请求签名
 */
export function generateRequestSignature(
  method: string,
  path: string,
  timestamp: number,
  nonce: string,
  body: string = '',
  secretKey: string = 'EduMind_Platform_SecretKey_2026'
): string {
  const canonical = buildCanonicalString(method, path, timestamp, nonce, body);
  return sm3Hmac(secretKey, canonical);
}
