import { createDefaultObjectiveCallout } from '@/composables/course/useLessonDocumentModel';
import type { LessonMetaForm } from '@/components/course/lesson/studio/LessonStudioSidebar.vue';
import type { LessonStudioDocument } from '@/composables/course/useLessonDocumentModel';

export type LessonEditBackupPayload = {
  meta?: LessonMetaForm;
  studio?: Pick<
    LessonStudioDocument,
    'mainMarkdown' | 'objectiveCallout' | 'extraBlocks' | 'knowledgePointIds'
  >;
};

export function fingerprintLessonEditState(
  meta: LessonMetaForm,
  studio: Pick<
    LessonStudioDocument,
    'mainMarkdown' | 'objectiveCallout' | 'extraBlocks' | 'knowledgePointIds'
  >
): string {
  return JSON.stringify({
    title: meta.title?.trim() ?? '',
    description: meta.description?.trim() ?? '',
    durationMinutes: meta.durationMinutes,
    lessonType: meta.lessonType,
    mainMarkdown: studio.mainMarkdown?.trim() ?? '',
    objectiveTitle: studio.objectiveCallout?.title?.trim() ?? '',
    objectiveBody: studio.objectiveCallout?.body?.trim() ?? '',
    extraBlocks: studio.extraBlocks ?? [],
    knowledgePointIds: [...(studio.knowledgePointIds ?? [])].sort((a, b) => a - b)
  });
}

/** 仅含默认占位、无实质内容的本地备份（多为初始化误写入） */
export function isTrivialLessonEditBackup(payload: LessonEditBackupPayload): boolean {
  const title = payload.meta?.title?.trim() ?? '';
  const md = payload.studio?.mainMarkdown?.trim() ?? '';
  if (title || md) return false;

  const defaultObjective = createDefaultObjectiveCallout();
  const body = payload.studio?.objectiveCallout?.body?.trim() ?? '';
  const objectiveOk =
    !body || body === defaultObjective.body.trim();
  const extras = payload.studio?.extraBlocks?.length ?? 0;
  const kps = payload.studio?.knowledgePointIds?.length ?? 0;
  return objectiveOk && extras === 0 && kps === 0;
}

export function shouldOfferLessonEditBackupRestore(
  backupPayload: LessonEditBackupPayload,
  currentMeta: LessonMetaForm,
  currentStudio: Pick<
    LessonStudioDocument,
    'mainMarkdown' | 'objectiveCallout' | 'extraBlocks' | 'knowledgePointIds'
  >
): boolean {
  if (isTrivialLessonEditBackup(backupPayload)) {
    return false;
  }
  const backupMeta = backupPayload.meta ?? currentMeta;
  const backupStudio = backupPayload.studio ?? currentStudio;
  const backupFp = fingerprintLessonEditState(backupMeta, backupStudio);
  const currentFp = fingerprintLessonEditState(currentMeta, currentStudio);
  return backupFp !== currentFp;
}
