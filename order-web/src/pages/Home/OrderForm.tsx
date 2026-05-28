import { useEffect, useState, type FormEvent } from 'react'
import { v4 as uuidv4 } from 'uuid'
import { toast } from 'sonner'
import { Loader2, PlusCircle, RefreshCw } from 'lucide-react'
import { ordersService } from '@/services/orders.service'
import { paymentService } from '@/services/payment.service'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { cn } from '@/lib/utils'
import type { CreateOrderResponse, PaymentMethod } from '@/types'

interface Props {
  onOrderCreated: (order: CreateOrderResponse, cpfComprador: string) => void
}

type FieldKey = 'id_item' | 'valor' | 'meio_pagamento' | 'nome_comprador' | 'cpf_comprador'
type FieldErrors = Partial<Record<FieldKey, string>>
type TouchedFields = Partial<Record<FieldKey, boolean>>

const emptyFields = { id_item: '', valor: '', meio_pagamento: '', nome_comprador: '', cpf_comprador: '' }

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function validateCpf(cpf: string): boolean {
  const d = cpf.replace(/\D/g, '')
  if (d.length !== 11 || /^(\d)\1+$/.test(d)) return false
  const calc = (n: number) => {
    const sum = Array.from({ length: n - 1 }, (_, i) => Number(d[i]) * (n - i)).reduce((a, b) => a + b, 0)
    const rem = (sum * 10) % 11
    return rem >= 10 ? 0 : rem
  }
  return calc(10) === Number(d[9]) && calc(11) === Number(d[10])
}

