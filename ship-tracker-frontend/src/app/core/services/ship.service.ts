import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Ship, ShipRequest } from '../models/ship.model';

@Injectable({ providedIn: 'root' })
export class ShipService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ships`;

  getAll(): Observable<Ship[]> {
    return this.http.get<Ship[]>(this.apiUrl);
  }

  getById(id: number): Observable<Ship> {
    return this.http.get<Ship>(`${this.apiUrl}/${id}`);
  }

  create(ship: ShipRequest): Observable<Ship> {
    return this.http.post<Ship>(this.apiUrl, ship);
  }

  update(id: number, ship: ShipRequest): Observable<Ship> {
    return this.http.put<Ship>(`${this.apiUrl}/${id}`, ship);
  }

  generateName(): Observable<{ name: string }> {
    return this.http.get<{ name: string }>(`${this.apiUrl}/generate-name`);
  }
}
