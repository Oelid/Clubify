package ma.clubify.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Traduit toute erreur en {@code ProblemDetail} (RFC 9457) portant un code
 * stable et un message résolu dans la langue de la demande.
 *
 * <p>Ni trace technique, ni nom de classe, ni donnée d'enfant ne franchissent
 * cette frontière (CLAUDE.md §7, critère C36).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messages;
    private final ma.clubify.common.audit.AccessDenialAuditor refus;

    public GlobalExceptionHandler(MessageSource messages,
                                  ma.clubify.common.audit.AccessDenialAuditor refus) {
        this.messages = messages;
        this.refus = refus;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> onBusinessRule(BusinessRuleException echec) {
        return ResponseEntity.status(echec.getStatus())
                .body(probleme(echec.getStatus(), echec.getCode(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> onAccessDenied(AccessDeniedException echec,
                                                        HttpServletRequest demande) {
        refus.refus(demande.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(probleme(HttpStatus.FORBIDDEN, "security.permission.denied", null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException echec, HttpHeaders entetes,
            HttpStatusCode statut, WebRequest demande) {

        List<Map<String, String>> champs = echec.getBindingResult().getFieldErrors().stream()
                .map(erreur -> Map.of(
                        "field", erreur.getField(),
                        "code", String.valueOf(erreur.getDefaultMessage())))
                .toList();

        ProblemDetail corps = probleme(HttpStatus.UNPROCESSABLE_ENTITY, "validation.failed", champs);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(corps);
    }

    private ProblemDetail probleme(HttpStatusCode statut, String code, Object erreurs) {
        ProblemDetail detail = ProblemDetail.forStatus(statut);
        detail.setProperty("code", code);
        detail.setDetail(messages.getMessage(
                "errors." + code, null, code, LocaleContextHolder.getLocale()));
        if (erreurs != null) {
            detail.setProperty("errors", erreurs);
        }
        return detail;
    }

    /** Le corps ne contient jamais l'exception d'origine, seulement un code. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> onUnexpected(Exception echec, HttpServletRequest demande) {
        logger.error("Erreur inattendue sur " + demande.getRequestURI(), echec);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(probleme(HttpStatus.INTERNAL_SERVER_ERROR, "generic", null));
    }
}
