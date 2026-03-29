import { createReducer, on } from '@ngrx/store';
import { BuyerOfferItem } from '../../../core/services/request.service';
import * as OffersActions from './offers.actions';

export interface OffersState {
  offers: BuyerOfferItem[];
  loading: boolean;
  error: string | null;
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

const initialState: OffersState = {
  offers: [],
  loading: false,
  error: null,
  totalElements: 0,
  totalPages: 0,
  currentPage: 0,
};

export const offersReducer = createReducer(
  initialState,

  on(OffersActions.loadOffers, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(OffersActions.loadOffersSuccess, (state, { response }) => ({
    ...state,
    loading: false,
    offers: response.content,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
    currentPage: response.number,
  })),

  on(OffersActions.loadOffersFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  on(OffersActions.acceptOfferSuccess, (state, { offerId }) => ({
    ...state,
    offers: state.offers.map(o =>
      o.offerId === offerId ? { ...o, offerStatus: 'ACCEPTED' } : o
    ),
  })),

  on(OffersActions.declineOfferSuccess, (state, { offerId }) => ({
    ...state,
    offers: state.offers.map(o =>
      o.offerId === offerId ? { ...o, offerStatus: 'REJECTED' } : o
    ),
  }))
);
