package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.ClubSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubSettingRepository extends JpaRepository<ClubSetting, UUID> {

    Optional<ClubSetting> findBySettingKey(String settingKey);

    List<ClubSetting> findAll();
}
