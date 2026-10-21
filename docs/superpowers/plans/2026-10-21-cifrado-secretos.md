# Cifrado de secretos en la base: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** que la clave privada de firma, la contraseña del correo y el secreto HMAC se guarden cifrados en la base, con
la clave maestra y los demás secretos de producción fuera del deploy.

**Architecture:** tres piezas en `com.bluedot.commons.security`: `CifradoCampos` (AES-256-GCM puro), `ClaveMaestra`
(claves y modo, desde la configuración) y `CampoCifrado` (`AttributeConverter` de JPA en las tres columnas). La migración
y la reversión las hace `MigracionCifrado` (`com.bluedot.efactura.commons`) con SQL explícito, expuesta por un controlador solo ADMIN. Los secretos de
producción pasan a un archivo en el servidor incluido desde `application.conf`.

**Tech Stack:** Java 8 (javax.crypto, sin dependencias nuevas), Play 2.4, Hibernate 4.3 (JPA 2.1 converters), play4jpa,
Typesafe Config 1.3.0, JUnit 4, sbt 0.13.8.

**Spec:** `docs/superpowers/specs/2026-10-21-cifrado-secretos-design.md`

## Global Constraints

- Algoritmo `AES/GCM/NoPadding`, clave de 32 bytes, nonce de 12 bytes de `SecureRandom` por valor, tag de 128 bits.
- Formato: `enc:v1:<idClave>:<base64(nonce + cifrado + tag)>` (Base64 estándar, sin saltos de línea).
- Contextos (AAD, UTF-8): `"FirmaDigital.privateKey"`, `"Empresa.passRecepcion"`, `"Credential.secret"`.
- Configuración: `seguridad.cifrado.modo` (`off` | `cifrar` | `exigir`; si falta o es inválido: `off`),
  `seguridad.cifrado.claves.<id> = "<ruta>"`, `seguridad.cifrado.claveActual = "<id>"`. Toda opción nueva va en
  `conf/application.conf` (con comentario) y en `deploy_conf/{test,prod}/application.conf` (no versionados).
- Nunca loguear claves ni valores (ni en claro ni cifrados); solo columna, id de fila y cantidades.
- Repos públicos: nada de RUTs, nombres de clientes o empresas, hosts ni IPs en código, tests, commits ni PRs.
- Commits a nombre del usuario, sin mencionar IA. Trabajar en un worktree propio (no en el checkout principal), rama
  desde `origin/main`, PR a `main`.
- `sbt test`: `IntegrationTest` ya falla en `main`; el resto tiene que pasar. Java 8 (`zulu-8`).

## Review Focus

- **Valores vacíos o null** (`passRecepcion` vacío en muchas empresas): el converter los deja como están, no cifra `""`.
  Test en Task 3.
- **Un valor cifrado con un id de clave que ya no está configurado** (se borró `k1` antes de recifrar): error claro con
  el id de clave, no `NullPointerException`. Test en Task 1.
- **PEM de varios KB con `\r\n`**: la ida y vuelta devuelve exactamente el mismo texto. Test en Task 1.
- **`migrar` o `revertir` con `modo = off`** (o `revertir` en `exigir`): se rechaza antes de tocar la base. Test en Task 4.
- **Uso concurrente** (`Credential.secret` se descifra en cada pedido HMAC): sin `Cipher` compartido entre hilos. Test en
  Task 1.

---

### Task 1: `CifradoCampos` (AES-256-GCM)

**Files:**
- Create: `app/com/bluedot/commons/security/CifradoCampos.java`
- Test: `test/com/bluedot/commons/security/CifradoCamposTest.java`

**Interfaces:**
- Produces:
  - `public static String cifrar(String texto, String contexto, String idClave, javax.crypto.SecretKey clave)`
  - `public static String descifrar(String valor, String contexto, java.util.Map<String, javax.crypto.SecretKey> claves)`
    → texto; lanza `CifradoCampos.ErrorCifrado` (unchecked) con mensaje sin el valor
  - `public static boolean estaCifrado(String valor)` (`valor != null && valor.startsWith("enc:")`)
  - `public static String idClave(String valor)` → el `<idClave>` de un valor cifrado
  - `public static class ErrorCifrado extends RuntimeException`

