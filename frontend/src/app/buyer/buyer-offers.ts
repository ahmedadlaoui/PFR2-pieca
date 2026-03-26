import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RequestService, BuyerOfferItem, PagedResponse } from '../core/services/request.service';

@Component({
  selector: 'app-buyer-offers',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './buyer-offers.html'
})
export class BuyerOffers implements OnInit {
  private requestService = inject(RequestService);
  private sanitizer = inject(DomSanitizer);

  offers: BuyerOfferItem[] = [];
  loading = true;
  errorMessage = '';

  // Filters
  selectedStatus: string | null = null;
  selectedPeriod: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  // Actions
  actionLoading: Record<number, boolean> = {};

  // Detail modal
  selectedOffer: BuyerOfferItem | null = null;
  selectedImageIndex = 0;
  sellerMapUrl: SafeResourceUrl | null = null;

  statusFilters = [
    { value: null, label: 'Toutes' },
    { value: 'PENDING', label: 'En attente' },
    { value: 'ACCEPTED', label: 'Acceptees' },
    { value: 'REJECTED', label: 'Refusees' }
  ];

  periodFilters = [
    { value: null, label: 'Tout' },
    { value: '24h', label: '24h' },
    { value: 'week', label: '7 jours' }
  ];

  ngOnInit(): void {
    this.loadOffers();
  }

  loadOffers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.requestService.getBuyerOffers(this.selectedStatus, this.selectedPeriod, this.currentPage, this.pageSize).subscribe({
      next: (res: PagedResponse<BuyerOfferItem>) => {
        this.offers = res.content;
        this.totalElements = res.totalElements;
        this.totalPages = res.totalPages;
        this.currentPage = res.number;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Impossible de charger vos offres.';
      }
    });
  }

  onStatusFilter(status: string | null): void {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.loadOffers();
  }

  onPeriodFilter(period: string | null): void {
    this.selectedPeriod = period;
    this.currentPage = 0;
    this.loadOffers();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadOffers();
  }

  acceptOffer(offerId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.actionLoading[offerId] = true;
    this.requestService.buyerAcceptOffer(offerId).subscribe({
      next: () => {
        this.actionLoading[offerId] = false;
        const offer = this.offers.find(o => o.offerId === offerId);
        if (offer) offer.offerStatus = 'ACCEPTED';
        if (this.selectedOffer && this.selectedOffer.offerId === offerId) {
          this.selectedOffer.offerStatus = 'ACCEPTED';
        }
      },
      error: () => { this.actionLoading[offerId] = false; }
    });
  }

  declineOffer(offerId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.actionLoading[offerId] = true;
    this.requestService.buyerDeclineOffer(offerId).subscribe({
      next: () => {
        this.actionLoading[offerId] = false;
        const offer = this.offers.find(o => o.offerId === offerId);
        if (offer) offer.offerStatus = 'REJECTED';
        if (this.selectedOffer && this.selectedOffer.offerId === offerId) {
          this.selectedOffer.offerStatus = 'REJECTED';
        }
      },
      error: () => { this.actionLoading[offerId] = false; }
    });
  }

  openDetail(offer: BuyerOfferItem): void {
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

  closeDetail(): void {
    this.selectedOffer = null;
  }

  prevImage(): void {
    if (this.selectedImageIndex > 0) this.selectedImageIndex--;
  }

  nextImage(): void {
    if (this.selectedOffer && this.selectedImageIndex < (this.selectedOffer.sellerStoreImages?.length || 0) - 1) {
      this.selectedImageIndex++;
    }
  }

  sellerTypeLabel(type: string | null): string {
    if (!type) return 'Non specifie';
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
      ACCEPTED: 'Acceptee',
      REJECTED: 'Refusee',
      CANCELLED: 'Annulee'
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

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages, start + 5);
    for (let i = start; i < end; i++) pages.push(i);
    return pages;
  }
}
