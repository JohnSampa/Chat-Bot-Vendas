package unifan.chat_bot_vendas.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vendas_tb")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    private Integer quantidade;

    private String tamanho;

    private Double total;

    private Long userid;

    private String cpfCliente;

    private String formaPagamento;

    private String dadosPagamento;

    private String status = "FINALIZADO";

    public Double getTotal() {
        if (produto == null || produto.getPreco() == null || quantidade == null) {
            return 0.0;
        }

        return total = produto.getPreco() * quantidade;
    }

}
