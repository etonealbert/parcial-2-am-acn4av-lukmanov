# Firebase en AlberTUP

Firebase permite crear una cuenta con correo y contraseña y guardar una copia de las series en la
nube. Es opcional: la app funciona de forma local si no se configura.

## Configuración

1. Crear un proyecto en [Firebase Console](https://console.firebase.google.com/).
2. Agregar una app Android con el nombre de paquete `com.example.albertup`.
3. Descargar `google-services.json` y colocarlo en `albertup/app/`. El archivo está en
   `.gitignore` y no se sube al repositorio.
4. En **Authentication → Sign-in method**, habilitar **Email/Password**.
5. En **Firestore Database**, crear la base en modo producción. La configuración usada para este
   trabajo eligió `southamerica-east1` (São Paulo). La ubicación no se puede cambiar después.
6. En **Firestore → Rules**, copiar el contenido de [firestore.rules](../firebase/firestore.rules)
   y publicarlo.
7. Abrir `albertup/` en Android Studio, sincronizar Gradle y ejecutar la app. En **Ajustes →
   Gestionar cuenta** debe aparecer el formulario de inicio de sesión.

No hace falta configurar SHA-1 para el inicio de sesión con correo y contraseña. El plugin de
Google Services se activa cuando existe `google-services.json`.

## Cómo funciona el respaldo

Después de iniciar sesión, el botón **Respaldar mis series ahora** copia las series locales a
`users/{uid}/sets` en Firestore. Cada serie usa un identificador propio, por lo que repetir el
respaldo actualiza los mismos documentos. Las reglas permiten que cada usuario acceda solo a sus
datos y validan los datos principales de las series.

El respaldo es manual y en un solo sentido. Todavía no hay restauración desde la nube, sincronización
automática ni propagación de eliminaciones.

## Pruebas

El 17 de noviembre de 2026 se creó una cuenta temporal mediante el SDK y se verificó un respaldo
real desde un emulador. Al repetirlo, una lectura del servidor confirmó que había una sola serie y
que el resumen era correcto. El [video de respaldo](videos/firebase-backup.mp4) muestra el resultado
en la app con una sesión ya iniciada.

Queda pendiente probar los casos de error y el comportamiento sin conexión. El archivo de
configuración local no se incluye en el repositorio; para usar Firebase en otra instalación hay que
agregar uno correspondiente a su proyecto.
