import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
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
}

export interface RegisterSellerPayload {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
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
    return this.http.post<AuthResponse>(`${this.API}/login`, payload).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  registerBuyer(payload: RegisterBuyerPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register/buyer`, payload).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  registerSeller(payload: RegisterSellerPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register/seller`, payload).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  refreshAccessToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<AuthResponse>(`${this.API}/refresh`, { refreshToken }).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('currentUser');
    this.currentUser$.next(null);
  }

  private handleAuthResponse(res: AuthResponse): void {
    localStorage.setItem('accessToken', res.accessToken);
    localStorage.setItem('refreshToken', res.refreshToken);

    const user: CurrentUser = {
      email: res.email,
      role: res.role,
      firstName: res.firstName,
      lastName: res.lastName,
      profileImageUrl: res.profileImageUrl
    };

    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUser$.next(user);
  }

  private loadUserFromStorage(): void {
    const stored = localStorage.getItem('currentUser');
    if (stored) {
      try {
        this.currentUser$.next(JSON.parse(stored));
      } catch {
        this.logout();
      }
    }
  }
}
