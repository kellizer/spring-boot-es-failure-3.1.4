package es.playground.springesplayground.vault

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.stereotype.Repository
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName


@Testcontainers
@SpringBootTest(classes = [TestApp::class], properties = ["logging.level.org.testcontainers=debug"])
@ContextConfiguration(initializers = [SetupContainers::class])
class VaultDocumentTest(@Autowired private val vaultDocumentRepo: VaultDocumentRepo) {
    @Test
    fun simpleTest() {
        println("Count Call Works-->${vaultDocumentRepo.count()}")//this works - returns 0
        println("findAll Works -->${vaultDocumentRepo.findAll().toList().size}") //this works - returns 0
        vaultDocumentRepo.save(VaultDocument("Test101")) //save fails with error '[es/index] failed: [null] Incorrect HTTP method for uri [/vaultdoc/_doc/?refresh=false] and method [PUT], allowed: [POST]'
    }
}

@Document(indexName = "vaultdoc")
class VaultDocument(
    @Field(type = FieldType.Text, store = true, fielddata = true)
    val vaultDocumentName: String,
    @org.springframework.data.annotation.Id
    var id: String = "",
)

@Repository
interface VaultDocumentRepo : ElasticsearchRepository<VaultDocument, String> {
    fun findByVaultDocumentName(vaultDocumentName: String): VaultDocument?
}


@SpringBootApplication
@EnableElasticsearchRepositories
class TestApp


class SetupContainers : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {

        elasticsearchContainer.withEnv(
            mapOf(
                "xpack.security.enabled" to "false",
                "xpack.security.http.ssl.enabled" to "false",
                "action.destructive_requires_name" to "false",
                "reindex.remote.whitelist" to "localhost:9200"
            )
        )
        elasticsearchContainer.start()
        TestPropertyValues.of(
            "spring.elasticsearch.uris=http://${elasticsearchContainer.httpHostAddress}",
        ).applyTo(applicationContext.environment)
    }


    companion object {
        @Container
        val elasticsearchContainer: ElasticsearchContainer = ElasticsearchContainer(
            DockerImageName
                .parse("docker.elastic.co/elasticsearch/elasticsearch")
                .withTag("8.10.0")

        )
    }
}
