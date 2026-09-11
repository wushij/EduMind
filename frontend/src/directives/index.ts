import { App } from 'vue';
import { vPermission } from './permission';
import { vRole } from './role';

export function setupDirectives(app: App) {
  app.directive('permission', vPermission);
  app.directive('role', vRole);
}
