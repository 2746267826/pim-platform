import { useEffect, useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getManagedDevices, renameDevice, previewMerge, mergeDevices, previewDeleteDevice, deleteDevice, exportDevice } from '../api/mobile';

function MergeConfirmDialog({
  mergeSel,
  devices,
  onClose,
}: {
  mergeSel: string[];
  devices: { deviceId: string; displayName: string }[];
  onClose: () => void;
}) {
  const qc = useQueryClient();
  const [targetId, setTargetId] = useState('');
  const [preview, setPreview] = useState<{ items: { deviceId: string; dataCount: number }[]; total: number } | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDivElement>(null);
  const previouslyFocusedRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    previouslyFocusedRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialogRef.current?.focus();
    return () => {
      previouslyFocusedRef.current?.focus();
    };
  }, []);

  const mergeMut = useMutation({
    mutationFn: ({ src, tgt }: { src: string[]; tgt: string }) => mergeDevices(src, tgt),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['managed-devices'] });
      onClose();
    },
  });

  const handleTargetChange = async (tgt: string) => {
    setTargetId(tgt);
    if (!tgt) {
      setPreview(null);
      return;
    }
    const src = mergeSel.filter(x => x !== tgt);
    if (src.length === 0) {
      setPreview(null);
      setPreviewError('至少需要选择 2 台不同的设备');
      return;
    }
    setPreviewLoading(true);
    setPreviewError(null);
    try {
      const p = await previewMerge(src, tgt);
      setPreview(p);
    } catch (e) {
      setPreviewError(e instanceof Error ? e.message : '预览失败');
    } finally {
      setPreviewLoading(false);
    }
  };

  const srcDevices = mergeSel.filter(x => x !== targetId);
  const canMerge = targetId && srcDevices.length > 0 && !mergeMut.isPending;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/40 backdrop-blur-xs animate-backdrop" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="merge-confirm-dialog-title"
        tabIndex={-1}
        ref={dialogRef}
        onKeyDown={e => { if (e.key === 'Escape') { e.stopPropagation(); onClose(); } }}
        className="w-full max-w-lg rounded-xl border border-zinc-200 bg-white shadow-dialog animate-dialog"
        onClick={e => e.stopPropagation()}
      >
        <header className="flex items-center justify-between border-b border-zinc-200 px-5 py-4">
          <h2 id="merge-confirm-dialog-title" className="text-base font-semibold text-zinc-900">合并设备</h2>
          <button onClick={onClose} className="text-zinc-400 hover:text-zinc-600">
            <i data-lucide="x" className="w-4 h-4"></i>
          </button>
        </header>
        <div className="overflow-y-auto max-h-[75vh] px-5 py-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-zinc-700 mb-1">合并到目标设备</label>
            <select
              value={targetId}
              onChange={e => handleTargetChange(e.target.value)}
              className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:border-blue-300 focus:outline-none"
            >
              <option value="">选择目标设备</option>
              {mergeSel.map(id => {
                const d = devices.find(x => x.deviceId === id);
                return (
                  <option key={id} value={id}>{d?.displayName || id}</option>
                );
              })}
            </select>
          </div>

          {previewLoading && (
            <p className="text-sm text-zinc-500">正在加载预览...</p>
          )}

          {previewError && (
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{previewError}</p>
          )}

          {preview && !previewLoading && (
            <div className="rounded-lg border border-zinc-200 bg-zinc-50 p-3 space-y-2">
              <p className="text-sm font-semibold text-zinc-800">合并预览</p>
              <p className="text-sm text-zinc-700">影响记录总数：<span className="font-bold text-zinc-900">{preview.total}</span> 条</p>
              {preview.items.length > 0 && (
                <p className="text-xs text-zinc-500">涉及 {preview.items.length} 台设备</p>
              )}
            </div>
          )}

          {mergeMut.isError && (
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              合并失败：{mergeMut.error instanceof Error ? mergeMut.error.message : '请求失败'}
            </p>
          )}
        </div>
        <footer className="flex items-center justify-between border-t border-zinc-200 px-5 py-4">
          <div className="text-xs text-zinc-400">
            {mergeSel.length} 台设备已选
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              className="px-4 py-2 text-sm rounded-lg border border-zinc-200 text-zinc-600 hover:bg-zinc-50"
              onClick={onClose}
            >
              取消
            </button>
            <button
              type="button"
              className="px-4 py-2 text-sm rounded-lg bg-amber-600 text-white hover:bg-amber-700 disabled:opacity-50"
              disabled={!canMerge}
              onClick={() => {
                if (targetId) {
                  mergeMut.mutate({ src: mergeSel.filter(x => x !== targetId), tgt: targetId });
                }
              }}
            >
              {mergeMut.isPending ? '合并中...' : '确认合并'}
            </button>
          </div>
        </footer>
      </div>
    </div>
  );
}

