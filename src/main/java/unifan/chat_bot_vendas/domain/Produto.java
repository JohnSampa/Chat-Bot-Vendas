package unifan.chat_bot_vendas.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import unifan.chat_bot_vendas.domain.enums.Tamanho;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "produto_tb")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    private String nome;

    private Double preco;

    @Enumerated(EnumType.STRING)
    private Tamanho tamanho;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Id: ").append(id).append("|");
        builder.append("Nome: ").append(nome).append("|");
        builder.append("Preço: ").append(preco).append("|");
        builder.append("Tamanho: ").append(tamanho).append("|");

        return builder.toString();
    }
}
