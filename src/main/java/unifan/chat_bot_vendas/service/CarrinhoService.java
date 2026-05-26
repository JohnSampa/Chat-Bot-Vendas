package unifan.chat_bot_vendas.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.SessaoChat;
import unifan.chat_bot_vendas.dto.CarrinhoResponse;
import unifan.chat_bot_vendas.dto.ItemCarrinho;
import unifan.chat_bot_vendas.dto.ItemPendente;

import java.util.ArrayList;
import java.util.List;

@Service
public class CarrinhoService {

    private final ObjectMapper objectMapper;

    public CarrinhoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void limpar(SessaoChat sessao) {
        sessao.setCarrinhoJson(null);
        sessao.setItensPendentesJson(null);
        sessao.setItemPendenteJson(null);
    }

    public List<ItemCarrinho> getItens(SessaoChat sessao) {
        if (sessao.getCarrinhoJson() == null || sessao.getCarrinhoJson().isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(sessao.getCarrinhoJson(), new TypeReference<>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void adicionarItem(SessaoChat sessao, Produto produto, Integer quantidade) {
        List<ItemCarrinho> itens = getItens(sessao);
        itens.add(new ItemCarrinho(produto, quantidade));
        salvarItens(sessao, itens);
    }

    public CarrinhoResponse montarResponse(SessaoChat sessao) {
        List<ItemCarrinho> itens = getItens(sessao);
        double total = itens.stream().mapToDouble(ItemCarrinho::total).sum();
        return new CarrinhoResponse(itens, total);
    }

    public void salvarPendentes(SessaoChat sessao, List<ItemPendente> pendentes) {
        sessao.setItensPendentesJson(escrever(pendentes));
    }

    public List<ItemPendente> getPendentes(SessaoChat sessao) {
        if (sessao.getItensPendentesJson() == null || sessao.getItensPendentesJson().isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(sessao.getItensPendentesJson(), new TypeReference<>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void salvarItemPendente(SessaoChat sessao, ItemPendente itemPendente) {
        sessao.setItemPendenteJson(escrever(itemPendente));
    }

    public ItemPendente getItemPendente(SessaoChat sessao) {
        if (sessao.getItemPendenteJson() == null || sessao.getItemPendenteJson().isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(sessao.getItemPendenteJson(), ItemPendente.class);
        } catch (Exception e) {
            return null;
        }
    }

    public ItemPendente proximoPendente(SessaoChat sessao) {
        List<ItemPendente> pendentes = getPendentes(sessao);
        if (pendentes.isEmpty()) {
            sessao.setItemPendenteJson(null);
            sessao.setItensPendentesJson(null);
            return null;
        }

        ItemPendente proximo = pendentes.remove(0);
        salvarPendentes(sessao, pendentes);
        salvarItemPendente(sessao, proximo);
        return proximo;
    }

    private void salvarItens(SessaoChat sessao, List<ItemCarrinho> itens) {
        sessao.setCarrinhoJson(escrever(itens));
    }

    private String escrever(Object objeto) {
        try {
            return objectMapper.writeValueAsString(objeto);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel serializar o carrinho", e);
        }
    }
}
