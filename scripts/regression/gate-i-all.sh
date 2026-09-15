#!/usr/bin/env bash
# =============================================================================
# 智教云 · EduMind V2.0 RC 全量门禁与回归验证一键脚本 (Bash)
# =============================================================================

# 宽松错误捕获模式：各测试项收集实际退出码后统一在末尾汇总呈现，不中途静默中断
set +e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
FRONTEND_DIR="${ROOT_DIR}/frontend"

echo "============================================================================="
echo " 智教云 · EduMind V2.0 RC 全量回归测试套件启动"
echo " 工作区根目录: ${ROOT_DIR}"
echo "============================================================================="

declare -a STEP_NAMES
declare -a STEP_STATUS
declare -a STEP_DURATIONS

run_step() {
    local name="$1"
    local workdir="$2"
    local cmd="$3"
    local max_retries="${4:-0}"

    echo ""
    echo ">>> [RUNNING] ${name} ..."
    local start_time=$(date +%s)

    cd "${workdir}"
    local exit_code=1
    local attempt=0
    while [ ${attempt} -le ${max_retries} ]; do
        if [ ${attempt} -gt 0 ]; then
            echo ">>> [RETRY #${attempt}] ${name} (Retrying once)..."
        fi
        exit_code=0
        eval "${cmd}" || exit_code=$?
        if [ ${exit_code} -eq 0 ]; then
            break
        fi
        attempt=$((attempt + 1))
    done

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    STEP_NAMES+=("${name}")
    STEP_DURATIONS+=("${duration}s")

    if [ ${exit_code} -eq 0 ]; then
        STEP_STATUS+=("PASS")
        echo ">>> [PASS] ${name} (耗时: ${duration}s)"
    else
        STEP_STATUS+=("FAIL")
        echo ">>> [FAIL] ${name} (耗时: ${duration}s, ExitCode: ${exit_code})"
    fi

    return ${exit_code}
}

# 1. 后端依赖全量编译安装
run_step "1. Backend Clean & Install" "${BACKEND_DIR}" "mvn clean install -DskipTests -pl edu-mind-boot -am"

# 2. Gate I1~I11 专项集成测试
run_step "Gate I1/I2: TenantQuota & DataIsolation" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=TenantQuotaIntegrationTest,TenantDataIsolationIntegrationTest"
run_step "Gate I3: NotificationBroadcast" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=NotificationBroadcastIntegrationTest"
run_step "Gate I4: KnowledgeOcr" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=KnowledgeOcrIntegrationTest"
run_step "Gate I5/I8: TeachingIntervention" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=TeachingInterventionIntegrationTest"
run_step "Gate I6: AgentMemory" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=AgentMemoryIntegrationTest"
run_step "Gate I7: ExportTask" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=ExportTaskIntegrationTest"
run_step "Gate I9: SecurityKeyKms" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=SecurityKeyKmsIntegrationTest"
run_step "Gate I10: AiModelKeyKms" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=AiModelKeyKmsIntegrationTest"
run_step "Gate I11: OperationLog (Round 1)" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=OperationLogIntegrationTest" 1
run_step "Gate I11: OperationLog (Round 2 幂等)" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -Dtest=OperationLogIntegrationTest" 1

# 3. V1.1 历史门禁保绿
run_step "Baseline: GateV10 (V1.0)" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -DgateG.integration=true -Dtest=GateV10IntegrationTest"
run_step "Baseline: GateV11 (V1.1)" "${BACKEND_DIR}" "mvn test -pl edu-mind-boot -DgateH.integration=true -Dtest=GateV11IntegrationTest"

# 4. 前端构建校验
run_step "Frontend Build (TypeCheck & Vite)" "${FRONTEND_DIR}" "npm run build"

# 5. 输出汇总表
echo ""
echo "============================================================================="
echo " 智教云 · EduMind V2.0 RC 全量回归汇总报告"
echo "============================================================================="
printf "%-45s | %-8s | %-10s\n" "Step" "Status" "Duration"
echo "-----------------------------------------------------------------------------"
all_pass=true
for i in "${!STEP_NAMES[@]}"; do
    printf "%-45s | %-8s | %-10s\n" "${STEP_NAMES[$i]}" "${STEP_STATUS[$i]}" "${STEP_DURATIONS[$i]}"
    if [ "${STEP_STATUS[$i]}" != "PASS" ]; then
        all_pass=false
    fi
done

if [ "${all_pass}" = true ]; then
    echo ""
    echo "[SUCCESS] 全部测试与构建通过！V2.0 RC 满足工程冻结标准！"
    exit 0
else
    echo ""
    echo "[FAILURE] 存在未通过环节，请检查上述错误日志！"
    exit 1
fi
