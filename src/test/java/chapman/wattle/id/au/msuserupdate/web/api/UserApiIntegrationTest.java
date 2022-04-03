package chapman.wattle.id.au.msuserupdate.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import chapman.wattle.id.au.msuserupdate.IntegrationTest;
import chapman.wattle.id.au.msuserupdate.MongoDbTestContainerExtension;
import chapman.wattle.id.au.msuserupdate.TestConstants;
import chapman.wattle.id.au.msuserupdate.config.ApplicationProperties;
import chapman.wattle.id.au.msuserupdate.config.Constants;
import chapman.wattle.id.au.msuserupdate.config.IntegrationTestConfig;
import chapman.wattle.id.au.msuserupdate.domain.UserEvents;
import chapman.wattle.id.au.msuserupdate.security.jwt.JWTFilter;
import chapman.wattle.id.au.msuserupdate.service.api.dto.User;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserModify;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserModify.UpdateTypeEnum;
import chapman.wattle.id.au.msuserupdate.service.api.dto.UserPhoneModify;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;

@IntegrationTest
@Timeout(value = 240, unit = TimeUnit.SECONDS)
@ContextConfiguration(classes = {IntegrationTestConfig.class, ApplicationProperties.class})
public class UserApiIntegrationTest {

  private final Logger log = LoggerFactory.getLogger(UserApiIntegrationTest.class);

  private static final String PHONE_VALUE = "0123456789";
  private static final String EMAIL_VALUE = "test@test.com";
  private static final String FIRST_NAME_VALUE = "FirstName";
  private static final String LAST_NAME_VALUE = "LastName";
  private static final String USER_NAME_VALUE = "somebloke";
  private static final String JWT =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJhdXRoIjoibWUifQ.4UtUT1uT4sSLFt4k1wYnIIZ7hDSTU9Wu55IuJxDdfBs";

  @Autowired private KafkaContainer kafkaContainer;

  @Resource(name = "getConsumerProps")
  private Map<String, Object> consumerProperties;

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    createTopics(Constants.TOPIC_USER_EVENTS);
    insertData();
  }

  private void createTopics(String... topics) {
    var newTopics =
        Arrays.stream(topics)
            .map(topic -> new NewTopic(topic, 1, (short) 1))
            .collect(Collectors.toList());
    AdminClient admin =
        AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getMappedKafkaUrl()));
    admin.createTopics(newTopics);
  }

  private void insertData() {
    MongoClient mongo =
        MongoClients.create(
            ((MongoDBContainer) MongoDbTestContainerExtension.getThreadContainer().get())
                .getReplicaSetUrl());
    MongoDatabase db = mongo.getDatabase("users");
    MongoCollection<Document> collection = db.getCollection("userEvents");
    collection.drop();
    collection.insertOne(TestConstants.CREATED_ENTITY_DOCUMENT);
  }

  private String getMappedKafkaUrl() {
    Integer mappedPort = kafkaContainer.getMappedPort(KafkaContainer.KAFKA_PORT);
    return String.format("%s:%d", "localhost", mappedPort);
  }

  @Test
  void addUser() throws Exception {
    KafkaConsumer<String, UserEvents> consumer = new KafkaConsumer<>(consumerProperties);
    consumer.subscribe(Collections.singletonList(Constants.TOPIC_USER_EVENTS));

    User user =
        new User()
            .username(USER_NAME_VALUE)
            .orgId(UUID.randomUUID())
            .firstName(FIRST_NAME_VALUE)
            .lastName(LAST_NAME_VALUE)
            .email(EMAIL_VALUE)
            .phone(PHONE_VALUE);
    HttpHeaders headers = new HttpHeaders();
    headers.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + JWT);
    HttpEntity<User> entity = new HttpEntity<>(user, headers);
    ResponseEntity<Void> responseEntity =
        this.restTemplate.postForEntity(
            "http://localhost:" + port + "/api/user", entity, Void.class);
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

    ConsumerRecords<String, UserEvents> records = consumer.poll(Duration.ofSeconds(1));
    assertThat(records.count()).isGreaterThan(0);
    consumer.close();
  }

  @Test
  void getUser() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + JWT);
    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

    HttpEntity<Void> entity = new HttpEntity<>(null, headers);
    ResponseEntity<User> responseEntity =
        this.restTemplate.exchange(
            "http://localhost:" + port + "/api/user/" + TestConstants.USER_ID.toString(),
            HttpMethod.GET,
            entity,
            User.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(TestConstants.TEST_USER, responseEntity.getBody().getUsername());
  }

  @Test
  void modifyUser() throws Exception {
    KafkaConsumer<String, UserEvents> consumer = new KafkaConsumer<>(consumerProperties);
    consumer.subscribe(Collections.singletonList(Constants.TOPIC_USER_EVENTS));

    UserModify phone =
        new UserPhoneModify().phone(PHONE_VALUE).updateType(UpdateTypeEnum.USERPHONE);
    HttpHeaders headers = new HttpHeaders();
    headers.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + JWT);
    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    HttpEntity<UserModify> entity = new HttpEntity<>(phone, headers);

    ResponseEntity<Void> responseEntity =
        this.restTemplate.exchange(
            "http://localhost:" + port + "/api/user/" + TestConstants.USER_ID.toString(),
            HttpMethod.PATCH,
            entity,
            Void.class);
    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    ConsumerRecords<String, UserEvents> records = consumer.poll(Duration.ofSeconds(1));
    assertThat(records.count()).isGreaterThan(0);
    consumer.close();
  }

  @Test
  void deleteUser() throws Exception {
    KafkaConsumer<String, UserEvents> consumer = new KafkaConsumer<>(consumerProperties);
    consumer.subscribe(Collections.singletonList(Constants.TOPIC_USER_EVENTS));

    HttpHeaders headers = new HttpHeaders();
    headers.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + JWT);
    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    HttpEntity<Void> entity = new HttpEntity<>(null, headers);

    ResponseEntity<Void> responseEntity =
        this.restTemplate.exchange(
            "http://localhost:" + port + "/api/user/" + TestConstants.USER_ID.toString(),
            HttpMethod.DELETE,
            entity,
            Void.class);
    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    ConsumerRecords<String, UserEvents> records = consumer.poll(Duration.ofSeconds(1));
    assertThat(records.count()).isGreaterThan(0);
    consumer.close();
  }
}
