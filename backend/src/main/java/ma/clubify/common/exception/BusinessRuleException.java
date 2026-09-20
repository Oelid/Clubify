package ma.clubify.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Règle métier non respectée. Elle porte un <strong>code stable</strong>, que
 * l'interface traduit : le contrat ne transporte jamais de libellé
 * (contracts/README.md, critère C36).
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessRuleException(String code) {
        this(code, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessRuleException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
