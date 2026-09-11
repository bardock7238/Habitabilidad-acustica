# Habitabilidad Acústica

Proyecto de análisis de habitabilidad acústica (ruido) en edificaciones.
Simula la propagación del sonido a través de un edificio y determina si cada
espacio es habitable según la normativa de referencia.

## Estructura del proyecto

```
Habitabilidad-acustica/
├── datos/                          # Archivos de datos (CSV/JSON)
│   ├── normativa_oms.csv           #   Límites guía OMS por recinto y horario
│   └── sectores.csv                #   Sectores urbanos Res. 0627/2006
│
└── src/                            # Código fuente Java
    └── acustica/                   # Paquete raíz
        ├── dominio/                # Clases del mundo real (diagrama UML)
        │   ├── Material.java       #   Atenuación por material
        │   ├── FuenteSonido.java   #   Fuente de ruido (fija o móvil)
        │   ├── Superficie.java     #   Pared, losa, ventana, puerta
        │   ├── Habitacion.java     #   Recinto a evaluar
        │   └── Edificio.java       #   Compone las habitaciones
        │
        ├── normativa/              # Reglas y resultados
        │   ├── Normativa.java        #   Map de límites por recinto/horario
        │   └── ResultadoHabitabilidad.java
        │
        ├── simulacion/             # Motor de la simulación
        │   ├── Simulador.java        #   Ejecuta la evaluación y el reporte
        │   └── propagacion/          #   Algoritmos de propagación
        │       ├── Grafo.java        #   Grafo de adyacencia del edificio
        │       ├── Dijkstra.java     #   Camino de menor atenuación
        │       └── RayTracing.java   #   Propagar sonido por rayos
        │
        ├── config/                 # Carga de datos de entrada
        │   └── CargadorDatos.java    #   Lee los CSV de datos/
        └── Main.java               # Punto de entrada del programa
```

## Cómo compilar

```bash
javac -d out src/acustica/**/*.java
java -cp out acustica.Main
```

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