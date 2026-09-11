export const storage = {
  get: (key: string) => {
    try {
      const val = localStorage.getItem(key);
      return val ? JSON.parse(val) : null;
    } catch {
      return localStorage.getItem(key);
    }
  },
  set: (key: string, value: any) => {
    localStorage.setItem(key, typeof value === 'string' ? value : JSON.stringify(value));
  },
  remove: (key: string) => localStorage.removeItem(key)
};
