-- Script para agregar columnas de recuperación de contraseña a la tabla tbl_usuarios
-- Ejecutar este script después de crear la base de datos inicial

-- Agregar columna para el token de recuperación
ALTER TABLE tbl_usuarios
ADD COLUMN IF NOT EXISTS token_recuperacion VARCHAR(255);

-- Agregar columna para la fecha de expiración del token
ALTER TABLE tbl_usuarios
ADD COLUMN IF NOT EXISTS token_expiracion TIMESTAMP;

-- Agregar índice para búsqueda rápida por token
CREATE INDEX IF NOT EXISTS idx_token_recuperacion
ON tbl_usuarios(token_recuperacion);

-- Comentarios para documentación
COMMENT ON COLUMN tbl_usuarios.token_recuperacion IS 'Token UUID para recuperación de contraseña';
COMMENT ON COLUMN tbl_usuarios.token_expiracion IS 'Fecha y hora de expiración del token (24 horas desde generación)';

