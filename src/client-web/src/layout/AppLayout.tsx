import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Sidebar from './Sidebar';
import InboxPanel from '../panels/InboxPanel';
import TimelinePage from '../pages/TimelinePage';
import WeekPage from '../pages/WeekPage';
import MonthPage from '../pages/MonthPage';
import TaskListPage from '../pages/TaskListPage';
import PcTrackerPage from '../pages/PcTrackerPage';
import SettingsPage from '../pages/SettingsPage';
import CalendarDataManager from '../pages/CalendarDataManager';

export default function AppLayout() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="h-screen flex">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="flex-1 overflow-auto p-4">
          <Routes>
            <Route path="/timeline" element={<TimelinePage />} />
            <Route path="/week" element={<WeekPage />} />
            <Route path="/month" element={<MonthPage />} />
            <Route path="/tasks" element={<TaskListPage />} />
            <Route path="/pc-tracker" element={<PcTrackerPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/settings/calendar-data" element={<CalendarDataManager />} />
          </Routes>
        </div>
      </div>
      <InboxPanel />
    </div>
  );
}
