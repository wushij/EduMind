<template>
  <el-drawer
    v-model="visible"
    :title="`校区治理中心 · ${tenant?.name || ''}`"
    size="580px"
    class="campus-management-drawer"
    destroy-on-close
    @closed="handleClose"
  >
    <div class="campus-drawer-container" v-loading="loading">
      <!-- 顶部校区概况长圆卡 -->
      <div class="drawer-header-card">
        <div class="header-left">
          <div class="campus-stat-badge">
            <el-icon class="stat-icon"><OfficeBuilding /></el-icon>
            <div class="stat-meta">
              <span class="stat-title">已设立校区</span>
              <span class="stat-val">{{ campuses.length }} <small>个独立物理空间</small></span>
            </div>
          </div>
        </div>
        <el-button type="primary" class="pill-btn add-btn" @click="openAddModal">
          <el-icon><Plus /></el-icon>
          <span>增设新校区</span>
        </el-button>
      </div>

      <div class="drawer-tip-pill">
        <el-icon><InfoFilled /></el-icon>
        <span>每个校区拥有独立的教学楼宇、班级组织和物理架构，主校区承载校级核心管理中枢</span>
      </div>

      <!-- 校区列表 -->
      <div class="campus-list" v-if="campuses.length > 0">
        <div
          v-for="campus in campuses"
          :key="campus.id"
          class="campus-item-card"
          :class="{ 'is-main-campus': campus.isMain }"
        >
          <div class="card-main-header">
            <div class="campus-title-wrap">
              <div class="campus-icon-circle" :class="{ 'is-main': campus.isMain }">
                <el-icon><School /></el-icon>
              </div>
              <div class="campus-name-box">
                <div class="name-line">
                  <h4 class="campus-name">{{ campus.name }}</h4>
                  <span v-if="campus.isMain" class="pill-tag main-tag">主校区 / 本部</span>
                  <span class="pill-tag code-tag">{{ campus.campusCode || campus.code }}</span>
                </div>
                <p class="campus-address">
                  <el-icon><Location /></el-icon>
                  <span>{{ campus.address || '尚未配置具体地理位置' }}</span>
                </p>
              </div>
            </div>
            <div class="status-switch-box">
              <el-switch
                :model-value="campus.status === 1"
                inline-prompt
                active-text="启用"
                inactive-text="停用"
                active-color="#10B981"
                inactive-color="#94A3B8"
                @change="(val: any) => handleToggleStatus(campus, val)"
              />
            </div>
          </div>

          <div class="card-footer-actions">
            <div class="footer-left">
              <span class="meta-time">创建于 {{ campus.createTime ? campus.createTime.substring(0, 10) : '近期设立' }}</span>
            </div>
            <div class="footer-btns">
              <el-button size="small" class="pill-btn-sm text-btn" @click="openEditModal(campus)">
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button
                size="small"
                class="pill-btn-sm danger-btn"
                :disabled="campus.isMain"
                @click="handleDelete(campus)"
              >
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无校区配置，请点击上方增设校区" />
    </div>

    <!-- 增设/编辑校区对话框 -->
    <el-dialog
      v-model="modalVisible"
      :title="isEditing ? '编辑校区信息' : '增设新校区'"
      width="460px"
      append-to-body
      destroy-on-close
      class="campus-edit-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="校区编码 (英文/大写)" prop="code">
          <el-input
            v-model="form.code"
            placeholder="如：EAST_CAMPUS / SCIENCE_PARK"
            :disabled="isEditing"
            class="pill-input"
          />
        </el-form-item>
        <el-form-item label="校区全称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="如：东校区 (高新科学城)"
            class="pill-input"
          />
        </el-form-item>
        <el-form-item label="详细地理地址" prop="address">
          <el-input
            v-model="form.address"
            placeholder="如：上海市浦东新区张江高科园区科学大道88号"
            class="pill-input"
          />
        </el-form-item>
        <el-form-item label="设为主校区 / 行政本部">
          <div class="main-toggle-row">
            <el-switch v-model="form.isMain" active-color="#1677FF" />
            <span class="toggle-hint">设为主校区后，全校统一管理报表将默认以该校区为基准</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="modal-footer">
          <el-button class="pill-btn" @click="modalVisible = false">取消</el-button>
          <el-button type="primary" class="pill-btn confirm-btn" :loading="saving" @click="submitSave">
            {{ isEditing ? '保存修改' : '确认设立' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  OfficeBuilding,
  Plus,
  InfoFilled,
  School,
  Location,
  Edit,
  Delete
} from '@element-plus/icons-vue';
import {
  listCampuses,
  createCampus,
  updateCampus,
  updateCampusStatus,
  deleteCampus
} from '@/api/system/tenant';
import type { TenantListVO, CampusVO } from '@/types/system/tenant';

const props = defineProps<{
  modelValue: boolean;
  tenant: TenantListVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'changed'): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const saving = ref(false);
const campuses = ref<CampusVO[]>([]);

const modalVisible = ref(false);
const isEditing = ref(false);
const editingCampusId = ref<number | null>(null);
const formRef = ref();
const form = ref({
  code: '',
  name: '',
  address: '',
  isMain: false,
  status: 1
});

const rules = {
  code: [{ required: true, message: '请输入校区编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入校区全称', trigger: 'blur' }]
};

const loadCampuses = async () => {
  if (!props.tenant?.id) return;
  try {
    loading.value = true;
    const res = await listCampuses(props.tenant.id);
    if (res?.data) {
      campuses.value = res.data;
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载校区列表失败');
  } finally {
    loading.value = false;
  }
};

const openAddModal = () => {
  isEditing.value = false;
  editingCampusId.value = null;
  form.value = {
    code: '',
    name: '',
    address: '',
    isMain: campuses.value.length === 0,
    status: 1
  };
  modalVisible.value = true;
};

const openEditModal = (campus: CampusVO) => {
  isEditing.value = true;
  editingCampusId.value = campus.id;
  form.value = {
    code: campus.campusCode || campus.code || '',
    name: campus.name,
    address: campus.address || '',
    isMain: campus.isMain || false,
    status: campus.status
  };
  modalVisible.value = true;
};

const submitSave = async () => {
  if (!formRef.value || !props.tenant?.id) return;
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    try {
      saving.value = true;
      if (isEditing.value && editingCampusId.value) {
        await updateCampus(props.tenant!.id, editingCampusId.value, {
          name: form.value.name,
          address: form.value.address,
          isMain: form.value.isMain,
          status: form.value.status
        });
        ElMessage.success('校区信息已更新');
      } else {
        await createCampus(props.tenant!.id, {
          code: form.value.code,
          name: form.value.name,
          address: form.value.address,
          isMain: form.value.isMain,
          status: form.value.status
        });
        ElMessage.success('新校区设立成功，已联动更新组织架构树');
      }
      modalVisible.value = false;
      await loadCampuses();
      emit('changed');
    } catch (e: any) {
      ElMessage.error(e.message || '操作失败');
    } finally {
      saving.value = false;
    }
  });
};

