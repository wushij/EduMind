<template>
  <div class="bank-detail-container">
    <!-- 顶部面包屑与导航条 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/question/banks')">
        返回题库列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/banks' }">题库中心</el-breadcrumb-item>
        <el-breadcrumb-item>{{ bankInfo?.name || '题库详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 题库信息看板 -->
    <div class="bank-hero-card" v-loading="loading">
      <div class="hero-main-content">
        <div class="bank-avatar-badge">
          <el-icon><FolderOpened /></el-icon>
        </div>
        <div class="hero-info">
          <div class="title-row">
            <h1 class="bank-title">{{ bankInfo?.name || '试题库详情' }}</h1>
            <span class="course-pill-tag">{{ bankInfo?.courseName || '未指定课程' }}</span>
            <span class="update-time-tag">更新于 {{ bankInfo?.updateTime || '近期' }}</span>
          </div>
          <p class="bank-description">
            {{ bankInfo?.description || '本题库聚合了课程教学过程中的核心试题、历年真题与经典测验题目，可用于按需检索、日常练习与组卷出题。' }}
          </p>
        </div>
      </div>

      <!-- 右侧指标与操作区 -->
      <div class="hero-actions-dock">
        <div class="metrics-grid">
          <div class="metric-item">
            <span class="metric-val">{{ bankQuestions.length }}</span>
            <span class="metric-label">收录题目数</span>
          </div>
          <div class="metric-item">
            <span class="metric-val">{{ totalScore }}</span>
            <span class="metric-label">卷面参考总分</span>
          </div>
        </div>

        <div class="action-buttons-group">
          <el-button type="primary" class="capsule-btn-main" @click="openAddDrawer">
            <el-icon class="mr-1"><Plus /></el-icon> 挑选题目入库
          </el-button>
          <el-button class="capsule-btn-sub" @click="handleFastComposeExam">
            <el-icon class="mr-1"><DocumentCopy /></el-icon> 基于此题库组卷
          </el-button>
        </div>
      </div>
    </div>

    <!-- 筛选过滤与批量管理工具栏 -->
    <div class="bank-content-card">
      <div class="toolbar-wrapper">
        <div class="toolbar-left">
          <el-input
            v-model="searchKeyword"
            placeholder="在题库内搜索题干关键词、知识点..."
            clearable
            class="search-input"
            :prefix-icon="Search"
          />
          <el-select v-model="filterType" placeholder="题型筛选" clearable class="filter-select">
            <el-option label="全部题型" value="" />
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="填空题" value="FILL_BLANK" />
            <el-option label="简答题" value="SHORT_ANSWER" />
          </el-select>
          <el-select v-model="filterDifficulty" placeholder="难度筛选" clearable class="filter-select">
            <el-option label="全部难度" value="" />
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </div>

        <div class="toolbar-right">
          <el-button
            v-if="selectedRowKeys.length > 0"
            type="danger"
            plain
            size="small"
            @click="handleBatchRemove"
          >
            批量移出题库 ({{ selectedRowKeys.length }})
          </el-button>
          <span class="total-hint-text">共 {{ filteredQuestions.length }} 道符合条件的题目</span>
        </div>
      </div>

      <!-- 题库题目列表 -->
      <div class="questions-list-section">
        <div v-if="filteredQuestions.length > 0" class="question-items-stack">
          <div
            v-for="(item, index) in filteredQuestions"
            :key="item.id"
            class="question-bank-item-card"
          >
            <div class="item-checkbox-col">
              <el-checkbox
                :model-value="selectedRowKeys.includes(item.id)"
                @change="toggleSelectRow(item.id)"
              />
              <span class="index-num">#{{ index + 1 }}</span>
            </div>

            <div class="item-body">
              <div class="item-badges-row">
                <el-tag :type="getTypeTagType(item.type)" effect="light" round size="small">
                  {{ getTypeLabel(item.type) }}
                </el-tag>
                <el-tag :type="getDifficultyTagType(item.difficulty)" effect="plain" round size="small">
                  {{ getDifficultyLabel(item.difficulty) }}
                </el-tag>
                <span class="score-badge">{{ item.score || 5 }} 分</span>
                <span v-if="item.chapterName" class="chapter-badge">{{ item.chapterName }}</span>
              </div>

              <!-- 题干正文 -->
              <MathText :text="item.stem" tag="div" custom-class="stem-content" />

              <!-- 选项列表（若是选择题） -->
              <div v-if="item.options && item.options.length > 0" class="options-container">
                <div
                  v-for="opt in item.options"
                  :key="opt.key"
                  class="option-pill"
                  :class="{ 'option-pill--correct': opt.isCorrect }"
                >
                  <span class="opt-key">{{ opt.key }}.</span>
                  <MathText :text="opt.content" tag="span" custom-class="opt-text" />
                  <span v-if="opt.isCorrect" class="opt-check-icon"><el-icon><Check /></el-icon> 正确项</span>
                </div>
              </div>

              <!-- 解析折叠栏 -->
              <div v-if="expandedAnalyses.includes(item.id)" class="analysis-box">
                <div class="analysis-line">
                  <span class="label">参考答案：</span>
                  <span class="val font-semibold text-emerald-600">{{ item.correctAnswer || '无' }}</span>
                </div>
                <div class="analysis-line">
                  <span class="label">解析说明：</span>
                  <MathText :text="item.analysis || '暂无详细文字解析'" tag="span" custom-class="val" />
                </div>
                <div v-if="item.knowledgePointNames && item.knowledgePointNames.length > 0" class="analysis-line">
                  <span class="label">知识点：</span>
                  <div class="kp-tags">
                    <span v-for="kp in item.knowledgePointNames" :key="kp" class="kp-tag">{{ kp }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧快捷操作按钮 -->
            <div class="item-actions">
              <el-button
                link
                type="primary"
                size="small"
                @click="toggleExpandAnalysis(item.id)"
              >
                {{ expandedAnalyses.includes(item.id) ? '收起答案解析' : '查看答案解析' }}
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click="viewDetailDialog(item)"
              >
                完整题卡
              </el-button>
              <el-popconfirm
                title="确定要将该试题从当前题库移出吗？（原题库库源题不会被删除）"
                confirm-button-text="确定移出"
                cancel-button-text="取消"
                @confirm="handleRemoveQuestion(item.id)"
              >
                <template #reference>
                  <el-button link type="danger" size="small">移出题库</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-questions-card">
          <div class="empty-icon">
            <el-icon><Files /></el-icon>
          </div>
          <h3 class="empty-title">当前题库暂无题目数据</h3>
          <p class="empty-sub">
            您可以从平台的公共试题库中挑选试题批量加入，或者点击上方“挑选题目入库”。
          </p>
          <el-button type="primary" class="mt-4" @click="openAddDrawer">
            挑选题目加入此题库
          </el-button>
        </div>
      </div>
    </div>

    <!-- 抽屉：从公共题库挑选试题入库 -->
    <el-drawer
      v-model="drawerVisible"
      title="挑选试题批量入库"
      size="720px"
      destroy-on-close
    >
      <div class="drawer-content-box">
        <div class="drawer-filter-bar">
          <el-input
            v-model="drawerSearch"
            placeholder="搜索公共试题题干、知识点..."
            clearable
            class="flex-1"
            :prefix-icon="Search"
          />
          <el-select v-model="drawerType" placeholder="题型" clearable style="width: 140px">
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="填空题" value="FILL_BLANK" />
            <el-option label="简答题" value="SHORT_ANSWER" />
          </el-select>
        </div>

        <!-- 候选题目清单 -->
        <div class="candidate-questions-list">
          <div
            v-for="q in candidateQuestions"
            :key="q.id"
            class="candidate-item"
            :class="{ selected: selectedCandidateIds.includes(q.id) }"
            @click="toggleCandidateSelect(q.id)"
          >
            <el-checkbox
              :model-value="selectedCandidateIds.includes(q.id)"
              @click.stop
              @change="toggleCandidateSelect(q.id)"
            />
            <div class="candidate-main">
              <div class="badges-line">
                <el-tag size="small" :type="getTypeTagType(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                <el-tag size="small" :type="getDifficultyTagType(q.difficulty)">{{ getDifficultyLabel(q.difficulty) }}</el-tag>
                <span class="cand-score">{{ q.score || 5 }}分</span>
                <span class="cand-course">{{ q.courseName }}</span>
              </div>
              <p class="candidate-stem">{{ q.stem }}</p>
            </div>
          </div>

          <div v-if="candidateQuestions.length === 0" class="drawer-empty-hint">
            没有更多可添加的试题（题库已包含所有相关试题，或没有匹配结果）
          </div>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer-actions">
          <span class="selected-summary">已选中 <strong>{{ selectedCandidateIds.length }}</strong> 道试题</span>
          <div class="btns">
            <el-button @click="drawerVisible = false">取消</el-button>
            <el-button
              type="primary"
              :disabled="selectedCandidateIds.length === 0"
              :loading="addingLoading"
              @click="confirmAddQuestions"
            >
              确认批量入库 ({{ selectedCandidateIds.length }})
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- 试题完整题卡弹窗 -->
    <el-dialog v-model="detailModalVisible" title="试题完整题卡详情" width="640px" destroy-on-close>
      <div v-if="activeQuestion" class="detail-modal-body">
        <div class="modal-tags-row">
          <el-tag :type="getTypeTagType(activeQuestion.type)">{{ getTypeLabel(activeQuestion.type) }}</el-tag>
          <el-tag :type="getDifficultyTagType(activeQuestion.difficulty)">{{ getDifficultyLabel(activeQuestion.difficulty) }}</el-tag>
          <span class="text-sm text-slate-500">分值：{{ activeQuestion.score }} 分</span>
          <span class="text-sm text-slate-500">所属课程：{{ activeQuestion.courseName }}</span>
        </div>

        <div class="detail-stem-box">
          <h4 class="box-subtitle">试题题干：</h4>
          <p class="stem-text">{{ activeQuestion.stem }}</p>
        </div>

        <div v-if="activeQuestion.options && activeQuestion.options.length" class="detail-opts-box">
          <h4 class="box-subtitle">备选答案项：</h4>
          <div class="options-vert-list">
            <div
              v-for="opt in activeQuestion.options"
              :key="opt.key"
              class="opt-item"
              :class="{ 'opt-item--correct': opt.isCorrect }"
            >
              <span class="opt-tag">{{ opt.key }}</span>
              <span class="opt-text">{{ opt.content }}</span>
              <span v-if="opt.isCorrect" class="opt-badge">正确答案</span>
            </div>
          </div>
        </div>

        <div class="detail-answer-box">
          <h4 class="box-subtitle">参考答案及评阅要点：</h4>
          <div class="ans-content">{{ activeQuestion.correctAnswer || '无指定客观答案' }}</div>
        </div>

        <div class="detail-analysis-box">
          <h4 class="box-subtitle">试题深度解析：</h4>
          <div class="analysis-content">{{ activeQuestion.analysis || '暂无解析说明' }}</div>
        </div>

        <div v-if="activeQuestion.knowledgePointNames && activeQuestion.knowledgePointNames.length" class="detail-kp-box">
          <h4 class="box-subtitle">关联知识点：</h4>
          <div class="kp-chips">
            <span v-for="kp in activeQuestion.knowledgePointNames" :key="kp" class="kp-chip">
              # {{ kp }}
            </span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Search, Plus, DocumentCopy, FolderOpened, Files, Check } from '@element-plus/icons-vue';
import { getQuestionBankDetail, addQuestionsToBank, removeQuestionFromBank } from '@/api/question/question-bank';
import { getQuestions } from '@/api/question/question';
import type { QuestionItem, QuestionType, Difficulty } from '@/types/question/question';
import { USE_MOCK } from '@/config/mock';
import { MOCK_QUESTIONS } from '@/mock/questions';
import { normalizeQuestionList } from '@/utils/question/normalize-question';
import MathText from '@/components/common/MathText.vue';

const route = useRoute();
const router = useRouter();

const bankId = computed(() => Number(route.params.id) || 1);
const loading = ref(false);
const bankInfo = ref<any>(null);

// 题库内已有的试题
const bankQuestions = ref<QuestionItem[]>([]);
const searchKeyword = ref('');
const filterType = ref('');
const filterDifficulty = ref('');
const selectedRowKeys = ref<number[]>([]);
const expandedAnalyses = ref<number[]>([]);

// 抽屉状态
const drawerVisible = ref(false);
const drawerSearch = ref('');
const drawerType = ref('');
const candidatePool = ref<QuestionItem[]>([]);
const selectedCandidateIds = ref<number[]>([]);
const addingLoading = ref(false);

// 详情弹窗
const detailModalVisible = ref(false);
const activeQuestion = ref<QuestionItem | null>(null);

// 计算题库试题总分
const totalScore = computed(() => {
  return bankQuestions.value.reduce((acc, q) => acc + (q.score || 5), 0);
});

// 筛选后的题目列表
const filteredQuestions = computed(() => {
  return bankQuestions.value.filter(item => {
    if (filterType.value && item.type !== filterType.value) return false;
    if (filterDifficulty.value && item.difficulty !== filterDifficulty.value) return false;
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      const inStem = item.stem?.toLowerCase().includes(kw);
      const inKp = item.knowledgePointNames?.some(k => k.toLowerCase().includes(kw));
      if (!inStem && !inKp) return false;
    }
    return true;
  });
});

// 抽屉内候选试题（排除当前题库已有的试题）
const candidateQuestions = computed(() => {
  const currentIds = new Set(bankQuestions.value.map(q => q.id));
  return candidatePool.value.filter(q => {
    if (currentIds.has(q.id)) return false;
    if (drawerType.value && q.type !== drawerType.value) return false;
    if (drawerSearch.value.trim()) {
      const kw = drawerSearch.value.trim().toLowerCase();
      const inStem = q.stem?.toLowerCase().includes(kw);
      const inKp = q.knowledgePointNames?.some(k => k.toLowerCase().includes(kw));
      if (!inStem && !inKp) return false;
    }
    return true;
  });
});

onMounted(async () => {
  await loadBankDetail();
  await loadCandidatePool();
});

// 加载题库详情与试题
async function loadBankDetail() {
  loading.value = true;
  try {
    const res = await getQuestionBankDetail(bankId.value);
    bankInfo.value = res.data;
    if (bankInfo.value?.questions && Array.isArray(bankInfo.value.questions)) {
      bankQuestions.value = normalizeQuestionList(bankInfo.value.questions as Record<string, unknown>[]);
    } else if (USE_MOCK) {
      bankQuestions.value = normalizeQuestionList(MOCK_QUESTIONS.slice(0, 3) as unknown as Record<string, unknown>[]);
    } else {
      bankQuestions.value = [];
    }
  } catch (err: any) {
    if (USE_MOCK) {
      bankInfo.value = getDefaultBankMock(bankId.value);
      bankQuestions.value = normalizeQuestionList(MOCK_QUESTIONS.slice(0, 3) as unknown as Record<string, unknown>[]);
    } else {
      bankInfo.value = null;
      bankQuestions.value = [];
      ElMessage.error(err?.message || '加载题库详情失败，请检查网络或后端状态');
    }
  } finally {
    loading.value = false;
  }
}

// 加载全局候选题目池
async function loadCandidatePool() {
  try {
    const res = await getQuestions({ pageSize: 50 });
    candidatePool.value = normalizeQuestionList(res.data?.list || []);
  } catch (err: any) {
    if (USE_MOCK) {
      candidatePool.value = normalizeQuestionList(MOCK_QUESTIONS);
    } else {
      candidatePool.value = [];
      ElMessage.error(err?.message || '获取试题池失败');
    }
  }
}

function getDefaultBankMock(id: number) {
  const mockBanks: Record<number, any> = {
    1: {
      id: 1,
      name: '数据结构核心真题库',
      courseId: 101,
      courseName: '数据结构与算法',
      questionCount: 5,
      description: '涵盖全国统考408与期末高频真题，包括线性表、二叉树与图算法典型考察点。',
      updateTime: '2026-09-10'
    },
    2: {
      id: 2,
      name: 'Java面向对象精选题集',
      courseId: 102,
      courseName: 'Java程序设计',
      questionCount: 3,
      description: '针对类与对象、多态特性、集合框架体系与异常处理机制的精编测试题集。',
      updateTime: '2026-09-09'
    },
    3: {
      id: 3,
      name: '高等数学期末测试真题库',
      courseId: 103,
      courseName: '大学数学：高等数学（上）',
      questionCount: 3,
      description: '重点考察极限计算、洛必达法则、泰勒公式与不定积分经典真题。',
      updateTime: '2026-09-08'
    }
  };
  return mockBanks[id] || {
    id,
    name: '课程通用试题库',
    courseId: 101,
    courseName: '通用课程',
    questionCount: 3,
    description: '收录日常教学练习题与期中期末典型题目。',
    updateTime: '2026-09-11'
  };
}

// 勾选单行
function toggleSelectRow(id: number) {
  const idx = selectedRowKeys.value.indexOf(id);
  if (idx > -1) {
    selectedRowKeys.value.splice(idx, 1);
  } else {
    selectedRowKeys.value.push(id);
  }
}

// 展开/收起解析
function toggleExpandAnalysis(id: number) {
  const idx = expandedAnalyses.value.indexOf(id);
  if (idx > -1) {
    expandedAnalyses.value.splice(idx, 1);
  } else {
    expandedAnalyses.value.push(id);
  }
}

// 查看完整题卡弹窗
function viewDetailDialog(q: QuestionItem) {
  activeQuestion.value = q;
  detailModalVisible.value = true;
}

// 移出单个试题
async function handleRemoveQuestion(id: number) {
  try {
    if (USE_MOCK) {
      bankQuestions.value = bankQuestions.value.filter(q => q.id !== id);
    } else {
      await removeQuestionFromBank(bankId.value, id);
      await loadBankDetail();
    }
    selectedRowKeys.value = selectedRowKeys.value.filter(k => k !== id);
    ElMessage.success('已成功从题库中移出该试题');
  } catch (err: any) {
    ElMessage.error(err?.message || '移出试题失败');
  }
}

// 批量移出
async function handleBatchRemove() {
  const count = selectedRowKeys.value.length;
  if (count === 0) return;
  try {
    if (USE_MOCK) {
      const set = new Set(selectedRowKeys.value);
      bankQuestions.value = bankQuestions.value.filter(q => !set.has(q.id));
    } else {
      for (const qId of selectedRowKeys.value) {
        await removeQuestionFromBank(bankId.value, qId);
      }
      await loadBankDetail();
    }
    selectedRowKeys.value = [];
    ElMessage.success(`已批量移出 ${count} 道试题`);
  } catch (err: any) {
    ElMessage.error(err?.message || '批量移出试题失败');
  }
}

// 打开添加抽屉
function openAddDrawer() {
  selectedCandidateIds.value = [];
  drawerSearch.value = '';
  drawerType.value = '';
  drawerVisible.value = true;
}

// 抽屉选中试题切换
function toggleCandidateSelect(id: number) {
  const idx = selectedCandidateIds.value.indexOf(id);
  if (idx > -1) {
    selectedCandidateIds.value.splice(idx, 1);
  } else {
    selectedCandidateIds.value.push(id);
  }
}

// 确认从抽屉批量加入题库
async function confirmAddQuestions() {
  if (selectedCandidateIds.value.length === 0) return;
  addingLoading.value = true;
  try {
    if (USE_MOCK) {
      const selectedList = candidatePool.value.filter(q => selectedCandidateIds.value.includes(q.id));
      bankQuestions.value.push(...selectedList);
    } else {
      await addQuestionsToBank(bankId.value, selectedCandidateIds.value);
      await loadBankDetail();
    }
    ElMessage.success(`成功添加 ${selectedCandidateIds.value.length} 道题目到当前题库！`);
    drawerVisible.value = false;
  } catch (err: any) {
    ElMessage.error(err?.message || '批量添加题目失败');
  } finally {
    addingLoading.value = false;
  }
}

// 一键基于此题库组卷
function handleFastComposeExam() {
  router.push({
    path: '/question/exams/create',
    query: { bankId: bankId.value, courseId: bankInfo.value?.courseId }
  });
}

// 题型与难度标签辅助函数
function getTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[type] || type || '单选题';
}

function getTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as any) || '';
}

function getDifficultyLabel(diff: Difficulty | string) {
  const map: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  };
  return map[diff] || '中等';
}

function getDifficultyTagType(diff: Difficulty | string) {
  const map: Record<string, string> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  };
  return (map[diff] as any) || '';
}
</script>

<style scoped lang="scss">
.bank-detail-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 64px);

  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }
  }

  .bank-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 24px;

    .hero-main-content {
      display: flex;
      align-items: center;
      gap: 20px;
      max-width: 680px;

      .bank-avatar-badge {
        width: 64px;
        height: 64px;
        background: #eff6ff;
        border: 1px solid #dbeafe;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        color: #2563eb;
        flex-shrink: 0;
      }

      .hero-info {
        .title-row {
          display: flex;
          align-items: center;
          gap: 12px;
          flex-wrap: wrap;

          .bank-title {
            font-size: 22px;
            font-weight: 700;
            color: #0f172a;
            margin: 0;
          }

          .course-pill-tag {
            background: #f1f5f9;
            color: #475569;
            font-size: 12px;
            padding: 3px 10px;
            border-radius: 9999px;
            font-weight: 500;
          }

          .update-time-tag {
            font-size: 12px;
            color: #94a3b8;
          }
        }

        .bank-description {
          margin: 8px 0 0;
          font-size: 14px;
          line-height: 1.6;
          color: #64748b;
        }
      }
    }

    .hero-actions-dock {
      display: flex;
      align-items: center;
      gap: 32px;

      .metrics-grid {
        display: flex;
        gap: 24px;

        .metric-item {
          display: flex;
          flex-direction: column;
          align-items: center;

          .metric-val {
            font-size: 28px;
            font-weight: 800;
            color: #1e293b;
            line-height: 1;
          }

          .metric-label {
            font-size: 12px;
            color: #94a3b8;
            margin-top: 6px;
          }
        }
      }

      .action-buttons-group {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .capsule-btn-main {
          background: #2563eb;
          border-color: #2563eb;
          border-radius: 8px;
          font-weight: 500;
          padding: 8px 18px;
        }

        .capsule-btn-sub {
          border-radius: 8px;
          font-weight: 500;
          padding: 8px 18px;
        }
      }
    }
  }

  .bank-content-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px;

    .toolbar-wrapper {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 16px;
      padding-bottom: 20px;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 20px;

      .toolbar-left {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .search-input {
          width: 320px;
        }

        .filter-select {
          width: 140px;
        }
      }

      .toolbar-right {
        display: flex;
        align-items: center;
        gap: 12px;

        .total-hint-text {
          font-size: 13px;
          color: #94a3b8;
        }
      }
    }

    .questions-list-section {
      .question-items-stack {
        display: flex;
        flex-direction: column;
        gap: 16px;

        .question-bank-item-card {
          border: 1px solid #f1f5f9;
          background: #ffffff;
          border-radius: 12px;
          padding: 18px 20px;
          display: flex;
          gap: 16px;
          transition: all 0.2s ease;

          &:hover {
            border-color: #cbd5e1;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
          }

          .item-checkbox-col {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;

            .index-num {
              font-size: 12px;
              color: #94a3b8;
              font-family: monospace;
            }
          }

          .item-body {
            flex: 1;

            .item-badges-row {
              display: flex;
              align-items: center;
              gap: 8px;
              margin-bottom: 10px;

              .score-badge {
                font-size: 12px;
                color: #2563eb;
                background: #eff6ff;
                padding: 2px 8px;
                border-radius: 4px;
                font-weight: 600;
              }

              .chapter-badge {
                font-size: 12px;
                color: #64748b;
                background: #f1f5f9;
                padding: 2px 8px;
                border-radius: 4px;
              }
            }

            .stem-content {
              font-size: 15px;
              line-height: 1.6;
              color: #1e293b;
              font-weight: 500;
              margin-bottom: 12px;
            }

            .options-container {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
              gap: 8px;
              margin-bottom: 12px;

              .option-pill {
                background: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 8px;
                padding: 8px 12px;
                font-size: 13px;
                color: #334155;
                display: flex;
                align-items: center;
                gap: 8px;

                .opt-key {
                  font-weight: 700;
                  color: #64748b;
                }

                .opt-text {
                  flex: 1;
                }

                &--correct {
                  background: #f0fdf4;
                  border-color: #bbf7d0;
                  color: #15803d;

                  .opt-key {
                    color: #16a34a;
                  }

                  .opt-check-icon {
                    font-size: 11px;
                    font-weight: 600;
                    color: #16a34a;
                  }
                }
              }
            }

            .analysis-box {
              background: #f8fafc;
              border-radius: 8px;
              padding: 12px 16px;
              margin-top: 10px;
              border-left: 3px solid #3b82f6;

              .analysis-line {
                font-size: 13px;
                line-height: 1.6;
                color: #475569;
                margin-bottom: 4px;

                &:last-child {
                  margin-bottom: 0;
                }

                .label {
                  font-weight: 600;
                  color: #334155;
                }

                .kp-tags {
                  display: inline-flex;
                  gap: 6px;

                  .kp-tag {
                    background: #e2e8f0;
                    color: #475569;
                    font-size: 11px;
                    padding: 1px 6px;
                    border-radius: 4px;
                  }
                }
              }
            }
          }

          .item-actions {
            display: flex;
            flex-direction: column;
            align-items: flex-end;
            justify-content: flex-start;
            gap: 6px;
            width: 100px;
            flex-shrink: 0;
          }
        }
      }

      .empty-questions-card {
        padding: 48px;
        text-align: center;

        .empty-icon {
          font-size: 48px;
          color: #94a3b8;
          display: inline-flex;
          margin-bottom: 12px;
        }

        .empty-title {
          font-size: 18px;
          font-weight: 600;
          color: #334155;
          margin-bottom: 6px;
        }

        .empty-sub {
          font-size: 14px;
          color: #94a3b8;
        }
      }
    }
  }

  /* 抽屉样式 */
  .drawer-content-box {
    display: flex;
    flex-direction: column;
    height: 100%;

    .drawer-filter-bar {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;
    }

    .candidate-questions-list {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 12px;

      .candidate-item {
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 12px 14px;
        display: flex;
        gap: 12px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #f8fafc;
          border-color: #cbd5e1;
        }

        &.selected {
          border-color: #3b82f6;
          background: #eff6ff;
        }

        .candidate-main {
          flex: 1;

          .badges-line {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 6px;

            .cand-score {
              font-size: 12px;
              color: #2563eb;
              font-weight: 600;
            }

            .cand-course {
              font-size: 12px;
              color: #94a3b8;
            }
          }

          .candidate-stem {
            font-size: 14px;
            color: #1e293b;
            line-height: 1.5;
            margin: 0;
          }
        }
      }

      .drawer-empty-hint {
        padding: 40px;
        text-align: center;
        color: #94a3b8;
        font-size: 14px;
      }
    }
  }

  .drawer-footer-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;

    .selected-summary {
      font-size: 14px;
      color: #475569;

      strong {
        color: #2563eb;
        font-size: 16px;
      }
    }

    .btns {
      display: flex;
      gap: 8px;
    }
  }

  /* 试题详情弹窗 */
  .detail-modal-body {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .modal-tags-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding-bottom: 12px;
      border-bottom: 1px solid #f1f5f9;
    }

    .box-subtitle {
      font-size: 14px;
      font-weight: 600;
      color: #334155;
      margin: 0 0 6px;
    }

    .stem-text {
      font-size: 15px;
      line-height: 1.6;
      color: #1e293b;
      background: #f8fafc;
      padding: 12px 14px;
      border-radius: 8px;
      margin: 0;
    }

    .options-vert-list {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .opt-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px 12px;
        border-radius: 6px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;

        .opt-tag {
          font-weight: 700;
          color: #64748b;
        }

        .opt-text {
          flex: 1;
          font-size: 13px;
        }

        &--correct {
          background: #f0fdf4;
          border-color: #86efac;

          .opt-tag {
            color: #16a34a;
          }

          .opt-badge {
            font-size: 11px;
            color: #16a34a;
            font-weight: 600;
          }
        }
      }
    }

    .ans-content,
    .analysis-content {
      font-size: 14px;
      color: #334155;
      line-height: 1.6;
      background: #f8fafc;
      padding: 10px 14px;
      border-radius: 8px;
    }

    .kp-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .kp-chip {
        background: #eff6ff;
        color: #2563eb;
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 6px;
        font-weight: 500;
      }
    }
  }
}
</style>
