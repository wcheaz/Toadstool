import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ApiService } from '../api.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  fullName: string = '';
  email: string = '';
  password: string = '';
  confirmPassword: string = '';
  agreedToTerms: boolean = false;
  submitted: boolean = false;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(private router: Router, private apiService: ApiService) {}

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';
    this.isLoading = true;

    if (!this.fullName || this.fullName.trim() === '') {
      this.errorMessage = 'Please enter your full name';
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

    // Call backend API to register new client
    this.apiService.register({
      displayName: this.fullName,
      email: this.email,
      password: this.password
    }).subscribe({
      next: (response) => {
        // Registration successful - store client info in localStorage
        localStorage.setItem('clientId', response.clientId);
        localStorage.setItem('clientEmail', response.email);
        localStorage.setItem('clientName', response.displayName);
        localStorage.setItem('clientStatus', response.status);
        
        console.log('Registration successful:', response);
        
        // Navigate to homepage
        this.router.navigate(['/homepage']);
      },
      error: (error) => {
        // Registration failed - show error message
        this.isLoading = false;
        if (error.status === 409) {
          this.errorMessage = 'Email already exists. Please use a different email.';
        } else if (error.status === 400) {
          this.errorMessage = 'Invalid request. Please check your input and try again.';
        } else {
          this.errorMessage = 'Registration failed. Please try again.';
        }
        console.error('Registration error:', error);
      }
    });
  }

  resetForm() {
    this.fullName = '';
    this.email = '';
    this.password = '';
    this.confirmPassword = '';
    this.agreedToTerms = false;
    this.submitted = false;
    this.errorMessage = '';
    this.isLoading = false;
  }
}
