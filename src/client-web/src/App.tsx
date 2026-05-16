import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import LoginPage from './auth/LoginPage'

function AppLayout() {
  return <div>App Layout</div>
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/*" element={<AppLayout />} />
        <Route path="/" element={<Navigate to="/timeline" replace />} />
      </Routes>
    </AuthProvider>
  )
}
