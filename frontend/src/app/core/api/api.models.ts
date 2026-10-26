export interface LoginRequest {
  username: string;
  password: string;
}

export interface AdminLoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  refreshToken: string;
  refreshTokenExpiresIn: number;
  clientId: string | null;
  accountId: string | null;
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
  username: string;
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
  accountName: string;
  status: string;
  openedAt: string;
  balance: number;
}

export interface CashEventRequest {
  eventType: 'DEPOSIT' | 'WITHDRAWAL';
  amount: number;
}

export interface OrderResponse {
  orderId: string;
  accountId: string;
  instrumentId: string;
  side: string;
  quantity: string;
  idempotencyKey: string;
  /** ACCEPTED (passed the trading rules, awaiting execution), FILLED or REJECTED. */
  status: string;
  submittedAt: string | number;
  orderType?: 'MARKET' | 'LIMIT';
  limitPrice?: string | null;
  /** Execution price once FILLED. */
  fillPrice?: string | null;
  /** Why execution was REJECTED. */
  rejectionReason?: string | null;
}

export interface DepthLevel {
  volume: number;
  price: number;
  side: string;
}

/** API timestamps arrive either as ISO strings or epoch seconds, depending on the endpoint. */
export type ApiTimestamp = string | number;

export interface HoldingItem {
  instrumentId: string;
  symbol: string | null;
  name: string | null;
  assetClass: string | null;
  quantity: string;
}

/** One position in GET /accounts/{id}/holdings. */
export type HoldingResponse = HoldingItem;

export interface HoldingsResponse {
  accountId: string;
  holdings: HoldingItem[];
  cash: { balance: string; currency: string };
  asOfTimestamp: ApiTimestamp;
}

export interface FillResponse {
  fillId: string;
  orderId: string;
  price: string;
  quantity: string;
  status: string;
  executedAt: ApiTimestamp;
  fee: string;
}

export interface CashEventResponse {
  cashEventId: string;
  accountId: string;
  orderId: string | null;
  eventType: string;
  amount: number;
  occurredAt: ApiTimestamp;
  details: string;
}

export interface AdminClientSummary {
  clientId: string;
  displayName: string;
  email: string;
  username: string | null;
  status: string;
  createdAt: ApiTimestamp;
  accountCount: number;
  totalCash: number;
  primaryAccountId: string | null;
  primaryAccountName: string | null;
  latestActivity: string | null;
  latestActivityAt: ApiTimestamp | null;
  lastLoginAt: ApiTimestamp | null;
}

export interface AdminClientAccount {
  accountId: string;
  accountName: string | null;
  status: string;
  openedAt: ApiTimestamp;
  balance: number;
}

export interface AdminClientDetail {
  client: AdminClientSummary;
  accounts: AdminClientAccount[];
}

export interface LoginEventRow {
  loginEventId: string;
  clientId: string | null;
  clientName: string | null;
  emailAttempted: string;
  outcome: string;
  occurredAt: ApiTimestamp;
  details: string;
}

export interface TradeEventRow {
  tradeEventId: string;
  clientId: string | null;
  clientName: string | null;
  accountId: string | null;
  accountName: string | null;
  entityType: string;
  entityId: string;
  action: string;
  occurredAt: ApiTimestamp;
  details: string;
}

export interface AccessEventRow {
  accessEventId: string;
  adminUserId: string;
  adminEmail: string;
  clientId: string | null;
  clientName: string | null;
  action: string;
  occurredAt: ApiTimestamp;
  details: string;
}

export interface AdminCashEventRow {
  cashEventId: string;
  accountId: string;
  clientId: string;
  clientName: string;
  accountName: string;
  eventType: string;
  amount: number;
  occurredAt: ApiTimestamp;
  details: string;
}

export interface PlatformActivityRow {
  activityDate: string;
  orderCount: number;
  filledQuantity: string;
  grossAmount: string;
  updatedAt: ApiTimestamp;
}

export interface PlatformOverview {
  totalClients: number;
  activeClients: number;
  totalAccounts: number;
  totalCash: number;
  totalOrders: number;
  filledOrders: number;
  openOrders: number;
  ordersLast30Days: number;
  grossVolumeLast30Days: number;
  feesLast30Days: number;
  buyFillsLast30Days: number;
  sellFillsLast30Days: number;
  buyQuantityLast30Days: number;
  sellQuantityLast30Days: number;
  buyValueLast30Days: number;
  sellValueLast30Days: number;
  failedLoginsLast24Hours: number;
}

export interface Insight {
  name: string;
  title: string;
  value: string;
  detail: string;
}

export interface InsightsResponse {
  insights: Insight[];
  generatedAt: ApiTimestamp;
  overview: PlatformOverview | null;
  topInstruments: Array<{ instrumentId: string; symbol: string; name: string; fillCount: number; grossAmount: number; side?: string }>;
}
