package chapman.wattle.id.au.msuserupdate.service;

import chapman.wattle.id.au.msuserupdate.config.Constants;
import chapman.wattle.id.au.msuserupdate.domain.UserCreated;
import chapman.wattle.id.au.msuserupdate.domain.UserDeleted;
import chapman.wattle.id.au.msuserupdate.domain.UserEmailEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserEvents;
import chapman.wattle.id.au.msuserupdate.domain.UserEventsEntity;
import chapman.wattle.id.au.msuserupdate.domain.UserNameEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserPhoneEdited;
import chapman.wattle.id.au.msuserupdate.repository.UserEventsRepository;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserEmailModify;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserModify;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserNameModify;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserPhoneModify;
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
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);
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

    private ModelMapper modelMapper = new ModelMapper() {
        {
            final Converter<String, UUID> stringToUUIDConverter = new AbstractConverter<>() {
                protected UUID convert(final String source) {
                    return UUID.fromString(source);
                }
            };
            this.addConverter(stringToUUIDConverter);
        }
    };

    public UserService(UserEventsRepository eventsRepository, KafkaProducer<String, UserEvents> producer) {
        this.eventsRepository = eventsRepository;
        this.producer = producer;
    }

    private KafkaProducer<String, UserEvents> producer;

    public User getUser(UUID userId) {
        log.debug("getUser userId:{}", userId.toString());
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
        UUID userId = UUID.nameUUIDFromBytes(user.getUsername().getBytes());
        log.debug("addUser userId:{}", userId.toString());

        UserCreated created = UserCreated
            .newBuilder()
            .setOrgId(user.getOrgId().toString())
            .setUserName(user.getUsername())
            .setFirstName(user.getFirstName())
            .setLastName(user.getLastName())
            .setEmail(user.getEmail())
            .setPhone(user.getPhone())
            .build();

        sendEvent(userId, user.getOrgId(), created);
    }

    public void modifyUser(UUID userId, UserModify user) {
        log.debug("modifyUser userId:{}", userId.toString());
        User baseUser = getUser(userId);
        if (baseUser == null) throw new UserException("User doesn't exist");

        UUID orgId = baseUser.getOrgId();

        Object event = null;
        switch (user.getUpdateType()) {
            case USERPHONE:
                event = UserPhoneEdited.newBuilder().setPhone(((UserPhoneModify) user).getPhone()).build();
                break;
            case USERNAME:
                event =
                    UserNameEdited
                        .newBuilder()
                        .setFirstName(((UserNameModify) user).getFirstName())
                        .setMiddleName(((UserNameModify) user).getMiddleName())
                        .setLastName(((UserNameModify) user).getLastName())
                        .build();
                break;
            case USEREMAIL:
                event = UserEmailEdited.newBuilder().setEmail(((UserEmailModify) user).getEmail()).build();
                break;
        }
        sendEvent(userId, orgId, event);
    }

    public void sendEvent(UUID userId, UUID orgId, Object baseEvent) {
        log.debug("sendEvent userId:{}, orgId: {}, baseEvent: {}", userId.toString(), orgId.toString(), baseEvent.toString());

        UserEvents event = UserEvents.newBuilder().setCreatedAt(Instant.now()).setUserId(userId.toString()).setEvent(baseEvent).build();

        ProducerRecord<String, UserEvents> record = new ProducerRecord<>(Constants.TOPIC_USER_EVENTS, orgId.toString(), event);

        try {
            producer
                .send(
                    record,
                    new Callback() {
                        public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
                            if (exception != null) {
                                log.error(
                                    "Exception in sendEvent userId:{}, orgId: {}," + " baseEvent: {}",
                                    userId.toString(),
                                    orgId.toString(),
                                    baseEvent.toString(),
                                    exception
                                );
                                throw new UserException(exception);
                            }

                            if (log.isDebugEnabled() && recordMetadata != null) log.debug(
                                "sendEvent complete topic:{}, partition:{}, offset:" + " {}",
                                recordMetadata.topic(),
                                recordMetadata.partition(),
                                recordMetadata.offset()
                            );
                        }
                    }
                )
                .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(
                "Exception in sendEvent userId:{}, orgId: {}, baseEvent: {}",
                userId.toString(),
                orgId.toString(),
                baseEvent.toString(),
                e
            );
            throw new UserException(e);
        }
    }
}
