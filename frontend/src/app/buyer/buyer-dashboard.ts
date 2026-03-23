import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RequestService, BuyerDemandItem } from '../core/services/request.service';

@Component({
  selector: 'app-buyer-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './buyer-dashboard.html'
})
export class BuyerDashboard implements OnInit {
  private readonly requestService = inject(RequestService);

  demands: BuyerDemandItem[] = [];
  loading = false;
  errorMessage = '';

  readonly statuses = ['ALL', 'PENDING', 'OFFERS_RECEIVED', 'RESOLVED', 'EXPIRED'];
  selectedStatus = 'ALL';

  page = 0;
  readonly pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  ngOnInit(): void {
    this.loadDemands();
  }

  loadDemands(): void {
    this.loading = true;
    this.errorMessage = '';

    const status = this.selectedStatus === 'ALL' ? null : this.selectedStatus;
    this.requestService.getMyDemands(status, this.page, this.pageSize).subscribe({
      next: (res) => {
        this.demands = res.content;
        this.page = res.number;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.message || 'Impossible de charger vos demandes.';
      }
    });
  }

  onStatusChange(value: string): void {
    this.selectedStatus = value;
    this.page = 0;
    this.loadDemands();
  }

  previousPage(): void {
    if (this.page <= 0) {
      return;
    }
    this.page -= 1;
    this.loadDemands();
  }

  nextPage(): void {
    if (this.page + 1 >= this.totalPages) {
      return;
    }
    this.page += 1;
    this.loadDemands();
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
