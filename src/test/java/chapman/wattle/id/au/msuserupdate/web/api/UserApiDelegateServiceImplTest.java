package chapman.wattle.id.au.msuserupdate.web.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import chapman.wattle.id.au.msuserupdate.TestConstants;
import chapman.wattle.id.au.msuserupdate.service.UserService;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserModify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class UserApiDelegateServiceImplTest {

    @Mock
    private UserService userService;

    @Test
    public void testAddUserSuccess() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        User user = new User();
        user
            .orgId(TestConstants.ORG_ID)
            .firstName(TestConstants.TEST_FIRST)
            .lastName(TestConstants.TEST_LAST)
            .username(TestConstants.TEST_USER);
        when(userService.getUser(TestConstants.USER_ID)).thenReturn(null);
        doNothing().when(userService).addUser(user);

        UserApiDelegateServiceImpl delegate = new UserApiDelegateServiceImpl(userService);

        ResponseEntity<Void> entity = delegate.addUser(user);
        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
    }

    @Test
    public void testAddUserFailure() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        User user = new User();
        user
            .orgId(TestConstants.ORG_ID)
            .firstName(TestConstants.TEST_FIRST)
            .lastName(TestConstants.TEST_LAST)
            .username(TestConstants.TEST_USER);
        when(userService.getUser(TestConstants.USER_ID)).thenReturn(user);

        UserApiDelegateServiceImpl delegate = new UserApiDelegateServiceImpl(userService);

        ResponseEntity<Void> entity = delegate.addUser(user);
        assertEquals(HttpStatus.SEE_OTHER, entity.getStatusCode());
    }

    @Test
    public void testGetUserSuccess() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        User user = new User();
        user
            .orgId(TestConstants.ORG_ID)
            .firstName(TestConstants.TEST_FIRST)
            .lastName(TestConstants.TEST_LAST)
            .username(TestConstants.TEST_USER);
        when(userService.getUser(TestConstants.USER_ID)).thenReturn(user);

        UserApiDelegateServiceImpl delegate = new UserApiDelegateServiceImpl(userService);

        ResponseEntity<User> entity = delegate.getUserById(TestConstants.USER_ID);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    public void testModifyUserSuccess() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        UserModify body = new UserModify();

        doNothing().when(userService).modifyUser(TestConstants.USER_ID, body);

        UserApiDelegateServiceImpl delegate = new UserApiDelegateServiceImpl(userService);

        ResponseEntity<Void> entity = delegate.updateUser(TestConstants.USER_ID, body);
        assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode());
    }
}
