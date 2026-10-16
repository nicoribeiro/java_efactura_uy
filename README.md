# java_efactura_uy

API REST para la facturación electrónica de Uruguay: emite comprobantes fiscales electrónicos (CFE), los firma, los
envía a DGI y a los receptores, recibe los CFE de los proveedores, arma el reporte diario y genera los PDF, todo con
JSON.

> **English summary.** REST API (Java 8, Play Framework 2.4) that wraps Uruguay's electronic invoicing protocol
> (*e-factura*, DGI). It issues electronic tax documents (CFE) from JSON, signs them (XMLDSig), sends them to DGI's SOAP
> services and to the receiving companies by email, receives and validates incoming CFE from suppliers via IMAP,
> builds and sends the daily report (*reporte diario*), and renders PDFs. It supports several companies, users with
> roles, session or HMAC authentication, and background jobs for the recurring tasks. A web admin panel lives in
> [efactura-admin](https://github.com/nicoribeiro/efactura-admin). The rest of this document is in Spanish.

## Qué hace

- **Emisión**: recibe el comprobante en JSON, le asigna número de un CAE vigente, lo firma, lo envía a DGI dentro de
  un sobre y consulta el resultado. Genera el PDF con el código QR.
- **Envío al receptor**: cuando DGI acepta el comprobante, lo envía por mail al receptor electrónico (sobre e
  intercambio según DGI) y registra cada envío.
- **Recepción**: lee por IMAP los buzones de las empresas, valida los sobres y los CFE recibidos (controles de
  formato, firma, duplicados, montos), responde el acuse del sobre (M) y la aceptación o rechazo de los comprobantes
  (ME), incluido el rechazo comercial (E20 a E26).
- **Reporte diario** a DGI, manual o automático.
- **Consultas a DGI**: resultado de los comprobantes, datos de un RUT y padrón de emisores electrónicos.
- **Cotizaciones**: descarga la cotización del dólar del INE; con ella y la UI del año controla el tope de 5000 UI de
  los eTickets en moneda extranjera.
- **Multiempresa**: cada empresa tiene su firma digital, sucursales, CAE, buzón de correo y usuarios con acceso.

## Tipos de comprobante

| Tipo | Emisión | Recepción |
|---|---|---|
| 101 eTicket, 111 eFactura | Sí | Sí |
| 102, 103, 112, 113 notas de crédito y débito | Sí (con `Referencia`) | Sí |
| 121 eFactura de exportación | Sí | Sí |
| 122, 123 notas de exportación | Sí (con `Referencia`) | Sí |
| 124 eRemito de exportación | Sí | Sí |
| 131-133, 141-143 venta por cuenta ajena | Sí (con `CompFiscal`) | Sí |
| 182 eResguardo | Sí (con `Referencia`) | Sí |
| 181 eRemito | No | Sí |
| e-Boleta (151-153) | No | No |

Los tipos de contingencia (2xx) no se piden directamente: con el sistema en modo contingencia
(`POST /api/v1/modo/CONTINGENCIA`, y `NORMAL` para volver) los comprobantes se emiten como su tipo 2xx. Los formatos
y XSD son los de DGI versión 1.44.2 (`resources/xsd/XSDs_FE_1_44_2`).

## Requisitos

- **Java 8** (el build usa Zulu 8). No funciona con versiones posteriores.
- **sbt** 0.13 (el proyecto usa sbt 0.13.8, Play 2.4.11 y Scala 2.11.12).
- **PostgreSQL**. Hibernate crea y actualiza las tablas (`hibernate.hbm2ddl.auto=update`); los cambios que no puede
  hacer solo están en `resources/sql/` y `scripts/sql/`.
- **[play4jpa](https://github.com/nicoribeiro/play4jpa)** publicado en el repositorio local de sbt.
- **Firma digital** de la empresa (ver abajo) y su **habilitación como emisor electrónico** ante DGI.

## Puesta en marcha

```bash
git clone https://github.com/nicoribeiro/play4jpa.git
cd play4jpa && sbt publishLocal && cd ..

git clone https://github.com/nicoribeiro/java_efactura_uy.git
cd java_efactura_uy
```

Crear una base vacía en PostgreSQL y configurar `conf/application.conf` (ver [Configuración](#configuración)). Después:

```bash
sbt "run 9000"
```

La primera vez sbt tarda varios minutos en descargar las dependencias. La API queda en `http://localhost:9000/api/v1`
y la documentación Swagger en `GET /api/v1/documentacion`.

### Primeros pasos

1. **Primer administrador**: `POST /api/v1/signup`. Solo funciona mientras no exista ningún usuario ADMIN.
2. **Ingreso**: `POST /api/v1/signin` devuelve el token de sesión, que se manda en el encabezado `AUTH-TOKEN`.
3. **Datos iniciales**: `POST /api/v1/init` carga países, tasas de IVA y valores de UI si las tablas están vacías.
4. **Empresa**: `POST /api/v1/empresa` y sus sucursales (`POST /api/v1/empresa/:rut/sucursales`), con el código de
   sucursal que asignó DGI.
5. **Firma digital** y **CAE** de la empresa (ver abajo).
6. **Emitir**: `POST /api/v1/empresa/:rut/documentos`.

Todo esto también se puede hacer desde el panel [efactura-admin](https://github.com/nicoribeiro/efactura-admin).

## Firma digital

La firma de la empresa la emiten Abitab o el Correo Uruguayo a partir de un pedido PKCS#10:

```bash
openssl req -new -newkey rsa:2048 -nodes -keyout clave.pem -out pedido.pem \
  -subj '/CN={NombreFantasia}/O={RazonSocial},SERIALNUMBER=RUC{RUT}/ST={Ciudad}/C=UY'
openssl req -in pedido.pem -noout -verify -key clave.pem
```

Guardar `clave.pem` en un lugar seguro: sin ella el certificado no sirve. El contenido de `pedido.pem` se pega en el
formulario de la entidad certificadora, que después envía el certificado (`.cer`). Para verificarlo contra la cadena
de la entidad (ej. Abitab):

```bash
openssl verify -CAfile acrn.crt -untrusted abitab.crt {NombreFantasia}.cer
openssl x509 -in {NombreFantasia}.cer -noout -subject -dates
```

La firma se carga con `PUT /api/v1/empresa/:rut` (o desde el panel, *Empresas → Cargar firma digital*), de una de dos
formas:

- `pkcs12`: el archivo `.p12`/`.pfx` en base64, con `pkcs12Password`.
- `certificado` y `privateKey` en PEM.

La API verifica que la clave corresponda al certificado y que esté vigente. Si el certificado no menciona el RUT de
la empresa, hay que confirmarlo con `"forzarFirma": true`.

## CAE

Los rangos de numeración (CAE) que otorga DGI se cargan con `POST /api/v1/empresa/:rut/cae` (solo ADMIN):

```json
{ "NA": 90000000001, "TCFE": 111, "Serie": "A", "DNro": 1, "HNro": 10000, "FVD": "2027-12-31" }
```

La API rechaza los CAE de otra empresa, los duplicados y los rangos superpuestos. Desde el panel se pueden cargar
directamente los XML de CAE que descarga DGI. `GET /api/v1/empresa/:rut/cae` lista los CAE con su uso.

## Emisión

`POST /api/v1/empresa/:rut/documentos` con el comprobante en JSON. Los nombres de los campos son los del formato de
DGI (`Encabezado`, `Detalle`, `Referencia`, `CompFiscal`, `Adenda`...). Además:

- `Encabezado.IdDoc.TipoCFE`: el tipo de comprobante.
- `Encabezado.IdDoc.id`: un identificador propio del sistema que genera el comprobante. No se puede repetir, así un
  reintento no emite dos veces el mismo comprobante.
- `Encabezado.Emisor.CdgDGISucur`: la sucursal que emite.

La respuesta es sincrónica: incluye el comprobante con su serie, número y CAE, y el estado del envío a DGI. Otras
operaciones sobre los comprobantes:

| Ruta | Para qué |
|---|---|
| `GET /api/v1/empresa/:rut/documentos` | Listado paginado con filtros |
| `GET .../documentos/resultado` | Consulta en DGI el resultado de un comprobante |
| `GET /api/v1/empresa/:rut/resultados/:fecha` | Consulta en DGI los resultados de todos los sobres de una fecha |
| `GET .../documentos/pdf` | PDF del comprobante |
| `PUT .../documentos` | Reenvía el sobre a DGI |
| `DELETE .../documentos` | Anula el comprobante |
| `POST .../documentos/reenviarMail` | Reenvía el comprobante al receptor por mail |

## Recepción

Cada empresa configura su buzón (servidor IMAP, usuario y contraseña) y, con el proceso `EmailEntrantesPoller`
activo, la API:

1. Lee los emails nuevos de cada carpeta del buzón.
2. Valida el sobre (controles S02 a S06 y S20) y responde el acuse (M).
3. Valida cada comprobante (controles E02 a E05, E07, E10 y E12).
4. Envía la respuesta de los comprobantes (ME) cuando vence el plazo de la empresa (`diasRespuestaCfe`), o cuando
   se pide a mano. Los comprobantes se pueden rechazar comercialmente antes (E20 a E26).

Cada control se puede configurar en `log` (solo registra) o `enforce` (rechaza). Los emails que no se pudieron
procesar se pueden reintentar u omitir.

## Reporte diario

`POST /api/v1/empresa/:rut/reporteDiario/:fecha` genera, firma y envía el reporte diario de una fecha
(`?soloFaltantes` y `?cantReportes` para generar varios días). Con `ReporteDiarioPoller` activo se generan solos los
faltantes de las empresas con firma digital.

## Seguridad

- **Sesión**: `POST /api/v1/signin` devuelve un token que se manda en el encabezado `AUTH-TOKEN`. Las sesiones
  vencen por inactividad (8 h) y por duración máxima (7 días).
- **HMAC-SHA256** para integraciones: encabezados `ACCESS_KEY`, `ACCESS_SIGNATURE` y `ACCESS_NONCE`. La firma es
  `HMAC(nonce + URL + cuerpo, secreto)` y el nonce tiene que ser creciente. Las credenciales se generan por usuario
  (`/api/v1/usuarios/:uid/credenciales`).
- **Roles**: `ADMIN` (todo) y `USER` (las empresas que tenga asignadas).
- **Contraseñas** con PBKDF2 y sal; recuperación con un código por mail. Los usuarios nuevos confirman su email y
  cambian la contraseña temporal antes de usar la API.
- **Límite de intentos de login** por usuario y por IP.

Los controles nuevos se activan en tres modos, `off`, `log` y `enforce`, para poder revisarlos antes de que bloqueen:

| Clave | Qué controla |
|---|---|
| `seguridad.accesoEmpresa` | Que un USER solo vea sus empresas |
| `seguridad.sesion.expiracion` (`inactividadMinutos`, `duracionMaximaHoras`) | Vencimiento de las sesiones |
| `seguridad.signin.limite` (`intentosPorUsuario`, `intentosPorIp`, `ventanaMinutos`) | Intentos de login |

`GET /api/v1/ping` (para el monitoreo) no pide sesión pero solo responde a las redes de `seguridad.ping.redes`.

## Procesos automáticos

Vienen desactivados (`run = false`) y conviene activarlos en una sola instancia de la API. Su estado y ejecuciones se
ven en `GET /api/v1/procesos` o en el panel.

| Proceso | Qué hace | Claves |
|---|---|---|
| `EmailEntrantesPoller` | Recepción de CFE por IMAP | `run`, `intervaloMinutos` |
| `RespuestaCfePoller` | Respuesta (ME) a los comprobantes recibidos | `run`, `intervaloMinutos`, `maximo` |
| `ReporteDiarioPoller` | Reportes diarios faltantes | `run`, `hora`, `intervaloMinutos`, `diasDesde`, `cantReportes`, `empresas` |
| `TipoDeCambioPoller` | Cotización del dólar del INE | `run`, `intervaloMinutos`, `diasAtras` |
| `EmisoresElectronicosPoller` | Padrón de emisores electrónicos de DGI | `run`, `hora`, `intervaloMinutos`, `diasAtras` |
| `NotificationPoller` | Avisos de CAE y firmas por vencer | `run`, `intervaloMinutos` |

## Configuración

`conf/application.conf` (desarrollo) y `deploy_conf/{test,prod}/application.conf` (cada ambiente; no se versionan).
Las principales:

| Clave | Uso |
|---|---|
| `db.default.url`, `username`, `password` | Base de datos PostgreSQL |
| `play.crypto.secret` | Secreto de Play |
| `environment` | `test` o `produccion`: qué servicios de DGI se usan |
| `service.{recepcion,consulta,rut}.url.{test,produccion}` | URLs de los web services de DGI |
| `mail.sistema.*` | SMTP del sistema (códigos de confirmación y recuperación, notificaciones) |
| `mail.subject`, `mail.body` | Texto de los mails con los comprobantes a los receptores |
| `mail.recepcion.*` | Recepción por IMAP (carpetas excluidas, días de la primera sincronización) |
| `RecepcionCFE.control.*`, `RecepcionSobre.control.*` | Controles de recepción (`log` o `enforce`) |
| `RespuestaCFE.diasPorDefecto` | Días de espera para responder los comprobantes recibidos |
| `documentos.pdf.path` | Carpeta de los PDF generados |
| `seguridad.*` | Ver [Seguridad](#seguridad) |
| `<Poller>.*` | Ver [Procesos automáticos](#procesos-automáticos) |

Los mails a los receptores salen con el servidor y la cuenta de cada empresa.

## API

Todas las rutas están en `conf/routes` y documentadas con Swagger en `GET /api/v1/documentacion`. Por área:

| Área | Rutas |
|---|---|
| Sesión y cuenta | `/signin`, `/signup`, `/cuenta`, `/recuperar` |
| Usuarios (ADMIN) | `/usuarios`, `/usuarios/:uid/...` (contraseña, sesiones, credenciales, empresas) |
| Empresas | `/empresa`, `/empresa/:rut`, `/empresa/:rut/sucursales`, `/empresa/:rut/logo` |
| CAE | `/empresa/:rut/cae` |
| Comprobantes | `/empresa/:rut/documentos/...` |
| Sobres | `/empresa/:rut/sobres`, `/empresa/:rut/sobre`, respuestas y rechazos de los recibidos |
| Emails recibidos | `/empresa/:rut/emails`, `/empresa/:rut/emails/carpetas`, reintentos |
| Reporte diario | `/empresa/:rut/reporteDiario/:fecha` |
| DGI | `/rut/:rut`, `/emisoresElectronicos/...` |
| Cotizaciones | `/tiposDeCambio`, `/ui` |
| Operación | `/procesos`, `/ping`, `/version`, `/modo/:modo` |

Los errores se devuelven como `{"result_code", "result_message", "result_detail"}` (`result_code` 0 es OK).

## Build y deploy

```bash
scripts/build.sh                       # sbt dist: builds/java_efactura_uy-<version>.zip
scripts/deploy.sh <test|prod>          # arma deploys/<ambiente> con su application.conf
scripts/pushDeploy.sh <test|prod>      # lo copia a los servidores del ambiente
scripts/EFACTURA-cluster.sh restart-full <test|prod>
```

- La versión es `conf/major.conf`.`conf/minor.conf`. `GET /api/v1/version` devuelve la versión, el commit y la fecha
  de compilación.
- Los servidores de cada ambiente se definen en `conf/servers.conf` (no se versiona), una línea por servidor:
  `nombre | ip | puerto ssh | ambiente | clave ssh | usuario`.
- `EFACTURA-cluster.sh` también acepta `start`, `stop`, `restart-hot` y `status`.
- `scripts/java_efactura_uy.service` es una unidad de systemd para levantarla como servicio.

### Clases de DGI

Las clases Java de los formatos de DGI (`dgi.classes.*`) se generan con JAXB a partir de los XSD:

```bash
scripts/generar-clases-dgi.sh   # por defecto resources/xsd/XSDs_FE_1_44_2
```

Necesita el `xjc` del JDK 8 y python3.

## Tests

```bash
sbt test
```

Hay tests unitarios de los builders de exportación, la recepción, el hash de contraseñas, los códigos por mail y el
límite de login. `RegresionClasesDgiTest` compara las clases de DGI contra una muestra de comprobantes reales (no
incluida) y se corre a mano.

## Notas

- `app/org/apache/xml/security/c14n/helper/C14nHelper.java` es una copia modificada de la clase de xmlsec que acepta
  como absoluto el namespace relativo que usa el servicio de consulta de RUT de DGI. Funciona porque las clases de la
  aplicación van primero en el classpath; hay que revisarla si se actualiza xmlsec.
- Panel web: [efactura-admin](https://github.com/nicoribeiro/efactura-admin).

## Autores

- **Nicolas Ribeiro** - [Bluedot](https://github.com/nicoribeiro)

Ver también [CONTRIBUTORS.md](CONTRIBUTORS.md) y los
[colaboradores](https://github.com/nicoribeiro/java_efactura_uy/contributors). Agradecimientos a
[LerandGroup](https://lerandgroup.com.uy).

## Licencia

[MIT](LICENSE.md)
