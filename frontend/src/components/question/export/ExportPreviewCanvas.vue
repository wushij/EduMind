<template>
<div class="preview-canvas-card">
  <!-- 画布顶部控制条 -->
  <div class="canvas-top-bar no-print">
    <div class="preview-page-tabs">
      <el-radio-group v-model="previewPageMode" size="small">
        <el-radio-button label="page1">第 1 页 · 客观选择与填空</el-radio-button>
        <el-radio-button label="page2">第 2 页 · 解答题与压轴题</el-radio-button>
        <el-radio-button label="page3" v-if="configForm.showAnswerSheet">第 3 页 · 标准答题卡</el-radio-button>
        <el-radio-button label="page4" v-if="configForm.showAnalysis">第 4 页 · 踩分细则与解析</el-radio-button>
        <el-radio-button label="continuous">连续整卷排版</el-radio-button>
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
</template>

<script setup lang="ts">
import { Printer } from '@element-plus/icons-vue';
import type { ExportConfigForm } from '@/composables/question/useExport';

defineProps<{
  configForm: ExportConfigForm;
  handlePrintDirect: () => void;
}>();

const previewPageMode = defineModel<string>('previewPageMode', { required: true });
const zoomScale = defineModel<number>('zoomScale', { required: true });
const printScope = defineModel<string>('printScope', { required: true });

</script>

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
</style>
