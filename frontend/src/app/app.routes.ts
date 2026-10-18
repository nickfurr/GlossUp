import { Routes } from '@angular/router';
import { roleGuard } from './shared/role-guard';
import { Login } from './pages/login/login';
import { Vendedor } from './pages/vendedor/vendedor';
import { Cliente } from './pages/cliente/cliente';
import { Admin } from './pages/admin/admin';
import { Inicio } from './pages/inicio/inicio';
import { Simple } from './pages/simple/simple';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', component: Inicio },

  // Panel por rol: cada guard deja pasar solo a su tipo de usuario.
  { path: 'cliente', component: Cliente, canActivate: [roleGuard('CLIENTE')] },
  { path: 'vendedor', component: Vendedor, canActivate: [roleGuard('VENDEDOR')] },
  { path: 'admin', component: Admin, canActivate: [roleGuard('ADMIN')] },

  // Zona del cliente (placeholders, igual que en la versión anterior).
  { path: 'catalogo', component: Simple, canActivate: [roleGuard('CLIENTE')], data: { titulo: 'Catálogo' } },
  { path: 'compatibilidad', component: Simple, canActivate: [roleGuard('CLIENTE')], data: { titulo: 'Compatibilidad' } },
  { path: 'perfil', component: Simple, canActivate: [roleGuard('CLIENTE')], data: { titulo: 'Mi perfil dermatológico' } },
  { path: 'carrito', component: Simple, canActivate: [roleGuard('CLIENTE')], data: { titulo: 'Carrito' } },

  { path: '**', redirectTo: '' },
];
