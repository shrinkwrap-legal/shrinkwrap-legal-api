package legal.shrinkwrap.api.persistence.repo;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import legal.shrinkwrap.api.persistence.entity.CaseLawMetadataEntity;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class CaseLawMetadataRepositoryTest {

    @Autowired
    private CaseLawMetadataRepository repository;


    @Test
    public void test_findAll() {
         List<CaseLawMetadataEntity> entries = repository.findAll();
         assertThat(entries).isNotNull().isNotEmpty().hasSize(1);
    }
}
