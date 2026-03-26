import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RequestService, BuyerDemandDetails } from '../core/services/request.service';

@Component({
  selector: 'app-demand-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './demand-details.html'
})
export class DemandDetails implements OnInit {
  private route = inject(ActivatedRoute);
  private requestService = inject(RequestService);
  private sanitizer = inject(DomSanitizer);

  demand: BuyerDemandDetails | null = null;
  loading = true;
  errorMessage = '';
  actionLoading: Record<number, boolean> = {};
  actionSuccess: Record<number, string> = {};
  selectedOffer: any = null;
  selectedImageIndex = 0;
  sellerMapUrl: SafeResourceUrl | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadDemand(+id);
    } else {
      this.errorMessage = 'ID de demande invalide.';
      this.loading = false;
    }
  }

  loadDemand(id: number): void {
    this.loading = true;
    this.requestService.getDemandDetails(id).subscribe({
      next: (res) => {
        this.demand = res;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = "Impossible de charger les details de la demande. Vous n'avez peut-etre pas acces.";
      }
    });
  }

  acceptOffer(offerId: number): void {
    this.actionLoading[offerId] = true;
    this.requestService.buyerAcceptOffer(offerId).subscribe({
      next: () => {
        this.actionLoading[offerId] = false;
        this.actionSuccess[offerId] = 'accepted';
        if (this.demand) {
          const offer = this.demand.offers?.find(o => o.id === offerId);
          if (offer) offer.status = 'ACCEPTED';
          this.demand.status = 'RESOLVED';
        }
      },
      error: () => {
        this.actionLoading[offerId] = false;
      }
    });
  }

  declineOffer(offerId: number): void {
    this.actionLoading[offerId] = true;
    this.requestService.buyerDeclineOffer(offerId).subscribe({
      next: () => {
        this.actionLoading[offerId] = false;
        this.actionSuccess[offerId] = 'declined';
        if (this.demand) {
          const offer = this.demand.offers?.find(o => o.id === offerId);
          if (offer) offer.status = 'REJECTED';
        }
      },
      error: () => {
        this.actionLoading[offerId] = false;
      }
    });
  }

  openOfferDetail(offer: any): void {
    this.selectedOffer = offer;
    this.selectedImageIndex = 0;
    if (offer.sellerLatitude && offer.sellerLongitude) {
      const lon = offer.sellerLongitude;
      const lat = offer.sellerLatitude;
      const url = `https://www.openstreetmap.org/export/embed.html?bbox=${lon - 0.02},${lat - 0.01},${lon + 0.02},${lat + 0.01}&layer=mapnik&marker=${lat},${lon}`;
      this.sellerMapUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    } else {
      this.sellerMapUrl = null;
    }
  }

  closeOfferDetail(): void {
    this.selectedOffer = null;
  }

  prevImage(): void {
    if (this.selectedOffer && this.selectedImageIndex > 0) {
      this.selectedImageIndex--;
    }
  }

  nextImage(): void {
    if (this.selectedOffer && this.selectedImageIndex < (this.selectedOffer.sellerStoreImages?.length || 0) - 1) {
      this.selectedImageIndex++;
    }
  }

  sellerTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      COMPANY: 'Entreprise',
      LOCAL_STORE: 'Magasin local',
      AUTO_ENTREPRENEUR: 'Auto-entrepreneur',
      CASUAL: 'Particulier'
    };
    return labels[type] || type;
  }

  offerStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente',
      ACCEPTED: 'Acceptée',
      REJECTED: 'Refusée',
      CANCELLED: 'Annulée'
    };
    return labels[status] || status;
  }

  offerStatusClasses(status: string): string {
    const classes: Record<string, string> = {
      PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
      ACCEPTED: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
      REJECTED: 'bg-red-50 text-red-700 border border-red-200',
      CANCELLED: 'bg-gray-100 text-gray-500 border border-gray-200'
    };
    return classes[status] || 'bg-gray-100 text-gray-700 border border-gray-200';
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente',
      OFFERS_RECEIVED: 'Offres reçues',
      RESOLVED: 'Résolue',
      EXPIRED: 'Expirée'
    };
    return labels[status] || status;
  }

  statusClasses(status: string): string {
    const classes: Record<string, string> = {
      PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
      OFFERS_RECEIVED: 'bg-sky-50 text-sky-700 border border-sky-200',
      RESOLVED: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
      EXPIRED: 'bg-gray-100 text-gray-600 border border-gray-200'
    };
    return classes[status] || 'bg-gray-100 text-gray-700 border border-gray-200';
  }
}

