package ma.clubify.support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Capture les journaux techniques d'un paquet, le temps d'un test.
 *
 * <p>Permet de vérifier qu'aucune donnée personnelle n'y figure (CLAUDE.md §7,
 * critère C15) sans avoir à exposer les journaux dans l'application.
 */
public final class LogCapture implements AutoCloseable {

    private final Logger journal;
    private final ListAppender<ILoggingEvent> collecteur = new ListAppender<>();
    private final Level niveauInitial;

    private LogCapture(String paquet) {
        this.journal = (Logger) LoggerFactory.getLogger(paquet);
        this.niveauInitial = journal.getLevel();
        journal.setLevel(Level.TRACE);
        collecteur.start();
        journal.addAppender(collecteur);
    }

    public static LogCapture sur(String paquet) {
        return new LogCapture(paquet);
    }

    public List<ILoggingEvent> lignes() {
        return List.copyOf(collecteur.list);
    }

    public String texte() {
        return collecteur.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public void close() {
        journal.detachAppender(collecteur);
        journal.setLevel(niveauInitial);
        collecteur.stop();
    }
}
