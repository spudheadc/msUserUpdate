package chapman.wattle.id.au.msuserupdate.config;

import chapman.wattle.id.au.msuserupdate.domain.UserCreated;
import chapman.wattle.id.au.msuserupdate.domain.UserDeleted;
import chapman.wattle.id.au.msuserupdate.domain.UserEmailEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserEventsEntity;
import chapman.wattle.id.au.msuserupdate.domain.UserNameEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserPhoneEdited;
import io.mongock.runner.springboot.EnableMongock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tech.jhipster.config.JHipsterConstants;
import tech.jhipster.domain.util.JSR310DateConverters.DateToZonedDateTimeConverter;
import tech.jhipster.domain.util.JSR310DateConverters.ZonedDateTimeToDateConverter;

@Configuration
@EnableMongock
@EnableMongoRepositories("chapman.wattle.id.au.msuserupdate.repository")
@Profile("!" + JHipsterConstants.SPRING_PROFILE_CLOUD)
@Import(value = MongoAutoConfiguration.class)
@EnableMongoAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class DatabaseConfiguration {

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(DateToZonedDateTimeConverter.INSTANCE);
        converters.add(ZonedDateTimeToDateConverter.INSTANCE);
        converters.add(UserEventsEntityConverter.INSTANCE);
        return new MongoCustomConversions(converters);
    }

    public static class UserEventsEntityConverter implements Converter<Document, UserEventsEntity> {

        public static final UserEventsEntityConverter INSTANCE = new UserEventsEntityConverter();

        private UserEventsEntityConverter() {}

        private ModelMapper modelMapper = new ModelMapper();

        @SuppressWarnings("rawtypes")
        private static final Class[] CLASSES = {
            UserCreated.class,
            UserDeleted.class,
            UserNameEdited.class,
            UserPhoneEdited.class,
            UserEmailEdited.class,
        };

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public UserEventsEntity convert(Document obj) {
            UserEventsEntity ret = new UserEventsEntity();
            ret.setCreatedAt(Instant.parse((String) obj.get("createdAt")));
            ret.setUserId((String) obj.get("userId"));
            Map<String, Map<String, String>> eventMap = (Map) obj.get("event");
            List<Object> list = Arrays
                .stream(CLASSES)
                .map(clazz -> mapEvent(clazz, eventMap))
                .filter(entry -> entry != null)
                .collect(Collectors.toList());
            ret.setEvent(list.get(0));

            return ret;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private Object mapEvent(Class clazz, Object obj) {
            String className = clazz.getSimpleName();
            if ((obj instanceof Map) && (((Map<String, Object>) obj).get(className) != null)) return modelMapper.map(
                ((Map<String, Object>) obj).get(className),
                clazz
            );
            return null;
        }
    }
}
