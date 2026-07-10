-- SQL para adicionar somente novas palavras-chave de consulta de pedidos.
-- Banco alvo: PostgreSQL.
-- Pode executar mais de uma vez sem duplicar palavras-chave.

DO $$
DECLARE
    v_intencao_id bigint;
BEGIN
    SELECT id
      INTO v_intencao_id
      FROM intencao_tb
     WHERE intencao = 'Consultar pedidos e vendas do cliente'
     ORDER BY id
     LIMIT 1;

    IF v_intencao_id IS NULL THEN
        INSERT INTO intencao_tb (intencao, resposta, tipo_acao, setor, ativa)
        VALUES (
            'Consultar pedidos e vendas do cliente',
            'Vou buscar os pedidos vinculados ao CPF/CNPJ informado.',
            'VERIFICAR_VENDAS',
            'VENDAS',
            true
        )
        RETURNING id INTO v_intencao_id;
    ELSE
        UPDATE intencao_tb
           SET resposta = 'Vou buscar os pedidos vinculados ao CPF/CNPJ informado.',
               tipo_acao = 'VERIFICAR_VENDAS',
               setor = 'VENDAS',
               ativa = true
         WHERE id = v_intencao_id;
    END IF;

    INSERT INTO tb_palavra_chave (palavra_chave, setor, peso, intencao_id)
    SELECT palavra_chave, 'VENDAS', peso, v_intencao_id
      FROM (VALUES
        ('consultar meus pedidos', 5),
        ('checar pedidos', 5),
        ('checar meus pedidos', 6),
        ('verificar pedidos', 5),
        ('verificar meus pedidos', 6),
        ('quero verificar meus pedidos', 7),
        ('quero checar meus pedidos', 7),
        ('quero consultar meus pedidos', 7),
        ('listar meus pedidos', 5),
        ('ver minhas compras', 5),
        ('consultar minhas compras', 5),
        ('historico de pedidos', 5)
      ) AS novas_palavras(palavra_chave, peso)
     WHERE NOT EXISTS (
        SELECT 1
          FROM tb_palavra_chave existente
         WHERE existente.intencao_id = v_intencao_id
           AND lower(existente.palavra_chave) = lower(novas_palavras.palavra_chave)
     );
END $$;
