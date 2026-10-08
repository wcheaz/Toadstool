import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { SidebarComponent } from './homepage/components/sidebar/sidebar.component';
import { TopBarComponent } from './homepage/components/top-bar/top-bar.component';
import { ProfileComponent } from './profile/profile.component';

@Component({
  imports: [RouterOutlet, SidebarComponent, TopBarComponent, ProfileComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
  clientName: string = '';
  isProfileModalOpen: boolean = false;

  constructor(private authService: AuthService) {
    this.clientName = this.authService.getClientName();
  }

  openProfileModal() {
    this.isProfileModalOpen = true;
  }

  closeProfileModal() {
    this.isProfileModalOpen = false;
  }
}
