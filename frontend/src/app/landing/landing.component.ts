import { Component, OnInit, OnDestroy, Renderer2, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { AuthService, CurrentUser } from '../core/services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html'
})
export class LandingPageComponent implements OnInit, OnDestroy {

  currentUser: CurrentUser | null = null;
  dropdownOpen = false;

  private userSub!: Subscription;
  private renderer = inject(Renderer2);

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }

  ngOnDestroy(): void {
    this.userSub.unsubscribe();
    this.renderer.removeClass(document.body, 'overflow-hidden');
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  get isSeller(): boolean {
    return this.currentUser?.role === 'SELLER';
  }

  get canCreateDemand(): boolean {
    return !this.currentUser || this.currentUser.role === 'BUYER';
  }

  get canBecomeSeller(): boolean {
    return !this.currentUser || this.currentUser.role === 'BUYER';
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  goToDemandPage(event?: Event): void {
    event?.preventDefault();

    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/buyer/demand/new' } });
      return;
    }

    if (this.currentUser?.role === 'SELLER') {
      this.router.navigate(['/seller/dashboard']);
      return;
    }

    this.router.navigate(['/buyer/demand/new']);
  }

  goToDashboard(event?: Event): void {
    event?.preventDefault();
    this.closeDropdown();

    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    if (this.currentUser?.role === 'SELLER') {
      this.router.navigate(['/seller/dashboard']);
      return;
    }

    this.router.navigate(['/buyer/dashboard']);
  }

  logout(): void {
    this.authService.logout();
    this.dropdownOpen = false;
    this.router.navigate(['/']);
  }

  getInitials(): string {
    if (!this.currentUser) return '';
    const first = this.currentUser.firstName?.charAt(0) || '';
    const last = this.currentUser.lastName?.charAt(0) || '';
    return (first + last).toUpperCase();
  }
}
