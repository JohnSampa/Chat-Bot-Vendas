package unifan.chat_bot_vendas.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.service.ChatBotService;
import unifan.chat_bot_vendas.service.IntencaoService;
import unifan.chat_bot_vendas.service.ProdutoService;

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

    @PostMapping()
    public ResponseEntity<String> sendMessage(@RequestBody String message) {
        String response = chatBotService.processarMensagem(message);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/intencao")
    public ResponseEntity<List<Intencao>> salveIntecao(@RequestBody List<Intencao> intencao) {
        var response = intencaoService.salveAll(intencao);
        return ResponseEntity.ok(response);
    }

    @GetMapping()
    public ResponseEntity<List<Intencao>> getIntencao(){
        var intencoes = intencaoService.getIntencao();

        return ResponseEntity.ok(intencoes);
    }

    @PostMapping("/produtos")
    public ResponseEntity<List<Produto>> salveProdutos(@RequestBody List<Produto> produtos) {
        var response = produtoService.salveProdutos(produtos);

        return ResponseEntity.ok(response);
    }




}
