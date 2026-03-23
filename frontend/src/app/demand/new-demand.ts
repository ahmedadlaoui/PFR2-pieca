import { AfterViewInit, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RequestService } from '../core/services/request.service';
import { AuthService, CurrentUser } from '../core/services/auth.service';
import { Subscription } from 'rxjs';

declare const L: any;

@Component({
  selector: 'app-new-demand',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './new-demand.html'
})
export class NewDemand implements OnInit, AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly requestService = inject(RequestService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly defaultLat = 33.5731;
  private readonly defaultLon = -7.5898;

  currentUser: CurrentUser | null = null;
  dropdownOpen = false;
  private userSub!: Subscription;

  private map: any = null;
  private marker: any = null;
  private circle: any = null;
  private gpsWatchId: number | null = null;

  loadingLocation = false;
  trackingLocation = false;
  submitting = false;
  locationError = '';
  successMessage = '';
  errorMessage = '';
  selectedPhoto: File | null = null;
  selectedPhotoName = '';
  selectedPhotoPreviewUrl = '';
  photoErrorMessage = '';

  private readonly maxPhotoSizeBytes = 5 * 1024 * 1024;
  private readonly allowedPhotoMimeTypes = ['image/jpeg', 'image/png', 'image/webp'];

  readonly categories = [
    { id: 1, label: 'Electronique et High-Tech' },
    { id: 2, label: 'Pieces Auto et Moto' },
    { id: 3, label: 'Vetements et Mode' },
    { id: 4, label: 'Electromenager' },
    { id: 5, label: 'Mobilier et Decoration' },
    { id: 6, label: 'Autre' }
  ];

  readonly demandForm = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    categoryId: [null as number | null, [Validators.required]],
    locationLabel: ['', [Validators.required]],
    latitude: [null as number | null, [Validators.required]],
    longitude: [null as number | null, [Validators.required]],
    radiusKm: [2, [Validators.required, Validators.min(1), Validators.max(100)]]
  });

  constructor() {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initMapSafe(), 50);
  }

  ngOnDestroy(): void {
    this.destroyMap();
    this.revokePhotoPreview();
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.dropdownOpen = false;
    this.router.navigate(['/']);
  }

  getInitials(): string {
    if (!this.currentUser) return '';
    const first = this.currentUser.firstName?.charAt(0) || '';
    const last = this.currentUser.lastName?.charAt(0) || '';
    return (first + last).toUpperCase();
  }

  goToDemandPage(event?: Event): void {
    event?.preventDefault();
    this.router.navigate(['/buyer/demand/new']);
  }

  goToDashboard(event?: Event): void {
    event?.preventDefault();
    this.closeDropdown();

    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    if (this.currentUser?.role === 'SELLER') {
      this.router.navigate(['/seller/dashboard']);
      return;
    }

    this.router.navigate(['/buyer/dashboard']);
  }

  get selectedRadiusKm(): number {
    return this.demandForm.value.radiusKm ?? 2;
  }

  get latitudeValue(): number | null {
    return this.demandForm.value.latitude ?? null;
  }

  get longitudeValue(): number | null {
    return this.demandForm.value.longitude ?? null;
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.photoErrorMessage = '';
    this.errorMessage = '';

    if (!file) {
      this.clearSelectedPhoto();
      return;
    }

    if (!this.allowedPhotoMimeTypes.includes(file.type)) {
      this.clearSelectedPhoto();
      this.photoErrorMessage = 'Format non supporte. Utilisez JPG, PNG ou WEBP.';
      return;
    }

    if (file.size > this.maxPhotoSizeBytes) {
      this.clearSelectedPhoto();
      this.photoErrorMessage = 'Image trop volumineuse. Taille maximale autorisee: 5 MB.';
      return;
    }

    const previewObjectUrl = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      this.revokePhotoPreview();
      this.selectedPhoto = file;
      this.selectedPhotoName = file.name;
      this.selectedPhotoPreviewUrl = previewObjectUrl;
    };
    img.onerror = () => {
      URL.revokeObjectURL(previewObjectUrl);
      this.clearSelectedPhoto();
      this.photoErrorMessage = 'Le fichier selectionne est invalide ou corrompu.';
    };
    img.src = previewObjectUrl;
  }

  removeSelectedPhoto(): void {
    this.clearSelectedPhoto();
  }

  async initMapSafe(): Promise<void> {
    const leafletReady = await this.ensureLeafletLoaded();
    if (!leafletReady) {
      this.locationError = 'La carte n\'a pas pu se charger. Veuillez rafraichir la page.';
      return;
    }

    this.initMap();
  }

  initMap(): void {
    if (this.map || typeof L === 'undefined') {
      return;
    }

    const mapEl = document.getElementById('demand-map');
    if (!mapEl) {
      return;
    }

    this.map = L.map('demand-map', { 
      zoomControl: true,
      attributionControl: false // Remove Leaflet attribution 
    }).setView([this.defaultLat, this.defaultLon], 10); // slightly zoomed out

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19
    }).addTo(this.map);

    this.map.on('click', (e: any) => {
      this.placeMarker(e.latlng.lat, e.latlng.lng, true);
    });

    // Affiche Casablanca immediatement pour garantir une carte utilisable.
    this.placeMarker(this.defaultLat, this.defaultLon, true);

    // Leaflet sometimes needs a delayed resize when inside animated layouts.
    setTimeout(() => this.map?.invalidateSize(), 0);
    setTimeout(() => this.map?.invalidateSize(), 300);

  }

  placeMarker(lat: number, lon: number, fitView: boolean): void {
    const latLng = [lat, lon];

    this.demandForm.patchValue({
      latitude: lat,
      longitude: lon,
      locationLabel: `${lat.toFixed(6)}, ${lon.toFixed(6)}`
    });

    if (!this.marker) {
      this.marker = L.marker(latLng, { draggable: true }).addTo(this.map);
      this.marker.on('dragend', (e: any) => {
        const pos = e.target.getLatLng();
        this.placeMarker(pos.lat, pos.lng, false);
      });
    } else {
      this.marker.setLatLng(latLng);
    }

    this.updateCircle(fitView);
  }

  updateCircle(fitView: boolean): void {
    if (!this.marker || !this.map) {
      return;
    }

    const latLng = this.marker.getLatLng();
    const radiusMeters = this.selectedRadiusKm * 1000;

    if (!this.circle) {
      this.circle = L.circle(latLng, {
        radius: radiusMeters,
        color: '#255362',
        fillColor: '#255362',
        fillOpacity: 0.14,
        weight: 2
      }).addTo(this.map);
    } else {
      this.circle.setLatLng(latLng);
      this.circle.setRadius(radiusMeters);
    }

    if (fitView) {
      this.map.fitBounds(this.circle.getBounds(), { padding: [24, 24] });
    }
  }

  onRadiusInput(value: number | string): void {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
      return;
    }

    const radius = Math.max(1, Math.min(100, parsed));
    this.demandForm.patchValue({ radiusKm: radius });
    this.updateCircle(false);
  }

  toggleTracking(): void {
    if (this.trackingLocation) {
      this.stopTracking();
      return;
    }

    this.startTracking();
  }

  startTracking(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.locationError = '';

    if (!navigator.geolocation) {
      this.errorMessage = 'La geolocalisation n\'est pas supportee par votre navigateur. Casablanca reste selectionnee.';
      this.placeMarker(this.defaultLat, this.defaultLon, true);
      return;
    }

    this.loadingLocation = true;
    this.trackingLocation = true;
    this.gpsWatchId = navigator.geolocation.watchPosition(
      (position) => {
        this.loadingLocation = false;
        this.placeMarker(position.coords.latitude, position.coords.longitude, false);
        this.map?.setView([position.coords.latitude, position.coords.longitude], 12);
      },
      () => {
        this.loadingLocation = false;
        this.stopTracking();
        this.errorMessage = 'Permission GPS refusee. Casablanca reste la position par defaut.';
        this.placeMarker(this.defaultLat, this.defaultLon, true);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 1500 }
    );
  }

  stopTracking(): void {
    if (this.gpsWatchId !== null && navigator.geolocation) {
      navigator.geolocation.clearWatch(this.gpsWatchId);
    }

    this.gpsWatchId = null;
    this.trackingLocation = false;
    this.loadingLocation = false;
  }

  private async ensureLeafletLoaded(): Promise<boolean> {
    if (typeof L !== 'undefined') {
      return true;
    }

    const leafletScriptId = 'leaflet-script-cdn';
    const leafletCssId = 'leaflet-css-cdn';

    if (!document.getElementById(leafletCssId)) {
      const css = document.createElement('link');
      css.id = leafletCssId;
      css.rel = 'stylesheet';
      css.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(css);
    }

    if (!document.getElementById(leafletScriptId)) {
      await new Promise<void>((resolve, reject) => {
        const script = document.createElement('script');
        script.id = leafletScriptId;
        script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
        script.async = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Leaflet script failed to load'));
        document.body.appendChild(script);
      }).catch(() => undefined);
    }

    if (typeof L !== 'undefined') {
      return true;
    }

    const maxChecks = 20;
    for (let i = 0; i < maxChecks; i += 1) {
      await new Promise(resolve => setTimeout(resolve, 100));
      if (typeof L !== 'undefined') {
        return true;
      }
    }

    return false;
  }

  submitDemand(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.locationError = '';

    if (this.demandForm.invalid) {
      this.demandForm.markAllAsTouched();
      this.errorMessage = 'Merci de completer tous les champs obligatoires avant creation.';
      return;
    }

    if (this.photoErrorMessage) {
      this.errorMessage = 'Merci de corriger le probleme de photo avant creation.';
      return;
    }

    this.submitting = true;
    this.requestService.createDemand({
      title: this.demandForm.value.title!.trim(),
      description: (this.demandForm.value.description || '').trim(),
      categoryId: this.demandForm.value.categoryId!,
      latitude: this.demandForm.value.latitude!,
      longitude: this.demandForm.value.longitude!,
      radiusKm: this.selectedRadiusKm
    }, this.selectedPhoto).subscribe({
      next: (res) => {
        this.submitting = false;
        this.successMessage = `Demande #${res.id} creee avec succes.`;
        this.clearSelectedPhoto();
      },
      error: (err) => {
        this.submitting = false;
        const fallback = 'Impossible de creer la demande pour le moment.';
        const message = (err?.message || fallback) as string;

        if (message.toLowerCase().includes('payload') || message.toLowerCase().includes('too large') || message.includes('413')) {
          this.errorMessage = 'Image trop lourde pour le serveur. Reduisez sa taille et reessayez.';
          return;
        }

        if (message.toLowerCase().includes('unsupported') || message.toLowerCase().includes('media type') || message.includes('415')) {
          this.errorMessage = 'Type de fichier non supporte par le serveur. Utilisez JPG, PNG ou WEBP.';
          return;
        }

        this.errorMessage = message || fallback;
      }
    });
  }

  get selectedPhotoSizeLabel(): string {
    if (!this.selectedPhoto) {
      return '';
    }

    const sizeKb = this.selectedPhoto.size / 1024;
    if (sizeKb < 1024) {
      return `${Math.round(sizeKb)} KB`;
    }

    return `${(sizeKb / 1024).toFixed(2)} MB`;
  }

  private clearSelectedPhoto(): void {
    this.selectedPhoto = null;
    this.selectedPhotoName = '';
    this.revokePhotoPreview();
  }

  private revokePhotoPreview(): void {
    if (this.selectedPhotoPreviewUrl) {
      URL.revokeObjectURL(this.selectedPhotoPreviewUrl);
      this.selectedPhotoPreviewUrl = '';
    }
  }

  private destroyMap(): void {
    this.stopTracking();

    if (!this.map) {
      return;
    }

    this.map.remove();
    this.map = null;
    this.marker = null;
    this.circle = null;
  }
}
