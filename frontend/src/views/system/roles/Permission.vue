<template>
  <div class="permission-page-container">
    <!-- 顶部操作头区 -->
    <div class="perm-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <el-icon class="header-icon"><Key /></el-icon>
          <h1 class="main-title">系统权限与功能资源树</h1>
          <span class="capsule-count-tag">细粒度 RBAC 授权体系统</span>
        </div>
        <p class="sub-desc">
          涵盖前端路由菜单权限、页面按钮操作权限与后端 API 资源防护端点，支持按模块层级统一树状展现与检索。
        </p>
      </div>

      <div class="header-right-actions">
        <el-button @click="toggleExpandAll">
          {{ isAllExpanded ? '折叠全部' : '展开全部' }}
        </el-button>
        <el-button type="primary" plain @click="loadPermissions">
          刷新权限树
        </el-button>
      </div>
    </div>

    <!-- 树状卡片与搜索过滤 -->
    <el-card shadow="never" class="perm-tree-card">
      <div class="tree-search-bar">
        <el-input
          v-model="filterText"
          placeholder="输入权限名称、编码或路径进行实时过滤..."
          clearable
          :prefix-icon="Search"
          class="tree-search-input"
        />
        <div class="tree-legend">
          <span class="legend-item"><span class="dot bg-blue-500"></span> 菜单路由</span>
          <span class="legend-item"><span class="dot bg-emerald-500"></span> 页面按钮</span>
          <span class="legend-item"><span class="dot bg-amber-500"></span> API 接口</span>
        </div>
      </div>

      <div v-loading="loading" class="tree-content-box">
        <el-empty
          v-if="!loading && permissions.length === 0"
          description="暂无权限数据，请确认后端已初始化权限种子或检查登录权限"
        />
        <el-tree
          v-else
          ref="treeRef"
          :data="permissionTree"
          node-key="id"
          :default-expand-all="isAllExpanded"
          :filter-node-method="filterNode"
          :props="{ label: 'label', children: 'children' }"
        >
          <template #default="{ data }">
            <div class="custom-tree-node">
              <div class="node-left">
                <el-icon class="node-icon"><component :is="getNodeIcon(data.permissionType)" /></el-icon>
                <span class="node-name">{{ data.permissionName }}</span>
                <span class="node-code font-mono">{{ data.permissionCode }}</span>
              </div>
              <div class="node-right">
                <el-tag size="small" :type="getTypeTagType(data.permissionType)" effect="light">
                  {{ getTypeLabel(data.permissionType) }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-tree>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, Key, FolderOpened, Pointer, Connection } from '@element-plus/icons-vue';
import { getPermissions } from '@/api/system/permission';
import { USE_MOCK } from '@/config/mock';
import type { PermissionVO } from '@/types/system/rbac';

const loading = ref(false);
const filterText = ref('');
const treeRef = ref<any>();
const isAllExpanded = ref(true);

const permissions = ref<PermissionVO[]>([]);

watch(filterText, (val) => {
  treeRef.value?.filter(val);
});

function filterNode(value: string, data: any) {
  if (!value) return true;
  const kw = value.toLowerCase();
  const matchName = data.permissionName?.toLowerCase().includes(kw);
  const matchCode = data.permissionCode?.toLowerCase().includes(kw);
  return matchName || matchCode;
}

const permissionTree = computed(() => mapPermissionLabels(permissions.value));

function mapPermissionLabels(nodes: PermissionVO[]): Array<PermissionVO & { label: string }> {
  return nodes.map((item) => ({
    ...item,
    label: `${item.permissionName} (${item.permissionCode})`,
    children: item.children?.length ? mapPermissionLabels(item.children) : []
  }));
}

async function loadPermissions() {
  loading.value = true;
  try {
    const res = await getPermissions();
    permissions.value = res.data || [];
    if (permissions.value.length === 0 && USE_MOCK) {
      permissions.value = getDefaultPermissionMock();
    }
  } catch (err: any) {
    if (USE_MOCK) {
      permissions.value = getDefaultPermissionMock();
    } else {
      permissions.value = [];
      ElMessage.error(err?.message || '加载权限树失败，请检查网络或登录权限');
    }
  } finally {
    loading.value = false;
  }
}