const handleToggleStatus = async (campus: CampusVO, val: boolean) => {
  if (!props.tenant?.id) return;
  const targetStatus = val ? 1 : 0;
  try {
    await updateCampusStatus(props.tenant.id, campus.id, targetStatus);
    campus.status = targetStatus;
    ElMessage.success(val ? '校区已恢复启用' : '校区已停用');
    emit('changed');
  } catch (e: any) {
    ElMessage.error(e.message || '更新状态失败');
  }
};

const handleDelete = async (campus: CampusVO) => {
  if (!props.tenant?.id) return;
  try {
    await ElMessageBox.confirm(
      `确定注销并删除校区【${campus.name}】吗？删除后该校区将脱离学校多租户空间。`,
      '注销校区确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    await deleteCampus(props.tenant.id, campus.id);
    ElMessage.success('校区注销成功');
    await loadCampuses();
    emit('changed');
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败');
    }
  }
};

const handleClose = () => {
  campuses.value = [];
};

defineExpose({
  loadCampuses
});
</script>

<style scoped lang="scss">
.campus-management-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 16px;
    font-weight: 700;
    font-size: 16px;
    color: #0F172A;
    border-bottom: 1px solid rgba(226, 232, 240, 0.8);
    padding-bottom: 14px;
  }
}

