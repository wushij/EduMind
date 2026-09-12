import { App } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import { router } from '@/router';
import { setupDirectives } from '@/directives';
import { setupAppErrorHandlers, logAppError } from '@/core/http/error-handler';
import { useAuthStore } from '@/stores/auth/auth';
import { tokenUtil } from '@/core/auth/token';
import '@/styles/index.scss';
import 'katex/dist/katex.min.css';

export async function bootstrap(app: App) {
  const pinia = createPinia();
  app.use(pinia);
  app.use(router);
  app.use(ElementPlus);
  setupDirectives(app);
  setupAppErrorHandlers(app);

  const authStore = useAuthStore();
  if (tokenUtil.get()) {
    try {
      await authStore.fetchUserInfo();
    } catch (err) {
      logAppError('AuthBootstrap', err);
      authStore.logout();
    }
  }
}
