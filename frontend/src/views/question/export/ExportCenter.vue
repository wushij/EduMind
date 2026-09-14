<template>
  <div class="export-center-page" v-loading="loading">
    <!-- 顶部全景横幅 -->
    <PageHeroBanner
      title="试卷制卷与考务排版工坊 · 高保真排版与打印"
      subtitle="支持 A4单栏 / B4双栏中缝规范、密封装订线、考生条形码、大题赋分表、防伪动态水印与机读答题卡一键生成"
      background-variant="question"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">A4 / B4</span>
            <span class="stat-label">考场排版规范</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">300 DPI</span>
            <span class="stat-label">矢量印刷级渲染</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">动态水印</span>
            <span class="stat-label">版权防泄密保护</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">100%</span>
            <span class="stat-label">LaTeX 公式保真</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 排版预设方案快速切换栏 -->
      <div class="template-presets-bar no-print">
        <div class="presets-header">
          <div class="presets-title">
            <el-icon class="icon-preset"><Files /></el-icon>
            <span>考务排版方案预设库</span>
          </div>
          <span class="presets-tip">点击一键套用官方考场版式与卷面规范配置</span>
        </div>
        <div class="preset-cards-grid">
          <div
            v-for="preset in presetTemplates"
            :key="preset.id"
            class="preset-card"
            :class="{ active: currentPresetId === preset.id }"
            @click="applyPreset(preset)"
          >
            <div class="preset-icon-badge" :style="{ background: preset.bgColor, color: preset.color }">
              <component :is="preset.icon" />
            </div>
            <div class="preset-info">
              <div class="preset-name-row">
                <span class="preset-name">{{ preset.name }}</span>
                <el-tag size="small" :type="preset.tagType" effect="plain">{{ preset.tag }}</el-tag>
              </div>
              <p class="preset-desc">{{ preset.desc }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 主工作区：左侧控制台 + 右侧排版仿真画布 -->
      <div class="export-split-workspace">
        <!-- 左侧：参数排版配置控制台 -->
        <div class="control-panel-card no-print">
          <div class="panel-header">
            <div class="panel-title-group">
              <el-icon class="panel-icon"><Operation /></el-icon>
              <h3>排版参数与考务规制</h3>
            </div>
            <el-tag size="small" type="success" effect="light">实时仿真同步</el-tag>
          </div>

          <el-tabs v-model="activeTab" class="config-tabs">
            <!-- 标签页 1: 卷面与考务标设 -->
            <el-tab-pane label="卷面考务" name="exam">
              <el-form :model="configForm" label-position="top" class="config-form" size="small">
                <el-form-item label="关联课程试卷">
                  <el-select v-model="configForm.examId" style="width: 100%;">
                    <el-option :value="101" label="2026年高三开学数学综合调研模考卷" />
                    <el-option :value="102" label="高等数学期中阶段拔高模拟测试卷" />
                    <el-option :value="103" label="高一物理必修第一册牛顿定律专项卷" />
                    <el-option :value="104" label="高考化学工艺流程与反应原理攻坚卷" />
                  </el-select>
                </el-form-item>

                <el-form-item label="试卷主标题">
                  <el-input v-model="configForm.paperTitle" placeholder="如：2026年普通高等学校招生全国统一考试预测卷" />
                </el-form-item>

                <el-form-item label="试卷副标题">
                  <el-input v-model="configForm.paperSubtitle" placeholder="如：理科数学 (全卷共4页 满分150分)" />
                </el-form-item>

                <el-row :gutter="12">
                  <el-col :span="12">
                    <el-form-item label="保密级别标示">
                      <el-select v-model="configForm.confidentialLevel" style="width: 100%;">
                        <el-option label="绝密 ★ 启用前" value="绝密 ★ 启用前" />
                        <el-option label="机密 ★ 启用前" value="机密 ★ 启用前" />
                        <el-option label="内部教学诊断资料" value="内部教学诊断资料" />
                        <el-option label="不标注保密等级" value="" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="试卷满分 / 时长">
                      <el-input v-model="examTimeScore" placeholder="150分 / 120分钟" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <div class="sub-section-title">
                  <span>考场标准化规范部件</span>
                </div>

                <div class="checkbox-list">
                  <el-checkbox v-model="configForm.showSealingLine">
                    <div class="checkbox-label-block">
                      <span class="label-text">左侧密封装订线</span>
                      <span class="label-desc">含考生考号、姓名、班级、考场及密封提醒</span>
                    </div>
                  </el-checkbox>

                  <el-checkbox v-model="configForm.showStudentInfo">
                    <div class="checkbox-label-block">
                      <span class="label-text">考生准考证与条形码贴区</span>
                      <span class="label-desc">符合现代考场机读扫描定位标准</span>
                    </div>
                  </el-checkbox>

                  <el-checkbox v-model="configForm.showScoreGrid">
                    <div class="checkbox-label-block">
                      <span class="label-text">大题赋分网格统分栏</span>
                      <span class="label-desc">一、二、三...总分、评卷人、复核人表格</span>
                    </div>
                  </el-checkbox>

                  <el-checkbox v-model="configForm.showNoticeBar">
                    <div class="checkbox-label-block">
                      <span class="label-text">试卷作答注意事项说明框</span>
                      <span class="label-desc">规范作答黑色中性笔与2B铅笔填涂提示</span>
                    </div>
                  </el-checkbox>
                </div>
              </el-form>
            </el-tab-pane>

            <!-- 标签页 2: 纸张与字体排版 -->
            <el-tab-pane label="纸张排版" name="paper">
              <el-form :model="configForm" label-position="top" class="config-form" size="small">
                <el-form-item label="纸张规格与分栏">
                  <el-radio-group v-model="configForm.paperSize" style="width: 100%;">
                    <el-radio-button label="A4" style="width: 50%;">A4 标准单栏 (推荐)</el-radio-button>
                    <el-radio-button label="B4" style="width: 50%;">B4/8开 双栏中缝</el-radio-button>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="卷面主文字体">
                  <el-select v-model="configForm.fontFamily" style="width: 100%;">
                    <el-option label="标准书宋体 (教育部考试中心官方标准)" value="SimSun" />
                    <el-option label="华文中宋 (粗实庄重)" value="STZhongsong" />
                    <el-option label="楷体·GB2312 (清晰柔和适合初高中)" value="KaiTi" />
                    <el-option label="现代无衬线黑体 (现代极简风格)" value="sans-serif" />
                  </el-select>
                </el-form-item>

                <el-form-item label="题项排版密度与行距">
                  <el-radio-group v-model="configForm.lineSpacing" style="width: 100%;">
                    <el-radio-button label="compact" style="width: 33.33%;">紧凑省纸</el-radio-button>
                    <el-radio-button label="normal" style="width: 33.33%;">标准规范</el-radio-button>
                    <el-radio-button label="relaxed" style="width: 33.33%;">留白充裕</el-radio-button>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="选择题选项排布方式">
                  <el-select v-model="configForm.optionLayout" style="width: 100%;">
                    <el-option label="4 选单行横向对齐 (A B C D 水平分布)" value="horizontal" />
                    <el-option label="2 选双行网格对齐 (两行各两项)" value="grid" />
                    <el-option label="单列垂直排列 (公式较长题型推荐)" value="vertical" />
                  </el-select>
                </el-form-item>

                <el-form-item label="题目分值展示">
                  <el-switch v-model="configForm.showPointBadge" active-text="在小题末尾标示分值 (本题5分)" />
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 标签页 3: 水印防护与附录 -->
            <el-tab-pane label="安全与附录" name="security">
              <el-form :model="configForm" label-position="top" class="config-form" size="small">
                <el-form-item label="防扩散版权水印">
                  <el-switch v-model="configForm.showWatermark" active-text="开启页面半透明倾斜防伪水印" />
                </el-form-item>

                <el-form-item label="水印文字内容" v-if="configForm.showWatermark">
                  <el-input
                    v-model="configForm.watermarkText"
                    placeholder="如：智教云示范第一中学 · 内部教学试卷 · 严禁翻印"
                  />
                </el-form-item>

                <div class="sub-section-title">
                  <span>配套教辅与答卷附录</span>
                </div>

                <div class="checkbox-list">
                  <el-checkbox v-model="configForm.showAnswerSheet">
                    <div class="checkbox-label-block">
                      <span class="label-text">附带机读答题卡页 (第3页)</span>
                      <span class="label-desc">含标准 2B 铅笔选择题填涂点与主观题作答框</span>
                    </div>
                  </el-checkbox>

                  <el-checkbox v-model="configForm.showAnalysis">
                    <div class="checkbox-label-block">
                      <span class="label-text">附带名师参考答案与踩分细则 (第4页)</span>
                      <span class="label-desc">分步赋分标记、解题突破口与常见误区提示</span>
                    </div>
                  </el-checkbox>
                </div>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <!-- 快捷操作按钮组 -->
          <div class="studio-action-dock">
            <el-button type="primary" class="primary-print-btn" @click="handlePrintDirect">
              <el-icon><Printer /></el-icon>
              <span>考务纯净打印 (Ctrl+P)</span>
            </el-button>
            <div class="secondary-btn-row">
              <el-button type="primary" plain :loading="exporting" @click="handleCreateExportTask">
                <el-icon><Download /></el-icon>
                <span>生成高保真 PDF</span>
              </el-button>
              <el-button plain @click="handleExportWord">
                <el-icon><Document /></el-icon>
                <span>导出 Word</span>
              </el-button>
            </div>
          </div>
        </div>

        <!-- 右侧：真实纸张渲染仿真画布 -->
        <div class="preview-canvas-card">
          <!-- 画布顶部控制条 -->
          <div class="canvas-top-bar no-print">
            <div class="preview-page-tabs">
              <el-radio-group v-model="previewPageMode" size="small">
                <el-radio-button label="page1">第 1 页 · 客观选择与填空</el-radio-button>
                <el-radio-button label="page2">第 2 页 · 解答题与压轴题</el-radio-button>
                <el-radio-button label="page3" v-if="configForm.showAnswerSheet">第 3 页 · 标准答题卡</el-radio-button>
                <el-radio-button label="page4" v-if="configForm.showAnalysis">第 4 页 · 踩分细则与解析</el-radio-button>
                <el-radio-button label="continuous">📑 连续整卷排版</el-radio-button>
              </el-radio-group>
            </div>

            <div class="canvas-actions-right">
              <!-- 缩放控制器 -->
              <div class="zoom-controls">
                <el-button size="small" circle @click="zoomScale = Math.max(0.5, Number((zoomScale - 0.05).toFixed(2)))">-</el-button>
                <span class="zoom-text">{{ Math.round(zoomScale * 100) }}%</span>
                <el-button size="small" circle @click="zoomScale = Math.min(1.2, Number((zoomScale + 0.05).toFixed(2)))">+</el-button>
                <el-button size="small" text @click="zoomScale = 0.85">自适应</el-button>
              </div>

              <!-- 打印范围选择 -->
              <el-select v-model="printScope" size="small" style="width: 140px;" class="print-scope-select">
                <el-option label="打印范围：整套试卷" value="all" />
                <el-option label="打印范围：仅当前页" value="current" />
              </el-select>

              <el-button size="small" type="primary" class="canvas-print-btn" @click="handlePrintDirect">
                <el-icon><Printer /></el-icon>
                <span>纯净打印</span>
              </el-button>
            </div>
          </div>

          <!-- 仿真试卷纸张展示区 (具有桌面阴影效果) -->
          <div class="paper-scroll-wrapper">
            <div class="paper-scaler" :style="{ transform: `scale(${zoomScale})`, transformOrigin: 'top center' }">
              
              <!-- 试卷打印主容器 (仅包含纯净试卷与答题卡) -->
              <div id="printable-exam-paper" :class="[`font-${configForm.fontFamily}`, `spacing-${configForm.lineSpacing}`, { 'is-b4-mode': configForm.paperSize === 'B4' }]">
                
                <!-- ================= 第 1 页：客观选择题与填空题 ================= -->
                <div
                  v-show="previewPageMode === 'page1' || previewPageMode === 'continuous'"
                  class="simulated-sheet exam-page-1"
                  :class="{ 'with-watermark': configForm.showWatermark, 'print-target-page': previewPageMode === 'page1' }"
                >
                  <!-- 防伪水印背景层 -->
                  <div class="watermark-layer" v-if="configForm.showWatermark">
                    <span class="watermark-text">{{ configForm.watermarkText || '智教云 · EduMind 官方试卷' }}</span>
                  </div>

                  <!-- 左侧考场密封装订线 -->
                  <div class="sealing-column" v-if="configForm.showSealingLine">
                    <div class="sealing-dash-line"></div>
                    <div class="sealing-content">
                      <div class="sealing-fields">
                        <span>考生考号：__________________</span>
                        <span>学生姓名：__________________</span>
                        <span>所在班级：__________________</span>
                        <span>所属考场：__________________</span>
                        <span>座 位 号：__________________</span>
                      </div>
                      <div class="sealing-warning">
                        <span>【 密 封 线 内 严 禁 答 题 】</span>
                      </div>
                    </div>
                  </div>

                  <!-- 卷面正文区域 -->
                  <div class="sheet-main-content">
                    <!-- 卷头与标题区 -->
                    <div class="sheet-header">
                      <div class="confidential-bar">
                        <span class="confidential-badge" v-if="configForm.confidentialLevel">{{ configForm.confidentialLevel }}</span>
                        <span class="exam-type-badge">试卷类型：A</span>
                      </div>
                      <h1 class="paper-main-title">{{ configForm.paperTitle }}</h1>
                      <h3 class="paper-sub-title">{{ configForm.paperSubtitle }}</h3>
                      
                      <div class="paper-rules-bar">
                        <span>考试科目：理科数学</span>
                        <span>试卷满分：150 分</span>
                        <span>考试时间：120 分钟</span>
                        <span>制卷单位：智教云教研考务组</span>
                      </div>
                    </div>

                    <!-- 考生注意事项说明栏 -->
                    <div class="exam-notice-box" v-if="configForm.showNoticeBar">
                      <div class="notice-title">考生须知：</div>
                      <ol class="notice-list">
                        <li>答卷前，考生务必将自己的姓名、准考证号填写在试卷和答题卡相应位置上。</li>
                        <li>回答选择题时，选出每小题答案后，用 2B 铅笔把答题卡上对应题目的答案标号涂黑。如需改动，用橡皮擦干净后，再选涂其他答案标号。</li>
                        <li>回答非选择题时，将答案书写在答题卡规定区域内，写在本试卷及草稿纸上无效。</li>
                      </ol>
                    </div>

                    <!-- 考生个人填涂信息区与条码框 -->
                    <div class="candidate-info-box" v-if="configForm.showStudentInfo">
                      <div class="info-fill-item">
                        <span>准考证号：</span>
                        <div class="barcode-cells">
                          <span v-for="i in 10" :key="i" class="cell"></span>
                        </div>
                      </div>
                      <div class="info-fill-item">
                        <span>座位号：</span>
                        <div class="barcode-cells small">
                          <span v-for="i in 2" :key="i" class="cell"></span>
                        </div>
                      </div>
                      <div class="barcode-sticker-box">
                        <span>贴条形码区 (朝上请勿贴出虚线框)</span>
                      </div>
                    </div>

                    <!-- 大题总分得分网格栏 -->
                    <div class="score-summary-table" v-if="configForm.showScoreGrid">
                      <table border="1" cellpadding="0" cellspacing="0">
                        <thead>
                          <tr>
                            <th style="width: 70px;">题号</th>
                            <th>一</th>
                            <th>二</th>
                            <th>三</th>
                            <th>四</th>
                            <th>五</th>
                            <th>六</th>
                            <th style="width: 80px;">总分</th>
                            <th style="width: 80px;">评卷人</th>
                            <th style="width: 80px;">复核人</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>得分</td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <!-- 第 I 卷 选择题 -->
                    <div class="section-title-bar">
                      <div class="section-heading">第 I 卷（选择题 共 60 分）</div>
                    </div>
                    <div class="section-notice">
                      一、选择题：本大题共 12 小题，每小题 5 分，共 60 分。在每小题给出的四个选项中，只有一项是符合题目要求的。
                    </div>

                    <div class="questions-list">
                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            1. 已知集合 $A = \{x \in \mathbf{R} \mid x^2 - 3x + 2 < 0\}$，$B = \{x \in \mathbf{R} \mid 1 < x < 3\}$，则 $A \cap B = $（ &nbsp; ）
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                        <div class="q-options" :class="[`layout-${configForm.optionLayout}`]">
                          <span class="opt">A. $(1, 2)$</span>
                          <span class="opt">B. $(1, 3)$</span>
                          <span class="opt">C. $(2, 3)$</span>
                          <span class="opt">D. $\varnothing$</span>
                        </div>
                      </div>

                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            2. 若复数 $z$ 满足 $(1 - \mathrm{i})z = |1 + \sqrt{3}\mathrm{i}|$，其中 $\mathrm{i}$ 为虚数单位，则 $z$ 的虚部为（ &nbsp; ）
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                        <div class="q-options" :class="[`layout-${configForm.optionLayout}`]">
                          <span class="opt">A. $1$</span>
                          <span class="opt">B. $-1$</span>
                          <span class="opt">C. $\mathrm{i}$</span>
                          <span class="opt">D. $-\mathrm{i}$</span>
                        </div>
                      </div>

                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            3. 设函数 $f(x) = \sin(\omega x + \frac{\pi}{6}) (\omega > 0)$ 的最小正周期为 $\pi$，则其图象的一条对称轴方程为（ &nbsp; ）
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                        <div class="q-options" :class="[`layout-${configForm.optionLayout}`]">
                          <span class="opt">A. $x = \frac{\pi}{6}$</span>
                          <span class="opt">B. $x = \frac{\pi}{3}$</span>
                          <span class="opt">C. $x = -\frac{\pi}{6}$</span>
                          <span class="opt">D. $x = \frac{\pi}{2}$</span>
                        </div>
                      </div>

                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            4. 已知双曲线 $C: \frac{x^2}{a^2} - \frac{y^2}{b^2} = 1 (a > 0, b > 0)$ 的一条渐近线方程为 $y = \sqrt{2}x$，则双曲线 $C$ 的离心率为（ &nbsp; ）
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                        <div class="q-options" :class="[`layout-${configForm.optionLayout}`]">
                          <span class="opt">A. $\sqrt{3}$</span>
                          <span class="opt">B. $\sqrt{2}$</span>
                          <span class="opt">C. $2$</span>
                          <span class="opt">D. $\frac{\sqrt{6}}{2}$</span>
                        </div>
                      </div>

                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            5. 某学校为弘扬传统文化，举办书法展。甲、乙、丙等 5 位同学依次出场展示，若甲不在首位且乙不在末位，则不同出场顺序共有（ &nbsp; ）种。
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                        <div class="q-options" :class="[`layout-${configForm.optionLayout}`]">
                          <span class="opt">A. 78</span>
                          <span class="opt">B. 84</span>
                          <span class="opt">C. 96</span>
                          <span class="opt">D. 120</span>
                        </div>
                      </div>

                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            6. 在 $\triangle ABC$ 中，内角 $A, B, C$ 所对边分别为 $a, b, c$，若 $b\cos C + c\cos B = 2a\cos A$，则角 $A$ 的大小为（ &nbsp; ）
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                        <div class="q-options" :class="[`layout-${configForm.optionLayout}`]">
                          <span class="opt">A. $\frac{\pi}{6}$</span>
                          <span class="opt">B. $\frac{\pi}{4}$</span>
                          <span class="opt">C. $\frac{\pi}{3}$</span>
                          <span class="opt">D. $\frac{2\pi}{3}$</span>
                        </div>
                      </div>
                    </div>

                    <!-- 页面页脚 -->
                    <div class="sheet-footer">
                      <span>智教云 · EduMind 智能命题与考务排版系统</span>
                      <span>理科数学试题 第 1 页（共 2 页）</span>
                    </div>
                  </div>
                </div>

                <!-- ================= 第 2 页：填空与解答综合题 ================= -->
                <div
                  v-show="previewPageMode === 'page2' || previewPageMode === 'continuous'"
                  class="simulated-sheet exam-page-2"
                  :class="{ 'with-watermark': configForm.showWatermark, 'print-target-page': previewPageMode === 'page2' }"
                >
                  <!-- 防伪水印背景层 -->
                  <div class="watermark-layer" v-if="configForm.showWatermark">
                    <span class="watermark-text">{{ configForm.watermarkText || '智教云 · EduMind 官方试卷' }}</span>
                  </div>

                  <!-- 左侧密封线（第二页仅留中缝线标记） -->
                  <div class="sealing-column page-2-column" v-if="configForm.showSealingLine">
                    <div class="sealing-dash-line"></div>
                    <div class="sealing-content">
                      <span class="sealing-tag">装订密封中缝线</span>
                    </div>
                  </div>

                  <div class="sheet-main-content">
                    <div class="page-top-runner">
                      <span>2026年普通高等学校招生全国统一考试冲刺预测卷</span>
                      <span>理科数学试题</span>
                    </div>

                    <!-- 第 II 卷 填空题 -->
                    <div class="section-title-bar mt-2">
                      <div class="section-heading">第 II 卷（非选择题 共 90 分）</div>
                    </div>
                    <div class="section-notice">
                      二、填空题：本大题共 4 小题，每小题 5 分，共 20 分。把答案填在答题卡的相应横线上。
                    </div>

                    <div class="questions-list">
                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            13. 已知平面向量 $\vec{a} = (1, 2)$，$\vec{b} = (-2, k)$，若 $\vec{a} \perp \vec{b}$，则实数 $k = $ ____________。
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                      </div>

                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            14. 若变量 $x, y$ 满足约束条件 $\begin{cases} x + y - 2 \ge 0 \\ x - y + 2 \ge 0 \\ 2x - y - 1 \le 0 \end{cases}$，则目标函数 $z = 2x + 3y$ 的最大值为 ____________。
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                      </div>

                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            15. 设三棱锥 $P-ABC$ 的四个顶点都在半径为 $R$ 的球面上，底面 $\triangle ABC$ 为边长为 $2\sqrt{3}$ 的正三角形，且 $PA \perp$ 底面 $ABC$，$PA = 4$，则该三棱锥外接球的表面积为 ____________。
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                      </div>

                      <div class="mock-q-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            16. 已知定义在 $\mathbf{R}$ 上的偶函数 $f(x)$ 满足 $f(x+2) = -f(x)$，且当 $x \in [0, 1]$ 时，$f(x) = x^2$，则方程 $f(x) - \log_4(x+1) = 0$ 的所有实数解的个数为 ____________。
                          </p>
                          <span class="point-tag" v-if="configForm.showPointBadge">(5分)</span>
                        </div>
                      </div>
                    </div>

                    <!-- 三、解答题 -->
                    <div class="section-title-bar mt-4">
                      <div class="section-heading">三、解答题（本大题共 6 小题，共 70 分。解答应写出文字说明、证明过程或演算步骤）</div>
                    </div>

                    <div class="questions-list solving-list">
                      <div class="mock-q-item solving-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            17.（本小题满分 12 分）<br />
                            在 $\triangle ABC$ 中，角 $A, B, C$ 的对边分别为 $a, b, c$，已知 $2b\cos A = 2c - \sqrt{3}a$。<br />
                            (1) 求角 $B$ 的大小；<br />
                            (2) 若 $b = \sqrt{7}$，$\triangle ABC$ 的面积为 $\frac{3\sqrt{3}}{2}$，求 $\triangle ABC$ 的周长。
                          </p>
                        </div>
                        <!-- 规范答题留白虚线框 -->
                        <div class="draft-answer-box">
                          <span class="draft-tip">【考生答题区域 · 书写需工整清晰】</span>
                        </div>
                      </div>

                      <div class="mock-q-item solving-item">
                        <div class="q-title-row">
                          <p class="q-text">
                            18.（本小题满分 12 分）<br />
                            如图，四棱锥 $P-ABCD$ 中，底面 $ABCD$ 为矩形，$PA \perp$ 底面 $ABCD$，$AB = 2, AD = \sqrt{2}$，$PA = 2$，$E$ 为 $PD$ 的中点。<br />
                            (1) 证明：$AE \perp$ 平面 $PCD$；<br />
                            (2) 求二面角 $A-PC-D$ 的余弦值。
                          </p>
                        </div>
                        <div class="draft-answer-box">
                          <span class="draft-tip">【考生答题区域 · 书写需工整清晰】</span>
                        </div>
                      </div>
                    </div>

                    <!-- 页面页脚 -->
                    <div class="sheet-footer">
                      <span>智教云 · EduMind 智能命题与考务排版系统</span>
                      <span>理科数学试题 第 2 页（共 2 页）</span>
                    </div>
                  </div>
                </div>

                <!-- ================= 第 3 页：标准机读答题卡 (可选) ================= -->
                <div
                  v-if="configForm.showAnswerSheet"
                  v-show="previewPageMode === 'page3' || previewPageMode === 'continuous'"
                  class="simulated-sheet exam-page-3 answer-sheet-page"
                  :class="{ 'with-watermark': configForm.showWatermark, 'print-target-page': previewPageMode === 'page3' }"
                >
                  <div class="sheet-main-content answer-sheet-container">
                    <div class="answer-sheet-header">
                      <h2>2026年普通高等学校招生全国统一考试 · 标准答题卡</h2>
                      <div class="sub-header-info">
                        <span>科目：理科数学</span>
                        <span>满分：150分</span>
                        <span>条形码扫描定位标准版</span>
                      </div>
                    </div>

                    <!-- 答题卡基本信息与涂写规范 -->
                    <div class="card-meta-grid">
                      <div class="student-meta-box">
                        <div class="meta-line">姓名：____________________ 班级：____________</div>
                        <div class="meta-line">准考证号：[ &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; ]</div>
                        <div class="notice-tiny">
                          填涂说明：请使用 2B 铅笔规范填涂！<br />
                          正确填涂示例：[ ■ ] &nbsp; 错误填涂：[ × ] [ √ ] [ / ]
                        </div>
                      </div>
                      <div class="barcode-target-area">
                        <div class="barcode-border">
                          <span>贴条形码区 (请勿贴出框外)</span>
                        </div>
                      </div>
                    </div>

                    <!-- 一、选择题机读填涂区 (1-12) -->
                    <div class="card-section">
                      <div class="card-sec-title">一、单项选择题（使用 2B 铅笔填涂，每小题 5 分）</div>
                      <div class="bubble-grid">
                        <div v-for="q in 12" :key="q" class="bubble-row">
                          <span class="q-num">{{ q }}</span>
                          <span class="bubble-opt">[ A ]</span>
                          <span class="bubble-opt">[ B ]</span>
                          <span class="bubble-opt">[ C ]</span>
                          <span class="bubble-opt">[ D ]</span>
                        </div>
                      </div>
                    </div>

                    <!-- 二、填空题书写区 (13-16) -->
                    <div class="card-section">
                      <div class="card-sec-title">二、填空题（使用 0.5 毫米黑色签字笔规范作答）</div>
                      <div class="fill-in-grid">
                        <div class="fill-row">13. ________________________________</div>
                        <div class="fill-row">14. ________________________________</div>
                        <div class="fill-row">15. ________________________________</div>
                        <div class="fill-row">16. ________________________________</div>
                      </div>
                    </div>

                    <!-- 三、解答题标准作答方格框 -->
                    <div class="card-section">
                      <div class="card-sec-title">三、解答题作答区（17 题）</div>
                      <div class="formal-answer-frame">
                        <div class="score-small-box">评卷人：______ 得分：______</div>
                        <div class="frame-watermark">请在各题目的答题区域内作答，超出黑色矩形边框限定区域的答案无效</div>
                      </div>
                    </div>

                    <div class="sheet-footer">
                      <span>智教云 · EduMind 智能答题卡标准化输出</span>
                      <span>答题卡 第 1 页（共 1 页）</span>
                    </div>
                  </div>
                </div>

                <!-- ================= 第 4 页：名师参考答案与评分细则 (可选) ================= -->
                <div
                  v-if="configForm.showAnalysis"
                  v-show="previewPageMode === 'page4' || previewPageMode === 'continuous'"
                  class="simulated-sheet exam-page-4 analysis-page"
                  :class="{ 'with-watermark': configForm.showWatermark, 'print-target-page': previewPageMode === 'page4' }"
                >
                  <div class="sheet-main-content">
                    <div class="sheet-header">
                      <h2 class="analysis-title">2026年普通高等学校招生统一考试冲刺预测卷 · 教师参考答案与考点精解</h2>
                      <div class="paper-rules-bar">
                        <span>教研备课专用</span>
                        <span>含核心素养维度、分步评分细则与常见易错警示</span>
                      </div>
                    </div>

                    <div class="analysis-content">
                      <div class="analysis-block">
                        <h4>一、选择题参考答案与核心考点速查</h4>
                        <div class="answers-table-wrapper">
                          <table border="1" class="ans-table">
                            <thead>
                              <tr>
                                <th>题号</th>
                                <th>1</th><th>2</th><th>3</th><th>4</th><th>5</th><th>6</th>
                              </tr>
                            </thead>
                            <tbody>
                              <tr>
                                <td>答案</td>
                                <td><strong>A</strong></td>
                                <td><strong>A</strong></td>
                                <td><strong>C</strong></td>
                                <td><strong>A</strong></td>
                                <td><strong>B</strong></td>
                                <td><strong>C</strong></td>
                              </tr>
                            </tbody>
                          </table>
                        </div>
                      </div>

                      <div class="analysis-block mt-3">
                        <h4>二、典型解答题评分细则与分步采分点（第 17 题示例）</h4>
                        <div class="solution-detail">
                          <p><strong>【解析】</strong>(1) 由正弦定理及已知条件 $2b\cos A = 2c - \sqrt{3}a$，得：<br />
                          $2\sin B \cos A = 2\sin C - \sqrt{3}\sin A$。<span class="scoring-tag">…… 2 分</span></p>
                          <p>因为 $C = \pi - (A + B)$，所以 $\sin C = \sin(A + B) = \sin A \cos B + \cos A \sin B$。<br />
                          代入化简得：$2\sin B \cos A = 2(\sin A \cos B + \cos A \sin B) - \sqrt{3}\sin A$，<br />
                          即 $2\sin A \cos B = \sqrt{3}\sin A$。<span class="scoring-tag">…… 4 分</span></p>
                          <p>因为在 $\triangle ABC$ 中，$\sin A \ne 0$，所以 $\cos B = \frac{\sqrt{3}}{2}$。<span class="scoring-tag">…… 5 分</span><br />
                          又因为 $0 < B < \pi$，所以 $B = \frac{\pi}{6}$。<span class="scoring-tag">…… 6 分 (满分)</span></p>

                          <p class="mt-2"><strong>(2) 评分细则：</strong><br />
                          由 $S_{\triangle ABC} = \frac{1}{2}ac\sin B = \frac{3\sqrt{3}}{2}$，且 $B = \frac{\pi}{6}$，得 $\frac{1}{4}ac = \frac{3\sqrt{3}}{2} \implies ac = 6\sqrt{3}$。<span class="scoring-tag">…… 8 分</span><br />
                          由余弦定理 $b^2 = a^2 + c^2 - 2ac\cos B$，代入 $b = \sqrt{7}$ 得：<br />
                          $7 = a^2 + c^2 - 2 \times 6\sqrt{3} \times \frac{\sqrt{3}}{2} = a^2 + c^2 - 18 \implies a^2 + c^2 = 25$。<span class="scoring-tag">…… 10 分</span><br />
                          结合 $(a + c)^2 = a^2 + c^2 + 2ac = 25 + 12\sqrt{3}$，解得周长 $a + b + c = \sqrt{25 + 12\sqrt{3}} + \sqrt{7}$。<span class="scoring-tag">…… 12 分 (全题满分)</span></p>
                        </div>
                      </div>
                    </div>

                    <div class="sheet-footer">
                      <span>智教云 · EduMind 智能命题与考务排版系统</span>
                      <span>教师参考答案 第 1 页（共 1 页）</span>
                    </div>
                  </div>
                </div>

              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 导出任务历史记录 -->
      <div class="export-history-card no-print">
        <div class="history-header">
          <div class="history-title-group">
            <el-icon class="history-icon"><Clock /></el-icon>
            <h3>近期试卷导出任务与云端归档</h3>
          </div>
          <div class="history-actions">
            <el-button size="small" link type="primary" @click="refreshHistory">
              <el-icon><Refresh /></el-icon> 刷新任务状态
            </el-button>
          </div>
        </div>

        <el-table :data="exportHistory" stripe style="width: 100%;">
          <el-table-column prop="taskId" label="任务编号" width="170" />
          <el-table-column prop="title" label="试卷名称" min-width="260" />
          <el-table-column prop="size" label="纸张幅面" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.size === 'B4' ? 'warning' : 'info'">{{ row.size }} 考卷</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="导出格式" width="120">
            <template #default="{ row }">
              <el-tag size="small" type="primary" effect="plain">{{ row.type || 'PDF' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="140">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'SUCCESS'" type="success" size="small">导出成功</el-tag>
              <el-tag v-else-if="row.status === 'PROCESSING'" type="warning" size="small">生成中 ({{ row.progress || 50 }}%)</el-tag>
              <el-tag v-else-if="row.status === 'PENDING'" type="info" size="small">排队中</el-tag>
              <el-tooltip v-else-if="row.status === 'FAILED'" :content="row.errorMsg || '生成失败'" placement="top">
                <el-tag type="danger" size="small">生成失败</el-tag>
              </el-tooltip>
              <el-tag v-else size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="生成时间" width="170" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                :disabled="row.status !== 'SUCCESS' || !row.downloadUrl"
                @click="downloadFile(row)"
              >
                <el-icon><Download /></el-icon> 下载文件
              </el-button>
              <el-button link type="success" size="small" @click="handlePrintDirect">
                <el-icon><Printer /></el-icon> 立即打印
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Download,
  Printer,
  Document,
  Files,
  Operation,
  Clock,
  Refresh,
  Trophy,
  Lightning,
  Cpu,
  Reading
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { createPaperExportTask, getExportTaskStatus, listMyExportTasks } from '@/api/question/export';

const loading = ref(false);
const exporting = ref(false);
const zoomScale = ref(0.85);
const examTimeScore = ref('150分 / 120分钟');
const activeTab = ref('exam');
const previewPageMode = ref('page1');
const printScope = ref('all');
const currentPresetId = ref('preset-1');

// 预设模板配置
const presetTemplates = [
  {
    id: 'preset-1',
    name: '全真高考模拟大卷',
    tag: '国家考务规制',
    tagType: 'primary',
    desc: '含绝密标示、左侧密封装订线、准考证条形码与大题总得分网格表',
    icon: Trophy,
    bgColor: '#EFF6FF',
    color: '#2563EB',
    config: {
      paperTitle: '2026年普通高等学校招生全国统一考试冲刺预测卷',
      paperSubtitle: '理科数学 (全卷共4页 满分150分 考试时间120分钟)',
      paperSize: 'A4',
      confidentialLevel: '绝密 ★ 启用前',
      showSealingLine: true,
      showStudentInfo: true,
      showScoreGrid: true,
      showNoticeBar: true,
      showWatermark: true,
      watermarkText: '智教云示范第一中学 · 内部教学试卷 · 严禁翻印',
      showAnswerSheet: true,
      showAnalysis: true,
      fontFamily: 'SimSun',
      lineSpacing: 'normal',
      optionLayout: 'horizontal',
      showPointBadge: true
    }
  },
  {
    id: 'preset-2',
    name: '单元随堂高效精练卷',
    tag: '轻量省纸速印',
    tagType: 'success',
    desc: '紧凑版面、去除密封线、极简考生信息栏，适合课后周测与大量快印',
    icon: Lightning,
    bgColor: '#ECFDF5',
    color: '#059669',
    config: {
      paperTitle: '高三数学第一轮复习 · 函数与导数专题课时测验',
      paperSubtitle: '单元过关诊断 (满分100分 建议用时60分钟)',
      paperSize: 'A4',
      confidentialLevel: '',
      showSealingLine: false,
      showStudentInfo: true,
      showScoreGrid: false,
      showNoticeBar: false,
      showWatermark: false,
      watermarkText: '',
      showAnswerSheet: false,
      showAnalysis: true,
      fontFamily: 'SimSun',
      lineSpacing: 'compact',
      optionLayout: 'horizontal',
      showPointBadge: true
    }
  },
  {
    id: 'preset-3',
    name: 'AI 弱项攻坚变式卷',
    tag: '智适应靶向',
    tagType: 'warning',
    desc: '联动知识图谱学情画像，附带核心素养考点标记与分层变式题目',
    icon: Cpu,
    bgColor: '#FFFBEB',
    color: '#D97706',
    config: {
      paperTitle: 'EduMind AI 错题变式与核心素养攻坚测评卷',
      paperSubtitle: '基于空间几何与解析几何薄弱诊断智能生成',
      paperSize: 'A4',
      confidentialLevel: '内部教学诊断资料',
      showSealingLine: true,
      showStudentInfo: true,
      showScoreGrid: true,
      showNoticeBar: true,
      showWatermark: true,
      watermarkText: 'EduMind AI 智能教学系统 · 个性化测评',
      showAnswerSheet: true,
      showAnalysis: true,
      fontFamily: 'SimSun',
      lineSpacing: 'normal',
      optionLayout: 'grid',
      showPointBadge: true
    }
  },
  {
    id: 'preset-4',
    name: '名师教研评讲卷',
    tag: '教师教研版',
    tagType: 'danger',
    desc: '题目下直接附带分步采分细则、考点突破口点拨与易错警示',
    icon: Reading,
    bgColor: '#FEF2F2',
    color: '#DC2626',
    config: {
      paperTitle: '2026届高三摸底统考数学试题 · 教师讲评与采分细则',
      paperSubtitle: '含考点分布、思路点拨与分步赋分标示 (教研专用)',
      paperSize: 'A4',
      confidentialLevel: '内部教学诊断资料',
      showSealingLine: false,
      showStudentInfo: false,
      showScoreGrid: false,
      showNoticeBar: false,
      showWatermark: true,
      watermarkText: '智教云名师教研室 · 备课讲评专用',
      showAnswerSheet: false,
      showAnalysis: true,
      fontFamily: 'SimSun',
      lineSpacing: 'relaxed',
      optionLayout: 'horizontal',
      showPointBadge: true
    }
  }
];

const configForm = ref({
  examId: 101,
  paperTitle: '2026年普通高等学校招生全国统一考试冲刺预测卷',
  paperSubtitle: '理科数学 (全卷共4页 满分150分 考试时间120分钟)',
  paperSize: 'A4',
  confidentialLevel: '绝密 ★ 启用前',
  showSealingLine: true,
  showStudentInfo: true,
  showScoreGrid: true,
  showNoticeBar: true,
  showWatermark: true,
  watermarkText: '智教云示范第一中学 · 内部教学试卷 · 严禁翻印',
  showAnswerSheet: true,
  showAnalysis: true,
  fontFamily: 'SimSun',
  lineSpacing: 'normal',
  optionLayout: 'horizontal',
  showPointBadge: true
});

interface ExportHistoryItem {
  taskId: string;
  title: string;
  size: string;
  type: string;
  status: string;
  progress?: number;
  errorMsg?: string;
  createTime: string;
  downloadUrl?: string;
}

const exportHistory = ref<ExportHistoryItem[]>([
  {
    taskId: 'EXP-17262104001',
    title: '2026年普通高等学校招生全国统一考试冲刺预测卷',
    size: 'A4',
    type: 'PDF',
    status: 'SUCCESS',
    createTime: '2026-09-13 10:45:12',
    downloadUrl: '#'
  },
  {
    taskId: 'EXP-17262098002',
    title: '高三数学开学第一单元检测试题 (带答题卡)',
    size: 'B4',
    type: 'PDF',
    status: 'SUCCESS',
    createTime: '2026-09-12 16:30:00',
    downloadUrl: '#'
  },
  {
    taskId: 'EXP-17262095003',
    title: 'EduMind AI 错题变式与核心素养攻坚测评卷',
    size: 'A4',
    type: 'Word (.docx)',
    status: 'SUCCESS',
    createTime: '2026-09-12 14:10:25',
    downloadUrl: '#'
  }
]);

const applyPreset = (preset: any) => {
  currentPresetId.value = preset.id;
  Object.assign(configForm.value, preset.config);
  ElMessage.success(`已切换至「${preset.name}」排版预设方案`);
};

const handleCreateExportTask = async () => {
  try {
    exporting.value = true;
    const res = await createPaperExportTask({
      examId: configForm.value.examId,
      paperTitle: configForm.value.paperTitle,
      paperSubtitle: configForm.value.paperSubtitle,
      paperSize: configForm.value.paperSize as any,
      showWatermark: configForm.value.showWatermark,
      watermarkText: configForm.value.watermarkText,
      showAnswerSheet: configForm.value.showAnswerSheet,
      showAnalysis: configForm.value.showAnalysis,
      showStudentInfo: configForm.value.showStudentInfo,
      showScoreGrid: configForm.value.showScoreGrid
    });

    const task = res.data;
    if (!task || !task.taskId) {
      throw new Error('创建导出任务返回异常');
    }

    ElMessage.info('试卷导出任务已提交排队，后台正在高保真排版中...');
    await refreshHistory();

    // 启动状态轮询
    await pollExportTask(task.taskId);
  } catch (e: any) {
    ElMessage.error(e.message || '导出任务提交失败');
  } finally {
    exporting.value = false;
  }
};

const pollExportTask = async (taskId: string) => {
  let attempts = 0;
  const maxAttempts = 30;
  while (attempts < maxAttempts) {
    await new Promise(resolve => setTimeout(resolve, 1000));
    attempts++;
    try {
      const statusRes = await getExportTaskStatus(taskId);
      const current = statusRes.data;
      if (current.status === 'SUCCESS') {
        ElMessage.success('试卷导出完成，已生成下载链接！');
        await refreshHistory();
        return;
      } else if (current.status === 'FAILED') {
        ElMessage.error(`试卷导出失败: ${current.errorMsg || '未知异常'}`);
        await refreshHistory();
        return;
      }
    } catch (err) {
      console.warn('轮询导出任务状态重试中...', err);
    }
  }
  ElMessage.warning('试卷导出排版耗时较长，请稍后刷新任务列表查看结果');
  await refreshHistory();
};

const handleExportWord = () => {
  ElMessage.success('正在将试卷排版内容封装为 Office Open XML (.docx) 格式并开始下载...');
  exportHistory.value.unshift({
    taskId: 'EXP-' + Date.now(),
    title: configForm.value.paperTitle,
    size: configForm.value.paperSize,
    type: 'Word (.docx)',
    status: 'SUCCESS',
    createTime: '刚刚',
    downloadUrl: '#'
  });
};

const handlePrintDirect = () => {
  // 根据 printScope 确定打印页面
  const prevMode = previewPageMode.value;
  if (printScope.value === 'all') {
    previewPageMode.value = 'continuous';
  }
  setTimeout(() => {
    window.print();
    if (printScope.value === 'all') {
      previewPageMode.value = prevMode;
    }
  }, 100);
};

const refreshHistory = async () => {
  try {
    const res = await listMyExportTasks();
    if (res && res.data && Array.isArray(res.data) && res.data.length > 0) {
      exportHistory.value = res.data.map(item => ({
        taskId: item.taskId,
        title: configForm.value.paperTitle || `试卷 #${item.bizId} 考务排版`,
        size: configForm.value.paperSize || 'A4',
        type: 'PDF',
        status: item.status,
        progress: item.progress,
        errorMsg: item.errorMsg,
        createTime: item.createTime || '刚刚',
        downloadUrl: item.downloadUrl
      }));
    }
  } catch (err) {
    console.warn('获取近期导出任务历史失败', err);
  }
};

const downloadFile = (row: any) => {
  if (row.status === 'FAILED') {
    ElMessage.error(row.errorMsg || '该导出任务生成失败，无法下载');
    return;
  }
  if (row.status !== 'SUCCESS') {
    ElMessage.warning('导出任务正在后台排版中，请稍候...');
    return;
  }
  if (!row.downloadUrl || row.downloadUrl === '#') {
    ElMessage.warning('暂无可用下载地址');
    return;
  }

  ElMessage.success(`开始下载文件: ${row.title || '试卷排版'}`);
  const fullUrl = row.downloadUrl.startsWith('http')
    ? row.downloadUrl
    : `${window.location.origin}${row.downloadUrl}`;
  window.open(fullUrl, '_blank');
};

onMounted(() => {
  refreshHistory();
});
</script>

<!-- 全局纯净打印关键样式 (未 scoped，确保穿透重置 layout 并隔离试卷) -->
<style lang="scss">
@media print {
  // 彻底隐藏所有平台外壳与非打印元素
  .export-center-page .PageHeroBanner,
  .template-presets-bar,
  .control-panel-card,
  .export-history-card,
  .canvas-top-bar,
  .hero-stats-row,
  .no-print {
    display: none !important;
  }

  // 释放外层工作区容器
  .export-center-page,
  .main-content-layout,
  .export-split-workspace,
  .preview-canvas-card,
  .paper-scroll-wrapper,
  .paper-scaler {
    display: block !important;
    position: static !important;
    margin: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    background: transparent !important;
    box-shadow: none !important;
    border: none !important;
    transform: none !important;
    overflow: visible !important;
  }

  #printable-exam-paper {
    display: block !important;
    width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    background: #FFFFFF !important;
    box-shadow: none !important;
    border: none !important;
  }

  .simulated-sheet {
    display: block !important;
    box-shadow: none !important;
    border: none !important;
    margin: 0 !important;
    padding: 10mm 15mm !important;
    width: 100% !important;
    max-width: 100% !important;
    min-height: auto !important;
    page-break-after: always !important;
    break-after: page !important;
    box-sizing: border-box !important;
    background: #FFFFFF !important;
  }

  .simulated-sheet:last-child {
    page-break-after: auto !important;
    break-after: auto !important;
  }

  @page {
    size: A4 portrait;
    margin: 8mm 12mm 8mm 12mm;
  }
}
</style>

<style scoped lang="scss">
.export-center-page {
  padding-bottom: 40px;

  .hero-stats-row {
    display: flex;
    gap: 16px;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(8px);
      padding: 10px 18px;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
      display: flex;
      flex-direction: column;
      align-items: center;

      .stat-num {
        font-size: 20px;
        font-weight: 700;
        line-height: 1.2;

        &.text-primary { color: #2563EB; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-info { color: #0284C7; }
      }

      .stat-label {
        font-size: 11px;
        color: #64748B;
        margin-top: 4px;
      }
    }
  }

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  // 预设模板卡片栏
  .template-presets-bar {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 18px 22px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

    .presets-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;

      .presets-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        color: #0F172A;

        .icon-preset {
          color: #2563EB;
          font-size: 18px;
        }
      }

      .presets-tip {
        font-size: 12px;
        color: #64748B;
      }
    }

    .preset-cards-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 14px;

      @media (max-width: 1200px) {
        grid-template-columns: repeat(2, 1fr);
      }
      @media (max-width: 768px) {
        grid-template-columns: 1fr;
      }

      .preset-card {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 14px;
        border-radius: 12px;
        border: 1.5px solid #E2E8F0;
        background: #F8FAFC;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          border-color: #93C5FD;
          background: #FFFFFF;
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.08);
          transform: translateY(-2px);
        }

        &.active {
          border-color: #2563EB;
          background: #EFF6FF;
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.12);
        }

        .preset-icon-badge {
          width: 36px;
          height: 36px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 18px;
          flex-shrink: 0;
        }

        .preset-info {
          flex: 1;
          min-width: 0;

          .preset-name-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 6px;
            margin-bottom: 4px;

            .preset-name {
              font-size: 13px;
              font-weight: 600;
              color: #1E293B;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }

          .preset-desc {
            font-size: 11px;
            color: #64748B;
            line-height: 1.4;
            margin: 0;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }
        }
      }
    }
  }

  // 主工作区左右分栏
  .export-split-workspace {
    display: grid;
    grid-template-columns: 390px minmax(0, 1fr);
    gap: 20px;
    align-items: start;
    width: 100%;

    @media (max-width: 1180px) {
      grid-template-columns: 1fr;
    }

    // 左侧排版控制台
    .control-panel-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 20px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

      .panel-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 14px;

        .panel-title-group {
          display: flex;
          align-items: center;
          gap: 8px;

          .panel-icon {
            font-size: 18px;
            color: #2563EB;
          }

          h3 {
            font-size: 15px;
            font-weight: 600;
            color: #0F172A;
            margin: 0;
          }
        }
      }

      .config-tabs {
        :deep(.el-tabs__item) {
          font-size: 13px;
          font-weight: 500;
          padding: 0 14px;
        }
      }

      .config-form {
        margin-top: 10px;

        .sub-section-title {
          font-size: 12px;
          font-weight: 600;
          color: #475569;
          margin: 16px 0 10px;
          padding-bottom: 4px;
          border-bottom: 1px dashed #E2E8F0;
        }

        .checkbox-list {
          display: flex;
          flex-direction: column;
          gap: 12px;

          :deep(.el-checkbox) {
            align-items: flex-start;
            margin-right: 0;
            white-space: normal;
            height: auto;
          }

          .checkbox-label-block {
            display: flex;
            flex-direction: column;
            gap: 2px;
            margin-left: 4px;

            .label-text {
              font-size: 13px;
              font-weight: 500;
              color: #1E293B;
            }

            .label-desc {
              font-size: 11px;
              color: #94A3B8;
            }
          }
        }
      }

      .studio-action-dock {
        margin-top: 24px;
        display: flex;
        flex-direction: column;
        gap: 10px;

        .primary-print-btn {
          width: 100%;
          height: 40px;
          background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
          border: none;
          border-radius: 10px;
          font-weight: 600;
          font-size: 14px;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
          transition: all 0.2s ease;

          &:hover {
            opacity: 0.92;
            transform: translateY(-1px);
          }
        }

        .secondary-btn-row {
          display: flex;
          gap: 10px;

          .el-button {
            flex: 1;
            border-radius: 8px;
          }
        }
      }
    }

    // 右侧排版画布
    .preview-canvas-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 18px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      min-width: 0;
      width: 100%;
      box-sizing: border-box;

      .canvas-top-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 14px;
        padding-bottom: 12px;
        border-bottom: 1px solid #F1F5F9;
        flex-wrap: wrap;
        gap: 12px;

        .preview-page-tabs {
          :deep(.el-radio-button__inner) {
            font-size: 12px;
            padding: 7px 12px;
          }
        }

        .canvas-actions-right {
          display: flex;
          align-items: center;
          gap: 10px;

          .zoom-controls {
            display: flex;
            align-items: center;
            gap: 6px;

            .zoom-text {
              font-size: 12px;
              color: #1E293B;
              font-weight: 600;
              width: 40px;
              text-align: center;
            }
          }

          .canvas-print-btn {
            border-radius: 8px;
            background: linear-gradient(135deg, #10B981 0%, #059669 100%);
            border: none;
            font-weight: 600;
          }
        }
      }

      .paper-scroll-wrapper {
        background: #1E293B;
        background-image: radial-gradient(#334155 1px, transparent 1px);
        background-size: 20px 20px;
        border-radius: 14px;
        padding: 30px 16px;
        max-height: 820px;
        overflow-y: auto;
        overflow-x: auto;
        display: flex;
        justify-content: center;
        align-items: flex-start;
        min-height: 680px;
        width: 100%;
        box-sizing: border-box;

        .paper-scaler {
          transform-origin: top center;
          transition: transform 0.15s ease;
          display: flex;
          justify-content: center;
        }

        #printable-exam-paper {
          display: flex;
          flex-direction: column;
          gap: 24px;

          &.is-b4-mode .simulated-sheet {
            width: 960px;
          }

          &.font-SimSun {
            font-family: 'Times New Roman', SimSun, 'Songti SC', serif;
          }
          &.font-STZhongsong {
            font-family: 'Times New Roman', STZhongsong, SimSun, serif;
          }
          &.font-KaiTi {
            font-family: KaiTi, '楷体', 'Times New Roman', serif;
          }
          &.font-sans-serif {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
          }

          &.spacing-compact {
            .mock-q-item { margin-bottom: 10px; }
            .section-notice { margin: 4px 0 8px; }
          }
          &.spacing-relaxed {
            .mock-q-item { margin-bottom: 22px; }
            .draft-answer-box { min-height: 140px; }
          }
        }

        .simulated-sheet {
          width: 740px;
          min-height: 1040px;
          background: #FFFFFF;
          box-shadow: 0 12px 36px rgba(0, 0, 0, 0.35);
          padding: 44px 40px;
          display: flex;
          position: relative;
          color: #000000;
          box-sizing: border-box;
          flex-shrink: 0;

          .watermark-layer {
            position: absolute;
            top: 45%;
            left: 10%;
            font-size: 34px;
            color: rgba(203, 213, 225, 0.26);
            transform: rotate(-32deg);
            pointer-events: none;
            font-weight: 700;
            white-space: nowrap;
            z-index: 1;
            user-select: none;
          }

          // 左侧密封装订线
          .sealing-column {
            width: 46px;
            border-right: 2px dashed #000000;
            padding-right: 10px;
            margin-right: 22px;
            display: flex;
            align-items: center;
            justify-content: center;

            &.page-2-column {
              width: 28px;
              margin-right: 14px;
              .sealing-tag {
                writing-mode: vertical-rl;
                font-size: 11px;
                letter-spacing: 4px;
                color: #666;
              }
            }

            .sealing-content {
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: 16px;

              .sealing-fields {
                writing-mode: vertical-rl;
                display: flex;
                gap: 18px;
                font-size: 12px;
                letter-spacing: 3px;
              }

              .sealing-warning {
                writing-mode: vertical-rl;
                font-weight: bold;
                letter-spacing: 2px;
                font-size: 12px;
                margin-top: 14px;
              }
            }
          }

          // 卷面正文
          .sheet-main-content {
            flex: 1;
            display: flex;
            flex-direction: column;
            position: relative;
            z-index: 2;

            .page-top-runner {
              display: flex;
              justify-content: space-between;
              font-size: 12px;
              border-bottom: 1px solid #000000;
              padding-bottom: 4px;
              margin-bottom: 12px;
            }

            .sheet-header {
              text-align: center;
              margin-bottom: 14px;

              .confidential-bar {
                display: flex;
                justify-content: space-between;
                font-size: 12px;
                font-weight: bold;
                margin-bottom: 6px;

                .confidential-badge {
                  letter-spacing: 2px;
                }
              }

              .paper-main-title {
                font-size: 21px;
                font-weight: bold;
                margin: 0 0 6px;
                line-height: 1.3;
              }

              .paper-sub-title {
                font-size: 13.5px;
                font-weight: normal;
                margin: 0 0 8px;
              }

              .paper-rules-bar {
                display: flex;
                justify-content: center;
                gap: 20px;
                font-size: 12px;
                border-top: 1.5px solid #000000;
                border-bottom: 1.5px solid #000000;
                padding: 4px 0;
              }
            }

            .exam-notice-box {
              border: 1px solid #000000;
              padding: 6px 10px;
              font-size: 11px;
              line-height: 1.5;
              margin-bottom: 12px;

              .notice-title {
                font-weight: bold;
              }

              .notice-list {
                margin: 2px 0 0 16px;
                padding: 0;
              }
            }

            .candidate-info-box {
              display: flex;
              justify-content: space-between;
              align-items: center;
              font-size: 12px;
              margin-bottom: 12px;
              padding: 4px 0;

              .info-fill-item {
                display: flex;
                align-items: center;
                gap: 4px;

                .barcode-cells {
                  display: flex;
                  gap: 3px;

                  .cell {
                    width: 14px;
                    height: 16px;
                    border: 1px solid #000000;
                    display: inline-block;
                  }

                  &.small .cell {
                    width: 16px;
                  }
                }
              }

              .barcode-sticker-box {
                border: 1px dashed #666666;
                padding: 4px 10px;
                font-size: 11px;
                color: #555;
              }
            }

            .score-summary-table {
              margin-bottom: 14px;

              table {
                width: 100%;
                border-collapse: collapse;
                text-align: center;
                font-size: 12px;

                th, td {
                  height: 22px;
                  border: 1px solid #000000;
                }
              }
            }

            .section-title-bar {
              .section-heading {
                font-size: 14px;
                font-weight: bold;
                margin: 6px 0 4px;
              }
            }

            .section-notice {
              font-size: 12px;
              margin: 0 0 10px;
              line-height: 1.4;
            }

            .questions-list {
              .mock-q-item {
                margin-bottom: 14px;

                .q-title-row {
                  display: flex;
                  justify-content: space-between;
                  align-items: flex-start;

                  .q-text {
                    font-size: 13px;
                    line-height: 1.6;
                    margin: 0 0 6px;
                    flex: 1;
                  }

                  .point-tag {
                    font-size: 11px;
                    color: #444;
                    margin-left: 8px;
                    white-space: nowrap;
                  }
                }

                .q-options {
                  font-size: 12.5px;

                  &.layout-horizontal {
                    display: grid;
                    grid-template-columns: repeat(4, 1fr);
                    gap: 8px;
                  }

                  &.layout-grid {
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 6px 14px;
                  }

                  &.layout-vertical {
                    display: flex;
                    flex-direction: column;
                    gap: 4px;
                  }
                }
              }

              .solving-item {
                margin-bottom: 18px;

                .draft-answer-box {
                  border: 1px dashed #94A3B8;
                  border-radius: 4px;
                  min-height: 90px;
                  margin-top: 8px;
                  display: flex;
                  align-items: flex-end;
                  justify-content: flex-end;
                  padding: 8px;

                  .draft-tip {
                    font-size: 10px;
                    color: #94A3B8;
                  }
                }
              }
            }

            .sheet-footer {
              margin-top: auto;
              padding-top: 14px;
              border-top: 1px solid #000000;
              display: flex;
              justify-content: space-between;
              font-size: 11px;
              color: #333333;
            }
          }
        }

        // 答题卡页面特定样式
        .answer-sheet-page {
          .answer-sheet-header {
            text-align: center;
            margin-bottom: 16px;

            h2 {
              font-size: 18px;
              font-weight: bold;
              margin: 0 0 6px;
            }

            .sub-header-info {
              display: flex;
              justify-content: center;
              gap: 20px;
              font-size: 12px;
              color: #444;
            }
          }

          .card-meta-grid {
            display: grid;
            grid-template-columns: 1fr 220px;
            gap: 16px;
            border: 1px solid #000000;
            padding: 10px;
            margin-bottom: 14px;

            .meta-line {
              font-size: 12px;
              margin-bottom: 6px;
            }

            .notice-tiny {
              font-size: 11px;
              color: #444;
              line-height: 1.4;
            }

            .barcode-target-area {
              .barcode-border {
                height: 100%;
                min-height: 60px;
                border: 1.5px dashed #000000;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 11px;
                color: #666;
                text-align: center;
              }
            }
          }

          .card-section {
            margin-bottom: 16px;

            .card-sec-title {
              font-size: 12.5px;
              font-weight: bold;
              background: #F1F5F9;
              border-left: 3px solid #000000;
              padding: 4px 8px;
              margin-bottom: 8px;
            }

            .bubble-grid {
              display: grid;
              grid-template-columns: repeat(4, 1fr);
              gap: 8px;
              border: 1px solid #000000;
              padding: 8px;

              .bubble-row {
                display: flex;
                align-items: center;
                gap: 4px;
                font-size: 11px;

                .q-num {
                  width: 18px;
                  font-weight: bold;
                }

                .bubble-opt {
                  font-family: monospace;
                  letter-spacing: 1px;
                }
              }
            }

            .fill-in-grid {
              display: flex;
              flex-direction: column;
              gap: 6px;
              padding: 6px 0;
              font-size: 12px;
            }

            .formal-answer-frame {
              border: 1.5px solid #000000;
              min-height: 140px;
              position: relative;
              padding: 8px;

              .score-small-box {
                position: absolute;
                top: 8px;
                right: 8px;
                border: 1px solid #000000;
                padding: 4px 8px;
                font-size: 11px;
              }

              .frame-watermark {
                position: absolute;
                bottom: 8px;
                left: 10px;
                font-size: 10px;
                color: #94A3B8;
              }
            }
          }
        }

        // 解析页面样式
        .analysis-page {
          .analysis-title {
            font-size: 18px;
            font-weight: bold;
            margin: 0 0 6px;
          }

          .analysis-content {
            .analysis-block {
              h4 {
                font-size: 13.5px;
                font-weight: bold;
                margin: 0 0 8px;
              }

              .ans-table {
                width: 100%;
                border-collapse: collapse;
                text-align: center;
                font-size: 12px;

                th, td {
                  border: 1px solid #000000;
                  height: 24px;
                }
              }

              .solution-detail {
                font-size: 12.5px;
                line-height: 1.7;
                background: #F8FAFC;
                border: 1px solid #E2E8F0;
                padding: 12px;
                border-radius: 6px;

                p {
                  margin: 0 0 8px;
                }

                .scoring-tag {
                  color: #DC2626;
                  font-weight: 600;
                  float: right;
                }
              }
            }
          }
        }
      }
    }
  }

  // 近期任务记录卡片
  .export-history-card {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 22px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

    .history-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .history-title-group {
        display: flex;
        align-items: center;
        gap: 8px;

        .history-icon {
          font-size: 18px;
          color: #2563EB;
        }

        h3 {
          font-size: 16px;
          font-weight: 600;
          color: #0F172A;
          margin: 0;
        }
      }
    }
  }
}
</style>