.campus-drawer-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 4px;

  .drawer-header-card {
    background: linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%);
    border: 1.5px solid rgba(22, 119, 255, 0.12);
    border-radius: 20px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .campus-stat-badge {
      display: flex;
      align-items: center;
      gap: 12px;

      .stat-icon {
        font-size: 26px;
        color: #1677FF;
        background: #FFFFFF;
        width: 46px;
        height: 46px;
        border-radius: 9999px;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.12);
      }

      .stat-meta {
        display: flex;
        flex-direction: column;

        .stat-title {
          font-size: 12px;
          color: #64748B;
        }

        .stat-val {
          font-size: 20px;
          font-weight: 800;
          color: #0F172A;

          small {
            font-size: 12px;
            font-weight: 400;
            color: #94A3B8;
            margin-left: 4px;
          }
        }
      }
    }
  }

  .drawer-tip-pill {
    background: #F1F5F9;
    border-radius: 9999px;
    padding: 8px 16px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #475569;

    .el-icon {
      color: #1677FF;
      font-size: 14px;
      flex-shrink: 0;
    }
  }

  .campus-list {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .campus-item-card {
      background: #FFFFFF;
      border: 1.5px solid #E2E8F0;
      border-radius: 18px;
      padding: 16px;
      transition: all 0.25s ease;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);

      &:hover {
        border-color: #93C5FD;
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(22, 119, 255, 0.08);
      }

      &.is-main-campus {
        border-color: rgba(16, 185, 129, 0.35);
        background: linear-gradient(180deg, #FAFCFA 0%, #FFFFFF 100%);
      }

      .card-main-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;

        .campus-title-wrap {
          display: flex;
          align-items: flex-start;
          gap: 12px;

          .campus-icon-circle {
            width: 40px;
            height: 40px;
            border-radius: 9999px;
            background: #F1F5F9;
            color: #475569;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            flex-shrink: 0;

            &.is-main {
              background: #DCFCE7;
              color: #10B981;
            }
          }

          .campus-name-box {
            .name-line {
              display: flex;
              align-items: center;
              gap: 8px;
              flex-wrap: wrap;

              .campus-name {
                margin: 0;
                font-size: 15px;
                font-weight: 700;
                color: #0F172A;
              }

              .pill-tag {
                padding: 2px 10px;
                border-radius: 9999px;
                font-size: 11px;
                font-weight: 600;

                &.main-tag {
                  background: #ECFDF5;
                  color: #059669;
                  border: 1px solid rgba(16, 185, 129, 0.3);
                }

                &.code-tag {
                  background: #EFF6FF;
                  color: #2563EB;
                  border: 1px solid rgba(37, 99, 235, 0.2);
                }
              }
            }

            .campus-address {
              margin: 6px 0 0;
              font-size: 12px;
              color: #64748B;
              display: flex;
              align-items: center;
              gap: 4px;
            }
          }
        }
      }

      .card-footer-actions {
        border-top: 1px dashed #E2E8F0;
        padding-top: 10px;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .meta-time {
          font-size: 11px;
          color: #94A3B8;
        }

        .footer-btns {
          display: flex;
          gap: 6px;
        }
      }
    }
  }
}

.pill-btn {
  border-radius: 9999px;
  font-weight: 600;
  padding: 8px 18px;

  &.add-btn {
    background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);
  }
}

.pill-btn-sm {
  border-radius: 9999px;
  font-size: 12px;
  padding: 4px 12px;

  &.text-btn {
    background: #F1F5F9;
    color: #334155;
    border: none;

    &:hover {
      background: #E2E8F0;
      color: #1E293B;
    }
  }

  &.danger-btn {
    background: #FEF2F2;
    color: #EF4444;
    border: none;

    &:hover:not(:disabled) {
      background: #FEE2E2;
      color: #DC2626;
    }
  }
}

.pill-input {
  :deep(.el-input__wrapper) {
    border-radius: 9999px;
    padding-left: 14px;
    padding-right: 14px;
  }
}

.main-toggle-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .toggle-hint {
    font-size: 12px;
    color: #64748B;
  }
}
</style>
