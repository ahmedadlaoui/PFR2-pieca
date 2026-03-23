import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateDemandPayload {
  title: string;
  description?: string;
  categoryId: number;
  latitude: number;
  longitude: number;
  radiusKm: number;
}

export interface CreateDemandResponse {
  id: number;
  title: string;
  description: string;
  categoryId: number;
  status: string;
  latitude: number;
  longitude: number;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class RequestService {
  private readonly API = '/api/v1/requests';

  constructor(private http: HttpClient) {}

  createDemand(payload: CreateDemandPayload): Observable<CreateDemandResponse> {
    return this.http.post<CreateDemandResponse>(this.API, payload, { withCredentials: true });
  }
}
