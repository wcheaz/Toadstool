import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.css'
})
export class HomepageComponent {
  clientName: string = '';
  currentBalance: number = 142384.50;
  availableForTrading: number = 12450.00;
  isBalanceVisible: boolean = true;
  activeTab: string = 'news';

  constructor() {
    // Get client name from session/localStorage
    this.clientName = localStorage.getItem('clientName') || 'User';
  }

  toggleBalanceVisibility() {
    this.isBalanceVisible = !this.isBalanceVisible;
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  deposit() {
    alert('Deposit functionality coming soon');
  }

  withdraw() {
    alert('Withdraw functionality coming soon');
  }

  transfer() {
    alert('Transfer functionality coming soon');
  }

  navigateTo(section: string) {
    console.log('Navigating to:', section);
  }

  openProfile() {
    alert('Profile settings coming soon');
  }

  openNotifications() {
    alert('Notifications settings coming soon');
  }

  logout() {
    localStorage.removeItem('clientName');
    window.location.href = '/login';
  }
}
