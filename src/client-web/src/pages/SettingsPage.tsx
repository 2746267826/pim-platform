import { useNavigate } from 'react-router-dom';

export default function SettingsPage() {
  const navigate = useNavigate();

  return (
    <div className="max-w-2xl mx-auto">
      <h2 className="text-xl font-bold mb-6">设置</h2>

      <div
        className="bg-white border rounded-lg p-5 hover:border-blue-300 cursor-pointer transition-colors flex items-center justify-between"
        onClick={() => navigate('/settings/calendar-data')}
      >
        <div>
          <h3 className="font-semibold text-base flex items-center gap-2">
            <span>📅</span> 管理日程数据
          </h3>
          <p className="text-sm text-gray-500 mt-1">
            查看、筛选、导入导出全部日程
          </p>
        </div>
        <span className="text-gray-300 text-xl">→</span>
      </div>
    </div>
  );
}
