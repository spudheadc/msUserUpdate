package chapman.wattle.id.au.msuserupdate.domain;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import chapman.wattle.id.au.msuserupdate.TestConstants;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class UserEventsEntityTest {

    private ModelMapper modelMapper = new ModelMapper();

    @Test
    void testGetEvent() {
        LinkedHashMap<String, Object> eventMap = new java.util.LinkedHashMap<String, Object>();
        eventMap.put("UserCreated", modelMapper.map(TestConstants.CREATED_ENTITY.getEvent(), LinkedHashMap.class));
        UserEventsEntity entity = new UserEventsEntity() {
            {
                this.setEvent(eventMap);
            }
        };
        assertInstanceOf(UserCreated.class, entity.getEvent());
    }
}
