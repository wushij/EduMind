export function isAllowedFileType(filename: string, types: string[]): boolean {
  const ext = filename.split('.').pop()?.toLowerCase();
  return ext ? types.includes(ext) : false;
}
