import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AuthService, CurrentUser } from '../core/services/auth.service';

type BuyerProfileData = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  city: string;
  address: string;
};

@Component({
  selector: 'app-buyer-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './buyer-profile.html'
})
export class BuyerProfile implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  isSaving = false;
  saveSuccess = '';
  saveError = '';

  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    phone: ['', [Validators.required, Validators.pattern(/^\+?[0-9\s-]{8,20}$/)]],
    city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    address: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(180)]]
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
      const data: BuyerProfileData = this.form.getRawValue();
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

  hasError(controlName: keyof BuyerProfileData): boolean {
    const control = this.form.controls[controlName];
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  errorText(controlName: keyof BuyerProfileData): string {
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

    return 'Valeur invalide.';
  }

  private loadInitialFormData(user: CurrentUser | null): void {
    const defaults = this.getDefaultProfile(user);
    let loaded = defaults;

    try {
      const raw = localStorage.getItem(this.getStorageKey(defaults.email));
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<BuyerProfileData>;
        loaded = {
          ...defaults,
          ...parsed
        };
      }
    } catch {
      this.saveError = 'Certaines donnees locales etaient invalides et ont ete reinitialisees.';
    }

    this.form.patchValue(loaded);
  }

  private getDefaultProfile(user: CurrentUser | null): BuyerProfileData {
    return {
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
      email: user?.email ?? '',
      phone: '+212 600 00 00 00',
      city: 'Casablanca',
      address: 'Adresse a completer'
    };
  }

  private getStorageKey(email: string): string {
    return `buyer_profile_${email || 'unknown'}`;
  }
}
