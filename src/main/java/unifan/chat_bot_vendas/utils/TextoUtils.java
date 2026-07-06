package unifan.chat_bot_vendas.utils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

public final class TextoUtils {

    private TextoUtils() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto.toLowerCase().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static boolean contemTermo(String textoNormalizado, String termo) {
        String termoNormalizado = normalizar(termo);
        if (textoNormalizado == null || textoNormalizado.isBlank() || termoNormalizado.isBlank()) {
            return false;
        }

        return textoNormalizado.matches(".*\\b" + java.util.regex.Pattern.quote(termoNormalizado) + "\\b.*");
    }

    public static List<String> tokens(String texto) {
        return Arrays.stream(normalizar(texto).split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    public static String somenteDigitos(String texto) {
        if (texto == null) {
            return "";
        }

        return texto.replaceAll("\\D", "");
    }
}
