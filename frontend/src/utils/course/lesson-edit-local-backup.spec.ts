import { describe, expect, it } from 'vitest';
import {
  fingerprintLessonEditState,
  isTrivialLessonEditBackup,
  shouldOfferLessonEditBackupRestore
} from '@/utils/course/lesson-edit-local-backup';

const baseMeta = {
  title: '课节 A',
  description: '导读',
  durationMinutes: 30,
  lessonType: 'LECTURE'
};

describe('lesson-edit-local-backup', () => {
  it('treats empty meta-only backup as trivial', () => {
    expect(
      isTrivialLessonEditBackup({
        meta: { title: '', description: '', durationMinutes: 30, lessonType: 'LECTURE' },
        studio: {
          mainMarkdown: '',
          objectiveCallout: { title: '学习目标', body: '学完本节后，你将能够掌握本课核心概念并完成相关练习。' },
          extraBlocks: [],
          knowledgePointIds: []
        }
      })
    ).toBe(true);
  });

  it('does not offer restore when backup matches server state', () => {
    const studio = {
      mainMarkdown: '# 正文',
      objectiveCallout: { title: '学习目标', body: '目标' },
      extraBlocks: [],
      knowledgePointIds: [1]
    };
    const fp = fingerprintLessonEditState(baseMeta, studio);
    expect(fp).toBe(fingerprintLessonEditState(baseMeta, studio));
    expect(
      shouldOfferLessonEditBackupRestore({ meta: baseMeta, studio }, baseMeta, studio)
    ).toBe(false);
  });

  it('offers restore when backup has newer edits', () => {
    const serverStudio = {
      mainMarkdown: '',
      objectiveCallout: { title: '学习目标', body: '' },
      extraBlocks: [],
      knowledgePointIds: []
    };
    const backupStudio = {
      mainMarkdown: '用户写的正文',
      objectiveCallout: { title: '学习目标', body: '' },
      extraBlocks: [],
      knowledgePointIds: []
    };
    expect(
      shouldOfferLessonEditBackupRestore(
        { meta: baseMeta, studio: backupStudio },
        baseMeta,
        serverStudio
      )
    ).toBe(true);
  });
});
