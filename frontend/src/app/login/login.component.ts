import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['../shared/styles/auth.css', './login.component.css']
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  rememberMe: boolean = false;
  submitted: boolean = false;
  errorMessage: string = '';
  isLoading: boolean = false;
  showPassword: boolean = false;

  constructor(
    private router: Router,
    private apiService: ApiService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';
    this.isLoading = true;

    if (!this.username || !this.password) {
      this.errorMessage = 'Please enter your username and password';
      this.isLoading = false;
      return;
    }

    this.apiService.login({
      username: this.username.trim(),
      password: this.password
    }).subscribe({
      next: (response) => {
        // Login successful - store client info in localStorage
        this.authService.storeClientSession(response);
        console.log('Login successful:', response);
        
        // Navigate to homepage
        this.router.navigate(['/homepage']);
      },
      error: (error) => {
        this.isLoading = false;
        if (error.status === 401) {
          this.errorMessage = 'Invalid username or password.';
        } else if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Please enter your username and password.';
        } else if (error.status === 0) {
          this.errorMessage = 'The backend is unavailable. Please make sure the API is running and try again.';
        } else if (error.status >= 500) {
          this.errorMessage = 'The backend is running but could not complete login. Check the server and database connection, then try again.';
        } else {
          this.errorMessage = 'Login failed. Please try again in a moment.';
        }
        this.cdr.detectChanges();
      }
    });

  }

  resetForm() {
    this.username = '';
    this.password = '';
    this.submitted = false;
    this.errorMessage = '';
    this.isLoading = false;
  }
}
