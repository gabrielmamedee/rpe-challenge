import api from './api'
import type { PaymentMethod } from '../types'

export const paymentService = {
  async getMethods(): Promise<PaymentMethod[]> {
    const { data } = await api.get<PaymentMethod[]>('/api/v1/payment-methods')
    return data
  },
}
