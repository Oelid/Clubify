package ma.clubify.platform.service;

import ma.clubify.common.exception.NotFoundException;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.config.AuthenticatedUser;
import ma.clubify.platform.model.dto.CurrentUserDto;
import ma.clubify.platform.model.entity.Club;
import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.UserAccount;
import ma.clubify.platform.repository.ClubRepository;
import ma.clubify.platform.repository.MembershipRepository;
import ma.clubify.platform.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Lectures sur l'utilisateur connecté et son club.
 *
 * <p>Un service ne rend ni entité ni modèle de contrat : la mise en forme de la
 * réponse appartient au contrôleur (backend/CLAUDE.md).
 */
@Service
public class UserQueryService {

    private final UserAccountRepository comptes;
    private final MembershipRepository appartenances;
    private final ClubRepository clubs;
    private final ClubSettingService reglages;
    private final MfaPolicy politique;

    public UserQueryService(UserAccountRepository comptes, MembershipRepository appartenances,
                            ClubRepository clubs, ClubSettingService reglages,
                            MfaPolicy politique) {
        this.comptes = comptes;
        this.appartenances = appartenances;
        this.clubs = clubs;
        this.reglages = reglages;
        this.politique = politique;
    }

    @Transactional(readOnly = true)
    public CurrentUserDto utilisateurCourant() {
        AuthenticatedUser authentifie = PermissionChecker.requis();
        UserAccount compte = comptes.findById(authentifie.userId())
                .orElseThrow(() -> new NotFoundException("user.notFound"));
        Membership appartenance = appartenances.findById(authentifie.membershipId())
                .orElseThrow(() -> new NotFoundException("user.notFound"));
        Club club = clubs.findById(authentifie.clubId())
                .orElseThrow(() -> new NotFoundException("club.notFound"));

        MfaPolicy.Etat etat = politique.etatDe(compte, appartenance.getRole());
        return new CurrentUserDto(compte.getId(), compte.getEmail(), compte.getFirstName(),
                compte.getLastName(), compte.getLanguage(), appartenance.getRole().name(),
                compte.isMfaEnabled(),
                new CurrentUserDto.MfaStatut(etat.actif(), etat.attendu(), etat.bloquant(),
                        etat.exigeA()),
                new ArrayList<>(authentifie.permissions()),
                resume(club));
    }

    private CurrentUserDto.ClubIdentity resume(Club club) {
        return new CurrentUserDto.ClubIdentity(club.getId(), club.getName(), club.getTimezone(),
                club.getCurrency(), club.getLogoFileId(),
                reglages.texte("club.brand.primary"),
                reglages.texte("club.brand.secondary"));
    }
}
