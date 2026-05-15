package service;



import domain.Produto;

import domain.Venda;



import java.util.ArrayList;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Optional;

import java.util.Scanner;

import java.util.regex.Matcher;

import java.util.regex.Pattern;

import java.util.stream.Collectors;



public class BotCommands {



    private static final List<String> CHAVES_CATALOGO = List.of(

            "consulta", "estoque", "produtos", "opções", "opcoes", "catalogo", "catálogo"

    );



    private static boolean contextoAposListagemCatalogo = false;



    public static boolean textoConsultaCatalogo(String texto) {

        for (String k : CHAVES_CATALOGO) {

            if (texto.contains(k)) {

                return true;

            }

        }

        return false;

    }



    public static void encerrarContextoCatalogo() {

        contextoAposListagemCatalogo = false;

    }



    public static boolean tentarCompraEmContextoCatalogo(String texto) {

        if (!contextoAposListagemCatalogo) {

            return false;

        }

        Optional<Produto> porId = resolverProdutoPorIdNoTexto(texto);

        List<Produto> candidatos;

        if (porId.isPresent()) {

            candidatos = new ArrayList<>();

            candidatos.add(porId.get());

        } else {

            candidatos = encontrarCandidatosPorNomeNoTrecho(texto.trim());

        }

        if (candidatos.isEmpty()) {
            encerrarContextoCatalogo();
            if (texto.trim().matches("\\d+")) {
                System.out.println("Não encontrei esse id no catálogo. Digite estoque para ver a lista.");
                return true;
            }
            return false;
        }

        comprarApartirLista(candidatos, "produto", Optional.empty());

        return true;

    }



    public static boolean consultaCatalogoComReferenciaProduto(String texto) {

        if (!textoConsultaCatalogo(texto)) {

            return false;

        }

        if (!temReferenciaProdutoNoTexto(texto)) {

            return false;

        }

        estoqueComReferenciaProduto(texto);

        return true;

    }



    private static boolean temReferenciaProdutoNoTexto(String texto) {

        if (resolverProdutoPorIdNoTexto(texto).isPresent()) {

            return true;

        }

        String limpo = removerChavesCatalogo(texto).trim();

        return limpo.length() >= 2 && !encontrarCandidatosPorNomeNoTrecho(limpo).isEmpty();

    }



    private static String removerChavesCatalogo(String texto) {

        String s = texto.toLowerCase();

        for (String k : CHAVES_CATALOGO) {

            s = s.replace(k, " ");

        }

        return s.trim().replaceAll("\\s+", " ");

    }



    private static Optional<Produto> resolverProdutoPorIdNoTexto(String texto) {

        Matcher m = Pattern.compile("(\\d+)").matcher(texto);

        while (m.find()) {

            try {

                long id = Long.parseLong(m.group(1));

                Optional<Produto> p = EstoqueService.getInstance()

                        .getProdutos()

                        .stream()

                        .filter(pr -> pr.getId().equals(id))

                        .findFirst();

                if (p.isPresent()) {

                    return p;

                }

            } catch (NumberFormatException ignored) {

            }

        }

        return Optional.empty();

    }



    private static List<Produto> encontrarCandidatosPorNomeNoTrecho(String trechoLimpo) {

        String t = trechoLimpo.toLowerCase().trim();

        if (t.isEmpty()) {

            return new ArrayList<>();

        }

        LinkedHashSet<Produto> set = new LinkedHashSet<>();

        for (Produto p : EstoqueService.getInstance().getProdutos()) {

            String n = p.getNome().toLowerCase();

            if (t.contains(n) || (t.length() >= 2 && n.contains(t))) {

                set.add(p);

            }

        }

        return new ArrayList<>(set);

    }



    private static void estoqueComReferenciaProduto(String texto) {

        Optional<Produto> porId = resolverProdutoPorIdNoTexto(texto);

        List<Produto> candidatos;

        if (porId.isPresent()) {

            candidatos = new ArrayList<>();

            candidatos.add(porId.get());

        } else {

            candidatos = encontrarCandidatosPorNomeNoTrecho(removerChavesCatalogo(texto));

        }

        if (candidatos.isEmpty()) {

            estoque();

            return;

        }

        encerrarContextoCatalogo();

        comprarApartirLista(candidatos, "produto", Optional.empty());

    }



