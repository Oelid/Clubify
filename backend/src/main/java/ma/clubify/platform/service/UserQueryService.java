package ma.clubify.platform.service;

import ma.clubify.common.exception.NotFoundException;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.config.AuthenticatedUser;
import ma.clubify.generated.model.ClubSummary;
import ma.clubify.generated.model.CurrentUser;
import ma.clubify.generated.model.Role;
import ma.clubify.platform.model.entity.Club;
import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.UserAccount;
import ma.clubify.platform.repository.ClubRepository;
import ma.clubify.platform.repository.MembershipRepository;
import ma.clubify.platform.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/** Lectures sur l'utilisateur connecté et son club. */
@Service
public class UserQueryService {

    private final UserAccountRepository comptes;
    private final MembershipRepository appartenances;
    private final ClubRepository clubs;

    public UserQueryService(UserAccountRepository comptes, MembershipRepository appartenances,
                            ClubRepository clubs) {
        this.comptes = comptes;
        this.appartenances = appartenances;
        this.clubs = clubs;
    }

    @Transactional(readOnly = true)
    public CurrentUser utilisateurCourant() {
        AuthenticatedUser authentifie = PermissionChecker.requis();
        UserAccount compte = comptes.findById(authentifie.userId())
                .orElseThrow(() -> new NotFoundException("user.notFound"));
        Membership appartenance = appartenances.findById(authentifie.membershipId())
                .orElseThrow(() -> new NotFoundException("user.notFound"));
        Club club = clubs.findById(authentifie.clubId())
                .orElseThrow(() -> new NotFoundException("club.notFound"));

        CurrentUser courant = new CurrentUser();
        courant.setId(compte.getId());
        courant.setEmail(compte.getEmail());
        courant.setFirstName(compte.getFirstName());
        courant.setLastName(compte.getLastName());
        courant.setLanguage(compte.getLanguage());
        courant.setRole(Role.fromValue(appartenance.getRole().name()));
        courant.setMfaEnabled(compte.isMfaEnabled());
        courant.setPermissions(new ArrayList<>(authentifie.permissions()));
        courant.setClub(resume(club));
        return courant;
    }

    private static ClubSummary resume(Club club) {
        ClubSummary resume = new ClubSummary();
        resume.setId(club.getId());
        resume.setName(club.getName());
        resume.setTimezone(club.getTimezone());
        resume.setCurrency(club.getCurrency());
        resume.setLogoFileId(club.getLogoFileId());
        return resume;
    }
}
