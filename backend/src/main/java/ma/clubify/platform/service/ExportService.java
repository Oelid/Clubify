package ma.clubify.platform.service;

import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.exception.BusinessRuleException;
import ma.clubify.common.export.Colonne;
import ma.clubify.common.export.ExportWriter;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.common.util.Json;
import ma.clubify.config.AuthenticatedUser;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Exports de listes (INT-03).
 *
 * <p>Exporter est une permission distincte de consulter : un export quitte
 * l'application, et ne se rattrape pas (benchmark B7, critère C33b). Chaque
 * export laisse une trace (critère C35).
 */
@Service
public class ExportService {

    private final UserService utilisateurs;
    private final ExportWriter ecrivain;
    private final MessageSource messages;
    private final DomainEvents evenements;
    private final Json json;

    public ExportService(UserService utilisateurs, ExportWriter ecrivain,
                         MessageSource messages, DomainEvents evenements, Json json) {
        this.utilisateurs = utilisateurs;
        this.ecrivain = ecrivain;
        this.messages = messages;
        this.evenements = evenements;
        this.json = json;
    }

    @Transactional
    public Resultat exporter(String liste, String format) {
        AuthenticatedUser utilisateur = PermissionChecker.requis();
        String permission = liste + ".exporter";
        if (!utilisateur.detient(permission)) {
            throw new AccessDeniedException(permission);
        }

        Resultat resultat = switch (liste) {
            case "users" -> exporterUtilisateurs(format);
            default -> throw new BusinessRuleException("export.dataset.unknown");
        };

        evenements.publish(new DomainEvent(utilisateur.clubId(), "export.created", "Export", null,
                null, json.de(Map.of("dataset", liste, "format", format,
                "rows", String.valueOf(resultat.lignes()))), null));
        return resultat;
    }

    private Resultat exporterUtilisateurs(String format) {
        List<UserService.Vue> vues = utilisateurs
                .lister(org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        List<Colonne<UserService.Vue>> colonnes = List.of(
                Colonne.de("lastName", libelle("export.users.lastName"),
                        v -> v.compte().getLastName()),
                Colonne.de("firstName", libelle("export.users.firstName"),
                        v -> v.compte().getFirstName()),
                Colonne.de("email", libelle("export.users.email"), v -> v.compte().getEmail()),
                Colonne.de("role", libelle("export.users.role"),
                        v -> libelle("role." + v.appartenance().getRole().name())),
                Colonne.de("active", libelle("export.users.active"),
                        v -> libelle(v.compte().isActive() ? "yes" : "no")));

        byte[] octets = "XLSX".equals(format)
                ? ecrivain.xlsx(libelle("export.users.sheet"), colonnes, vues)
                : ecrivain.csv(colonnes, vues);

        return new Resultat(octets, format, vues.size());
    }

    private String libelle(String cle) {
        return messages.getMessage(cle, null, cle, LocaleContextHolder.getLocale());
    }

    public record Resultat(byte[] octets, String format, int lignes) {
    }
}
