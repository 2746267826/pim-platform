import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getManagedDevices, renameDevice, previewMerge, mergeDevices, previewDeleteDevice, deleteDevice, exportDevice } from '../api/mobile';

export default function DeviceManagementPage() {
  const qc = useQueryClient();
  const [sortBy, setSortBy] = useState<'lastSeen'|'data'>('lastSeen');
  const { data: devices, isLoading } = useQuery({ queryKey: ['managed-devices', sortBy], queryFn: () => getManagedDevices(sortBy) });
  const [renameId, setRenameId] = useState<string | null>(null);
  const [renameVal, setRenameVal] = useState('');
  const [mergeSel, setMergeSel] = useState<string[]>([]);
  const renameMut = useMutation({ mutationFn: ({id, name}:{id:string,name:string})=>renameDevice(id,name), onSuccess:()=>qc.invalidateQueries({queryKey:['managed-devices']})});
  const mergeMut = useMutation({ mutationFn: ({src,tgt}:{src:string[],tgt:string})=>mergeDevices(src,tgt), onSuccess:()=>qc.invalidateQueries({queryKey:['managed-devices']})});
  const delMut = useMutation({ mutationFn: (id:string)=>deleteDevice(id), onSuccess:()=>qc.invalidateQueries({queryKey:['managed-devices']})});
  if (isLoading) return <div className="p-4">加载中...</div>;
  return <div className="p-4 space-y-4">
    <h1 className="text-xl font-semibold">设备管理</h1>
    <div className="flex gap-2">
      <button onClick={()=>setSortBy('lastSeen')} className={`px-3 py-1 rounded ${sortBy==='lastSeen'?'bg-slate-900 text-white':'bg-slate-100'}`}>按活跃</button>
      <button onClick={()=>setSortBy('data')} className={`px-3 py-1 rounded ${sortBy==='data'?'bg-slate-900 text-white':'bg-slate-100'}`}>按数据量</button>
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
    {mergeSel.length>=2 && <div className="border p-3 rounded bg-amber-50">
      <div>已选 {mergeSel.length} 台，合并到：<select onChange={e=> (window as any).__mergeTarget=e.target.value}><option value="">选择目标</option>{mergeSel.map(id=><option key={id} value={id}>{id}</option>)}</select>
      <button className="ml-2 bg-amber-600 text-white px-3 py-1 text-xs" onClick={async()=>{ const tgt=(window as any).__mergeTarget; if(!tgt) return alert('请选择目标'); const src=mergeSel.filter(x=>x!==tgt); const p=await previewMerge(src,tgt); if(confirm(`合并预览 total:${p.total} 确认?`)) mergeMut.mutate({src,tgt});}}>合并</button></div>
    </div>}
  </div>;
}
