package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.repositories.ProdutoRepository;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public Produto buscarProdutoPorIdMensagem(String mensagem) {
        String idProduto = mensagem.replaceAll("[^0-9]", "");
        if (idProduto.isBlank()) {
            return null;
        }

        try {
            return produtoRepository.findById(Long.parseLong(idProduto)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Produto buscarProdutoPorIdMensagem(String mensagem, String tipoProdutoInteresse) {
        Produto produto = buscarProdutoPorIdMensagem(mensagem);
        if (produto == null || tipoProdutoInteresse == null || tipoProdutoInteresse.isBlank()) {
            return produto;
        }

        if (normalizar(produto.getNome()).contains(normalizar(tipoProdutoInteresse))) {
            return produto;
        }

        return null;
    }

    public Produto buscarProdutoPorNomeExato(String mensagem, String tipoProdutoInteresse) {
        String mensagemNormalizada = normalizar(mensagem);
        if (mensagemNormalizada.isBlank()) {
            return null;
        }

        return produtosPorTipoInteresse(tipoProdutoInteresse)
                .stream()
                .filter(produto -> normalizar(produto.getNome()).equals(mensagemNormalizada))
                .findFirst()
                .orElse(null);
    }

    public String detectarTipoProdutoInteresse(String mensagem) {
        String mensagemNormalizada = normalizar(mensagem);

        if (mensagemNormalizada.equals("camisa") || mensagemNormalizada.equals("camiseta")) {
            return "camisa";
        }

        if (mensagemNormalizada.equals("calca")) {
            return "calca";
        }

        if (mensagemNormalizada.equals("blusa") || mensagemNormalizada.equals("moletom")) {
            return "blusa";
        }

        if (mensagemNormalizada.equals("sapato") || mensagemNormalizada.equals("calcado")
                || mensagemNormalizada.equals("tenis")) {
            return "sapato";
        }

        return null;
    }

    public Produto buscarProdutoMaisProximoPorMensagem(String mensagem, String tipoProdutoInteresse) {
        String mensagemNormalizada = normalizar(mensagem);
        if (mensagemNormalizada.isBlank()) {
            return null;
        }

        if (tipoProdutoInteresse != null && mensagemNormalizada.equals(normalizar(tipoProdutoInteresse))) {
            return null;
        }

        List<Produto> produtos = produtosPorTipoInteresse(tipoProdutoInteresse);

        List<ProdutoScore> scores = produtos
                .stream()
                .map(produto -> new ProdutoScore(produto, calcularScore(mensagemNormalizada, normalizar(produto.getNome()))))
                .filter(produtoScore -> produtoScore.score() <= 2)
                .sorted(Comparator.comparingInt(ProdutoScore::score))
                .toList();

        if (scores.isEmpty()) {
            return null;
        }

        if (scores.size() > 1 && scores.get(0).score() == scores.get(1).score()) {
            return null;
        }

        return scores.get(0).produto();
    }

    public Produto buscarProdutoPorMensagem(String mensagem) {
        String idProduto = mensagem.replaceAll("[^0-9]", "");
        if (!idProduto.isBlank()) {
            Optional<Produto> produto = produtoRepository.findById(Long.parseLong(idProduto));
            if (produto.isPresent()) {
                return produto.get();
            }
        }

        List<Produto> produtos = produtoRepository.findAll();
        Optional<Produto> produto = produtos
                .stream()
                .filter(produto1 -> normalizar(mensagem).contains(normalizar(produto1.getNome())))
                .findFirst();

        return produto.orElse(null);
    }

    public Produto buscarProdutoPorNomeParcial(String nomeProduto) {
        String nomeNormalizado = normalizar(nomeProduto);
        List<Produto> produtos = produtoRepository.findAll();
        Optional<Produto> produto = produtos
                .stream()
                .filter(produto1 -> normalizar(produto1.getNome()).contains(nomeNormalizado))
                .findFirst();

        return produto.orElse(null);
    }

    public List<Produto> buscarProdutosPorNomeParcial(String nomeProduto) {
        String nomeNormalizado = normalizar(nomeProduto);
        return produtoRepository.findAll()
                .stream()
                .filter(produto -> normalizar(produto.getNome()).contains(nomeNormalizado))
                .toList();
    }

    public List<Produto> buscarProdutos(){
        return produtoRepository.findAll();
    }

    public List<Produto> buscarProdutosPorTipoInteresse(String tipoProdutoInteresse) {
        return produtosPorTipoInteresse(tipoProdutoInteresse);
    }

    public List<Produto> salveProdutos(List<Produto> produtos){
        produtos.forEach(produto -> produtoRepository.save(produto));
        return produtoRepository.findAll();
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.toLowerCase().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s]", "")
                .trim();
    }

    private List<Produto> produtosPorTipoInteresse(String tipoProdutoInteresse) {
        if (tipoProdutoInteresse == null || tipoProdutoInteresse.isBlank()) {
            return produtoRepository.findAll();
        }

        return buscarProdutosPorNomeParcial(tipoProdutoInteresse);
    }

    private int calcularScore(String mensagem, String nomeProduto) {
        if (nomeProduto.contains(mensagem) || mensagem.contains(nomeProduto)) {
            return 0;
        }

        List<String> tokensMensagem = tokens(mensagem);
        List<String> tokensProduto = tokens(nomeProduto);

        if (tokensMensagem.isEmpty() || tokensProduto.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        int score = 0;
        for (String tokenMensagem : tokensMensagem) {
            int menorDistancia = tokensProduto
                    .stream()
                    .mapToInt(tokenProduto -> levenshtein(tokenMensagem, tokenProduto))
                    .min()
                    .orElse(Integer.MAX_VALUE);

            if (menorDistancia > 2) {
                return Integer.MAX_VALUE;
            }
            score += menorDistancia;
        }

        return score;
    }

    private List<String> tokens(String texto) {
        return Arrays.stream(texto.split("\\s+"))
                .filter(token -> token.length() >= 3)
                .toList();
    }

    private int levenshtein(String texto1, String texto2) {
        int[][] distancia = new int[texto1.length() + 1][texto2.length() + 1];

        for (int i = 0; i <= texto1.length(); i++) {
            distancia[i][0] = i;
        }

        for (int j = 0; j <= texto2.length(); j++) {
            distancia[0][j] = j;
        }

        for (int i = 1; i <= texto1.length(); i++) {
            for (int j = 1; j <= texto2.length(); j++) {
                int custo = texto1.charAt(i - 1) == texto2.charAt(j - 1) ? 0 : 1;
                distancia[i][j] = Math.min(
                        Math.min(distancia[i - 1][j] + 1, distancia[i][j - 1] + 1),
                        distancia[i - 1][j - 1] + custo
                );
            }
        }

        return distancia[texto1.length()][texto2.length()];
    }

    private record ProdutoScore(Produto produto, int score) {
    }

}
