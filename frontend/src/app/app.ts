import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth.service';
import { SidebarComponent } from './homepage/components/sidebar/sidebar.component';
import { TopBarComponent } from './homepage/components/top-bar/top-bar.component';
import { ProfileComponent } from './profile/profile.component';
import { filter } from 'rxjs/operators';

@Component({
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopBarComponent, ProfileComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit {
  protected readonly title = signal('frontend');
  clientName: string = '';
  isProfileModalOpen: boolean = false;
  isShowingMainLayout: boolean = false;

  constructor(private authService: AuthService, private router: Router) {
    this.clientName = this.authService.getClientName();
  }

  ngOnInit() {
    // Check initial route
    this.updateLayoutVisibility(this.router.url);
    
    // Update layout visibility on route changes
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd)
      )
      .subscribe((event: any) => {
        this.updateLayoutVisibility(event.url);
      });
  }

  private updateLayoutVisibility(url: string) {
    // Hide sidebar and top-bar for auth pages
    const authPages = ['/login', '/register', '/admin-login'];
    this.isShowingMainLayout = !authPages.some(page => url.startsWith(page));
  }

  openProfileModal() {
    this.isProfileModalOpen = true;
  }

  closeProfileModal() {
    this.isProfileModalOpen = false;
  }
}
