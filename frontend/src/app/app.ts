import { Component, signal, OnDestroy, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth.service';
import { SidebarComponent } from './homepage/components/sidebar/sidebar.component';
import { TopBarComponent } from './homepage/components/top-bar/top-bar.component';
import { ProfileComponent } from './profile/profile.component';
import { isAuthenticated } from './auth-storage';
import { filter } from 'rxjs/operators';

@Component({
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopBarComponent, ProfileComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('frontend');
  clientName: string = '';
  isProfileModalOpen: boolean = false;
  isShowingMainLayout: boolean = false;
  private readonly handlePageShow = (event: PageTransitionEvent): void => {
    if (event.persisted && this.isProtectedRoute(window.location.pathname) && !isAuthenticated()) {
      window.location.replace('/login');
    }
  };

  constructor(private authService: AuthService, private router: Router) {
    this.clientName = this.authService.getClientName();
  }

  ngOnInit() {
    window.addEventListener('pageshow', this.handlePageShow);

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

  ngOnDestroy(): void {
    window.removeEventListener('pageshow', this.handlePageShow);
  }

  private updateLayoutVisibility(url: string) {
    const mainLayoutPages = ['/homepage', '/news', '/activity', '/trading'];
    this.isShowingMainLayout = mainLayoutPages.some(page => url.startsWith(page));
  }

  private isProtectedRoute(pathname: string): boolean {
    const protectedRoutes = [
      '/homepage',
      '/news',
      '/activity',
      '/trading',
      '/admin-dashboard',
      '/admin-client/',
      '/admin-audit'
    ];
    return protectedRoutes.some(route => pathname === route || pathname.startsWith(route));
  }

  openProfileModal() {
    this.isProfileModalOpen = true;
  }

  closeProfileModal() {
    this.isProfileModalOpen = false;
  }
}
