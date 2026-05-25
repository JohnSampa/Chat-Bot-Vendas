package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.PalavraChave;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;
import unifan.chat_bot_vendas.exceptions.BusinessException;
import unifan.chat_bot_vendas.repositories.IntencaoRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class IntencaoService {

    @Autowired
    private IntencaoRepository intencaoRepository;

    public Intencao detectarIntencao(String msg) {
        String msgNormalizada = normalizar(msg);

        return intencaoRepository.findByAtivaTrue()
                .stream()
                .map(intencao -> {
                    long score = intencao.getPalavrasChaves()
                            .stream()
                            .filter(p -> msgNormalizada
                                    .contains(normalizar(p.getPalavraChave())))
                            .mapToLong(p -> p.getPalavraChave().length())
                            .sum();
                    return Map.entry(intencao, score);
                })
                .filter(entry -> entry.getValue() > 0)
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String normalizar(String texto) {
        return texto.toLowerCase()
                .trim()
                .replaceAll("[^a-záéíóúãõâêô\\s]", "");
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

    public Intencao adicionarPalavrasChave(Long intencaoId, List<PalavraChave> palavrasChaves) {
        Intencao intencao = intencaoRepository.findById(intencaoId)
                .orElseThrow(() -> new BusinessException("Intencao nao encontrada"));

        if (intencao.getPalavrasChaves() == null) {
            intencao.setPalavrasChaves(new ArrayList<>());
        }

        palavrasChaves.forEach(palavraChave -> {
            palavraChave.setId(null);
            palavraChave.setIntencao(intencao);
            intencao.getPalavrasChaves().add(palavraChave);
        });

        return intencaoRepository.save(intencao);
    }
}
