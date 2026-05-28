import { useEffect, useRef, useState } from 'react'
import { AlertCircle, CheckCircle2, Loader2, XCircle } from 'lucide-react'
import Header from '@/components/layout/Header'
import OrderForm from './OrderForm'
import OrderList from './OrderList'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { ordersService } from '@/services/orders.service'
import type { CreateOrderResponse, OrderStatus } from '@/types'

const POLL_INTERVAL_MS = 5000

const PENDING_STATUSES: OrderStatus[] = ['PENDENTE', 'PENDENTE_PAGAMENTO', 'PROCESSANDO']
const FAILURE_STATUSES: OrderStatus[] = ['REPROVADO', 'CANCELADO', 'RECUSADO']

const statusLabel: Record<OrderStatus, string> = {
  PAGO:              'Pago',
  PENDENTE:          'Pendente',
  PENDENTE_PAGAMENTO:'Aguard. Pagamento',
  PROCESSANDO:       'Processando',
  RECUSADO:          'Recusado',
  REPROVADO:         'Reprovado',
  CANCELADO:         'Cancelado',
}

const statusBadgeClass: Record<OrderStatus, string> = {
  PAGO:              'bg-green-100 text-green-700 border-green-200',
  PENDENTE:          'bg-yellow-100 text-yellow-700 border-yellow-200',
  PENDENTE_PAGAMENTO:'bg-yellow-100 text-yellow-700 border-yellow-200',
  PROCESSANDO:       'bg-blue-100 text-blue-700 border-blue-200',
  RECUSADO:          'bg-red-100 text-red-700 border-red-200',
  REPROVADO:         'bg-red-100 text-red-700 border-red-200',
  CANCELADO:         'bg-gray-100 text-gray-600 border-gray-200',
}

function OrderResultCard({ order }: { order: CreateOrderResponse }) {
  const isPending = PENDING_STATUSES.includes(order.status)
  const isFailure = FAILURE_STATUSES.includes(order.status)

  if (isPending) {
    return (
      <Card className="border-yellow-200 bg-yellow-50/50">
        <CardContent className="pt-4">
          <div className="flex items-center gap-4">
            <div className="flex items-center justify-center w-10 h-10 rounded-full bg-yellow-100 shrink-0">
              <Loader2 className="h-5 w-5 text-yellow-600 animate-spin" />
            </div>
            <div className="space-y-0.5">
              <p className="text-sm font-semibold text-yellow-700">Validando PIX...</p>
              <p className="text-xs text-yellow-600/80 font-mono">{order.id_ordem}</p>
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (isFailure) {
    const isReprovado = order.status === 'REPROVADO'
    return (
      <Card className={isReprovado ? 'border-red-200 bg-red-50/50' : 'border-gray-200 bg-gray-50/50'}>
        <CardContent className="pt-4">
          <div className="flex items-start gap-3">
            {isReprovado
              ? <XCircle className="h-5 w-5 text-red-500 mt-0.5 shrink-0" />
              : <AlertCircle className="h-5 w-5 text-gray-500 mt-0.5 shrink-0" />
            }
            <div className="space-y-3 w-full">
              <p className={`text-sm font-semibold ${isReprovado ? 'text-red-700' : 'text-gray-600'}`}>
                {isReprovado ? 'Pagamento reprovado' : 'Pagamento cancelado'}
              </p>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 text-xs">
                <div>
                  <p className="text-muted-foreground font-medium mb-0.5">ID da Ordem</p>
                  <p className="font-mono text-foreground truncate">{order.id_ordem}</p>
                </div>
                <div>
                  <p className="text-muted-foreground font-medium mb-1">Status</p>
                  <Badge variant="outline" className={statusBadgeClass[order.status]}>
                    {statusLabel[order.status]}
                  </Badge>
                </div>
                <div>
                  <p className="text-muted-foreground font-medium mb-0.5">Criado em</p>
                  <p>{new Date(order.data_criacao).toLocaleString('pt-BR')}</p>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="border-green-200 bg-green-50/50">
      <CardContent className="pt-4">
        <div className="flex items-start gap-3">
          <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 shrink-0" />
          <div className="space-y-3 w-full">
            <p className="text-sm font-semibold text-green-700">Pagamento confirmado!</p>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
              <div>
                <p className="text-muted-foreground font-medium mb-0.5">ID da Ordem</p>
                <p className="font-mono text-foreground truncate">{order.id_ordem}</p>
              </div>
              <div>
                <p className="text-muted-foreground font-medium mb-1">Status</p>
                <Badge variant="outline" className={statusBadgeClass[order.status]}>
                  {statusLabel[order.status]}
                </Badge>
              </div>
              <div>
                <p className="text-muted-foreground font-medium mb-0.5">Criado em</p>
                <p>{new Date(order.data_criacao).toLocaleString('pt-BR')}</p>
              </div>
              {order.data_pagamento && (
                <div>
                  <p className="text-muted-foreground font-medium mb-0.5">Pago em</p>
                  <p>{new Date(order.data_pagamento).toLocaleString('pt-BR')}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

export default function HomePage() {
  const [lastOrder, setLastOrder] = useState<CreateOrderResponse | null>(null)
  const [lastOrderCpf, setLastOrderCpf] = useState('')
  const lastOrderRef = useRef(lastOrder)
  lastOrderRef.current = lastOrder

  const isPending = lastOrder ? PENDING_STATUSES.includes(lastOrder.status) : false

  function handleOrderCreated(order: CreateOrderResponse, cpfComprador: string) {
    setLastOrder(order)
    setLastOrderCpf(cpfComprador)
  }

  useEffect(() => {
    if (!isPending || !lastOrderCpf) return

    const interval = setInterval(async () => {
      const current = lastOrderRef.current
      if (!current) return
      try {
        const orders = await ordersService.list(lastOrderCpf)
        const match = orders.find((o) => o.id === current.id_ordem)
        if (match && match.status !== current.status) {
          setLastOrder((prev) => prev ? { ...prev, status: match.status } : prev)
        }
      } catch {
        // polling silencioso — ignora erros transitórios
      }
    }, POLL_INTERVAL_MS)

    return () => clearInterval(interval)
  }, [isPending, lastOrderCpf])

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <main className="max-w-5xl mx-auto px-4 py-8 space-y-6">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Gestão de Ordens</h2>
          <p className="text-muted-foreground text-sm mt-1">Crie e acompanhe ordens de pagamento.</p>
        </div>

        <Separator />

        {lastOrder && <OrderResultCard order={lastOrder} />}

        <OrderForm onOrderCreated={handleOrderCreated} />
        <OrderList />
      </main>
    </div>
  )
}
