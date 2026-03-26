import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AuthService, CurrentUser } from '../core/services/auth.service';
import { RequestService } from '../core/services/request.service';

type SellerProfileData = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  serviceName: string;
  city: string;
  address: string;
  radiusKm: number;
  about: string;
};

@Component({
  selector: 'app-seller-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './seller-profile.html'
})
export class SellerProfile implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly requestService = inject(RequestService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  isSaving = false;
  saveSuccess = '';
  saveError = '';

  storeImages: string[] = [];
  storeImagesUploading = false;
  storeImagesError = '';

  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    phone: ['', [Validators.required, Validators.pattern(/^\+?[0-9\s-]{8,20}$/)]],
    serviceName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    address: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(180)]],
    radiusKm: [10, [Validators.required, Validators.min(1), Validators.max(500)]],
    about: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.loadInitialFormData(user);
        },
        error: () => {
          this.saveError = 'Impossible de charger votre profil pour le moment.';
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  saveProfile(): void {
    this.saveSuccess = '';
    this.saveError = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.saveError = 'Veuillez corriger les champs invalides avant de continuer.';
      return;
    }

    this.isSaving = true;

    try {
      const data: SellerProfileData = this.form.getRawValue();
      const key = this.getStorageKey(data.email);
      localStorage.setItem(key, JSON.stringify(data));

      const current = this.authService.currentUser$.value;
      if (current) {
        this.authService.currentUser$.next({
          ...current,
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email
        });
      }

      this.saveSuccess = 'Profil enregistre avec succes.';
    } catch {
      this.saveError = 'Une erreur est survenue pendant la sauvegarde. Veuillez reessayer.';
    } finally {
      this.isSaving = false;
    }
  }

  hasError(controlName: keyof SellerProfileData): boolean {
    const control = this.form.controls[controlName];
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  errorText(controlName: keyof SellerProfileData): string {
    const control = this.form.controls[controlName];
    if (!control?.errors) {
      return '';
    }

    if (control.errors['required']) {
      return 'Ce champ est obligatoire.';
    }
    if (control.errors['email']) {
      return 'Format email invalide.';
    }
    if (control.errors['minlength']) {
      return `Minimum ${control.errors['minlength'].requiredLength} caracteres.`;
    }
    if (control.errors['maxlength']) {
      return `Maximum ${control.errors['maxlength'].requiredLength} caracteres.`;
    }
    if (control.errors['pattern']) {
      return 'Format invalide.';
    }
    if (control.errors['min']) {
      return `La valeur minimale est ${control.errors['min'].min}.`;
    }
    if (control.errors['max']) {
      return `La valeur maximale est ${control.errors['max'].max}.`;
    }

    return 'Valeur invalide.';
  }

  private loadInitialFormData(user: CurrentUser | null): void {
    const defaults = this.getDefaultProfile(user);
    let loaded = defaults;

    try {
      const raw = localStorage.getItem(this.getStorageKey(defaults.email));
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<SellerProfileData>;
        loaded = {
          ...defaults,
          ...parsed,
          radiusKm: Number(parsed.radiusKm ?? defaults.radiusKm)
        };
      }
    } catch {
      this.saveError = 'Certaines donnees locales etaient invalides et ont ete reinitialisees.';
    }

    this.form.patchValue(loaded);
  }

  private getDefaultProfile(user: CurrentUser | null): SellerProfileData {
    return {
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
      email: user?.email ?? '',
      phone: '+212 600 00 00 00',
      serviceName: 'Atelier vendeur',
      city: 'Casablanca',
      address: 'Adresse a completer',
      radiusKm: 10,
      about: 'Je propose des produits et services avec une reponse rapide aux clients locaux.'
    };
  }

  private getStorageKey(email: string): string {
    return `seller_profile_${email || 'unknown'}`;
  }

  onStoreImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const files = Array.from(input.files);
    if (this.storeImages.length + files.length > 6) {
      this.storeImagesError = 'Maximum 6 images autorisees.';
      return;
    }

    this.storeImagesUploading = true;
    this.storeImagesError = '';
    this.requestService.uploadStoreImages(files).subscribe({
      next: (urls) => {
        this.storeImages = urls;
        this.storeImagesUploading = false;
      },
      error: (err) => {
        this.storeImagesUploading = false;
        this.storeImagesError = err.error?.message || 'Erreur lors de l\'upload des images.';
      }
    });

    input.value = '';
  }

  removeStoreImage(imageUrl: string): void {
    this.requestService.deleteStoreImage(imageUrl).subscribe({
      next: () => {
        this.storeImages = this.storeImages.filter(u => u !== imageUrl);
      },
      error: () => {
        this.storeImagesError = 'Erreur lors de la suppression.';
      }
    });
  }
}
