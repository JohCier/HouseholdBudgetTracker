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
import de.jcss.hbt.users.application.shared.ApplicationService;
import de.jcss.hbt.users.domain.shared.AggregateRoot;
import de.jcss.hbt.users.domain.shared.DomainCommand;
import de.jcss.hbt.users.domain.shared.DomainEvent;
import de.jcss.hbt.users.domain.shared.ValueObject;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the layering (infrastructure → application → domain) and the placement and
 * shape of the DDD building blocks, like {@code ArchitectureTest} of the backend.
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

    @ArchTest
    static final ArchRule layersOnlyDependInwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("de.jcss.hbt.users.domain..")
            .layer("Application").definedBy("de.jcss.hbt.users.application..")
            .layer("Infrastructure").definedBy("de.jcss.hbt.users.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = classes()
            .that().resideInAPackage("de.jcss.hbt.users.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("de.jcss.hbt.users.domain..", "java..");

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
    static final ArchRule eventsAreRecords = classes()
            .that().implement(DomainEvent.class)
            .should().beRecords();

    @ArchTest
    static final ArchRule commandsAreRecordsInTheDomain = classes()
            .that().implement(DomainCommand.class)
            .should().beRecords()
            .andShould().resideInAPackage("de.jcss.hbt.users.domain..command..");

    @ArchTest
    static final ArchRule applicationServicesLiveInTheApplicationLayer = classes()
            .that().areAnnotatedWith(ApplicationService.class)
            .should().resideInAPackage("de.jcss.hbt.users.application..");
}
