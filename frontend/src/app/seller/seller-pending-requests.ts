import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

type PendingRequest = {
  id: number;
  title: string;
  city: string;
  createdAt: string;
  offerStatus: 'PENDING';
  note: string;
};

@Component({
  selector: 'app-seller-pending-requests',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seller-pending-requests.html'
})
export class SellerPendingRequests {
  requests: PendingRequest[] = [
    {
      id: 204,
      title: 'Isolation veste travail',
      city: 'Rabat',
      createdAt: '24/03/2026 10:12',
      offerStatus: 'PENDING',
      note: 'Client attend votre confirmation finale.'
    },
    {
      id: 198,
      title: 'Retouche manteau hiver',
      city: 'Casablanca',
      createdAt: '23/03/2026 17:40',
      offerStatus: 'PENDING',
      note: 'Offre envoyee, en attente de validation du client.'
    },
    {
      id: 191,
      title: 'Reparation fermeture textile technique',
      city: 'Marrakech',
      createdAt: '22/03/2026 09:05',
      offerStatus: 'PENDING',
      note: 'Le client a consulte l offre, aucune reponse pour le moment.'
    }
  ];
}
