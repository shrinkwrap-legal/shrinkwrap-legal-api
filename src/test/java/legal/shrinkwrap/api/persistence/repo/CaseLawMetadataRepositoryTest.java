package legal.shrinkwrap.api.persistence.repo;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import legal.shrinkwrap.api.config.TestcontainersConfiguration;
import legal.shrinkwrap.api.persistence.entity.CaseLawMetadataEntity;

@ActiveProfiles("test")
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class CaseLawMetadataRepositoryTest {

    @Autowired
    private CaseLawMetadataRepository repository;


    @Test
    public void test_findAll() {
         CaseLawMetadataEntity entity = new CaseLawMetadataEntity();
         entity.setId(1L);
         repository.save(entity);

         List<CaseLawMetadataEntity> entries = repository.findAll();
         assertThat(entries).isNotNull().isNotEmpty().hasSize(1);
    }
}
