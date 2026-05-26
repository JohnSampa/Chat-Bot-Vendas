ALTER TABLE sessao_chat
    ADD COLUMN IF NOT EXISTS tipo_produto_interesse varchar(255),
    ADD COLUMN IF NOT EXISTS carrinho_json text,
    ADD COLUMN IF NOT EXISTS itens_pendentes_json text,
    ADD COLUMN IF NOT EXISTS item_pendente_json text;

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
        'SAIR_COMPRA'
    )
);
