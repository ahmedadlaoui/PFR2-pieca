import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html'
})
export class Register implements OnInit {

  currentView: 'selector' | 'buyer' | 'seller' = 'selector';

  buyerForm!: FormGroup;
  sellerForm!: FormGroup;

  serverError = '';
  loading = false;

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
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.loading = false;
        this.serverError = err.message || 'Une erreur est survenue';
      }
    });
  }

  submitSeller(): void {
    this.serverError = '';

    if (this.sellerForm.value.password !== this.sellerForm.value.confirmPassword) {
      this.serverError = 'Les mots de passe ne correspondent pas';
      return;
    }

    if (this.sellerForm.invalid) {
      this.sellerForm.markAllAsTouched();
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
      customCategoryNote: this.sellerForm.value.customCategoryNote || undefined
    }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.loading = false;
        this.serverError = err.message || 'Une erreur est survenue';
      }
    });
  }

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
