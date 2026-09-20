package ma.clubify.platform.service;

import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.exception.BusinessRuleException;
import ma.clubify.common.exception.NotFoundException;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.common.util.PhoneNumbers;
import ma.clubify.platform.model.entity.Club;
import ma.clubify.platform.repository.ClubRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Identité et réglages du club (ADM-01). */
@Service
public class ClubService {

    /** L'ICE marocain compte quinze chiffres. */
    private static final Pattern ICE = Pattern.compile("^\\d{15}$");

    private final ClubRepository clubs;
    private final ClubSettingService reglages;
    private final DomainEvents evenements;

    public ClubService(ClubRepository clubs, ClubSettingService reglages, DomainEvents evenements) {
        this.clubs = clubs;
        this.reglages = reglages;
        this.evenements = evenements;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@perm.a('club.settings.consulter')")
    public Club lire() {
        return courant();
    }

    @Transactional
    @PreAuthorize("@perm.a('club.settings.modifier')")
    public Club modifier(String nom, String formeJuridique, String ice, String identifiantFiscal,
                         String registreDuCommerce, String adresse, String telephone,
                         String email, String fuseau, String devise, String langue) {
        Club club = courant();

        if (nom == null || nom.isBlank()) {
            throw new BusinessRuleException("club.name.required");
        }
        if (ice != null && !ice.isBlank() && !ICE.matcher(ice).matches()) {
            throw new BusinessRuleException("club.ice.invalid");
        }

        String avant = resume(club);

        club.setName(nom);
        club.setLegalForm(formeJuridique);
        club.setIce(ice);
        club.setTaxId(identifiantFiscal);
        club.setTradeRegister(registreDuCommerce);
        club.setAddress(adresse);
        club.setPhone(PhoneNumbers.normaliser(telephone));
        club.setEmail(email);
        if (fuseau != null) {
            club.setTimezone(fuseau);
        }
        if (devise != null) {
            club.setCurrency(devise);
        }
        if (langue != null) {
            club.setDefaultLanguage(langue);
        }

        evenements.publish(DomainEvent.modification(club.getId(), "club.updated", "Club",
                club.getId(), avant, resume(club)));
        return club;
    }

    @Transactional
    @PreAuthorize("@perm.a('club.settings.modifier')")
    public void definirLogo(UUID fileId) {
        Club club = courant();
        club.setLogoFileId(fileId);
        evenements.publish(DomainEvent.of(club.getId(), "club.logo.updated", "Club", club.getId()));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@perm.a('club.settings.consulter')")
    public Map<String, ClubSettingService.ValeurEffective> reglages() {
        return reglages.toutes();
    }

    @Transactional
    @PreAuthorize("@perm.a('club.settings.modifier')")
    public Map<String, ClubSettingService.ValeurEffective> definirReglages(
            List<Map.Entry<String, Object>> demandes) {
        demandes.forEach(demande -> reglages.definir(demande.getKey(), demande.getValue()));

        UUID clubId = PermissionChecker.requis().clubId();
        evenements.publish(new DomainEvent(clubId, "club.settings.updated", "Club", clubId,
                null, String.valueOf(demandes.size()), null));
        return reglages.toutes();
    }

    private Club courant() {
        UUID clubId = PermissionChecker.requis().clubId();
        return clubs.findById(clubId).orElseThrow(() -> new NotFoundException("club.notFound"));
    }

    private static String resume(Club club) {
        return "{\"name\":\"" + club.getName() + "\"}";
    }
}
