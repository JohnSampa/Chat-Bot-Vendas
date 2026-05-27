ALTER TABLE sessao_chat
    ADD COLUMN IF NOT EXISTS tipo_produto_interesse varchar(255),
    ADD COLUMN IF NOT EXISTS carrinho_json text,
    ADD COLUMN IF NOT EXISTS itens_pendentes_json text,
    ADD COLUMN IF NOT EXISTS item_pendente_json text,
    ADD COLUMN IF NOT EXISTS forma_pagamento varchar(255),
    ADD COLUMN IF NOT EXISTS dados_pagamento text;

ALTER TABLE sessao_chat
    ALTER COLUMN carrinho_json TYPE text,
    ALTER COLUMN itens_pendentes_json TYPE text,
    ALTER COLUMN item_pendente_json TYPE text;

ALTER TABLE sessao_chat
DROP CONSTRAINT IF EXISTS sessao_chat_estado_check;

ALTER TABLE sessao_chat
ADD CONSTRAINT sessao_chat_estado_check
CHECK (
    estado IN (
        'INICIAL',
        'AGUARDANDO_PRODUTO',
        'AGUARDANDO_RESOLUCAO_ITEM',
        'AGUARDANDO_CONFIRMACAO_PRODUTO',
        'AGUARDANDO_QUANTIDADE',
        'AGUARDANDO_CONFIRMACAO_COMPRA',
        'AGUARDANDO_CONFIRMACAO_CARRINHO',
        'AGUARDANDO_FORMA_PAGAMENTO',
        'AGUARDANDO_DADOS_PAGAMENTO',
        'SAIR_COMPRA'
    )
);

ALTER TABLE vendas_tb
    ADD COLUMN IF NOT EXISTS userid bigint,
    ADD COLUMN IF NOT EXISTS forma_pagamento varchar(255),
    ADD COLUMN IF NOT EXISTS dados_pagamento text;
