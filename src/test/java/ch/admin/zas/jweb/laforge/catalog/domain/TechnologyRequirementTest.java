package ch.admin.zas.jweb.laforge.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TechnologyRequirementTest {

    @Test
    void nameOnlyIsValidAndSurvivesJsonRoundTrip() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var mapper = JsonMapper.builder().build();
            var technology = mapper.readValue("{\"technology\":\"Java\"}", TechnologyRequirement.class);
            assertThat(factory.getValidator().validate(technology)).isEmpty();
            assertThat(technology.minimumVersion()).isNull();
            assertThat(technology.featureStatus()).isNull();
            assertThat(mapper.readValue(mapper.writeValueAsString(technology), TechnologyRequirement.class)).isEqualTo(technology);
        }
    }

    @Test
    void suppliedValuesRemainValidated() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new TechnologyRequirement(" ", null, null, null))).isNotEmpty();
            assertThat(validator.validate(new TechnologyRequirement("Java", " ", null, null))).isNotEmpty();
            assertThat(validator.validate(new TechnologyRequirement("Java", "25", FeatureStatus.PREVIEW, "Notes"))).isEmpty();
        }
    }
}
