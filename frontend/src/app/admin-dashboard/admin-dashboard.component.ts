import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CLIENTS, ClientRecord } from './client-data';
import { SidebarComponent } from '../homepage/components/sidebar/sidebar.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent {
  searchTerm = '';
  appliedSearchTerm = '';
  selectedClient = 'All clients';
  readonly adminName: string;

  readonly clients = CLIENTS;

  constructor(private authService: AuthService, private router: Router) {
    this.adminName = this.authService.getClientName() || 'Sarah Jenkins';
  }

  get filteredClients(): ClientRecord[] {
    const term = this.appliedSearchTerm.trim().toLowerCase();
    return this.clients.filter((client) => {
      const matchesSearch = !term || `${client.name} ${client.username} ${client.account}`.toLowerCase().includes(term);
      const matchesClient = this.selectedClient === 'All clients' || client.name === this.selectedClient;
      return matchesSearch && matchesClient;
    });
  }

  applySearch(): void {
    this.appliedSearchTerm = this.searchTerm.trim();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.appliedSearchTerm = '';
  }

  logout(): void {
    this.authService.logout();
  }

  viewDetails(client: ClientRecord): void {
    this.router.navigate(['/admin-client', client.account]);
  }

  goTo(section: string): void {
    if (section === 'dashboard' || section === 'clients') {
      this.router.navigate(['/admin-dashboard']);
    }
  }
}