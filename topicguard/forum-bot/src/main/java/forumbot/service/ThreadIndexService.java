package forumbot.service;

import common.util.TokenizerUtil;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadIndexService {

    public ThreadIndexService() {
    }

    private final Map<String, Set<ThreadChannel>> index = new ConcurrentHashMap<>();

    public void removeThread(ThreadChannel thread) {
        for (String token : TokenizerUtil.tokenize(thread.getName())) {
            Set<ThreadChannel> threads = index.get(token);
            if (threads != null) {
                threads.remove(thread);
                if (threads.isEmpty()) {
                    index.remove(token);
                }
            }
        }
    }

    public void indexThread(ThreadChannel thread) {
        for (String token : TokenizerUtil.tokenize(thread.getName())) {
            index.computeIfAbsent(token, k -> ConcurrentHashMap.newKeySet())
                    .add(thread);
        }
    }

    public List<ThreadChannel> findCandidates(String title) {
        Set<ThreadChannel> candidates = new HashSet<>();
        for (String token : TokenizerUtil.tokenize(title)) {
            Set<ThreadChannel> threads = index.get(token);
            if (threads != null) candidates.addAll(threads);
        }
        return new ArrayList<>(candidates);
    }
}