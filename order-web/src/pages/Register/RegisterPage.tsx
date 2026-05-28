import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { Loader2, CheckCircle2, ArrowLeft } from 'lucide-react'
import { authService } from '@/services/auth.service'
import Header from '@/components/layout/Header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import type { User } from '@/types'

export default function RegisterPage() {
  const [login, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [created, setCreated] = useState<User | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    try {
      const user = await authService.register({ login, password })
      setCreated(user)
      setLogin('')
      setPassword('')
      toast.success(`Usuário "${user.login}" criado com sucesso!`)
    } catch {
      toast.error('Não foi possível criar o usuário. Verifique os dados e tente novamente.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <main className="max-w-lg mx-auto px-4 py-10">
        <Link
          to="/"
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar para Home
        </Link>
        <Card>
          <CardHeader>
            <CardTitle>Cadastrar novo usuário</CardTitle>
            <CardDescription>Crie credenciais de acesso para um novo usuário.</CardDescription>
          </CardHeader>
          <CardContent>
            {created ? (
              <div className="space-y-4">
                <div className="flex items-start gap-3 rounded-lg bg-muted p-4">
                  <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 shrink-0" />
                  <div className="space-y-2 text-sm">
                    <p className="font-semibold">Usuário criado com sucesso!</p>
                    <div className="space-y-1 text-muted-foreground">
                      <p><span className="font-medium text-foreground">ID:</span> {created.id}</p>
                      <p><span className="font-medium text-foreground">Login:</span> {created.login}</p>
                      <p className="flex items-center gap-2">
                        <span className="font-medium text-foreground">Role:</span>
                        <Badge variant="secondary" className="text-xs">{created.role}</Badge>
                      </p>
                    </div>
                  </div>
                </div>
                <div className="flex gap-3">
                  <Button variant="outline" className="flex-1" onClick={() => setCreated(null)}>
                    Criar outro
                  </Button>
                  <Button asChild className="flex-1">
                    <Link to="/">Ir para Home</Link>
                  </Button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="reg-login">Usuário</Label>
                  <Input
                    id="reg-login"
                    placeholder="nome_usuario"
                    value={login}
                    onChange={(e) => setLogin(e.target.value)}
                    required
                    minLength={3}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="reg-password">Senha</Label>
                  <Input
                    id="reg-password"
                    type="password"
                    placeholder="Mínimo 8 caracteres"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    minLength={8}
                  />
                </div>
                <div className="flex gap-3 pt-2">
                  <Button variant="outline" asChild className="flex-1">
                    <Link to="/">Cancelar</Link>
                  </Button>
                  <Button type="submit" className="flex-1" disabled={loading}>
                    {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                    Cadastrar
                  </Button>
                </div>
              </form>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
