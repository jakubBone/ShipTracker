import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { Login } from './features/auth/login/login';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];
