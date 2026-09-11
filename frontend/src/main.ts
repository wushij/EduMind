import { createApp } from 'vue';
import App from '@/app/App.vue';
import { bootstrap } from '@/app/bootstrap';
import { registerProviders } from '@/app/providers';

const app = createApp(App);
bootstrap(app).then(() => {
  registerProviders(app);
  app.mount('#app');
});
