package com.familygrowth;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.test.context.ActiveProfiles; import javax.sql.DataSource;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:familygrowth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate"}) @ActiveProfiles("test")
class FamilyGrowthApplicationTest { @Autowired DataSource dataSource; @Test void contextLoadsWithFlywayAndJpaValidation(){assertThat(dataSource).isNotNull();} }
