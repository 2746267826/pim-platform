import { useNavigate, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCalendars } from '../api/calendar';
import { useAuth } from '../auth/AuthContext';

const navItems = [
  { label: '时间轴', path: '/timeline', icon: '⏱' },
  { label: '本周', path: '/week', icon: '📅' },
  { label: '月视图', path: '/month', icon: '📆' },
  { label: '任务', path: '/tasks', icon: '📋' },
  { label: 'PC记录', path: '/pc-tracker', icon: '💻' },
  { label: '设置', path: '/settings', icon: '⚙' },
];

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout, username } = useAuth();
  const { data: calendars } = useQuery({
    queryKey: ['calendars'],
    queryFn: getCalendars
  });

  return (
    <div className="w-[200px] bg-gray-50 border-r flex flex-col h-full">
      <div className="p-4 font-bold text-lg text-blue-600">PIM</div>

      <nav className="flex-1 px-2 space-y-1">
        {navItems.map(item => (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            className={`w-full text-left px-3 py-2 rounded text-sm font-medium transition-colors ${
              location.pathname.startsWith(item.path)
                ? 'bg-blue-100 text-blue-700'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            {item.icon}  {item.label}
          </button>
        ))}
      </nav>

      <div className="p-3 border-t">
        <p className="text-xs text-gray-400 mb-2">日历本</p>
        {calendars?.map(cal => (
          <div key={cal.id} className="flex items-center gap-2 py-1">
            <span className="w-3 h-3 rounded-full" style={{ backgroundColor: cal.color }} />
            <span className="text-xs text-gray-600">{cal.name}</span>
          </div>
        ))}
      </div>

      <div className="p-3 border-t flex items-center justify-between">
        <span className="text-xs text-gray-500">{username}</span>
        <button onClick={logout} className="text-xs text-red-500 hover:underline">退出</button>
      </div>
    </div>
  );
}
