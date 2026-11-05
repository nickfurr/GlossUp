import { Component } from '@angular/core';

/** Correo del canal de consultas y reclamos (Ley 1581 art. 14 y 15). */
const CORREO_DATOS = 'manuel.pinedav@uqvirtual.edu.co';

/**
 * Política de Tratamiento de Datos Personales (Ley 1581 de 2012, Decreto 1377 de 2013 art. 13).
 * Su versión debe coincidir con RegistrarUsuarioService.POLITICA_VIGENTE: es la que el registro guarda como aceptada.
 */
@Component({
  selector: 'app-privacidad',
  template: `
    <article class="card politica">
      <a class="back-link" href="/">← Volver a GlossUP</a>
      <span class="eyebrow">Versión 1.0 · vigente desde el 5 de noviembre de 2026</span>
      <h1>Política de Tratamiento de Datos Personales</h1>

      <h2>1. Responsable del tratamiento</h2>
      <p>
        GlossUP, proyecto académico de Ingeniería de Software de la Universidad del Quindío, Armenia (Quindío), Colombia.
        Correo de contacto: <a [href]="'mailto:' + correo">{{ correo }}</a>.
      </p>

      <h2>2. Datos que tratamos y para qué</h2>
      <ul>
        <li><strong>Cuenta:</strong> nombre, correo y contraseña cifrada. Para identificarte y darte acceso.</li>
        <li><strong>Pedidos y entregas:</strong> direcciones, teléfono de contacto, productos comprados y pagos. Para vender,
          despachar y emitir el comprobante de compra.</li>
        <li><strong>Perfil dermatológico:</strong> tipo de piel y cabello, pH, alergias y afecciones. Únicamente para avisarte
          si un producto puede no convenirte.</li>
      </ul>
      <p>No vendemos ni cedemos tus datos a terceros, y no los usamos para publicidad.</p>

      <h2>3. Datos sensibles</h2>
      <p>
        Tus alergias, afecciones y demás datos del perfil dermatológico son datos relativos a la salud y, por tanto,
        <strong>datos sensibles</strong> (Ley 1581 art. 5). Entregarlos es <strong>facultativo</strong>: puedes comprar sin
        llenar tu perfil. Solo los guardamos después de que lo autorizas expresamente en la página de tu perfil, y puedes
        retirar esa autorización en cualquier momento; al hacerlo los borramos.
      </p>

      <h2>4. Tus derechos</h2>
      <p>Como titular puedes (Ley 1581 art. 8):</p>
      <ul>
        <li>Conocer, actualizar y rectificar tus datos: desde tu perfil y los ajustes de tu cuenta.</li>
        <li>Pedir prueba de la autorización que nos diste: guardamos la fecha y la versión de esta política que aceptaste.</li>
        <li>Saber qué uso les hemos dado a tus datos.</li>
        <li>Revocar la autorización y pedir que borremos tus datos: puedes borrar tu perfil dermatológico o eliminar tu
          cuenta desde la aplicación.</li>
        <li>Presentar quejas ante la Superintendencia de Industria y Comercio después de agotar el trámite con nosotros.</li>
      </ul>

      <h2>5. Consultas y reclamos</h2>
      <p>
        Escríbenos a <a [href]="'mailto:' + correo">{{ correo }}</a> desde el correo de tu cuenta. Respondemos las
        consultas en máximo 10 días hábiles (prorrogables 5) y los reclamos en máximo 15 días hábiles (prorrogables 8),
        como indican los artículos 14 y 15 de la Ley 1581.
      </p>

      <h2>6. Dónde se guardan tus datos</h2>
      <p>
        La aplicación funciona en Microsoft Azure y la base de datos en Neon (sobre Amazon Web Services), ambos con
        servidores en Estados Unidos (región este, Virginia). Por eso tus datos se transmiten a ese país, a proveedores
        que solo los tratan por cuenta nuestra y con medidas de seguridad (Ley 1581 art. 26). Los pagos los procesa
        Wompi (Colombia); GlossUP no recibe ni guarda los datos de tu tarjeta.
      </p>

      <h2>7. Menores de edad</h2>
      <p>GlossUP es solo para mayores de 18 años. No recogemos a sabiendas datos de menores de edad.</p>

      <h2>8. Seguridad y conservación</h2>
      <p>
        Las contraseñas se guardan cifradas, la comunicación viaja cifrada (HTTPS) y solo tú puedes ver tu perfil
        dermatológico. Si eliminas tu cuenta borramos tu perfil y tus direcciones y dejamos la cuenta anónima; los
        pedidos y comprobantes se conservan porque la ley exige guardar los soportes contables (Ley 962 de 2005, art. 28).
      </p>

      <h2>9. Cambios a esta política</h2>
      <p>
        Si cambiamos algo sustancial de esta política, lo publicaremos aquí con una versión nueva y te pediremos de nuevo
        la autorización cuando corresponda.
      </p>
    </article>
  `,
  styles: `
    .politica { max-width: 760px; margin: 2rem auto; padding: 2rem; line-height: 1.6; }
    .politica h1 { margin: .4rem 0 1.2rem; }
    .politica h2 { font-size: 1.05rem; margin: 1.6rem 0 .4rem; }
    .politica ul { padding-left: 1.2rem; }
    .politica .back-link { display: block; margin-bottom: 1rem; }
    @media (max-width: 600px) { .politica { margin: 0; border-radius: 0; padding: 1.2rem 1rem; } }
  `,
})
export class Privacidad {
  readonly correo = CORREO_DATOS;
}
