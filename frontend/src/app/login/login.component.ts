import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ApiService } from '../api.service';
import { storeAuthSession } from '../auth-storage';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  submitted: boolean = false;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(private router: Router, private apiService: ApiService) {}

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
        storeAuthSession(response);
        this.router.navigate(['/homepage']);
      },
      error: (error) => {
        this.isLoading = false;
        if (error.status === 401) {
          this.errorMessage = 'Invalid username or password.';
        } else if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Please enter your username and password.';
        } else {
          this.errorMessage = 'Login failed. Please try again in a moment.';
        }
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
