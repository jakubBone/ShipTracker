import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ShipService } from '../../../core/services/ship.service';
import { LocationReportService } from '../../../core/services/location-report.service';
import { Ship } from '../../../core/models/ship.model';
import { LocationReport } from '../../../core/models/location-report.model';
import { Timeline } from './timeline/timeline';
import { LocationReportForm } from './location-report-form/location-report-form';

@Component({
  selector: 'app-ship-detail',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    DatePipe,
    DecimalPipe,
    Timeline,
    LocationReportForm
  ],
  templateUrl: './ship-detail.html',
  styleUrl: './ship-detail.scss'
})
export class ShipDetail implements OnInit {
  private readonly shipService = inject(ShipService);
  private readonly locationReportService = inject(LocationReportService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly ship = signal<Ship | null>(null);
  protected readonly reports = signal<LocationReport[]>([]);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.shipService.getById(id).subscribe({
      next: (ship) => this.ship.set(ship)
    });
    this.locationReportService.getByShipId(id).subscribe({
      next: (reports) => this.reports.set(reports)
    });
  }

  protected onReportAdded(report: LocationReport): void {
    this.reports.update(current => [...current, report]);
  }

  protected goBack(): void {
    this.router.navigate(['/ships']);
  }
}