export default function DeviceManagementPage() {
  const qc = useQueryClient();
  const [sortBy, setSortBy] = useState<'lastSeen'|'data'>('lastSeen');
  const { data: devices, isLoading } = useQuery({ queryKey: ['managed-devices', sortBy], queryFn: () => getManagedDevices(sortBy) });
  const [renameId, setRenameId] = useState<string | null>(null);
  const [renameVal, setRenameVal] = useState('');
  const [mergeSel, setMergeSel] = useState<string[]>([]);
  const [showMergeDialog, setShowMergeDialog] = useState(false);
  const renameMut = useMutation({ mutationFn: ({id, name}:{id:string,name:string})=>renameDevice(id,name), onSuccess:()=>qc.invalidateQueries({queryKey:['managed-devices']})});
  const delMut = useMutation({ mutationFn: (id:string)=>deleteDevice(id), onSuccess:()=>qc.invalidateQueries({queryKey:['managed-devices']})});
  if (isLoading) return <div className="p-4">加载中...</div>;

  const handleMerge = () => {
    setShowMergeDialog(true);
  };

  return <div className="p-4 space-y-4">
    <h1 className="text-xl font-semibold">设备管理</h1>
    <div className="flex gap-2">
      <button onClick={()=>setSortBy('lastSeen')} className={`px-3 py-1 rounded ${sortBy==='lastSeen'?'bg-slate-900 text-white':'bg-slate-100'}`}>按活跃</button>
      <button onClick={()=>setSortBy('data')} className={`px-3 py-1 rounded ${sortBy==='data'?'bg-slate-900 text-white':'bg-slate-100'}`}>按数据量</button>
    </div>

    {/* 批量操作与状态引导栏 */}
    <div className="flex items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
      <p className="text-sm text-slate-600">
        {mergeSel.length === 0
          ? '请勾选需要合并或管理的设备'
          : mergeSel.length === 1
            ? '已选 1 台，请至少勾选 2 台设备以执行数据合并'
            : `已选 ${mergeSel.length} 台设备`}
      </p>
      {mergeSel.length >= 2 && (
        <button
          type="button"
          className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
          onClick={handleMerge}
        >
          合并选中的设备...
        </button>
      )}
    </div>

    <div className="space-y-2">
      {(devices??[]).map(d=> <div key={d.deviceId} className="border p-3 rounded flex justify-between">
        <div className="space-y-1">
          <div className="font-medium">{d.displayName || d.deviceId} <span className="text-xs text-slate-500">{d.isOnline?'在线':'离线'}</span></div>
          <div className="text-xs text-slate-600">{d.brand} {d.model} · {d.osVersion} · App {d.appVersion}</div>
          <div className="text-xs">ID: {d.deviceId} <button className="ml-2 text-blue-600" onClick={()=>navigator.clipboard.writeText(d.deviceId)}>复制</button></div>
          <div className="text-xs">注册: {d.registeredAtUtc} · 最后活跃: {d.lastSeenAtUtc}</div>
          <div className="text-xs">sessions:{d.sessionCount} events:{d.eventCount} locations:{d.locationCount} 范围:{d.earliest}~{d.latest} 占用:{d.storageEstimateKb}KB</div>
          <div className="text-xs">同步:{d.syncStatus} 数据质量:{d.dataQuality} 存储:{d.storagePressure}</div>
          {renameId===d.deviceId ? <div className="flex gap-2"><input value={renameVal} onChange={e=>setRenameVal(e.target.value)} maxLength={50} className="border px-2 py-1"/><button onClick={()=>{renameMut.mutate({id:d.deviceId,name:renameVal}); setRenameId(null);}} className="bg-blue-600 text-white px-2">保存</button><button onClick={()=>setRenameId(null)}>取消</button></div> : <button className="text-blue-600 text-xs" onClick={()=>{setRenameId(d.deviceId); setRenameVal(d.displayName);}}>重命名</button>}
        </div>
        <div className="flex flex-col gap-2">
          <label className="text-xs"><input type="checkbox" checked={mergeSel.includes(d.deviceId)} onChange={e=> setMergeSel(prev=> e.target.checked? [...prev,d.deviceId] : prev.filter(x=>x!==d.deviceId))}/> 合并</label>
          <button className="text-xs text-red-600" onClick={async()=>{ const p=await previewDeleteDevice(d.deviceId); if(confirm(`删除 ${p.displayName} 将删除 sessions:${p.sessionCount} events:${p.eventCount} 不可恢复 确认?`)) delMut.mutate(d.deviceId);}}>删除</button>
          <button className="text-xs text-slate-600" onClick={async()=>{ const blob=await exportDevice(d.deviceId); const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download=`pim-export-${d.displayName}-${new Date().toISOString().slice(0,10)}.json`; a.click();}}>导出</button>
          <a className="text-xs text-blue-600" href={`/devices/${d.deviceId}`}>详情</a>
        </div>
      </div>)}
    </div>

    {showMergeDialog && (
      <MergeConfirmDialog
        mergeSel={mergeSel}
        devices={devices ?? []}
        onClose={() => {
          setShowMergeDialog(false);
          setMergeSel([]);
        }}
      />
    )}
  </div>;
}
