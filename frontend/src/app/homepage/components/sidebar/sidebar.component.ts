import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AccountResponse } from '../../../core/api/api.models';
import { AccountStateService } from '../../../core/account-state.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit {
  @Input() adminMode = false;
  /** Highlights the matching admin nav item, e.g. 'admin-dashboard' or 'admin-audit'. */
  @Input() activeSection = '';
  accounts: AccountResponse[] = [];
  selectedAccount: AccountResponse | null = null;
  isAccountMenuOpen = false;
  isCreatingAccount = false;

  constructor(
    private router: Router,
    private accountState: AccountStateService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.accountState.accounts$.subscribe(accounts => this.accounts = accounts);
    this.accountState.selectedAccount$.subscribe(account => {
      this.selectedAccount = account;
      this.changeDetectorRef.markForCheck();
    });
    if (!this.adminMode) this.accountState.loadAccounts();
  }

  selectAccount(account: AccountResponse) {
    this.accountState.selectAccount(account);
    this.isAccountMenuOpen = false;
  }

  createAccount() {
    if (this.isCreatingAccount) return;
    const accountName = window.prompt('Name this account', `Account ${this.accounts.length + 1}`)?.trim();
    if (!accountName) return;

    this.isCreatingAccount = true;
    const createRequest = this.accountState.createAccount(accountName);
    if (!createRequest) {
      this.isCreatingAccount = false;
      window.alert('Your client session is unavailable. Please sign in again before creating an account.');
      return;
    }

    createRequest.subscribe({
      next: (account) => {
        this.accountState.addAccount(account);
        this.isCreatingAccount = false;
        this.isAccountMenuOpen = false;
      },
      error: (error) => {
        console.error('Failed to create account:', error);
        this.isCreatingAccount = false;
      }
    });
  }

  manageAccounts() {
    this.isAccountMenuOpen = false;
    this.router.navigate(['/manage-accounts']);
  }

  navigateTo(section: string) {
    switch (section) {
      case 'dashboard':
        this.router.navigate(['/homepage']);
        break;
      case 'news':
        this.router.navigate(['/news']);
        break;
      case 'trading':
        this.router.navigate(['/trading']);
        break;
      case 'activity':
        this.router.navigate(['/activity']);
        break;
      case 'admin-dashboard':
      case 'admin-clients':
        this.router.navigate(['/admin-dashboard']);
        break;
      case 'admin-audit':
        this.router.navigate(['/admin-audit']);
        break;
      default:
        console.log('Unknown section:', section);
    }
  }
}