function getDefaultPermissionMock(): PermissionVO[] {
  return [
    {
      id: 1,
      permissionCode: 'course:manage',
      permissionName: '课程体系与教学管理',
      permissionType: 'MENU',
      children: [
        { id: 11, permissionCode: 'course:list', permissionName: '查看课程列表', permissionType: 'MENU' },
        { id: 12, permissionCode: 'course:create', permissionName: '新增创建课程', permissionType: 'BUTTON' },
        { id: 13, permissionCode: 'course:update', permissionName: '修改课程信息', permissionType: 'BUTTON' },
        { id: 14, permissionCode: 'course:delete', permissionName: '下架归档课程', permissionType: 'BUTTON' }
      ]
    },
    {
      id: 2,
      permissionCode: 'question:manage',
      permissionName: '试题中心与在线考核',
      permissionType: 'MENU',
      children: [
        { id: 21, permissionCode: 'question:bank:view', permissionName: '题库查询浏览', permissionType: 'MENU' },
        { id: 22, permissionCode: 'question:bank:manage', permissionName: '题库创建与题目调度', permissionType: 'BUTTON' },
        { id: 23, permissionCode: 'exam:compose', permissionName: '试卷编排与发布', permissionType: 'BUTTON' },
        { id: 24, permissionCode: 'assignment:grading', permissionName: '作业批改与成绩终审', permissionType: 'BUTTON' }
      ]
    },
    {
      id: 3,
      permissionCode: 'knowledge:manage',
      permissionName: '知识库与文档切片中心',
      permissionType: 'MENU',
      children: [
        { id: 31, permissionCode: 'knowledge:base:view', permissionName: '知识库列表浏览', permissionType: 'MENU' },
        { id: 32, permissionCode: 'knowledge:doc:upload', permissionName: '上传教材课件文档', permissionType: 'BUTTON' },
        { id: 33, permissionCode: 'knowledge:doc:parse', permissionName: '触发语义向量化解析', permissionType: 'API' }
      ]
    },
    {
      id: 4,
      permissionCode: 'ai:service',
      permissionName: 'AI 教学赋能与大模型调用',
      permissionType: 'MENU',
      children: [
        { id: 41, permissionCode: 'ai:chat:use', permissionName: '智能学伴交互对话', permissionType: 'MENU' },
        { id: 42, permissionCode: 'ai:grading:trigger', permissionName: 'AI 辅助预批改服务', permissionType: 'API' },
        { id: 43, permissionCode: 'ai:model:config', permissionName: '模型参数与配额调优', permissionType: 'BUTTON' }
      ]
    },
    {
      id: 5,
      permissionCode: 'system:manage',
      permissionName: '系统安全与全局管理',
      permissionType: 'MENU',
      children: [
        { id: 51, permissionCode: 'system:user:manage', permissionName: '系统用户档案管理', permissionType: 'MENU' },
        { id: 52, permissionCode: 'system:role:assign', permissionName: '角色分配与权限授权', permissionType: 'BUTTON' },
        { id: 53, permissionCode: 'system:audit:view', permissionName: '安全审计日志查看', permissionType: 'MENU' }
      ]
    }
  ];
}

function getNodeIcon(type?: string) {
  if (type === 'MENU') return FolderOpened;
  if (type === 'BUTTON') return Pointer;
  return Connection;
}

function getTypeLabel(type?: string) {
  const map: Record<string, string> = {
    MENU: '菜单路由',
    BUTTON: '操作按钮',
    API: '后端API'
  };
  return map[type || 'MENU'] || '资源';
}

function getTypeTagType(type?: string) {
  const map: Record<string, string> = {
    MENU: 'primary',
    BUTTON: 'success',
    API: 'warning'
  };
  return (map[type || 'MENU'] as any) || 'info';
}

function toggleExpandAll() {
  isAllExpanded.value = !isAllExpanded.value;
}

onMounted(loadPermissions);
</script>

<style scoped lang="scss">
.permission-page-container {
  padding: 24px;
  background: #f8fafc;
  .perm-header-dock {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 24px;

    .header-left {
      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 12px;

        .header-icon {
          font-size: 28px;
        }

        .main-title {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }

        .capsule-count-tag {
          font-size: 12px;
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #bfdbfe;
          border-radius: 9999px;
          padding: 2px 10px;
          font-weight: 500;
        }
      }

      .sub-desc {
        margin: 6px 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }

    .header-right-actions {
      display: flex;
      gap: 12px;
    }
  }

  .perm-tree-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 20px 24px;

    .tree-search-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20px;
      padding-bottom: 16px;
      border-bottom: 1px solid #f1f5f9;

      .tree-search-input {
        width: 360px;
      }

      .tree-legend {
        display: flex;
        align-items: center;
        gap: 18px;

        .legend-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          color: #64748b;

          .dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
          }
        }
      }
    }

    .tree-content-box {
      :deep(.el-tree-node__content) {
        height: 44px;
        border-radius: 8px;
        margin-bottom: 4px;
        transition: background-color 0.2s;

        &:hover {
          background-color: #f1f5f9;
        }
      }

      .custom-tree-node {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        padding-right: 16px;

        .node-left {
          display: flex;
          align-items: center;
          gap: 10px;

          .node-icon {
            font-size: 16px;
          }

          .node-name {
            font-size: 14px;
            font-weight: 600;
            color: #1e293b;
          }

          .node-code {
            font-size: 12px;
            color: #64748b;
            background: #f8fafc;
            padding: 1px 6px;
            border-radius: 4px;
            border: 1px solid #e2e8f0;
          }
        }
      }
    }
  }
}
</style>
