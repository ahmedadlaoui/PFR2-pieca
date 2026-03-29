import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RequestService, SellerRequestItem, PagedResponse } from '../../core/services/request.service';

@Component({
  selector: 'app-seller-pending-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './seller-pending-requests.html'
})
export class SellerPendingRequests implements OnInit {
  private requestService = inject(RequestService);

  requests: SellerRequestItem[] = [];
  isLoading = false;
  isCancelling = false;
  errorMessage = '';
  toastMessage = '';

  selectedRequest: SellerRequestItem | null = null;

  statusFilter: string = 'PENDING';
  currentPage = 0;
  pageSize = 15;
  totalElements = 0;
  totalPages = 0;

  readonly statusOptions = [
    { value: '', label: 'Tous les statuts' },
    { value: 'PENDING', label: 'En attente' },
    { value: 'ACCEPTED', label: 'Acceptees' },
    { value: 'REJECTED', label: 'Rejetees' },
    { value: 'CANCELLED', label: 'Annulees' }
  ];

  ngOnInit(): void {
    this.loadOffers();
  }

  loadOffers(): void {
    this.isLoading = true;
    this.errorMessage = '';
    const status = this.statusFilter || null;
    this.requestService.getSellerOffers(status, this.currentPage, this.pageSize).subscribe({
      next: (res: PagedResponse<SellerRequestItem>) => {
        this.requests = res.content || [];
        this.totalElements = res.totalElements;
        this.totalPages = res.totalPages;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger vos demandes.';
        this.isLoading = false;
      }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.selectedRequest = null;
    this.loadOffers();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.selectedRequest = null;
    this.loadOffers();
  }

  selectRequest(req: SellerRequestItem): void {
    this.selectedRequest = this.selectedRequest?.offerId === req.offerId ? null : req;
  }

  closeDetail(): void {
    this.selectedRequest = null;
  }

  cancelOffer(req: SellerRequestItem): void {
    if (this.isCancelling || req.offerStatus !== 'PENDING') return;
    this.isCancelling = true;
    this.requestService.cancelOffer(req.requestId).subscribe({
      next: () => {
        this.isCancelling = false;
        this.selectedRequest = null;
        this.showToast('Offre annulee avec succes.');
        this.loadOffers();
      },
      error: () => {
        this.isCancelling = false;
        this.showToast('Erreur lors de l\'annulation.');
      }
    });
  }

  showToast(msg: string): void {
    this.toastMessage = msg;
    setTimeout(() => { if (this.toastMessage === msg) this.toastMessage = ''; }, 4000);
  }

  offerStatusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'En attente',
      ACCEPTED: 'Acceptee',
      REJECTED: 'Rejetee',
      CANCELLED: 'Annulee'
    };
    return map[status] || status;
  }

  offerStatusClasses(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
      ACCEPTED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      REJECTED: 'bg-red-50 text-red-700 border-red-200',
      CANCELLED: 'bg-gray-100 text-gray-500 border-gray-200'
    };
    return map[status] || 'bg-gray-100 text-gray-600 border-gray-200';
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages, start + 5);
    for (let i = start; i < end; i++) pages.push(i);
    return pages;
  }
}
