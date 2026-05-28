import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { jwtDecode } from 'jwt-decode'

interface JwtPayload {
  sub: string
  role: string
  exp: number
}

interface AuthContextValue {
  token: string | null
  login: (token: string) => void
  logout: () => void
  isAuthenticated: boolean
  user: JwtPayload | null
}

const AuthContext = createContext<AuthContextValue | null>(null)

function isTokenValid(token: string): boolean {
  try {
    const { exp } = jwtDecode<JwtPayload>(token)
    return Date.now() < exp * 1000
  } catch {
    return false
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    const stored = localStorage.getItem('token')
    return stored && isTokenValid(stored) ? stored : null
  })

  const user = token ? jwtDecode<JwtPayload>(token) : null

  useEffect(() => {
    if (!token) localStorage.removeItem('token')
  }, [token])

  function login(newToken: string) {
    localStorage.setItem('token', newToken)
    setToken(newToken)
  }

  function logout() {
    localStorage.removeItem('token')
    setToken(null)
  }

  return (
    <AuthContext.Provider value={{ token, login, logout, isAuthenticated: !!token, user }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
