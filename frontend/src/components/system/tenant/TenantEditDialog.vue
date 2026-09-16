<template>
  <el-dialog
    v-model="visible"
    :title="isEditing ? '编辑学校/租户基本信息' : '入驻新学校/集团租户'"
    width="640px"
    destroy-on-close
    class="tenant-edit-dialog"
    @closed="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="tenant-form"
    >
      <!-- 套餐方案长圆选择器 -->
      <div class="plan-selector-block">
        <label class="form-label-title">选择入驻资源套餐方案</label>
        <div class="plan-cards-row">
          <div
            v-for="plan in planOptions"
            :key="plan.code"
            class="plan-card-pill"
            :class="[plan.code.toLowerCase(), { 'is-selected': form.planCode === plan.code }]"
            @click="form.planCode = plan.code"
          >
            <div class="plan-top">
              <el-icon class="plan-icon"><component :is="plan.icon" /></el-icon>
              <span class="plan-name">{{ plan.name }}</span>
            </div>
            <div class="plan-specs">
              <span>{{ plan.tokens }}</span>
              <span>{{ plan.storage }}</span>
              <span>{{ plan.seats }}</span>
            </div>
            <div class="plan-select-indicator" v-if="form.planCode === plan.code">
              <el-icon><Check /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="学校/租户编码 (英文标识)" prop="tenantCode">
            <el-input
              v-model="form.tenantCode"
              placeholder="如：ECNU_HIGH / PKU_EXP"
              :disabled="isEditing"
              class="pill-input"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="学校/机构全称" prop="name">
            <el-input
              v-model="form.name"
              placeholder="如：华东师范大学附属第二实验中学"
              class="pill-input"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="专属二级访问域名" prop="domain">
            <el-input
              v-model="form.domain"
              placeholder="如：ecnu.edumind.edu.cn"
              class="pill-input"
            >
              <template #prepend>https://</template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="服务授权到期时间" prop="expireTime">
            <el-date-picker
              v-model="form.expireTime"
              type="date"
              placeholder="选择到期日"
              style="width: 100%;"
              value-format="YYYY-MM-DD"
              class="pill-date-picker"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        <span class="divider-text">系统管理员初始账号 (校级超管)</span>
      </el-divider>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="管理员姓名" prop="adminName">
            <el-input
              v-model="form.adminName"
              placeholder="如：李校长 / 信息中心王主任"
              class="pill-input"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号码 (作为登录账号)" prop="adminPhone">
            <el-input
              v-model="form.adminPhone"
              placeholder="如：13800000000"
              class="pill-input"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16" v-if="!isEditing">
        <el-col :span="12">
          <el-form-item label="初始登录密码 (留空默认 123456)">
            <el-input
              v-model="form.adminPassword"
              type="password"
              placeholder="默认密码：123456"
              show-password
              class="pill-input"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="租户运行状态">
            <div class="status-radio-group">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常服务</el-radio>
                <el-radio :value="0">暂缓启用</el-radio>
              </el-radio-group>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <div class="modal-footer">
        <el-button class="pill-btn" @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          class="pill-btn confirm-btn"
          :loading="submitting"
          @click="handleSubmit"
        >
          {{ isEditing ? '保存修改' : '确认入驻设立' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { Check, Trophy, Promotion, HelpFilled } from '@element-plus/icons-vue';
import { createTenant, updateTenant } from '@/composables/system/useTenant';
import type { TenantListVO } from '@/types/system/tenant';

const props = defineProps<{
  modelValue: boolean;
  tenant?: TenantListVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'saved'): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const isEditing = computed(() => !!props.tenant?.id);
const submitting = ref(false);
const formRef = ref();

const planOptions = [
  {
    code: 'FLAGSHIP',
    name: '尊享旗舰版',
    icon: Trophy,
    tokens: '5,000万 Token',
    storage: '500GB 存储',
    seats: '2,000 席位'
  },
  {
    code: 'PRO',
    name: '高配专业版',
    icon: Promotion,
    tokens: '3,000万 Token',
    storage: '300GB 存储',
    seats: '1,000 席位'
  },
  {
    code: 'STANDARD',
    name: '敏捷标准版',
    icon: HelpFilled,
    tokens: '1,500万 Token',
    storage: '100GB 存储',
    seats: '500 席位'
  }
];

const form = ref({
  tenantCode: '',
  name: '',
  domain: '',
  planCode: 'PRO',
  expireTime: '2028-12-31',
  adminName: '',
  adminPhone: '',
  adminPassword: '',
  status: 1
});

const rules = {
  tenantCode: [{ required: true, message: '请输入租户唯一编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入学校/机构全称', trigger: 'blur' }],
  adminName: [{ required: true, message: '请输入管理员姓名', trigger: 'blur' }],
  adminPhone: [{ required: true, message: '请输入管理员手机号码', trigger: 'blur' }]
};

const initForm = () => {
  if (props.tenant) {
    form.value = {
      tenantCode: props.tenant.tenantCode || props.tenant.code || '',
      name: props.tenant.name || '',
      domain: props.tenant.domain || '',
      planCode: props.tenant.planCode || 'PRO',
      expireTime: props.tenant.expireTime ? props.tenant.expireTime.substring(0, 10) : '2028-12-31',
      adminName: props.tenant.adminName || '',
      adminPhone: props.tenant.adminPhone || '',
      adminPassword: '',
      status: props.tenant.status ?? 1
    };
  } else {
    form.value = {
      tenantCode: '',
      name: '',
      domain: '',
      planCode: 'PRO',
      expireTime: '2028-12-31',
      adminName: '',
      adminPhone: '',
      adminPassword: '',
      status: 1
    };
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    try {
      submitting.value = true;
      if (isEditing.value && props.tenant?.id) {
        await updateTenant(props.tenant.id, {
          name: form.value.name,
          domain: form.value.domain,
          planCode: form.value.planCode,
          expireTime: form.value.expireTime ? `${form.value.expireTime} 23:59:59` : undefined,
          adminName: form.value.adminName,
          adminPhone: form.value.adminPhone,
          status: form.value.status
        });
        ElMessage.success('学校租户信息已成功保存');
      } else {
        await createTenant({
          code: form.value.tenantCode,
          tenantCode: form.value.tenantCode,
          name: form.value.name,
          domain: form.value.domain,
          planCode: form.value.planCode,
          expireTime: form.value.expireTime ? `${form.value.expireTime} 23:59:59` : '2028-12-31 23:59:59',
          adminName: form.value.adminName,
          adminPhone: form.value.adminPhone,
          adminPassword: form.value.adminPassword || '123456'
        });
        ElMessage.success('新学校租户成功入驻，已初始化本部校区与资源配额！');
      }
      visible.value = false;
      emit('saved');
    } catch (e: any) {
      ElMessage.error(e.message || '操作失败');
    } finally {
      submitting.value = false;
    }
  });
};

const handleClose = () => {
  // reset form
};

defineExpose({
  initForm
});
</script>

<style scoped lang="scss">
.tenant-edit-dialog {
  :deep(.el-dialog__header) {
    font-weight: 700;
    font-size: 16px;
    color: #0F172A;
    border-bottom: 1px solid rgba(226, 232, 240, 0.8);
    padding-bottom: 14px;
    margin-right: 0;
  }
}

.tenant-form {
  padding: 6px 4px 0;

  .plan-selector-block {
    margin-bottom: 20px;

    .form-label-title {
      display: block;
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      margin-bottom: 10px;
    }

    .plan-cards-row {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;

      .plan-card-pill {
        position: relative;
        background: #F8FAFC;
        border: 1.5px solid #E2E8F0;
        border-radius: 16px;
        padding: 12px 14px;
        cursor: pointer;
        transition: all 0.22s ease;

        &:hover {
          border-color: #93C5FD;
          transform: translateY(-2px);
        }

        &.is-selected {
          border-color: #1677FF;
          background: #EFF6FF;
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.14);
        }

        &.flagship.is-selected {
          border-color: #F59E0B;
          background: #FFFBEB;
          box-shadow: 0 4px 14px rgba(245, 158, 11, 0.15);
        }

        &.standard.is-selected {
          border-color: #10B981;
          background: #ECFDF5;
          box-shadow: 0 4px 14px rgba(16, 185, 129, 0.15);
        }

        .plan-top {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-bottom: 8px;

          .plan-icon {
            font-size: 16px;
          }

          .plan-name {
            font-size: 13px;
            font-weight: 700;
            color: #0F172A;
          }
        }

        .plan-specs {
          display: flex;
          flex-direction: column;
          gap: 2px;
          font-size: 11px;
          color: #64748B;
        }

        .plan-select-indicator {
          position: absolute;
          top: -6px;
          right: -6px;
          width: 20px;
          height: 20px;
          border-radius: 9999px;
          background: #1677FF;
          color: #FFF;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          box-shadow: 0 2px 6px rgba(22, 119, 255, 0.3);
        }
      }
    }
  }

  .divider-text {
    font-size: 12px;
    font-weight: 600;
    color: #64748B;
  }
}

.pill-input {
  :deep(.el-input__wrapper) {
    border-radius: 9999px;
    padding-left: 14px;
    padding-right: 14px;
  }

  :deep(.el-input-group__prepend) {
    border-top-left-radius: 9999px;
    border-bottom-left-radius: 9999px;
    background: #F1F5F9;
    color: #64748B;
    font-size: 12px;
  }
}

.pill-date-picker {
  :deep(.el-input__wrapper) {
    border-radius: 9999px;
  }
}

.status-radio-group {
  display: flex;
  align-items: center;
  height: 32px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;

  .pill-btn {
    border-radius: 9999px;
    padding: 8px 22px;
    font-weight: 600;

    &.confirm-btn {
      background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);
    }
  }
}
</style>
