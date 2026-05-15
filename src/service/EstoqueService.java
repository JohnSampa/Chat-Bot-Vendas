package service;

import domain.Produto;

import java.util.ArrayList;
import java.util.List;

public class EstoqueService {

    private static final List<Produto> produtos = new ArrayList<>();

    private final static EstoqueService instance = new EstoqueService();

    private EstoqueService(){
        produtos.add(new Produto(
                1L,
                "Camisa",
                40.00,
                "Personalizada",
                "M"
        ));

        produtos.add(new Produto(
                2L,
                "Camisa do Corinthians",
                40.00,
                "Personalizada",
                "G"
        ));

        produtos.add(new Produto(
                3L,
                "Calça Jeans",
                120.00,
                "Inferior",
                "42"
        ));

        produtos.add(new Produto(
                4L,
                "Bermuda Sarja",
                75.00,
                "Inferior",
                "M"
        ));

        produtos.add(new Produto(
                5L,
                "Boné Bordado",
                35.00,
                "Acessório",
                "Único"
        ));

        produtos.add(new Produto(
                6L,
                "Tênis Casual",
                199.90,
                "Calçado",
                "41"
        ));

        produtos.add(new Produto(
                7L,
                "Jaqueta Corta-Vento",
                160.00,
                "Superior",
                "G"
        ));

        produtos.add(new Produto(
                8L,
                "Meia Cano Alto (par)",
                25.00,
                "Íntima",
                "Único"
        ));

        produtos.add(new Produto(
                9L,
                "Vestido Floral",
                145.00,
                "Vestido",
                "P"
        ));

        produtos.add(new Produto(
                10L,
                "Cachecol de Lã",
                45.00,
                "Acessório",
                "Único"
        ));

        produtos.add(new Produto(
                11L,
                "Luvas Térmicas",
                32.00,
                "Acessório",
                "M"
        ));
    }

    public static EstoqueService getInstance(){
        return instance;
    }

    public List<Produto> getProdutos(){
        return produtos;
    }

}
