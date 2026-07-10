-- SQL para adicionar/atualizar as novas intencoes do chatbot.
-- Banco alvo: PostgreSQL.
-- Pode executar mais de uma vez: o script atualiza a intencao e recria suas palavras-chave.

DO $$
DECLARE
    v_intencao_fora_escopo_id bigint;
    v_intencao_desconto_pix_id bigint;
BEGIN
    SELECT id
      INTO v_intencao_fora_escopo_id
      FROM intencao_tb
     WHERE intencao = 'Compra fora do escopo da loja'
     ORDER BY id
     LIMIT 1;

    IF v_intencao_fora_escopo_id IS NULL THEN
        INSERT INTO intencao_tb (intencao, resposta, setor, tipo_acao, ativa)
        VALUES (
            'Compra fora do escopo da loja',
            'Desculpe, trabalhamos apenas com roupas, calcados e produtos de times, como camisas, calcas, shorts, bermudas, blusas, sapatos e tenis.',
            'ATENDIMENTO',
            NULL,
            true
        )
        RETURNING id INTO v_intencao_fora_escopo_id;
    ELSE
        UPDATE intencao_tb
           SET resposta = 'Desculpe, trabalhamos apenas com roupas, calcados e produtos de times, como camisas, calcas, shorts, bermudas, blusas, sapatos e tenis.',
               setor = 'ATENDIMENTO',
               tipo_acao = NULL,
               ativa = true
         WHERE id = v_intencao_fora_escopo_id;
    END IF;

    DELETE FROM tb_palavra_chave
     WHERE intencao_id = v_intencao_fora_escopo_id;

    INSERT INTO tb_palavra_chave (palavra_chave, setor, peso, intencao_id)
    VALUES
        ('moto', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('motocicleta', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('carro', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('automovel', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('veiculo', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('bicicleta', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('patinete', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('caminhao', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('onibus', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('caminhonete', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('pneu', 'ATENDIMENTO', 7, v_intencao_fora_escopo_id),
        ('celular', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('iphone', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('smartphone', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('notebook', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('computador', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('tablet', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('televisao', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('monitor', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('teclado', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('mouse', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('fone', 'ATENDIMENTO', 7, v_intencao_fora_escopo_id),
        ('carregador', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('videogame', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('playstation', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('xbox', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('geladeira', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('fogao', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('microondas', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('ventilador', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('liquidificador', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('airfryer', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('sofa', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('cama', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('colchao', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('mesa', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('cadeira', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('armario', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('estante', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('arroz', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('feijao', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('carne', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('pizza', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('hamburguer', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('lanche', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('refrigerante', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('bebida', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('cerveja', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('vinho', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('shampoo', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('perfume', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('maquiagem', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('fralda', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('remedio', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('medicamento', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('farmacia', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('consulta', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('dentista', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('medico', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('passagem', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('ingresso', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('hotel', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('viagem', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('seguro', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('casa', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('apartamento', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('terreno', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('imovel', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('racao', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('coleira', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('aquario', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('pet', 'ATENDIMENTO', 7, v_intencao_fora_escopo_id),
        ('cimento', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('tijolo', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('areia', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('tinta', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('torneira', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('martelo', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('furadeira', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('parafusadeira', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('jogo', 'ATENDIMENTO', 6, v_intencao_fora_escopo_id),
        ('giftcard', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('assinatura', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('streaming', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('curso', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('livro', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('caderno', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('mochila', 'ATENDIMENTO', 6, v_intencao_fora_escopo_id),
        ('caneta', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id),
        ('lapis', 'ATENDIMENTO', 8, v_intencao_fora_escopo_id);

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
