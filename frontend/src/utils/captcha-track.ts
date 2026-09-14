export interface CaptchaTrackEvent {
  x: number;
  y: number;
  t: number;
}

export function createTrackRecorder() {
  const events: CaptchaTrackEvent[] = [];
  let startAt = 0;
  let lastX = 0;
  let lastMoveAt = 0;

  function start(startX: number, startY: number) {
    events.length = 0;
    startAt = Date.now();
    lastMoveAt = startAt;
    lastX = 0;
    events.push({ x: 0, y: 0, t: 0 });
    move(startX, startY, true);
  }

  function move(clientX: number, clientY: number, force = false) {
    const now = Date.now();
    if (!force && now - lastMoveAt < 16) return;
    lastMoveAt = now;
    const x = Math.max(0, Math.round(clientX));
    const y = Math.round(clientY);
    lastX = x;
    events.push({ x, y, t: now - startAt });
  }

  function end() {
    const durationMs = Math.max(0, Date.now() - startAt);
    return {
      events,
      durationMs,
      offsetX: lastX
    };
  }

  return { start, move, end };
}
