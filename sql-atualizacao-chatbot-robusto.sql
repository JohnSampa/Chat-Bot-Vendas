ALTER TABLE sessao_chat
    ADD COLUMN IF NOT EXISTS tipo_produto_interesse varchar(255),
    ADD COLUMN IF NOT EXISTS carrinho_json text,
    ADD COLUMN IF NOT EXISTS itens_pendentes_json text,
    ADD COLUMN IF NOT EXISTS item_pendente_json text,
    ADD COLUMN IF NOT EXISTS forma_pagamento varchar(255),
    ADD COLUMN IF NOT EXISTS dados_pagamento text,
    ADD COLUMN IF NOT EXISTS cpf_cliente varchar(20),
    ADD COLUMN IF NOT EXISTS client_session_id varchar(255),
    ADD COLUMN IF NOT EXISTS iniciada_em timestamp,
    ADD COLUMN IF NOT EXISTS ativa boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS tamanho varchar(20),
    ADD COLUMN IF NOT EXISTS itens_tamanho_pendentes_json text,
    ADD COLUMN IF NOT EXISTS item_tamanho_pendente_json text,
    ADD COLUMN IF NOT EXISTS mensagem_pendente_cpf text;

ALTER TABLE sessao_chat
    ALTER COLUMN carrinho_json TYPE text,
    ALTER COLUMN itens_pendentes_json TYPE text,
    ALTER COLUMN item_pendente_json TYPE text;

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

ALTER TABLE vendas_tb
    ADD COLUMN IF NOT EXISTS userid bigint,
    ADD COLUMN IF NOT EXISTS cpf_cliente varchar(20),
    ADD COLUMN IF NOT EXISTS tamanho varchar(20),
    ADD COLUMN IF NOT EXISTS forma_pagamento varchar(255),
    ADD COLUMN IF NOT EXISTS dados_pagamento text,
    ADD COLUMN IF NOT EXISTS status varchar(50) DEFAULT 'FINALIZADO';

ALTER TABLE intencao_tb
    ADD COLUMN IF NOT EXISTS setor varchar(50) DEFAULT 'VENDAS';

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

ALTER TABLE tb_palavra_chave
    ADD COLUMN IF NOT EXISTS setor varchar(50) DEFAULT 'VENDAS',
    ADD COLUMN IF NOT EXISTS peso integer DEFAULT 1;

CREATE TABLE IF NOT EXISTS item_carrinho_sessao_tb (
    id bigserial PRIMARY KEY,
    sessao_id bigint NOT NULL REFERENCES sessao_chat(id),
    produto_id bigint NOT NULL REFERENCES produto_tb(id),
    quantidade integer NOT NULL,
    tamanho varchar(20)
);

ALTER TABLE item_carrinho_sessao_tb
    ADD COLUMN IF NOT EXISTS tamanho varchar(20);

CREATE INDEX IF NOT EXISTS idx_sessao_chat_cpf_ativa
    ON sessao_chat(cpf_cliente, ativa);

CREATE INDEX IF NOT EXISTS idx_sessao_chat_client_session_ativa
    ON sessao_chat(client_session_id, ativa);

CREATE INDEX IF NOT EXISTS idx_vendas_cpf
    ON vendas_tb(cpf_cliente);
