package ma.clubify.platform.model.dto;

import java.util.List;
import java.util.Set;

/** Droits effectifs d'un utilisateur et surcharges qui s'y appliquent. */
public record PermissionsDto(String role, Set<String> effective, List<OverrideDto> overrides) {

    /** Une surcharge : permission accordée ou retirée, avec son éventuel plafond. */
    public record OverrideDto(String code, boolean granted, String parameter) {
    }
}
