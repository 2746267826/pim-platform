import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Sidebar from './Sidebar';
import InboxPanel from '../panels/InboxPanel';

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
            <Route path="/timeline" element={<div>Timeline placeholder</div>} />
            <Route path="/week" element={<div>Week placeholder</div>} />
            <Route path="/month" element={<div>Month placeholder</div>} />
            <Route path="/tasks" element={<div>Tasks placeholder</div>} />
          </Routes>
        </div>
      </div>
      <InboxPanel />
    </div>
  );
}
