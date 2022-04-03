package chapman.wattle.id.au.msuserupdate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/** Base composite annotation for integration tests. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = MsUserUpdateApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(RedisTestContainerExtension.class)
@ExtendWith(MongoDbTestContainerExtension.class)
public @interface IntegrationTest {}
