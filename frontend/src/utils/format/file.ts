export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export function normalizeAvatarUrl(url?: string | null): string | undefined {
  if (!url) return undefined;
  if (url.startsWith('file://') || url.includes('edumind-storage') || url.includes('backend/data') || url.includes('backend\\data')) {
    const dataMatch = url.match(/(?:edumind-storage|backend[/\\]data)[/\\](.+)$/i);
    if (dataMatch) {
      return `/api/storage/files/${dataMatch[1].replace(/\\/g, '/')}`;
    }
    const avatarMatch = url.match(/edumind[/\\]avatars[/\\]([^/\\]+)/i);
    if (avatarMatch) {
      return `/api/storage/files/edumind/avatars/${avatarMatch[1]}`;
    }
    return undefined;
  }
  return url;
}
