import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-login.component.html',
  styleUrls: ['../shared/styles/auth.css', './admin-login.component.css']
})
export class AdminLoginComponent {
  email: string = '';
  password: string = '';
  rememberMe: boolean = false;
  submitted: boolean = false;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private router: Router,
    private apiService: ApiService,
    private authService: AuthService
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

    this.apiService.adminLogin({
      email: this.email.trim(),
      password: this.password
    }).subscribe({
      next: (response) => {
        this.authService.storeClientSession(response);
        this.router.navigate(['/homepage']);
      },
      error: (error) => {
        this.isLoading = false;
        if (error.status === 401) {
          this.errorMessage = 'Invalid admin email or password.';
        } else if (error.status === 403) {
          this.errorMessage = error.error?.message || 'Admin access is required.';
        } else if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Please enter your email and password.';
        } else if (error.status === 0) {
          this.errorMessage = 'The backend is unavailable. Please make sure the API is running and try again.';
        } else {
          this.errorMessage = 'Admin login failed. Please try again in a moment.';
        }
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
