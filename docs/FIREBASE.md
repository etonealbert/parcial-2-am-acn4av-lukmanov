# Firebase en AlberTUP: estado actual y pasos pendientes

> **Estado:** integración inicial configurada y probada en un proyecto de Firebase y un emulador
> Android el 17 de noviembre de 2026. La app también funciona sin Firebase. La configuración local
> `google-services.json` no se incluye en Git y cada instalación debe proporcionar la suya.

Se verificó la creación de una cuenta temporal mediante el SDK y un respaldo real a Firestore.
El respaldo se repitió y una lectura desde el servidor confirmó una sola serie y el resumen correcto.
También se comprobó en la pantalla de la app el mensaje de respaldo completado. Esto no cubre todos
los casos de error, el modo sin conexión ni la restauración, que aún no está implementada.

## 1. Qué ya está en el código

| Pieza | Dónde | Detalle |
|---|---|---|
| Dependencias | `albertup/gradle/libs.versions.toml`, `albertup/app/build.gradle.kts` | Firebase BoM **35.2.0** (gestiona `firebase-auth` 25.0.0 y `firebase-firestore` 27.2.0, minSdk 24). |
| Plugin Google Services | `albertup/build.gradle.kts` (declarado con `apply false`) y `albertup/app/build.gradle.kts` | Versión **4.6.0**. Se aplica **solo si existe** `albertup/app/google-services.json`, así el proyecto compila sin el archivo. |
| Detección segura | `cloud/CloudBackup.isConfigured()` | Verifica que exista la app `[DEFAULT]` antes de llamar a `FirebaseAuth.getInstance()` o `FirebaseFirestore.getInstance()` (que lanzarían `IllegalStateException`). |
| Autenticación | `AccountActivity` (Ajustes → *Cuenta y respaldo en la nube*) | Crear cuenta, iniciar y cerrar sesión con email y contraseña; validación de formulario y mensajes claros por tipo de error (credenciales, email en uso, contraseña débil, sin conexión, demasiados intentos). |
| Respaldo a Firestore | `cloud/CloudBackup.backUp()` | Subida **manual y en un solo sentido** de todas las series locales, en lotes de hasta 450 escrituras (límite documentado: 500). El id de cada documento es el UUID de la serie, por lo que repetir el respaldo no duplica datos. |
| Reglas de seguridad | `firebase/firestore.rules` | Cada usuario solo accede a `users/{su uid}/…`; valida `exerciseId`, `reps` (1–200) y tipos de fecha. |
| Sin configuración | `AccountActivity` | Muestra una tarjeta que explica que falta `google-services.json` y remite a este documento; el resto de la app no cambia. |

### Modelo de datos en Firestore

```text
users/{uid}
  lastBackupAt : timestamp (del servidor)
  setCount     : number
users/{uid}/sets/{uuidDeLaSerie}
  exerciseId   : "push_up" | "pull_up"   (id estable, nunca el texto traducido)
  reps         : number (1–200)
  performedAt  : timestamp (fecha y hora real de la serie)
  updatedAt    : timestamp (última edición local)
  backedUpAt   : timestamp (del servidor)
```

## 2. Pasos para activarlo (en la otra máquina)

1. Entrar a <https://console.firebase.google.com/> y crear un proyecto (Google Analytics es opcional).
2. **Agregar app → Android** con el *package name* exacto `com.example.albertup`. El SHA-1 **no** es
   necesario para email y contraseña (sí lo sería para Google Sign-In o teléfono).
3. Descargar `google-services.json` y copiarlo en **`albertup/app/google-services.json`**.
   El archivo está en `.gitignore`: no lo subas al repositorio.
4. **Authentication → Sign-in method → Email/Password → Habilitar → Guardar.**
5. **Firestore Database → Crear base de datos** en **modo producción**. Elegí una ubicación cercana,
   por ejemplo `southamerica-east1` (San Pablo); **no se puede cambiar después**.
