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

    public Produto buscarProdutoPorId(Long id) {
        if (id == null) {
            return null;
        }

        return produtoRepository.findById(id).orElse(null);
    }

    public Produto buscarProdutoPorIdMensagem(String mensagem, String tipoProdutoInteresse) {
        Produto produto = buscarProdutoPorIdMensagem(mensagem);
        if (produto == null || tipoProdutoInteresse == null || tipoProdutoInteresse.isBlank()) {
            return produto;
        }

        if (normalizarTermoProduto(produto.getNome()).contains(normalizarTermoProduto(tipoProdutoInteresse))) {
            return produto;
        }

        return null;
    }

    public Produto buscarProdutoPorNomeExato(String mensagem, String tipoProdutoInteresse) {
        String mensagemNormalizada = normalizarTermoProduto(mensagem);
        if (mensagemNormalizada.isBlank()) {
            return null;
        }

        return produtosPorTipoInteresse(tipoProdutoInteresse)
                .stream()
                .filter(produto -> normalizarTermoProduto(produto.getNome()).equals(mensagemNormalizada))
                .findFirst()
                .orElse(null);
    }

    public String detectarTipoProdutoInteresse(String mensagem) {
        String mensagemNormalizada = normalizarTermoProduto(mensagem);

        if (mensagemNormalizada.equals("camisa") || mensagemNormalizada.equals("camiseta")) {
            return "camisa";
        }

        if (mensagemNormalizada.equals("regata")) {
            return "regata";
        }

        if (mensagemNormalizada.equals("short") || mensagemNormalizada.equals("shorts")
                || mensagemNormalizada.equals("shot")) {
            return "short";
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
        String mensagemNormalizada = normalizarTermoProduto(mensagem);
        if (mensagemNormalizada.isBlank()) {
            return null;
        }

        if (tipoProdutoInteresse != null && mensagemNormalizada.equals(normalizarTermoProduto(tipoProdutoInteresse))) {
            return null;
        }

        List<Produto> produtos = produtosPorTipoInteresse(tipoProdutoInteresse);

        List<ProdutoScore> scores = produtos
                .stream()
                .map(produto -> new ProdutoScore(produto, calcularScore(mensagemNormalizada, normalizarTermoProduto(produto.getNome()))))
                .filter(produtoScore -> produtoScore.score() <= 3)
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
                .filter(produto1 -> normalizarTermoProduto(mensagem).contains(normalizarTermoProduto(produto1.getNome())))
                .findFirst();

        return produto.orElse(null);
    }

    public Produto buscarProdutoPorNomeParcial(String nomeProduto) {
        String nomeNormalizado = normalizarTermoProduto(nomeProduto);
        List<Produto> produtos = produtoRepository.findAll();
        Optional<Produto> produto = produtos
                .stream()
                .filter(produto1 -> normalizarTermoProduto(produto1.getNome()).contains(nomeNormalizado))
                .findFirst();

        return produto.orElse(null);
    }

    public List<Produto> buscarProdutosPorNomeParcial(String nomeProduto) {
        String nomeNormalizado = normalizarTermoProduto(nomeProduto);
        return produtoRepository.findAll()
                .stream()
                .filter(produto -> normalizarTermoProduto(produto.getNome()).contains(nomeNormalizado))
                .toList();
    }

    public List<Produto> buscarCandidatosPorTermo(String termo, String tipoProdutoInteresse) {
        String termoNormalizado = normalizarTermoProduto(termo);
        if (termoNormalizado.isBlank()) {
            return List.of();
        }

        return produtosPorTipoInteresse(tipoProdutoInteresse)
                .stream()
                .map(produto -> new ProdutoScore(produto, calcularScore(termoNormalizado, normalizarTermoProduto(produto.getNome()))))
                .filter(produtoScore -> produtoScore.score() <= 3)
                .sorted(Comparator.comparingInt(ProdutoScore::score))
                .map(ProdutoScore::produto)
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

    private String normalizarTermoProduto(String texto) {
        String normalizado = normalizar(texto);

        return Arrays.stream(normalizado.split("\\s+"))
                .map(this::normalizarTokenProduto)
                .filter(token -> !token.isBlank())
                .reduce("", (acc, token) -> acc.isBlank() ? token : acc + " " + token);
    }

    private String normalizarTokenProduto(String token) {
        return switch (token) {
            case "camisetas", "camiseta" -> "camiseta";
            case "regatas", "regata" -> "regata";
            case "calcas" -> "calca";
            case "shorts", "shot" -> "short";
            case "sapatos" -> "sapato";
            case "calcados", "calcado", "tenis" -> "sapato";
            case "blusas" -> "blusa";
            default -> removerPluralSimples(token);
        };
    }

    private String removerPluralSimples(String token) {
        if (token.length() > 4 && token.endsWith("s")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
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
