package bot.config;

import bot.engine.LevenshteinEngine;
import bot.engine.SimilarityEngine;
import bot.engine.TfIdfEngine;
import bot.listener.MessageListener;
import bot.service.DecisionService;
import bot.service.MetricsService;
import bot.service.similarity.SimilarityService;
import bot.service.TemplateService;
import bot.service.ThreadService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotConfig {

    public static void start() throws Exception {
        String token = System.getenv("DISCORD_TOKEN");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("DISCORD_TOKEN no configurado");
        }

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS
                )
                .build();

        jda.awaitReady();

        TemplateService templateService = new TemplateService();
        SimilarityEngine engine = new LevenshteinEngine();
        SimilarityService similarityService = new SimilarityService(engine, 0.95, 0.8);
        MetricsService metricsService = new MetricsService();
        DecisionService decisionService = new DecisionService(metricsService);
        ThreadService threadService =
                new ThreadService(templateService,
                        similarityService,
                        decisionService,
                        metricsService);

        jda.addEventListener(
                new MessageListener(threadService, decisionService)
        );

        jda.updateCommands().addCommands(
                Commands.slash("stats", "Muestra métricas del bot")
        ).queue();

        System.out.println("Bot conectado correctamente.");
    }
}