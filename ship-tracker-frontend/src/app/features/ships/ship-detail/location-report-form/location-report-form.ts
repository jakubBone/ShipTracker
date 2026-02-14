import { Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { COUNTRIES } from '../../../../core/data/countries.data';
import { LocationReportService } from '../../../../core/services/location-report.service';
import { LocationReport } from '../../../../core/models/location-report.model';

@Component({
  selector: 'app-location-report-form',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule
  ],
  templateUrl: './location-report-form.html',
  styleUrl: './location-report-form.scss'
})
export class LocationReportForm {
  @Input({ required: true }) shipId!: number;
  @Output() reportAdded = new EventEmitter<LocationReport>();

  private readonly locationReportService = inject(LocationReportService);

  protected readonly countries = COUNTRIES;
  protected readonly isLoading = signal(false);

  protected readonly form = new FormGroup({
    reportDate: new FormControl<Date | null>(null, [Validators.required]),
    country:    new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    port:       new FormControl<string>('', { nonNullable: true, validators: [Validators.required] })
  });

  protected submit(): void {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    const value = this.form.getRawValue();
    const request = {
      reportDate: this.toIsoDate(value.reportDate as Date),
      country: value.country,
      port: value.port
    };

    this.locationReportService.create(this.shipId, request).subscribe({
      next: (report) => {
        this.form.reset();
        this.isLoading.set(false);
        this.reportAdded.emit(report);
      },
      error: () => this.isLoading.set(false)
    });
  }

  private toIsoDate(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}
