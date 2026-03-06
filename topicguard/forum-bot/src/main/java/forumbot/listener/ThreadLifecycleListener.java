package forumbot.listener;

import forumbot.service.ThreadIndexService;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateArchivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ThreadLifecycleListener extends ListenerAdapter {

    private final ThreadIndexService threadIndexService;

    public ThreadLifecycleListener(ThreadIndexService threadIndexService) {
        this.threadIndexService = threadIndexService;
    }

    @Override
    public void onChannelUpdateArchived(ChannelUpdateArchivedEvent event) {
        if (!(event.getChannel() instanceof ThreadChannel thread)) return;

        if (Boolean.TRUE.equals(event.getNewValue())) {
            threadIndexService.removeThread(thread);
        } else {
            threadIndexService.indexThread(thread);
        }
    }
}