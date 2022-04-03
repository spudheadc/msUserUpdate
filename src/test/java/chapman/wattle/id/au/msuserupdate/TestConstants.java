package chapman.wattle.id.au.msuserupdate;

import chapman.wattle.id.au.msuserupdate.domain.UserCreated;
import chapman.wattle.id.au.msuserupdate.domain.UserDeleted;
import chapman.wattle.id.au.msuserupdate.domain.UserEmailEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserEventsEntity;
import chapman.wattle.id.au.msuserupdate.domain.UserNameEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserPhoneEdited;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserEmailModify;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserModify;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserModify.UpdateTypeEnum;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserNameModify;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserPhoneModify;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.ObjectId;

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
  public static final String TEST_REASON = "Some reason";
  public static User TEST_USER_DTO =
      (new User())
          .orgId(TestConstants.ORG_ID)
          .firstName(TestConstants.TEST_FIRST)
          .lastName(TestConstants.TEST_LAST)
          .username(TestConstants.TEST_USER)
          .email(TestConstants.TEST_EMAIL)
          .phone(TestConstants.TEST_PHONE);

  public static UserModify TEST_USER_PHONE_DTO =
      (new UserPhoneModify())
          .phone(TestConstants.TEST_PHONE_2)
          .updateType(UpdateTypeEnum.USERPHONE);
  public static UserModify TEST_USER_EMAIL_DTO =
      (new UserEmailModify())
          .email(TestConstants.TEST_EMAIL_2)
          .updateType(UpdateTypeEnum.USEREMAIL);
  public static UserModify TEST_USER_NAME_DTO =
      (new UserNameModify())
          .firstName(TestConstants.TEST_FIRST)
          .lastName(TestConstants.TEST_LAST_2)
          .updateType(UpdateTypeEnum.USERNAME);
  public static final UserEventsEntity NAME_CHANGE_ENTITY =
      new UserEventsEntity() {
        {
          this.setUserId(USER_ID.toString());
          this.setCreatedAt(Instant.now());
          this.setEvent(
              UserNameEdited.newBuilder()
                  .setFirstName(TEST_FIRST)
                  .setMiddleName(TEST_MIDDLE)
                  .setLastName(TEST_LAST_2)
                  .build());
        }
      };
  public static final UserEventsEntity EMAIL_CHANGE_ENTITY =
      new UserEventsEntity() {
        {
          this.setUserId(USER_ID.toString());
          this.setCreatedAt(Instant.now());
          this.setEvent(UserEmailEdited.newBuilder().setEmail(TEST_EMAIL_2).build());
        }
      };
  public static final UserEventsEntity PHONE_CHANGE_ENTITY =
      new UserEventsEntity() {
        {
          this.setUserId(USER_ID.toString());
          this.setCreatedAt(Instant.now());
          this.setEvent(UserPhoneEdited.newBuilder().setPhone(TEST_PHONE_2).build());
        }
      };
  public static final UserEventsEntity DELETED_ENTITY =
      new UserEventsEntity() {
        {
          this.setUserId(USER_ID.toString());
          this.setCreatedAt(Instant.now());
          this.setEvent(UserDeleted.newBuilder().setReason("").build());
        }
      };
  public static final UserEventsEntity CREATED_ENTITY =
      new UserEventsEntity() {
        {
          this.setUserId(USER_ID.toString());
          this.setCreatedAt(Instant.now());
          this.setEvent(
              UserCreated.newBuilder()
                  .setFirstName(TEST_FIRST)
                  .setMiddleName(TEST_MIDDLE)
                  .setLastName(TEST_LAST)
                  .setOrgId(ORG_ID.toString())
                  .setUserName(TEST_USER)
                  .setEmail(TEST_EMAIL)
                  .setPhone(TEST_PHONE)
                  .build());
        }
      };
  public static final Document CREATED_ENTITY_DOCUMENT =
      (new Document("_id", new ObjectId()))
          .append("userId", USER_ID.toString())
          .append("createdAt", Instant.now().toString())
          .append(
              "event",
              new LinkedHashMap<String, LinkedHashMap<String, String>>() {
                {
                  this.put(
                      "UserCreated",
                      new LinkedHashMap<String, String>() {
                        {
                          this.put("firstName", TEST_FIRST);
                          this.put("middleName", TEST_MIDDLE);
                          this.put("lastName", TEST_LAST);
                          this.put("userName", TEST_USER);
                          this.put("email", TEST_EMAIL);
                          this.put("phone", TEST_PHONE);
                          this.put("orgId", ORG_ID.toString());
                        }
                      });
                  this.put("UserDeleted", null);
                  this.put("UserNameEdited", null);
                  this.put("UserPhoneEdited", null);
                  this.put("UserEmailEdited", null);
                }
              });
}
