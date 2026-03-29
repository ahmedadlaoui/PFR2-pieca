import { createFeatureSelector, createSelector } from '@ngrx/store';
import { OffersState } from './offers.reducer';

const selectOffersState = createFeatureSelector<OffersState>('buyerOffers');

export const selectOffers       = createSelector(selectOffersState, s => s.offers);
export const selectLoading      = createSelector(selectOffersState, s => s.loading);
export const selectError        = createSelector(selectOffersState, s => s.error);
export const selectTotalPages   = createSelector(selectOffersState, s => s.totalPages);
export const selectTotalElements = createSelector(selectOffersState, s => s.totalElements);
export const selectCurrentPage  = createSelector(selectOffersState, s => s.currentPage);
