package unifan.chat_bot_vendas.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import unifan.chat_bot_vendas.domain.enums.Setor;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "intencao_tb")
public class Intencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String intencao;

    private String resposta;

    @Enumerated(EnumType.STRING)
    private Setor setor = Setor.VENDAS;

    @Enumerated(EnumType.STRING)
    private TipoAcao tipoAcao;

    private boolean ativa = true;

    @OneToMany(mappedBy = "intencao",cascade = CascadeType.ALL)
    private List<PalavraChave> palavrasChaves;
}
