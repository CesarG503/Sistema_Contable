CREATE DATABASE Contabilidad;
\c Contabilidad;

CREATE TABLE tbl_usuarios (
    id_usuario SERIAL PRIMARY KEY,
    usuario TEXT NOT NULL ,
    pwd VARCHAR(255) NOT NULL,
    permiso INTEGER
);

CREATE TABLE tbl_tipo_documento (
    id_tipo SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL
);


CREATE TABLE tbl_partidas (
    id_partida SERIAL PRIMARY KEY,
    autor INTEGER NOT NULL,
    concepto TEXT,
    fecha TIMESTAMP,
    CONSTRAINT fk_partida_usuario FOREIGN KEY (autor)
        REFERENCES tbl_usuarios (id_usuario)
);

CREATE TABLE tbl_cuentas (
    id_cuenta SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT,
    naturaleza CHAR(1)
);

CREATE TABLE tbl_documentos_fuente (
    id_documento SERIAL PRIMARY KEY,
    fecha_subida TIMESTAMP,
    hash TEXT,
    ruta TEXT,
    monto DECIMAL(12,2),
    aniadido_por INTEGER,
    id_tipo INTEGER,
    CONSTRAINT fk_doc_fuente_usuario FOREIGN KEY (aniadido_por)
        REFERENCES tbl_usuarios (id_usuario),
    CONSTRAINT fk_doc_fuente_tipo FOREIGN KEY (id_tipo)
        REFERENCES tbl_tipo_documento (id_tipo)
);


CREATE TABLE tbl_documentos_partidas (
    id_documento_partida SERIAL PRIMARY KEY,
    id_documento INTEGER,
    id_partida INTEGER,
    CONSTRAINT fk_doc_part_documento FOREIGN KEY (id_documento)
        REFERENCES tbl_documentos_fuente (id_documento),
    CONSTRAINT fk_doc_part_partida FOREIGN KEY (id_partida)
        REFERENCES tbl_partidas (id_partida)
);


CREATE TABLE tbl_movimientos (
    id_movimiento SERIAL PRIMARY KEY,
    id_partida INTEGER NOT NULL,
    id_cuenta INTEGER NOT NULL,
    monto DECIMAL(12,2),
    tipo CHAR(1),
    CONSTRAINT fk_mov_partida FOREIGN KEY (id_partida)
        REFERENCES tbl_partidas (id_partida),
    CONSTRAINT fk_mov_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES tbl_cuentas (id_cuenta)
);

CREATE TABLE tbl_periodos (
    id_periodo SERIAL PRIMARY KEY,
    fecha_inicio TIMESTAMP,
    fecha_final TIMESTAMP,
    cerrado BOOLEAN
);

CREATE TABLE tbl_reportes (
    id_reporte SERIAL PRIMARY KEY,
    id_periodo INTEGER,
    generado TIMESTAMP,
    ruta_archivo TEXT,
    autor INTEGER,
    CONSTRAINT fk_rep_periodo FOREIGN KEY (id_periodo)
        REFERENCES tbl_periodos (id_periodo),
    CONSTRAINT fk_rep_usuario FOREIGN KEY (autor)
        REFERENCES tbl_usuarios (id_usuario)
);