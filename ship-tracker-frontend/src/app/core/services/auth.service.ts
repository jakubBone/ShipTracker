import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly loggedIn = signal(false);

  login(username: string, password: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/login`, { username, password }).pipe(
      tap(() => this.loggedIn.set(true))
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/logout`, {}).pipe(
      tap(() => {
        this.loggedIn.set(false);
        this.router.navigate(['/login']);
      })
    );
  }

  isLoggedIn(): boolean {
    return this.loggedIn();
  }
}
