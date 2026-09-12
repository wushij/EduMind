import type { Router } from 'vue-router';
import type { AITool } from '@/types/ai/tool';
import { recordToolUse } from '@/api/ai/tools';

export function launchAITool(tool: AITool, router: Router) {
  recordToolUse(tool.id).catch(() => {
    // 统计失败不阻塞跳转
  });

  if (tool.executionMode === 'V05_NOTICE') {
    router.push(`/ai/marketplace/v05/${tool.id}`);
    return;
  }

  router.push(tool.route);
}
