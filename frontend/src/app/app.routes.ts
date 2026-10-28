import { Routes } from '@angular/router';
import { roleGuard } from './shared/role-guard';
import { Login } from './pages/login/login';
import { Vendedor } from './pages/vendedor/vendedor';
import { Cliente } from './pages/cliente/cliente';
import { Admin } from './pages/admin/admin';
import { Inicio } from './pages/inicio/inicio';
import { Perfil } from './pages/perfil/perfil';
import { Catalogo } from './pages/catalogo/catalogo';
import { Carrito } from './pages/carrito/carrito';
import { Compatibilidad } from './pages/compatibilidad/compatibilidad';
import { Producto } from './pages/producto/producto';
import { PagoResultado } from './pages/pago/pago-resultado';
import { Pedidos } from './pages/pedidos/pedidos';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', component: Inicio },

  // Panel por rol: cada guard deja pasar solo a su tipo de usuario.
  { path: 'cliente', component: Cliente, canActivate: [roleGuard('CLIENTE')] },
  { path: 'vendedor', component: Vendedor, canActivate: [roleGuard('VENDEDOR')] },
  { path: 'admin', component: Admin, canActivate: [roleGuard('ADMIN')] },

  // Zona del cliente.
  { path: 'catalogo', component: Catalogo, canActivate: [roleGuard('CLIENTE')] },
  { path: 'producto/:id', component: Producto, canActivate: [roleGuard('CLIENTE')] },
  { path: 'compatibilidad', component: Compatibilidad, canActivate: [roleGuard('CLIENTE')] },
  { path: 'perfil', component: Perfil, canActivate: [roleGuard('CLIENTE')] },
  { path: 'carrito', component: Carrito, canActivate: [roleGuard('CLIENTE')] },
  { path: 'pedidos', component: Pedidos, canActivate: [roleGuard('CLIENTE')] },
  // Wompi devuelve aquí al cliente tras pagar (HU10).
  { path: 'pago/resultado/:pedido', component: PagoResultado, canActivate: [roleGuard('CLIENTE')] },

  { path: '**', redirectTo: '' },
];
