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
    sellerType: string;
    sellerCategories: string[];
    sellerLatitude: number | null;
    sellerLongitude: number | null;
    sellerActiveRadiusKm: number | null;
    sellerStoreImages: string[];
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

export interface SellerRequestItem {
  requestId: number;
  offerId: number;
  title: string;
  description: string;
  categoryName: string;
  requestStatus: string;
  offerStatus: string;
  offerPrice: number;
  buyerFirstName: string;
  buyerLastName: string;
  buyerPhone: string | null;
  buyerEmail: string | null;
  imageUrl: string | null;
  createdAt: string;
  offerCreatedAt: string;
}

export interface SellerDashboardStats {
  totalOffers: number;
  pendingOffers: number;
  acceptedOffers: number;
  rejectedOffers: number;
  cancelledOffers: number;
  totalRevenue: number;
  totalClients: number;
  monthlyRevenue: number[];
}

export interface BuyerOfferItem {
  offerId: number;
  price: number;
  proofImageUrl: string;
  offerStatus: string;
  offerCreatedAt: string;
  requestId: number;
  requestTitle: string;
  requestDescription: string;
  requestCategoryName: string;
  requestImageUrl: string | null;
  requestStatus: string;
  sellerName: string;
  sellerEmail: string;
  sellerPhone: string | null;
  storeName: string | null;
  sellerType: string | null;
  sellerCategories: string[];
  sellerLatitude: number | null;
  sellerLongitude: number | null;
  sellerActiveRadiusKm: number | null;
  sellerStoreImages: string[];
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
    const params: Record<string, string | number> = { page, size };
    if (status) {
      params['status'] = status;
    }
    return this.http.get<PagedResponse<BuyerDemandItem>>(`${this.API}/me`, { withCredentials: true, params });
  }

  getDemandDetails(id: number): Observable<BuyerDemandDetails> {
    return this.http.get<BuyerDemandDetails>(`${this.API}/${id}`, { withCredentials: true });
  }

  getSellerOffers(offerStatus: string | null, page = 0, size = 20): Observable<PagedResponse<SellerRequestItem>> {
    const params: Record<string, string | number> = { page, size };
    if (offerStatus) {
      params['offerStatus'] = offerStatus;
    }
    return this.http.get<PagedResponse<SellerRequestItem>>(`${this.API}/seller/offers`, { params });
  }

  getSellerStats(): Observable<SellerDashboardStats> {
    return this.http.get<SellerDashboardStats>(`${this.API}/seller/stats`);
  }

  cancelOffer(requestId: number): Observable<void> {
    return this.http.post<void>(`${this.API}/${requestId}/cancel`, {});
  }

  acceptDemand(requestId: number, price?: number): Observable<void> {
    const params: Record<string, string> = {};
    if (price != null && price > 0) {
      params['price'] = String(price);
    }
    return this.http.post<void>(`${this.API}/${requestId}/accept`, {}, { params });
  }

  buyerAcceptOffer(offerId: number): Observable<void> {
    return this.http.post<void>(`${this.API}/offers/${offerId}/buyer-accept`, {}, { withCredentials: true });
  }

  buyerDeclineOffer(offerId: number): Observable<void> {
    return this.http.post<void>(`${this.API}/offers/${offerId}/buyer-decline`, {}, { withCredentials: true });
  }

  uploadStoreImages(files: File[]): Observable<string[]> {
    const formData = new FormData();
    for (const file of files) {
      formData.append('files', file);
    }
    return this.http.post<string[]>('/api/v1/sellers/me/store-images', formData);
  }

  deleteStoreImage(imageUrl: string): Observable<void> {
    return this.http.delete<void>('/api/v1/sellers/me/store-images', { params: { imageUrl } });
  }

  getBuyerOffers(offerStatus: string | null, period: string | null, page = 0, size = 10): Observable<PagedResponse<BuyerOfferItem>> {
    const params: Record<string, string | number> = { page, size };
    if (offerStatus) params['offerStatus'] = offerStatus;
    if (period) params['period'] = period;
    return this.http.get<PagedResponse<BuyerOfferItem>>(`${this.API}/buyer/offers`, { params });
  }
}

