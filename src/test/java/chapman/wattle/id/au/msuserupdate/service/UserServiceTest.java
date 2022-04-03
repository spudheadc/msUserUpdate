package chapman.wattle.id.au.msuserupdate.service;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import chapman.wattle.id.au.msuserupdate.TestConstants;
import chapman.wattle.id.au.msuserupdate.domain.UserCreated;
import chapman.wattle.id.au.msuserupdate.domain.UserEmailEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserEvents;
import chapman.wattle.id.au.msuserupdate.domain.UserNameEdited;
import chapman.wattle.id.au.msuserupdate.domain.UserPhoneEdited;
import chapman.wattle.id.au.msuserupdate.repository.UserEventsRepository;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserEventsRepository eventsRepository;

    @Mock
    private KafkaProducer<String, UserEvents> producer;

    @Mock
    Future<RecordMetadata> future;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, UserEvents>> producerCaptor;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, UserEvents>> callbackCaptor;

    @Test
    @SuppressWarnings("unchecked")
    public void testAddUserSuccess() {
        UserService service = new UserService(eventsRepository, producer);

        doAnswer((InvocationOnMock invocation) -> {
                Object arg0 = invocation.getArgument(0);
                Object arg1 = invocation.getArgument(1);
                assertInstanceOf(ProducerRecord.class, arg0);
                assertInstanceOf(Callback.class, arg1);
                Callback callback = (Callback) arg1;
                callback.onCompletion(null, null);

                ProducerRecord<String, UserEvents> record = (ProducerRecord<String, UserEvents>) arg0;
                assertEquals(TestConstants.ORG_ID.toString(), record.key());
                assertTrue(record.value().getEvent() instanceof UserCreated);
                assertEquals(TestConstants.TEST_FIRST, ((UserCreated) record.value().getEvent()).getFirstName());
                return future;
            })
            .when(producer)
            .send(any(ProducerRecord.class), any(Callback.class));

        service.addUser(TestConstants.TEST_USER_DTO);
    }

    @Test
    public void testGetUserSuccess() {
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString()))
            .thenReturn(Collections.singletonList(TestConstants.CREATED_ENTITY));

        UserService service = new UserService(eventsRepository, producer);
        User user = service.getUser(TestConstants.USER_ID);
        assertNotNull(user);
        assertEquals(TestConstants.TEST_FIRST, user.getFirstName());
        assertEquals(TestConstants.TEST_MIDDLE, user.getMiddleName());
        assertEquals(TestConstants.TEST_LAST, user.getLastName());
        assertEquals(TestConstants.TEST_PHONE, user.getPhone());
        assertEquals(TestConstants.TEST_EMAIL, user.getEmail());
        assertEquals(TestConstants.ORG_ID, user.getOrgId());
    }

    @Test
    public void testGetUserDoesntExist() {
        UserService service = new UserService(eventsRepository, producer);
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString())).thenReturn(null);
        assertNull(service.getUser(TestConstants.USER_ID));
    }

    @Test
    public void testCheckUserExistAndDeleted() {
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString()))
            .thenReturn(Arrays.asList(TestConstants.CREATED_ENTITY, TestConstants.DELETED_ENTITY));

        UserService service = new UserService(eventsRepository, producer);
        assertNull(service.getUser(TestConstants.USER_ID));
    }

    @Test
    public void testGetUserSuccessAndChangedEmailPhone() {
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString()))
            .thenReturn(Arrays.asList(TestConstants.CREATED_ENTITY, TestConstants.PHONE_CHANGE_ENTITY, TestConstants.EMAIL_CHANGE_ENTITY));

        UserService service = new UserService(eventsRepository, producer);
        User user = service.getUser(TestConstants.USER_ID);
        assertNotNull(user);
        assertEquals(TestConstants.TEST_PHONE_2, user.getPhone());
        assertEquals(TestConstants.TEST_EMAIL_2, user.getEmail());
        assertEquals(TestConstants.TEST_FIRST, user.getFirstName());
        assertEquals(TestConstants.TEST_MIDDLE, user.getMiddleName());
        assertEquals(TestConstants.TEST_LAST, user.getLastName());
        assertEquals(TestConstants.ORG_ID, user.getOrgId());
    }

    @Test
    public void testGetUserSuccessAndChangedName() {
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString()))
            .thenReturn(Arrays.asList(TestConstants.CREATED_ENTITY, TestConstants.NAME_CHANGE_ENTITY));

        UserService service = new UserService(eventsRepository, producer);
        User user = service.getUser(TestConstants.USER_ID);
        assertNotNull(user);
        assertEquals(TestConstants.TEST_FIRST, user.getFirstName());
        assertEquals(TestConstants.TEST_MIDDLE, user.getMiddleName());
        assertEquals(TestConstants.TEST_LAST_2, user.getLastName());
        assertEquals(TestConstants.TEST_PHONE, user.getPhone());
        assertEquals(TestConstants.TEST_EMAIL, user.getEmail());
        assertEquals(TestConstants.ORG_ID, user.getOrgId());
    }

    @Test
    public void testModifyUserDoesntExist() {
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString()))
            .thenReturn(Arrays.asList(TestConstants.CREATED_ENTITY, TestConstants.DELETED_ENTITY));

        UserService service = new UserService(eventsRepository, producer);
        Exception exception = assertThrows(
            UserException.class,
            () -> {
                service.modifyUser(TestConstants.USER_ID, TestConstants.TEST_USER_EMAIL_DTO);
            }
        );

        assertEquals("User doesn't exist", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModifyUserEmailSuccess() {
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString()))
            .thenReturn(Collections.singletonList(TestConstants.CREATED_ENTITY));
        doAnswer((InvocationOnMock invocation) -> {
                Object arg0 = invocation.getArgument(0);
                Object arg1 = invocation.getArgument(1);
                assertInstanceOf(ProducerRecord.class, arg0);
                assertInstanceOf(Callback.class, arg1);
                Callback callback = (Callback) arg1;
                callback.onCompletion(null, null);

                ProducerRecord<String, UserEvents> record = (ProducerRecord<String, UserEvents>) arg0;
                assertEquals(TestConstants.ORG_ID.toString(), record.key());
                assertTrue(record.value().getEvent() instanceof UserEmailEdited);
                assertEquals(TestConstants.TEST_EMAIL_2, ((UserEmailEdited) record.value().getEvent()).getEmail());
                return future;
            })
            .when(producer)
            .send(any(ProducerRecord.class), any(Callback.class));
        UserService service = new UserService(eventsRepository, producer);

        service.modifyUser(TestConstants.USER_ID, TestConstants.TEST_USER_EMAIL_DTO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModifyUserPhoneSuccess() {
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString()))
            .thenReturn(Collections.singletonList(TestConstants.CREATED_ENTITY));
        doAnswer((InvocationOnMock invocation) -> {
                Object arg0 = invocation.getArgument(0);
                Object arg1 = invocation.getArgument(1);
                assertInstanceOf(ProducerRecord.class, arg0);
                assertInstanceOf(Callback.class, arg1);
                Callback callback = (Callback) arg1;
                callback.onCompletion(null, null);

                ProducerRecord<String, UserEvents> record = (ProducerRecord<String, UserEvents>) arg0;
                assertEquals(TestConstants.ORG_ID.toString(), record.key());
                assertTrue(record.value().getEvent() instanceof UserPhoneEdited);
                assertEquals(TestConstants.TEST_PHONE_2, ((UserPhoneEdited) record.value().getEvent()).getPhone());
                return future;
            })
            .when(producer)
            .send(any(ProducerRecord.class), any(Callback.class));
        UserService service = new UserService(eventsRepository, producer);

        service.modifyUser(TestConstants.USER_ID, TestConstants.TEST_USER_PHONE_DTO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModifyUserNameSuccess() {
        when(eventsRepository.findByUserId(TestConstants.USER_ID.toString()))
            .thenReturn(Collections.singletonList(TestConstants.CREATED_ENTITY));
        doAnswer((InvocationOnMock invocation) -> {
                Object arg0 = invocation.getArgument(0);
                Object arg1 = invocation.getArgument(1);
                assertInstanceOf(ProducerRecord.class, arg0);
                assertInstanceOf(Callback.class, arg1);
                Callback callback = (Callback) arg1;
                callback.onCompletion(null, null);

                ProducerRecord<String, UserEvents> record = (ProducerRecord<String, UserEvents>) arg0;
                assertEquals(TestConstants.ORG_ID.toString(), record.key());
                assertTrue(record.value().getEvent() instanceof UserNameEdited);
                assertEquals(TestConstants.TEST_LAST_2, ((UserNameEdited) record.value().getEvent()).getLastName());
                return future;
            })
            .when(producer)
            .send(any(ProducerRecord.class), any(Callback.class));
        UserService service = new UserService(eventsRepository, producer);

        service.modifyUser(TestConstants.USER_ID, TestConstants.TEST_USER_NAME_DTO);
    }
}
