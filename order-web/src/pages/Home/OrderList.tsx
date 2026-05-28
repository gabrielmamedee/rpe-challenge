import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { toast } from 'sonner'
import { Loader2, Search, Radio } from 'lucide-react'
import { ordersService } from '@/services/orders.service'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { cn } from '@/lib/utils'
import type { Order, OrderStatus } from '@/types'

const POLL_INTERVAL_MS = 5000

const statusConfig: Record<OrderStatus, { label: string; className: string }> = {
  PAGO:              { label: 'Pago',              className: 'bg-green-100 text-green-700 border-green-200' },
  PENDENTE:          { label: 'Pendente',          className: 'bg-yellow-100 text-yellow-700 border-yellow-200' },
  PENDENTE_PAGAMENTO:{ label: 'Aguard. Pagamento', className: 'bg-yellow-100 text-yellow-700 border-yellow-200' },
  PROCESSANDO:       { label: 'Processando',       className: 'bg-blue-100 text-blue-700 border-blue-200' },
  RECUSADO:          { label: 'Recusado',          className: 'bg-red-100 text-red-700 border-red-200' },
  REPROVADO:         { label: 'Reprovado',         className: 'bg-red-100 text-red-700 border-red-200' },
  CANCELADO:         { label: 'Cancelado',         className: 'bg-gray-100 text-gray-600 border-gray-200' },
}

function StatusBadge({ status }: { status: OrderStatus }) {
  const config = statusConfig[status] ?? { label: status, className: 'bg-muted text-muted-foreground' }
  return (
    <Badge variant="outline" className={cn('font-medium', config.className)}>
      {config.label}
    </Badge>
  )
}

export default function OrderList() {
  const [cpf, setCpf] = useState('')
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const hasPending = orders.some((o) => ['PENDENTE', 'PENDENTE_PAGAMENTO', 'PROCESSANDO'].includes(o.status))

  const fetchOrders = useCallback(async (cpfValue: string, silent = false) => {
    if (!silent) setLoading(true)
    try {
      const data = await ordersService.list(cpfValue)
      setOrders(data)
      setSearched(true)
    } catch {
      if (!silent) toast.error('Erro ao buscar ordens.')
    } finally {
      if (!silent) setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (pollingRef.current) clearInterval(pollingRef.current)
    if (hasPending && searched && cpf) {
      pollingRef.current = setInterval(() => fetchOrders(cpf, true), POLL_INTERVAL_MS)
    }
    return () => { if (pollingRef.current) clearInterval(pollingRef.current) }
  }, [hasPending, searched, cpf, fetchOrders])

  async function handleSearch(e: FormEvent) {
    e.preventDefault()
    await fetchOrders(cpf)
  }

  return (
    <Card>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base flex items-center gap-2">
            <Search className="h-4 w-4 text-primary" />
            Consultar Ordens
          </CardTitle>
          {hasPending && (
            <span className="flex items-center gap-1.5 text-xs text-yellow-600 bg-yellow-50 border border-yellow-200 px-2.5 py-1 rounded-full">
              <Radio className="h-3 w-3 animate-pulse" />
              Atualizando PIX...
            </span>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={handleSearch} className="flex gap-2">
          <div className="flex-1 space-y-1.5">
            <Label htmlFor="cpf_search" className="sr-only">CPF do comprador</Label>
            <Input
              id="cpf_search"
              placeholder="CPF do comprador"
              value={cpf}
              onChange={(e) => setCpf(e.target.value)}
              required
            />
          </div>
          <Button type="submit" disabled={loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
            Buscar
          </Button>
        </form>

        {searched && orders.length === 0 && !loading && (
          <p className="text-sm text-muted-foreground text-center py-8">
            Nenhuma ordem encontrada para este CPF.
          </p>
        )}

        {orders.length > 0 && (
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[280px]">ID</TableHead>
                  <TableHead>Comprador</TableHead>
                  <TableHead className="text-right">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {orders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="font-mono text-xs text-muted-foreground">
                      {order.id}
                    </TableCell>
                    <TableCell className="font-medium">{order.nome_comprador}</TableCell>
                    <TableCell className="text-right">
                      <StatusBadge status={order.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
