package chapman.wattle.id.au.msuserupdate.domain;

import java.time.Instant;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "userEvents")
public class UserEventsEntity extends UserEvents {

    @Field
    public Object getEvent() {
        return super.getEvent();
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
