package chapman.wattle.id.au.msuserupdate.domain;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "userEvents")
public class UserEventsEntity extends UserEvents {

    private ModelMapper modelMapper = new ModelMapper();

    @SuppressWarnings("rawtypes")
    private static Class[] CLASSES = {
        UserCreated.class,
        UserDeleted.class,
        UserNameEdited.class,
        UserPhoneEdited.class,
        UserEmailEdited.class,
    };

    @Field
    public Object getEvent() {
        Object obj = super.getEvent();
        if (!(obj instanceof LinkedHashMap)) return obj;
        List<Object> list = Arrays
            .stream(CLASSES)
            .map(clazz -> mapEvent(clazz, obj))
            .filter(entry -> entry != null)
            .collect(Collectors.toList());

        return list.get(0);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object mapEvent(Class clazz, Object obj) {
        String className = clazz.getSimpleName();
        if ((obj instanceof LinkedHashMap) && (((LinkedHashMap<String, Object>) obj).get(className) != null)) return modelMapper.map(
            ((LinkedHashMap<String, Object>) obj).get(className),
            clazz
        );
        return null;
    }

    @Field
    public Instant getCreatedAt() {
        return super.getCreatedAt();
    }

    @Field
    public String getUserId() {
        return super.getUserId();
    }
}
