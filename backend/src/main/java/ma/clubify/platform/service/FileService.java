package ma.clubify.platform.service;

import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.exception.BusinessRuleException;
import ma.clubify.common.exception.NotFoundException;
import ma.clubify.common.file.FileStorage;
import ma.clubify.common.model.entity.UuidV7;
import ma.clubify.common.security.EncryptionService;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.common.security.TokenService;
import ma.clubify.platform.model.entity.FileLink;
import ma.clubify.platform.model.entity.StoredFile;
import ma.clubify.platform.repository.FileLinkRepository;
import ma.clubify.platform.repository.StoredFileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Fichiers privés (PLT-05).
 *
 * <p>Trois protections se cumulent : le contenu est chiffré avant d'atteindre le
 * support, il n'est servi que par un lien signé et expirant, et chaque accès
 * laisse une trace (SEC-03, critères C13, C14, C23).
 */
@Service
public class FileService {

    private final StoredFileRepository fichiers;
    private final FileLinkRepository liens;
    private final FileStorage support;
    private final EncryptionService chiffrement;
    private final ClubSettingService reglages;
    private final TokenService jetons;
    private final DomainEvents evenements;
    private final Clock horloge;

    public FileService(StoredFileRepository fichiers, FileLinkRepository liens,
                       FileStorage support, EncryptionService chiffrement,
                       ClubSettingService reglages, TokenService jetons,
                       DomainEvents evenements, Clock horloge) {
        this.fichiers = fichiers;
        this.liens = liens;
        this.support = support;
        this.chiffrement = chiffrement;
        this.reglages = reglages;
        this.jetons = jetons;
        this.evenements = evenements;
        this.horloge = horloge;
    }

    @Transactional
    @PreAuthorize("@perm.a('files.deposer')")
    public StoredFile deposer(String nom, String typeMime, byte[] contenu, String usage) {
        long tailleMaximale = reglages.entier("files.max_size_mb") * 1024L * 1024L;
        if (contenu.length > tailleMaximale) {
            throw new BusinessRuleException("file.size.tooLarge", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        List<String> typesAutorises = reglages.liste("files.allowed_types");
        if (!typesAutorises.contains(typeMime)) {
            throw new BusinessRuleException("file.type.notAllowed",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        UUID clubId = PermissionChecker.requis().clubId();
        UUID id = UuidV7.next();
        String cle = clubId + "/" + id;

        // Le support ne reçoit que du chiffré (critère C13).
        support.ranger(cle, chiffrement.chiffrer(contenu));

        StoredFile fichier = new StoredFile();
        fichier.setId(id);
        fichier.setClubId(clubId);
        fichier.setPurpose(usage);
        fichier.setFilename(nom);
        fichier.setContentType(typeMime);
        fichier.setSizeBytes(contenu.length);
        fichier.setStorageKey(cle);
        fichier.setEncryptionKeyId(chiffrement.identifiantDeCle());
        fichiers.save(fichier);

        evenements.publish(DomainEvent.of(clubId, "file.uploaded", "StoredFile", id));
        return fichier;
    }

    /** Délivre un lien signé et expirant vers le fichier (critère C23). */
    @Transactional
    @PreAuthorize("@perm.a('files.consulter')")
    public Lien creerLien(UUID fichierId) {
        StoredFile fichier = fichiers.findById(fichierId)
                .orElseThrow(() -> new NotFoundException("file.notFound"));

        String jeton = jetons.nouveauJetonOpaque();
        long minutes = reglages.entier("files.link_ttl_minutes");

        FileLink lien = new FileLink();
        lien.setId(UuidV7.next());
        lien.setClubId(fichier.getClubId());
        lien.setFileId(fichier.getId());
        lien.setTokenHash(jetons.empreinte(jeton));
        lien.setExpiresAt(horloge.instant().plus(Duration.ofMinutes(minutes)));
        liens.save(lien);

        return new Lien("/api/v1/files/download/" + jeton, lien.getExpiresAt());
    }

    /**
     * Sert le contenu à partir d'un lien. Sans contexte de club : le jeton du
     * lien fait foi, et il ne porte que sur un fichier d'un seul club.
     */
    @Transactional
    public Contenu servir(String jeton) {
        var lien = liens.parEmpreinte(jetons.empreinte(jeton))
                .orElseThrow(() -> new NotFoundException("file.link.notFound"));

        if (lien.expiresAt().isBefore(horloge.instant())) {
            throw new BusinessRuleException("file.link.expired", HttpStatus.GONE);
        }

        var fichier = fichiers.parIdSansContexte(lien.fileId())
                .orElseThrow(() -> new NotFoundException("file.notFound"));

        evenements.publish(DomainEvent.of(fichier.clubId(), "file.accessed",
                "StoredFile", fichier.id()));

        return new Contenu(fichier.filename(), fichier.contentType(),
                chiffrement.dechiffrer(support.lire(fichier.storageKey())));
    }

    @Transactional
    @PreAuthorize("@perm.a('files.deposer')")
    public void supprimer(UUID fichierId) {
        StoredFile fichier = fichiers.findById(fichierId)
                .orElseThrow(() -> new NotFoundException("file.notFound"));

        // Suppression logique : la ligne reste, le fichier n'est plus servi (C26).
        fichiers.delete(fichier);
        evenements.publish(DomainEvent.of(fichier.getClubId(), "file.deleted",
                "StoredFile", fichierId));
    }

    /** Lit le contenu tel qu'il est conservé : sert au test du chiffrement. */
    public byte[] contenuBrut(String cle) {
        return support.lireBrut(cle);
    }

    public record Lien(String url, java.time.Instant expiresAt) {
    }

    public record Contenu(String filename, String contentType, byte[] octets) {
    }
}
