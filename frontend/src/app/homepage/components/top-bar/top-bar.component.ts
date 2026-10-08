import { Component, Input } from '@angular/core';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-top-bar',
  standalone: true,
  imports: [],
  templateUrl: './top-bar.component.html',
  styleUrl: './top-bar.component.css'
})
export class TopBarComponent {
  @Input() clientName: string = '';

  constructor(private authService: AuthService) {}

  openProfile() {
    alert('Profile settings coming soon');
  }

  openNotifications() {
    alert('Notifications settings coming soon');
  }

  logout() {
    this.authService.logout();
  }
}
