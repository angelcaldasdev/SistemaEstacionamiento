# 🅿️ SistemaEstacionamiento

**Sistema de gestión y control de estacionamiento vehicular desarrollado en Java**

`SistemaEstacionamiento` es una aplicación de escritorio desarrollada en Java con interfaz gráfica en **Swing** y persistencia relacional en **MySQL**, diseñada para administrar el flujo operativo de una playa de estacionamiento.

El sistema optimiza el control de ingresos, la asignación automática de espacios disponibles, el cobro por tiempo de permanencia según el tipo de vehículo y el cierre de caja diario, evitando registros duplicados y errores de cálculo manual.

---

## 🎯 Caso de negocio

Un estacionamiento vehicular necesita una herramienta confiable para controlar los ingresos y salidas de vehículos en tiempo real.

Actualmente, la gestión manual suele presentar problemas como:
- Desconocimiento inmediato de cuántos espacios libres quedan disponibles.
- Errores al calcular las horas o fracciones de permanencia y el monto a cobrar.
- Registro duplicado de placas que aún permanecen dentro del local.
- Falta de trazabilidad histórica sobre los tickets cobrados y el total recaudado en el día.

**Solución propuesta:** Desarrollar una aplicación de escritorio que centralice la administración de espacios, tickets activos e historial de cobros, aplicando validaciones de negocio y almacenamiento persistente en MySQL.

---

## ✨ Funcionalidades principales

1. **Registrar ingreso vehicular:** Asigna automáticamente el primer espacio libre disponible, normaliza la placa en mayúsculas, genera un código correlativo (`TCK-0001`) y registra la hora exacta de entrada.
2. **Control visual de espacios:** Muestra en tiempo real el estado (`LIBRE` u `OCUPADO`), placa, tipo de vehículo, tarifa por hora y hora de ingreso de cada plaza.
3. **Registrar salida y cobro:** Calcula las horas facturables (redondeando hacia arriba por hora o fracción, con mínimo de 1 hora), determina el monto total, emite un comprobante resumen y libera el espacio.
4. **Historial de tickets cobrados:** Lista todos los servicios finalizados con fecha/hora de entrada, salida, horas cobradas y monto pagado.
5. **Indicadores en tiempo real:** Muestra en la cabecera los espacios libres sobre la capacidad total y el acumulado recaudado en soles (`S/`).
6. **Persistencia en MySQL:** Al iniciar la aplicación, recupera automáticamente los vehículos activos y el historial de cobros desde la base de datos.

---

## 📋 Reglas de negocio

El sistema aplica las siguientes reglas para garantizar la integridad de las operaciones:

- **Validación de placa:** No se permite registrar ingresos con placas vacías; toda placa se normaliza automáticamente sin espacios extremos y en mayúsculas.
- **Unicidad de vehículo activo:** No se puede registrar el ingreso de una placa que ya tiene un ticket `ACTIVO` dentro del estacionamiento.
- **Control de aforo:** Si todos los espacios están ocupados, el sistema bloquea nuevos ingresos e informa que no hay disponibilidad.
- **Tarifas diferenciadas por tipo de vehículo:**
  - `AUTO`: **S/ 4.00** por hora
  - `MOTO`: **S/ 2.00** por hora
  - `CAMIONETA`: **S/ 6.00** por hora
- **Cálculo de tiempo facturable:** Se cobra por hora o fracción (`(minutos + 59) / 60`), garantizando un cobro mínimo de **1 hora** incluso si la salida ocurre en los primeros minutos.
- **Validación de salida:** Solo se puede cobrar y liberar un espacio si existe un ticket `ACTIVO` asociado a la placa ingresada.

---

## 🧩 Historias de usuario y criterios de aceptación

### HU-01: Registrar ingreso de vehículo
**Como** operador de cochera, **quiero** ingresar la placa y seleccionar el tipo de vehículo, **para** asignarle un espacio libre y generar su ticket de entrada.
- **CA-01 (correcto):** Dado que hay espacios libres y la placa no está activa, cuando presiono **"Ingresar"** (o `Enter`), entonces el sistema guarda el registro en MySQL, asigna el primer espacio libre (`TCK-0001`) y actualiza el contador de disponibilidad.
- **CA-02 (error):** Dado que el campo placa está vacío, cuando intento registrar el ingreso, entonces el sistema rechaza la operación y muestra *"La placa no puede estar vacía."*
- **CA-03 (error):** Dado que la placa ya se encuentra ocupando un espacio, cuando intento registrarla nuevamente, entonces el sistema muestra *"Ya existe un vehículo activo con esa placa."*
- **CA-04 (error):** Dado que los 15 espacios están ocupados, cuando intento ingresar otro vehículo, entonces el sistema muestra *"No hay espacios disponibles."*

### HU-02: Registrar salida y cobro
**Como** operador de cochera, **quiero** ingresar o seleccionar la placa de un vehículo estacionado, **para** calcular su tarifa, cobrar el servicio y liberar el espacio.
- **CA-05 (correcto):** Dado que existe un ticket activo para la placa, cuando presiono **"Cobrar y Liberar"**, entonces el sistema calcula las horas facturables, actualiza el ticket a `COBRADO` en MySQL, muestra el comprobante de pago, libera el espacio y suma el monto al total recaudado.
- **CA-06 (error):** Dado que la placa ingresada no corresponde a ningún vehículo estacionado, cuando intento procesar la salida, entonces el sistema muestra *"No se encontró un ticket activo para la placa ingresada."*

