# CargaCerca iOS

Primera aplicación iOS de CargaCerca conectada al módulo Kotlin Multiplatform `shared`.

## Abrir en Xcode

1. Abre `iosApp/CargaCercaIOS.xcodeproj`.
2. Selecciona un simulador iPhone o un iPhone físico.
3. Compila con **Run**.

El proyecto usa integración directa de Kotlin Multiplatform. Antes de compilar Swift, Xcode ejecuta el framework compartido mediante `:shared:embedAndSignAppleFrameworkForXcode`.

El script usa `./gradlew` cuando añadamos el Gradle Wrapper y, mientras tanto, utiliza `gradle` instalado en el Mac.

## Estado actual

- SwiftUI host preparado para iPhone.
- Framework `CargaCercaShared` para dispositivo y simulador Apple Silicon.
- La primera pantalla lee modelos/datos desde Kotlin compartido.
- Android sigue usando el mismo módulo `shared`.
- CI iOS valida el framework y el proyecto Xcode en un runner Apple Silicon.
- GPS, mapa y UI completa se migrarán de forma incremental.
