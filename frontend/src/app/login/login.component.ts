import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ApiService } from '../api.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  email: string = '';
  submitted: boolean = false;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(private router: Router, private apiService: ApiService) {}

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';
    this.isLoading = true;

    if (!this.email || this.email.trim() === '') {
      this.errorMessage = 'Please enter your email';
      this.isLoading = false;
      return;
    }

    // Call backend API to verify email exists in clients table
    this.apiService.login(this.email).subscribe({
      next: (response) => {
        // Login successful - store client info in localStorage
        localStorage.setItem('clientId', response.clientId);
        localStorage.setItem('clientEmail', response.email);
        localStorage.setItem('clientName', response.displayName);
        localStorage.setItem('clientStatus', response.status);
        
        console.log('Login successful:', response);
        
        // Navigate to homepage
        this.router.navigate(['/homepage']);
      },
      error: (error) => {
        // Login failed - show error message
        this.isLoading = false;
        if (error.status === 401) {
          this.errorMessage = 'Email not found. Please check your email and try again.';
        } else if (error.status === 400) {
          this.errorMessage = 'Invalid request. Please try again.';
        } else {
          this.errorMessage = 'Login failed. Please try again.';
        }
        console.error('Login error:', error);
      }
    });

  }

  resetForm() {
    this.email = '';
    this.submitted = false;
    this.errorMessage = '';
    this.isLoading = false;
  }
}
