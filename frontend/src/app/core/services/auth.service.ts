import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface AuthResponse {
  accessToken: string;
  role: string;
  email: string;
  firstName: string;
  lastName: string;
  profileImageUrl: string | null;
}

export interface CurrentUser {
  email: string;
  role: string;
  firstName: string;
  lastName: string;
  profileImageUrl: string | null;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterBuyerPayload {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  phoneNumber?: string;
}

export interface RegisterSellerPayload {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  phoneNumber?: string;
  sellerType: string;
  categoryIds: number[];
  latitude?: number;
  longitude?: number;
  activeRadiusKm?: number;
  customCategoryNote?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly API = '/api/v1/auth';

  currentUser$ = new BehaviorSubject<CurrentUser | null>(null);

  constructor(private http: HttpClient) {
    this.loadUserFromStorage();
  }

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/login`, payload, { withCredentials: true }).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  registerBuyer(payload: RegisterBuyerPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register/buyer`, payload, { withCredentials: true }).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  registerSeller(payload: RegisterSellerPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register/seller`, payload, { withCredentials: true }).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  refreshAccessToken(): Observable<AuthResponse> {
    // Relying on the HTTP-Only cookie automatically being sent by browser.
    return this.http.post<AuthResponse>(`${this.API}/refresh`, {}, { withCredentials: true }).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  isLoggedIn(): boolean {
    const token = this.getAccessToken();
    if (!token || this.isTokenExpired(token)) {
      this.clearLocalSession();
      return false;
    }

    if (!this.currentUser$.value) {
      this.decodeAndSetUser(token);
    }

    return !!this.currentUser$.value;
  }

  getCurrentUserSnapshot(): CurrentUser | null {
    return this.currentUser$.value;
  }

  logout(): void {
    // Notify server to clear the HTTP-Only cookie
    this.http.post(`${this.API}/logout`, {}, { withCredentials: true }).subscribe({
      next: () => this.clearLocalSession(),
      error: () => this.clearLocalSession() // even on error, clear local session
    });
  }

  private clearLocalSession(): void {
    localStorage.removeItem('accessToken');
    this.currentUser$.next(null);
  }

  private handleAuthResponse(res: AuthResponse): void {
    if (res.accessToken) {
      localStorage.setItem('accessToken', res.accessToken);
      this.decodeAndSetUser(res.accessToken);
    } else {
      this.clearLocalSession();
    }
  }

  private loadUserFromStorage(): void {
    const token = this.getAccessToken();
    if (token) {
      this.decodeAndSetUser(token);
    } else {
      this.clearLocalSession();
    }
  }

  private decodeAndSetUser(token: string): void {
    try {
      const payload = this.parseJwt(token);
      
      // Check if token is expired
      const currentTime = Math.floor(Date.now() / 1000);
      if (payload.exp && payload.exp < currentTime) {
        this.clearLocalSession();
        return;
      }

      this.currentUser$.next({
        email: payload.email || payload.sub,
        role: payload.role,
        firstName: payload.firstName,
        lastName: payload.lastName,
        profileImageUrl: payload.profileImageUrl || null
      });
    } catch {
      this.clearLocalSession();
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = this.parseJwt(token);
      const currentTime = Math.floor(Date.now() / 1000);
      return !!(payload.exp && payload.exp < currentTime);
    } catch {
      return true;
    }
  }

  private parseJwt(token: string): any {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      window.atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  }
}