- [ ] **Step 1: Write the failing tests** (claves de prueba generadas en el test con `KeyGenerator.getInstance("AES")` de 256)
  - `idaYVuelta`: `descifrar(cifrar("texto", "C", "k1", k1), "C", {k1})` == `"texto"`; el valor empieza con `"enc:v1:k1:"`.
  - `nonceDistinto`: dos `cifrar` del mismo texto dan valores distintos y los dos descifran.
  - `pemConCrLf`: un texto de 4 KB con `"\r\n"` y `"-----BEGIN PRIVATE KEY-----"` vuelve idéntico.
  - `valorModificadoFalla`: cambiar un carácter del base64 → `ErrorCifrado`.
  - `claveIncorrectaFalla`: mismo id `"k1"` con otra clave → `ErrorCifrado`.
  - `contextoIncorrectoFalla`: cifrado con `"Empresa.passRecepcion"`, descifrado con `"Credential.secret"` → `ErrorCifrado`.
  - `idDesconocidoFalla`: descifrar un valor `k2` con solo `{k1}` → `ErrorCifrado` cuyo mensaje contiene `"k2"`.
  - `formatoInvalidoFalla`: `"enc:v1:k1"` y `"enc:v9:k1:AAAA"` → `ErrorCifrado`.
  - `estaCifrado`: `true` para `"enc:v1:k1:x"`, `false` para `null`, `""` y un PEM.
  - `concurrente`: 8 hilos × 200 idas y vueltas con la misma clave, todas correctas.
- [ ] **Step 2: Run to verify they fail:** `sbt "testOnly com.bluedot.commons.security.CifradoCamposTest"` → no compila (no existe la clase).
- [ ] **Step 3: Implement `CifradoCampos`:** `Cipher` nuevo en cada llamada (no es thread-safe), `GCMParameterSpec(128, nonce)`,
  `updateAAD(contexto UTF-8)`, `SecureRandom` estático (es thread-safe). `ErrorCifrado` envuelve
  `GeneralSecurityException`/`IllegalArgumentException` sin incluir el valor.
- [ ] **Step 4: Run to verify they pass:** mismo comando → todos PASS.
- [ ] **Step 5: Commit:** `git commit -m "CifradoCampos: AES-256-GCM para columnas con secretos"`

### Task 2: `ClaveMaestra` (claves y modo) y control al arrancar

**Files:**
- Create: `app/com/bluedot/commons/security/ClaveMaestra.java`, `app/com/bluedot/efactura/global/ArranqueCifrado.java`
- Modify: `app/com/bluedot/efactura/global/Module.java` (bind), `conf/application.conf` (bloque `seguridad.cifrado`)
- Test: `test/com/bluedot/commons/security/ClaveMaestraTest.java`

**Interfaces:**
- Consumes: `CifradoCampos` (Task 1).
- Produces:
  - `public enum ClaveMaestra.Modo { OFF, CIFRAR, EXIGIR }`
  - `static ClaveMaestra desde(String modo, java.util.Map<String, String> rutasPorId, String claveActual)` → lanza
    `IllegalStateException` (mensaje con el id y la ruta, nunca el contenido) si el modo no es `OFF` y falta un archivo,
    no se puede leer, no tiene 32 bytes, o `claveActual` no está entre las claves. Modo inválido o null → `OFF`.
  - `public static ClaveMaestra actual()` → la de la configuración de Play (`seguridad.cifrado.*`), cargada una vez;
    sin aplicación Play o sin la opción → modo `OFF` sin claves.
  - `public Modo modo()`, `public String idActual()`, `public javax.crypto.SecretKey claveActual()`,
    `public java.util.Map<String, javax.crypto.SecretKey> claves()`
  - `static void usarEnTests(ClaveMaestra c)` (para Task 3 y 4)
  - `ArranqueCifrado`: constructor `@Inject` que llama a `ClaveMaestra.actual()`; con modo distinto de `OFF` y error,
    la excepción corta el arranque de Play. Además loguea `ERROR` si `db.default.password` está vacía (parte 9a: el
    archivo de secretos no se incluyó).

