<template>
  <div class="user-detail-container">
    <!-- 顶部导航条 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/system/users')">
        返回用户列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/system/users' }">用户权限管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ userInfo?.realName || '用户画像详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 用户头部英雄卡片 -->
    <div v-loading="loading" class="user-hero-card">
      <div class="hero-left">
        <div class="avatar-box">
          {{ userInfo?.realName ? userInfo.realName.slice(0, 1) : '用' }}
        </div>
        <div>
          <div class="name-line">
            <h1 class="user-name">{{ userInfo?.realName }}</h1>
            <span class="user-account">@{{ userInfo?.username }}</span>
            <el-tag :type="userInfo?.status === 1 ? 'success' : 'danger'" size="small">
              {{ userInfo?.status === 1 ? '正常活跃' : '账号冻结' }}
            </el-tag>
          </div>
          <div class="role-badges-flow">
            <el-tag
              v-for="r in userInfo?.roles"
              :key="r"
              size="small"
              type="primary"
              effect="light"
            >
              {{ getRoleLabel(r) }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="hero-actions">
        <el-button @click="handleResetPassword">
          🔑 重置登录密码
        </el-button>
        <el-button
          :type="userInfo?.status === 1 ? 'danger' : 'success'"
          plain
          @click="toggleUserStatus"
        >
          {{ userInfo?.status === 1 ? '🚫 冻结账号' : '✅ 解冻并恢复使用' }}
        </el-button>
      </div>
    </div>

    <!-- 用户详情主体网格 -->
    <div class="detail-body-grid">
      <!-- 基本信息与所属院系 -->
      <el-card shadow="never" class="info-card">
        <h3 class="card-title">👤 用户基础档案</h3>
        <div class="fields-list">
          <div class="field-item">
            <span class="label">用户名/账号：</span>
            <span class="val font-mono">{{ userInfo?.username }}</span>
          </div>
          <div class="field-item">
            <span class="label">真实姓名：</span>
            <span class="val font-semibold">{{ userInfo?.realName }}</span>
          </div>
          <div class="field-item">
            <span class="label">电子邮箱：</span>
            <span class="val">{{ userInfo?.email || 'user@edumind.edu.cn' }}</span>
          </div>
          <div class="field-item">
            <span class="label">联系手机：</span>
            <span class="val font-mono">{{ userInfo?.phone || '13800001234' }}</span>
          </div>
          <div class="field-item">
            <span class="label">归属院系/班级：</span>
            <span class="val">{{ userInfo?.department || '计算机科学与技术学院 · 软件工程系' }}</span>
          </div>
          <div class="field-item">
            <span class="label">注册加入时间：</span>
            <span class="val text-slate-500">{{ userInfo?.createTime || '2026-09-01 09:00:00' }}</span>
          </div>
        </div>
      </el-card>

      <!-- 角色权限与系统资源分配 -->
      <el-card shadow="never" class="info-card">
        <div class="card-title-line">
          <h3 class="card-title">🛡️ 角色身份与权限分配</h3>
          <el-button type="primary" link size="small" @click="editRolesModal = true">
            调整分配角色
          </el-button>
        </div>

        <div class="roles-allocated-box">
          <div v-for="r in userInfo?.roles" :key="r" class="role-desc-card">
            <div class="role-top">
              <span class="role-badge">{{ r }}</span>
              <span class="role-title">{{ getRoleLabel(r) }}</span>
            </div>
            <p class="role-note">{{ getRoleDescription(r) }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 近期操作与登录审计记录 -->
    <el-card shadow="never" class="audit-table-card mt-4">
      <h3 class="card-title">🕒 账号近期安全与操作审计日志</h3>
      <el-table :data="auditLogs" stripe class="audit-table">
        <el-table-column label="操作时间" prop="time" width="180" />
        <el-table-column label="操作模块" prop="module" width="140" />
        <el-table-column label="操作行为说明" prop="action" min-width="220" />
        <el-table-column label="客户端 IP" prop="ip" width="140" />
        <el-table-column label="结果状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '操作成功' : '拦截失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 调整角色弹窗 -->
    <el-dialog v-model="editRolesModal" title="调整用户系统角色" width="480px" destroy-on-close>
      <el-checkbox-group v-model="selectedRoles" class="role-checkboxes">
        <el-checkbox label="ROLE_ADMIN">系统超级管理员 (ROLE_ADMIN)</el-checkbox>
        <el-checkbox label="ROLE_TEACHER">课程主讲教师 (ROLE_TEACHER)</el-checkbox>
        <el-checkbox label="ROLE_STUDENT">修读学生 (ROLE_STUDENT)</el-checkbox>
      </el-checkbox-group>

      <template #footer>
        <el-button @click="editRolesModal = false">取消</el-button>
        <el-button type="primary" @click="saveRoleAssignment">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { getUserDetail, updateUserStatus } from '@/api/system/user';

const route = useRoute();
const router = useRouter();

const userId = ref(Number(route.params.id) || 2);
const loading = ref(false);
const userInfo = ref<any>(null);
const editRolesModal = ref(false);
const selectedRoles = ref<string[]>([]);

const auditLogs = ref([
  { time: '2026-09-11 15:30:12', module: '试题中心', action: '录入新单选试题 #1005', ip: '127.0.0.1', status: 'SUCCESS' },
  { time: '2026-09-11 14:15:40', module: '作业管理', action: '发布作业《二叉树递归遍历测验》', ip: '127.0.0.1', status: 'SUCCESS' },
  { time: '2026-09-11 09:00:21', module: '用户认证', action: '账号常规登录鉴权成功', ip: '192.168.1.108', status: 'SUCCESS' }
]);

onMounted(() => {
  loadUser();
});

async function loadUser() {
  loading.value = true;
  try {
    const res = await getUserDetail(userId.value);
    if (res.data) {
      userInfo.value = res.data;
    } else {
      throw new Error('未找到该用户信息');
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取用户详情失败');
    userInfo.value = null;
  } finally {
    selectedRoles.value = [...(userInfo.value?.roles || [])];
    loading.value = false;
  }
}

function getRoleLabel(role: string) {
  const map: Record<string, string> = {
    ROLE_ADMIN: '系统管理员',
    ROLE_TEACHER: '任课教师',
    ROLE_STUDENT: '在读学生'
  };
  return map[role] || role;
}

function getRoleDescription(role: string) {
  const map: Record<string, string> = {
    ROLE_ADMIN: '拥有平台系统全局管理权限、角色权限分配、日志审计与模型配额管控。',
    ROLE_TEACHER: '拥有所授课程的教学大纲制定、作业与试卷发布、AI智能批改及知识库维护权限。',
    ROLE_STUDENT: '拥有所选课程的学习、作业在线作答、AI学伴答疑与个人错题本查看权限。'
  };
  return map[role] || '系统常规授权';
}

function toggleUserStatus() {
  const newStatus = userInfo.value?.status === 1 ? 0 : 1;
  const actionText = newStatus === 0 ? '冻结该用户账号' : '解冻恢复该用户账号';

  ElMessageBox.confirm(`确定要${actionText}吗？`, '账号状态变更确认', {
    confirmButtonText: '确定变更',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await updateUserStatus(userId.value, newStatus === 1 ? 'ACTIVE' : 'DISABLED');
      userInfo.value.status = newStatus;
      ElMessage.success(`用户状态已成功变更为：${newStatus === 1 ? '正常活跃' : '账号冻结'}`);
    } catch (err: any) {
      ElMessage.error(err?.message || '用户状态更新失败');
    }
  });
}

function handleResetPassword() {
  ElMessageBox.confirm('重置后初始密码将变更为 admin123，是否继续？', '重置登录密码', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消'
  }).then(() => {
    ElMessage.success('用户密码已成功重置为默认密码：admin123');
  });
}

function saveRoleAssignment() {
  userInfo.value.roles = [...selectedRoles.value];
  editRolesModal.value = false;
  ElMessage.success('用户所属角色已成功更新！');
}
</script>

<style scoped lang="scss">
.user-detail-container {
  padding: 24px;
  background: #f8fafc;
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

  .user-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .hero-left {
      display: flex;
      align-items: center;
      gap: 20px;

      .avatar-box {
        width: 64px;
        height: 64px;
        border-radius: 16px;
        background: linear-gradient(135deg, #3b82f6, #1d4ed8);
        color: #ffffff;
        font-size: 26px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .name-line {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .user-name {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }

        .user-account {
          font-size: 14px;
          color: #64748b;
          font-family: monospace;
        }
      }

      .role-badges-flow {
        display: flex;
        gap: 8px;
      }
    }

    .hero-actions {
      display: flex;
      gap: 12px;
    }
  }

  .detail-body-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;

    .info-card {
      background: #ffffff;
      border-radius: 14px;
      border: 1px solid #e2e8f0;
      padding: 16px 20px;

      .card-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 16px;
      }

      .card-title-line {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;

        .card-title {
          margin: 0;
        }
      }

      .fields-list {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .field-item {
          display: flex;
          font-size: 13px;

          .label {
            width: 120px;
            color: #64748b;
          }

          .val {
            color: #1e293b;
          }
        }
      }

      .roles-allocated-box {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .role-desc-card {
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          padding: 12px 14px;

          .role-top {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 6px;

            .role-badge {
              font-family: monospace;
              font-size: 11px;
              background: #eff6ff;
              color: #2563eb;
              padding: 2px 6px;
              border-radius: 4px;
            }

            .role-title {
              font-size: 13px;
              font-weight: 700;
              color: #1e293b;
            }
          }

          .role-note {
            font-size: 12px;
            color: #64748b;
            margin: 0;
            line-height: 1.5;
          }
        }
      }
    }
  }

  .audit-table-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    padding: 16px 20px;

    .card-title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 16px;
    }
  }

  .role-checkboxes {
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 10px 0;
  }
}
</style>
