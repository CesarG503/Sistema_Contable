CREATE DATABASE Contabilidad;
\c Contabilidad;

CREATE TABLE tbl_usuarios
(
    id_usuario SERIAL PRIMARY KEY,
    usuario    TEXT         NOT NULL,
    correo     TEXT         NOT NULL,
    pwd        VARCHAR(255) NOT NULL
);


CREATE TABLE tbl_empresas
(
    id_empresa     SERIAL PRIMARY KEY,
    nombre         TEXT NOT NULL,
    nit            TEXT UNIQUE,
    direccion      TEXT,
    descripcion    TEXT,
    telefono       TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tbl_usuarios_empresas
(
    id_usuario_empresa SERIAL PRIMARY KEY,
    id_usuario         INTEGER NOT NULL,
    id_empresa         INTEGER NOT NULL,
    permiso            INTEGER,
    fecha_afiliacion   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuemp_usuario FOREIGN KEY (id_usuario)
        REFERENCES tbl_usuarios (id_usuario)
        ON DELETE CASCADE,
    CONSTRAINT fk_usuemp_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE,
    CONSTRAINT uq_usuemp UNIQUE (id_usuario, id_empresa)
);


CREATE TABLE tbl_tipo_documento
(
    id_tipo    SERIAL PRIMARY KEY,
    nombre     TEXT NOT NULL,
    id_empresa INTEGER,
    CONSTRAINT fk_tipo_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE
);

CREATE TABLE tbl_partidas
(
    id_partida         SERIAL PRIMARY KEY,
    autor              INTEGER NOT NULL,
    concepto           TEXT,
    fecha              TIMESTAMP,
    id_empresa         INTEGER NOT NULL,
    id_usuario_empresa INTEGER,
    CONSTRAINT fk_partida_usuario FOREIGN KEY (autor)
        REFERENCES tbl_usuarios (id_usuario)
        ON DELETE SET NULL,
    CONSTRAINT fk_partida_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE,
    CONSTRAINT fk_partida_usuemp FOREIGN KEY (id_usuario_empresa)
        REFERENCES tbl_usuarios_empresas (id_usuario_empresa)
        ON DELETE SET NULL
);

CREATE TABLE tbl_cuentas
(
    id_cuenta   SERIAL PRIMARY KEY,
    nombre      TEXT NOT NULL,
    descripcion TEXT,
    naturaleza  CHAR(1),
    tipo        TEXT,
    id_empresa  INTEGER,
    CONSTRAINT fk_cuenta_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE
);


CREATE TABLE tbl_documentos_fuente
(
    id_documento       SERIAL PRIMARY KEY,
    fecha_subida       TIMESTAMP,
    hash               TEXT,
    ruta               TEXT,
    monto              DECIMAL(12, 2),
    aniadido_por       INTEGER,
    id_tipo            INTEGER,
    id_empresa         INTEGER,
    id_usuario_empresa INTEGER,
    CONSTRAINT fk_doc_fuente_usuario FOREIGN KEY (aniadido_por)
        REFERENCES tbl_usuarios (id_usuario)
        ON DELETE SET NULL,
    CONSTRAINT fk_doc_fuente_tipo FOREIGN KEY (id_tipo)
        REFERENCES tbl_tipo_documento (id_tipo)
        ON DELETE SET NULL,
    CONSTRAINT fk_doc_fuente_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE,
    CONSTRAINT fk_doc_fuente_usuemp FOREIGN KEY (id_usuario_empresa)
        REFERENCES tbl_usuarios_empresas (id_usuario_empresa)
        ON DELETE SET NULL
);


CREATE TABLE tbl_documentos_partidas
(
    id_documento_partida SERIAL PRIMARY KEY,
    id_documento         INTEGER,
    id_partida           INTEGER,
    id_empresa           INTEGER,
    CONSTRAINT fk_doc_part_documento FOREIGN KEY (id_documento)
        REFERENCES tbl_documentos_fuente (id_documento)
        ON DELETE CASCADE,
    CONSTRAINT fk_doc_part_partida FOREIGN KEY (id_partida)
        REFERENCES tbl_partidas (id_partida)
        ON DELETE CASCADE,
    CONSTRAINT fk_doc_part_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE
);

CREATE TABLE tbl_movimientos
(
    id_movimiento      SERIAL PRIMARY KEY,
    id_partida         INTEGER NOT NULL,
    id_cuenta          INTEGER NOT NULL,
    monto              DECIMAL(12, 2),
    tipo               CHAR(1),
    id_empresa         INTEGER,
    id_usuario_empresa INTEGER,
    CONSTRAINT fk_mov_partida FOREIGN KEY (id_partida)
        REFERENCES tbl_partidas (id_partida)
        ON DELETE CASCADE,
    CONSTRAINT fk_mov_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES tbl_cuentas (id_cuenta)
        ON DELETE CASCADE,
    CONSTRAINT fk_mov_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE,
    CONSTRAINT fk_mov_usuemp FOREIGN KEY (id_usuario_empresa)
        REFERENCES tbl_usuarios_empresas (id_usuario_empresa)
        ON DELETE SET NULL
);


CREATE TABLE tbl_periodos
(
    id_periodo   SERIAL PRIMARY KEY,
    fecha_inicio TIMESTAMP,
    fecha_final  TIMESTAMP,
    cerrado      BOOLEAN,
    id_empresa   INTEGER,
    CONSTRAINT fk_periodo_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE
);


CREATE TABLE tbl_reportes
(
    id_reporte         SERIAL PRIMARY KEY,
    id_periodo         INTEGER,
    generado           TIMESTAMP,
    ruta_archivo       TEXT,
    autor              INTEGER,
    id_empresa         INTEGER,
    id_usuario_empresa INTEGER,
    CONSTRAINT fk_rep_periodo FOREIGN KEY (id_periodo)
        REFERENCES tbl_periodos (id_periodo)
        ON DELETE CASCADE,
    CONSTRAINT fk_rep_usuario FOREIGN KEY (autor)
        REFERENCES tbl_usuarios (id_usuario)
        ON DELETE SET NULL,
    CONSTRAINT fk_rep_empresa FOREIGN KEY (id_empresa)
        REFERENCES tbl_empresas (id_empresa)
        ON DELETE CASCADE,
    CONSTRAINT fk_rep_usuemp FOREIGN KEY (id_usuario_empresa)
        REFERENCES tbl_usuarios_empresas (id_usuario_empresa)
        ON DELETE SET NULL
);

INSERT INTO tbl_empresas (nombre, nit, direccion, descripcion, telefono)
VALUES ('Contabilidad Global S.A.', '0614-123456-001-0', 'San Miguel, El Salvador', 'Servicios contables', '2660-0000');

INSERT INTO tbl_usuarios (usuario, correo, pwd)
VALUES ('admin', 'admin@empresa.com', '$2y$10$XgNS34l91X/h1nGcEnX9X.IjBNjvcq7zAwcw2R7vl3KsmVFf2i572'); -- contraseña es "admin"

INSERT INTO tbl_usuarios_empresas (id_usuario, id_empresa, permiso)
VALUES (1, 1, 1);
