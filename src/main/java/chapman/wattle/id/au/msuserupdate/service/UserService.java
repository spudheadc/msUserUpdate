package chapman.wattle.id.au.msuserupdate.service;

import chapman.wattle.id.au.msuserupdate.domain.UserCreated;
import chapman.wattle.id.au.msuserupdate.domain.UserDeleted;
import chapman.wattle.id.au.msuserupdate.domain.UserEmailEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserEvents;
import chapman.wattle.id.au.msuserupdate.domain.UserEventsEntity;
import chapman.wattle.id.au.msuserupdate.domain.UserNameEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserPhoneEdited;
import chapman.wattle.id.au.msuserupdate.repository.UserEventsRepository;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final BiFunction<UserEntity, UserEntity, UserEntity> USER_CREATE_BIFUNCTION = (
        UserEntity currentUser,
        UserEntity update
    ) -> {
        currentUser.user = update.user;
        return currentUser;
    };

    private static final BiFunction<UserEntity, UserEntity, UserEntity> USER_DELETED_BIFUNCTION = (
        UserEntity currentUser,
        UserEntity update
    ) -> {
        currentUser.user = null;
        return currentUser;
    };
    private static final BiFunction<UserEntity, UserEntity, UserEntity> USER_EDIT_BIFUNCTION = (
        UserEntity currentUser,
        UserEntity update
    ) -> {
        if (currentUser.user != null) currentUser.user
            .firstName(update.user.getFirstName())
            .middleName(update.user.getMiddleName())
            .lastName(update.user.getLastName());
        return currentUser;
    };
    private static final BiFunction<UserEntity, UserEntity, UserEntity> USER_PHONE_BIFUNCTION = (
        UserEntity currentUser,
        UserEntity update
    ) -> {
        if (currentUser.user != null) currentUser.user.phone(update.user.getPhone());
        return currentUser;
    };
    private static final BiFunction<UserEntity, UserEntity, UserEntity> USER_EMAIL_BIFUNCTION = (
        UserEntity currentUser,
        UserEntity update
    ) -> {
        if (currentUser.user != null) currentUser.user.email(update.user.getEmail());
        return currentUser;
    };

    private static final Map<Class<?>, BiFunction<UserEntity, UserEntity, UserEntity>> UNMARSHALL = Map.of(
        UserCreated.class,
        USER_CREATE_BIFUNCTION,
        UserNameEdited.class,
        USER_EDIT_BIFUNCTION,
        UserPhoneEdited.class,
        USER_PHONE_BIFUNCTION,
        UserEmailEdited.class,
        USER_EMAIL_BIFUNCTION,
        UserDeleted.class,
        USER_DELETED_BIFUNCTION
    );

    private UserEventsRepository eventsRepository;

    private ModelMapper modelMapper = new ModelMapper();

    public UserService(UserEventsRepository eventsRepository, KafkaProducer<String, UserEvents> producer) {
        this.eventsRepository = eventsRepository;
        this.producer = producer;
    }

    private KafkaProducer<String, UserEvents> producer;

    public User getUser(UUID userId) {
        List<UserEventsEntity> events = eventsRepository.findByUserId(userId.toString());
        User user = null;
        if (events != null && events.size() > 0) {
            UserEntity entity = null;
            UserEntity result = events
                .stream()
                .map(event -> event.getEvent())
                .map(event -> new UserEntity((User) modelMapper.map(event, User.class), event))
                .reduce(
                    entity,
                    (UserEntity currentEntry, UserEntity entry) -> {
                        if (currentEntry == null) return entry;
                        return currentEntry.updateEntity(entry);
                    }
                );
            if (result != null) user = result.getUser();
        }

        return user;
    }

    private class UserEntity {

        private User user;
        private Object event;

        public UserEntity(User user, Object event) {
            this.user = user;
            this.event = event;
        }

        public User getUser() {
            return user;
        }

        public Object getEvent() {
            return event;
        }

        public UserEntity updateEntity(UserEntity update) {
            BiFunction<UserEntity, UserEntity, UserEntity> func = UNMARSHALL.get(update.getEvent().getClass());
            if (func == null) return this;
            return func.apply(this, update);
        }
    }

    public void addUser(User user) {
        String userId = UUID.nameUUIDFromBytes(user.getUsername().getBytes()).toString();

        UserCreated created = UserCreated
            .newBuilder()
            .setOrgId(user.getOrgId().toString())
            .setUserName(user.getUsername())
            .setFirstName(user.getFirstName())
            .setLastName(user.getLastName())
            .setEmail(user.getEmail())
            .setPhone(user.getPhone())
            .build();
        UserEvents event = UserEvents.newBuilder().setCreatedAt(Instant.now()).setUserId(userId).setEvent(created).build();

        ProducerRecord<String, UserEvents> record = new ProducerRecord<>("userEvents", user.getOrgId().toString(), event);
        try {
            producer
                .send(
                    record,
                    new Callback() {
                        public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
                            if (exception != null) {
                                throw new UserException(exception);
                            }
                        }
                    }
                )
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new UserException(e);
        }
    }
}
