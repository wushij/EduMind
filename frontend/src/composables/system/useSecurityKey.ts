import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  listSecurityKeys,
  rotateSecurityKey,
  testSecurityKeyCrypto
} from '@/api/system/security-key';
import type {
  SecurityKeyVersionVO,
  SecurityKeyCryptoTestResponse
} from '@/types/system/security-key';
import { useTenantStore } from '@/stores/system/tenant';

export function filterSecurityKeys(
  list: SecurityKeyVersionVO[],
  aliasFilter: string,
  statusFilter: string,
  searchKeyword: string
): SecurityKeyVersionVO[] {
  return list.filter(item => {
    if (aliasFilter && item.keyAlias !== aliasFilter) {
      return false;
    }
    if (statusFilter && item.status !== statusFilter) {
      return false;
    }
    if (searchKeyword.trim()) {
      const kw = searchKeyword.trim().toLowerCase();
      const matchAlias = item.keyAlias?.toLowerCase().includes(kw);
      const matchVer = `v${item.keyVersion}`.toLowerCase().includes(kw);
      const matchFp = item.keyFingerprint?.toLowerCase().includes(kw);
      const matchAlgo = item.algorithm?.toLowerCase().includes(kw);
      if (!matchAlias && !matchVer && !matchFp && !matchAlgo) {
        return false;
      }
    }
    return true;
  });
}

export function paginateList<T>(list: T[], pageNum: number, pageSize: number): T[] {
  const start = (pageNum - 1) * pageSize;
  return list.slice(start, start + pageSize);
}

export function getActiveKeyByAlias(
  list: SecurityKeyVersionVO[],
  alias: string
): SecurityKeyVersionVO | null {
  return list.find(k => k.keyAlias === alias && k.status === 'ACTIVE') || null;
}

export function getActiveVerByAlias(list: SecurityKeyVersionVO[], alias?: string): number {
  if (!alias) return 1;
  const match = list.find(k => k.keyAlias === alias && k.status === 'ACTIVE');
  return match?.keyVersion || 1;
}

export function getDistinctAliases(list: SecurityKeyVersionVO[]): string[] {
  return Array.from(new Set(list.map(k => k.keyAlias))).filter(Boolean) as string[];
}

