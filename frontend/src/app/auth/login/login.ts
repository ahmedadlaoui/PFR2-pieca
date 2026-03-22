import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html'
})
export class Login {

  loginForm: FormGroup;
  serverError = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.loginForm = this.fb.group({
      phoneNumber: ['', [Validators.required, Validators.pattern(/^(\+212|0)[5-7]\d{8}$/)]],
      password: ['', [Validators.required]]
    });
  }

  submit(): void {
    this.serverError = '';
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.authService.login({
      phoneNumber: this.loginForm.value.phoneNumber,
      password: this.loginForm.value.password
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
}
