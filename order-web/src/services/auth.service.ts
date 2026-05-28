import api from './api'
import type { AuthResponse, LoginPayload, RegisterPayload, User } from '../types'

export const authService = {
  async login(payload: LoginPayload): Promise<AuthResponse> {
    const { data } = await api.post<AuthResponse>('/api/v1/login', payload)
    return data
  },

  async register(payload: RegisterPayload): Promise<User> {
    const { data } = await api.post<User>('/api/v1/users', payload)
    return data
  },
}
