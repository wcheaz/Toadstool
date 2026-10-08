import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

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

    // TODO: Remove - simulated admin login, replace with actual admin authentication service
    console.log('Admin login attempt for user:', this.email);

    // Simulated successful login
    alert(`Welcome, Admin ${this.email}!`);
    this.resetForm();
    this.isLoading = false;
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
