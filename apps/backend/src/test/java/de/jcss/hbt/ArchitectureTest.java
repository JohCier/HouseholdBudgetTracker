package de.jcss.hbt;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import de.jcss.hbt.domain.householdplan.HouseholdPlanRepository;
import de.jcss.hbt.domain.householdplan.overview.HouseholdPlanOverviewQueryHandler;
import de.jcss.hbt.domain.shared.QueryHandler;
import de.jcss.hbt.domain.shared.TimestampedDomainEvent;
import de.jcss.hbt.test.DddArchitectureRules;
import org.jmolecules.architecture.cqrs.QueryModel;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.event.types.DomainEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the rules shared with the users service ({@link DddArchitectureRules}: layering, placement and shape of
 * the DDD building blocks, the DDD rules of jMolecules) and those of the backend alone: entities next to their
 * aggregate root, timestamped events and the separation of the write side from the read side.
 */
@AnalyzeClasses(packages = "de.jcss.hbt", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final ArchCondition<JavaClass> RESIDE_NEXT_TO_AN_AGGREGATE_ROOT =
            new ArchCondition<>("reside in the package of an aggregate root") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean nextToRoot = javaClass.getPackage().getClasses().stream()
                            .anyMatch(other -> other.isAnnotatedWith(AggregateRoot.class));
                    events.add(new SimpleConditionEvent(javaClass, nextToRoot,
                            javaClass.getName() + " does not reside in the package of an aggregate root"));
                }
            };

    /** Layering and DDD rules shared with the users service (libs/test-support). */
    @ArchTest
    static final ArchRule sharedDddRules = DddArchitectureRules.forBasePackage("de.jcss.hbt");

    /** Keeps the package-private methods of an entity out of reach of everything but its root. */
    @ArchTest
    static final ArchRule entitiesLiveNextToTheirAggregateRoot = classes()
            .that().areAnnotatedWith(Entity.class)
            .should(RESIDE_NEXT_TO_AN_AGGREGATE_ROOT);

    @ArchTest
    static final ArchRule readModelsAreRecords = classes()
            .that().areAnnotatedWith(QueryModel.class)
            .should().beRecords();

    @ArchTest
    static final ArchRule eventsAreTimestampedRecords = classes()
            .that().implement(DomainEvent.class)
            .and().areNotInterfaces()
            .should().beRecords()
            .andShould().implement(TimestampedDomainEvent.class);

    @ArchTest
    static final ArchRule readModelsAndQueryHandlersAreDeclaredInTheDomain = classes()
            .that().areAnnotatedWith(QueryModel.class)
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
            .orShould().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .orShould().dependOnClassesThat().areAssignableTo(HouseholdPlanRepository.class);

    @ArchTest
    static final ArchRule writeSideDoesNotUseTheReadSide = noClasses()
            .that().resideInAPackage("de.jcss.hbt.domain..")
            .and().resideOutsideOfPackage("de.jcss.hbt.domain..overview..")
            .or().implement(HouseholdPlanRepository.class)
            .should().dependOnClassesThat().resideInAPackage("de.jcss.hbt.domain..overview..");
}
