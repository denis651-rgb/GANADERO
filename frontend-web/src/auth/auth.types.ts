export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export interface AuthUser {
  id: string
  email: string
  displayName: string
  companyId?: string
  companyName: string
  roles: string[]
  permissions: string[]
  propertyIds: string[]
}

export interface SignInInput {
  email: string
  password: string
}
