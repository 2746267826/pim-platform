import { Routes, Route, Navigate } from 'react-router-dom'

function LoginPage() {
  return <div>Login</div>
}

function AppLayout() {
  return <div>App Layout</div>
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={<AppLayout />} />
      <Route path="/" element={<Navigate to="/timeline" replace />} />
    </Routes>
  )
}
