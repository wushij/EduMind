<template>
  <div class="learning-path-page" v-loading="loading">
    <LearningSubpageHero
      title="知识图谱学习路径"
      subtitle="基于薄弱考点与先修关系自动生成周计划，引导你完成章节学习、练习、错题攻坚与 AI 巩固"
    >
      <template #actions>
        <button type="button" class="capsule-switch-btn" @click="router.push('/learning')">
          <el-icon class="btn-icon"><Back /></el-icon>
          <span>返回我的学习</span>
        </button>
      </template>
      <template #toolbar>
        <el-select
          v-if="courseOptions.length"
          :model-value="courseId"
          placeholder="选择课程"
          style="width: 220px"
          @change="onCourseChange"
        >
          <el-option
            v-for="c in courseOptions"
            :key="c.id"
            :label="c.name || (c as any).title || ('课程 #' + c.id)"
            :value="c.id"
          />
        </el-select>
        <el-select
          v-if="canPickStudent && studentOptions.length"
          :model-value="studentId ?? undefined"
          placeholder="选择学员"
          style="width: 200px"
          @change="onStudentChange"
        >
          <el-option
            v-for="s in studentOptions"
            :key="s.studentId"
            :label="s.realName || s.username || `学员 ${s.studentId}`"
            :value="s.studentId"
          />
        </el-select>
        <button type="button" class="capsule-btn capsule-btn--default" @click="router.push('/learning/practice')">
          <el-icon class="btn-icon"><MagicStick /></el-icon>
          <span>AI 练习</span>
        </button>
        <button type="button" class="capsule-btn capsule-btn--primary" :disabled="loading" @click="loadPath">
          <el-icon class="btn-icon"><Refresh /></el-icon>
          <span>刷新路径</span>
        </button>
      </template>
      <template #extra>
        <LearningPathHeroStats
          v-if="detail"
          :overall-progress-percent="detail.overallProgressPercent ?? 0"
          :weak-point-count="detail.weakPointCount ?? 0"
          :graph-gap-count="detail.graphGapCount ?? 0"
          :estimated-total-minutes="detail.estimatedTotalMinutes ?? 0"
        />
      </template>
    </LearningSubpageHero>

    <el-empty
      v-if="!loading && !hasEnrolledCourses"
      description="你还没有加入任何课程，先去课程中心选课吧"
    >
      <el-button type="primary" @click="router.push('/course')">前往课程中心</el-button>
    </el-empty>

    <template v-else>
      <div class="path-main-grid">
        <LearningPathGraphPanel :graph-slice="detail?.graphSlice" />
        <div class="path-plan-column">
          <div class="plan-panel">
            <div class="plan-panel-header">
              <h4>{{ detail?.title || '自适应推荐学习路径' }}</h4>
              <el-tag size="small" type="success">规则引擎动态生成</el-tag>
            </div>
            <LearningPathWeekGrid
              :weeks="detail?.weeks ?? []"
              @execute-task="executeTask"
            />
          </div>
        </div>
      </div>

      <LearningPathInterpretPanel
        :hint="detail?.interpretHint"
        :generated-at="detail?.generatedAt"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Back, MagicStick, Refresh } from '@element-plus/icons-vue';
import LearningSubpageHero from '@/components/learning/LearningSubpageHero.vue';
import LearningPathHeroStats from '@/components/learning/path/LearningPathHeroStats.vue';
import LearningPathGraphPanel from '@/components/learning/path/LearningPathGraphPanel.vue';
import LearningPathWeekGrid from '@/components/learning/path/LearningPathWeekGrid.vue';
import LearningPathInterpretPanel from '@/components/learning/path/LearningPathInterpretPanel.vue';
import { useLearningPathPage } from '@/composables/learning/useLearningPathPage';

const router = useRouter();
const {
  loading,
  detail,
  courseOptions,
  courseId,
  studentId,
  studentOptions,
  canPickStudent,
  hasEnrolledCourses,
  loadPath,
  onCourseChange,
  onStudentChange,
  executeTask
} = useLearningPathPage();
</script>

<style scoped lang="scss">
.learning-path-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.path-main-grid {
  display: grid;
  grid-template-columns: 42% 58%;
  gap: 18px;
  align-items: start;
}

.plan-panel {
  background: #fff;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  padding: 18px 20px;

  .plan-panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
    }
  }
}

.capsule-switch-btn,
.capsule-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 9999px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #334155;

  .btn-icon {
    font-size: 16px;
  }

  &--primary {
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
    border-color: transparent;
    color: #fff;
  }

  &--default:hover,
  &.capsule-switch-btn:hover {
    background: #f8fafc;
  }
}

@media (max-width: 1100px) {
  .path-main-grid {
    grid-template-columns: 1fr;
  }
}
</style>
