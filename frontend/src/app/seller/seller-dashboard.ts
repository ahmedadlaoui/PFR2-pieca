import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

interface NearbyDemand {
  id: number;
  title: string;
  description: string;
  categoryId: number | null;
  categoryName: string | null;
  buyerFirstName: string | null;
  buyerLastName: string | null;
  buyerPhone: string | null;
  distanceKm: number | null;
  status: string;
  imageUrl: string | null;
  createdAt: string;
}

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seller-dashboard.html',
  styles: [
    `
      .pages-container {
        overflow: hidden;
        width: 100%;
      }

      .pages-track {
        display: flex;
        width: 300%;
        transition: transform 360ms ease;
      }

      .page {
        width: 33.333333%;
        flex: 0 0 33.333333%;
      }
    `
  ]
})
export class SellerDashboard implements OnInit {
  demands: NearbyDemand[] = [];
  acceptedDemands: NearbyDemand[] = [];
  acceptedDemandIds = new Set<number>();
  
  isLoading = false;
  isAccepting = false;
  isLoadingAccepted = false;
  
  mapUrl?: SafeResourceUrl;
  hasSellerPosition = false;
  hasSearched = false;
  selectedDemand: NearbyDemand | null = null;
  actionMessage = '';
  
  activePage = 0; // For "recherche" slider
  currentTab: 'recherche' | 'attente' = 'recherche'; // Main views
  
  currentLat: number | null = null;
  currentLon: number | null = null;
  searchRadius = 12;
  private profileFetchRetries = 0;
  private readonly maxProfileFetchRetries = 5;

  constructor(private http: HttpClient, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.fetchSellerProfile(false);
    this.fetchAcceptedDemands();
  }

  get trackTransform(): string {
    return `translateX(-${this.activePage * 33.333333}%)`;
  }

  showToast(message: string): void {
    this.actionMessage = message;
    setTimeout(() => {
      if (this.actionMessage === message) {
        this.actionMessage = '';
      }
    }, 4000);
  }

  switchTab(tab: 'recherche' | 'attente'): void {
    this.currentTab = tab;
    if (tab === 'attente') {
      this.fetchAcceptedDemands();
    }
  }

  fetchAcceptedDemands(): void {
    this.isLoadingAccepted = true;
    this.http.get<any>('/api/v1/requests/accepted?page=0&size=50').subscribe({
      next: (res) => {
        this.acceptedDemands = res.content || [];
        this.acceptedDemands.forEach(d => this.acceptedDemandIds.add(d.id));
        this.isLoadingAccepted = false;
      },
      error: (err) => {
        console.error('Failed to fetch accepted demands', err);
        // Fallback or ignore
        this.isLoadingAccepted = false;
      }
    });
  }

  fetchSellerProfile(searchAfterLoad: boolean): void {
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
        if (this.hasSellerPosition && searchAfterLoad) {
          this.searchNearbyRequests();
        } else {
          this.isLoading = false;
        }
      },
      error: (err) => {
        console.error('Failed to fetch seller profile', err);
        if (this.profileFetchRetries < this.maxProfileFetchRetries) {
          this.profileFetchRetries += 1;
          setTimeout(() => this.fetchSellerProfile(searchAfterLoad), 1500);
          return;
        }
        this.isLoading = false;
      }
    });
  }

  onSearchClientsClick(): void {
    this.hasSearched = true;
    this.actionMessage = '';
    this.selectedDemand = null;
    this.fetchSellerProfile(true);
  }

  backToMapPage(): void {
    this.activePage = 0;
    this.selectedDemand = null;
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
      this.fetchSellerProfile(true);
      return;
    }

    this.isLoading = true;
    this.http.get<any>('/api/v1/requests/nearby?page=0&size=20')
      .subscribe({
        next: (res) => {
          this.demands = res.content || [];
          // Add a small artificial delay for the loading animation to be visible
          setTimeout(() => {
            this.isLoading = false;
            this.activePage = 1;
          }, 800);
        },
        error: (err) => {
          console.error('Error fetching nearby requests', err);
          this.demands = [];
          this.isLoading = false;
        }
      });
  }

  viewDemandDetails(demand: NearbyDemand): void {
    this.selectedDemand = demand;
    this.activePage = 2;
  }

  backToDemandsList(): void {
    this.selectedDemand = null;
    this.activePage = 1;
  }

  acceptDemand(): void {
    if (!this.selectedDemand || this.isAccepting) {
      return;
    }

    const acceptedId = this.selectedDemand.id;
    this.isAccepting = true;
    
    this.http.post(`/api/v1/requests/${acceptedId}/accept`, {}).subscribe({
      next: () => {
        this.isAccepting = false;
        this.acceptedDemandIds.add(acceptedId);
        // Refresh accepted demands in background
        this.fetchAcceptedDemands();
        this.showToast('Demande acceptée avec succès (' + acceptedId + '). Consultez vos Demandes en attente.');
      },
      error: (err) => {
        console.error('Erreur accept', err);
        this.isAccepting = false;
        // Even if error (maybe already accepted), let's mark it
        if (err.status === 400 && err.error?.message?.includes('deja')) {
          this.acceptedDemandIds.add(acceptedId);
          this.showToast('Vous avez déjà accepté cette demande.');
        } else {
          this.showToast('Erreur lors de l\'acceptation de la demande.');
        }
      }
    });
  }

  declineDemand(): void {
    if (!this.selectedDemand) {
      return;
    }

    const declinedId = this.selectedDemand.id;
    this.demands = this.demands.filter(d => d.id !== declinedId);
    this.selectedDemand = null;
    this.activePage = 1; // return to list naturally
    this.showToast('La demande a été masquée de votre vue.');
  }
}
