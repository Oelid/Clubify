package ma.clubify.common.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stockage sur disque : développement, et exploitation tant que S3 n'est pas choisi. */
@Component
@ConditionalOnProperty(name = "clubify.files.storage", havingValue = "disk", matchIfMissing = true)
public class DiskFileStorage implements FileStorage {

    private final Path racine;

    public DiskFileStorage(@Value("${clubify.files.disk-root:./var/files}") String racine) {
        this.racine = Path.of(racine);
    }

    @Override
    public String ranger(String cle, byte[] contenuChiffre) {
        try {
            Path cible = racine.resolve(cle);
            Files.createDirectories(cible.getParent());
            Files.write(cible, contenuChiffre);
            return cle;
        } catch (IOException echec) {
            throw new UncheckedIOException("Écriture du fichier impossible.", echec);
        }
    }

    @Override
    public byte[] lire(String cle) {
        return lireBrut(cle);
    }

    @Override
    public byte[] lireBrut(String cle) {
        try {
            return Files.readAllBytes(racine.resolve(cle));
        } catch (IOException echec) {
            throw new UncheckedIOException("Lecture du fichier impossible.", echec);
        }
    }

    @Override
    public void supprimer(String cle) {
        try {
            Files.deleteIfExists(racine.resolve(cle));
        } catch (IOException echec) {
            throw new UncheckedIOException("Suppression du fichier impossible.", echec);
        }
    }
}
