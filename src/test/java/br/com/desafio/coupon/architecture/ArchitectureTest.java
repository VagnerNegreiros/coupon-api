package br.com.desafio.coupon.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Transforma as decisões de arquitetura em testes: se alguém importar JPA/Spring na application ou
 * criar um "CouponService" genérico, o build quebra.
 */
@AnalyzeClasses(packages = "br.com.desafio.coupon", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "br.com.desafio.coupon.domain..";
    private static final String APPLICATION = "br.com.desafio.coupon.application..";
    private static final String INFRASTRUCTURE = "br.com.desafio.coupon.infrastructure..";

    @ArchTest
    static final ArchRule layersRespectDependencyDirection = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domainIsPureJava = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..");

    @ArchTest
    static final ArchRule applicationIsFrameworkAgnostic = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "javax..", "org.hibernate..", INFRASTRUCTURE);

    @ArchTest
    static final ArchRule outputPortsAreInterfaces = classes()
            .that().resideInAPackage("..application.port..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule useCasesExposeOnlyExecute = classes()
            .that().resideInAPackage("..application.usecase..")
            .should(haveASinglePublicMethodNamedExecute());

    @ArchTest
    static final ArchRule useCasesDependOnlyOnInterfaces = classes()
            .that().resideInAPackage("..application.usecase..")
            .should(haveOnlyInterfaceTypedFields());

    @ArchTest
    static final ArchRule noGenericServices = noClasses()
            .should().haveSimpleNameEndingWith("Service");

    private static ArchCondition<JavaClass> haveASinglePublicMethodNamedExecute() {
        return new ArchCondition<>("have a single public method named execute") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                List<String> publicMethods = javaClass.getMethods().stream()
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .map(JavaMethod::getName)
                        .toList();
                boolean satisfied = publicMethods.equals(List.of("execute"));
                events.add(new SimpleConditionEvent(javaClass, satisfied,
                        "%s has public methods %s".formatted(javaClass.getSimpleName(), publicMethods)));
            }
        };
    }

    private static ArchCondition<JavaClass> haveOnlyInterfaceTypedFields() {
        return new ArchCondition<>("depend only on interfaces") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaField field : javaClass.getFields()) {
                    boolean satisfied = field.getRawType().isInterface();
                    events.add(new SimpleConditionEvent(field, satisfied,
                            "%s.%s is of concrete type %s".formatted(javaClass.getSimpleName(),
                                    field.getName(), field.getRawType().getName())));
                }
            }
        };
    }
}
