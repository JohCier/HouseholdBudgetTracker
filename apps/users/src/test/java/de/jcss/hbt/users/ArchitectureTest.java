package de.jcss.hbt.users;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jmolecules.architecture.cqrs.Command;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Enforces the layering (infrastructure → application → domain, declared per package as
 * jMolecules onion rings), the placement and shape of the DDD building blocks and
 * the DDD rules of jMolecules, like {@code ArchitectureTest} of the backend.
 */
@AnalyzeClasses(packages = "de.jcss.hbt.users", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final DescribedPredicate<CanBeAnnotated> DOMAIN_OBJECT = annotatedWith(AggregateRoot.class)
            .or(annotatedWith(ValueObject.class))
            .as("aggregate roots or value objects");

    private static final ArchCondition<JavaClass> DECLARE_EQUALS_AND_HASH_CODE =
            new ArchCondition<>("declare equals and hashCode") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean declaresBoth = javaClass.tryGetMethod("equals", Object.class).isPresent()
                            && javaClass.tryGetMethod("hashCode").isPresent();
                    events.add(new SimpleConditionEvent(javaClass, declaresBoth,
                            javaClass.getName() + " does not declare both equals and hashCode"));
                }
            };

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

    /** The rings are declared on the top-level packages (package-info), so sub-packages inherit them. */
    @ArchTest
    static final ArchRule layersOnlyDependInwards = JMoleculesArchitectureRules.ensureOnionSimple(
            StereotypeLookup.defaultLookup().withParentPackageTraversal());

    @ArchTest
    static final ArchRule dddRulesOfJMolecules = JMoleculesDddRules.all();

    @ArchTest
    static final ArchRule domainIsFrameworkFree = classes()
            .that().resideInAPackage("de.jcss.hbt.users.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("de.jcss.hbt.users.domain..", "org.jmolecules..", "java..");

    @ArchTest
    static final ArchRule domainBuildingBlocksLiveInTheDomain = classes()
            .that().areAnnotatedWith(AggregateRoot.class)
            .or().areAnnotatedWith(ValueObject.class)
            .or().implement(DomainEvent.class)
            .should().resideInAPackage("de.jcss.hbt.users.domain..");

    @ArchTest
    static final ArchRule domainObjectsHaveNoPublicSetters = methods()
            .that().arePublic()
            .and().areDeclaredInClassesThat(DOMAIN_OBJECT)
            .should().haveNameNotMatching("set[A-Z].*");

    @ArchTest
    static final ArchRule domainObjectsOnlyHaveFinalFields = fields()
            .that().areDeclaredInClassesThat(DOMAIN_OBJECT)
            .should().beFinal();

    @ArchTest
    static final ArchRule valueObjectsAreRecordsOrEnums = classes()
            .that().areAnnotatedWith(ValueObject.class)
            .should().beRecords()
            .orShould().beEnums();

    @ArchTest
    static final ArchRule aggregatesDefineEqualityByTheirId = classes()
            .that().areAnnotatedWith(AggregateRoot.class)
            .should(DECLARE_EQUALS_AND_HASH_CODE);

    @ArchTest
    static final ArchRule eventsAreRecordsWithATimestamp = classes()
            .that().implement(DomainEvent.class)
            .should().beRecords()
            .andShould(CARRY_A_TIMESTAMP);

    @ArchTest
    static final ArchRule commandsAreRecordsInTheDomain = classes()
            .that().areAnnotatedWith(Command.class)
            .should().beRecords()
            .andShould().resideInAPackage("de.jcss.hbt.users.domain..command..");
}
