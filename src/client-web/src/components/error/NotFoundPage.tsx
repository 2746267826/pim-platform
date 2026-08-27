import { useLocation, useNavigate } from 'react-router-dom';

export default function NotFoundPage() {
  const location = useLocation();
  const navigate = useNavigate();
  return (
    <div className="mx-auto flex min-h-[60vh] w-full max-w-[560px] flex-col items-center justify-center px-4 py-12">
      <div className="pim-panel w-full px-6 py-10 text-center">
        <div className="text-[48px] leading-none" aria-hidden="true">🤷</div>
        <h1 className="mt-4 text-lg font-semibold text-slate-900">页面不存在</h1>
        <p className="mt-2 text-sm text-slate-500">你访问的路径不存在或已迁移</p>
        <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-500">
          当前路径: <span className="font-mono font-semibold text-slate-700">{location.pathname}{location.search}{location.hash}</span>
        </div>
        <button
          type="button"
          onClick={() => navigate('/today', { replace: true })}
          className="pim-button-primary mt-6 px-5 py-2 text-sm"
        >
          返回首页
        </button>
      </div>
    </div>
  );
}
