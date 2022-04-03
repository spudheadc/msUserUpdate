package chapman.wattle.id.au.msuserupdate.repository;

import chapman.wattle.id.au.msuserupdate.domain.UserEventsEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/** Spring Data MongoDB repository for the Events entity. */
@SuppressWarnings("unused")
@Repository
public interface UserEventsRepository extends MongoRepository<UserEventsEntity, String> {
  List<UserEventsEntity> findByUserId(String userId);
}