- [ ] **Step 1: Write the failing tests** (archivos de clave temporales con `Files.createTempFile`)
  - `offSinClaves`: `desde(null, {}, null).modo()` == `OFF`; `desde("cualquiera", {}, null).modo()` == `OFF`.
  - `cifrarConClaveValida`: archivo de 32 bytes → `modo()` == `CIFRAR`, `idActual()` == `"k1"`.
  - `archivoFaltanteFalla`, `tamanoIncorrectoFalla` (31 y 33 bytes), `claveActualDesconocidaFalla` → `IllegalStateException`
    cuyo mensaje contiene la ruta o el id.
  - `variasClaves`: `k1` y `k2`, actual `k2` → `claves()` tiene las dos.
- [ ] **Step 2: Run to verify they fail:** `sbt "testOnly com.bluedot.commons.security.ClaveMaestraTest"` → no compila.
- [ ] **Step 3: Implement `ClaveMaestra`** (lee `seguridad.cifrado.claves` como objeto de config: un id por clave) y
  `ArranqueCifrado`; `bind(ArranqueCifrado.class).asEagerSingleton()` en `Module` (el módulo está activo en los
  `deploy_conf`; en desarrollo `actual()` se carga al primer uso).
- [ ] **Step 4: Add the config block to `conf/application.conf`:** `seguridad.cifrado.modo="off"`, comentarios con los
  tres modos, el formato de `seguridad.cifrado.claves.k1 = "/srv/java_efactura_uy/secrets/k1.key"`, `claveActual`,
  cómo generar la clave (`openssl rand 32 > k1.key; chmod 400`) y que va en el gestor de contraseñas. Mismo bloque, con
  `modo="off"`, en los dos `deploy_conf` locales (no se versionan; avisar en el PR).
- [ ] **Step 5: Run to verify they pass:** mismo comando → PASS; `sbt test` sin fallas nuevas.
- [ ] **Step 6: Commit:** `git commit -m "ClaveMaestra: claves y modo de cifrado desde la configuracion, control al arrancar"`

### Task 3: `CampoCifrado` en las tres columnas

**Files:**
- Create: `app/com/bluedot/commons/security/CampoCifrado.java`
- Modify: `app/com/bluedot/efactura/model/FirmaDigital.java` (`privateKey`), `app/com/bluedot/efactura/model/Empresa.java`
  (`passRecepcion`), `app/com/bluedot/commons/security/Credential.java` (`secret`), `conf/META-INF/persistence.xml`
  (`<class>` de los converters)
- Test: `test/com/bluedot/commons/security/CampoCifradoTest.java`

**Interfaces:**
- Consumes: `CifradoCampos` (Task 1), `ClaveMaestra.actual()` / `usarEnTests` (Task 2).
- Produces:
  - `public abstract class CampoCifrado implements AttributeConverter<String, String>` con `protected abstract String contexto()`
    y tres subclases `@Converter` anidadas: `CampoCifrado.PrivateKey` (`"FirmaDigital.privateKey"`),
    `CampoCifrado.PassRecepcion` (`"Empresa.passRecepcion"`), `CampoCifrado.SecretHmac` (`"Credential.secret"`).
  - `convertToDatabaseColumn`: `null`/`""` igual; en `OFF` igual; en `CIFRAR`/`EXIGIR` cifra con la clave actual.
  - `convertToEntityAttribute`: `null`/`""` igual; `enc:` → descifra (en `OFF` también si hay claves; sin claves
    `ErrorCifrado` "no hay clave para descifrar"); en claro → igual en `OFF`/`CIFRAR`, `ErrorCifrado` en `EXIGIR`.
  - Los mensajes de `ErrorCifrado` nombran la columna (el contexto). Un converter de JPA no sabe qué fila convierte: el
    id de la fila lo informa la migración (Task 4), no la lectura normal.
  - Las entidades usan `@Convert(converter = CampoCifrado.PrivateKey.class)`, etc.; getters y setters sin cambios.

