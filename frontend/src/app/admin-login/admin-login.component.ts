import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.css'
})
export class AdminLoginComponent {
  username: string = '';
  password: string = '';
  submitted: boolean = false;
  errorMessage: string = '';

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';

    if (!this.username || !this.password) {
      this.errorMessage = 'Please enter both username and password';
      return;
    }

    // TODO: Replace with actual admin authentication service
    console.log('Admin login attempt:', { username: this.username, password: this.password });
    
    // Simulated successful login
    alert(`Welcome, Admin ${this.username}!`);
    this.resetForm();
  }

  resetForm() {
    this.username = '';
    this.password = '';
    this.submitted = false;
    this.errorMessage = '';
  }
}
