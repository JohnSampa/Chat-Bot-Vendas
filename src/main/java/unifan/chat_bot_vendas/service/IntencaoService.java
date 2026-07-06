package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.PalavraChave;
import unifan.chat_bot_vendas.domain.enums.Setor;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;
import unifan.chat_bot_vendas.exceptions.BusinessException;
import unifan.chat_bot_vendas.repositories.IntencaoRepository;
import unifan.chat_bot_vendas.utils.TextoUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class IntencaoService {

    @Autowired
    private IntencaoRepository intencaoRepository;

    public Intencao detectarIntencao(String msg) {
        String msgNormalizada = TextoUtils.normalizar(msg);
        List<Intencao> intencoesAtivas = intencaoRepository.findByAtivaTrue();
        Setor setorDominante = detectarSetorDominante(msgNormalizada, intencoesAtivas);

        return intencoesAtivas
                .stream()
                .filter(intencao -> pertenceAoSetor(intencao, setorDominante))
                .map(intencao -> {
                    long score = palavrasDaIntencao(intencao)
                            .stream()
                            .filter(palavra -> palavraPertenceAoSetor(palavra, setorDominante))
                            .filter(palavra -> TextoUtils.contemTermo(msgNormalizada, palavra.getPalavraChave()))
                            .mapToLong(this::calcularPesoPalavra)
                            .sum();
                    return Map.entry(intencao, score);
                })
                .filter(entry -> entry.getValue() > 0)
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Setor detectarSetorDominante(String msgNormalizada, List<Intencao> intencoes) {
        Map<Setor, Long> pesosPorSetor = new EnumMap<>(Setor.class);

        intencoes.stream()
                .flatMap(intencao -> palavrasDaIntencao(intencao).stream())
                .filter(palavra -> TextoUtils.contemTermo(msgNormalizada, palavra.getPalavraChave()))
                .forEach(palavra -> pesosPorSetor.merge(setorDaPalavra(palavra), calcularPesoPalavra(palavra), Long::sum));

        return pesosPorSetor.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != Setor.GERAL)
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(Setor.GERAL);
    }

    private List<PalavraChave> palavrasDaIntencao(Intencao intencao) {
        if (intencao.getPalavrasChaves() == null) {
            return List.of();
        }

        return intencao.getPalavrasChaves();
    }

    private boolean pertenceAoSetor(Intencao intencao, Setor setorDominante) {
        if (setorDominante == Setor.GERAL) {
            return true;
        }

        if (intencao.getSetor() == setorDominante) {
            return true;
        }

        return palavrasDaIntencao(intencao)
                .stream()
                .anyMatch(palavra -> setorDaPalavra(palavra) == setorDominante);
    }

    private boolean palavraPertenceAoSetor(PalavraChave palavra, Setor setorDominante) {
        return setorDominante == Setor.GERAL || setorDaPalavra(palavra) == setorDominante;
    }

    private Setor setorDaPalavra(PalavraChave palavra) {
        if (palavra.getSetor() != null) {
            return palavra.getSetor();
        }

        if (palavra.getIntencao() != null && palavra.getIntencao().getSetor() != null) {
            return palavra.getIntencao().getSetor();
        }

        return Setor.GERAL;
    }

    private long calcularPesoPalavra(PalavraChave palavra) {
        int peso = palavra.getPeso() == null || palavra.getPeso() <= 0 ? 1 : palavra.getPeso();
        return (long) TextoUtils.normalizar(palavra.getPalavraChave()).length() * peso;
    }

    public Intencao findByTipoAcao(TipoAcao tipoAcao) {
        return intencaoRepository.findByTipoAcao(tipoAcao);
    }

    public List<Intencao> salveAll(List<Intencao> intencoes) {
        if (intencoes == null || intencoes.isEmpty()) {
            throw new BusinessException("Informe ao menos uma intencao para salvar");
        }

        intencoes.forEach(intencao -> {
            intencao.setId(null);

            if (intencao.getPalavrasChaves() == null) {
                intencao.setPalavrasChaves(new ArrayList<>());
            }

            intencao.getPalavrasChaves().forEach(palavra -> prepararPalavraChave(intencao, palavra));
        });
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
            prepararPalavraChave(intencao, palavraChave);
            intencao.getPalavrasChaves().add(palavraChave);
        });

        return intencaoRepository.save(intencao);
    }

    private void prepararPalavraChave(Intencao intencao, PalavraChave palavraChave) {
        palavraChave.setId(null);
        if (palavraChave.getSetor() == null) {
            palavraChave.setSetor(intencao.getSetor() == null ? Setor.GERAL : intencao.getSetor());
        }
        if (palavraChave.getPeso() == null || palavraChave.getPeso() <= 0) {
            palavraChave.setPeso(1);
        }
        palavraChave.setIntencao(intencao);
    }
}
