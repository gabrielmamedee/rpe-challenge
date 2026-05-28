import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { Loader2 } from 'lucide-react'
import { useAuth } from '@/contexts/AuthContext'
import { authService } from '@/services/auth.service'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

type Mode = 'login' | 'register'

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState<Mode>('login')
  const [loginVal, setLoginVal] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  function switchMode(next: Mode) {
    setMode(next)
    setLoginVal('')
    setPassword('')
    setConfirmPassword('')
  }

  async function handleLogin(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    try {
      const { token } = await authService.login({ login: loginVal, password })
      login(token)
      navigate('/')
    } catch {
      toast.error('Login ou senha inválidos.')
    } finally {
      setLoading(false)
    }
  }

  async function handleRegister(e: FormEvent) {
    e.preventDefault()
    if (password !== confirmPassword) {
      toast.error('As senhas não coincidem.')
      return
    }
    setLoading(true)
    try {
      await authService.register({ login: loginVal, password })
      toast.success('Conta criada! Entrando automaticamente...')
      const { token } = await authService.login({ login: loginVal, password })
      login(token)
      navigate('/')
    } catch {
      toast.error('Não foi possível criar a conta. Verifique os dados e tente novamente.')
    } finally {
      setLoading(false)
    }
  }

  const isRegister = mode === 'register'

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary to-primary/70 p-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <h1 className="text-4xl font-bold text-primary-foreground tracking-tight">RPE</h1>
          <p className="text-primary-foreground/60 text-sm mt-1">Order Manager</p>
        </div>

        <Card className="shadow-xl border-0">
          <CardHeader className="pb-4">
            <CardTitle className="text-lg">
              {isRegister ? 'Criar conta' : 'Entrar na plataforma'}
            </CardTitle>
            <CardDescription>
              {isRegister
                ? 'Preencha os dados para criar seu acesso'
                : 'Use suas credenciais de acesso'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={isRegister ? handleRegister : handleLogin} className="space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="login">Usuário</Label>
                <Input
                  id="login"
                  placeholder={isRegister ? 'nome_usuario' : 'adminrpe'}
                  value={loginVal}
                  onChange={(e) => setLoginVal(e.target.value)}
                  autoComplete="username"
                  required
                  minLength={isRegister ? 3 : undefined}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="password">Senha</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder={isRegister ? 'Mínimo 8 caracteres' : '••••••••'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                  required
                  minLength={isRegister ? 8 : undefined}
                />
              </div>
              {isRegister && (
                <div className="space-y-1.5">
                  <Label htmlFor="confirm-password">Confirmar senha</Label>
                  <Input
                    id="confirm-password"
                    type="password"
                    placeholder="Repita a senha"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    autoComplete="new-password"
                    required
                    minLength={8}
                    className={
                      confirmPassword && password !== confirmPassword
                        ? 'border-destructive focus-visible:ring-destructive'
                        : ''
                    }
                  />
                  {confirmPassword && password !== confirmPassword && (
                    <p className="text-xs text-destructive">As senhas não coincidem.</p>
                  )}
                </div>
              )}
              <Button type="submit" className="w-full mt-2" disabled={loading}>
                {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                {isRegister ? 'Criar conta' : 'Entrar'}
              </Button>
            </form>

            <div className="mt-4 text-center text-sm text-muted-foreground">
              {isRegister ? (
                <>
                  Já tem uma conta?{' '}
                  <button
                    type="button"
                    onClick={() => switchMode('login')}
                    className="text-primary font-medium hover:underline"
                  >
                    Entrar
                  </button>
                </>
              ) : (
                <>
                  Não tem uma conta?{' '}
                  <button
                    type="button"
                    onClick={() => switchMode('register')}
                    className="text-primary font-medium hover:underline"
                  >
                    Criar agora
                  </button>
                </>
              )}
            </div>
          </CardContent>
        </Card>

        <p className="text-center text-primary-foreground/40 text-xs">
          © {new Date().getFullYear()} RPE. Todos os direitos reservados.
        </p>
      </div>
    </div>
  )
}
