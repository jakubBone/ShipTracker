import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { ShipService } from '../../../core/services/ship.service';

const SHIP_TYPES = ['Cargo', 'Tanker', 'Container', 'Bulk Carrier'];

@Component({
  selector: 'app-ship-form',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatDatepickerModule
  ],
  templateUrl: './ship-form.html',
  styleUrl: './ship-form.scss'
})
export class ShipForm implements OnInit {
  private readonly shipService = inject(ShipService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly shipTypes = SHIP_TYPES;
  protected readonly isLoading = signal(false);
  protected readonly nameError = signal('');
  protected editId: number | null = null;

  protected readonly form = new FormGroup({
    name:       new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    launchDate: new FormControl<Date | null>(null, [Validators.required]),
    shipType:   new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    tonnage:    new FormControl<number | null>(null, [Validators.required, Validators.min(1)])
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = Number(id);
      this.shipService.getById(this.editId).subscribe({
        next: (ship) => {
          this.form.patchValue({
            name: ship.name,
            launchDate: new Date(ship.launchDate),
            shipType: ship.shipType,
            tonnage: ship.tonnage
          });
        }
      });
    }
  }

  protected get isEditMode(): boolean {
    return this.editId !== null;
  }

  protected generateName(): void {
    this.nameError.set('');
    this.shipService.generateName().subscribe({
      next: ({ name }) => this.form.patchValue({ name }),
      error: () => this.nameError.set('Nie udało się pobrać nazwy. Spróbuj ponownie.')
    });
  }

  protected submit(): void {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    const value = this.form.getRawValue();
    const request = {
      name: value.name,
      launchDate: this.toIsoDate(value.launchDate as Date),
      shipType: value.shipType,
      tonnage: value.tonnage as number
    };

    const operation = this.isEditMode
      ? this.shipService.update(this.editId!, request)
      : this.shipService.create(request);

    operation.subscribe({
      next: () => this.router.navigate(['/ships']),
      error: () => this.isLoading.set(false)
    });
  }

  protected cancel(): void {
    this.router.navigate(['/ships']);
  }

  private toIsoDate(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}
