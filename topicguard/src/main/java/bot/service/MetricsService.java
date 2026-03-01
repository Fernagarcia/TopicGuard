package bot.service;

import java.util.concurrent.atomic.AtomicLong;

public class MetricsService {

    private final AtomicLong messagesProcessed = new AtomicLong();
    private final AtomicLong threadsCreated = new AtomicLong();
    private final AtomicLong redirectedExact = new AtomicLong();
    private final AtomicLong confirmationRequested = new AtomicLong();
    private final AtomicLong confirmationAccepted = new AtomicLong();
    private final AtomicLong confirmationRejected = new AtomicLong();

    // 🔹 Incrementadores

    public void incrementMessagesProcessed() {
        messagesProcessed.incrementAndGet();
    }

    public void incrementThreadsCreated() {
        threadsCreated.incrementAndGet();
    }

    public void incrementRedirectedExact() {
        redirectedExact.incrementAndGet();
    }

    public void incrementConfirmationRequested() {
        confirmationRequested.incrementAndGet();
    }

    public void incrementConfirmationAccepted() {
        confirmationAccepted.incrementAndGet();
    }

    public void incrementConfirmationRejected() {
        confirmationRejected.incrementAndGet();
    }

    // 🔹 Getters

    public long getMessagesProcessed() {
        return messagesProcessed.get();
    }

    public long getThreadsCreated() {
        return threadsCreated.get();
    }

    public long getRedirectedExact() {
        return redirectedExact.get();
    }

    public long getConfirmationRequested() {
        return confirmationRequested.get();
    }

    public long getConfirmationAccepted() {
        return confirmationAccepted.get();
    }

    public long getConfirmationRejected() {
        return confirmationRejected.get();
    }

    public double getConfirmationAcceptRate() {
        long requested = confirmationRequested.get();
        if (requested == 0) return 0.0;
        return (double) confirmationAccepted.get() / requested;
    }
}