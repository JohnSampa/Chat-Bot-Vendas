package unifan.chat_bot_vendas.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.ItemCarrinhoSessao;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.SessaoChat;
import unifan.chat_bot_vendas.dto.CarrinhoResponse;
import unifan.chat_bot_vendas.dto.ItemCarrinho;
import unifan.chat_bot_vendas.dto.ItemPendente;
import unifan.chat_bot_vendas.dto.ItemTamanhoPendente;
import unifan.chat_bot_vendas.repositories.ItemCarrinhoSessaoRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class CarrinhoService {

    private final ObjectMapper objectMapper;
    private final ItemCarrinhoSessaoRepository itemCarrinhoRepository;

    public CarrinhoService(ObjectMapper objectMapper, ItemCarrinhoSessaoRepository itemCarrinhoRepository) {
        this.objectMapper = objectMapper;
        this.itemCarrinhoRepository = itemCarrinhoRepository;
    }

    @Transactional
    public void limpar(SessaoChat sessao) {
        if (sessao.getId() != null) {
            itemCarrinhoRepository.deleteBySessao(sessao);
        }
        sessao.setCarrinhoJson(null);
        sessao.setItensPendentesJson(null);
        sessao.setItemPendenteJson(null);
        sessao.setItensTamanhoPendentesJson(null);
        sessao.setItemTamanhoPendenteJson(null);
    }

    public List<ItemCarrinho> getItens(SessaoChat sessao) {
        if (sessao.getId() == null) {
            return new ArrayList<>();
        }

        return itemCarrinhoRepository.findBySessaoOrderByIdAsc(sessao)
                .stream()
                .map(item -> new ItemCarrinho(item.getProduto(), item.getQuantidade(), item.getTamanho()))
                .toList();
    }

    public void adicionarItem(SessaoChat sessao, Produto produto, Integer quantidade) {
        adicionarItem(sessao, produto, quantidade, null);
    }

    public void adicionarItem(SessaoChat sessao, Produto produto, Integer quantidade, String tamanho) {
        if (produto == null || produto.getId() == null || quantidade == null || quantidade <= 0) {
            return;
        }

        ItemCarrinhoSessao item = itemCarrinhoRepository.findBySessaoAndProdutoIdAndTamanho(sessao, produto.getId(), tamanho)
                .orElseGet(() -> {
                    ItemCarrinhoSessao novoItem = new ItemCarrinhoSessao();
                    novoItem.setSessao(sessao);
                    novoItem.setProduto(produto);
                    novoItem.setQuantidade(0);
                    novoItem.setTamanho(tamanho);
                    return novoItem;
                });

        item.setQuantidade((item.getQuantidade() == null ? 0 : item.getQuantidade()) + quantidade);
        itemCarrinhoRepository.save(item);
    }

    public boolean atualizarProduto(SessaoChat sessao, Produto produtoAtual, Produto novoProduto) {
        if (produtoAtual == null || produtoAtual.getId() == null || novoProduto == null || novoProduto.getId() == null) {
            return false;
        }

        return itemCarrinhoRepository.findBySessaoAndProdutoId(sessao, produtoAtual.getId())
                .map(item -> {
                    item.setProduto(novoProduto);
                    itemCarrinhoRepository.save(item);
                    return true;
                })
                .orElse(false);
    }

    public boolean removerProduto(SessaoChat sessao, Produto produto) {
        if (produto == null || produto.getId() == null) {
            return false;
        }

        return itemCarrinhoRepository.findBySessaoAndProdutoId(sessao, produto.getId())
                .map(item -> {
                    itemCarrinhoRepository.delete(item);
                    return true;
                })
                .orElse(false);
    }

    public boolean atualizarQuantidadeItemUnico(SessaoChat sessao, Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            return false;
        }

        List<ItemCarrinhoSessao> itens = itemCarrinhoRepository.findBySessaoOrderByIdAsc(sessao);
        if (itens.size() != 1) {
            return false;
        }

        ItemCarrinhoSessao item = itens.get(0);
        item.setQuantidade(quantidade);
        itemCarrinhoRepository.save(item);
        return true;
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

    public void salvarTamanhosPendentes(SessaoChat sessao, List<ItemTamanhoPendente> pendentes) {
        sessao.setItensTamanhoPendentesJson(escrever(pendentes));
    }

    public List<ItemTamanhoPendente> getTamanhosPendentes(SessaoChat sessao) {
        if (sessao.getItensTamanhoPendentesJson() == null || sessao.getItensTamanhoPendentesJson().isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(sessao.getItensTamanhoPendentesJson(), new TypeReference<>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void salvarItemTamanhoPendente(SessaoChat sessao, ItemTamanhoPendente itemPendente) {
        sessao.setItemTamanhoPendenteJson(escrever(itemPendente));
    }

    public ItemTamanhoPendente getItemTamanhoPendente(SessaoChat sessao) {
        if (sessao.getItemTamanhoPendenteJson() == null || sessao.getItemTamanhoPendenteJson().isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(sessao.getItemTamanhoPendenteJson(), ItemTamanhoPendente.class);
        } catch (Exception e) {
            return null;
        }
    }

    public ItemTamanhoPendente proximoTamanhoPendente(SessaoChat sessao) {
        List<ItemTamanhoPendente> pendentes = getTamanhosPendentes(sessao);
        if (pendentes.isEmpty()) {
            sessao.setItemTamanhoPendenteJson(null);
            sessao.setItensTamanhoPendentesJson(null);
            return null;
        }

        ItemTamanhoPendente proximo = pendentes.remove(0);
        salvarTamanhosPendentes(sessao, pendentes);
        salvarItemTamanhoPendente(sessao, proximo);
        return proximo;
    }

    private String escrever(Object objeto) {
        try {
            return objectMapper.writeValueAsString(objeto);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel serializar dados temporarios da sessao", e);
        }
    }
}
