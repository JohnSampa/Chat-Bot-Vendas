-- SQL para adicionar/atualizar somente a intencao de desconto no Pix.
-- Banco alvo: PostgreSQL.
-- Pode executar mais de uma vez.
-- Este script NAO altera a intencao "Compra fora do escopo da loja".

DO $$
DECLARE
    v_intencao_desconto_pix_id bigint;
BEGIN
    SELECT id
      INTO v_intencao_desconto_pix_id
      FROM intencao_tb
     WHERE intencao = 'Desconto no Pix'
     ORDER BY id
     LIMIT 1;

    IF v_intencao_desconto_pix_id IS NULL THEN
        INSERT INTO intencao_tb (intencao, resposta, setor, tipo_acao, ativa)
        VALUES (
            'Desconto no Pix',
            'No momento nao ha desconto automatico no Pix. Voce pode pagar no Pix ao finalizar o pedido.',
            'FINANCEIRO',
            NULL,
            true
        )
        RETURNING id INTO v_intencao_desconto_pix_id;
    ELSE
        UPDATE intencao_tb
           SET resposta = 'No momento nao ha desconto automatico no Pix. Voce pode pagar no Pix ao finalizar o pedido.',
               setor = 'FINANCEIRO',
               tipo_acao = NULL,
               ativa = true
         WHERE id = v_intencao_desconto_pix_id;
    END IF;

    DELETE FROM tb_palavra_chave
     WHERE intencao_id = v_intencao_desconto_pix_id;

    INSERT INTO tb_palavra_chave (palavra_chave, setor, peso, intencao_id)
    VALUES
        ('desconto no pix', 'FINANCEIRO', 8, v_intencao_desconto_pix_id),
        ('tem desconto no pix', 'FINANCEIRO', 8, v_intencao_desconto_pix_id),
        ('pix tem desconto', 'FINANCEIRO', 8, v_intencao_desconto_pix_id),
        ('desconto para pix', 'FINANCEIRO', 8, v_intencao_desconto_pix_id),
        ('desconto pagando no pix', 'FINANCEIRO', 8, v_intencao_desconto_pix_id),
        ('pagando no pix tem desconto', 'FINANCEIRO', 8, v_intencao_desconto_pix_id),
        ('a vista no pix tem desconto', 'FINANCEIRO', 8, v_intencao_desconto_pix_id);
END $$;
