import { Component, Output, EventEmitter } from '@angular/core';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-top-bar',
  standalone: true,
  imports: [],
  templateUrl: './top-bar.component.html',
  styleUrl: './top-bar.component.css'
})
export class TopBarComponent {
  clientName: string = '';
  @Output() profileClicked = new EventEmitter<void>();

  constructor(private authService: AuthService) {
    // Get display name from auth service
    this.clientName = this.authService.getClientName();
  }

  openProfile() {
    this.profileClicked.emit();
  }

  openNotifications() {
    alert('Notifications settings coming soon');
  }

  logout() {
    this.authService.logout();
  }
}
