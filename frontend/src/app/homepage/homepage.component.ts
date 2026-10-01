import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';

interface Asset {
  symbol: string;
  name: string;
  price: number;
  change: number;
  holdings: number;
}

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [CommonModule, TradingModalComponent],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.css'
})
export class HomepageComponent {
  clientName: string = '';
  currentBalance: number = 142384.50;
  availableForTrading: number = 12450.00;
  isBalanceVisible: boolean = true;
  activeTab: string = 'news';
  platformStatus: string = 'Platform live status: Standard Trading Hours';
  
  // Trading modal state
  isTradeModalOpen: boolean = false;
  selectedAsset: Asset | null = null;

  // Available assets
  assets: Asset[] = [
    { symbol: 'NEXS', name: 'Nexus Equity Fund', price: 142.10, change: 1.76, holdings: 40.0 },
    { symbol: 'USDT', name: 'US Digital Dollar', price: 1.00, change: 0.00, holdings: 12450.0 },
    { symbol: 'BTC', name: 'Bitcoin Vault Share', price: 67230.00, change: -0.45, holdings: 0.15 }
  ];

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

  openTradeModal(asset: Asset) {
    this.selectedAsset = asset;
    this.isTradeModalOpen = true;
  }

  closeTradeModal() {
    this.isTradeModalOpen = false;
    this.selectedAsset = null;
  }

  logout() {
    localStorage.removeItem('clientName');
    window.location.href = '/login';
  }
}
