import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { RequestService } from '../../../core/services/request.service';
import * as OffersActions from './offers.actions';

@Injectable()
export class OffersEffects {

  private actions$ = inject(Actions);
  private requestService = inject(RequestService);

  loadOffers$ = createEffect(() =>
    this.actions$.pipe(
      ofType(OffersActions.loadOffers),
      switchMap(({ status, period, page, size }) =>
        this.requestService.getBuyerOffers(status, period, page, size).pipe(
          map(response => OffersActions.loadOffersSuccess({ response })),
          catchError(() => of(OffersActions.loadOffersFailure({ error: 'Impossible de charger vos offres.' })))
        )
      )
    )
  );

  acceptOffer$ = createEffect(() =>
    this.actions$.pipe(
      ofType(OffersActions.acceptOffer),
      switchMap(({ offerId }) =>
        this.requestService.buyerAcceptOffer(offerId).pipe(
          map(() => OffersActions.acceptOfferSuccess({ offerId })),
          catchError(() => of(OffersActions.loadOffersFailure({ error: "Erreur lors de l'acceptation." })))
        )
      )
    )
  );

  declineOffer$ = createEffect(() =>
    this.actions$.pipe(
      ofType(OffersActions.declineOffer),
      switchMap(({ offerId }) =>
        this.requestService.buyerDeclineOffer(offerId).pipe(
          map(() => OffersActions.declineOfferSuccess({ offerId })),
          catchError(() => of(OffersActions.loadOffersFailure({ error: 'Erreur lors du refus.' })))
        )
      )
    )
  );
}
