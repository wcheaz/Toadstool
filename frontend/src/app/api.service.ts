import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/logout`, {});
  }

  validateToken(token: string): Observable<TokenValidationResponse> {
    return this.http.post<TokenValidationResponse>(`${this.apiUrl}/auth/validate`, { token });
  }
}

export interface RegisterRequest {
  email: string;
  displayName: string;
  username: string;
  password: string;
}

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
