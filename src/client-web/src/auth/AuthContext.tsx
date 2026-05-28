import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import { loadTokens, clearTokens, setTokens, onTokensChanged } from '../api/client';
import { authFailureMessage, readAuthResponse } from './authApi';

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
    return () => onTokensChanged(() => {});
  }, []);

  const login = useCallback(async (uname: string, pwd: string): Promise<string | null> => {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: uname, password: pwd })
    });
    const json = await readAuthResponse(res);
    if (!res.ok || json?.code !== 0 || !json.data) return authFailureMessage('login', res, json);
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
    const json = await readAuthResponse(res);
    if (!res.ok || json?.code !== 0 || !json.data) return authFailureMessage('register', res, json);
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
