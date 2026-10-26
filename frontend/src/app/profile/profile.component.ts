import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/auth.service';
import { ApiService } from '../core/api/api.service';

@Component({
  selector: 'app-profile-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit, OnChanges {
  @Input() isOpen: boolean = false;
  @Output() close = new EventEmitter<void>();

  clientName: string = '';
  accountId: string = '';
  accountName: string = '';
  email: string = '';
  joinDate: string = '';
  profileStats = {
    totalTrades: 2841,
    successRate: 99.42,
    totalValue: 0
  };

  constructor(private authService: AuthService, private apiService: ApiService) {
    this.clientName = this.authService.getClientName();
    this.email = this.authService.getClientEmail();
    this.accountId = this.authService.getAccountId();
  }

  ngOnInit() {
    this.refreshClientDetails();
    this.joinDate = 'January 15, 2024';
    this.loadAccountBalance();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['isOpen']?.currentValue === true) {
      this.refreshClientDetails();
      this.loadAccountBalance();
    }
  }

  private refreshClientDetails() {
    this.clientName = this.authService.getClientName();
    this.email = this.authService.getClientEmail();
  }

  private loadAccountBalance() {
    const selectedAccountId = this.authService.getAccountId();
    if (!selectedAccountId) return;

    this.accountId = selectedAccountId;
    this.apiService.getAccountById(selectedAccountId).subscribe({
      next: (account) => {
        this.accountName = account.accountName;
        this.profileStats.totalValue = account.balance;
      },
      error: (error) => console.error('Failed to load profile account:', error)
    });
  }

  closeModal() {
    this.close.emit();
  }

}
