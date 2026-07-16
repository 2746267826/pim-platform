import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import LoginPage from './auth/LoginPage'
import AppLayout from './layout/AppLayout'
import AndroidEmbedLayout from './layout/AndroidEmbedLayout'
import TodayPage from './pages/TodayPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/embed/android/today" element={<AndroidEmbedLayout><TodayPage /></AndroidEmbedLayout>} />
      <Route path="/embed/android/tracks" element={<AndroidEmbedLayout><div className="p-4 text-sm text-slate-500">轨迹页面</div></AndroidEmbedLayout>} />
      <Route path="/*" element={<AuthProvider><Routes><Route path="/login" element={<LoginPage />} /><Route path="/" element={<Navigate to="/today" replace />} /><Route path="/*" element={<AppLayout />} /></Routes></AuthProvider>} />
    </Routes>
  )
}

export default function App() {
  return <AppRoutes />
}
