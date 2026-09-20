package ma.clubify.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Chiffre un champ sensible au repos. Posé sur l'attribut, il rend impossible
 * d'écrire la donnée en clair par inadvertance (SEC-03).
 *
 * <p>Un seul constructeur, avec injection : Hibernate obtient l'instance auprès
 * de Spring plutôt que de la construire lui-même, ce qui garantit que le service
 * de chiffrement est présent. Un constructeur vide rouvrirait la porte à une
 * instance sans clé, qui échouerait à la première écriture.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionService chiffrement;

    public EncryptedStringConverter(EncryptionService chiffrement) {
        this.chiffrement = chiffrement;
    }

    @Override
    public String convertToDatabaseColumn(String clair) {
        return clair == null ? null : chiffrement.chiffrerTexte(clair);
    }

    @Override
    public String convertToEntityAttribute(String chiffre) {
        return chiffre == null ? null : chiffrement.dechiffrerTexte(chiffre);
    }
}