    public static void ajuda() {

        System.out.println("===== Ajuda rápida =====");

        System.out.println("• Digite oi, bom dia, boa tarde ou boa noite para cumprimentos.");

        System.out.println("• Para ver itens: estoque, produtos ou catálogo.");

        System.out.println("• Para comprar: digite compra ou cite o tipo (ex.: camisa, calça, tênis) e siga o passo a passo.");

        System.out.println("• Carrinho: carrinho ou pedido. Para limpar: limpar carrinho ou esvaziar carrinho.");

        System.out.println("• Preços e políticas: preço, entrega, pagamento, troca, promoção, contato.");

        System.out.println("• Sair: sair, exit ou quit.");

    }



    public static void carrinho() {

        List<Venda> vendas = VendasService.getInstance().getVendas();

        if (vendas.isEmpty()) {

            System.out.println("Seu carrinho está vazio. Peça o estoque ou compre um produto pelo nome.");

            return;

        }

        System.out.println("===== Seu carrinho =====");

        double total = 0;

        for (Venda v : vendas) {

            double sub = v.getTotal();

            total += sub;

            System.out.printf(

                    "Item #%d | %s | Qtd: %d | Subtotal: %.2f%n",

                    v.getId(),

                    v.getProduto().getNome(),

                    v.getQuantidade(),

                    sub

            );

        }

        System.out.printf("Total geral: %.2f%n", total);

    }



    public static void limparCarrinho() {

        List<Venda> vendas = VendasService.getInstance().getVendas();

        if (vendas.isEmpty()) {

            System.out.println("Não há itens para remover.");

            return;

        }

        vendas.clear();

        System.out.println("Carrinho esvaziado. Quando quiser, pode montar um novo pedido.");

    }



    public static void informacaoPrecos() {

        System.out.println("Os preços estão no catálogo. Digite estoque ou produtos para ver id, nome e valor.");

        System.out.println("Na hora de comprar, informe o id do produto e a quantidade quando o bot pedir.");

    }



    public static void entrega() {

        System.out.println("Entregamos em até 5 dias úteis após confirmação do pagamento.");

        System.out.println("Frete grátis em compras acima de R$ 150,00 (promoções podem mudar regras).");

        System.out.println("Enviamos código de rastreio por e-mail ou mensagem após despacho.");

    }



    public static void pagamento() {

        System.out.println("Formas de pagamento: Pix (aprov. imediata), cartão em até 12x e boleto (1 dia útil).");

        System.out.println("Enviamos instruções de pagamento após você fechar o pedido no carrinho.");

    }



    public static void politicaTrocas() {

        System.out.println("Trocas em até 30 dias com etiqueta e nota, desde que sem uso e na embalagem original.");

        System.out.println("Devolução por defeito: abra um chamado com foto do problema em até 7 dias da entrega.");

    }



    public static void promocoes() {

        System.out.println("Sem campanha ativa no momento, mas o frete grátis acima de R$ 150 continua valendo.");

        System.out.println("Consulte o estoque: às vezes destacamos peças em liquidação no nome ou tipo.");

    }



    public static void contato() {

        System.out.println("Atendimento: seg. a sáb., 9h–18h (horário de Brasília).");

        System.out.println("WhatsApp: (11) 90000-0000 | E-mail: contato@loja-exemplo.com");
    }



    public static void agradecimento() {

        System.out.println("Eu que agradeço! Se precisar de mais alguma coisa, é só falar.");

    }



    public static void despedida() {

        System.out.println("Até logo! Volte sempre que quiser cotar ou comprar conosco.");

    }



    public static void sobreLoja() {

        System.out.println("Somos um chatbot de vendas focado em roupas e acessórios, com atendimento simples por texto.");

        System.out.println("Use ajuda para ver todos os comandos ou estoque para ver o catálogo.");

    }



    public static void saudacao(){

        System.out.println("Olá como posso ajuda-lo");

    }



    public static void bomDia(){

        System.out.println("Bom dia como posso ajuda-lo");

    }



    public static void boaTarde(){

        System.out.println("Boa tarde como posso ajuda-lo");

    }



    public static void boaNoite(){

        System.out.println("Boa noite como posso ajuda-lo");

    }



