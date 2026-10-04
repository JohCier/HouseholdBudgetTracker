package de.jcss.hbt;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/** Enforces the hexagonal dependency rules: adapters → application → domain. */
@AnalyzeClasses(packages = "de.jcss.hbt", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layersOnlyDependInwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("de.jcss.hbt.domain..")
            .layer("Application").definedBy("de.jcss.hbt.application..")
            .layer("Adapters").definedBy("de.jcss.hbt.adapter..")
            .whereLayer("Adapters").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = classes()
            .that().resideInAPackage("de.jcss.hbt.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("de.jcss.hbt.domain..", "java..");

    @ArchTest
    static final ArchRule adaptersDoNotKnowEachOther = classes()
            .that().resideInAPackage("de.jcss.hbt.adapter.in..")
            .should().onlyDependOnClassesThat().resideOutsideOfPackage("de.jcss.hbt.adapter.out..");
}
