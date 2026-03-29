import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { BuyerOfferItem } from '../../core/services/request.service';
import * as OffersActions from './store/offers.actions';
import * as OffersSelectors from './store/offers.selectors';

@Component({
  selector: 'app-buyer-offers',
  standalone: true,
  imports: [CommonModule, AsyncPipe, RouterLink],
  templateUrl: './buyer-offers.html'
})
export class BuyerOffers implements OnInit {

  private store = inject(Store);
  private sanitizer = inject(DomSanitizer);

  offers$: Observable<BuyerOfferItem[]>     = this.store.select(OffersSelectors.selectOffers);
  loading$: Observable<boolean>             = this.store.select(OffersSelectors.selectLoading);
  error$: Observable<string | null>         = this.store.select(OffersSelectors.selectError);
  totalPages$: Observable<number>           = this.store.select(OffersSelectors.selectTotalPages);
  totalElements$: Observable<number>        = this.store.select(OffersSelectors.selectTotalElements);
  currentPage$: Observable<number>          = this.store.select(OffersSelectors.selectCurrentPage);

  selectedStatus: string | null = null;
  selectedPeriod: string | null = null;
  pageSize = 10;
  currentPageSnapshot = 0;
  totalPagesSnapshot = 0;

  selectedOffer: BuyerOfferItem | null = null;
  selectedImageIndex = 0;
  sellerMapUrl: SafeResourceUrl | null = null;

  statusFilters = [
    { value: null,       label: 'Toutes' },
    { value: 'PENDING',  label: 'En attente' },
    { value: 'ACCEPTED', label: 'Acceptees' },
    { value: 'REJECTED', label: 'Refusees' }
  ];

  periodFilters = [
    { value: null,   label: 'Tout' },
    { value: '24h',  label: '24h' },
    { value: 'week', label: '7 jours' }
  ];

  ngOnInit(): void {
    this.currentPage$.subscribe(p => this.currentPageSnapshot = p);
    this.totalPages$.subscribe(t => this.totalPagesSnapshot = t);
    this.dispatchLoad(0);
  }

  private dispatchLoad(page: number): void {
    this.store.dispatch(OffersActions.loadOffers({
      status: this.selectedStatus,
      period: this.selectedPeriod,
      page,
      size: this.pageSize
    }));
  }

  onStatusFilter(status: string | null): void {
    this.selectedStatus = status;
    this.dispatchLoad(0);
  }

  onPeriodFilter(period: string | null): void {
    this.selectedPeriod = period;
    this.dispatchLoad(0);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPagesSnapshot) return;
    this.dispatchLoad(page);
  }

  acceptOffer(offerId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.store.dispatch(OffersActions.acceptOffer({ offerId }));
    if (this.selectedOffer?.offerId === offerId) {
      this.selectedOffer = { ...this.selectedOffer, offerStatus: 'ACCEPTED' };
    }
  }

  declineOffer(offerId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.store.dispatch(OffersActions.declineOffer({ offerId }));
    if (this.selectedOffer?.offerId === offerId) {
      this.selectedOffer = { ...this.selectedOffer, offerStatus: 'REJECTED' };
    }
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
      PENDING:   'bg-amber-50 text-amber-700 border border-amber-200',
      ACCEPTED:  'bg-emerald-50 text-emerald-700 border border-emerald-200',
      REJECTED:  'bg-red-50 text-red-700 border border-red-200',
      CANCELLED: 'bg-gray-100 text-gray-500 border border-gray-200'
    };
    return classes[status] || 'bg-gray-100 text-gray-700 border border-gray-200';
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const start = Math.max(0, this.currentPageSnapshot - 2);
    const end = Math.min(this.totalPagesSnapshot, start + 5);
    for (let i = start; i < end; i++) pages.push(i);
    return pages;
  }
}
