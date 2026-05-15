package domain;

public class Produto {

    private Long  id;

    private String nome;

    private Double preco;

    private String tipo;

    private String tamanho;

    public Produto(Long id, String nome, Double preco, String tipo, String tamanho) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.tipo = tipo;
        this.tamanho = tamanho;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTamanho() {
        return tamanho;
    }

    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Id: ").append(id).append("|");
        builder.append("Nome: ").append(nome).append("|");
        builder.append("Preço: ").append(preco).append("|");
        builder.append("Tipo: ").append(tipo).append("|");
        builder.append("Tamanho: ").append(tamanho).append("|");

        return builder.toString();
    }
}
