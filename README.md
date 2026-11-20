# Habitabilidad Acústica

Proyecto de análisis de habitabilidad acústica (ruido) en edificaciones.
Simula la propagación del sonido a través de un edificio y determina si cada
espacio es habitable según la normativa de referencia (guías OMS).

## Estructura del proyecto

```
Habitabilidad-acustica/
├── datos/
│   ├── normativa_oms.csv           # Límites OMS por recinto y horario
│   └── sectores.csv                # Sectores urbanos Res. 0627/2006
├── docs/
│   ├── modelo-matematico.md        # Modelo v2: R_k, absorción Sabine, Dijkstra
│   └── casos-de-prueba-raytracing.md
└── src/acustica/
    ├── dominio/                    # Material, FuenteSonido, Superficie, Habitacion,
    │                               # Edificio y Actividad
    ├── normativa/                  # Normativa, ResultadoHabitabilidad y
    │                               # EstadoHabitabilidad
    ├── simulacion/
    │   ├── Simulador.java          # Ejecuta la evaluación y arma el reporte
    │   ├── IndiceHabitabilidad.java
    │   ├── IndiceHabitabilidadPorPisos.java
    │   ├── IndicePorPisoYNivel.java # Índice B+ (piso, nivel, habitación)
    │   └── propagacion/            # Grafo, Dijkstra, RayTracing,
    │                               # CalculoAcustico y verificaciones
    ├── config/                     # CargadorDatos (lee los CSV de datos/)
    ├── Main.java                   # Punto de entrada con un edificio de ejemplo
    └── MainEdificioComunal.java    # Edificio con parqueadero, salón comunal, etc.
    ├── estructuras/                # Árboles AVL, rojo-negro y B+,
    │                               # además de NodoVisual
    ├── mejora/                     # Soluciones de aislamiento,
    │                               # planes de mejora y reubicación
    ├── visual/                      # GraficadorArboles: genera
    │                               # la visualización HTML
```

## Modelo acústico (resumen)

- **Propagación entre recintos** con `Grafo` + `Dijkstra`:

  ```
  w_{i→j} = max(0, R_k − 10·log10(S_k / A_j))
  A_j     = 0.16 · V_j / T60
  ```

  El peso depende del recinto receptor, por eso el grafo es **dirigido**.
- **Ray tracing** en la habitación de origen: cada superficie recibe rayos
  proporcionales a su área y el nivel se **promedia** sobre los `N` rayos
  (no se suman).
- **Nivel total** = suma energética de las fuentes activas y el ruido de fondo.

El detalle completo, con las hipótesis y limitaciones, está en
[`docs/modelo-matematico.md`](docs/modelo-matematico.md).

## Índice B+ por piso y nivel

`IndicePorPisoYNivel` guarda los resultados de una simulación en un
`ArbolBMas` con clave compuesta `(piso, nivelRecibido, habitacionId)`.
Se usa `nivelRecibido` (el ruido que llega al espacio) y no `nivelFinal`
(el del vecino en una actividad de alto impacto); el id desempata dos
habitaciones del mismo piso con igual nivel.

Consultas principales:

- `habitacionesDePiso(piso)`: todas las habitaciones de un piso.
- `queSuperanNivel(piso, nivel)`: las que están estrictamente por encima.
- `enRangoDeNiveles(piso, desde, hasta)`: las de un rango inclusivo.

El reporte por consola (`ReporteHabitabilidad`) muestra estas consultas y
`GraficadorArboles` dibuja el B+ en una sección nueva de `arboles.html`.

## Cómo compilar y ejecutar

Desde la raíz del proyecto:

```bash
rm -rf out
javac -d out $(find src -name "*.java")
java -cp out acustica.Main
java -cp out acustica.MainEdificioComunal   # edificio con zonas comunes
```

En Windows (PowerShell):

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object FullName)
java -cp out acustica.Main
```

### Visualización de árboles

El proyecto incluye `GraficadorArboles`, que permite representar gráficamente las estructuras de datos utilizadas en el análisis de habitabilidad acústica.

Después de ejecutar la simulación, si se ha generado el archivo `out/arboles.html`, puede abrirse desde la raíz del proyecto con:

```bash
xdg-open out/arboles.html
```

También puede abrirse manualmente desde el explorador de archivos o desde un navegador web.

La visualización permite inspeccionar la organización de los árboles y comprender cómo se ordenan los datos utilizados por el sistema.

## Verificaciones

```bash
java -cp out acustica.simulacion.propagacion.VerificacionGrafo
java -cp out acustica.simulacion.propagacion.VerificacionRayTracing
java -cp out acustica.normativa.VerificacionNormativa
java -cp out acustica.simulacion.VerificacionActividades
java -cp out acustica.estructuras.VerificacionArboles
java -cp out acustica.simulacion.VerificacionIndicePorPisoYNivel
java -cp out acustica.normativa.VerificacionResultadoHabitabilidad
java -cp out acustica.normativa.VerificacionIndicePorPisoYNivel
```

Salida esperada:

```
VerificacionGrafo:      21 / 21 verificaciones OK
VerificacionRayTracing: 21 / 21 verificaciones OK
VerificacionNormativa:  17 / 17 verificaciones OK
VerificacionActividades: 16 / 16 verificaciones OK
VerificacionArboles:    17 / 17 verificaciones OK
VerificacionIndicePorPisoYNivel: 17 / 17 verificaciones OK
```

`VerificacionArboles` termina con código de salida 1 si alguna verificación falla.

Para ver los datos crudos de los CSV:

```bash
java -cp out acustica.config.CargadorDatos
```

> `CargadorDatos` y `Normativa` leen `datos/` con rutas relativas, así que hay
> que ejecutar los comandos desde la raíz del proyecto.

## Metodología de trabajo (Git)

- `main`: código estable, siempre debe compilar.
- Cada integrante trabaja en su rama `feature/...` y la integra vía pull request.
- Nadie mergea su propio código; siempre revisa un compañero.

### Ramas

| Integrante | Rama |
|---|---|
| 1 | `feature/material`, `feature/fuente` |
| 2 | `feature/habitacion` |
| 3 | `feature/superficie` |
| 4 | `feature/edificio`, `feature/normativa` |
