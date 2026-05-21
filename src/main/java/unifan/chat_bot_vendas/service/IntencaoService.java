package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;
import unifan.chat_bot_vendas.repositories.IntencaoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class IntencaoService {

    @Autowired
    private IntencaoRepository intencaoRepository;

    public Intencao detectarIntencao(String msg) {

        return intencaoRepository.findAll()
                .stream()
                .filter(intencao -> intencao.getPalavrasChaves()
                        .stream()
                        .anyMatch(palavraChave -> msg.contains(palavraChave.getPalavraChave())
                        )
                ).findFirst()
                .orElse(null);

    }

    public Intencao findByTipoAcao(TipoAcao tipoAcao) {
        return intencaoRepository.findByTipoAcao(tipoAcao);
    }

    public List<Intencao> salveAll(List<Intencao> intencoes) {

        intencoes.forEach(intencao -> intencao
                .getPalavrasChaves()
                .forEach(palavra -> palavra.setIntencao(intencao))
        );

        return intencaoRepository.saveAll(intencoes);
    }

    public List<Intencao> getIntencao() {
        return intencaoRepository.findAll();
    }
}
