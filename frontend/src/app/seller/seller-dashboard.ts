import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seller-dashboard.html'
})
export class SellerDashboard implements OnInit {
  demands: any[] = [];
  isLoading = false;
  mapUrl?: SafeResourceUrl;
  hasSellerPosition = false;
  
  currentLat: number | null = null;
  currentLon: number | null = null;
  searchRadius = 12;
  private profileFetchRetries = 0;
  private readonly maxProfileFetchRetries = 5;

  constructor(private http: HttpClient, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.fetchSellerProfileAndRequests();
  }

  fetchSellerProfileAndRequests(): void {
    this.isLoading = true;
    this.http.get<any>('/api/v1/sellers/me').subscribe({
      next: (profile) => {
        if (profile.latitude !== null && profile.latitude !== undefined && profile.longitude !== null && profile.longitude !== undefined) {
          this.currentLat = profile.latitude;
          this.currentLon = profile.longitude;
          this.hasSellerPosition = true;
          this.profileFetchRetries = 0;
        }
        if (profile.activeRadiusKm !== null && profile.activeRadiusKm !== undefined) {
          this.searchRadius = profile.activeRadiusKm;
        }
        this.updateMapUrl();
        if (this.hasSellerPosition) {
          this.searchNearbyRequests();
        } else {
          this.isLoading = false;
        }
      },
      error: (err) => {
        console.error('Failed to fetch seller profile', err);
        if (this.profileFetchRetries < this.maxProfileFetchRetries) {
          this.profileFetchRetries += 1;
          setTimeout(() => this.fetchSellerProfileAndRequests(), 1500);
          return;
        }
        this.isLoading = false;
      }
    });
  }

  private updateMapUrl(): void {
    if (this.currentLat === null || this.currentLon === null) {
      this.mapUrl = undefined;
      return;
    }

    const lon = this.currentLon;
    const lat = this.currentLat;
    const coordDiff = 0.05;
    const bbox = `${lon - coordDiff},${lat - coordDiff},${lon + coordDiff},${lat + coordDiff}`;
    const url = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat},${lon}`;
    this.mapUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  searchNearbyRequests(): void {
    if (!this.hasSellerPosition) {
      this.fetchSellerProfileAndRequests();
      return;
    }

    this.isLoading = true;
    this.http.get<any>('/api/v1/requests/nearby?page=0&size=20')
      .subscribe({
        next: (res) => {
          this.demands = res.content || [];
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error fetching nearby requests', err);
          this.demands = [];
          this.isLoading = false;
        }
      });
  }
}
