import type { SysMenu } from '@/types/system/menu';

/** 递归过滤菜单树 */
export function filterMenuTree(nodes: SysMenu[], kw?: string): SysMenu[] {
  if (!kw) return nodes;
  const keyword = kw.trim().toLowerCase();

  const filterNode = (item: SysMenu): SysMenu | null => {
    const matchSelf =
      item.name.toLowerCase().includes(keyword) ||
      (item.path && item.path.toLowerCase().includes(keyword)) ||
      (item.permission && item.permission.toLowerCase().includes(keyword)) ||
      (item.component && item.component.toLowerCase().includes(keyword));

    let matchingChildren: SysMenu[] = [];
    if (item.children && item.children.length > 0) {
      matchingChildren = item.children
        .map(filterNode)
        .filter((child): child is SysMenu => child !== null);
    }

    if (matchSelf || matchingChildren.length > 0) {
      return {
        ...item,
        children: matchingChildren
      };
    }
    return null;
  };

  return nodes
    .map(filterNode)
    .filter((node): node is SysMenu => node !== null);
}