---

## 🛠️ Tecnologías utilizadas

| Tecnología | Aplicación |
| :--- | :--- |
| **Java (JDK 26)** | Lenguaje principal para el modelo de dominio, servicios y lógica de negocio. |
| **Java Swing** | Desarrollo de la interfaz gráfica de escritorio (`JFrame`, `JTable`, `JSplitPane`). |
| **MySQL 8** | Motor de base de datos relacional para el almacenamiento persistente. |
| **JDBC (`mysql-connector-j`)** | Conectividad y ejecución de consultas SQL parametrizadas (`PreparedStatement`). |
| **Apache NetBeans IDE 32** | Entorno de desarrollo integrado. |
| **Git y GitHub** | Control de versiones y alojamiento del repositorio. |

---

## 🖥️ Interfaz gráfica

La aplicación presenta una interfaz limpia y funcional construida en `ui.FrmEstacionamiento`:

- **Barra superior de métricas:** Muestra el título del sistema, los espacios libres (`X / 15`) en verde y el total recaudado (`S/ 0.00`) en amarillo.
- **Paneles de operación rápida:** Formularios lado a lado para **Registrar Ingreso** y **Registrar Salida / Cobro**, con soporte para tecla `Enter` y botones estilizados de alto contraste.
- **Autocompletado por selección:** Al hacer clic sobre una fila ocupada en la tabla de espacios, la placa se carga automáticamente en el formulario de salida.
- **Vista dividida (`JSplitPane`):** Permite redimensionar dinámicamente la tabla superior (**Estado de Espacios**) y la tabla inferior (**Historial de Tickets Cobrados**).

---

## 🏗️ Arquitectura del proyecto

El proyecto sigue una arquitectura en capas para separar responsabilidades y facilitar su mantenimiento:

- `model`: Define las entidades principales (`Vehiculo`, `Ticket`, `EspacioEstacionamiento`) y el enumerado `TipoVehiculo` con sus tarifas.
- `service`: Implementa `EstacionamientoService`, encargado de validar reglas de negocio, asignar espacios, calcular cobros y sincronizar operaciones con MySQL.
- `config`: Administra la conexión JDBC mediante `ConexionMySQL`.
- `ui`: Gestiona la interfaz gráfica Swing (`FrmEstacionamiento`) y la interacción con el operador.
- `principal`: Contiene la clase de arranque `Main` que configura el *Look and Feel* e inicia la aplicación.

---

## 🗄️ Base de datos MySQL

El sistema utiliza la base de datos `estacionamiento_db` y la tabla `tickets` en **MySQL 8**.

Pasos de configuración:
1. Crear la base de datos `estacionamiento_db`, el usuario `estacionamiento_app` con privilegios y la tabla `tickets`.
2. Verificar que el servicio `MySQL80` esté activo y coincidan las credenciales en `ConexionMySQL.java`.
3. Agregar el conector `mysql-connector-j` en `Libraries` dentro de NetBeans.

---

## 📂 Estructura del proyecto

```text
SistemaEstacionamiento/
├── src/
│   ├── config/
│   │   └── ConexionMySQL.java
│   ├── model/
│   │   ├── EspacioEstacionamiento.java
│   │   ├── Ticket.java
│   │   ├── TipoVehiculo.java
│   │   └── Vehiculo.java
│   ├── service/
│   │   └── EstacionamientoService.java
│   ├── ui/
│   │   └── FrmEstacionamiento.java
│   └── principal/
│       └── Main.java
├── lib/
│   └── mysql-connector-j-9.4.0.jar
└── README.md
```
---

## 🚀 Cómo ejecutar el proyecto

1. Clonar el repositorio: `git clone https://github.com/angelcaldasdev/SistemaEstacionamiento.git`
2. Asegurar que `MySQL80` esté iniciado y que exista `estacionamiento_db` con la tabla `tickets`.
3. Abrir el proyecto en **Apache NetBeans IDE 32** (`File > Open Project`), verificar el `.jar` en `Libraries` y las credenciales en `ConexionMySQL.java`.
4. Ejecutar `principal/Main.java` con `Run File` (`Shift + F6`).

---

## 🗺️ Próximas mejoras

- Externalizar las credenciales de `ConexionMySQL` mediante un archivo `config.properties` ignorado en Git.
- Agregar reporte diario exportable (CSV/PDF) de ingresos y ocupación.
- Permitir configurar dinámicamente la capacidad total de espacios y tarifas desde la interfaz.
- Implementar búsqueda y filtrado por placa o rango de fechas en el historial de tickets.
- Incorporar pruebas unitarias con JUnit para el cálculo de tarifas y validaciones de negocio.

---

## 👨‍💻 Autor

**Ángel Caldas**  
Estudiante de Ingeniería de Sistemas Computacionales | Desarrollador Full Stack  

Enfocado en el desarrollo de soluciones de software con Java, Spring Boot, Angular, TypeScript y bases de datos relacionales y en la nube.

- GitHub: https://github.com/angelcaldasdev

---

*🅿️ SistemaEstacionamiento: control operativo, cálculo preciso y persistencia confiable para la gestión vehicular.*
