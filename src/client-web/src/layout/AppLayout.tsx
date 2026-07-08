import { lazy, Suspense, useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { CalendarVisibilityProvider } from '../context/CalendarVisibilityContext';
import QuickNoteFloatingButton from '../components/quick-notes/QuickNoteFloatingButton';
import Sidebar from './Sidebar';
import InboxPanel from '../panels/InboxPanel';
import TodayPage from '../pages/TodayPage';
import CalendarPage from '../pages/CalendarPage';
import TaskListPage from '../pages/TaskListPage';
import PcTrackerPage from '../pages/PcTrackerPage';
import SettingsPage from '../pages/SettingsPage';
import AiSettingsPage from '../pages/AiSettingsPage';
import CalendarDataManager from '../pages/CalendarDataManager';
import RecycleBinPage from '../pages/RecycleBinPage';
import PcDetailQueryPage from '../pages/PcDetailQueryPage';
import StatusPage from '../pages/StatusPage';
import AppKnowledgeBasePage from '../pages/AppKnowledgeBasePage';
import CategoryTreePage from '../pages/CategoryTreePage';

const QuickNotesPage = lazy(() => import('../pages/QuickNotesPage'));
const FilesPage = lazy(() => import('../pages/FilesPage'));
const MobileRecordsPage = lazy(() => import('../pages/MobileRecordsPage'));
const HistoricalLocationPage = lazy(() => import('../pages/HistoricalLocationPage'));
const QuickNoteFloatingPanel = lazy(() => import('../components/quick-notes/QuickNoteFloatingPanel'));
const WorkbenchPage = lazy(() => import('../pages/WorkbenchPage'));
const SyncPage = lazy(() => import('../pages/SyncPage'));
const DataCenterPage = lazy(() => import('../pages/DataCenterPage'));
const ConfirmationsPage = lazy(() => import('../pages/ConfirmationsPage'));
const RemindersPage = lazy(() => import('../pages/RemindersPage'));
const ReportsPage = lazy(() => import('../pages/ReportsPage'));
const HabitsPage = lazy(() => import('../pages/HabitsPage'));
const AuditTimelinePage = lazy(() => import('../pages/AuditTimelinePage'));

function SuspenseFallback() {
  return <div className="h-full" aria-busy="true" />;
}

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
          <Suspense fallback={<SuspenseFallback />}>
            <Routes>
              <Route path="/today" element={<TodayPage />} />
              <Route path="/calendar" element={<CalendarPage />} />
              <Route path="/workbench" element={<WorkbenchPage />} />
              <Route path="/sync" element={<SyncPage />} />
              <Route path="/data-center" element={<DataCenterPage />} />
              <Route path="/confirmations" element={<ConfirmationsPage />} />
              <Route path="/reminders" element={<RemindersPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/habits" element={<HabitsPage />} />
              <Route path="/audit/:objectType/:objectId" element={<AuditTimelinePage />} />
              <Route path="/quick-notes" element={<QuickNotesPage />} />
              <Route path="/files" element={<FilesPage />} />
              <Route path="/timeline" element={<Navigate to="/calendar?view=timeline" replace />} />
              <Route path="/week" element={<Navigate to="/calendar?view=timeline" replace />} />
              <Route path="/month" element={<Navigate to="/calendar?view=month" replace />} />
              <Route path="/tasks" element={<TaskListPage />} />
              <Route path="/pc-tracker" element={<PcTrackerPage />} />
              <Route path="/mobile-records" element={<MobileRecordsPage />} />
              <Route path="/location-history" element={<HistoricalLocationPage />} />
              <Route path="/status" element={<StatusPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/settings/ai" element={<AiSettingsPage />} />
              <Route path="/settings/calendar-data" element={<CalendarDataManager />} />
              <Route path="/settings/recycle-bin" element={<RecycleBinPage />} />
              <Route path="/settings/pc-data" element={<PcDetailQueryPage />} />
              <Route path="/app-knowledge-base" element={<AppKnowledgeBasePage />} />
              <Route path="/app-knowledge-base/categories" element={<CategoryTreePage />} />
              <Route path="/pc-categories" element={<Navigate to="/app-knowledge-base/categories" replace />} />
              <Route path="/pc-classification" element={<Navigate to="/app-knowledge-base" replace />} />
              <Route path="*" element={<Navigate to="/today" replace />} />
            </Routes>
          </Suspense>
        </main>
        {showCalendarInbox && <InboxPanel draggable />}
        <QuickNoteFloatingButton onClick={() => setQuickNoteOpen(true)} />
        {quickNoteOpen && (
          <Suspense fallback={null}>
            <QuickNoteFloatingPanel onClose={() => setQuickNoteOpen(false)} />
          </Suspense>
        )}
      </div>
    </CalendarVisibilityProvider>
  );
}
