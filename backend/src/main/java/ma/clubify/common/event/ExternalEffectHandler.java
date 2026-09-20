package ma.clubify.common.event;

/** Abonné à un effet externe déposé dans l'outbox. */
public interface ExternalEffectHandler {

    boolean accepte(String type);

    void produire(ExternalEffect effet);
}
