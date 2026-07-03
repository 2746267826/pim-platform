import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getAppSignatures, createAppSignature, deleteAppSignature } from '../api/appSignatures';
import PageHeader from '../ui/PageHeader';

const productivities = [
  { value: 'productive', label: '✅ 高效率', color: 'text-green-600 bg-green-50' },
  { value: 'neutral', label: '➖ 中性', color: 'text-slate-600 bg-slate-50' },
  { value: 'distracting', label: '❌ 分散精力', color: 'text-red-600 bg-red-50' },
];

export default function AppKnowledgeBasePage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);
  const [form, setForm] = useState({
    processName: '',
    displayName: '',
    categoryPath: '',
    productivity: 'neutral',
    icon: '',
    description: '',
  });

  const { data: signatures = [], isLoading } = useQuery({
    queryKey: ['app-signatures', search],
    queryFn: () => getAppSignatures(search || undefined),
  });

  const createMut = useMutation({
    mutationFn: () => createAppSignature({
      processName: form.processName.trim(),
      displayName: form.displayName.trim(),
      categoryPath: form.categoryPath.trim() || undefined,
      productivity: form.productivity,
      icon: form.icon.trim() || undefined,
      description: form.description.trim() || undefined,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['app-signatures'] });
      setShowAddForm(false);
      setForm({ processName: '', displayName: '', categoryPath: '', productivity: 'neutral', icon: '', description: '' });
    },
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteAppSignature(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['app-signatures'] });
    },
  });

  return (
    <div className="space-y-4">
      <PageHeader
        title="App 知识库"
        subtitle="管理已知应用的名称、分类和图标映射"
      />

      {/* Search + Add toolbar */}
      <div className="flex flex-wrap items-center gap-3">
        <input
          type="text"
          placeholder="搜索应用名称或进程名..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition-colors focus:border-blue-400"
        />
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
        >
          {showAddForm ? '取消' : '+ 添加应用'}
        </button>
      </div>

      {/* Add form */}
      {showAddForm && (
        <div className="rounded-lg border border-slate-200 bg-white p-4 space-y-3">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label className="text-xs font-medium text-slate-500">进程名 *</label>
              <input type="text" value={form.processName} onChange={e => setForm(f => ({ ...f, processName: e.target.value }))}
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400" />
            </div>
            <div>
              <label className="text-xs font-medium text-slate-500">显示名称 *</label>
              <input type="text" value={form.displayName} onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))}
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400" />
            </div>
            <div>
              <label className="text-xs font-medium text-slate-500">分类路径（如 工作·编程）</label>
              <input type="text" value={form.categoryPath} onChange={e => setForm(f => ({ ...f, categoryPath: e.target.value }))}
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400" />
            </div>
            <div>
              <label className="text-xs font-medium text-slate-500">效率评分</label>
              <select value={form.productivity} onChange={e => setForm(f => ({ ...f, productivity: e.target.value }))}
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400">
                {productivities.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-slate-500">Emoji 图标</label>
              <input type="text" value={form.icon} onChange={e => setForm(f => ({ ...f, icon: e.target.value }))}
                placeholder="🎮"
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400" />
            </div>
            <div>
              <label className="text-xs font-medium text-slate-500">描述</label>
              <input type="text" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400" />
            </div>
          </div>
          <div className="flex justify-end">
            <button
              onClick={() => createMut.mutate()}
              disabled={!form.processName.trim() || !form.displayName.trim() || createMut.isPending}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
            >
              {createMut.isPending ? '提交中...' : '保存'}
            </button>
          </div>
        </div>
      )}

      {/* Table */}
      {isLoading ? (
        <div className="text-center text-sm text-slate-500 py-8">加载中...</div>
      ) : signatures.length === 0 ? (
        <div className="text-center text-sm text-slate-500 py-8">
          {search ? '未找到匹配的应用' : '知识库为空，点击"+ 添加应用"开始添加'}
        </div>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-slate-200">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
                <th className="px-3 py-2">图标</th>
                <th className="px-3 py-2">显示名称</th>
                <th className="px-3 py-2">进程名</th>
                <th className="px-3 py-2">分类路径</th>
                <th className="px-3 py-2">效率</th>
                <th className="px-3 py-2">来源</th>
                <th className="px-3 py-2">操作</th>
              </tr>
            </thead>
            <tbody>
              {signatures.map(sig => (
                <tr key={sig.id} className="border-b border-slate-100 transition-colors hover:bg-slate-50">
                  <td className="px-3 py-2.5 text-lg">{sig.icon || '❓'}</td>
                  <td className="px-3 py-2.5 font-medium text-slate-900">{sig.displayName}</td>
                  <td className="px-3 py-2.5 font-mono text-xs text-slate-500">{sig.processName}</td>
                  <td className="px-3 py-2.5 text-slate-600">{sig.categoryPath || '-'}</td>
                  <td className="px-3 py-2.5">
                    {productivities.find(p => p.value === sig.productivity) ? (
                      <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${productivities.find(p => p.value === sig.productivity)?.color}`}>
                        {productivities.find(p => p.value === sig.productivity)?.label}
                      </span>
                    ) : '-'}
                  </td>
                  <td className="px-3 py-2.5">
                    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${sig.source === 'builtin' ? 'bg-slate-100 text-slate-600' : 'bg-blue-50 text-blue-700'}`}>
                      {sig.source === 'builtin' ? '内置' : '自定义'}
                    </span>
                  </td>
                  <td className="px-3 py-2.5">
                    <button
                      onClick={() => {
                        if (sig.source === 'builtin') {
                          alert('内置项不可删除');
                          return;
                        }
                        if (confirm(`确定删除「${sig.displayName}」？`)) {
                          deleteMut.mutate(sig.id);
                        }
                      }}
                      disabled={sig.source === 'builtin'}
                      className={`rounded px-2 py-1 text-xs font-medium transition-colors ${
                        sig.source === 'builtin'
                          ? 'text-slate-300 cursor-not-allowed'
                          : 'text-red-500 hover:bg-red-50'
                      }`}
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
