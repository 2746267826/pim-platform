import { useMemo } from 'react';
import { Tree, type NodeApi } from 'react-arborist';
import { FileText, Folder, FolderOpen } from 'lucide-react';
import type { FileItem } from '../../types';

export interface OneDriveFileTreeProps {
  /** 已加载的子节点缓存：路径 → 直接子项（含文件与文件夹）。 */
  childrenByPath: Record<string, FileItem[]>;
  selectedItemId: string | null;
  onSelect: (item: FileItem) => void;
  /** 展开文件夹时按需加载子项（用于懒加载尚未拉取的目录）。 */
  onExpandFolder: (path: string) => void;
}

interface TreeDataNode {
  id: string;
  name: string;
  path: string;
  itemType: string;
  mimeType: string | null;
  children?: TreeDataNode[];
}

const PLACEHOLDER_ITEM: FileItem = {
  id: '',
  providerId: '',
  externalFileId: '',
  parentExternalFileId: null,
  path: '',
  name: '',
  itemType: 'folder',
  mimeType: null,
  size: null,
  etag: null,
  contentHash: null,
  currentVersionId: null,
  permissions: null,
  isDeleted: false,
  deletedAt: null,
  lastSeenAt: null,
  createdAt: '',
  modifiedAt: '',
  syncedAt: '',
  indexStatus: '',
  ai: null,
};

/**
 * 方案 A 左栏：懒加载文件树（react-arborist 虚拟化）。
 * 未加载的目录 children 为空数组仍显示展开箭头，展开时回调 onExpandFolder 拉取。
 */
export default function OneDriveFileTree({
  childrenByPath,
  selectedItemId,
  onSelect,
  onExpandFolder,
}: OneDriveFileTreeProps) {
  const data = useMemo<TreeDataNode[]>(() => {
    const build = (path: string, depth: number): TreeDataNode[] => {
      const children = childrenByPath[path] ?? [];
      return children.map(child => {
        if (child.itemType !== 'folder') {
          return { id: child.id, name: child.name, path: child.path, itemType: child.itemType, mimeType: child.mimeType };
        }
        const loaded = Object.prototype.hasOwnProperty.call(childrenByPath, child.path);
        return {
          id: child.id,
          name: child.name,
          path: child.path,
          itemType: child.itemType,
          mimeType: child.mimeType,
          children: loaded && depth < 24 ? build(child.path, depth + 1) : [],
        };
      });
    };
    return build('/', 0);
  }, [childrenByPath]);

  const nodesById = useMemo<Map<string, TreeDataNode>>(() => {
    const map = new Map<string, TreeDataNode>();
    const walk = (nodes: TreeDataNode[]) => {
      for (const node of nodes) {
        map.set(node.id, node);
        if (node.children) walk(node.children);
      }
    };
    walk(data);
    return map;
  }, [data]);

  const handleSelect = useCallbackSelect(onSelect);

  return (
    <div className="h-full" data-testid="onedrive-tree">
      <Tree
        data={data}
        width={248}
        height={960}
        rowHeight={32}
        indent={14}
        openByDefault={false}
        disableDrag
        disableDrop
        disableEdit
        selection={selectedItemId ?? undefined}
        onSelect={nodes => {
          const node = nodes[0] as NodeApi<TreeDataNode> | undefined;
          if (!node) return;
          const row = node.data;
          if (row.itemType === 'folder') {
            onExpandFolder(row.path);
          }
          handleSelect(row);
        }}
        onToggle={id => {
          const nodeData = nodesById.get(id);
          if (nodeData?.itemType === 'folder') {
            onExpandFolder(nodeData.path);
          }
        }}
      >
        {NodeRenderer}
      </Tree>
    </div>
  );
}

function useCallbackSelect(onSelect: (item: FileItem) => void) {
  return (row: TreeDataNode) => {
    onSelect({
      ...PLACEHOLDER_ITEM,
      id: row.id,
      path: row.path,
      name: row.name,
      itemType: row.itemType,
      mimeType: row.mimeType,
    });
  };
}

function NodeRenderer({ node, style }: { node: NodeApi<TreeDataNode>; style: React.CSSProperties }) {
  const row = node.data;
  const isFolder = row.itemType === 'folder';
  const Icon = isFolder ? (node.isOpen ? FolderOpen : Folder) : FileText;

  return (
    <div
      style={style}
      className={`flex h-full items-center gap-1.5 pr-2 text-[13px] ${
        node.isSelected ? 'bg-[var(--pim-primary-soft)]' : 'hover:bg-[var(--pim-surface-muted)]'
      }`}
    >
      <span className="flex h-4 w-4 shrink-0 items-center justify-center text-[var(--pim-text-muted)]">
        {isFolder && node.isOpen ? <FolderOpen size={14} /> : <Icon size={14} />}
      </span>
      <span className="truncate" title={row.name}>{row.name}</span>
    </div>
  );
}
