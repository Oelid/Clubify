package ma.clubify.common.file;

/**
 * Support de stockage des fichiers.
 *
 * <p>Une interface, parce que le lieu d'hébergement n'est pas tranché
 * (décision 0018). F01 livre le disque et la mémoire ; une implémentation
 * compatible S3 s'ajoutera sans toucher aux appelants (décision 0029, M6).
 *
 * <p>Le contenu qui traverse cette frontière est <strong>déjà chiffré</strong> :
 * le support ne voit jamais le clair (SEC-03).
 */
public interface FileStorage {

    /** Range le contenu et retourne la clé qui permettra de le relire. */
    String ranger(String cle, byte[] contenuChiffre);

    byte[] lire(String cle);

    void supprimer(String cle);

    /** Le contenu tel qu'il est conservé, pour que les tests le vérifient. */
    byte[] lireBrut(String cle);
}
