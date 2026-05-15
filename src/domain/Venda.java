package domain;

public class Venda {

    private Long id;

    private Produto produto;

    private Integer quantidade;

    private static Long contId = 1L;


    public Venda(Produto produto, Integer quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.id = contId;
        contId = contId++;
    }

    public Double getTotal() {
        return produto.getPreco() * quantidade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }
}
