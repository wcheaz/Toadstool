import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  @Input() adminMode = false;
  /** Highlights the matching admin nav item, e.g. 'admin-dashboard' or 'admin-audit'. */
  @Input() activeSection = '';

  constructor(private router: Router) {}

  navigateTo(section: string) {
    switch (section) {
      case 'dashboard':
        this.router.navigate(['/homepage']);
        break;
      case 'news':
        this.router.navigate(['/news']);
        break;
      case 'trading':
        this.router.navigate(['/trading']);
        break;
      case 'activity':
        this.router.navigate(['/activity']);
        break;
      case 'settings':
        this.router.navigate(['/settings']);
        break;
      case 'support':
        this.router.navigate(['/support']);
        break;
      case 'admin-dashboard':
      case 'admin-clients':
        this.router.navigate(['/admin-dashboard']);
        break;
      case 'admin-audit':
        this.router.navigate(['/admin-audit']);
        break;
      default:
        console.log('Unknown section:', section);
    }
  }
}