- [ ] **Step 1: Write the failing tests** (con `ClaveMaestra.usarEnTests`)
  - `vaciosQuedanIgual`: `null` y `""` en las dos direcciones y los tres modos.
  - `offNoCifra`: en `OFF`, `convertToDatabaseColumn("x")` == `"x"`.
  - `cifrarYLeer`: en `CIFRAR`, lo escrito empieza con `"enc:v1:k1:"` y `convertToEntityAttribute` lo devuelve.
  - `cifrarLeeEnClaro`: en `CIFRAR`, `convertToEntityAttribute("x")` == `"x"`.
  - `exigirRechazaEnClaro`: en `EXIGIR`, `convertToEntityAttribute("x")` → `ErrorCifrado`.
  - `contextosDistintos`: lo cifrado por `PassRecepcion` no lo lee `SecretHmac`.
- [ ] **Step 2: Run to verify they fail:** `sbt "testOnly com.bluedot.commons.security.CampoCifradoTest"` → no compila.
- [ ] **Step 3: Implement `CampoCifrado`** y anotar las tres columnas; registrar las tres subclases en `persistence.xml`.
- [ ] **Step 4: Run to verify they pass:** mismo comando → PASS; `sbt test` sin fallas nuevas.
- [ ] **Step 5: Commit:** `git commit -m "CampoCifrado: privateKey, passRecepcion y secret cifrados al guardar"`

### Task 4: migrar y revertir (solo ADMIN)

**Files:**
- Create: `app/com/bluedot/efactura/commons/MigracionCifrado.java` (toca tablas de efactura y `EjecucionProceso`: no va en
  `commons.security`), `app/com/bluedot/efactura/controllers/CifradoController.java`
- Modify: `conf/routes`
- Test: `test/com/bluedot/efactura/commons/MigracionCifradoTest.java`

**Interfaces:**
- Consumes: `CifradoCampos`, `ClaveMaestra`, contextos de Task 3.
- Produces:
  - `static String nuevoValor(String crudo, String contexto, ClaveMaestra c, boolean revertir)` (puro): `null` si no
    hay que cambiar nada; si no, el valor a escribir. Migrar: en claro → cifrado; cifrado con un id distinto del actual →
    recifrado; cifrado con el actual → `null`. Revertir: cifrado → en claro; en claro → `null`.
  - `static void validarModo(ClaveMaestra c, boolean revertir)`: `OFF` → error para las dos; `EXIGIR` → error para revertir.
  - `public static org.json.JSONObject ejecutar(boolean revertir)`: para cada tabla (`firma_digital.private_key`,
    `empresa.pass_recepcion`, `credential.secret`), en su propia transacción
    (`DatabaseExecutor.syncDatabaseAction(..., true)`): `select id, <columna>` nativo, `update ... where id = ?` nativo
    por fila que cambia. Devuelve `{ "<tabla.columna>": { "revisados": n, "cambiados": m } }` o, para la tabla que
    falló, `{ "error": "<fila id>: <motivo sin valor>" }` (esa tabla se revierte; las demás siguen).
    Registra `EjecucionProceso.registrar("cifrado-migrar" | "cifrado-revertir", ...)` y una línea en el log `auditoria`.
  - Rutas: `POST /api/v1/procesos/cifrado/migrar` y `POST /api/v1/procesos/cifrado/revertir` →
    `CifradoController.migrar()` / `revertir()` (`@ErrorMessage`, `@Security.Authenticated(Secured.class)`,
    `@EmpresaAccess`, **sin** `@Tx`: la transacción es por tabla). `/procesos/...` ya exige ADMIN en cualquier modo
    (`EmpresaAccessAction`).

