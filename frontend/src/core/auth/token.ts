import { storage } from '../storage/local';
import { TOKEN_KEY } from '@/constants/auth';

export const tokenUtil = {
  get: () => storage.get(TOKEN_KEY),
  set: (token: string) => storage.set(TOKEN_KEY, token),
  remove: () => storage.remove(TOKEN_KEY)
};
