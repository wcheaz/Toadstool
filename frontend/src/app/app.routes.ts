import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { AdminLoginComponent } from './admin-login/admin-login.component';
import { HomepageComponent } from './homepage/homepage.component';
import { authGuard } from './auth.guard';
import { NewsComponent } from './news/news.component';
import { ActivityComponent } from './activity/activity.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'admin-login', component: AdminLoginComponent },
  { path: 'homepage', component: HomepageComponent, canActivate: [authGuard] },
  { path: 'news', component: NewsComponent },
  { path: 'activity', component: ActivityComponent }
];
