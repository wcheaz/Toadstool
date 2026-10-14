import { Component, signal, OnDestroy, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth.service';
import { ApiService } from './core/api/api.service';
import { getAccessToken } from './auth-storage';
import { SidebarComponent } from './homepage/components/sidebar/sidebar.component';
import { TopBarComponent } from './homepage/components/top-bar/top-bar.component';
import { ProfileComponent } from './profile/profile.component';
import { filter } from 'rxjs/operators';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

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
  private destroy$ = new Subject<void>();

  constructor(private authService: AuthService, private apiService: ApiService, private router: Router) {
    this.clientName = this.authService.getClientName();
  }

  ngOnInit() {
    // Check initial route
    this.updateLayoutVisibility(this.router.url);
    
    // Update layout visibility on route changes
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: any) => {
        this.updateLayoutVisibility(event.url);
      });

    interval(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.ensureSessionIsValid());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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

  private ensureSessionIsValid() {
    const accessToken = getAccessToken();
    if (!accessToken) {
      return;
    }

    this.apiService.validateToken(accessToken).subscribe({
      error: () => {
        this.authService.forceLogout();
      }
    });
  }
}
