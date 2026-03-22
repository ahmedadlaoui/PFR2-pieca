import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface AuthResponse {
  token: string;
  role: string;
  phoneNumber: string;
}

export interface RegisterBuyerPayload {
  phoneNumber: string;
  password: string;
}

export interface RegisterSellerPayload {
  phoneNumber: string;
  password: string;
  sellerType: string;
  categoryIds: number[];
  latitude?: number;
  longitude?: number;
  activeRadiusKm?: number;
  customCategoryNote?: string;
}

export interface LoginPayload {
  phoneNumber: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly API = '/api/v1/auth';

  constructor(private http: HttpClient) {}

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/login`, payload).pipe(
      tap(res => this.storeToken(res))
    );
  }

  registerBuyer(payload: RegisterBuyerPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register/buyer`, payload).pipe(
      tap(res => this.storeToken(res))
    );
  }

  registerSeller(payload: RegisterSellerPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register/seller`, payload).pipe(
      tap(res => this.storeToken(res))
    );
  }

  private storeToken(res: AuthResponse): void {
    localStorage.setItem('token', res.token);
    localStorage.setItem('role', res.role);
    localStorage.setItem('phone', res.phoneNumber);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('phone');
  }
}
