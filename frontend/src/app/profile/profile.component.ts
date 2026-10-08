import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-profile-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  @Input() isOpen: boolean = false;
  @Output() close = new EventEmitter<void>();

  clientName: string = '';
  accountId: string = '';
  email: string = '';
  joinDate: string = '';
  profileStats = {
    totalTrades: 2841,
    successRate: 99.42,
    totalValue: 142384.50
  };

  constructor(private authService: AuthService) {
    this.clientName = this.authService.getClientName();
    this.accountId = this.authService.getAccountId();
  }

  ngOnInit() {
    this.email = `${this.clientName.toLowerCase().replace(/\s+/g, '.')}@toadstool.io`;
    this.joinDate = 'January 15, 2024';
  }

  closeModal() {
    this.close.emit();
  }

  updateProfile() {
    alert('Profile editing coming soon');
  }

  changePassword() {
    alert('Password change feature coming soon');
  }

  logout() {
    if (confirm('Are you sure you want to sign out?')) {
      this.authService.logout();
    }
  }
}
