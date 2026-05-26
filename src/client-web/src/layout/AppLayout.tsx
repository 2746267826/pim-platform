import { useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { CalendarVisibilityProvider } from '../context/CalendarVisibilityContext';
import QuickNoteFloatingButton from '../components/quick-notes/QuickNoteFloatingButton';
import QuickNoteFloatingPanel from '../components/quick-notes/QuickNoteFloatingPanel';
import Sidebar from './Sidebar';
import InboxPanel from '../panels/InboxPanel';
import TodayPage from '../pages/TodayPage';
import CalendarPage from '../pages/CalendarPage';
import TaskListPage from '../pages/TaskListPage';
import PcTrackerPage from '../pages/PcTrackerPage';
import PcClassificationPage from '../pages/PcClassificationPage';
import SettingsPage from '../pages/SettingsPage';
import CalendarDataManager from '../pages/CalendarDataManager';
import PcDetailQueryPage from '../pages/PcDetailQueryPage';
import StatusPage from '../pages/StatusPage';

export default function AppLayout() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const [quickNoteOpen, setQuickNoteOpen] = useState(false);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const showCalendarInbox = location.pathname === '/calendar' || location.pathname.startsWith('/calendar/');

  return (
    <CalendarVisibilityProvider>
      <div className="pim-shell h-screen flex overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-auto p-4">
          <Routes>
            <Route path="/today" element={<TodayPage />} />
            <Route path="/calendar" element={<CalendarPage />} />
            <Route path="/timeline" element={<Navigate to="/calendar?view=timeline" replace />} />
            <Route path="/week" element={<Navigate to="/calendar?view=timeline" replace />} />
            <Route path="/month" element={<Navigate to="/calendar?view=month" replace />} />
            <Route path="/tasks" element={<TaskListPage />} />
            <Route path="/pc-tracker" element={<PcTrackerPage />} />
            <Route path="/pc-classification" element={<PcClassificationPage />} />
            <Route path="/status" element={<StatusPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/settings/calendar-data" element={<CalendarDataManager />} />
            <Route path="/settings/pc-data" element={<PcDetailQueryPage />} />
            <Route path="*" element={<Navigate to="/today" replace />} />
          </Routes>
        </main>
        {showCalendarInbox && <InboxPanel draggable />}
        <QuickNoteFloatingButton onClick={() => setQuickNoteOpen(true)} />
        {quickNoteOpen && <QuickNoteFloatingPanel onClose={() => setQuickNoteOpen(false)} />}
      </div>
    </CalendarVisibilityProvider>
  );
}
