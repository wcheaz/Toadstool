import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = 'http://localhost:8082/api';

  constructor(private http: HttpClient) {}

  /**
   * Login with email - verifies email exists in clients table
   */
  login(email: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, { email });
  }
}

export interface LoginResponse {
  clientId: string;
  email: string;
  displayName: string;
  status: string;
  message: string;
}
