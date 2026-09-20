package ma.clubify.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Entité introuvable.
 *
 * <p>Une donnée appartenant à un autre club produit la même réponse : dire
 * « interdit » révélerait son existence (critère C1).
 */
public class NotFoundException extends BusinessRuleException {

    public NotFoundException(String code) {
        super(code, HttpStatus.NOT_FOUND);
    }
}
