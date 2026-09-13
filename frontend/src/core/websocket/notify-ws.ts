import { tokenUtil } from '@/core/auth/token';
import { API_BASE_URL } from '@/config/api';

export interface NotificationPush {
  id: number;
  type: string;
  title: string;
  content: string;
  refId?: number;
  priority?: number;
  isRead: number;
  createTime: string;
  unreadCount: number;
}

type PushHandler = (payload: NotificationPush) => void;

let ws: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let reconnectDelay = 1000;
const maxReconnectDelay = 30000;
const handlers = new Set<PushHandler>();
let intentionalClose = false;

function buildWsUrl(token: string): string {
  const apiBase = API_BASE_URL || '/api';
  let protocol: string;
  let host: string;
  let pathPrefix: string;

  if (/^https?:\/\//i.test(apiBase)) {
    const parsed = new URL(apiBase);
    protocol = parsed.protocol === 'https:' ? 'wss:' : 'ws:';
    host = parsed.host;
    pathPrefix = parsed.pathname.replace(/\/$/, '');
  } else {
    protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    host = window.location.host;
    pathPrefix = apiBase.replace(/\/$/, '');
  }

  return `${protocol}//${host}${pathPrefix}/ws/notify?token=${encodeURIComponent(token)}`;
}

function detachSocketHandlers(socket: WebSocket): void {
  socket.onopen = null;
  socket.onmessage = null;
  socket.onerror = null;
  socket.onclose = null;
}

export function onNotifyPush(handler: PushHandler): () => void {
  handlers.add(handler);
  return () => handlers.delete(handler);
}

export function connectNotifyWs(): void {
  const token = tokenUtil.get();
  if (!token) return;

  disconnectNotifyWs(false);
  intentionalClose = false;

  let socket: WebSocket;
  try {
    socket = new WebSocket(buildWsUrl(token));
  } catch {
    scheduleReconnect();
    return;
  }

  ws = socket;

  socket.onopen = () => {
    if (ws !== socket) return;
    reconnectDelay = 1000;
  };

  socket.onmessage = (event) => {
    if (ws !== socket) return;
    try {
      const msg = JSON.parse(String(event.data));
      if (msg?.type === 'notification' && msg.data) {
        const data = msg.data;
        handlers.forEach((handler) =>
          handler({
            id: data.id,
            type: data.type,
            title: data.title,
            content: data.content,
            refId: data.refId ?? data.ref_id,
            priority: data.priority ?? 0,
            isRead: data.isRead ?? data.is_read ?? 0,
            createTime: data.createTime ?? data.created_at ?? '',
            unreadCount: data.unreadCount ?? data.unread_count ?? 0
          })
        );
      }
    } catch {
      /* ignore malformed frames */
    }
  };

  socket.onerror = () => {
    if (ws !== socket) return;
    socket.close();
  };

  socket.onclose = () => {
    if (ws === socket) {
      ws = null;
    }
    if (!intentionalClose && tokenUtil.get()) {
      scheduleReconnect();
    }
  };
}

function scheduleReconnect(): void {
  if (reconnectTimer || intentionalClose) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    if (intentionalClose || !tokenUtil.get()) return;
    reconnectDelay = Math.min(reconnectDelay * 2, maxReconnectDelay);
    connectNotifyWs();
  }, reconnectDelay);
}

export function disconnectNotifyWs(clearHandlers = true): void {
  intentionalClose = true;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  const socket = ws;
  ws = null;
  if (socket) {
    detachSocketHandlers(socket);
    socket.close();
  }
  if (clearHandlers) {
    handlers.clear();
  }
}

export function isNotifyWsConnected(): boolean {
  return ws?.readyState === WebSocket.OPEN;
}
