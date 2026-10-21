# Cifrado de secretos en la base y secretos fuera del deploy

Fecha: 2026-10-21. Paso 9 del plan de seguridad.

## Objetivo

Hoy la base guarda en claro:

- la clave privada de firma de cada empresa (`FirmaDigital.privateKey`, PEM): con ella se firman CFE, sobres y pedidos a
  DGI en nombre de la empresa;
- la contraseña del correo de cada empresa (`Empresa.passRecepcion`, IMAP/SMTP);
- el secreto HMAC de cada credencial de integración (`Credential.secret`): con él se llama a la API como esa integración.

Ademas, los secretos de producción (contraseña de la base, secreto de Play, contraseña del correo del sistema) viajan en
`deploy_conf/prod/application.conf`: están en la máquina desde la que se despliega y en una copia de `conf` por cada
versión desplegada.

**Éxito:**

1. Un volcado o un backup de la base no contiene ninguna clave privada, contraseña de correo ni secreto HMAC usable.
2. Ninguna copia de `deploy_conf`, de `deploys/` ni del zip del deploy contiene secretos de producción.
3. Firmar CFE, hablar con DGI, el poller de correo y las llamadas HMAC siguen funcionando igual.
4. La migración es idempotente y se puede revertir.

**Contra qué protege:** filtración de la base o de sus backups; filtración de la máquina de deploy o de las carpetas
de deploy. **Contra qué no:** quien controle el servidor de la API (tiene la clave maestra y el proceso). Tampoco
vuelve inútiles los backups viejos, que tienen las claves en claro.

## Decisiones

| Tema | Decisión | Alternativas descartadas |
|---|---|---|
| Dónde se cifra | En la aplicación, AES-256-GCM | pgcrypto (la clave viaja en cada SQL y puede quedar en los logs de PostgreSQL); cifrado de disco (no protege volcados); archivos `.p12` por empresa (más piezas móviles, misma protección); Vault/KMS y HSM (infraestructura nueva) |
| Dónde vive la clave maestra | Archivo en el servidor de la API, fuera de las carpetas por versión, solo legible por el usuario de la aplicación; copia de respaldo en el gestor de contraseñas, aparte de los backups de la base | En `deploy_conf` con cada deploy (quedaría junto a la contraseña de la base) |
| Columnas | `FirmaDigital.privateKey`, `Empresa.passRecepcion`, `Credential.secret` | Datos de negocio (CFE, sobres): poca ganancia, complican búsquedas; contraseñas de usuario: ya son hash PBKDF2 |
| Atar cada valor a | La columna (contexto `"Entidad.campo"`) | La fila (RUT o id): código más frágil y un cambio de RUT dejaría de descifrar; solo protege contra quien ya puede escribir en la base |
| Secretos de producción | Un archivo de secretos en el servidor incluido desde `application.conf` (`include file(...)`) | Variables de entorno de systemd (equivalente, más disperso) |

## Parte 9a: secretos fuera del deploy

- En el servidor (test y producción): `/srv/java_efactura_uy/secrets/<ambiente>.conf`, `chmod 400`, dueño el usuario de
  la aplicación. Contiene `db.default.password`, `play.crypto.secret`, `mail.sistema.password` y las rutas
  `seguridad.cifrado.*` de la parte 9b.
- `deploy_conf/<ambiente>/application.conf` deja de tener esos valores y agrega al final:
  `include file("/srv/java_efactura_uy/secrets/<ambiente>.conf")` (Typesafe Config 1.3.0, el de Play 2.4, lo soporta).
- Los scripts de deploy no tocan esa carpeta.
- Si el archivo falta, `include file` no falla: la API arranca sin contraseña de la base y no conecta. El arranque tiene
  que dejarlo claro en el log (ver Errores).
- El contenido del archivo se respalda en el gestor de contraseñas.
- Es solo configuración: no cambia código (salvo el aviso de arranque).

## Parte 9b: cifrado de columnas

### Componentes (`app/com/bluedot/commons/security/`)

1. **`CifradoCampos`**: el único que conoce AES. Sin Play ni base.
   - `cifrar(texto, contexto)` → `enc:v1:<idClave>:<base64(nonce 12 bytes + cifrado + tag 16 bytes)>`.
   - `descifrar(valor, contexto)` → texto; falla si el tag no coincide (valor modificado, clave o contexto incorrecto).
   - `estaCifrado(valor)`: prefijo `enc:`.
   - Nonce aleatorio de 12 bytes por valor (`SecureRandom`), nunca repetido con la misma clave. El contexto
     (`"FirmaDigital.privateKey"`, etc.) va como datos asociados (AAD).
