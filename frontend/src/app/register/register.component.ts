import { Component, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['../shared/styles/auth.css', './register.component.css']
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
  showPassword: boolean = false;
  showConfirmPassword: boolean = false;
  activeLegalDocument: 'terms' | 'privacy' | null = null;

  constructor(private router: Router, private apiService: ApiService, private authService: AuthService, private cdr: ChangeDetectorRef) {}

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  openLegalDocument(document: 'terms' | 'privacy') {
    this.activeLegalDocument = document;
  }

  closeLegalDocument() {
    this.activeLegalDocument = null;
  }

  @HostListener('document:keydown.escape')
  onEscapeKey() {
    this.closeLegalDocument();
  }

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';
    this.isLoading = true;

    if (!this.displayName || this.displayName.trim() === '') {
      this.errorMessage = 'Please enter your display name';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    if (!this.username || this.username.trim() === '') {
      this.errorMessage = 'Please enter a username';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    if (!this.email || this.email.trim() === '') {
      this.errorMessage = 'Please enter your email';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim())) {
      this.errorMessage = 'Please enter a valid email address';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    if (!this.password || this.password.trim() === '') {
      this.errorMessage = 'Please enter your password';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    if (this.password.length < 8) {
      this.errorMessage = 'Password must be at least 8 characters';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    if (!this.agreedToTerms) {
      this.errorMessage = 'Please agree to the Terms & Conditions';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.apiService.register({
      email: this.email.trim(),
      displayName: this.displayName.trim(),
      username: this.username.trim(),
      password: this.password
    }).subscribe({
      next: (response) => {
        // Registration successful - store client info for the current session
        this.authService.storeClientSession(response);

        console.log('Registration successful:', response);
        
        // Navigate to homepage
        this.router.navigate(['/homepage']);
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
        this.cdr.detectChanges();
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
