import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import type { SmsLogRecord, EmailLogRecord } from '@/types/system/config';

interface SystemConfigModalDeps {
  triggerPaymentTest: (type: 'wechat' | 'alipay') => Promise<{
    type: 'wechat' | 'alipay';
    orderNo: string;
    qrcode: string;
    amount: string;
    status: 'PENDING' | 'PAID';
  } | null | undefined>;
  triggerSmsTest: (phone: string, templateCode?: string) => Promise<void>;
  sendTestEmail: (toEmail: string) => Promise<void>;
  fetchSmsLogs: (params: {
    page: number;
    size: number;
    phone?: string;
    status?: number;
  }) => Promise<{ list: SmsLogRecord[]; total: number }>;
  fetchEmailLogs: (params: {
    page: number;
    size: number;
    email?: string;
    status?: number;
  }) => Promise<{ list: EmailLogRecord[]; total: number }>;
}

export function useSystemConfigModals(deps: SystemConfigModalDeps) {
  const showPaymentModal = ref(false);
  const payOrderStatus = ref<'PENDING' | 'PAID'>('PENDING');
  const payStatusRefreshing = ref(false);
  const paymentResult = reactive({
    type: '' as 'wechat' | 'alipay' | '',
    orderNo: '',
    qrcode: '',
    amount: '0.01'
  });

  async function handleTriggerPaymentTest(type: 'wechat' | 'alipay') {
    try {
      const result = await deps.triggerPaymentTest(type);
      if (result) {
        paymentResult.type = result.type;
        paymentResult.orderNo = result.orderNo;
        paymentResult.qrcode = result.qrcode;
        paymentResult.amount = result.amount;
        payOrderStatus.value = result.status;
        showPaymentModal.value = true;
      }
    } catch {
      // error handled in composable
    }
  }

  function handleCheckPaymentStatus() {
    payStatusRefreshing.value = true;
    setTimeout(() => {
      payStatusRefreshing.value = false;
      payOrderStatus.value = 'PAID';
      ElMessage.success('支付成功');
    }, 600);
  }

  const showSmsLogsModal = ref(false);
  const smsLogsLoading = ref(false);
  const smsLogsList = ref<SmsLogRecord[]>([]);
  const smsSearch = reactive({
    phone: '',
    status: undefined as number | undefined
  });
  const smsPage = reactive({
    page: 1,
    size: 10,
    total: 0
  });

  async function handleTriggerSmsTest(phone: string, templateCode?: string) {
    try {
      await deps.triggerSmsTest(phone, templateCode);
    } catch {
      // error handled in composable
    }
  }

  function handleOpenSmsLogs() {
    showSmsLogsModal.value = true;
  }

  async function loadSmsLogsData() {
    smsLogsLoading.value = true;
    try {
      const [result] = await Promise.all([
        deps.fetchSmsLogs({
          page: smsPage.page,
          size: smsPage.size,
          phone: smsSearch.phone ? smsSearch.phone.trim() : undefined,
          status: smsSearch.status
        }),
        new Promise((resolve) => setTimeout(resolve, 220))
      ]);
      smsLogsList.value = result.list;
      smsPage.total = result.total;
    } catch (err: any) {
      ElMessage.error(err?.message || '获取短信记录失败');
    } finally {
      smsLogsLoading.value = false;
    }
  }

  function handleResetSmsSearch() {
    smsSearch.phone = '';
    smsSearch.status = undefined;
    smsPage.page = 1;
    loadSmsLogsData();
  }

  function handleSmsPageSizeChange(size: number) {
    smsPage.size = size;
    smsPage.page = 1;
    loadSmsLogsData();
  }

  const showEmailLogsModal = ref(false);
  const emailLogsLoading = ref(false);
  const emailLogsList = ref<EmailLogRecord[]>([]);
  const emailSearch = reactive({
    email: '',
    status: undefined as number | undefined
  });
  const emailPage = reactive({
    page: 1,
    size: 10,
    total: 0
  });

  async function handleSendTestEmail(toEmail: string) {
    try {
      await deps.sendTestEmail(toEmail);
    } catch {
      // error handled in composable
    }
  }

  function handleOpenEmailLogs() {
    showEmailLogsModal.value = true;
  }

  async function loadEmailLogsData() {
    emailLogsLoading.value = true;
    try {
      const [result] = await Promise.all([
        deps.fetchEmailLogs({
          page: emailPage.page,
          size: emailPage.size,
          email: emailSearch.email ? emailSearch.email.trim() : undefined,
          status: emailSearch.status
        }),
        new Promise((resolve) => setTimeout(resolve, 220))
      ]);
      emailLogsList.value = result.list;
      emailPage.total = result.total;
    } catch (err: any) {
      ElMessage.error(err?.message || '获取邮件记录失败');
    } finally {
      emailLogsLoading.value = false;
    }
  }

  function handleResetEmailSearch() {
    emailSearch.email = '';
    emailSearch.status = undefined;
    emailPage.page = 1;
    loadEmailLogsData();
  }

  function handleEmailPageSizeChange(size: number) {
    emailPage.size = size;
    emailPage.page = 1;
    loadEmailLogsData();
  }

  return {
    showPaymentModal,
    payOrderStatus,
    payStatusRefreshing,
    paymentResult,
    handleTriggerPaymentTest,
    handleCheckPaymentStatus,
    showSmsLogsModal,
    smsLogsLoading,
    smsLogsList,
    smsSearch,
    smsPage,
    handleTriggerSmsTest,
    handleOpenSmsLogs,
    loadSmsLogsData,
    handleResetSmsSearch,
    handleSmsPageSizeChange,
    showEmailLogsModal,
    emailLogsLoading,
    emailLogsList,
    emailSearch,
    emailPage,
    handleSendTestEmail,
    handleOpenEmailLogs,
    loadEmailLogsData,
    handleResetEmailSearch,
    handleEmailPageSizeChange
  };
}
