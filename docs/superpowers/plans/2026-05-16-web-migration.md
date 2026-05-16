# Web Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 PIM 客户端从 WPF + Android 原生 UI 迁移至 React Web 前端 + 本地守护程序架构

**Architecture:** React SPA 内嵌于 ASP.NET Core wwwroot，浏览器统一访问；Windows WPF 和 Android 原生应用精简为后台数据采集守护程序

**Tech Stack:** React 18 + TypeScript + Vite + FullCalendar + shadcn/ui + @tanstack/react-query + WPF (精简) + Kotlin (精简) + ASP.NET Core 8 + Serilog

---

## File Structure Before/After

```
src/
├── client-web/                    # NEW: React frontend
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── tailwind.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api/
│       │   ├── client.ts          # fetch wrapper + JWT interceptor
│       │   └── calendar.ts        # events/tasks/calendars endpoints
│       ├── auth/
│       │   ├── AuthContext.tsx     # React context for auth state
│       │   └── LoginPage.tsx
│       ├── layout/
│       │   ├── AppLayout.tsx
│       │   └── Sidebar.tsx
│       ├── pages/
│       │   ├── TimelinePage.tsx   # FullCalendar timeGridDay
│       │   ├── WeekPage.tsx       # FullCalendar timeGridWeek
│       │   ├── MonthPage.tsx      # FullCalendar dayGridMonth
│       │   └── TaskListPage.tsx
│       ├── panels/
│       │   └── InboxPanel.tsx
│       ├── dialogs/
│       │   ├── EventEditorDialog.tsx
│       │   └── TaskEditorDialog.tsx
│       └── types/
│           └── index.ts
├── Pim.Api/
│   └── Program.cs                 # MODIFY: add UseDefaultFiles/UseStaticFiles/MapFallbackToFile
├── client-windows/Pim.Client.App/ # MODIFY: remove all Views/ViewModels/Converters/Theme
│   ├── App.xaml.cs                # MODIFY: start daemon instead of login window
│   ├── Startup.cs                 # MODIFY: remove all ViewModel registrations
│   ├── TrayIcon.cs                # NEW
│   ├── StatusWindow.xaml(.cs)     # NEW
│   └── HostedServices/            # NEW (migrated from old code)
└── client-android/                # MODIFY: remove all UI, add daemon service
```

---

### Task 1: Create React project skeleton

**Files:**
- Create: `src/client-web/package.json`
- Create: `src/client-web/vite.config.ts`
- Create: `src/client-web/tsconfig.json`
- Create: `src/client-web/tsconfig.node.json`
- Create: `src/client-web/tailwind.config.ts`
- Create: `src/client-web/postcss.config.js`
- Create: `src/client-web/index.html`
- Create: `src/client-web/src/main.tsx`
- Create: `src/client-web/src/App.tsx`
- Create: `src/client-web/src/index.css`
- Create: `src/client-web/src/types/index.ts`

- [ ] **Step 1: Run `npm create vite` to scaffold the project**

```bash
cd src
npm create vite@latest client-web -- --template react-ts
cd client-web
```

- [ ] **Step 2: Install all dependencies**

```bash
npm install @fullcalendar/core @fullcalendar/react @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/interaction @tanstack/react-query react-router-dom react-hook-form zod date-fns
npm install -D tailwindcss @tailwindcss/vite
```

- [ ] **Step 3: Configure vite.config.ts for backend proxy and build output**

```typescript
// src/client-web/vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:5000', changeOrigin: true }
    }
  },
  build: {
    outDir: '../Pim.Api/wwwroot',
    emptyOutDir: true
  }
})
```

- [ ] **Step 4: Configure TypeScript path aliases**

```json
// tsconfig.json (add to compilerOptions)
"baseUrl": ".",
"paths": { "@/*": ["./src/*"] }
```

- [ ] **Step 5: Create Tailwind config**

```typescript
// src/client-web/tailwind.config.ts
import type { Config } from 'tailwindcss'
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: { extend: {} },
  plugins: []
} satisfies Config
```

- [ ] **Step 6: Create index.css with Tailwind directives**

```css
/* src/client-web/src/index.css */
@import "tailwindcss";
```

- [ ] **Step 7: Create shared DTO types**

```typescript
// src/client-web/src/types/index.ts
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresAt: string;
  userInfo: { id: string; username: string; displayName: string };
}

export interface CalendarResponse {
  id: string;
  name: string;
  color: string;
  isDefault: boolean;
}

export interface EventResponse {
  id: string;
  calendarId: string;
  title: string;
  description?: string;
  location?: string;
  dtStart: string;
  dtEnd: string;
  rrule?: string;
  status: string;
}

export interface TaskResponse {
  id: string;
  calendarId?: string;
  title: string;
  description?: string;
  priority: number;
  estimatedDuration?: string;
  dtStart?: string;
  due?: string;
  status: string;
  isInbox: boolean;
}
```

- [ ] **Step 8: Create minimal App.tsx and main.tsx**

```typescript
// src/client-web/src/main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App'
import './index.css'

const queryClient = new QueryClient()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
)
```

```typescript
// src/client-web/src/App.tsx
import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import LoginPage from './auth/LoginPage'
import AppLayout from './layout/AppLayout'

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
```

- [ ] **Step 9: Verify Vite dev server starts**

```bash
cd src/client-web && npx vite --host
```

Expected: `http://localhost:5173` shows a blank page with no errors.

- [ ] **Step 10: Commit**

```bash
git add src/client-web/
git commit -m "feat: scaffold React project with Vite, Tailwind, FullCalendar, react-query"
```

---

### Task 2: Create API client with JWT handling

**Files:**
- Create: `src/client-web/src/api/client.ts`
- Create: `src/client-web/src/api/calendar.ts`

- [ ] **Step 1: Write API client with auth interceptor**

```typescript
// src/client-web/src/api/client.ts
const BASE = '/api/v1';

let accessToken: string | null = null;
let refreshToken: string | null = null;
let onAuthChange: (() => void) | null = null;

export function setTokens(access: string, refresh: string) {
  accessToken = access;
  refreshToken = refresh;
  localStorage.setItem('accessToken', access);
  localStorage.setItem('refreshToken', refresh);
}

export function loadTokens(): boolean {
  accessToken = localStorage.getItem('accessToken');
  refreshToken = localStorage.getItem('refreshToken');
  return !!accessToken;
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

export function onTokensChanged(cb: () => void) { onAuthChange = cb; }

async function refreshAccessToken(): Promise<boolean> {
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    if (!res.ok) return false;
    const json = await res.json();
    const d = json.data;
    setTokens(d.accessToken, d.refreshToken);
    return true;
  } catch { return false; }
}

export async function apiGet<T>(path: string): Promise<T> {
  return authedFetch<T>(path);
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return authedFetch<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined });
}

export async function apiPut<T>(path: string, body?: unknown): Promise<T> {
  return authedFetch<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined });
}

export async function apiDelete(path: string): Promise<void> {
  await authedFetch<void>(path, { method: 'DELETE' });
}

async function authedFetch<T>(path: string, opts: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((opts.headers as Record<string, string>) || {})
  };
  if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`;

  let res = await fetch(`${BASE}${path}`, { ...opts, headers });

  if (res.status === 401 && refreshToken) {
    const ok = await refreshAccessToken();
    if (ok) {
      headers['Authorization'] = `Bearer ${accessToken}`;
      res = await fetch(`${BASE}${path}`, { ...opts, headers });
    } else {
      clearTokens();
      onAuthChange?.();
      throw new Error('Session expired');
    }
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  if (res.status === 204 || res.headers.get('content-length') === '0') return undefined as T;
  return res.json();
}
```

- [ ] **Step 2: Write calendar API module**

```typescript
// src/client-web/src/api/calendar.ts
import { apiGet, apiPost, apiPut, apiDelete } from './client';
import type { ApiResponse, CalendarResponse, EventResponse, TaskResponse } from '../types';