    public static void estoque(){

        System.out.println("Exibindo estoque");



        System.out.println("=====Produtos Disponíveis=====");

        EstoqueService.getInstance()

                .getProdutos()

                .forEach(System.out::println);

        System.out.println("Digite o id ou o nome do produto para comprar, ou outro comando.");

        contextoAposListagemCatalogo = true;

    }



    public static void compra() {

        System.out.println("===== Como comprar =====");

        System.out.println("1) Veja os produtos digitando estoque ou catálogo.");

        System.out.println("2) Escreva o que deseja pelo tipo: camisa, calça, bermuda, boné, tênis, jaqueta, meia, vestido, cachecol ou luvas.");

        System.out.println("3) Informe o id e a quantidade quando o sistema pedir. Use carrinho para ver o resumo.");

    }



    public static void camisa(){

        comprarPorPalavraNoNome("camisa", "camisa");

    }



    public static void calca(){

        comprarPorPalavraNoNome("calça", "calça");

    }



    public static void bermuda(){

        comprarPorPalavraNoNome("bermuda", "bermuda");

    }



    public static void bone(){

        comprarPorPalavraNoNome("boné", "boné");

    }



    public static void tenis(){

        comprarPorPalavraNoNome("tênis", "tênis");

    }



    public static void jaqueta(){

        comprarPorPalavraNoNome("jaqueta", "jaqueta");

    }



    public static void meia(){

        comprarPorPalavraNoNome("meia", "meia");

    }



    public static void vestido(){

        comprarPorPalavraNoNome("vestido", "vestido");

    }



    public static void cachecol(){

        comprarPorPalavraNoNome("cachecol", "cachecol");

    }



    public static void luvas(){

        comprarPorPalavraNoNome("luvas", "luvas");

    }



    private static void comprarPorPalavraNoNome(String palavraChave, String tituloCategoria) {

        String chave = palavraChave.toLowerCase();

        List<Produto> candidatos = EstoqueService.getInstance()

                .getProdutos()

                .stream()

                .filter(produto -> produto.getNome().toLowerCase().contains(chave))

                .collect(Collectors.toList());

        comprarApartirLista(candidatos, tituloCategoria, Optional.of(chave));

    }



    private static void comprarApartirLista(List<Produto> candidatos, String titulo, Optional<String> nomeDeveConter) {

        if (candidatos.isEmpty()) {

            return;

        }

        boolean concluido = false;

        if (candidatos.size() == 1) {

            Produto unico = candidatos.get(0);

            do {

                System.out.println("Produto: " + unico);

                Scanner scanner = new Scanner(System.in);

                try {

                    System.out.println("Quantidade");

                    int quantidade = Integer.parseInt(scanner.nextLine());

                    if (quantidade < 1) {

                        System.out.println("Quantidade inválida.");

                        continue;

                    }

                    VendasService.getInstance()

                            .getVendas().add(new Venda(unico, quantidade));

                    System.out.println("Venda adicionada com sucesso!");

                    concluido = true;

                } catch (NumberFormatException e) {

                    System.out.println("Digite um número válido!");

                }

            } while (!concluido);

            encerrarContextoCatalogo();

            return;

        }

        do {

            System.out.println("Qual " + titulo + " deseja?");

            candidatos.forEach(System.out::println);



            Scanner scanner = new Scanner(System.in);



            try {

                System.out.println("Digite o id do produto");

                int opcao = Integer.parseInt(scanner.nextLine());

                System.out.println("Quantidade");

                int quantidade = Integer.parseInt(scanner.nextLine());

                Optional<Produto> produtoOpt = candidatos.stream()

                        .filter(produto -> produto.getId() == opcao)

                        .findFirst();

                if (!produtoOpt.isPresent()) {

                    System.out.println("Id não encontrado entre as opções listadas.");

                } else if (nomeDeveConter.isPresent()

                        && !produtoOpt.get().getNome().toLowerCase().contains(nomeDeveConter.get())) {

                    System.out.println("Este id não corresponde à categoria escolhida.");

                } else {

                    VendasService.getInstance()

                            .getVendas().add(new Venda(produtoOpt.get(), quantidade));

                    System.out.println("Venda adicionada com sucesso!");

                    concluido = true;

                }

            } catch (NumberFormatException e) {

                System.out.println("Digite um id válido!");

            }



        } while (!concluido);

        encerrarContextoCatalogo();

    }



}


