import { Link, useNavigate } from 'react-router-dom'
import { LogOut, UserPlus } from 'lucide-react'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'

export default function Header() {
  const { logout, user } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="bg-primary text-primary-foreground shadow-md">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-3">
          <span className="text-xl font-bold tracking-tight">RPE</span>
          <Separator orientation="vertical" className="h-5 bg-primary-foreground/30" />
          <span className="text-sm font-light opacity-75">Order Manager</span>
        </Link>

        <nav className="flex items-center gap-2">
          {user && (
            <span className="text-sm opacity-70 hidden sm:block mr-2">
              Olá, <strong className="opacity-100">{user.sub}</strong>
            </span>
          )}
          <Button
            variant="ghost"
            size="sm"
            asChild
            className="text-primary-foreground hover:bg-primary-foreground/10 hover:text-primary-foreground"
          >
            <Link to="/register">
              <UserPlus className="h-4 w-4" />
              <span className="hidden sm:inline">Novo usuário</span>
            </Link>
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={handleLogout}
            className="gap-1.5"
          >
            <LogOut className="h-4 w-4" />
            Sair
          </Button>
        </nav>
      </div>
    </header>
  )
}