export function useSecurityKey() {
  const tenantStore = useTenantStore();
  const loading = ref(false);
  const refreshing = ref(false);
  const rotating = ref(false);
  const testingCrypto = ref(false);

  const aliasFilter = ref('');
  const statusFilter = ref('');
  const searchKeyword = ref('');
  const pageNum = ref(1);
  const pageSize = ref(10);

  const keyList = ref<SecurityKeyVersionVO[]>([]);

  const activeDataKey = computed(() => getActiveKeyByAlias(keyList.value, 'edumind-data-key'));
  const activeModelKey = computed(() => getActiveKeyByAlias(keyList.value, 'edumind-model-key'));
  const distinctAliases = computed(() => getDistinctAliases(keyList.value));

  const filteredKeyList = computed(() =>
    filterSecurityKeys(keyList.value, aliasFilter.value, statusFilter.value, searchKeyword.value)
  );

  const paginatedKeyList = computed(() =>
    paginateList(filteredKeyList.value, pageNum.value, pageSize.value)
  );

  async function fetchList(silent = false) {
    if (!silent) {
      loading.value = true;
    }
    try {
      const [res] = await Promise.all([
        listSecurityKeys(aliasFilter.value || undefined),
        !silent ? new Promise((resolve) => setTimeout(resolve, 220)) : Promise.resolve()
      ]);
      if (res?.data && Array.isArray(res.data)) {
        keyList.value = res.data;
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '获取国密密钥资产列表失败';
      ElMessage.error(message);
    } finally {
      if (!silent) {
        loading.value = false;
      }
    }
  }

  async function handleRefreshAll() {
    refreshing.value = true;
    try {
      await fetchList(true);
      ElMessage.success('国密 KMS 密钥与安全状态已同步更新');
    } finally {
      refreshing.value = false;
    }
  }

  function handleSearch() {
    pageNum.value = 1;
    fetchList();
  }

  function handleFilterChange() {
    pageNum.value = 1;
    fetchList();
  }

  function handleReset() {
    aliasFilter.value = '';
    statusFilter.value = '';
    searchKeyword.value = '';
    pageNum.value = 1;
    fetchList();
  }

  const cryptoTestModalVisible = ref(false);
  const testForm = ref<{
    keyAlias: string;
    keyVersion?: number;
    operation: 'ENCRYPT' | 'DECRYPT';
    text: string;
  }>({
    keyAlias: 'edumind-data-key',
    operation: 'ENCRYPT',
    text: '学生张三（学号 S2026001）高考数学模拟考立体几何弱项诊断画像与个性化辅导策略建议'
  });
  const testResult = ref<SecurityKeyCryptoTestResponse | null>(null);

  function openCryptoTestModal(targetKey?: SecurityKeyVersionVO | null) {
    testForm.value = {
      keyAlias: targetKey?.keyAlias || 'edumind-data-key',
      keyVersion: targetKey?.keyVersion,
      operation: 'ENCRYPT',
      text: targetKey?.keyAlias?.includes('model')
        ? 'sk-deepseek-v3-ai-prod-master-key-credential-918237198237'
        : '学生张三（学号 S2026001）高考数学模拟考立体几何弱项诊断画像与个性化辅导策略建议'
    };
    testResult.value = null;
    cryptoTestModalVisible.value = true;
  }

  function loadSampleText(type: 'student_memory' | 'model_key') {
    if (type === 'student_memory') {
      testForm.value.keyAlias = 'edumind-data-key';
      testForm.value.operation = 'ENCRYPT';
      testForm.value.text = '学生张三（学号 S2026001）高考数学模拟考立体几何弱项诊断画像与个性化辅导策略建议';
    } else {
      testForm.value.keyAlias = 'edumind-model-key';
      testForm.value.operation = 'ENCRYPT';
      testForm.value.text = 'sk-deepseek-v3-ai-prod-master-key-credential-918237198237';
    }
  }

  async function executeCryptoTest() {
    if (!testForm.value.text.trim()) {
      ElMessage.warning('请输入测试内容');
      return;
    }
    testingCrypto.value = true;
    testResult.value = null;
    try {
      const res = await testSecurityKeyCrypto({
        keyAlias: testForm.value.keyAlias,
        keyVersion: testForm.value.keyVersion,
        operation: testForm.value.operation,
        text: testForm.value.text.trim()
      });
      testResult.value = res.data;
      if (res.data.success) {
        ElMessage.success(testForm.value.operation === 'ENCRYPT' ? '国密 SM4-GCM 认证加密执行成功' : '国密 SM4-GCM 认证解密还原成功');
      } else {
        ElMessage.warning('执行受阻：Fail-Closed 保护已生效');
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '执行自检测试失败';
      ElMessage.error(message);
    } finally {
      testingCrypto.value = false;
    }
  }

  function flipToDecrypt() {
    if (!testResult.value?.resultText) return;
    testForm.value.operation = 'DECRYPT';
    testForm.value.text = testResult.value.resultText;
    testResult.value = null;
  }

  const rotateDialogVisible = ref(false);
  const selectedRotateAlias = ref('edumind-data-key');

  function openRotateModal(alias?: string) {
    selectedRotateAlias.value = alias || 'edumind-data-key';
    rotateDialogVisible.value = true;
  }

  async function executeRotate() {
    rotating.value = true;
    try {
      const res = await rotateSecurityKey({
        keyAlias: selectedRotateAlias.value
      });
      ElMessage.success(`密钥【${selectedRotateAlias.value}】轮换成功，全新激活版本: v${res.data?.keyVersion || '新'}`);
      rotateDialogVisible.value = false;
      await fetchList();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '执行密钥轮换失败';
      ElMessage.error(message);
    } finally {
      rotating.value = false;
    }
  }

  const detailDrawerVisible = ref(false);
  const selectedKeyDetail = ref<SecurityKeyVersionVO | null>(null);

  function openDetailDrawer(row: SecurityKeyVersionVO) {
    selectedKeyDetail.value = row;
    detailDrawerVisible.value = true;
  }

  async function copyText(text?: string) {
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      ElMessage.success('已复制到剪贴板');
    } catch {
      ElMessage.error('复制失败，请手动选择复制');
    }
  }

  function resolveActiveVerByAlias(alias?: string): number {
    return getActiveVerByAlias(keyList.value, alias);
  }

  onMounted(() => {
    fetchList();
  });

  return {
    tenantStore,
    loading,
    refreshing,
    rotating,
    testingCrypto,
    aliasFilter,
    statusFilter,
    searchKeyword,
    pageNum,
    pageSize,
    keyList,
    activeDataKey,
    activeModelKey,
    distinctAliases,
    filteredKeyList,
    paginatedKeyList,
    fetchList,
    handleRefreshAll,
    handleSearch,
    handleFilterChange,
    handleReset,
    cryptoTestModalVisible,
    testForm,
    testResult,
    openCryptoTestModal,
    loadSampleText,
    executeCryptoTest,
    flipToDecrypt,
    rotateDialogVisible,
    selectedRotateAlias,
    openRotateModal,
    executeRotate,
    detailDrawerVisible,
    selectedKeyDetail,
    openDetailDrawer,
    copyText,
    getActiveVerByAlias: resolveActiveVerByAlias
  };
}
