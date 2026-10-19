import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { AdminLoginComponent } from './admin-login/admin-login.component';
import { HomepageComponent } from './homepage/homepage.component';
import { authGuard } from './auth.guard';
import { NewsComponent } from './news/news.component';
import { ActivityComponent } from './activity/activity.component';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';
import { AdminClientDetailsComponent } from './admin-dashboard/admin-client-details.component';
import { TradingComponent } from './trading/trading.component';
import { AdminAuditComponent } from './admin-audit/admin-audit.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'admin-login', component: AdminLoginComponent },
  { path: 'admin-dashboard', component: AdminDashboardComponent, canActivate: [authGuard] },
  { path: 'admin-client/:clientId', component: AdminClientDetailsComponent, canActivate: [authGuard] },
  { path: 'admin-audit', component: AdminAuditComponent, canActivate: [authGuard] },
  { path: 'homepage', component: HomepageComponent, canActivate: [authGuard] },
  { path: 'news', component: NewsComponent, canActivate: [authGuard] },
  { path: 'activity', component: ActivityComponent, canActivate: [authGuard] },
  { path: 'trading', component: TradingComponent, canActivate: [authGuard] }
];
