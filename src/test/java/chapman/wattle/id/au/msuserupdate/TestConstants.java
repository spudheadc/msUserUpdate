package chapman.wattle.id.au.msuserupdate;

import chapman.wattle.id.au.msuserupdate.domain.UserCreated;
import chapman.wattle.id.au.msuserupdate.domain.UserDeleted;
import chapman.wattle.id.au.msuserupdate.domain.UserEmailEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserEventsEntity;
import chapman.wattle.id.au.msuserupdate.domain.UserNameEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserPhoneEdited;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import java.util.UUID;

public class TestConstants {

    public static final String TEST_FIRST = "testFirst";
    public static final String TEST_LAST = "testLast";
    public static final String TEST_USER = "testUser";
    public static final String TEST_MIDDLE = "testMiddle";
    public static final String TEST_LAST_2 = "testLast2";
    public static final String TEST_EMAIL = "testEmail@test.com";
    public static final String TEST_EMAIL_2 = "testEmail2@test.com";
    public static final String TEST_PHONE = "1234567890";
    public static final String TEST_PHONE_2 = "0987654321";
    public static UUID ORG_ID = UUID.randomUUID();
    public static UUID USER_ID = UUID.nameUUIDFromBytes(TEST_USER.getBytes());
    public static User TEST_USER_DTO =
        (new User()).orgId(TestConstants.ORG_ID)
            .firstName(TestConstants.TEST_FIRST)
            .lastName(TestConstants.TEST_LAST)
            .username(TestConstants.TEST_USER)
            .email(TestConstants.TEST_EMAIL)
            .phone(TestConstants.TEST_PHONE);
    public static final UserEventsEntity NAME_CHANGE_ENTITY = new UserEventsEntity() {
        {
            this.setEvent(UserNameEdited.newBuilder().setFirstName(TEST_FIRST).setMiddleName(TEST_MIDDLE).setLastName(TEST_LAST_2).build());
        }
    };
    public static final UserEventsEntity EMAIL_CHANGE_ENTITY = new UserEventsEntity() {
        {
            this.setEvent(UserEmailEdited.newBuilder().setEmail(TEST_EMAIL_2).build());
        }
    };
    public static final UserEventsEntity PHONE_CHANGE_ENTITY = new UserEventsEntity() {
        {
            this.setEvent(UserPhoneEdited.newBuilder().setPhone(TEST_PHONE_2).build());
        }
    };
    public static final UserEventsEntity DELETED_ENTITY = new UserEventsEntity() {
        {
            this.setEvent(UserDeleted.newBuilder().setReason("").build());
        }
    };
    public static final UserEventsEntity CREATED_ENTITY = new UserEventsEntity() {
        {
            this.setEvent(
                    UserCreated
                        .newBuilder()
                        .setFirstName(TEST_FIRST)
                        .setMiddleName(TEST_MIDDLE)
                        .setLastName(TEST_LAST)
                        .setOrgId(ORG_ID.toString())
                        .setUserName(TEST_USER)
                        .setEmail(TEST_EMAIL)
                        .setPhone(TEST_PHONE)
                        .build()
                );
        }
    };
}
