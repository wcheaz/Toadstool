import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ApiService } from '../api.service';
import { timeout } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  email: string = '';
  password: string = '';
  rememberMe: boolean = false;
  submitted: boolean = false;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private router: Router,
    private apiService: ApiService,
    private cdr: ChangeDetectorRef
  ) {}

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';
    this.isLoading = true;

    if (!this.email || this.email.trim() === '') {
      this.errorMessage = 'Please enter your email';
      this.isLoading = false;
      return;
    }

    if (!this.password || this.password.trim() === '') {
      this.errorMessage = 'Please enter your password';
      this.isLoading = false;
      return;
    }

    // Call backend API to verify email exists in clients table
    // Add timeout to prevent indefinite hanging (10 seconds)
    this.apiService.login(this.email).pipe(
      timeout(10000)
    ).subscribe({
      next: (response) => {
        // Login successful - store client info in localStorage
        localStorage.setItem('clientId', response.clientId);
        localStorage.setItem('clientEmail', response.email);
        localStorage.setItem('clientName', response.displayName);
        localStorage.setItem('clientStatus', response.status);
        
        // Fetch the account ID for this client
        this.apiService.getAccountByClientId(response.clientId).subscribe({
          next: (account) => {
            localStorage.setItem('accountId', account.accountId);
            console.log('Login successful:', response);
            console.log('Account retrieved:', account.accountId);
            
            // Navigate to homepage
            this.router.navigate(['/homepage']);
          },
          error: (error) => {
            this.isLoading = false;
            console.error('Failed to fetch account:', error);
            this.errorMessage = 'Login succeeded but account retrieval failed. Please try again.';
          }
        });
      },
      error: (error) => {
        // Login failed - show error message
        this.isLoading = false;
        
        // Handle timeout
        if (error.name === 'TimeoutError') {
          this.errorMessage = 'Login request timed out. Please check your connection and try again.';
        } else if (error.status === 0) {
          this.errorMessage = 'Cannot connect to server. Please check your internet connection.';
        } else if (error.status === 401) {
          this.errorMessage = 'Email not found. Please check your email and try again.';
        } else if (error.status === 400) {
          this.errorMessage = 'Invalid request. Please try again.';
        } else if (error.status >= 500) {
          this.errorMessage = 'Server error. Please try again later.';
        } else {
          this.errorMessage = 'Login failed. Please try again.';
        }
        
        // Trigger change detection to update the UI
        this.cdr.markForCheck();
        
        console.error('Login error:', error);
      }
    });

  }

  resetForm() {
    this.email = '';
    this.password = '';
    this.rememberMe = false;
    this.submitted = false;
    this.errorMessage = '';
    this.isLoading = false;
  }
}
