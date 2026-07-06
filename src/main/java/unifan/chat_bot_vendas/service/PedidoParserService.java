package unifan.chat_bot_vendas.service;

import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.dto.ItemPedidoExtraido;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PedidoParserService {

    private static final String QUANTIDADE = "\\d+|uma|um|duas|dois|tres|três|quatro|cinco|seis|sete|oito|nove|dez";

    private static final Pattern QUANTIDADE_ANTES_PRODUTO = Pattern.compile(
            "^(" + QUANTIDADE + ")\\s+(.+)$"
    );

    private static final Pattern PRODUTO_ANTES_QUANTIDADE = Pattern.compile(
            "^(.+?)\\s+(" + QUANTIDADE + ")(?:\\s+.*)?$"
    );

    private static final Map<String, Integer> NUMEROS = Map.ofEntries(
            Map.entry("um", 1),
            Map.entry("uma", 1),
            Map.entry("dois", 2),
            Map.entry("duas", 2),
            Map.entry("tres", 3),
            Map.entry("três", 3),
            Map.entry("quatro", 4),
            Map.entry("cinco", 5),
            Map.entry("seis", 6),
            Map.entry("sete", 7),
            Map.entry("oito", 8),
            Map.entry("nove", 9),
            Map.entry("dez", 10)
    );

    public List<ItemPedidoExtraido> extrairItens(String mensagem) {
        String texto = limparComandosCompra(normalizar(mensagem));
        List<ItemPedidoExtraido> itens = new ArrayList<>();

        for (String trecho : quebrarEmTrechos(texto)) {
            ItemPedidoExtraido item = extrairItem(trecho);
            if (item != null && itens.stream().noneMatch(i ->
                    i.termo().equals(item.termo()) && i.quantidade().equals(item.quantidade()))) {
                itens.add(item);
            }
        }

        return itens;
    }

    private List<String> quebrarEmTrechos(String texto) {
        String textoQuebrado = texto
                .replaceAll("\\s+e\\s+(?=(?:" + QUANTIDADE + ")\\s+)", ",")
                .replaceAll("\\s+e\\s+(?=[a-z]+(?:\\s+[a-z]+)*\\s+(?:" + QUANTIDADE + "))", ",");

        return List.of(textoQuebrado.split("[,;]+"))
                .stream()
                .map(String::trim)
                .filter(trecho -> !trecho.isBlank())
                .toList();
    }

    private ItemPedidoExtraido extrairItem(String trecho) {
        Matcher quantidadeAntes = QUANTIDADE_ANTES_PRODUTO.matcher(trecho);
        if (quantidadeAntes.matches()) {
            return montarItem(quantidadeAntes.group(1), quantidadeAntes.group(2));
        }

        Matcher produtoAntes = PRODUTO_ANTES_QUANTIDADE.matcher(trecho);
        if (produtoAntes.matches()) {
            return montarItem(produtoAntes.group(2), produtoAntes.group(1));
        }

        String termo = limparTermo(trecho);
        if (pareceProduto(termo)) {
            return new ItemPedidoExtraido(termo, 1);
        }

        return null;
    }

    private ItemPedidoExtraido montarItem(String quantidadeTexto, String termoTexto) {
        Integer quantidade = converterQuantidade(quantidadeTexto);
        String termo = limparTermo(termoTexto);

        if (quantidade == null || quantidade <= 0 || termo.isBlank()) {
            return null;
        }

        return new ItemPedidoExtraido(termo, quantidade);
    }

    private Integer converterQuantidade(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return NUMEROS.get(normalizar(texto));
        }
    }

    private String limparComandosCompra(String texto) {
        return texto
                .replaceAll("\\b(quero|queria|gostaria|desejo|preciso|comprar|compra|comparar|levar|pegar|adicionar|adiciona|adiciono|incluir|inclui|colocar|coloca|add|mais|tambem|de)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limparTermo(String termo) {
        return termo
                .replaceAll("\\b(unidades|unidade|pecas|peças|itens|item|do|da|de|tambem|tem|por favor|pfv)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean pareceProduto(String termo) {
        return termo.matches(".*\\b(camisa|camiseta|regata|calca|short|shorts|bermuda|blusa|moletom|sapato|calcado|tenis)\\b.*");
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto.toLowerCase().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9,;\\s]", "")
                .trim();
    }
}
