import { ChangeDetectorRef, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { interval, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { AuthService } from './core/auth.service';
import { ApiService } from './core/api/api.service';
import { getAccessToken, getRefreshToken, getRefreshTokenExpiresAt, isAuthenticated } from './auth-storage';
import { SidebarComponent } from './homepage/components/sidebar/sidebar.component';
import { TopBarComponent } from './homepage/components/top-bar/top-bar.component';
import { ProfileComponent } from './profile/profile.component';
import { ThemeToggleComponent } from './components/theme-toggle/theme-toggle.component';

const SESSION_WARNING_LEAD_TIME_MS = 5 * 60 * 1000;
const SESSION_EXTENSION_MINUTES = 15;

@Component({
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopBarComponent, ProfileComponent, ThemeToggleComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('frontend');
  clientName: string = '';
  isProfileModalOpen: boolean = false;
  isShowingMainLayout: boolean = false;
  isSessionRenewalModalOpen: boolean = false;
  isRefreshingSession: boolean = false;
  sessionRenewalMessage: string = 'Your session is about to expire. Stay signed in for another 15 minutes?';
  private destroy$ = new Subject<void>();
  private sessionWarningTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private sessionExpiryTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private trackedRefreshToken: string | null = null;
  private trackedRefreshTokenExpiresAt: number | null = null;
  private readonly handleAuthSessionChanged = (): void => {
    this.syncSessionTimers();
  };

  private readonly handlePageShow = (event: PageTransitionEvent): void => {
    if (event.persisted && this.isProtectedRoute(window.location.pathname) && !isAuthenticated()) {
      window.location.replace('/login');
    }
  };

  constructor(
    private authService: AuthService,
    private apiService: ApiService,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.clientName = this.authService.getClientName();
  }

  ngOnInit() {
    window.addEventListener('pageshow', this.handlePageShow);

    // Check initial route
    this.updateLayoutVisibility(this.router.url);
    this.syncSessionTimers();

    window.addEventListener('auth-session-changed', this.handleAuthSessionChanged);
    
    // Update layout visibility on route changes
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: any) => {
        this.updateLayoutVisibility(event.url);
      });

    interval(60000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.ensureSessionIsValid();
        this.syncSessionTimers();
      });
  }

  ngOnDestroy(): void {
    window.removeEventListener('auth-session-changed', this.handleAuthSessionChanged);
    window.removeEventListener('pageshow', this.handlePageShow);
    this.clearSessionTimers();
    this.destroy$.next();
    this.destroy$.complete();
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

  staySignedIn() {
    if (this.isRefreshingSession) {
      return;
    }

    this.isRefreshingSession = true;
    this.isSessionRenewalModalOpen = false;
    this.refreshView();
    this.clearSessionTimers();
    this.authService.refreshSession().subscribe({
      next: () => {
        this.isRefreshingSession = false;
        this.syncSessionTimers();
      },
      error: (error) => {
        console.error('Failed to refresh the session:', error);
        this.isRefreshingSession = false;
        this.refreshView();
        this.authService.forceLogout();
      }
    });
  }

  signOutNow() {
    this.isSessionRenewalModalOpen = false;
    this.clearSessionTimers();
    this.refreshView();
    this.authService.logout(getAccessToken());
  }

  private ensureSessionIsValid() {
    if (this.isSessionRenewalModalOpen || this.isRefreshingSession) {
      return;
    }

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

  private syncSessionTimers() {
    const accessToken = getAccessToken();
    const refreshTokenExpiresAt = getRefreshTokenExpiresAt();

    if (!accessToken || !refreshTokenExpiresAt) {
      this.clearSessionTimers();
      this.isSessionRenewalModalOpen = false;
      this.isRefreshingSession = false;
      this.refreshView();
      return;
    }

    const now = Date.now();
    const expiryDelayMs = refreshTokenExpiresAt - now;
    if (expiryDelayMs <= 0) {
      this.handleSessionExpiry();
      return;
    }

    const refreshToken = getRefreshToken();
    if (this.trackedRefreshToken === refreshToken && this.trackedRefreshTokenExpiresAt === refreshTokenExpiresAt) {
      if (!this.isSessionRenewalModalOpen && expiryDelayMs <= SESSION_WARNING_LEAD_TIME_MS) {
        this.openSessionRenewalModal();
      }
      return;
    }

    this.clearSessionTimers();
    this.trackedRefreshToken = refreshToken;
    this.trackedRefreshTokenExpiresAt = refreshTokenExpiresAt;

    const warningDelayMs = Math.max(expiryDelayMs - SESSION_WARNING_LEAD_TIME_MS, 0);
    this.sessionWarningTimeoutId = window.setTimeout(() => {
      this.openSessionRenewalModal();
    }, warningDelayMs);
    this.sessionExpiryTimeoutId = window.setTimeout(() => {
      this.handleSessionExpiry();
    }, expiryDelayMs);

    if (warningDelayMs === 0) {
      this.openSessionRenewalModal();
    }
  }

  private openSessionRenewalModal() {
    if (!getAccessToken()) {
      return;
    }

    this.isSessionRenewalModalOpen = true;
    this.sessionRenewalMessage = `Your session expires in about 5 minutes. Stay signed in for another ${SESSION_EXTENSION_MINUTES} minutes?`;
    this.refreshView();
  }

  private handleSessionExpiry() {
    this.isSessionRenewalModalOpen = false;
    this.isRefreshingSession = false;
    this.clearSessionTimers();
    this.refreshView();
    this.authService.forceLogout();
  }

  private clearSessionTimers() {
    if (this.sessionWarningTimeoutId !== null) {
      clearTimeout(this.sessionWarningTimeoutId);
      this.sessionWarningTimeoutId = null;
    }
    if (this.sessionExpiryTimeoutId !== null) {
      clearTimeout(this.sessionExpiryTimeoutId);
      this.sessionExpiryTimeoutId = null;
    }
    this.trackedRefreshToken = null;
    this.trackedRefreshTokenExpiresAt = null;
  }

  private refreshView() {
    this.changeDetectorRef.detectChanges();
  }
}
