package service;

import domain.Intention;
import domain.ProductIntention;

import java.util.ArrayList;
import java.util.List;

public class IntentionService {

    private static final List<Intention> intentions = new ArrayList<>();

    private static final IntentionService instance = new IntentionService();


    private IntentionService() {
        intentions.add(new Intention(
                List.of("oi","olá","ola"),
                BotCommands::saudacao
        ));

        intentions.add(new Intention(
                List.of("bom dia"),
                BotCommands::bomDia
        ));

        intentions.add(new Intention(
                List.of("boa tarde"),
                BotCommands::boaTarde
        ));

        intentions.add(new Intention(
                List.of("boa noite"),
                BotCommands::boaNoite
        ));

        intentions.add(new Intention(
                List.of("limpar carrinho", "esvaziar carrinho", "zerar carrinho", "cancelar pedido"),
                BotCommands::limparCarrinho
        ));

        intentions.add(new Intention(
                List.of("ajuda", "help", "comandos", "menu"),
                BotCommands::ajuda
        ));

        intentions.add(new Intention(
                List.of("carrinho", "meu carrinho", "resumo do pedido", "fechar pedido", "meu pedido"),
                BotCommands::carrinho
        ));

        intentions.add(new Intention(
                List.of("consulta", "estoque", "produtos", "opções", "opcoes", "catalogo", "catálogo"),
                BotCommands::estoque
        ));

        intentions.add(new Intention(
                List.of("preço", "preco", "valor", "quanto custa", "tabela de preços", "tabela de precos"),
                BotCommands::informacaoPrecos
        ));

        intentions.add(new Intention(
                List.of("entrega", "frete", "envio", "prazo de entrega", "rastreio", "rastreamento"),
                BotCommands::entrega
        ));

        intentions.add(new Intention(
                List.of("pagamento", "pix", "cartão", "cartao", "boleto", "parcelamento", "parcelas"),
                BotCommands::pagamento
        ));

        intentions.add(new Intention(
                List.of("troca", "trocar", "devolução", "devolucao", "garantia", "reembolso"),
                BotCommands::politicaTrocas
        ));

        intentions.add(new Intention(
                List.of("promoção", "promocao", "desconto", "cupom", "oferta", "liquidação", "liquidacao"),
                BotCommands::promocoes
        ));

        intentions.add(new Intention(
                List.of("horário", "horario", "atendimento", "contato", "telefone", "whatsapp", "falar com"),
                BotCommands::contato
        ));

        intentions.add(new Intention(
                List.of("obrigado", "obrigada", "valeu", "agradeço", "agradeco", "muito obrigado", "muito obrigada"),
                BotCommands::agradecimento
        ));

        intentions.add(new Intention(
                List.of("tchau", "até logo", "ate logo", "até breve", "ate breve", "adeus", "falou"),
                BotCommands::despedida
        ));

        intentions.add(new Intention(
                List.of("quem somos", "sobre a loja", "sobre vocês", "sobre voces"),
                BotCommands::sobreLoja
        ));

        intentions.add(new ProductIntention(
                List.of("camisa", "camisa do corinthians", "corinthians"),
                BotCommands::camisa
        ));

        intentions.add(new ProductIntention(
                List.of("calça", "calca", "jeans"),
                BotCommands::calca
        ));

        intentions.add(new ProductIntention(
                List.of("bermuda"),
                BotCommands::bermuda
        ));

        intentions.add(new ProductIntention(
                List.of("boné", "bone", "bone bordado"),
                BotCommands::bone
        ));

        intentions.add(new ProductIntention(
                List.of("tênis", "tenis", "tennis"),
                BotCommands::tenis
        ));

        intentions.add(new ProductIntention(
                List.of("jaqueta", "corta-vento", "corta vento"),
                BotCommands::jaqueta
        ));

        intentions.add(new ProductIntention(
                List.of("meia", "meias"),
                BotCommands::meia
        ));

        intentions.add(new ProductIntention(
                List.of("vestido"),
                BotCommands::vestido
        ));

        intentions.add(new ProductIntention(
                List.of("cachecol"),
                BotCommands::cachecol
        ));

        intentions.add(new ProductIntention(
                List.of("luva", "luvas"),
                BotCommands::luvas
        ));

        intentions.add(new Intention(
                List.of(
                        "quero comprar",
                        "quero compra",
                        "fazer compra",
                        "fazer uma compra",
                        "iniciar compra",
                        "adicionar ao carrinho",
                        "quero levar",
                        "comprar agora",
                        "como comprar",
                        "compra",
                        "comprar"
                ),
                BotCommands::compra
        ));

    }

    public static IntentionService getInstance() {
        return instance;
    }

    public List<Intention> getIntentions() {
        return intentions;
    }


}
