# StepDroid

StepDroid is a lightweight emulator for Android that allows players to enjoy music and dance games by using StepMania 5 SCC files. This project is perfect for enthusiasts looking to bring their custom dance routines and music tracks on the go.

## Features

- **File Support**: Specifically designed to read and play StepMania 5 SCC files.
- **Portable Dance Experience**: Brings the rhythm game experience to your Android device.
- **Ongoing Development**: Actively developed to add new features and enhance user experience.

## How to Use

1. Install the APK on your Android device.
2. Load your StepMania 5 SCC files into the app.
3. Start playing and enjoy your custom dance routines!

## Development Setup

To build StepDroid you need the Android SDK installed. Set the `ANDROID_HOME` environment variable or create a `local.properties` file with the path to your SDK.

```bash
sdk.dir=/path/to/Android/sdk
```

The project uses **Jetpack Compose**. Ensure you are running JDK 17 and a recent version of Android Studio (Hedgehog or newer). Compose libraries are managed with the Compose BOM so compatible versions are resolved automatically.

Use the Gradle wrapper to build and run static analysis:

```bash
./gradlew assembleDebug
./gradlew lint
```

Recommended build commands:

```bash
# run unit tests and lint
./gradlew check

# create a release APK
./gradlew assembleRelease
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for more information about contributing.

## Contributing

Contributions to StepDroid are welcome! If you have ideas for improvements or new features, please read the `CONTRIBUTING.md` file for more information on how to contribute.

## Extending data sources

Song files can come from multiple locations. The `FileSource` interface abstracts
the origin of the data with `open()`, `read()` and `close()` methods. The default
implementation `LocalFileSource` reads from the filesystem, but additional
sources (e.g. assets, SAF or network downloads) can be added by implementing
`FileSource` and wiring it into the repository responsible for loading songs.

## License

This project is licensed under the MIT License - see the `LICENSE` file for details.

## 🎀 StepDroid Graphics Guide

### Table of Contents

- [Introducción](#introducción)
- [Arquitectura general](#arquitectura-general)
- [Renderizado con Canvas](#renderizado-con-canvas)
- [Renderizado con OpenGL ES 2.0](#renderizado-con-opengl-es-20)
- [Comparativa y mejoras](#comparativa-y-mejoras)
- [Apéndice](#apéndice)

### Introducción

StepDroid puede dibujar la interfaz de juego usando **Canvas** o **OpenGL ES&nbsp;2.0**. Ambos motores comparten la misma lógica base y sólo cambian la forma en la que se envían los gráficos a pantalla.

```
     +-----------+     +--------------+     +-----------+
     | GameState | --> | StepsDrawer* | --> | Canvas/GL |
     +-----------+     +--------------+     +-----------+
