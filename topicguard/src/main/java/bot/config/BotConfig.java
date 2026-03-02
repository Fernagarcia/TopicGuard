package bot.config;

import bot.engine.LevenshteinEngine;
import bot.engine.SimilarityEngine;
import bot.listener.MessageListener;
import bot.orchestrator.MessageOrchestrator;
import bot.service.*;
import bot.service.similarity.SimilarityService;
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

        // ---------- Core Engines ----------
        SimilarityEngine engine = new LevenshteinEngine();
        SimilarityService similarityService =
                new SimilarityService(engine, 0.95, 0.8);

        // ---------- Services ----------
        TemplateService templateService = new TemplateService();
        MetricsService metricsService = new MetricsService();
        DecisionService decisionService = new DecisionService(metricsService);
        SpamService spamService = new SpamService();
        FeedbackService feedbackService = new FeedbackService();

        ThreadService threadService =
                new ThreadService(
                        similarityService,
                        decisionService,
                        metricsService
                );

        MessageOrchestrator orchestrator =
                new MessageOrchestrator(
                        templateService,
                        spamService,
                        feedbackService,
                        threadService
                );

        // ---------- Listener ----------
        jda.addEventListener(
                new MessageListener(orchestrator, decisionService)
        );

        // ---------- Slash Commands ----------
        jda.updateCommands().addCommands(
                Commands.slash("stats", "Muestra métricas del bot")
        ).queue();

        System.out.println("Bot conectado correctamente.");
    }
}