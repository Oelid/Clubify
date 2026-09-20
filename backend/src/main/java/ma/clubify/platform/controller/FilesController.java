package ma.clubify.platform.controller;

import ma.clubify.generated.api.FilesApi;
import ma.clubify.generated.model.FileLink;
import ma.clubify.generated.model.FilePurpose;
import ma.clubify.generated.model.StoredFile;
import ma.clubify.platform.service.FileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.util.UUID;

/** Fichiers privés : dépôt, lien temporaire, téléchargement, suppression. */
@RestController
public class FilesController implements FilesApi {

    private final FileService fichiers;

    public FilesController(FileService fichiers) {
        this.fichiers = fichiers;
    }

    @Override
    @PreAuthorize("@perm.a('files.deposer')")
    public ResponseEntity<StoredFile> uploadFile(MultipartFile file, FilePurpose purpose) {
        try {
            ma.clubify.platform.model.entity.StoredFile depose = fichiers.deposer(
                    file.getOriginalFilename(), file.getContentType(), file.getBytes(),
                    purpose.getValue());
            return ResponseEntity.status(HttpStatus.CREATED).body(versContrat(depose));
        } catch (IOException echec) {
            throw new UncheckedIOException("Lecture du fichier reçu impossible.", echec);
        }
    }

    @Override
    @PreAuthorize("@perm.a('files.consulter')")
    public ResponseEntity<FileLink> createFileLink(UUID fileId) {
        FileService.Lien lien = fichiers.creerLien(fileId);

        FileLink contrat = new FileLink();
        contrat.setUrl(lien.url());
        contrat.setExpiresAt(lien.expiresAt().atOffset(ZoneOffset.UTC));
        return ResponseEntity.ok(contrat);
    }

    /**
     * Sert le contenu. Ouvert sans jeton porteur : c'est le lien signé, opaque et
     * expirant, qui fait foi (PLT-05).
     */
    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<Resource> downloadFile(String token) {
        FileService.Contenu contenu = fichiers.servir(token);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contenu.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + contenu.filename() + "\"")
                .body(new ByteArrayResource(contenu.octets()));
    }

    @Override
    @PreAuthorize("@perm.a('files.deposer')")
    public ResponseEntity<Void> deleteFile(UUID fileId) {
        fichiers.supprimer(fileId);
        return ResponseEntity.noContent().build();
    }

    private static StoredFile versContrat(ma.clubify.platform.model.entity.StoredFile fichier) {
        StoredFile contrat = new StoredFile();
        contrat.setId(fichier.getId());
        contrat.setPurpose(FilePurpose.fromValue(fichier.getPurpose()));
        contrat.setFilename(fichier.getFilename());
        contrat.setContentType(fichier.getContentType());
        contrat.setSizeBytes(fichier.getSizeBytes());
        if (fichier.getCreatedAt() != null) {
            contrat.setCreatedAt(fichier.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        return contrat;
    }
}
