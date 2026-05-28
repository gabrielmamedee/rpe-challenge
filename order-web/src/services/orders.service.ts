import api from './api'
import type { CreateOrderPayload, CreateOrderResponse, Order } from '../types'

export const ordersService = {
  async create(payload: CreateOrderPayload, idempotencyKey: string): Promise<CreateOrderResponse> {
    const { data } = await api.post<CreateOrderResponse>('/api/v1/orders', payload, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    return data
  },

  async list(cpfComprador: string): Promise<Order[]> {
    const { data } = await api.get<Order[]>('/api/v1/orders', {
      params: { cpf_comprador: cpfComprador },
    })
    return data
  },
}
