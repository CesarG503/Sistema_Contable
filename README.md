# Sistema Contable ONEDI

## Descripcion
Este es un proyecto de sistema contable realizado para la materia SIC155 de la carrera de **Ingenieria de Sistemas Informaticos** con el proposito de poder tener un mejor control y registro de las cuentas de una empresa general, puediendo crear cuentas, asientos a necesidad de la empresa, facilitando de tal manera que la empresa tenga un mejor panorama del dinero que manejan.

## Dependencias basicas
- Java 23
- jbcrypt-0.4
- postgresql

## Instrucciones de ejecución
Las instrucciones para poder ejecutar el codigo del proyecto:

***Nota:** El proyecto ha sido creado usando el IDE **IntelliJ IDEA** se recomienda usarlo para poder tener una mejor experiencia de ejecución del proyecto e instalacion de dependencias*

### 1. Clonar el repositorio

``` bash
git clone "https://github.com/CesarG503/Sistema_Contable"
```
### 2. Abrir proyecto
Con el IDE **IntelliJ IDEA** se presiona el boton *Open* para buscar el proyecto, debemos buscar en la carpeta donde anteriormente clonamos el repositorio. Luego de ello procedemos a dar los permisos correspondientes y que confiamos en el autor.

### 3. Conexion a PostgreSQL
En la carpeta del proyecto ```LoginCliente > src > main > resources``` encontramos el archivo ```application.properties``` dicho archivo contiene una propiedad que se llama ```spring.datasource.url```, esta propiedad contiene la cadena de conexion a la base de datos.

Si no posees en tu Postgres una base de datos llamada ```Contabilidad``` entonces creala manualmente pero dejala vacia sin ninguna tabla dentro de ella.

Luego de esto puedes volver a la cadena de conexion y modificar segun tengas configurada tu base de datos; *si tienes el mismo puerto, ip, etc*.

### 4. Buscamos el archivo de arranque
Cuando tengamos la base de datos creada y sin ninguna tabla, entonces busquemos el archivo donde debe arrancar el sistema ```LoginCliente > src > main > java > com > example > LoginCliente > LoginClienteApplicaction.java```.

Si estamos utilizando **IntelliJ IDEA** puede proceder con la ejecución del proyecto solo pulsando el boton de *play verde* en la orilla izquierda, y automaticamente el IDE lo configurara como un archivo de arranque del proyecto.

***Nota:** Si usas otro IDE lee la documentacion del IDE para saber cual seria la manera mas optima de utilizarlo en tu entorno especifico.*

### 5. Visualizar el proyecto
Una vez ejecutado podemos encontrar por defecto la vista del proyecto en el http://localhost:8080 o en http://127.0.0.1:8080 desde nuestro navegador preferido
