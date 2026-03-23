import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
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

  demand: BuyerDemandDetails | null = null;
  loading = true;
  errorMessage = '';

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
      error: (err) => {
        this.loading = false;
        this.errorMessage = "Impossible de charger les d�tails de la demande. Vous n'avez peut-�tre pas acc�s.";
      }
    });
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