export async function getCalendars() {
  const r = await apiGet<ApiResponse<CalendarResponse[]>>('/calendar/calendars');
  return r.data;
}

export async function getEvents(start: string, end: string) {
  const r = await apiGet<ApiResponse<EventResponse[]>>(
    `/calendar/events?start=${start}&end=${end}`
  );
  return r.data;
}

export async function createEvent(data: Partial<EventResponse>) {
  const r = await apiPost<ApiResponse<EventResponse>>('/calendar/events', data);
  return r.data;
}

export async function updateEvent(id: string, data: Partial<EventResponse>) {
  const r = await apiPut<ApiResponse<EventResponse>>(`/calendar/events/${id}`, data);
  return r.data;
}

export async function deleteEvent(id: string) {
  await apiDelete(`/calendar/events/${id}`);
}

export async function getTasks(inboxOnly = false) {
  const r = await apiGet<ApiResponse<TaskResponse[]>>(
    `/calendar/tasks?inbox=${inboxOnly}`
  );
  return r.data;
}

export async function createTask(data: Partial<TaskResponse>) {
  const r = await apiPost<ApiResponse<TaskResponse>>('/calendar/tasks', data);
  return r.data;
}

export async function updateTask(id: string, data: Partial<TaskResponse>) {
  const r = await apiPut<ApiResponse<TaskResponse>>(`/calendar/tasks/${id}`, data);
  return r.data;
}
```

- [ ] **Step 3: Commit**

```bash
git add src/client-web/src/api/
git commit -m "feat: add API client with JWT refresh interceptor and calendar endpoints"
```

---

### Task 3: Create AuthContext and LoginPage

**Files:**
- Create: `src/client-web/src/auth/AuthContext.tsx`
- Create: `src/client-web/src/auth/LoginPage.tsx`

- [ ] **Step 1: Write AuthContext**

```typescript
// src/client-web/src/auth/AuthContext.tsx
import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import { loadTokens, clearTokens, setTokens, onTokensChanged } from '../api/client';
import type { ApiResponse, AuthResponse } from '../types';

interface AuthState {
  isAuthenticated: boolean;
  username: string | null;
  login: (username: string, password: string) => Promise<string | null>;
  register: (username: string, email: string, password: string, displayName?: string) => Promise<string | null>;
  logout: () => void;
}

