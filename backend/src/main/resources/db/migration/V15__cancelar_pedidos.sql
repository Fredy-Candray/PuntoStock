DECLARE @restriccion sysname;
SELECT @restriccion = name
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'pedidos')
  AND parent_column_id = COLUMNPROPERTY(OBJECT_ID(N'pedidos'), N'estado', 'ColumnId');

IF @restriccion IS NULL
    THROW 50000, 'No se encontro la restriccion de estado de pedidos.', 1;

DECLARE @sql nvarchar(500) = N'ALTER TABLE pedidos DROP CONSTRAINT ' + QUOTENAME(@restriccion);
EXEC sys.sp_executesql @sql;

ALTER TABLE pedidos ADD CONSTRAINT ck_pedido_estado
    CHECK (estado IN ('PENDIENTE', 'LISTO', 'ENTREGADO', 'CANCELADO'));
