import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { clearAuthSession } from '../auth-storage';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  email: string = '';
  displayName: string = '';
  username: string = '';
  password: string = '';
  confirmPassword: string = '';
  agreedToTerms: boolean = false;
  submitted: boolean = false;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(private readonly router: Router, private readonly apiService: ApiService) {}

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';
    this.isLoading = true;

    if (!this.displayName || this.displayName.trim() === '') {
      this.errorMessage = 'Please enter your display name';
      this.isLoading = false;
      return;
    }

    if (!this.username || this.username.trim() === '') {
      this.errorMessage = 'Please enter a username';
      this.isLoading = false;
      return;
    }

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

    if (this.password.length < 8) {
      this.errorMessage = 'Password must be at least 8 characters';
      this.isLoading = false;
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      this.isLoading = false;
      return;
    }

    if (!this.agreedToTerms) {
      this.errorMessage = 'Please agree to the Terms & Conditions';
      this.isLoading = false;
      return;
    }

    this.apiService.register({
      email: this.email.trim(),
      displayName: this.displayName.trim(),
      username: this.username.trim(),
      password: this.password
    }).subscribe({
      next: () => {
        clearAuthSession();
        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.isLoading = false;
        if (error.status === 409) {
          this.errorMessage = error.error?.message || 'That email or username is already in use.';
        } else if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Please check your registration details.';
        } else if (error.status === 0) {
          this.errorMessage = 'The backend is unavailable. Please make sure the API is running and try again.';
        } else if (error.status >= 500) {
          this.errorMessage = 'The backend is running but could not complete registration. Check the server and database connection, then try again.';
        } else {
          this.errorMessage = 'Registration failed. Please try again in a moment.';
        }
        console.error('Registration error:', error);
      }
    });
  }

  resetForm() {
    this.email = '';
    this.displayName = '';
    this.username = '';
    this.password = '';
    this.confirmPassword = '';
    this.agreedToTerms = false;
    this.submitted = false;
    this.errorMessage = '';
    this.isLoading = false;
  }
}
