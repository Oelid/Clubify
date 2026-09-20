package ma.clubify.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Chiffre un champ sensible au repos. Posé sur l'attribut, il rend impossible
 * d'écrire la donnée en clair par inadvertance (SEC-03).
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService service;

    public EncryptedStringConverter(EncryptionService service) {
        EncryptedStringConverter.service = service;
    }

    /** JPA instancie aussi le convertisseur lui-même : le service est alors déjà posé. */
    public EncryptedStringConverter() {
    }

    @Override
    public String convertToDatabaseColumn(String clair) {
        return clair == null ? null : service.chiffrerTexte(clair);
    }

    @Override
    public String convertToEntityAttribute(String chiffre) {
        return chiffre == null ? null : service.dechiffrerTexte(chiffre);
    }
}
