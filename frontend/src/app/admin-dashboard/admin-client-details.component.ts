import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CLIENTS, ClientRecord } from './client-data';
import { SidebarComponent } from '../homepage/components/sidebar/sidebar.component';

@Component({
  selector: 'app-admin-client-details',
  standalone: true,
  imports: [CommonModule, SidebarComponent],
  templateUrl: './admin-client-details.component.html',
  styleUrl: './admin-client-details.component.css'
})
export class AdminClientDetailsComponent {
  readonly adminName: string;
  readonly client: ClientRecord;
  readonly holdingRows: Array<{ asset: string; description: string; quantity: string; price: string; value: string; change: string; allocation: string }>;

  constructor(private authService: AuthService, private router: Router, route: ActivatedRoute) {
    this.adminName = this.authService.getClientName() || 'Sarah Jenkins';
    const account = route.snapshot.paramMap.get('account');
    this.client = CLIENTS.find((item) => item.account === account) ?? CLIENTS[0];
    this.holdingRows = this.createHoldings(this.client);
  }

  get initials(): string {
    return this.client.name.split(' ').map((part) => part.charAt(0)).join('').slice(0, 2);
  }

  get holdingsValue(): string {
    const value = Number(this.client.portfolio.replace(/[$,]/g, '')) - Number(this.client.cash.replace(/[$,]/g, ''));
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
  }

  get buyingPower(): string {
    const value = Number(this.client.cash.replace(/[$,]/g, '')) - 1001;
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
  }

  backToClients(): void {
    this.router.navigate(['/admin-dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  private createHoldings(client: ClientRecord) {
    const portfolioValue = Number(client.portfolio.replace(/[$,]/g, ''));
    const cashValue = Number(client.cash.replace(/[$,]/g, ''));
    const allocation = Math.max(1, Math.round(((portfolioValue - cashValue) / portfolioValue) * 100));
    return [
      { asset: 'NEMS', description: 'Neusu Equity Fund', quantity: '400.00', price: '$142.10', value: `$${(portfolioValue * 0.91).toLocaleString('en-US', { minimumFractionDigits: 2 })}`, change: '+1.24%', allocation: `${Math.min(75, allocation - 2)}.75%` },
      { asset: 'BTC', description: 'Bitcoin Vault Share', quantity: '1.0000', price: '$7,230.00', value: `$${(portfolioValue * 0.06).toLocaleString('en-US', { minimumFractionDigits: 2 })}`, change: '-0.45%', allocation: '5.74%' },
      { asset: 'USDT', description: 'US Digital Dollar', quantity: '5,854.50', price: '$1.00', value: `$${(portfolioValue * 0.03).toLocaleString('en-US', { minimumFractionDigits: 2 })}`, change: '+0.01%', allocation: '4.51%' }
    ];
  }
}