2. **`ClaveMaestra`**: de dónde salen las claves. Es la única pieza que cambiaría para usar Vault/KMS.
   - Configuración: `seguridad.cifrado.claves.<id> = "<ruta del archivo>"` (32 bytes aleatorios, generados en el
     servidor con `openssl rand 32`), `seguridad.cifrado.claveActual = "<id>"`, `seguridad.cifrado.modo`.
   - Lee las claves al arrancar y las tiene solo en memoria. El id (`k1`, `k2`, …) va en cada valor cifrado para poder
     rotar: con una clave nueva como actual se cifra con ella y se siguen leyendo las anteriores.
3. **`CampoCifrado`**: `AttributeConverter` de JPA 2.1 (Hibernate 4.3) que usa los dos anteriores. Se aplica con
   `@Convert` a las tres columnas y se registra en `persistence.xml`. Los getters y setters no cambian: firma,
   DGI, correo y HMAC siguen igual.

### Modos (`seguridad.cifrado.modo`)

| Modo | Escribe | Lee en claro | Lee `enc:` |
|---|---|---|---|
| `off` (si falta o es inválido) | en claro | sí | error claro (no devuelve basura) |
| `cifrar` | cifrado con la clave actual | sí | sí |
| `exigir` | cifrado con la clave actual | error | sí |

La opción va en `conf/application.conf` (con comentarios) y en los dos `deploy_conf`, con default seguro en el código.

### Arranque

Con un modo distinto de `off`, si falta un archivo de clave, no se puede leer, no tiene 32 bytes o `claveActual` no está
entre las claves, la API no arranca y el log dice cuál. Nunca se loguean claves ni valores, solo cantidades.

### Migración y reversión (solo ADMIN, siempre)

- `POST /api/v1/procesos/cifrado/migrar`: cifra los valores en claro y recifra los que usan una clave que no es la
  actual. Devuelve cantidades por columna. Idempotente. Como con el converter Hibernate no detecta cambios (ve el mismo
  texto en claro), escribe con `UPDATE` explícitos fila por fila, en una transacción por tabla.
- `POST /api/v1/procesos/cifrado/revertir`: descifra todo a claro, para poder volver a una versión de la API anterior
  a este cambio. Necesita las claves cargadas: se ejecuta en `cifrar` (en `exigir` se rechaza) y después se pone
  `modo = off`.
- Cada ejecución queda en la auditoría y en la tabla de procesos.
- El panel no cambia por ahora: se ejecutan con una llamada desde la terminal.

### Errores

- Un valor que no se puede descifrar: error que nombra la columna, nunca el valor (en la migración también el id de la
  fila; un converter de JPA no sabe qué fila convierte). La operación que lo
  necesitaba (firmar, conectar al correo, verificar HMAC) falla visible.
- Migración: si una fila falla, se revierte esa tabla y se informa cuál fila y por qué; las otras tablas no se tocan.
- 9a: si al arrancar falta la contraseña de la base (archivo de secretos no incluido), el log lo dice explícitamente.

## Puesta en marcha (test primero, después producción)

1. 9a: crear el archivo de secretos en el servidor y el `include` en `deploy_conf`; reiniciar y verificar.
2. Desplegar 9b con `modo = off`: no cambia nada.
3. Generar `k1` en el servidor, configurar `modo = cifrar`, reiniciar.
4. Antes en producción: backup de la base y `k1` en el gestor de contraseñas. Ejecutar `migrar` y verificar: sin valores
   en claro en las tres columnas; emitir un CFE (firma y respuesta de DGI); una llamada HMAC; un ciclo del poller de
   correo.
5. Después de unos días sin problemas: `modo = exigir`.

## Pruebas

- `CifradoCampos`: ida y vuelta; dos cifrados del mismo texto dan valores distintos; fallan el valor modificado, la
  clave incorrecta, el contexto incorrecto y un id de clave desconocido; `estaCifrado`.
- `ClaveMaestra`: archivo faltante o de otro tamaño falla al arrancar; sin claves y en `off` funciona.
- `CampoCifrado`: en `off`, `cifrar` y `exigir`, con valores en claro y cifrados.
- En test: migrar, verificar en la base, emitir una e-Boleta, una llamada HMAC, un ciclo del poller de correo;
  `revertir` y `migrar` de nuevo con el mismo resultado.

## Fuera de alcance

- Vault/KMS o HSM (el diseño deja `ClaveMaestra` como el punto a cambiar).
- Renovar los certificados de las empresas (solo si se expuso un backup).
- Hash de los tokens de sesión (paso de endurecimiento).
- Botón en el panel para migrar y revertir.
