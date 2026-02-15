import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly loggedIn = signal(false);
  private sessionCheck$: Observable<void> | null = null;

  login(username: string, password: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/login`, { username, password }).pipe(
      tap(() => {
        this.loggedIn.set(true);
        this.sessionCheck$ = of(undefined);
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/logout`, {}).pipe(
      tap(() => {
        this.loggedIn.set(false);
        this.sessionCheck$ = null;
        this.router.navigate(['/login']);
      })
    );
  }

  checkSession(): Observable<void> {
    if (!this.sessionCheck$) {
      this.sessionCheck$ = this.http.get<void>(`${environment.apiUrl}/auth/me`).pipe(
        tap(() => this.loggedIn.set(true)),
        catchError(() => {
          this.loggedIn.set(false);
          return of(undefined);
        }),
        shareReplay(1)
      );
    }
    return this.sessionCheck$;
  }

  isLoggedIn(): boolean {
    return this.loggedIn();
  }
}
