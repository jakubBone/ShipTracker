import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LocationReport, LocationReportRequest } from '../models/location-report.model';

@Injectable({ providedIn: 'root' })
export class LocationReportService {
  private readonly http = inject(HttpClient);

  getByShipId(shipId: number): Observable<LocationReport[]> {
    return this.http.get<LocationReport[]>(`${environment.apiUrl}/ships/${shipId}/reports`);
  }

  create(shipId: number, report: LocationReportRequest): Observable<LocationReport> {
    return this.http.post<LocationReport>(`${environment.apiUrl}/ships/${shipId}/reports`, report);
  }
}
