package chapman.wattle.id.au.msuserupdate.config;

import chapman.wattle.id.au.msuserupdate.domain.UserEvents;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class IntegrationTestConfig {

    private static final DockerImageName KAFKA_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka:latest");

    private final Logger log = LoggerFactory.getLogger(IntegrationTestConfig.class);

    private KafkaContainer kafkaContainer;

    @Bean
    @Primary
    public KafkaProducer<String, UserEvents> testEventProducer() {
        KafkaProducer<String, UserEvents> ret = new KafkaProducer<>(getProducerProps());

        return ret;
    }

    @Bean
    public KafkaContainer getKafkaContainer() {
        kafkaContainer = new KafkaContainer(KAFKA_IMAGE_NAME);
        kafkaContainer.start();
        return kafkaContainer;
    }

    @Bean
    public Map<String, Object> getProducerProps() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put("key.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        producerProps.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        producerProps.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        producerProps.put("bootstrap.servers", getKafkaContainer().getBootstrapServers());
        producerProps.put("schema.registry.url", "mock://schema_registry");

        return producerProps;
    }

    @Bean
    public Map<String, Object> getConsumerProps() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put("key.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        consumerProps.put("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        consumerProps.put("bootstrap.servers", getKafkaContainer().getBootstrapServers());
        consumerProps.put("schema.registry.url", "mock://schema_registry");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("group.id", "group-produce");
        return consumerProps;
    }
}
