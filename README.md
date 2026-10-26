# Transaction Management API - Challenge

API REST desarrollada en Spring Boot para la gestión, procesamiento y consulta de transacciones financieras
(crédito y débito) con integración a un proveedor externo.


---

## Tecnologías utilizadas para la construccion de esta aplicacion.

- **Java 21 / 17**
- **Spring Boot 3**
- **Spring RestClient**
- **PostgreSQL Database** (JPA)
- **MapStruct & Lombok**
- **Arquitectura Hexagonal**
- **Springdoc OpenAPI (Swagger)**
- **JUnit 5, Mockito** ración
- **IA Asistente**: **Junie**

---

## Decisiones de Diseño

### Arquitectura Hexagonal

El proyecto está estructurado desacoplando las reglas de negocio de los detalles de infraestructura:

- Maneja las 3 principales capas de la arquitectura Hexagonal para el manejo y desacoplamiento de cada una de las capas
  tambien se implemento esta arquitectura en el caso de que se quisiera escalar la aplicacion con conexiones a
  otros proveedores externos. Asi mismo para poder tener los datos separados por capa se maneja una interfaz mapStruct.
- Las validaciones que se piden en las reglas de negocio se implementaron por medio de clases
  validator, se que tambien se podia manejar a nivel de anotaciones sin embargo me gusta que tiene mas visualizacion el
  tener clases validators.
- Para evitar que se reenvien peticiones se implemento un campo que se llama idempotencyKey que se genera a partir de
  los datos
  clave de la transaccion y una ventana de tiempo de 10 minutos.
- Los mensajes de la aplicacion se manejan en un archivo por separado llamado messages-eng.properties, esto nos puede
  permitir en el futuro cambiar el idioma de la aplicacion, o cambiar los mensajes que se presentan en la aplicacion de
  forma mas limpia
- Un detalle que vi en el documento es que el proveedor externo no muestra un providerReference, este lo agregue en la
  respuesta que genero en el mock, y ademas internamente la tabla de transacciones tambien cuenta con su id, Esto por
  trazabilidad, en el caso de que algo fallase del lado del proveedor se puede rastrear la operacion por ambos lados.
- Dentro del desafio venia un detalle llamado balanceAfter, sin tambien menciona que no se debe manejar la logica de
  gestion de balances, creo que para poder tener esto mejor aterrizado hubiera sido perfecto tener una tabla de account,
  y otra de historico de transacciones pero bueno eso estaba fuera del reto.
- Adicionalmente se agrego swagger en la aplicacion para poder tener la documentacion de la api, siempre es bueno tener
  esto, se implemento con anotaciones, me parece mas limpio hacerlo asi a tener un yaml con todo el swagger definido,
  esto porque cuando una aplicacion crece mucho, esos archivos suelen ser dificiles para darles mantenimiento.
- Finalmente aunque menciona que es una aplicacion que es para millones de transacciones, esto no solo depende de una
  aplicacion para esto tambien se requerie tener un balanceador de carga para tener siempre disponible la aplicacion,
  asi como la base de datos con alta dispoibilidad, tambien creo que seria bueno tener una cola de mensajes para la
  comunicacion asincrona.

## Levantar la aplicación

### Se debe tener instalado:

- JDK 21 o superior instalado.
- Maven.
- Se debe tambien tener levantado el proyecto challenge-mock, a este le configure podman para levantar, aunque tambien
  se podria usar docker.

### Compilar la aplicacion

```bash
mvn clean install
```

### Ejecutar la Aplicación

```bash
.\mvnw.cmd spring-boot:run
```

La aplicación iniciará en `http://localhost:8080`.

El swagger se puede visualizar en `http://localhost:8080/swagger-ui/index.html`.

### Colección de Pruebas

Agregue una coleccion para poder importar ya sea en postman o en bruno en la carpeta:
`bruno_collection/Challenge.json`

### Script para la creacion de bd

Agregue una coleccion para poder importar ya sea en postman o en bruno en la carpeta:
`src/main/resources/db.migration/V1__create_transactions_table.sql`