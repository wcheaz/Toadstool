import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

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

  constructor(private router: Router) {}

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';

    if (!this.username || !this.password) {
      this.errorMessage = 'Please enter both username and password';
      return;
    }

    // TODO: Replace with actual authentication service
    console.log('Login attempt:', { username: this.username, password: this.password });
    
    // Store client name in localStorage
    localStorage.setItem('clientName', this.username);
    
    // Navigate to homepage
    this.router.navigate(['/homepage']);
  }

  resetForm() {
    this.username = '';
    this.password = '';
    this.submitted = false;
    this.errorMessage = '';
  }
}
