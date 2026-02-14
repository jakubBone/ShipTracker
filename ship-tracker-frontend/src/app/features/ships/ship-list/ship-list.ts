import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ShipService } from '../../../core/services/ship.service';
import { Ship } from '../../../core/models/ship.model';

@Component({
  selector: 'app-ship-list',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    DatePipe,
    DecimalPipe
  ],
  templateUrl: './ship-list.html',
  styleUrl: './ship-list.scss'
})
export class ShipList implements OnInit {
  private readonly shipService = inject(ShipService);
  private readonly router = inject(Router);

  protected readonly ships = signal<Ship[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly displayedColumns = ['name', 'shipType', 'tonnage', 'launchDate', 'reportCount', 'actions'];

  ngOnInit(): void {
    this.loadShips();
  }

  private loadShips(): void {
    this.isLoading.set(true);
    this.shipService.getAll().subscribe({
      next: (ships) => {
        this.ships.set(ships);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  protected navigateToDetails(id: number): void {
    this.router.navigate(['/ships', id]);
  }

  protected navigateToEdit(id: number): void {
    this.router.navigate(['/ships', id, 'edit']);
  }

  protected navigateToNew(): void {
    this.router.navigate(['/ships/new']);
  }
}