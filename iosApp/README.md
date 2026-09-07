# CargaCerca iOS

Primera aplicación iOS de CargaCerca conectada al módulo Kotlin Multiplatform `shared`.

## Abrir en Xcode

1. Abre `iosApp/CargaCercaIOS.xcodeproj`.
2. Selecciona un simulador iPhone o un iPhone físico.
3. Compila con **Run**.

El proyecto usa integración directa de Kotlin Multiplatform. Antes de compilar Swift, Xcode ejecuta:

```bash
gradle :shared:embedAndSignAppleFrameworkForXcode
```

Si más adelante añadimos el Gradle Wrapper, el script lo usará automáticamente.

## Estado actual

- SwiftUI host funcional preparado para iPhone.
- Framework `CargaCercaShared` para dispositivo y simulador Apple Silicon.
- La primera pantalla lee modelos/datos desde Kotlin compartido.
- Android sigue usando el mismo módulo `shared`.
- GPS, mapa y UI completa se migrarán de forma incremental.
