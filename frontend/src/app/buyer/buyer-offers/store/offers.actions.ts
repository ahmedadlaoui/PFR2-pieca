import { createAction, props } from '@ngrx/store';
import { BuyerOfferItem, PagedResponse } from '../../../core/services/request.service';

export const loadOffers = createAction(
  '[Buyer Offers] Load Offers',
  props<{ status: string | null; period: string | null; page: number; size: number }>()
);

export const loadOffersSuccess = createAction(
  '[Buyer Offers] Load Offers Success',
  props<{ response: PagedResponse<BuyerOfferItem> }>()
);

export const loadOffersFailure = createAction(
  '[Buyer Offers] Load Offers Failure',
  props<{ error: string }>()
);

export const acceptOffer = createAction(
  '[Buyer Offers] Accept Offer',
  props<{ offerId: number }>()
);

export const acceptOfferSuccess = createAction(
  '[Buyer Offers] Accept Offer Success',
  props<{ offerId: number }>()
);

export const declineOffer = createAction(
  '[Buyer Offers] Decline Offer',
  props<{ offerId: number }>()
);

export const declineOfferSuccess = createAction(
  '[Buyer Offers] Decline Offer Success',
  props<{ offerId: number }>()
);
