import { Component, OnInit, OnDestroy, Renderer2, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService, CurrentUser } from '../core/services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './landing.component.html'
})
export class LandingPageComponent implements OnInit, OnDestroy {

  currentUser: CurrentUser | null = null;
  dropdownOpen = false;
  isSearchOpen = false;
  rayon = 10;
  searchLocation = '';
  detectingLocation = false;

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

  detectLocation(): void {
    if (!navigator.geolocation) {
      alert('La géolocalisation n\'est pas supportée par votre navigateur.');
      return;
    }

    this.detectingLocation = true;
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.detectingLocation = false;
        const lat = position.coords.latitude.toFixed(6);
        const lon = position.coords.longitude.toFixed(6);
        this.searchLocation = `${lat}, ${lon}`;
      },
      (error) => {
        this.detectingLocation = false;
        console.error('Error detecting location:', error);
        alert('Impossible de détecter votre position. Veuillez l\'entrer manuellement.');
      },
      { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
    );
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  openSearch(): void {
    this.isSearchOpen = true;
    this.dropdownOpen = false; // Close auth dropdown if open
    this.renderer.addClass(document.body, 'overflow-hidden');
  }

  closeSearch(): void {
    this.isSearchOpen = false;
    this.renderer.removeClass(document.body, 'overflow-hidden');
  }

  setRayon(value: number): void {
    this.rayon = value;
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
