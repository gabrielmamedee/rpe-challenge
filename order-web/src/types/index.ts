export interface AuthResponse {
  token: string
  type: string
}

export interface LoginPayload {
  login: string
  password: string
}

export interface RegisterPayload {
  login: string
  password: string
}

export interface User {
  id: string
  login: string
  role: string
}

export interface PaymentMethod {
  id: string
  description: string
}

export type OrderStatus = 'PAGO' | 'PENDENTE' | 'PENDENTE_PAGAMENTO' | 'PROCESSANDO' | 'RECUSADO' | 'REPROVADO' | 'CANCELADO'

export interface CreateOrderPayload {
  id_item: string
  valor: number
  meio_pagamento: string
  nome_comprador: string
  cpf_comprador: string
}

export interface CreateOrderResponse {
  id_ordem: string
  status: OrderStatus
  data_criacao: string
  data_pagamento: string | null
}

export interface Order {
  id: string
  nome_comprador: string
  status: OrderStatus
}
