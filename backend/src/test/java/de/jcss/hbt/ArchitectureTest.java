package de.jcss.hbt;

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
import de.jcss.hbt.application.shared.ApplicationService;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverviewQueryHandler;
import de.jcss.hbt.domain.shared.AggregateRoot;
import de.jcss.hbt.domain.shared.DomainCommand;
import de.jcss.hbt.domain.shared.DomainEntity;
import de.jcss.hbt.domain.shared.DomainEvent;
import de.jcss.hbt.domain.shared.QueryHandler;
import de.jcss.hbt.domain.shared.ReadModel;
import de.jcss.hbt.domain.shared.ValueObject;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the layering (infrastructure → application → domain), the placement and shape
 * of the DDD building blocks, and the separation of the write side from the read side.
 */
@AnalyzeClasses(packages = "de.jcss.hbt", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final DescribedPredicate<CanBeAnnotated> DOMAIN_OBJECT = annotatedWith(AggregateRoot.class)
            .or(annotatedWith(DomainEntity.class))
            .or(annotatedWith(ValueObject.class))
            .as("aggregate roots, entities or value objects");

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
            .layer("Domain").definedBy("de.jcss.hbt.domain..")
            .layer("Application").definedBy("de.jcss.hbt.application..")
            .layer("Infrastructure").definedBy("de.jcss.hbt.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = classes()
            .that().resideInAPackage("de.jcss.hbt.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("de.jcss.hbt.domain..", "java..");

    @ArchTest
    static final ArchRule domainBuildingBlocksLiveInTheDomain = classes()
            .that().areAnnotatedWith(AggregateRoot.class)
            .or().areAnnotatedWith(DomainEntity.class)
            .or().areAnnotatedWith(ValueObject.class)
            .or().implement(DomainEvent.class)
            .should().resideInAPackage("de.jcss.hbt.domain..");

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
    static final ArchRule entitiesDefineEqualityByTheirId = classes()
            .that().areAnnotatedWith(AggregateRoot.class)
            .or().areAnnotatedWith(DomainEntity.class)
            .should(DECLARE_EQUALS_AND_HASH_CODE);

    @ArchTest
    static final ArchRule readModelsAreRecords = classes()
            .that().areAnnotatedWith(ReadModel.class)
            .should().beRecords();

    @ArchTest
    static final ArchRule eventsAreRecords = classes()
            .that().implement(DomainEvent.class)
            .should().beRecords();

    @ArchTest
    static final ArchRule commandsAreRecordsInTheDomain = classes()
            .that().implement(DomainCommand.class)
            .should().beRecords()
            .andShould().resideInAPackage("de.jcss.hbt.domain..command..");

    @ArchTest
    static final ArchRule applicationServicesLiveInTheApplicationLayer = classes()
            .that().areAnnotatedWith(ApplicationService.class)
            .should().resideInAPackage("de.jcss.hbt.application..");

    @ArchTest
    static final ArchRule readModelsAndQueryHandlersAreDeclaredInTheDomain = classes()
            .that().areAnnotatedWith(ReadModel.class)
            .or().areAnnotatedWith(QueryHandler.class)
            .should().resideInAPackage("de.jcss.hbt.domain..overview..");

    @ArchTest
    static final ArchRule queryHandlersAreInterfacesImplementedInPersistence = classes()
            .that().areAnnotatedWith(QueryHandler.class)
            .should().beInterfaces()
            .andShould().onlyHaveDependentClassesThat().resideInAnyPackage(
                    "de.jcss.hbt.domain..overview..", "de.jcss.hbt.application..",
                    "de.jcss.hbt.infrastructure.persistence..");

    @ArchTest
    static final ArchRule readSideDoesNotUseTheWriteSide = noClasses()
            .that().resideInAPackage("de.jcss.hbt.domain..overview..")
            .or().implement(HouseholdPlanOverviewQueryHandler.class)
            .should().dependOnClassesThat().areAnnotatedWith(AggregateRoot.class)
            .orShould().dependOnClassesThat().areAnnotatedWith(DomainEntity.class)
            .orShould().dependOnClassesThat().areAssignableTo(HouseholdPlanRepository.class);

    @ArchTest
    static final ArchRule writeSideDoesNotUseTheReadSide = noClasses()
            .that().resideInAPackage("de.jcss.hbt.domain..")
            .and().resideOutsideOfPackage("de.jcss.hbt.domain..overview..")
            .or().implement(HouseholdPlanRepository.class)
            .should().dependOnClassesThat().resideInAPackage("de.jcss.hbt.domain..overview..");
}
