-- Execute este arquivo quando o banco ja existe e o chatbot falha ao salvar
-- AGUARDANDO_CPF ou AGUARDANDO_TAMANHO em sessao_chat.estado.

ALTER TABLE sessao_chat
    ADD COLUMN IF NOT EXISTS cpf_cliente varchar(20),
    ADD COLUMN IF NOT EXISTS client_session_id varchar(255),
    ADD COLUMN IF NOT EXISTS iniciada_em timestamp,
    ADD COLUMN IF NOT EXISTS ativa boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS tamanho varchar(20),
    ADD COLUMN IF NOT EXISTS itens_tamanho_pendentes_json text,
    ADD COLUMN IF NOT EXISTS item_tamanho_pendente_json text,
    ADD COLUMN IF NOT EXISTS mensagem_pendente_cpf text;

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
        WHERE constraint_table.relname = 'sessao_chat'
          AND constraint_definition.contype = 'c'
          AND pg_get_constraintdef(constraint_definition.oid) ILIKE '%estado%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
            constraint_record.schema_name,
            constraint_record.table_name,
            constraint_record.constraint_name
        );
    END LOOP;
END $$;

ALTER TABLE sessao_chat
ADD CONSTRAINT sessao_chat_estado_check
CHECK (
    estado IN (
        'INICIAL',
        'AGUARDANDO_CPF',
        'AGUARDANDO_PRODUTO',
        'AGUARDANDO_RESOLUCAO_ITEM',
        'AGUARDANDO_CONFIRMACAO_PRODUTO',
        'AGUARDANDO_TAMANHO',
        'AGUARDANDO_QUANTIDADE',
        'AGUARDANDO_CONFIRMACAO_COMPRA',
        'AGUARDANDO_CONFIRMACAO_CARRINHO',
        'AGUARDANDO_ITEM_ATUALIZACAO',
        'AGUARDANDO_NOVO_PRODUTO_ATUALIZACAO',
        'AGUARDANDO_FORMA_PAGAMENTO',
        'AGUARDANDO_DADOS_PAGAMENTO',
        'SAIR_COMPRA'
    )
);

CREATE INDEX IF NOT EXISTS idx_sessao_chat_cpf_ativa
    ON sessao_chat(cpf_cliente, ativa);

CREATE INDEX IF NOT EXISTS idx_sessao_chat_client_session_ativa
    ON sessao_chat(client_session_id, ativa);
