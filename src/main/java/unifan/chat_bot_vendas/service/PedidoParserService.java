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

    private static final Pattern ITEM_COM_QUANTIDADE = Pattern.compile(
            "(\\d+|uma|um|duas|dois|tres|três|quatro|cinco|seis|sete|oito|nove|dez)\\s+([^,;]+?)(?=\\s+(?:e\\s+)?(?:\\d+|uma|um|duas|dois|tres|três|quatro|cinco|seis|sete|oito|nove|dez)\\s+|[,;]|$)"
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
        Matcher matcher = ITEM_COM_QUANTIDADE.matcher(texto);
        List<ItemPedidoExtraido> itens = new ArrayList<>();

        while (matcher.find()) {
            Integer quantidade = converterQuantidade(matcher.group(1));
            String termo = limparTermo(matcher.group(2));

            if (quantidade != null && quantidade > 0 && !termo.isBlank()) {
                itens.add(new ItemPedidoExtraido(termo, quantidade));
            }
        }

        return itens;
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
                .replaceAll("\\b(quero|queria|gostaria|desejo|preciso|comprar|compra|levar|pegar|adicionar|add|mais|de)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limparTermo(String termo) {
        return termo
                .replaceAll("\\b(unidades|unidade|pecas|peças|itens|item|do|da|de|tem|por favor|pfv)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
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
