import domain.ProductIntention;
import service.BotCommands;
import service.EstoqueService;
import service.IntentionService;

import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        Scanner input = new Scanner(System.in);

        IntentionService intentionService = IntentionService.getInstance();

        System.out.println("====Bem vindo ao ChatBot de Vendas====");
        System.out.println("Como posso ajuda-lo");

        while (true) {
            String command = input.nextLine();
            command = command.toLowerCase();

            if (command.equals("exit") || command.equals("quit") || command.equals("sair"))
                break;

            if (command.isBlank()) {
                System.out.println("Mensagem invalida!");
                continue;
            }

            String finalCommand = command;

            if (BotCommands.tentarCompraEmContextoCatalogo(finalCommand)) {
                System.out.println("Posso te ajudar em algo mais");
                continue;
            }

            if (BotCommands.consultaCatalogoComReferenciaProduto(finalCommand)) {
                System.out.println("Posso te ajudar em algo mais");
                continue;
            }

            AtomicBoolean compraDireta = new AtomicBoolean(false);
            EstoqueService.getInstance()
                    .getProdutos()
                    .stream()
                    .filter(produto -> finalCommand
                            .contains(produto.getNome().toLowerCase())
                    )
                    .findFirst().flatMap(produto -> intentionService.getIntentions()
                            .stream()
                            .filter(intention -> intention instanceof ProductIntention)
                            .filter(intention -> intention.getIntentions()
                                    .contains(produto.getNome().toLowerCase())
                            ).findFirst()).ifPresent(intention -> {
                        intention.getRunnable().run();
                        compraDireta.set(true);
                    });

            if (compraDireta.get()) {
                compraDireta.set(false);
                System.out.println("Posso te ajudar em algo mais");
                continue;
            }

            intentionService.getIntentions()
                    .stream()
                    .filter(intention ->
                            intention.getIntentions()
                                    .stream()
                                    .anyMatch(finalCommand::contains)
                    ).findFirst()
                    .ifPresent(intention -> intention.getRunnable().run());
        }
    }
}
