package chapman.wattle.id.au.msuserupdate.web.api;

import chapman.wattle.id.au.msuserupdate.service.UserException;
import chapman.wattle.id.au.msuserupdate.service.UserService;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserModify;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

@Service
public class UserApiDelegateServiceImpl implements UserApiDelegate {

    private final Logger log = LoggerFactory.getLogger(UserApiDelegateServiceImpl.class);
    private UserService userService;

    public UserApiDelegateServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<Void> addUser(User user) {
        UUID userId = UUID.nameUUIDFromBytes(user.getUsername().getBytes());
        if (userService.getUser(userId) != null) return ResponseEntity.status(HttpStatus.SEE_OTHER).headers(getUserURI(userId)).build();

        userService.addUser(user);

        return ResponseEntity.status(HttpStatus.CREATED).headers(getUserURI(userId)).build();
    }

    private HttpHeaders getUserURI(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        URI uri;
        try {
            uri = MvcUriComponentsBuilder.fromController(UserApiController.class).path("/user/" + userId).build().toUri();
        } catch (Exception x) {
            uri = URI.create("/user/" + userId);
        }
        headers.setLocation(uri);
        return headers;
    }

    @Override
    public ResponseEntity<Void> deleteUser(Long userId, String apiKey) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<User> getUserById(UUID userId) {
        User user = userService.getUser(userId);
        if (user == null) throw new UserException("User doesn't exist");
        return ResponseEntity.ok(user);
    }

    @Override
    public ResponseEntity<Void> updateUser(UUID userId, UserModify body) {
        userService.modifyUser(userId, body);
        return ResponseEntity.noContent().headers(getUserURI(userId)).build();
    }
}