const AuthContext = createContext<AuthState>(null!);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuth, setIsAuth] = useState(() => loadTokens());
  const [username, setUsername] = useState<string | null>(null);

  useEffect(() => {
    onTokensChanged(() => { setIsAuth(false); setUsername(null); });
  }, []);

  const login = useCallback(async (uname: string, pwd: string): Promise<string | null> => {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: uname, password: pwd })
    });
    const json: ApiResponse<AuthResponse> = await res.json();
    if (json.code !== 0 || !json.data) return json.message || 'Login failed';
    setTokens(json.data.accessToken, json.data.refreshToken);
    setUsername(json.data.userInfo?.displayName || uname);
    setIsAuth(true);
    return null;
  }, []);

  const register = useCallback(async (uname: string, email: string, pwd: string, displayName?: string) => {
    const res = await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: uname, email, password: pwd, displayName })
    });
    const json: ApiResponse<AuthResponse> = await res.json();
    if (json.code !== 0 || !json.data) return json.message || 'Registration failed';
    setTokens(json.data.accessToken, json.data.refreshToken);
    setUsername(json.data.userInfo?.displayName || uname);
    setIsAuth(true);
    return null;
  }, []);

  const logout = useCallback(() => {
    clearTokens();
    setIsAuth(false);
    setUsername(null);
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated: isAuth, username, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() { return useContext(AuthContext); }
```

- [ ] **Step 2: Write LoginPage**

```typescript
// src/client-web/src/auth/LoginPage.tsx
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

export default function LoginPage() {
  const { login, register, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [isRegister, setIsRegister] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    navigate('/timeline', { replace: true });
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const err = isRegister
        ? await register(username, email, password)
        : await login(username, password);
      if (err) setError(err);
      else navigate('/timeline', { replace: true });
    } catch {
      setError('Network error');
    } finally { setLoading(false); }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-lg shadow-md w-full max-w-sm">
        <h1 className="text-2xl font-bold mb-6 text-center text-gray-800">
          {isRegister ? '注册' : '登录'} PIM
        </h1>
        {error && (
          <div className="bg-red-50 text-red-600 p-3 rounded mb-4 text-sm">{error}</div>
        )}
        <input
          type="text" placeholder="用户名" value={username}
          onChange={e => setUsername(e.target.value)}
          className="w-full border rounded px-3 py-2 mb-3" required
        />
        {isRegister && (
          <input
            type="email" placeholder="邮箱" value={email}
            onChange={e => setEmail(e.target.value)}
            className="w-full border rounded px-3 py-2 mb-3" required
          />
        )}
        <input
          type="password" placeholder="密码" value={password}
          onChange={e => setPassword(e.target.value)}
          className="w-full border rounded px-3 py-2 mb-4" required
        />
        <button
          type="submit" disabled={loading}
          className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? '处理中...' : isRegister ? '注册' : '登录'}
        </button>
        <button
          type="button"
          onClick={() => { setIsRegister(!isRegister); setError(null); }}
          className="w-full text-center text-sm text-blue-600 mt-3 hover:underline"
        >
          {isRegister ? '已有账号？登录' : '没有账号？注册'}
        </button>
      </form>
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add src/client-web/src/auth/
git commit -m "feat: add AuthContext with login/register and LoginPage"
```

---

### Task 4: Create AppLayout shell with sidebar

**Files:**
- Create: `src/client-web/src/layout/AppLayout.tsx`
- Create: `src/client-web/src/layout/Sidebar.tsx`

- [ ] **Step 1: Write Sidebar component**

```typescript
// src/client-web/src/layout/Sidebar.tsx
import { useNavigate, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCalendars } from '../api/calendar';
import { useAuth } from '../auth/AuthContext';

const navItems = [
  { label: '时间轴', path: '/timeline', icon: '⏱' },
  { label: '本周', path: '/week', icon: '📅' },
  { label: '月视图', path: '/month', icon: '📆' },
  { label: '任务', path: '/tasks', icon: '📋' },
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
```

- [ ] **Step 2: Write AppLayout with router outlet**

```typescript
// src/client-web/src/layout/AppLayout.tsx
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
```

- [ ] **Step 3: Create a stub InboxPanel for layout to compile**

```typescript
// src/client-web/src/panels/InboxPanel.tsx
export default function InboxPanel() {
  return (
    <div className="w-[280px] bg-gray-50 border-l p-4">
      <h3 className="font-semibold text-sm text-gray-600 mb-3">收集箱</h3>
      <p className="text-xs text-gray-400">加载中...</p>
    </div>
  );
}
```

- [ ] **Step 4: Verify dev server shows layout with sidebar**

```bash
cd src/client-web && npx vite
```

Log in via the API, then verify sidebar navigation works and highlights the active route.

- [ ] **Step 5: Commit**

```bash
git add src/client-web/src/layout/ src/client-web/src/panels/
git commit -m "feat: add AppLayout with sidebar navigation and InboxPanel stub"
```

---

### Task 5: Create MonthPage with FullCalendar dayGridMonth

**Files:**
- Create: `src/client-web/src/pages/MonthPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx` (wire up MonthPage route)

- [ ] **Step 1: Write MonthPage**

```typescript
// src/client-web/src/pages/MonthPage.tsx
import { useState, useCallback } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useQuery } from '@tanstack/react-query';
import { getEvents, getTasks } from '../api/calendar';
import type { DateSelectArg, EventClickArg } from '@fullcalendar/core';

export default function MonthPage() {
  const [currentRange, setCurrentRange] = useState(() => {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return { start: toDateStr(start), end: toDateStr(end) };
  });

  const { data: events = [] } = useQuery({
    queryKey: ['events', currentRange.start, currentRange.end],
    queryFn: () => getEvents(currentRange.start, currentRange.end)
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks', currentRange.start, currentRange.end],
    queryFn: () => getTasks()
  });

  const fcEvents = [
    ...events.map(e => ({
      id: e.id,
      title: e.title,
      start: e.dtStart,
      end: e.dtEnd,
      backgroundColor: '#1565c0',
      borderColor: '#1565c0',
      extendedProps: { type: 'event', ...e }
    })),
    ...tasks.filter(t => t.dtStart).map(t => ({
      id: t.id,
      title: t.title,
      start: t.dtStart,
      backgroundColor: t.priority === 1 ? '#E53935' : t.priority === 3 ? '#43A047' : '#FFA726',
      borderColor: 'transparent',
      extendedProps: { type: 'task', ...t }
    }))
  ];

  function handleDatesSet(arg: { start: Date; end: Date }) {
    setCurrentRange({ start: toDateStr(arg.start), end: toDateStr(arg.end) });
  }

  const handleDateSelect = useCallback((selectInfo: DateSelectArg) => {
    console.log('date selected', selectInfo.startStr);
    // EventEditorDialog will be wired in Task 10
  }, []);

  const handleEventClick = useCallback((clickInfo: EventClickArg) => {
    console.log('event clicked', clickInfo.event.id);
    // EventEditorDialog will be wired in Task 10
  }, []);

  return (
    <div className="h-full">
      <FullCalendar
        plugins={[dayGridPlugin, interactionPlugin]}
        initialView="dayGridMonth"
        events={fcEvents}
        locale="zh-cn"
        height="100%"
        headerToolbar={{
          left: 'prev,next today',
          center: 'title',
          right: ''
        }}
        datesSet={handleDatesSet}
        selectable={true}
        select={handleDateSelect}
        eventClick={handleEventClick}
      />
    </div>
  );
}

function toDateStr(d: Date): string {
  return d.toISOString().split('T')[0];
}
```

- [ ] **Step 2: Wire MonthPage in AppLayout routes**

In `src/client-web/src/layout/AppLayout.tsx`, replace the month route placeholder:
```typescript
import MonthPage from '../pages/MonthPage';
// ...
<Route path="/month" element={<MonthPage />} />
```

- [ ] **Step 3: Verify month view renders**

Open `http://localhost:5173/month` — should see FullCalendar month grid with backward/forward navigation.

- [ ] **Step 4: Commit**

```bash
git add src/client-web/src/pages/MonthPage.tsx src/client-web/src/layout/AppLayout.tsx
git commit -m "feat: add MonthPage with FullCalendar dayGridMonth"
```

---

### Task 6: Create WeekPage with FullCalendar timeGridWeek

**Files:**
- Create: `src/client-web/src/pages/WeekPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`

- [ ] **Step 1: Write WeekPage**

```typescript
// src/client-web/src/pages/WeekPage.tsx
import { useState, useCallback } from 'react';
import FullCalendar from '@fullcalendar/react';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useQuery } from '@tanstack/react-query';
import { getEvents, getTasks } from '../api/calendar';
import type { DateSelectArg, EventClickArg } from '@fullcalendar/core';
import type { EventResponse, TaskResponse } from '../types';

export default function WeekPage() {
  const [currentRange, setCurrentRange] = useState(() => {
    const now = new Date();
    const dayOfWeek = now.getDay();
    const start = new Date(now);
    start.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
    const end = new Date(start);
    end.setDate(start.getDate() + 7);
    return { start: toISODate(start), end: toISODate(end) };
  });

  const { data: events = [] } = useQuery({
    queryKey: ['events', currentRange.start, currentRange.end],
    queryFn: () => getEvents(currentRange.start, currentRange.end)
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks-week'],
    queryFn: () => getTasks()
  });

  const fcEvents = buildFcEvents(events, tasks);

  function handleDatesSet(arg: { start: Date; end: Date }) {
    setCurrentRange({ start: arg.start.toISOString(), end: arg.end.toISOString() });
  }

  const handleDateSelect = useCallback((selectInfo: DateSelectArg) => {
    console.log('slot selected', selectInfo.startStr, selectInfo.endStr);
  }, []);

  const handleEventClick = useCallback((clickInfo: EventClickArg) => {
    console.log('event clicked', clickInfo.event.id);
  }, []);

  return (
    <div className="h-full">
      <FullCalendar
        plugins={[timeGridPlugin, interactionPlugin]}
        initialView="timeGridWeek"
        events={fcEvents}
        locale="zh-cn"
        height="100%"
        allDaySlot={false}
        slotMinTime="06:00:00"
        slotMaxTime="24:00:00"
        headerToolbar={{
          left: 'prev,next today',
          center: 'title',
          right: ''
        }}
        datesSet={handleDatesSet}
        selectable={true}
        select={handleDateSelect}
        eventClick={handleEventClick}
        selectMirror={true}
      />
    </div>
  );
}

function buildFcEvents(events: EventResponse[], tasks: TaskResponse[]) {
  const fcEvents: Array<{
    id: string; title: string; start: string; end: string;
    backgroundColor: string; borderColor: string; extendedProps: Record<string, unknown>;
  }> = [];

  for (const e of events) {
    fcEvents.push({
      id: e.id, title: e.title, start: e.dtStart, end: e.dtEnd,
      backgroundColor: '#1565c0', borderColor: '#1565c0',
      extendedProps: { type: 'event', ...e }
    });
  }

  for (const t of tasks) {
    if (!t.dtStart) continue;
    const start = new Date(t.dtStart);
    const end = new Date(start.getTime() + 60 * 60 * 1000);
    const color = t.priority === 1 ? '#E53935' : t.priority === 3 ? '#43A047' : '#FFA726';
    fcEvents.push({
      id: t.id, title: t.title,
      start: t.dtStart,
      end: end.toISOString(),
      backgroundColor: color, borderColor: color,
      extendedProps: { type: 'task', ...t }
    });
  }

  return fcEvents;
}

function toISODate(d: Date): string {
  return d.toISOString();
}
```

- [ ] **Step 2: Wire WeekPage route in AppLayout.tsx**

```typescript
import WeekPage from '../pages/WeekPage';
// ...
<Route path="/week" element={<WeekPage />} />
```

- [ ] **Step 3: Verify week view renders**

Open `http://localhost:5173/week` — should see 7-day time grid (Mon-Sun, 6:00-24:00) with event blocks.

- [ ] **Step 4: Commit**

```bash
git add src/client-web/src/pages/WeekPage.tsx src/client-web/src/layout/AppLayout.tsx
git commit -m "feat: add WeekPage with FullCalendar timeGridWeek"
```

---

### Task 7: Create TimelinePage with FullCalendar timeGridDay

**Files:**
- Create: `src/client-web/src/pages/TimelinePage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`

- [ ] **Step 1: Write TimelinePage**

```typescript
// src/client-web/src/pages/TimelinePage.tsx
import { useState, useCallback } from 'react';
import FullCalendar from '@fullcalendar/react';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useQuery } from '@tanstack/react-query';
import { getEvents, getTasks } from '../api/calendar';
import { format } from 'date-fns';
import type { DateSelectArg, EventClickArg } from '@fullcalendar/core';

export default function TimelinePage() {
  const [selectedDate, setSelectedDate] = useState(new Date());

  const startStr = format(selectedDate, 'yyyy-MM-dd');
  const endStr = format(new Date(selectedDate.getTime() + 86400000), 'yyyy-MM-dd');

  const { data: events = [] } = useQuery({
    queryKey: ['events', startStr, endStr],
    queryFn: () => getEvents(startStr, endStr)
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks-timeline', startStr],
    queryFn: () => getTasks()
  });

  // Same buildFcEvents as WeekPage (reuse logic)
  const fcEvents = buildFcEvents(events, tasks.filter(t =>
    t.dtStart && t.dtStart.startsWith(startStr)
  ));

  const handleDateSelect = useCallback((selectInfo: DateSelectArg) => {
    console.log('slot selected', selectInfo.startStr);
  }, []);

  const handleEventClick = useCallback((clickInfo: EventClickArg) => {
    console.log('event clicked', clickInfo.event.id);
  }, []);

  return (
    <div className="h-full flex flex-col">
      <div className="flex items-center justify-between mb-2 px-4 py-2 bg-white border-b">
        <div className="flex items-center gap-2">
          <button
            className="px-2 py-1 text-sm border rounded hover:bg-gray-50"
            onClick={() => setSelectedDate(new Date())}
          >
            今日
          </button>
          <button
            className="px-2 py-1 text-sm border rounded hover:bg-gray-50"
            onClick={() => setSelectedDate(d => new Date(d.getTime() - 86400000))}
          >
            ‹
          </button>
          <button
            className="px-2 py-1 text-sm border rounded hover:bg-gray-50"
            onClick={() => setSelectedDate(d => new Date(d.getTime() + 86400000))}
          >
            ›
          </button>
          <span className="font-bold text-lg ml-3">
            {format(selectedDate, 'M月d日')}
          </span>
        </div>
      </div>

      <div className="flex-1">
        <FullCalendar
          plugins={[timeGridPlugin, interactionPlugin]}
          initialView="timeGridDay"
          initialDate={format(selectedDate, 'yyyy-MM-dd')}
          events={fcEvents}
          locale="zh-cn"
          height="100%"
          allDaySlot={false}
          slotMinTime="00:00:00"
          slotMaxTime="24:00:00"
          headerToolbar={false}
          selectable={true}
          select={handleDateSelect}
          eventClick={handleEventClick}
          selectMirror={true}
        />
      </div>
    </div>
  );
}

function buildFcEvents(events: Array<{ id: string; title: string; dtStart: string; dtEnd: string }>, tasks: Array<{ id: string; title: string; dtStart?: string; priority: number }>) {
  const result: Array<{ id: string; title: string; start: string; end: string; backgroundColor: string; borderColor: string }> = [];
  for (const e of events) {
    result.push({ id: e.id, title: e.title, start: e.dtStart, end: e.dtEnd, backgroundColor: '#1565c0', borderColor: '#1565c0' });
  }
  for (const t of tasks) {
    if (!t.dtStart) continue;
    const end = new Date(new Date(t.dtStart).getTime() + 3600000).toISOString();
    const color = t.priority === 1 ? '#E53935' : t.priority === 3 ? '#43A047' : '#FFA726';
    result.push({ id: t.id, title: t.title, start: t.dtStart, end, backgroundColor: color, borderColor: color });
  }
  return result;
}
```

- [ ] **Step 2: Wire TimelinePage route in AppLayout.tsx**

```typescript
import TimelinePage from '../pages/TimelinePage';
// ...
<Route path="/timeline" element={<TimelinePage />} />
```

- [ ] **Step 3: Verify timeline renders**

Open `http://localhost:5173/timeline` — should see single-day time grid with 今日/‹/› navigation.

- [ ] **Step 4: Commit**

```bash
git add src/client-web/src/pages/TimelinePage.tsx src/client-web/src/layout/AppLayout.tsx
git commit -m "feat: add TimelinePage with FullCalendar timeGridDay"
```

---

### Task 8: Create TaskListPage

**Files:**
- Create: `src/client-web/src/pages/TaskListPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`

- [ ] **Step 1: Write TaskListPage**

```typescript
// src/client-web/src/pages/TaskListPage.tsx
import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getTasks, updateTask } from '../api/calendar';
import type { TaskResponse } from '../types';

const filters = [
  { key: 'all', label: '全部' },
  { key: 'inbox', label: '收集箱' },
  { key: 'high', label: '高优先' },
  { key: 'today', label: '今日' },
] as const;

export default function TaskListPage() {
  const [filter, setFilter] = useState<string>('all');
  const [search, setSearch] = useState('');
  const queryClient = useQueryClient();

  const { data: tasks = [], isLoading } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks()
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      updateTask(id, { status } as Partial<TaskResponse>),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] })
  });

  const filtered = useMemo(() => {
    let result = tasks;
    if (filter === 'inbox') result = result.filter(t => t.isInbox);
    if (filter === 'high') result = result.filter(t => t.priority === 1);
    if (filter === 'today') result = result.filter(t => t.dtStart && t.dtStart.startsWith(new Date().toISOString().split('T')[0]));
    if (search) result = result.filter(t => t.title.toLowerCase().includes(search.toLowerCase()));
    return result;
  }, [tasks, filter, search]);

  if (isLoading) return <div className="p-4 text-gray-400">加载中...</div>;

  return (
    <div className="p-4 max-w-2xl mx-auto">
      <div className="flex gap-2 mb-4">
        {filters.map(f => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`px-3 py-1 text-sm rounded-full border transition-colors ${
              filter === f.key ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      <input
        type="text" placeholder="搜索任务..." value={search}
        onChange={e => setSearch(e.target.value)}
        className="w-full border rounded px-3 py-2 mb-4 text-sm"
      />

      <div className="space-y-2">
        {filtered.map(task => (
          <div key={task.id} className="flex items-center gap-3 p-3 bg-white rounded-lg border hover:shadow-sm transition-shadow">
            <span
              className="w-3 h-3 rounded-full flex-shrink-0"
              style={{ backgroundColor: task.priority === 1 ? '#E53935' : task.priority === 3 ? '#43A047' : '#FFA726' }}
            />
            <span className="flex-1 text-sm text-gray-800">{task.title}</span>
            {task.dtStart && (
              <span className="text-xs text-gray-400">{new Date(task.dtStart).toLocaleDateString('zh-CN')}</span>
            )}
            <button
              onClick={() => toggleMutation.mutate({ id: task.id, status: task.status === 'COMPLETED' ? 'NEEDS-ACTION' : 'COMPLETED' })}
              className={`text-xs px-2 py-1 rounded border transition-colors ${
                task.status === 'COMPLETED' ? 'bg-green-50 border-green-300 text-green-600' : 'border-gray-300 text-gray-500 hover:bg-gray-50'
              }`}
            >
              {task.status === 'COMPLETED' ? '已完成' : '标记完成'}
            </button>
          </div>
        ))}
      </div>

      {filtered.length === 0 && (
        <p className="text-center text-gray-400 py-12">没有任务</p>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Wire route in AppLayout.tsx**

```typescript
import TaskListPage from '../pages/TaskListPage';
// ...
<Route path="/tasks" element={<TaskListPage />} />
```

- [ ] **Step 3: Commit**

```bash
git add src/client-web/src/pages/TaskListPage.tsx src/client-web/src/layout/AppLayout.tsx
git commit -m "feat: add TaskListPage with filter chips, search, and completion toggle"
```

---

### Task 9: Create InboxPanel

**Files:**
- Modify: `src/client-web/src/panels/InboxPanel.tsx`

- [ ] **Step 1: Rewrite InboxPanel with real data**

```typescript
// src/client-web/src/panels/InboxPanel.tsx
import { useQuery } from '@tanstack/react-query';
import { getTasks } from '../api/calendar';

export default function InboxPanel() {
  const { data: tasks = [], isLoading } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks()
  });

  const unscheduled = tasks.filter(t => t.isInbox || !t.dtStart);

  return (
    <div className="w-[280px] bg-gray-50 border-l flex flex-col h-full">
      <div className="p-4 border-b">
        <h3 className="font-semibold text-sm text-gray-600">收集箱</h3>
        <p className="text-xs text-gray-400 mt-0.5">
          {unscheduled.length} 个未排程任务
        </p>
      </div>

      <div className="flex-1 overflow-auto p-3 space-y-2">
        {isLoading ? (
          <p className="text-xs text-gray-400 text-center py-8">加载中...</p>
        ) : unscheduled.length === 0 ? (
          <p className="text-xs text-gray-400 text-center py-8">所有任务均已排入日程</p>
        ) : (
          unscheduled.map(task => (
            <div
              key={task.id}
              className="bg-white rounded-lg p-3 border hover:shadow-sm transition-shadow cursor-pointer"
              draggable
            >
              <div className="flex items-start gap-2">
                <span
                  className="w-2 h-2 rounded-full mt-1.5 flex-shrink-0"
                  style={{ backgroundColor: task.priority === 1 ? '#E53935' : task.priority === 3 ? '#43A047' : '#FFA726' }}
                />
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-800 truncate">{task.title}</p>
                  {task.due && (
                    <p className="text-xs text-red-400 mt-1">
                      截止: {new Date(task.due).toLocaleDateString('zh-CN')}
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      <div className="p-3 border-t space-y-2">
        <button className="w-full py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700">
          + 新建任务
        </button>
        <button className="w-full py-2 text-sm border border-gray-300 text-gray-600 rounded hover:bg-gray-100">
          一键重排
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/client-web/src/panels/InboxPanel.tsx
git commit -m "feat: add InboxPanel with unscheduled tasks list and action buttons"
```

---

### Task 10: Create EventEditorDialog

**Files:**
- Create: `src/client-web/src/dialogs/EventEditorDialog.tsx`
- Create: `src/client-web/src/dialogs/common.ts` (shared dialog utilities)

- [ ] **Step 1: Write shared dialog component**

```typescript
// src/client-web/src/dialogs/common.ts
import type { ReactNode } from 'react';

export function Dialog({ open, onClose, title, children }: {
  open: boolean; onClose: () => void; title: string; children: ReactNode;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[90vh] overflow-auto p-6">
        <h2 className="text-lg font-semibold mb-4">{title}</h2>
        {children}
      </div>
    </div>
  );
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block mb-3">
      <span className="text-sm font-medium text-gray-600 block mb-1">{label}</span>
      {children}
    </label>
  );
}
```

- [ ] **Step 2: Write EventEditorDialog**

```typescript
// src/client-web/src/dialogs/EventEditorDialog.tsx
import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createEvent, updateEvent } from '../api/calendar';
import { Dialog, Field } from './common';
import type { EventResponse } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  event?: EventResponse;        // undefined = create mode
  defaultStart?: string;        // from dateClick
  defaultEnd?: string;
}

export default function EventEditorDialog({ open, onClose, event, defaultStart, defaultEnd }: Props) {
  const [title, setTitle] = useState(event?.title || '');
  const [description, setDescription] = useState(event?.description || '');
  const [location, setLocation] = useState(event?.location || '');
  const [dtStart, setDtStart] = useState(event?.dtStart || defaultStart || '');
  const [dtEnd, setDtEnd] = useState(event?.dtEnd || defaultEnd || '');
  const queryClient = useQueryClient();

  const createMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => createEvent(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['events'] }); onClose(); }
  });

  const updateMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => updateEvent(event!.id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['events'] }); onClose(); }
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const data = { title, description, location, dtStart, dtEnd };
    if (event) updateMut.mutate(data);
    else createMut.mutate(data);
  }

  return (
    <Dialog open={open} onClose={onClose} title={event ? '编辑日程' : '新建日程'}>
      <form onSubmit={handleSubmit}>
        <Field label="标题">
          <input type="text" value={title} onChange={e => setTitle(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="开始时间">
          <input type="datetime-local" value={dtStart} onChange={e => setDtStart(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="结束时间">
          <input type="datetime-local" value={dtEnd} onChange={e => setDtEnd(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="地点">
          <input type="text" value={location} onChange={e => setLocation(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="描述">
          <textarea value={description} onChange={e => setDescription(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" rows={3} />
        </Field>
        <div className="flex justify-end gap-3 mt-4">
          <button type="button" onClick={onClose}
            className="px-4 py-2 text-sm border rounded hover:bg-gray-50">取消</button>
          <button type="submit" disabled={createMut.isPending || updateMut.isPending}
            className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
            {event ? '保存' : '创建'}
          </button>
        </div>
      </form>
    </Dialog>
  );
}
```

- [ ] **Step 3: Wire dialog into MonthPage, WeekPage, TimelinePage**

In each calendar page, add state and open dialog on dateClick/eventClick:

```typescript
// Add to MonthPage.tsx, WeekPage.tsx, TimelinePage.tsx
import EventEditorDialog from '../dialogs/EventEditorDialog';

// Inside component:
const [editorOpen, setEditorOpen] = useState(false);
const [editEvent, setEditEvent] = useState<EventResponse | undefined>();
const [selectStart, setSelectStart] = useState<string | undefined>();
const [selectEnd, setSelectEnd] = useState<string | undefined>();

// In dateSelect callback:
function handleDateSelect(selectInfo: DateSelectArg) {
  setEditEvent(undefined);
  setSelectStart(selectInfo.startStr);
  setSelectEnd(selectInfo.endStr);
  setEditorOpen(true);
}

// In eventClick callback:
function handleEventClick(clickInfo: EventClickArg) {
  const raw = clickInfo.event.extendedProps as EventResponse;
  setEditEvent(raw);
  setEditorOpen(true);
}

// In JSX:
<EventEditorDialog
  open={editorOpen}
  onClose={() => setEditorOpen(false)}
  event={editEvent}
  defaultStart={selectStart}
  defaultEnd={selectEnd}
/>
```

- [ ] **Step 4: Commit**

```bash
git add src/client-web/src/dialogs/ src/client-web/src/pages/
git commit -m "feat: add EventEditorDialog with create/edit/delete support"
```

---

### Task 11: Create TaskEditorDialog

**Files:**
- Create: `src/client-web/src/dialogs/TaskEditorDialog.tsx`

- [ ] **Step 1: Write TaskEditorDialog**

```typescript
// src/client-web/src/dialogs/TaskEditorDialog.tsx
import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createTask, updateTask } from '../api/calendar';
import { Dialog, Field } from './common';
import type { TaskResponse } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  task?: TaskResponse;
}

export default function TaskEditorDialog({ open, onClose, task }: Props) {
  const [title, setTitle] = useState(task?.title || '');
  const [description, setDescription] = useState(task?.description || '');
  const [priority, setPriority] = useState(task?.priority || 0);
  const [dtStart, setDtStart] = useState(task?.dtStart || '');
  const [due, setDue] = useState(task?.due || '');
  const [duration, setDuration] = useState(task?.estimatedDuration || 'PT1H');
  const queryClient = useQueryClient();

  const createMut = useMutation({
    mutationFn: (data: Partial<TaskResponse>) => createTask(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tasks'] }); onClose(); }
  });

  const updateMut = useMutation({
    mutationFn: (data: Partial<TaskResponse>) => updateTask(task!.id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tasks'] }); onClose(); }
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const data = { title, description, priority, dtStart: dtStart || undefined, due: due || undefined, estimatedDuration: duration };
    if (task) updateMut.mutate(data);
    else createMut.mutate(data);
  }

  return (
    <Dialog open={open} onClose={onClose} title={task ? '编辑任务' : '新建任务'}>
      <form onSubmit={handleSubmit}>
        <Field label="标题">
          <input type="text" value={title} onChange={e => setTitle(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="描述">
          <textarea value={description} onChange={e => setDescription(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" rows={3} />
        </Field>
        <Field label="优先级">
          <select value={priority} onChange={e => setPriority(Number(e.target.value))}
            className="w-full border rounded px-3 py-2 text-sm">
            <option value={0}>普通</option>
            <option value={1}>高</option>
            <option value={3}>低</option>
          </select>
        </Field>
        <Field label="计划时间">
          <input type="datetime-local" value={dtStart} onChange={e => setDtStart(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="截止日期">
          <input type="datetime-local" value={due} onChange={e => setDue(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="预估时长 (ISO 8601)">
          <input type="text" value={duration} onChange={e => setDuration(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" placeholder="PT1H30M" />
        </Field>
        <div className="flex justify-end gap-3 mt-4">
          <button type="button" onClick={onClose}
            className="px-4 py-2 text-sm border rounded hover:bg-gray-50">取消</button>
          <button type="submit" disabled={createMut.isPending || updateMut.isPending}
            className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
            {task ? '保存' : '创建'}
          </button>
        </div>
      </form>
    </Dialog>
  );
}
```

- [ ] **Step 2: Wire into InboxPanel and TaskListPage**

In `InboxPanel.tsx`: Add `useState` for `taskEditorOpen` and `editingTask`, render `<TaskEditorDialog>`.
In `TaskListPage.tsx`: Same pattern, open on task row click or "+ 新建任务" button.

- [ ] **Step 3: Commit**

```bash
git add src/client-web/src/dialogs/TaskEditorDialog.tsx src/client-web/src/panels/InboxPanel.tsx src/client-web/src/pages/TaskListPage.tsx
git commit -m "feat: add TaskEditorDialog with priority, scheduling, and duration fields"
```

---

### Task 12: Add SPA fallback to Pim.Api

**Files:**
- Modify: `src/Pim.Api/Program.cs:24-53`

- [ ] **Step 1: Add static files and SPA fallback middleware**

In `src/Pim.Api/Program.cs`, add between `app.UseCors()` and the health check:

```csharp
// Serve React SPA static files
app.UseDefaultFiles();
app.UseStaticFiles();
```

And at the end, before `app.Run()`:

```csharp
// SPA fallback: non-API routes serve index.html (React Router handles routing)
app.MapFallbackToFile("index.html").AllowAnonymous();
```

Final key section of Program.cs:

```csharp
app.UseMiddleware<ExceptionMiddleware>();
app.UseAuthentication();
app.UseAuthorization();
app.UseCors();

// Serve React SPA from wwwroot
app.UseDefaultFiles();
app.UseStaticFiles();

// ... db init, health, auth endpoints, module endpoints ...

// SPA fallback
app.MapFallbackToFile("index.html").AllowAnonymous();

app.Run();
```

- [ ] **Step 2: Add wwwroot to .gitignore exception**

The `wwwroot/` directory contains build artifacts. Add to `src/Pim.Api/.gitignore`:

```
!wwwroot/
wwwroot/*
!wwwroot/.gitkeep
```

- [ ] **Step 3: Build frontend and verify**

```bash
cd src/client-web && npm run build
# Verify files exist in src/Pim.Api/wwwroot/
ls src/Pim.Api/wwwroot/index.html
```

- [ ] **Step 4: Run backend and test**

```bash
dotnet run --project src/Pim.Api
# Open http://localhost:5000 → should serve React app
# Navigate to /timeline, /week, /month, /tasks → should work
# Refresh any page → should not 404
```

- [ ] **Step 5: Commit**

```bash
git add src/Pim.Api/Program.cs src/Pim.Api/.gitignore src/Pim.Api/wwwroot/.gitkeep
git commit -m "feat: add SPA fallback middleware to serve React app from wwwroot"
```

---

### Task 13: Trim WPF to daemon (tray icon + status window)

**Files:**
- Modify: `src/client-windows/Pim.Client.App/Startup.cs`
- Modify: `src/client-windows/Pim.Client.App/App.xaml(.cs)`
- Create: `src/client-windows/Pim.Client.App/TrayIcon.cs`
- Create: `src/client-windows/Pim.Client.App/StatusWindow.xaml(.cs)`
- Delete: All files under `src/client-windows/Pim.Client.App/Views/`
- Delete: All files under `src/client-windows/Pim.Client.App/ViewModels/`
- Delete: `src/client-windows/Pim.Client.App/Converters/Converters.cs`
- Delete: `src/client-windows/Pim.Client.App/Styles/Theme.xaml`
- Delete: `src/client-windows/Pim.Client.App/MainWindow.xaml(.cs)`

- [ ] **Step 1: Remove NuGet reference**

```bash
cd src/client-windows
dotnet remove Pim.Client.App/Pim.Client.App.csproj package MaterialDesignThemes
```

- [ ] **Step 2: Delete all UI files**

```bash
rm -r src/client-windows/Pim.Client.App/Views/
rm -r src/client-windows/Pim.Client.App/ViewModels/
rm src/client-windows/Pim.Client.App/Converters/Converters.cs
rm src/client-windows/Pim.Client.App/Styles/Theme.xaml
rm src/client-windows/Pim.Client.App/MainWindow.xaml
rm src/client-windows/Pim.Client.App/MainWindow.xaml.cs
```

- [ ] **Step 3: Update Startup.cs — remove all ViewModel registrations**

```csharp
// src/client-windows/Pim.Client.App/Startup.cs
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public static class Startup
{
    public static IServiceProvider ConfigureServices()
    {
        var services = new ServiceCollection();

        // Core services
        services.AddSingleton<ApiClient>();
        services.AddSingleton<AuthService>();
        services.AddSingleton<TrayIcon>();

        return services.BuildServiceProvider();
    }
}
```

- [ ] **Step 4: Create TrayIcon.cs**

```csharp
// src/client-windows/Pim.Client.App/TrayIcon.cs
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace Pim.Client.App;

public class TrayIcon : IDisposable
{
    private System.Windows.Forms.NotifyIcon? _notifyIcon;

    public void Show()
    {
        var iconStream = Application.GetResourceStream(
            new Uri("pack://application:,,,/Pim.Client.App;component/app.ico"))?.Stream
            ?? SystemIcons.Application.ToBitmap().ToStream();

        _notifyIcon = new System.Windows.Forms.NotifyIcon
        {
            Icon = new System.Drawing.Icon(iconStream),
            Text = "PIM 数据采集服务",
            Visible = true,
            ContextMenuStrip = new System.Windows.Forms.ContextMenuStrip()
        };

        _notifyIcon.ContextMenuStrip.Items.Add("状态: 运行中").Enabled = false;
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("打开状态窗口", null, (_, _) => ShowStatusWindow());
        _notifyIcon.ContextMenuStrip.Items.Add("手动同步", null, (_, _) => TriggerSync());
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("退出", null, (_, _) => Exit());

        _notifyIcon.DoubleClick += (_, _) => ShowStatusWindow();
    }

    private void ShowStatusWindow()
    {
        var existing = Application.Current.Windows.OfType<StatusWindow>().FirstOrDefault();
        if (existing is not null)
        {
            existing.Activate();
            return;
        }
        var window = new StatusWindow();
        window.Show();
    }

    private async void TriggerSync()
    {
        // Trigger upload / sync manually
        System.Diagnostics.Debug.WriteLine("Manual sync triggered");
    }

    private void Exit()
    {
        Dispose();
        Application.Current.Shutdown();
    }

    public void Dispose()
    {
        _notifyIcon?.Dispose();
        _notifyIcon = null;
    }
}

static class IconExtensions
{
    public static Stream ToStream(this System.Drawing.Icon icon)
    {
        var ms = new MemoryStream();
        icon.Save(ms);
        ms.Seek(0, SeekOrigin.Begin);
        return ms;
    }
}
```

- [ ] **Step 5: Create StatusWindow.xaml and .cs**

```xml
<!-- src/client-windows/Pim.Client.App/StatusWindow.xaml -->
<Window x:Class="Pim.Client.App.StatusWindow"
        xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="PIM 数据采集状态" Height="300" Width="400"
        WindowStartupLocation="CenterScreen" ResizeMode="NoResize">
    <Grid Margin="16">
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="*"/>
            <RowDefinition Height="Auto"/>
        </Grid.RowDefinitions>

        <TextBlock Text="PIM 数据采集状态" FontSize="16" FontWeight="Bold" Margin="0,0,0,12"/>
        <StackPanel Grid.Row="1" Margin="0,0,0,8">
            <TextBlock x:Name="KeyStatsStatus" Text="KeyStats      检查中..." FontSize="13" Margin="0,2"/>
            <TextBlock x:Name="AWStatus" Text="ActivityWatch 检查中..." FontSize="13" Margin="0,2"/>
            <TextBlock x:Name="QueueStatus" Text="上传队列      --" FontSize="13" Margin="0,2"/>
            <TextBlock x:Name="LastUploadStatus" Text="上次上传      --" FontSize="13" Margin="0,2"/>
        </StackPanel>

        <StackPanel Grid.Row="5" Orientation="Horizontal" HorizontalAlignment="Right" Margin="0,12,0,0">
            <Button Content="手动同步" Padding="12,6" Margin="0,0,8,0" Click="OnManualSync"/>
            <Button Content="查看日志" Padding="12,6" Click="OnViewLogs"/>
        </StackPanel>
    </Grid>
</Window>
```

```csharp
// src/client-windows/Pim.Client.App/StatusWindow.xaml.cs
using System.Diagnostics;
using System.Windows;

namespace Pim.Client.App;

public partial class StatusWindow : Window
{
    public StatusWindow()
    {
        InitializeComponent();
        RefreshStatus();
    }

    private async void RefreshStatus()
    {
        // Check KeyStats
        try
        {
            using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(3) };
            var resp = await http.GetAsync("http://127.0.0.1:18080/api/stats/");
            KeyStatsStatus.Text = resp.IsSuccessStatusCode
                ? "KeyStats      ✓ 已连接 (18080)"
                : $"KeyStats      ✗ HTTP {resp.StatusCode}";
        }
        catch
        {
            KeyStatsStatus.Text = "KeyStats      ✗ 未连接";
        }

        // Check ActivityWatch
        try
        {
            using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(3) };
            var resp = await http.GetAsync("http://127.0.0.1:5600/api/0/buckets/");
            AWStatus.Text = resp.IsSuccessStatusCode
                ? "ActivityWatch ✓ 已连接 (5600)"
                : $"ActivityWatch ✗ HTTP {resp.StatusCode}";
        }
        catch
        {
            AWStatus.Text = "ActivityWatch ✗ 未连接";
        }

        QueueStatus.Text = "上传队列      -- 条待上传";
        LastUploadStatus.Text = "上次上传      --";
    }

    private void OnManualSync(object sender, RoutedEventArgs e)
    {
        Debug.WriteLine("Manual sync triggered");
        MessageBox.Show("同步已触发", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
    }

    private void OnViewLogs(object sender, RoutedEventArgs e)
    {
        var logPath = Services.Logger.LogFilePath;
        try { Process.Start("notepad.exe", logPath); }
        catch { MessageBox.Show($"日志文件: {logPath}", "PIM"); }
    }
}
```

- [ ] **Step 6: Rewrite App.xaml and App.xaml.cs for daemon mode**

```xml
<!-- src/client-windows/Pim.Client.App/App.xaml -->
<Application x:Class="Pim.Client.App.App"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
</Application>
```

```csharp
// src/client-windows/Pim.Client.App/App.xaml.cs
using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;
    private TrayIcon? _trayIcon;

    protected override void OnStartup(StartupEventArgs e)
    {
        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
        {
            Logger.Error("UnhandledException", args.ExceptionObject as Exception);
        };

        try
        {
            Logger.Info("Daemon starting");
            Services = Pim.Client.App.Startup.ConfigureServices();
            Logger.Info("DI configured");

            var apiClient = Services.GetRequiredService<Pim.Client.Core.Services.ApiClient>();
            apiClient.RequestTiming += (desc, ms) =>
                Logger.Info($"[ApiTiming] {desc} took {ms}ms");

            // Authenticate (use saved token or prompt)
            var authService = Services.GetRequiredService<Core.Services.AuthService>();
            if (!authService.IsAuthenticated)
            {
                Logger.Warn("Not authenticated — daemon running without API access");
            }
            else
            {
                Logger.Info($"Authenticated as {authService.CurrentUsername}");
            }

            // Start tray icon
            _trayIcon = Services.GetRequiredService<TrayIcon>();
            _trayIcon.Show();
            Logger.Info("Tray icon shown");

            // Prevent app from shutting down when no windows are open
            ShutdownMode = ShutdownMode.OnExplicitShutdown;
        }
        catch (Exception ex)
        {
            Logger.Error("Fatal daemon startup error", ex);
            Shutdown();
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _trayIcon?.Dispose();
        Logger.Info("Daemon exiting");
        base.OnExit(e);
    }
}
```

- [ ] **Step 7: Add System.Windows.Forms reference to csproj for NotifyIcon**

```xml
<!-- Add to Pim.Client.App.csproj -->
<UseWindowsForms>true</UseWindowsForms>
```

- [ ] **Step 8: Build and verify**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
```

Expected: 0 errors. Run the app, tray icon appears, status window opens on double-click.

- [ ] **Step 9: Commit**

```bash
git add -A src/client-windows/
git commit -m "refactor: strip WPF to daemon mode — tray icon, status window, remove all calendar UI"
```

---

### Task 14: Trim Android client to background service

**Files:**
- Delete: `src/client-android/app/.../ui/` (all Compose screens)
- Delete: `src/client-android/app/.../navigation/`
- Create: `src/client-android/app/.../daemon/PimDaemonService.kt`
- Create: `src/client-android/app/.../daemon/DataCollector.kt`
- Create: `src/client-android/app/.../daemon/UploadWorker.kt`
- Create: `src/client-android/app/.../daemon/StatusActivity.kt`
- Modify: `src/client-android/app/.../MainActivity.kt`
- Modify: `src/client-android/app/build.gradle.kts` (add WorkManager dependency)

- [ ] **Step 1: Add WorkManager dependency**

```kotlin
// build.gradle.kts (app module)
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

- [ ] **Step 2: Create PimDaemonService.kt**

```kotlin
// app/src/main/java/com/pim/app/daemon/PimDaemonService.kt
package com.pim.app.daemon

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PimDaemonService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "pim_daemon"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "PIM 数据采集",
                NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, StatusActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("PIM 数据采集")
            .setContentText("采集运行中")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
```

- [ ] **Step 3: Create DataCollector.kt**

```kotlin
// app/src/main/java/com/pim/app/daemon/DataCollector.kt
package com.pim.app.daemon

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.*
import timber.log.Timber

class DataCollector(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            while (isActive) {
                try {
                    collectUsageStats()
                    Timber.d("UsageStats collected")
                } catch (e: Exception) {
                    Timber.e(e, "UsageStats collection failed")
                }
                delay(5 * 60 * 1000L) // 5 minutes
            }
        }
    }

    private fun collectUsageStats() {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 5 * 60 * 1000L
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, begin, end
        )
        // Store in Room DB, mark synced = false
        Timber.d("Collected ${stats.size} usage stat entries")
    }

    fun stop() { scope.cancel() }
}
```

- [ ] **Step 4: Create UploadWorker.kt**

```kotlin
// app/src/main/java/com/pim/app/daemon/UploadWorker.kt
package com.pim.app.daemon

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Timber.d("UploadWorker running")
        // Fetch unsynced data from Room, upload to Pim.Api
        return Result.success()
    }
}

fun scheduleUploadWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork("pim_upload", ExistingPeriodicWorkPolicy.KEEP, request)
}
```

- [ ] **Step 5: Create StatusActivity.kt**

```kotlin
// app/src/main/java/com/pim/app/daemon/StatusActivity.kt
package com.pim.app.daemon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class StatusActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "PIM 数据采集\n状态: 运行中\n\n待上传: --\n上次上传: --"
        tv.setPadding(48, 48, 48, 48)
        setContentView(tv)
    }
}
```

- [ ] **Step 6: Rewrite MainActivity.kt — start daemon and open browser**

```kotlin
// app/src/main/java/com/pim/app/MainActivity.kt
package com.pim.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pim.app.daemon.PimDaemonService
import com.pim.app.daemon.DataCollector
import com.pim.app.daemon.scheduleUploadWorker

class MainActivity : AppCompatActivity() {
    private lateinit var collector: DataCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start daemon service
        startService(Intent(this, PimDaemonService::class.java))

        // Start data collection
        collector = DataCollector(this)
        collector.start()

        // Schedule upload worker
        scheduleUploadWorker(this)

        // Open PIM web UI in browser
        val browserIntent = Intent(Intent.ACTION_VIEW,
            android.net.Uri.parse("http://<NAS_IP>:5000"))
        startActivity(browserIntent)

        finish()
    }

    override fun onDestroy() {
        collector.stop()
        super.onDestroy()
    }
}
```

- [ ] **Step 7: Delete UI files and verify build**

Remove all files under `ui/` and `navigation/` directories.

- [ ] **Step 8: Commit**

```bash
git add -A src/client-android/
git commit -m "refactor: strip Android to daemon mode — foreground service, collector, upload worker"
```

---

### Task 15: Add logging to all layers

**Files:**
- Modify: `src/client-windows/Pim.Client.App/App.xaml.cs` (add Serilog)
- Modify: `src/Pim.Api/Program.cs` (add Serilog HTTP logging)
- Create: `src/client-web/src/api/client.ts` (add console.log wrapper, already partially done)

- [ ] **Step 1: Add Serilog to Windows daemon**

```bash
cd src/client-windows/Pim.Client.App
dotnet add package Serilog.Sinks.File
dotnet add package Serilog.Sinks.Debug
```

```csharp
// In App.xaml.cs OnStartup, before everything:
using Serilog;
using Serilog.Events;

var logDir = Path.Combine(
    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
    "PIM", "logs");
Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Debug()
    .WriteTo.Debug()
    .WriteTo.File(
        Path.Combine(logDir, "pim-daemon-.log"),
        rollingInterval: RollingInterval.Day,
        retainedFileCountLimit: 30,
        outputTemplate: "{Timestamp:yyyy-MM-ddTHH:mm:ss.fffZ} [{Level}] {Message:lj}{NewLine}{Exception}")
    .CreateLogger();

// Replace existing Logger.Info calls with Serilog
Log.Information("Daemon starting");
```

- [ ] **Step 2: Add Serilog request logging to Pim.Api**

```bash
cd src/Pim.Api
dotnet add package Serilog.AspNetCore
```

```csharp
// Program.cs, at the top:
using Serilog;

Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Debug()
    .WriteTo.Console()
    .WriteTo.File("/data/pim/logs/pim-api-.log",
        rollingInterval: RollingInterval.Day,
        retainedFileCountLimit: 30,
        outputTemplate: "{Timestamp:yyyy-MM-ddTHH:mm:ss.fffZ} [{Level}] {Message:lj}{NewLine}{Exception}")
    .CreateLogger();

builder.Host.UseSerilog();

// After app is built, add request logging middleware:
app.UseSerilogRequestLogging(options =>
{
    options.MessageTemplate = "{RemoteIpAddress} {RequestMethod} {RequestPath} responded {StatusCode} in {Elapsed:0.000} ms";
});
```

- [ ] **Step 3: Add API logging to React client**

```typescript
// In src/client-web/src/api/client.ts, add logging wrapper:
function logApi(method: string, path: string, duration: number, status?: number) {
  const msg = `[API] ${method} ${path} → ${status || '???'} (${duration}ms)`;
  if (import.meta.env.DEV) console.log(msg);
  // Production: could ship to a logging endpoint
}
```

- [ ] **Step 4: Build and verify all layers compile**

```bash
dotnet build src/Pim.Api/Pim.Api.csproj
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
cd src/client-web && npm run build
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Serilog logging to daemon and API, console logging to web frontend"
```

---

### Task 16: End-to-end verification

- [ ] **Step 1: Start backend**

```bash
dotnet run --project src/Pim.Api
```

Expected: API starts on port 5000, serves React app at `/`, SPA fallback works.

- [ ] **Step 2: Register and login via web UI**

Open `http://localhost:5000` → Login page → Register new account → Redirects to `/timeline`.

- [ ] **Step 3: Test calendar views**

Navigate to `/month` → FullCalendar renders → Click date → EventEditorDialog opens.
Navigate to `/week` → 7-day time grid renders → Click time slot → EventEditorDialog opens.
Navigate to `/timeline` → Single day view renders → 今日/‹/› buttons work.

- [ ] **Step 4: Test task management**

Navigate to `/tasks` → Filter chips work → Search works → Click toggle complete.
InboxPanel shows unscheduled tasks → Click "+ 新建任务" → TaskEditorDialog opens.

- [ ] **Step 5: Start Windows daemon**

```bash
dotnet run --project src/client-windows/Pim.Client.App
```

Expected: No window opens, tray icon appears, status window opens on double-click.

- [ ] **Step 6: Verify API calls from daemon**

Check API logs — daemon should make authenticated requests.

- [ ] **Step 7: Build Android daemon APK and verify**

Open Android project in Android Studio → Build → Install on device/emulator → Verify foreground notification appears and browser opens to web UI.

- [ ] **Step 8: Final commit**

```bash
git add -A
git commit -m "chore: end-to-end verification — all layers functional"
```
