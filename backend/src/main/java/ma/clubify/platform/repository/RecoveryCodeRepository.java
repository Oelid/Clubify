package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.RecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, UUID> {

    List<RecoveryCode> findAllByUserIdAndUsedAtIsNull(UUID userId);

    void deleteAllByUserId(UUID userId);
}
