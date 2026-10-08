export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  clientId: string;
  accountId: string;
  displayName: string;
  username: string;
  email: string;
  role: string;
}

export interface TokenValidationResponse {
  authenticated: boolean;
  subject: string;
  username: string;
  email: string;
  role: string;
  tokenType: string;
  sessionKeyId: string;
  clientId: string;
  accountId: string;
  issuedAt: string;
}

export interface LoginResponse {
  clientId: string;
  email: string;
  displayName: string;
  status: string;
  message: string;
}

export interface RegisterRequest {
  displayName: string;
  email: string;
  password: string;
}

export interface Instrument {
  instrumentId: string;
  symbol: string;
  name: string;
  assetClass: string;
  status: string;
}

export interface QuoteResponse {
  symbol: string;
  price: number;
  bid: number;
  ask: number;
  change: number;
  changePercent: number;
  asOf: string;
}

export interface CandleResponse {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface QuotePreviewResponse {
  instrumentId: string;
  symbol: string;
  side: string;
  quantity: string;
  indicativePrice: string;
  estimatedTotal: string;
  timestamp: string;
  notes: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  pagination: {
    limit: number;
    offset: number;
    totalCount: number;
    hasMore: boolean;
  };
}

export interface AccountResponse {
  accountId: string;
  clientId: string;
  status: string;
  openedAt: string;
}

export interface OrderResponse {
  orderId: string;
  accountId: string;
  instrumentId: string;
  side: string;
  quantity: string;
  idempotencyKey: string;
  status: string;
  submittedAt: string;
}

export interface DepthLevel {
  volume: number;
  price: number;
  side: string;
}
