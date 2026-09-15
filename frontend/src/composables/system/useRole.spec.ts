import { describe, it, expect } from 'vitest';
import { countPermissionNodes } from './useRole';
import type { SysMenuNode } from '@/constants/permission';

describe('countPermissionNodes', () => {
  const tree: SysMenuNode[] = [
    {
      id: 1,
      rowKey: '1',
      name: 'Root',
      icon: '',
      sort: 0,
      type: 1,
      children: [
        {
          id: 2,
          rowKey: '2',
          name: 'Child A',
          icon: '',
          sort: 0,
          type: 2,
          children: [{ id: 3, rowKey: '3', name: 'Btn', icon: '', sort: 0, type: 3 }]
        },
        { id: 4, rowKey: '4', name: 'Child B', icon: '', sort: 0, type: 2 }
      ]
    }
  ];

  it('counts nested nodes recursively', () => {
    expect(countPermissionNodes(tree)).toBe(4);
  });

  it('returns zero for empty tree', () => {
    expect(countPermissionNodes([])).toBe(0);
  });

  it('counts single node tree as one', () => {
    expect(
      countPermissionNodes([{ id: 9, rowKey: '9', name: 'Only', icon: '', sort: 0, type: 2 }])
    ).toBe(1);
  });
});
