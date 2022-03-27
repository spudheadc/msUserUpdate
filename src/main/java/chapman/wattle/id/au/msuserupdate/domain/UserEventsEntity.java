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
    public Object mapEvent(Class clazz, Object obj) {
        if (
            (obj instanceof LinkedHashMap) && (((LinkedHashMap<String, Object>) obj).get(clazz.getSimpleName()) != null)
        ) return modelMapper.map(((LinkedHashMap<String, Object>) obj).get(clazz.getSimpleName()), clazz);
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean isEventOfType(Class clazz) {
        if (
            clazz.isInstance(getEvent()) ||
            ((getEvent() instanceof LinkedHashMap) && (((LinkedHashMap<String, Object>) getEvent()).get(clazz.getSimpleName()) != null))
        ) return true;
        return false;
    }
}
