package grails.buildtestdata

import grails.buildtestdata.mixin.Build
import grails.testing.gorm.DataTest
import grails.testing.spock.OnceBefore
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Unit tests should implement this trait to add build-test-data functionality
 */
@CompileStatic
@SuppressWarnings("GroovyUnusedDeclaration")
trait UnitTestDataBuilder extends DataTest implements TestDataBuilder {

    @Override
    void mockDomains(Class<?>... domainClassesToMock) {
        mockDependencyGraph(domainClassesToMock)
        //add build method
        Class[] domainClasses = dataStore.mappingContext.getPersistentEntities()*.javaClass
        addBuildMetaMethods(domainClasses)
    }

    @CompileDynamic
    void addBuildMetaMethods(Class<?>... entityClasses){
        entityClasses.each { ec ->
            def mc = ec.metaClass
            mc.static.build = {
                return TestData.build(ec)
            }
            mc.static.build = { Map data ->
                return TestData.build(ec, data)
            }
        }
    }

    @OnceBefore
    void handleBuildAnnotation() {
        Build build = this.getClass().getAnnotation(Build)
        if (build) {
            mockDomains(build.value())
        }
    }

    void mockDependencyGraph(Class<?>... domainClassesToMock) {
        mockDependencyGraph([] as Set, DomainUtil.expandSubclasses(domainClassesToMock))
    }

    void mockDependencyGraph(Set<Class> mockedList, Class<?>... domainClassesToMock) {
        // First mock these domains so they are registered with Grails
        super.mockDomains(domainClassesToMock)
        mockedList.addAll(domainClassesToMock)

        Set<Class> requiredClasses = domainClassesToMock.collectMany { Class clazz ->
            // For domain instance building, we only care about concrete classes
            if (!DomainUtil.isAbstract(clazz)) {
                DomainInstanceBuilder builder = DomainInstanceRegistry.lookup(clazz)
                builder.requiredDomainClasses
            }
            else {
                []
            }
        } as Set<Class>

        // Remove any that we've already seen
        requiredClasses.removeAll(mockedList)
        if (requiredClasses) {
            mockDependencyGraph(mockedList, requiredClasses as Class[])
        }
    }
}