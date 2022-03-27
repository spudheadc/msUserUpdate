package chapman.wattle.id.au.msuserupdate.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import chapman.wattle.id.au.msuserupdate.IntegrationTest;
import chapman.wattle.id.au.msuserupdate.config.ApplicationProperties;
import chapman.wattle.id.au.msuserupdate.config.Constants;
import chapman.wattle.id.au.msuserupdate.config.IntegrationTestConfig;
import chapman.wattle.id.au.msuserupdate.domain.UserEvents;
import chapman.wattle.id.au.msuserupdate.security.jwt.JWTFilter;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;

@IntegrationTest
@Timeout(value = 240, unit = TimeUnit.SECONDS)
@ContextConfiguration(classes = { IntegrationTestConfig.class, ApplicationProperties.class })
public class UserApiIntegrationTest {

    private final Logger log = LoggerFactory.getLogger(UserApiIntegrationTest.class);

    private static final String PHONE_VALUE = "0123456789";
    private static final String EMAIL_VALUE = "test@test.com";
    private static final String FIRST_NAME_VALUE = "FirstName";
    private static final String LAST_NAME_VALUE = "LastName";
    private static final String USER_NAME_VALUE = "somebloke";
    private static final String JWT =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJhdXRoIjoibWUifQ.4UtUT1uT4sSLFt4k1wYnIIZ7hDSTU9Wu55IuJxDdfBs";

    @Autowired
    private KafkaContainer kafkaContainer;

    @Resource(name = "getConsumerProps")
    private Map<String, Object> consumerProperties;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private void createTopics(String... topics) {
        var newTopics = Arrays.stream(topics).map(topic -> new NewTopic(topic, 1, (short) 1)).collect(Collectors.toList());
        AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getMappedKafkaUrl()));
        admin.createTopics(newTopics);
    }

    private String getMappedKafkaUrl() {
        Integer mappedPort = kafkaContainer.getMappedPort(KafkaContainer.KAFKA_PORT);
        return String.format("%s:%d", "localhost", mappedPort);
    }

    @Test
    void producesMessages() throws Exception {
        createTopics(Constants.TOPIC_USER_EVENTS);

        User user = new User()
            .username(USER_NAME_VALUE)
            .orgId(UUID.randomUUID())
            .firstName(FIRST_NAME_VALUE)
            .lastName(LAST_NAME_VALUE)
            .email(EMAIL_VALUE)
            .phone(PHONE_VALUE)
            .userStatus(0);
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + JWT);
        HttpEntity<User> entity = new HttpEntity<>(user, headers);
        ResponseEntity<Void> responseEntity = this.restTemplate.postForEntity("http://localhost:" + port + "/api/user", entity, Void.class);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        KafkaConsumer<String, UserEvents> consumer = new KafkaConsumer<>(consumerProperties);
        consumer.subscribe(Collections.singletonList(Constants.TOPIC_USER_EVENTS));
        ConsumerRecords<String, UserEvents> records = consumer.poll(Duration.ofSeconds(1));

        assertThat(records.count()).isEqualTo(1);
    }
}
