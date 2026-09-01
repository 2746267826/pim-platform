import { lazy, Suspense, useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useVersionInfo } from '../hooks/useVersionInfo';
import { useAuth } from '../auth/AuthContext';
import { CalendarVisibilityProvider } from '../context/CalendarVisibilityContext';
import QuickNoteFloatingButton from '../components/quick-notes/QuickNoteFloatingButton';
import Sidebar from './Sidebar';
import MobileNav from './MobileNav';
import InboxPanel from '../panels/InboxPanel';
import TodayPage from '../pages/TodayPage';
import CalendarPage from '../pages/CalendarPage';
import TaskListPage from '../pages/TaskListPage';
import PcTrackerPage from '../pages/PcTrackerPage';
import SettingsPage from '../pages/SettingsPage';
import AiSettingsPage from '../pages/AiSettingsPage';
import McpSettingsPage from '../pages/McpSettingsPage';
import CalendarDataManager from '../pages/CalendarDataManager';
import RecycleBinPage from '../pages/RecycleBinPage';
import PcDetailQueryPage from '../pages/PcDetailQueryPage';
import StatusPage from '../pages/StatusPage';
import AppKnowledgeBasePage from '../pages/AppKnowledgeBasePage';
import CategoryTreePage from '../pages/CategoryTreePage';
import { ErrorBoundary } from '../components/error/ErrorBoundary';
import NotFoundPage from '../components/error/NotFoundPage';

const QuickNotesPage = lazy(() => import('../pages/QuickNotesPage'));
const FilesPage = lazy(() => import('../pages/FilesPage'));
const MobileRecordsPage = lazy(() => import('../pages/MobileRecordsPage'));
const HistoricalLocationPage = lazy(() => import('../pages/HistoricalLocationPage'));
const QuickNoteFloatingPanel = lazy(() => import('../components/quick-notes/QuickNoteFloatingPanel'));
const WorkbenchPage = lazy(() => import('../pages/WorkbenchPage'));
const SyncPage = lazy(() => import('../pages/SyncPage'));
const DeviceManagementPage = lazy(() => import('../pages/DeviceManagementPage'));
const DeviceDetailPage = lazy(() => import('../pages/DeviceDetailPage'));
const DataCenterPage = lazy(() => import('../pages/DataCenterPage'));
const ConfirmationsPage = lazy(() => import('../pages/ConfirmationsPage'));
const RemindersPage = lazy(() => import('../pages/RemindersPage'));
const ReportsPage = lazy(() => import('../pages/ReportsPage'));
const HabitsPage = lazy(() => import('../pages/HabitsPage'));
const AuditTimelinePage = lazy(() => import('../pages/AuditTimelinePage'));
const EndpointShellPage = lazy(() => import('../pages/EndpointShellPage'));
const ExhibitionPage = lazy(() => import('../pages/ExhibitionPage'));

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
  const { localVersion, serverVersion, latestVersion, hasUpdate } = useVersionInfo();

  return (
    <CalendarVisibilityProvider>
      <div className="pim-shell h-screen flex overflow-hidden">
        <Sidebar />
        <main className="pim-route-surface flex-1 overflow-auto p-4 pb-20 md:pb-4">
          <ErrorBoundary key={location.pathname} resetKeys={[location.pathname]}>
            <Suspense fallback={<SuspenseFallback />}>
              <Routes>
                <Route path="/today" element={<TodayPage />} />
                <Route path="/calendar" element={<CalendarPage />} />
                <Route path="/workbench" element={<WorkbenchPage />} />
                <Route path="/sync" element={<Navigate to="/settings/sync" replace />} />
                <Route path="/data-center" element={<DataCenterPage />} />
                <Route path="/confirmations" element={<ConfirmationsPage />} />
                <Route path="/reminders" element={<RemindersPage />} />
                <Route path="/reports" element={<ReportsPage />} />
                <Route path="/habits" element={<HabitsPage />} />
                <Route path="/exhibition" element={<ExhibitionPage />} />
                <Route path="/audit/:objectType/:objectId" element={<AuditTimelinePage />} />
                <Route path="/endpoint-shell" element={<EndpointShellPage />} />
                <Route path="/quick-notes" element={<QuickNotesPage />} />
                <Route path="/files" element={<FilesPage />} />
                <Route path="/timeline" element={<Navigate to="/calendar?view=timeline" replace />} />
                <Route path="/week" element={<Navigate to="/calendar?view=timeline" replace />} />
                <Route path="/month" element={<Navigate to="/calendar?view=month" replace />} />
                <Route path="/tasks" element={<TaskListPage />} />
                <Route path="/pc-tracker" element={<PcTrackerPage />} />
                <Route path="/mobile-records" element={<MobileRecordsPage />} />
                <Route path="/location-history" element={<HistoricalLocationPage />} />
                <Route path="/devices" element={<DeviceManagementPage />} />
                <Route path="/devices/:deviceId" element={<DeviceDetailPage />} />
                <Route path="/status" element={<StatusPage />} />
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="/settings/sync" element={<SyncPage />} />
                <Route path="/settings/ai" element={<AiSettingsPage />} />
                <Route path="/settings/mcp" element={<McpSettingsPage />} />
                <Route path="/settings/calendar-data" element={<CalendarDataManager />} />
                <Route path="/settings/recycle-bin" element={<RecycleBinPage />} />
                <Route path="/settings/pc-data" element={<PcDetailQueryPage />} />
                <Route path="/app-knowledge-base" element={<AppKnowledgeBasePage />} />
                <Route path="/app-knowledge-base/categories" element={<CategoryTreePage />} />
                <Route path="/pc-categories" element={<Navigate to="/app-knowledge-base/categories" replace />} />
                <Route path="/pc-classification" element={<Navigate to="/app-knowledge-base" replace />} />
                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </Suspense>
          </ErrorBoundary>
          <footer className="mt-6 flex gap-3 border-t border-slate-100 pt-3 text-xs text-slate-400">
            <span>v{localVersion}</span><span>API v{serverVersion ?? '...'}</span>{hasUpdate && <span className="text-amber-600">有新版 v{latestVersion}</span>}
          </footer>
        </main>
        {showCalendarInbox && <InboxPanel draggable />}
        <QuickNoteFloatingButton onClick={() => setQuickNoteOpen(true)} />
        {quickNoteOpen && (
          <Suspense fallback={null}>
            <QuickNoteFloatingPanel onClose={() => setQuickNoteOpen(false)} />
          </Suspense>
        )}
        <MobileNav />
      </div>
    </CalendarVisibilityProvider>
  );
}
