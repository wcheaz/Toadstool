import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { storeAuthSession } from '../auth-storage';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: '../login/login.component.css'
})
export class RegisterComponent {
  email = '';
  displayName = '';
  username = '';
  password = '';
  confirmPassword = '';
  errorMessage = '';
  isLoading = false;

  constructor(private readonly router: Router, private readonly apiService: ApiService) {}

  onSubmit() {
    this.errorMessage = '';

    if (!this.email || !this.displayName || !this.username || !this.password || !this.confirmPassword) {
      this.errorMessage = 'Please complete every field.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    this.isLoading = true;
    this.apiService.register({
      email: this.email.trim(),
      displayName: this.displayName.trim(),
      username: this.username.trim(),
      password: this.password
    }).subscribe({
      next: (response) => {
        storeAuthSession(response);
        this.router.navigate(['/homepage']);
      },
      error: (error) => {
        this.isLoading = false;
        if (error.status === 409) {
          this.errorMessage = error.error?.message || 'That email or username is already in use.';
        } else if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Please check your registration details.';
        } else {
          this.errorMessage = 'Registration failed. Please try again in a moment.';
        }
      }
    });
  }

  resetForm() {
    this.email = '';
    this.displayName = '';
    this.username = '';
    this.password = '';
    this.confirmPassword = '';
    this.errorMessage = '';
    this.isLoading = false;
  }
}
