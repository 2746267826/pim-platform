import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import { loadTokens, clearTokens, setTokens, onTokensChanged, apiPost } from '../api/client';
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
    return () => onTokensChanged(() => {});
  }, []);

  const login = useCallback(async (uname: string, pwd: string): Promise<string | null> => {
    try {
      const json = await apiPost<ApiResponse<AuthResponse>>('/auth/login', { username: uname, password: pwd });
      if (json?.code !== 0 || !json.data) return json?.message || '登录失败';
      setTokens(json.data.accessToken, json.data.refreshToken);
      setUsername(json.data.userInfo?.displayName || uname);
      setIsAuth(true);
      return null;
    } catch (err) {
      return err instanceof Error ? err.message : '登录失败';
    }
  }, []);

  const register = useCallback(async (uname: string, email: string, pwd: string, displayName?: string) => {
    try {
      const json = await apiPost<ApiResponse<AuthResponse>>('/auth/register', { username: uname, email, password: pwd, displayName });
      if (json?.code !== 0 || !json.data) return json?.message || '注册失败';
      setTokens(json.data.accessToken, json.data.refreshToken);
      setUsername(json.data.userInfo?.displayName || uname);
      setIsAuth(true);
      return null;
    } catch (err) {
      return err instanceof Error ? err.message : '注册失败';
    }
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
