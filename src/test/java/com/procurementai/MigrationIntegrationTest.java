package com.procurementai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts a real Postgres container via Testcontainers and verifies:
 * - All Flyway migrations run without errors
 * - Core tables exist with expected structure
 * - Demo seed data is present
 *
 * Run with: mvn test
 */
@SpringBootTest
@Testcontainers
class MigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("procurement_ai_test")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable AWS for tests
        registry.add("app.aws.access-key", () -> "test");
        registry.add("app.aws.secret-key", () -> "test");
        registry.add("app.claude.api-key", () -> "test");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allMigrationsRunCleanly() {
        // Flyway would have thrown on startup if migrations failed
        // This test just verifies we got here
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void coreTablesExist() {
        List<String> expectedTables = List.of(
            "companies", "users", "suppliers", "documents",
            "ocr_results", "quotes", "quote_line_items",
            "extraction_jobs", "comparisons", "comparison_quotes",
            "comparison_line_matches", "audit_log"
        );

        for (String table : expectedTables) {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, table
            );
            assertThat(count)
                .as("Table '%s' should exist", table)
                .isEqualTo(1);
        }
    }

    @Test
    void demoCompanyAndUserSeeded() {
        Integer companyCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM companies WHERE name = 'Apex Construction Ltd'",
            Integer.class
        );
        assertThat(companyCount).isEqualTo(1);

        Integer userCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = 'demo@apexconstruction.co.uk'",
            Integer.class
        );
        assertThat(userCount).isEqualTo(1);

        Integer supplierCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM suppliers WHERE company_id = 'a0000000-0000-0000-0000-000000000001'",
            Integer.class
        );
        assertThat(supplierCount).isEqualTo(3);
    }

    @Test
    void indexesExistForPerformance() {
        // Verify key indexes were created — important for demo performance
        List<String> expectedIndexes = List.of(
            "idx_users_company_id",
            "idx_documents_status",
            "idx_quotes_requires_review",
            "idx_comparisons_company_id",
            "idx_suppliers_name_trgm"
        );

        for (String index : expectedIndexes) {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE schemaname = 'public' AND indexname = ?",
                Integer.class, index
            );
            assertThat(count)
                .as("Index '%s' should exist", index)
                .isEqualTo(1);
        }
    }
}
