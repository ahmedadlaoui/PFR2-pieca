import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

// Declare Leaflet as a global to avoid TypeScript errors (loaded via CDN)
declare const L: any;

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html'
})
export class Register implements OnInit, AfterViewInit, OnDestroy {

  currentView: 'selector' | 'buyer' | 'seller' = 'selector';
  sellerStep: 1 | 2 | 3 = 1; // Multi-step seller form (1=Account, 2=Business, 3=Location)

  buyerForm!: FormGroup;
  sellerForm!: FormGroup;

  serverError = '';
  loading = false;

  // Map state
  private map: any = null;
  private marker: any = null;
  private circle: any = null;

  // Location state bound to form
  selectedLat: number | null = null;
  selectedLon: number | null = null;
  selectedRadius = 10; // default 10 km

  sellerTypes = [
    { value: 'COMPANY', label: 'Entreprise' },
    { value: 'LOCAL_STORE', label: 'Magasin local' },
    { value: 'AUTO_ENTREPRENEUR', label: 'Auto-entrepreneur' },
    { value: 'CASUAL', label: 'Particulier' }
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.buyerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });

    this.sellerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      sellerType: ['', [Validators.required]],
      categoryIds: [[], [Validators.required]],
      customCategoryNote: ['']
    });

    this.route.paramMap.subscribe(params => {
      const role = params.get('role');
      if (role === 'buyer') {
        this.currentView = 'buyer';
      } else if (role === 'seller') {
        this.currentView = 'seller';
      } else {
        this.currentView = 'selector';
      }
    });
  }

  ngAfterViewInit(): void {
    // Map is initialized later when user reaches step 3
  }

  ngOnDestroy(): void {
    this.destroyMap();
  }

  // ──────────────── Multi-step Navigation ────────────────

  nextStep(): void {
    if (this.sellerStep === 1) {
      const { email, firstName, lastName, password, confirmPassword } = this.sellerForm.controls;
      email.markAsTouched(); firstName.markAsTouched(); lastName.markAsTouched();
      password.markAsTouched(); confirmPassword.markAsTouched();
      if (email.invalid || firstName.invalid || lastName.invalid || password.invalid) return;
      if (password.value !== confirmPassword.value) {
        this.serverError = 'Les mots de passe ne correspondent pas';
        return;
      }
      this.serverError = '';
      this.sellerStep = 2;
    } else if (this.sellerStep === 2) {
      const { sellerType, categoryIds } = this.sellerForm.controls;
      sellerType.markAsTouched();
      if (sellerType.invalid || !categoryIds.value?.length) {
        this.serverError = 'Veuillez sélectionner un type de vendeur et au moins une catégorie.';
        return;
      }
      this.serverError = '';
      this.sellerStep = 3;
      // Init map after Angular renders the map container
      setTimeout(() => this.initSellerMap(), 100);
    }
  }

  prevStep(): void {
    if (this.sellerStep > 1) {
      this.sellerStep = (this.sellerStep - 1) as 1 | 2;
      if (this.sellerStep < 3) this.destroyMap();
    }
  }

  // ──────────────── Leaflet Map ────────────────

  initSellerMap(): void {
    if (this.map) return; // Already init

    const mapEl = document.getElementById('seller-map');
    if (!mapEl || typeof L === 'undefined') return;

    // Morocco center as default
    const defaultLat = 31.7917;
    const defaultLon = -7.0926;

    this.map = L.map('seller-map', { zoomControl: true }).setView([defaultLat, defaultLon], 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19
    }).addTo(this.map);

    // Try to get GPS position immediately
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          this.placeMarker(pos.coords.latitude, pos.coords.longitude);
          this.map.setView([pos.coords.latitude, pos.coords.longitude], 12);
        },
        () => { /* user denied — they can click manually */ }
      );
    }

    // Click handler
    this.map.on('click', (e: any) => {
      this.placeMarker(e.latlng.lat, e.latlng.lng);
    });
  }

  placeMarker(lat: number, lon: number): void {
    this.selectedLat = lat;
    this.selectedLon = lon;

    const latlng = [lat, lon];

    if (!this.marker) {
      this.marker = L.marker(latlng, { draggable: true }).addTo(this.map);
      this.marker.on('dragend', (e: any) => {
        const pos = e.target.getLatLng();
        this.selectedLat = pos.lat;
        this.selectedLon = pos.lng;
        this.updateCircle();
      });
    } else {
      this.marker.setLatLng(latlng);
    }

    this.updateCircle();
  }

  updateCircle(): void {
    if (!this.marker) return;

    const latlng = this.marker.getLatLng();
    const radiusMeters = this.selectedRadius * 1000;

    if (!this.circle) {
      this.circle = L.circle(latlng, {
        radius: radiusMeters,
        color: '#255362',
        fillColor: '#255362',
        fillOpacity: 0.15,
        weight: 2
      }).addTo(this.map);
    } else {
      this.circle.setLatLng(latlng);
      this.circle.setRadius(radiusMeters);
    }

    // Fit map to circle bounds
    this.map.fitBounds(this.circle.getBounds(), { padding: [30, 30] });
  }

  onRadiusChange(value: number): void {
    this.selectedRadius = value;
    this.updateCircle();
  }

  useMyLocation(): void {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.placeMarker(pos.coords.latitude, pos.coords.longitude);
        this.map?.setView([pos.coords.latitude, pos.coords.longitude], 13);
      },
      () => {
        this.serverError = 'Impossible d\'accéder à votre position GPS. Veuillez cliquer sur la carte manuellement.';
      }
    );
  }

  destroyMap(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
      this.marker = null;
      this.circle = null;
    }
  }

  // ──────────────── Form Submissions ────────────────

  submitBuyer(): void {
    this.serverError = '';

    if (this.buyerForm.value.password !== this.buyerForm.value.confirmPassword) {
      this.serverError = 'Les mots de passe ne correspondent pas';
      return;
    }

    if (this.buyerForm.invalid) {
      this.buyerForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.authService.registerBuyer({
      email: this.buyerForm.value.email,
      firstName: this.buyerForm.value.firstName,
      lastName: this.buyerForm.value.lastName,
      password: this.buyerForm.value.password
    }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/buyer/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.serverError = err.message || 'Une erreur est survenue';
      }
    });
  }

  submitSeller(): void {
    this.serverError = '';

    if (!this.selectedLat || !this.selectedLon) {
      this.serverError = 'Veuillez sélectionner votre emplacement sur la carte.';
      return;
    }

    this.loading = true;
    this.authService.registerSeller({
      email: this.sellerForm.value.email,
      firstName: this.sellerForm.value.firstName,
      lastName: this.sellerForm.value.lastName,
      password: this.sellerForm.value.password,
      sellerType: this.sellerForm.value.sellerType,
      categoryIds: this.sellerForm.value.categoryIds,
      customCategoryNote: this.sellerForm.value.customCategoryNote || undefined,
      latitude: this.selectedLat,
      longitude: this.selectedLon,
      activeRadiusKm: this.selectedRadius
    }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/seller/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.serverError = err.message || 'Une erreur est survenue';
      }
    });
  }

  // ──────────────── Category Helpers ────────────────

  categories = [
    { id: 1, name: 'Électronique & High-Tech' },
    { id: 2, name: 'Pièces Auto & Moto' },
    { id: 3, name: 'Vêtements & Mode' },
    { id: 4, name: 'Électroménager' },
    { id: 5, name: 'Mobilier & Décoration' },
    { id: 6, name: 'Autre' }
  ];

  toggleCategory(id: number): void {
    const current: number[] = this.sellerForm.value.categoryIds || [];
    const index = current.indexOf(id);
    if (index > -1) {
      current.splice(index, 1);
    } else {
      current.push(id);
    }
    this.sellerForm.patchValue({ categoryIds: [...current] });
  }

  isCategorySelected(id: number): boolean {
    return (this.sellerForm.value.categoryIds || []).includes(id);
  }
}