- [ ] **Step 1: Write the failing tests** (sobre `nuevoValor` y `validarModo`, con claves de prueba)
  - `migrarCifraEnClaro`, `migrarNoTocaLoActual` (`null`), `migrarRecifraOtraClave` (valor `k1` con actual `k2` → empieza
    con `"enc:v1:k2:"` y descifra al original), `revertirDescifra`, `revertirNoTocaEnClaro` (`null`).
  - `migrarRechazaOff`, `revertirRechazaOff`, `revertirRechazaExigir` → excepción; `migrarAceptaExigir` sin excepción.
- [ ] **Step 2: Run to verify they fail:** `sbt "testOnly com.bluedot.efactura.commons.MigracionCifradoTest"` → no compila.
- [ ] **Step 3: Implement `MigracionCifrado`, `CifradoController` y las rutas.**
- [ ] **Step 4: Run to verify they pass:** mismo comando → PASS; `sbt test` sin fallas nuevas; `scripts/build.sh` compila
  las rutas (scaladoc las regenera).
- [ ] **Step 5: Commit:** `git commit -m "Migrar y revertir el cifrado de secretos (solo ADMIN)"`

### Task 5: secretos fuera del deploy (9a) y documentación

**Files:**
- Modify: `README.md` (sección de configuración y deploy), `conf/application.conf` (comentario del `include`)

**Interfaces:**
- Consumes: el aviso de `ArranqueCifrado` (Task 2).
- Produces: el procedimiento que usa la Task 6.

- [ ] **Step 1: Document in `README.md`:** archivo `/srv/java_efactura_uy/secrets/<ambiente>.conf` (`chmod 400`, dueño el
  usuario de la aplicación, fuera de las carpetas por versión) con `db.default.password`, `play.crypto.secret`,
  `mail.sistema.password` y `seguridad.cifrado.*`; línea `include file("/srv/java_efactura_uy/secrets/<ambiente>.conf")`
  al final de `deploy_conf/<ambiente>/application.conf`; respaldo en el gestor de contraseñas; modos y `migrar`/`revertir`
  con el `curl` de ejemplo (`AUTH-TOKEN` de un ADMIN); orden de puesta en marcha del spec.
- [ ] **Step 2: Verify:** el README no tiene hosts, IPs, RUTs ni nombres de clientes.
- [ ] **Step 3: Commit:** `git commit -m "README: secretos en el servidor y cifrado de columnas"`
- [ ] **Step 4: PR** a `main` con el resumen del spec y la lista de cambios en los `deploy_conf` locales.

### Task 6: puesta en marcha en test

Sin código. Requiere el PR mergeado o la rama desplegada en test.

- [ ] **Step 1 (9a):** dar al usuario los comandos para crear `/srv/java_efactura_uy/secrets/test.conf` en el servidor de
  test y sacar los secretos de `deploy_conf/test`; desplegar test; verificar `ping` 200 y sin el `ERROR` de contraseña
  vacía.
- [ ] **Step 2:** desplegar con `modo = off`; verificar que todo sigue igual (login, `GET /empresa`).
- [ ] **Step 3:** dar al usuario el comando para generar `k1` en el servidor de test; `modo = cifrar`, reiniciar; verificar
  que arranca.
- [ ] **Step 4:** `POST /api/v1/procesos/cifrado/migrar` con el ADMIN de prueba; en la base de test, `count(*)` de las tres
  columnas no vacías y sin `enc:` == 0.
- [ ] **Step 5:** emitir una e-Boleta de prueba (firma y respuesta de DGI), una llamada HMAC, un ciclo del poller de correo
  (o una conexión IMAP manual).
- [ ] **Step 6:** `revertir` y `migrar` de nuevo: mismas cantidades; la segunda `migrar` seguida cambia 0.
- [ ] **Step 7:** dejar a producción el mismo procedimiento (backup de la base y `k1` en el gestor de contraseñas antes de
  `migrar`); el despliegue en producción lo hace el usuario.
