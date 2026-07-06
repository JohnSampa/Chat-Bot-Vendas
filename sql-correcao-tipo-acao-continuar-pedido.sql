-- Execute este arquivo se o banco rejeitar a acao CONTINUAR_PEDIDO
-- ao salvar novas intencoes em intencao_tb.tipo_acao.

DO $$
DECLARE
    constraint_record record;
BEGIN
    FOR constraint_record IN
        SELECT
            constraint_table_namespace.nspname AS schema_name,
            constraint_table.relname AS table_name,
            constraint_definition.conname AS constraint_name
        FROM pg_constraint constraint_definition
        JOIN pg_class constraint_table
            ON constraint_table.oid = constraint_definition.conrelid
        JOIN pg_namespace constraint_table_namespace
            ON constraint_table_namespace.oid = constraint_table.relnamespace
        WHERE constraint_table.relname = 'intencao_tb'
          AND constraint_definition.contype = 'c'
          AND pg_get_constraintdef(constraint_definition.oid) ILIKE '%tipo_acao%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
            constraint_record.schema_name,
            constraint_record.table_name,
            constraint_record.constraint_name
        );
    END LOOP;
END $$;

ALTER TABLE intencao_tb
ADD CONSTRAINT intencao_tb_tipo_acao_check
CHECK (
    tipo_acao IS NULL
    OR tipo_acao IN (
        'INICIAR_COMPRA',
        'CHECAR_ESTOQUE',
        'VERIFICAR_VENDAS',
        'CONTINUAR_PEDIDO',
        'ATUALIZAR_PEDIDO',
        'DELETAR_PEDIDO',
        'SAIR_CHAT_VENDA',
        'COMPRAR_CAMISA',
        'COMPRAR_CALCA',
        'COMPRAR_SHORT_BERMUDA',
        'COMPRAR_BLUSA',
        'COMPRAR_SAPATO'
    )
);
