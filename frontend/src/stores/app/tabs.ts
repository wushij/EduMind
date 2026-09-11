import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useTabsStore = defineStore('tabs', () => {
  const visitedTabs = ref<Array<{ title: string; path: string }>>([
    { title: '工作台', path: '/dashboard' }
  ]);
  return { visitedTabs };
});