```

`StepsDrawer` es la versión para Canvas y `StepsDrawerGL` es la especializada para OpenGL.

### Arquitectura general

1. **GamePlayActivity** carga la canción y crea `GamePlayNew` (Canvas) o `GamePlayGLRenderer` (OpenGL).【F:app/src/main/java/com/kyagamy/step/views/gameplayactivity/GamePlayActivity.kt†L152-L191】【F:app/src/main/java/com/kyagamy/step/views/TestGLPlayerActivity.kt†L24-L47】
2. Ambos renderizadores gestionan un `GameState` que mantiene el progreso de la canción y las notas.  
3. `StepsDrawer`/`StepsDrawerGL` reciben la lista de notas visibles y las dibujan sobre la vista correspondiente.  
4. Elementos de interfaz como barra de vida y combo se actualizan en cada ciclo de dibujo.

### Renderizado con Canvas

`GamePlayNew` es un `SurfaceView` que implementa el ciclo `draw()` y actualiza internamente el juego. Durante el dibujo se llama a `StepsDrawer.draw()` para pintar flechas y efectos:

```kotlin
override fun draw(canvas: Canvas) {
    super.draw(canvas)
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    if (gameState?.isRunning == true) {
        calculateSpeed()
        drawList.clear()
        calculateVisibleNotes()
        renderer?.drawGame(canvas, drawList, gameState, speed)
    }
    renderer?.drawUI(canvas)
}
```
【F:app/src/main/java/com/kyagamy/step/game/newplayer/GamePlayNew.kt†L246-L273】

`StepsDrawer` procesa cada `GameRow` y dibuja sprite por sprite usando `Canvas`:

```kotlin
private fun drawNotes(canvas: Canvas, listRow: ArrayList<GameRow>) {
    for (gameRow in listRow) {
        val notes = gameRow.notes
        if (notes != null) {
            for (count in notes.indices) {
                val note = notes[count]
                if (note.type != CommonSteps.NOTE_EMPTY) {
                    drawSingleNote(canvas, note, gameRow, count)
                }
            }
        }
    }
}
```
【F:app/src/main/java/com/kyagamy/step/game/newplayer/StepsDrawer.kt†L185-L197】

Cada `SpriteReader` se encarga de actualizar y dibujar su frame actual. El refresco es controlado por la clase `MainThreadNew` que invoca `draw()` y `update()` a la frecuencia de la pantalla.

### Renderizado con OpenGL ES 2.0

El modo OpenGL usa `GamePlayGLRenderer`, que implementa `GLSurfaceView.Renderer`. En cada frame limpia la pantalla, calcula las notas visibles y delega el dibujo a `StepsDrawerGL`:

```kotlin
override fun onDrawFrame(gl: GL10?) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    if (!isGameStarted) return
    updateFPS()
    updateGame()
    drawList.clear()
    calculateVisibleNotes()
    stepsDrawer?.drawGame(drawList)
    stepsDrawer?.update()
}
```
【F:app/src/main/java/com/kyagamy/step/game/opengl/GamePlayGLRenderer.kt†L235-L247】

`StepsDrawerGL` prepara un programa de shaders y dibuja cada sprite como un cuadrado con textura. El shader principal se define en `SpriteGLRenderer`:

```glsl
attribute vec2 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
uniform mat4 uMVPMatrix;
void main(){
    vTexCoord = aTexCoord;
    gl_Position = uMVPMatrix * vec4(aPosition,0.0,1.0);
}
```
【F:app/src/main/java/com/kyagamy/step/engine/SpriteGLRenderer.kt†L147-L156】

Las texturas se cargan una vez por sprite y se enlazan antes de cada `glDrawArrays`. El cálculo de posiciones se realiza en píxeles y se convierte a coordenadas [-1,1] para OpenGL.

### Comparativa y mejoras

| Aspecto | Canvas | OpenGL ES 2.0 |
|---------|-------|---------------|
| **Facilidad** | ✅ Simple de depurar y acceder a `Canvas` | ⚠️ Requiere shaders y manejo manual de buffers |
| **Rendimiento** | Bien para dispositivos modestos pero puede saturarse con muchas animaciones | Mejor escalabilidad y uso de GPU para miles de sprites |
| **Flexibilidad** | Limitado a operaciones 2D básicas | Permite efectos avanzados (transparencias, escalado, etc.) |

**Sugerencias**

1. Utilizar el modo OpenGL para canciones con alto conteo de notas o efectos complejos.
2. Mantener los sprites en memoria para evitar cargas frecuentes.
3. Documentar los parámetros de `StepsDrawer` y `StepsDrawerGL` para facilitar su ajuste.

### Apéndice

*La ruta de código principal para cada componente:*

- **GamePlayNew** (Canvas): `app/src/main/java/com/kyagamy/step/game/newplayer/GamePlayNew.kt`
- **StepsDrawer**: `app/src/main/java/com/kyagamy/step/game/newplayer/StepsDrawer.kt`
- **GamePlayGLRenderer** (OpenGL): `app/src/main/java/com/kyagamy/step/game/opengl/GamePlayGLRenderer.kt`
- **StepsDrawerGL**: `app/src/main/java/com/kyagamy/step/engine/StepsDrawerGL.kt`
- **SpriteGLRenderer** (shaders y texturas): `app/src/main/java/com/kyagamy/step/engine/SpriteGLRenderer.kt`

¡Disfruta explorando el código y creando nuevas coreografías! 💃
