import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-seller-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './seller-layout.html',
  styles: [`
    :host {
      display: block;
      height: 100vh;
      display: flex;
      flex-direction: column;
    }
  `]
})
export class SellerLayout implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  currentUser: any = null;
  dropdownOpen = false;
  sidebarOpen = false;

  ngOnInit() {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }

  toggleDropdown() {
    this.dropdownOpen = !this.dropdownOpen;
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  getInitials(): string {
    if (!this.currentUser) return '';
    const f = this.currentUser.firstName ? this.currentUser.firstName[0] : '';
    const l = this.currentUser.lastName ? this.currentUser.lastName[0] : '';
    const initials = (f + l).toUpperCase();
    return initials || 'U';
  }
}
