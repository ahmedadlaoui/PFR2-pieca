import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-seller-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './seller-profile.html'
})
export class SellerProfile {
  profile = {
    firstName: 'Yassine',
    lastName: 'Bennani',
    email: 'seller@example.com',
    phone: '+212 600 00 00 00',
    serviceName: 'Atelier Textiles Pro',
    city: 'Casablanca',
    address: 'Bd Anfa, Centre ville',
    radiusKm: 12,
    about: 'Specialiste en isolation textile, retouche et reparation rapide.'
  };
}
