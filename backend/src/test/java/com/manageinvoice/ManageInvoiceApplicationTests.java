package com.manageinvoice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.manageinvoice.testsupport.SupabaseDatabaseTest;

@SpringBootTest
@SupabaseDatabaseTest
class ManageInvoiceApplicationTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextLoads() {
    }

    @Test
    void migratedTablesHaveRowLevelSecurityEnabled() {
        Integer unprotectedTables = jdbc.queryForObject("""
            SELECT COUNT(*) FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = current_schema() AND c.relkind IN ('r', 'p')
                            AND c.relname <> 'flyway_schema_history' AND NOT c.relrowsecurity
            """, Integer.class);
        assertEquals(0, unprotectedTables);
    }
}
