import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { Login } from './features/auth/login/login';
import { ShipList } from './features/ships/ship-list/ship-list';
import { ShipForm } from './features/ships/ship-form/ship-form';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'ships', component: ShipList, canActivate: [authGuard] },
  { path: 'ships/new', component: ShipForm, canActivate: [authGuard] },
  { path: 'ships/:id/edit', component: ShipForm, canActivate: [authGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];