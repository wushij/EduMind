import { App } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import { router } from '@/router';
import { setupDirectives } from '@/directives';
import { useAuthStore } from '@/stores/auth/auth';
import { tokenUtil } from '@/core/auth/token';
import '@/styles/index.scss';

export async function bootstrap(app: App) {
  const pinia = createPinia();
  app.use(pinia);
  app.use(router);
  app.use(ElementPlus);
  setupDirectives(app);

  const authStore = useAuthStore();
  if (tokenUtil.get()) {
    try {
      await authStore.fetchUserInfo();
    } catch {
      authStore.logout();
    }
  }
}
