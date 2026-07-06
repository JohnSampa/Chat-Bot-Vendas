package unifan.chat_bot_vendas.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.PalavraChave;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.Venda;
import unifan.chat_bot_vendas.dto.ChatbotRequest;
import unifan.chat_bot_vendas.dto.ChatbotResponse;
import unifan.chat_bot_vendas.dto.PedidoRequest;
import unifan.chat_bot_vendas.service.ChatBotService;
import unifan.chat_bot_vendas.service.IntencaoService;
import unifan.chat_bot_vendas.service.ProdutoService;
import unifan.chat_bot_vendas.service.VendaService;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @Autowired
    private IntencaoService intencaoService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private VendaService vendaService;

    @PostMapping()
    public ResponseEntity<ChatbotResponse> sendMessage(@RequestBody ChatbotRequest request) {
        return ResponseEntity.ok(chatBotService.processarMensagem(request));
    }

    @PostMapping("/iniciar")
    public ResponseEntity<ChatbotResponse> iniciarConversa(@RequestBody ChatbotRequest request) {
        return ResponseEntity.ok(chatBotService.iniciarNovaSessao(request));
    }

    @PostMapping("/reiniciar")
    public ResponseEntity<ChatbotResponse> reiniciarConversa(@RequestBody(required = false) ChatbotRequest request) {
        return ResponseEntity.ok(chatBotService.reiniciarConversa(request));
    }

    @PostMapping("/intencao")
    public ResponseEntity<List<Intencao>> salveIntecao(@RequestBody List<Intencao> intencao) {
        return ResponseEntity.ok(intencaoService.salveAll(intencao));
    }

    @PostMapping("/intencao/{id}/palavras-chave")
    public ResponseEntity<Intencao> adicionarPalavrasChave(
            @PathVariable Long id,
            @RequestBody List<PalavraChave> palavrasChaves
    ) {
        return ResponseEntity.ok(intencaoService.adicionarPalavrasChave(id, palavrasChaves));
    }

    @GetMapping({"", "/intencoes"})
    public ResponseEntity<List<Intencao>> getIntencao() {
        return ResponseEntity.ok(intencaoService.getIntencao());
    }

    @PostMapping("/produtos")
    public ResponseEntity<List<Produto>> salveProdutos(@RequestBody List<Produto> produtos) {
        return ResponseEntity.ok(produtoService.salveProdutos(produtos));
    }

    @PostMapping("/pedidos/{cpf}")
    public ResponseEntity<Venda> salvarPedido(
            @PathVariable String cpf,
            @RequestBody PedidoRequest request
    ) {
        return ResponseEntity.ok(vendaService.salvarPedido(cpf, request));
    }

    @GetMapping("/pedidos/{cpf}")
    public ResponseEntity<List<Venda>> listarPedidos(@PathVariable String cpf) {
        return ResponseEntity.ok(vendaService.getVendas(cpf));
    }

    @PutMapping("/pedidos/{cpf}/{pedidoId}")
    public ResponseEntity<Venda> atualizarPedido(
            @PathVariable String cpf,
            @PathVariable Long pedidoId,
            @RequestBody PedidoRequest request
    ) {
        return ResponseEntity.ok(vendaService.atualizarPedido(cpf, pedidoId, request));
    }

    @DeleteMapping("/pedidos/{cpf}/{pedidoId}")
    public ResponseEntity<Void> deletarPedido(
            @PathVariable String cpf,
            @PathVariable Long pedidoId
    ) {
        vendaService.deletarPedido(cpf, pedidoId);
        return ResponseEntity.noContent().build();
    }
}
