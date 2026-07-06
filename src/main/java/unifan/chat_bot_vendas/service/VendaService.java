package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.Venda;
import unifan.chat_bot_vendas.dto.ItemCarrinho;
import unifan.chat_bot_vendas.dto.PedidoRequest;
import unifan.chat_bot_vendas.exceptions.BusinessException;
import unifan.chat_bot_vendas.repositories.VendaRepository;
import unifan.chat_bot_vendas.utils.TextoUtils;

import java.util.List;

@Service
public class VendaService {

    private static final Long USER_ID_PADRAO = 1L;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ProdutoService produtoService;

    public Venda processarVenda(Produto produto, Integer quantidade) {
        return processarVenda(produto, quantidade, null, null, null, null);
    }

    public Venda processarVenda(Produto produto, Integer quantidade, String formaPagamento, String dadosPagamento) {
        return processarVenda(produto, quantidade, null, formaPagamento, dadosPagamento, null);
    }

    public Venda processarVenda(
            Produto produto,
            Integer quantidade,
            String tamanho,
            String formaPagamento,
            String dadosPagamento,
            String cpfCliente
    ) {
        validarPedido(produto, quantidade);

        Venda venda = new Venda();
        venda.setProduto(produto);
        venda.setQuantidade(quantidade);
        venda.setTamanho(tamanho);
        venda.setUserid(USER_ID_PADRAO);
        venda.setCpfCliente(normalizarCpf(cpfCliente));
        venda.setFormaPagamento(formaPagamento);
        venda.setDadosPagamento(dadosPagamento);
        venda.setStatus("FINALIZADO");
        return vendaRepository.save(venda);
    }

    public List<Venda> processarVendas(List<ItemCarrinho> itens) {
        return processarVendas(itens, null, null, null);
    }

    public List<Venda> processarVendas(List<ItemCarrinho> itens, String formaPagamento, String dadosPagamento) {
        return processarVendas(itens, formaPagamento, dadosPagamento, null);
    }

    public List<Venda> processarVendas(
            List<ItemCarrinho> itens,
            String formaPagamento,
            String dadosPagamento,
            String cpfCliente
    ) {
        return itens
                .stream()
                .map(item -> processarVenda(item.produto(), item.quantidade(), item.tamanho(), formaPagamento, dadosPagamento, cpfCliente))
                .toList();
    }

    public Venda salvarPedido(String cpfCliente, PedidoRequest request) {
        Produto produto = produtoService.buscarProdutoPorId(request.produtoId());
        return processarVenda(
                produto,
                request.quantidade(),
                request.tamanho(),
                request.formaPagamento(),
                request.dadosPagamento(),
                cpfCliente
        );
    }

    public Venda atualizarPedido(String cpfCliente, Long pedidoId, PedidoRequest request) {
        Venda venda = buscarPedidoDoCliente(cpfCliente, pedidoId);

        if (request.produtoId() != null) {
            Produto produto = produtoService.buscarProdutoPorId(request.produtoId());
            if (produto == null) {
                throw new BusinessException("Produto nao encontrado para atualizar o pedido");
            }
            venda.setProduto(produto);
        }

        if (request.quantidade() != null) {
            if (request.quantidade() <= 0) {
                throw new BusinessException("Informe uma quantidade maior que zero");
            }
            venda.setQuantidade(request.quantidade());
        }

        if (request.tamanho() != null) {
            venda.setTamanho(request.tamanho());
        }

        if (request.formaPagamento() != null && !request.formaPagamento().isBlank()) {
            venda.setFormaPagamento(request.formaPagamento());
        }

        if (request.dadosPagamento() != null && !request.dadosPagamento().isBlank()) {
            venda.setDadosPagamento(request.dadosPagamento());
        }

        return vendaRepository.save(venda);
    }

    public void deletarPedido(String cpfCliente, Long pedidoId) {
        Venda venda = buscarPedidoDoCliente(cpfCliente, pedidoId);
        venda.setStatus("CANCELADO");
        vendaRepository.save(venda);
    }

    public List<Venda> getVendas() {
        return vendaRepository.findByUseridOrderByIdDesc(USER_ID_PADRAO);
    }

    public List<Venda> getVendas(String cpfCliente) {
        String cpf = normalizarCpf(cpfCliente);
        if (cpf.isBlank()) {
            return getVendas();
        }

        return vendaRepository.findByCpfClienteOrderByIdDesc(cpf);
    }

    private Venda buscarPedidoDoCliente(String cpfCliente, Long pedidoId) {
        String cpf = normalizarCpf(cpfCliente);
        if (cpf.isBlank()) {
            throw new BusinessException("Informe o CPF do cliente");
        }

        Venda venda = vendaRepository.findById(pedidoId)
                .orElseThrow(() -> new BusinessException("Pedido nao encontrado"));

        if (!cpf.equals(venda.getCpfCliente())) {
            throw new BusinessException("Pedido nao pertence ao CPF informado");
        }

        return venda;
    }

    private void validarPedido(Produto produto, Integer quantidade) {
        if (produto == null) {
            throw new BusinessException("Produto nao encontrado");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new BusinessException("Informe uma quantidade maior que zero");
        }
    }

    private String normalizarCpf(String cpfCliente) {
        return TextoUtils.somenteDigitos(cpfCliente);
    }
}