6. **Firestore → Reglas**: pegar el contenido de `firebase/firestore.rules` y **Publicar**.
7. En Android Studio: *Sync Project with Gradle Files* y ejecutar la app (Gradle necesita JDK 17;
   el que trae Android Studio sirve).
8. Comprobar en **Ajustes → Cuenta y respaldo en la nube** que el estado diga *"Sin sesión
   iniciada"* (si dice *"Firebase no está configurado"*, el plugin no encontró el JSON).

### Pruebas manuales sugeridas

- [ ] Crear cuenta con un email nuevo → mensaje "Cuenta creada".
- [ ] Cerrar sesión e iniciar sesión otra vez; probar una contraseña incorrecta.
- [ ] Intentar crear una cuenta con un email ya usado y con una contraseña de menos de 6 caracteres.
- [ ] Registrar algunas series, tocar **Respaldar mis series ahora** y verificar en la consola los
      documentos en `users/{uid}/sets`.
- [ ] Repetir el respaldo: la cantidad de documentos no debe duplicarse.
- [ ] Modo avión + respaldo: a los 20 s la app avisa que quedó pendiente; al reconectar se envía.
- [ ] Con las reglas sin publicar (o erróneas), el respaldo muestra el aviso de reglas de seguridad.

## 3. Lo que falta integrar

1. **Ampliar la verificación de punta a punta** a los casos de error, reglas y modo sin conexión.
2. **Restaurar desde la nube**: leer `users/{uid}/sets`, convertir `exerciseId` con
   `Exercise.fromId()` y fusionar por UUID en `WorkoutRepository` (gana el `updatedAt` más reciente).
3. **Propagar eliminaciones**: al borrar localmente, borrar el documento o guardar una marca
   `deletedAt` para que otros dispositivos también lo eliminen.
4. **Sincronización automática** (al guardar/editar/borrar o periódica con WorkManager) y
   resolución de conflictos entre dispositivos.
5. **Cuenta completa**: recuperación de contraseña (`sendPasswordResetEmail`), verificación de email y
   **eliminación de cuenta y datos** (exigida por Google Play si la app permite crear cuentas).
6. **Google Sign-In** con Credential Manager (requiere registrar SHA-1/SHA-256).
7. **App Check**, monitoreo de cuotas y pruebas instrumentadas con **Firebase Emulator Suite**.
8. Textos de privacidad/consentimiento acordes a los datos que se suben.

## 4. Notas de la investigación

- Sin `google-services.json`, `FirebaseInitProvider` **no rompe la app**: registra que la
  inicialización no se hizo y `FirebaseApp.getApps()` queda vacío. Por eso se comprueba la app
  `[DEFAULT]` antes de usar las instancias sin argumentos.
- Firestore guarda las escrituras en una cola local cuando no hay conexión; la tarea de `commit()`
  termina recién cuando responde el servidor. La pantalla corta la espera a los 20 s y lo informa.
- La documentación del SDK de Android sigue indicando **500 escrituras por lote**; se usan 450.
- En API 24–25 no hay `java.time` sin *desugaring*: se usan `com.google.firebase.Timestamp`/`Date`.
- La compatibilidad del plugin 4.6.0 se probó oficialmente con AGP 8.13.0; con 8.13.2 es inferida.

### Fuentes

- Configuración en Android: <https://firebase.google.com/docs/android/setup>
- Plugin y archivo `google-services.json`: <https://firebase.google.com/docs/android/google-services-plugin-and-file>
- Autenticación con contraseña: <https://firebase.google.com/docs/auth/android/password-auth>
- Inicio rápido de Firestore: <https://firebase.google.com/docs/firestore/quickstart>
- Ubicaciones de Firestore: <https://firebase.google.com/docs/firestore/locations>
- Reglas de seguridad: <https://firebase.google.com/docs/firestore/security/rules-structure>
- BoM de Firebase: <https://firebase.google.com/docs/android/learn-more#bom>
- Notas de versión de Android: <https://firebase.google.com/support/release-notes/android>
- `WriteBatch` / límites: <https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/WriteBatch>
