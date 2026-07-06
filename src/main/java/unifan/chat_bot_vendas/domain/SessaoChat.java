package unifan.chat_bot_vendas.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import unifan.chat_bot_vendas.domain.enums.EstadoSessao;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class SessaoChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private EstadoSessao estado;

    private LocalDateTime ultimaAtualizacao;

    private Long userid;

    private String cpfCliente;

    private String clientSessionId;

    private LocalDateTime iniciadaEm;

    private boolean ativa = true;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    private Integer quantidade;

    private String tamanho;

    private String tipoProdutoInteresse;

    @Column(columnDefinition = "text")
    private String carrinhoJson;

    @Column(columnDefinition = "text")
    private String itensPendentesJson;

    @Column(columnDefinition = "text")
    private String itemPendenteJson;

    @Column(columnDefinition = "text")
    private String itensTamanhoPendentesJson;

    @Column(columnDefinition = "text")
    private String itemTamanhoPendenteJson;

    private String formaPagamento;

    @Column(columnDefinition = "text")
    private String dadosPagamento;

    @Column(columnDefinition = "text")
    private String mensagemPendenteCpf;

}