function maskCpf(value: string): string {
  const d = value.replace(/\D/g, '').slice(0, 11)
  if (d.length <= 3) return d
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`
}

function validate(fields: typeof emptyFields): FieldErrors {
  const e: FieldErrors = {}
  if (!fields.id_item) e.id_item = 'Campo obrigatório.'
  else if (!UUID_REGEX.test(fields.id_item)) e.id_item = 'Formato inválido. Use um UUID válido (ex: gerado pelo botão).'

  if (!fields.valor) e.valor = 'Campo obrigatório.'
  else if (parseFloat(fields.valor) <= 0) e.valor = 'O valor deve ser maior que zero.'

  if (!fields.meio_pagamento) e.meio_pagamento = 'Selecione um meio de pagamento.'

  if (!fields.nome_comprador.trim()) e.nome_comprador = 'Campo obrigatório.'
  else if (fields.nome_comprador.trim().length < 3) e.nome_comprador = 'Nome deve ter pelo menos 3 caracteres.'

  if (!fields.cpf_comprador) e.cpf_comprador = 'Campo obrigatório.'
  else if (!validateCpf(fields.cpf_comprador)) e.cpf_comprador = 'CPF inválido.'

  return e
}

export default function OrderForm({ onOrderCreated }: Props) {
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([])
  const [loading, setLoading] = useState(false)
  const [fields, setFields] = useState(emptyFields)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [touched, setTouched] = useState<TouchedFields>({})
  const [idempotencyKey, setIdempotencyKey] = useState(() => uuidv4())

  useEffect(() => {
    paymentService.getMethods().then(setPaymentMethods).catch(() => {})
  }, [])

  function set(key: FieldKey, value: string) {
    const updated = { ...fields, [key]: value }
    setFields(updated)
    if (touched[key]) {
      setErrors(validate(updated))
    }
  }

  function touch(key: FieldKey) {
    setTouched((prev) => ({ ...prev, [key]: true }))
    setErrors(validate(fields))
  }

  function fieldError(key: FieldKey) {
    return touched[key] ? errors[key] : undefined
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const allTouched: TouchedFields = { id_item: true, valor: true, meio_pagamento: true, nome_comprador: true, cpf_comprador: true }
    setTouched(allTouched)
    const currentErrors = validate(fields)
    setErrors(currentErrors)
    if (Object.keys(currentErrors).length > 0) return

    setLoading(true)
    try {
      const result = await ordersService.create({
        id_item: fields.id_item,
        valor: parseFloat(fields.valor),
        meio_pagamento: fields.meio_pagamento,
        nome_comprador: fields.nome_comprador,
        cpf_comprador: fields.cpf_comprador.replace(/\D/g, ''),
      }, idempotencyKey)
      onOrderCreated(result, fields.cpf_comprador.replace(/\D/g, ''))
      setFields(emptyFields)
      setTouched({})
      setErrors({})
      setIdempotencyKey(uuidv4())
      toast.success('Ordem criada com sucesso!')
    } catch (err: unknown) {
      const axiosError = err as { response?: { status?: number; data?: { message?: string } } }
      if (axiosError.response?.status === 409) {
        const msg = axiosError.response.data?.message ?? 'Esta ordem já foi processada anteriormente.'
        toast.warning(msg, { description: 'Nenhuma nova ordem foi criada.' })
      } else {
        toast.error('Erro ao criar ordem. Tente novamente.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card>
      <CardHeader className="pb-4">
        <CardTitle className="text-base flex items-center gap-2">
          <PlusCircle className="h-4 w-4 text-primary" />
          Nova Ordem
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4">

          <div className="space-y-1.5">
            <Label htmlFor="id_item">ID do Item</Label>
            <div className="flex gap-2">
              <Input
                id="id_item"
                placeholder="UUID do item"
                value={fields.id_item}
                onChange={(e) => set('id_item', e.target.value)}
                onBlur={() => touch('id_item')}
                className={cn('font-mono text-sm', fieldError('id_item') && 'border-destructive focus-visible:ring-destructive')}
              />
              <Button
                type="button"
                variant="outline"
                onClick={() => set('id_item', uuidv4())}
                className="shrink-0 gap-1.5 text-xs text-muted-foreground"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                Gerar
              </Button>
            </div>
            {fieldError('id_item') && <p className="text-xs text-destructive">{fieldError('id_item')}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="valor">Valor (R$)</Label>
            <Input
              id="valor"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0,00"
              value={fields.valor}
              onChange={(e) => set('valor', e.target.value)}
              onBlur={() => touch('valor')}
              className={cn(fieldError('valor') && 'border-destructive focus-visible:ring-destructive')}
            />
            {fieldError('valor') && <p className="text-xs text-destructive">{fieldError('valor')}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="meio_pagamento">Meio de Pagamento</Label>
            <Select
              value={fields.meio_pagamento}
              onValueChange={(v) => set('meio_pagamento', v)}
            >
              <SelectTrigger
                id="meio_pagamento"
                className={cn(fieldError('meio_pagamento') && 'border-destructive focus:ring-destructive')}
              >
                <SelectValue placeholder="Selecione..." />
              </SelectTrigger>
              <SelectContent>
                {paymentMethods.map((m) => (
                  <SelectItem key={m.id} value={m.description}>
                    {m.description}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {fieldError('meio_pagamento') && <p className="text-xs text-destructive">{fieldError('meio_pagamento')}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="nome_comprador">Nome do Comprador</Label>
            <Input
              id="nome_comprador"
              placeholder="Nome completo"
              value={fields.nome_comprador}
              onChange={(e) => set('nome_comprador', e.target.value)}
              onBlur={() => touch('nome_comprador')}
              className={cn(fieldError('nome_comprador') && 'border-destructive focus-visible:ring-destructive')}
            />
            {fieldError('nome_comprador') && <p className="text-xs text-destructive">{fieldError('nome_comprador')}</p>}
          </div>

          <div className="space-y-1.5 sm:col-span-2">
            <Label htmlFor="cpf_comprador">CPF do Comprador</Label>
            <Input
              id="cpf_comprador"
              placeholder="000.000.000-00"
              value={fields.cpf_comprador}
              onChange={(e) => set('cpf_comprador', maskCpf(e.target.value))}
              onBlur={() => touch('cpf_comprador')}
              className={cn(fieldError('cpf_comprador') && 'border-destructive focus-visible:ring-destructive')}
            />
            {fieldError('cpf_comprador') && <p className="text-xs text-destructive">{fieldError('cpf_comprador')}</p>}
          </div>

          <div className="sm:col-span-2">
            <Button type="submit" disabled={loading}>
              {loading && <Loader2 className="h-4 w-4 animate-spin" />}
              Criar Ordem
            </Button>
          </div>

        </form>
      </CardContent>
    </Card>
  )
}
