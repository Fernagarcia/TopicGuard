package bot.config;

import bot.listener.MessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
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
                        GatewayIntent.MESSAGE_CONTENT
                )
                .addEventListeners(new MessageListener())
                .build();

        jda.awaitReady();
        System.out.println("Bot conectado correctamente.");
    }
}