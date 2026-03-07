package forumbot.config;

import common.service.MetricsService;
import forumbot.command.ConfigCommand;
import common.engine.*;
import forumbot.listener.MessageListener;
import forumbot.listener.ThreadLifecycleListener;
import forumbot.orchestrator.ForumPostOrchestrator;
import forumbot.orchestrator.MessageOrchestrator;
import forumbot.repository.ServerSettingsRepository;
import forumbot.service.*;
import forumbot.service.similarity.SimilarityService;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotConfig {

    public static void start(){
        Dotenv dotenv = Dotenv.configure()
                .directory("config.env")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        String token = dotenv.get("DISCORD_TOKEN");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("DISCORD_TOKEN no configurado");
        }

        try {
            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS
                    )
                    .build();

            jda.awaitReady();

            // ---------- Core Engines ----------
            SimilarityEngine levenshtein = new LevenshteinEngine();
            SimilarityEngine jaccard = new JaccardEngine();
            SimilarityEngine containment = new ContainmentEngine();

            SimilarityEngine hybridEngine =
                    new HybridSimilarityEngine(
                            levenshtein,
                            jaccard,
                            containment
                    );

            SimilarityService similarityService = new SimilarityService(
                    hybridEngine,
                    0.92,  // exactThreshold
                    0.65   // similarThreshold
            );

            // ---------- Services ----------
            ServerSettingsRepository settingsRepository = new ServerSettingsRepository();
            ServerSettingsService settingsService = new ServerSettingsService(settingsRepository);
            TemplateService templateService = new TemplateService();
            ValidationService validationService = new ValidationService();
            MetricsService metricsService = new MetricsService();
            LogService logService = new LogService(jda, settingsService);
            DecisionService decisionService = new DecisionService(metricsService, logService);
            FeedbackService feedbackService = new FeedbackService();
            TagService tagService = new TagService(settingsService);

            SpamService spamService = new SpamService(settingsService);

            ThreadService threadService =
                    new ThreadService(
                            similarityService,
                            decisionService,
                            metricsService
                    );

            ThreadIndexService threadIndexService = new ThreadIndexService();

            ForumDuplicateService duplicateService = new ForumDuplicateService(decisionService, logService);

            ForumPostOrchestrator forumPostOrchestrator = new ForumPostOrchestrator(
                    similarityService,
                    duplicateService,
                    metricsService,
                    spamService,
                    feedbackService,
                    threadIndexService,
                    logService,
                    tagService,
                    validationService
            );

            MessageOrchestrator orchestrator =
                    new MessageOrchestrator(
                            templateService,
                            spamService,
                            feedbackService,
                            threadService
                    );

            // ---------- Commands ----------
            ConfigCommand configCommand = new ConfigCommand(settingsService);

            // ---------- Listener ----------

            jda.addEventListener(
                    new MessageListener(orchestrator, decisionService, forumPostOrchestrator),
                    configCommand,
                    new ThreadLifecycleListener(threadIndexService)
            );

            // ---------- Slash Commands ----------

            jda.updateCommands().addCommands(
                    Commands.slash("stats", "Muestra métricas del bot"),
                    Commands.slash("config", "Configuración del bot")
                            .addSubcommands(
                                    new SubcommandData("cooldown", "Tiempo mínimo entre publicaciones")
                                            .addOption(OptionType.INTEGER, "segundos", "Cooldown en segundos", true),
                                    new SubcommandData("logchannel", "Canal donde se registran los nuevos foros")
                                            .addOption(OptionType.CHANNEL, "canal", "Canal de texto", true),
                                    new SubcommandData("defaulttag", "Tag que se aplica al crear una publicación")
                                            .addOption(OptionType.STRING, "tag", "Nombre del tag", true)
                            )
            ).queue();

            jda.getGuilds().forEach(guild ->
                    guild.getForumChannels().forEach(forum ->
                            forum.getThreadChannels().forEach(threadIndexService::indexThread)
                    )
            );

            System.out.println("Bot conectado correctamente.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bot interrumpido durante el inicio", e);
        }
    }
}