package ma.clubify.common.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage en mémoire, pour les tests : ils n'ont pas à laisser de fichiers
 * derrière eux, et le même code de service les traverse.
 */
@Component
@ConditionalOnProperty(name = "clubify.files.storage", havingValue = "memory")
public class MemoryFileStorage implements FileStorage {

    private final Map<String, byte[]> contenus = new ConcurrentHashMap<>();

    @Override
    public String ranger(String cle, byte[] contenuChiffre) {
        contenus.put(cle, contenuChiffre);
        return cle;
    }

    @Override
    public byte[] lire(String cle) {
        return lireBrut(cle);
    }

    @Override
    public byte[] lireBrut(String cle) {
        byte[] contenu = contenus.get(cle);
        if (contenu == null) {
            throw new IllegalStateException("Aucun contenu pour la clé " + cle);
        }
        return contenu;
    }

    @Override
    public void supprimer(String cle) {
        contenus.remove(cle);
    }
}
