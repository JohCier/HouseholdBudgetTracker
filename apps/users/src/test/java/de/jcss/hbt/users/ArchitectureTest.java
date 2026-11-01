package de.jcss.hbt.users;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import de.jcss.hbt.test.DddArchitectureRules;
import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Enforces the rules shared with the backend ({@link DddArchitectureRules}: layering, placement and shape of the DDD
 * building blocks, the DDD rules of jMolecules) and that domain events are records with a timestamp.
 */
@AnalyzeClasses(packages = "de.jcss.hbt.users", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final ArchCondition<JavaClass> CARRY_A_TIMESTAMP =
            new ArchCondition<>("carry an Instant timestamp") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean hasTimestamp = javaClass.tryGetField("timestamp")
                            .filter(field -> field.getRawType().isEquivalentTo(Instant.class))
                            .isPresent();
                    events.add(new SimpleConditionEvent(javaClass, hasTimestamp,
                            javaClass.getName() + " has no field timestamp of type Instant"));
                }
            };

    /** Layering and DDD rules shared with the backend (libs/test-support). */
    @ArchTest
    static final ArchRule sharedDddRules = DddArchitectureRules.forBasePackage("de.jcss.hbt.users");

    @ArchTest
    static final ArchRule eventsAreRecordsWithATimestamp = classes()
            .that().implement(DomainEvent.class)
            .should().beRecords()
            .andShould(CARRY_A_TIMESTAMP);
}
