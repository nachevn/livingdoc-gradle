package info.novatec.testit.livingdoc.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class RepositoryDsl {

  public String name

  public Project project

  public String implementation

  public String url

  public String uid

  public File freezeDirectory

  public NamedDomainObjectContainer<RepositorySortFilterDsl> sortfilter

  public RepositoryDsl(String name, Project project) {
    this.name = name
    this.project = project
    this.freezeDirectory = new File("${project.buildDir.path}${File.separator}${project.LIVINGDOC_SOURCESET_NAME}${File.separator}specs")
  }

  public void sortfilter(final Closure configureClosure) {
    sortfilter.configure(configureClosure)
  }
}
