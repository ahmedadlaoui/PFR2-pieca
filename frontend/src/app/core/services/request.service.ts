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
  imageUrl?: string;
  createdAt: string;
}

export interface BuyerDemandItem {
  id: number;
  title: string;
  description: string;
  categoryId: number;
  status: string;
  imageUrl?: string;
  createdAt: string;
}


export interface BuyerDemandDetails {
  id: number;
  title: string;
  description: string;
  categoryName: string;
  status: string;
  imageUrl?: string;
  createdAt: string;
  offers?: Array<{
    id: number;
    price: number;
    proofImageUrl: string;
    status: string;
    createdAt: string;
    sellerName: string;
    sellerEmail: string;
    sellerPhone: string;
    storeName: string;
  }>;
}

export interface PagedResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class RequestService {
  private readonly API = '/api/v1/requests';

  constructor(private http: HttpClient) {}

  createDemand(payload: CreateDemandPayload, photo?: File | null): Observable<CreateDemandResponse> {
    const formData = new FormData();
    formData.append('title', payload.title);
    formData.append('description', payload.description || '');
    formData.append('categoryId', String(payload.categoryId));
    formData.append('latitude', String(payload.latitude));
    formData.append('longitude', String(payload.longitude));
    formData.append('radiusKm', String(payload.radiusKm));

    if (photo) {
      formData.append('photo', photo);
    }

    return this.http.post<CreateDemandResponse>(this.API, formData, { withCredentials: true });
  }

  getMyDemands(status: string | null, page = 0, size = 10): Observable<PagedResponse<BuyerDemandItem>> {
    const params: Record<string, string | number> = {
      page,
      size
    };

    if (status) {
      params['status'] = status;
    }

    return this.http.get<PagedResponse<BuyerDemandItem>>(`${this.API}/me`, {
      withCredentials: true,
      params
    });
  }

  getDemandDetails(id: number): Observable<BuyerDemandDetails> {
    return this.http.get<BuyerDemandDetails>(`${this.API}/${id}`, { withCredentials: true });
  }
}

