package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {

    Optional<TrustedDevice> findByTokenHash(String tokenHash);

    List<TrustedDevice> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}
