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
    │   └── propagacion/            # Grafo, Dijkstra, RayTracing,
    │                               # CalculoAcustico y verificaciones
    ├── config/                     # CargadorDatos (lee los CSV de datos/)
    └── Main.java                   # Punto de entrada con un edificio de ejemplo
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

## Cómo compilar y ejecutar

Desde la raíz del proyecto:

```bash
rm -rf out
javac -d out $(find src -name "*.java")
java -cp out acustica.Main
```

En Windows (PowerShell):

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object FullName)
java -cp out acustica.Main
```

## Verificaciones

```bash
java -cp out acustica.simulacion.propagacion.VerificacionGrafo
java -cp out acustica.simulacion.propagacion.VerificacionRayTracing
java -cp out acustica.normativa.VerificacionNormativa
```

Salida esperada:

```
VerificacionGrafo:      16 / 16 verificaciones OK
VerificacionRayTracing: 21 / 21 verificaciones OK
VerificacionNormativa:  15 / 15 verificaciones OK
```

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
