package de.jcss.hbt.test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jmolecules.architecture.cqrs.Command;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.event.types.DomainEvent;

import java.util.List;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The architecture rules both services share (.claude/rules/ddd.md): the layering (infrastructure → application →
 * domain, declared per package as jMolecules onion rings), the placement and shape of the DDD building blocks and the
 * DDD rules of jMolecules. Each service's {@code ArchitectureTest} checks them via {@link #forBasePackage(String)} and
 * adds its own rules (entities, read side, events).
 */
public final class DddArchitectureRules {

    private static final DescribedPredicate<CanBeAnnotated> DOMAIN_OBJECT = annotatedWith(AggregateRoot.class)
            .or(annotatedWith(Entity.class))
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

    private DddArchitectureRules() {
    }

    /** All shared rules for the service whose classes lie under the base package, e.g. {@code de.jcss.hbt.users}. */
    public static ArchRule forBasePackage(String basePackage) {
        String domain = basePackage + ".domain..";
        List<ArchRule> rules = List.of(
                // The rings are declared on the top-level packages (package-info), so sub-packages inherit them
                JMoleculesArchitectureRules.ensureOnionSimple(
                        StereotypeLookup.defaultLookup().withParentPackageTraversal()),
                JMoleculesDddRules.all(),
                classes()
                        .that().resideInAPackage(domain)
                        .should().onlyDependOnClassesThat().resideInAnyPackage(domain, "org.jmolecules..", "java.."),
                classes()
                        .that().areAnnotatedWith(AggregateRoot.class)
                        .or().areAnnotatedWith(Entity.class)
                        .or().areAnnotatedWith(ValueObject.class)
                        .or().implement(DomainEvent.class)
                        .should().resideInAPackage(domain),
                classes()
                        .that().areAnnotatedWith(ValueObject.class)
                        .should().resideInAPackage(basePackage + ".domain.*.valueobject"),
                classes()
                        .that().resideInAPackage(basePackage + ".domain..valueobject..")
                        .and().areTopLevelClasses()
                        .should().beAnnotatedWith(ValueObject.class),
                noClasses()
                        .that().areAnnotatedWith(ValueObject.class)
                        .should().dependOnClassesThat().areAnnotatedWith(AggregateRoot.class)
                        .orShould().dependOnClassesThat().areAnnotatedWith(Entity.class),
                methods()
                        .that().arePublic()
                        .and().areDeclaredInClassesThat(DOMAIN_OBJECT)
                        .should().haveNameNotMatching("set[A-Z].*"),
                fields()
                        .that().areDeclaredInClassesThat(DOMAIN_OBJECT)
                        .should().beFinal(),
                classes()
                        .that().areAnnotatedWith(ValueObject.class)
                        .should().beRecords()
                        .orShould().beEnums(),
                classes()
                        .that().areAnnotatedWith(AggregateRoot.class)
                        .or().areAnnotatedWith(Entity.class)
                        .should(DECLARE_EQUALS_AND_HASH_CODE),
                classes()
                        .that().areAnnotatedWith(Command.class)
                        .should().beRecords()
                        .andShould().resideInAPackage(basePackage + ".domain..command.."));
        return CompositeArchRule.of(rules).as("shared DDD architecture rules of " + basePackage);
    }
}
