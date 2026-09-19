import { ref, onUnmounted } from 'vue';

export function usePracticeTimer() {
  const usedSeconds = ref(0);
  let timer: ReturnType<typeof setInterval> | null = null;
  let startMs = 0;

  function start() {
    stop();
    startMs = Date.now();
    usedSeconds.value = 0;
    timer = setInterval(() => {
      usedSeconds.value = Math.max(0, Math.round((Date.now() - startMs) / 1000));
    }, 1000);
  }

  function stop() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
    if (startMs > 0) {
      usedSeconds.value = Math.max(1, Math.round((Date.now() - startMs) / 1000));
    }
  }

  onUnmounted(stop);

  return { usedSeconds, start, stop };
}
