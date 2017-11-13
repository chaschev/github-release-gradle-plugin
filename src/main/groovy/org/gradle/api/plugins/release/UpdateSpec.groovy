package org.gradle.api.plugins.release

import org.gradle.api.Project

class UpdateSpec {
  private def bumpVersionRule = { Project project, String version ->
    simpleBump(version)
  }


  List<File> files = []
  List<Project> projects = []

  void file(File f) {
    this.files << f
  }

  void project(Project p) {
    this.projects << p
  }

  void projects(Collection<Project> p) {
    this.projects.addAll(p)
  }


  def getBumpVersionRule() {
    bumpVersionRule = call(bumpVersionRule)
    return bumpVersionRule
  }

  void setBumpVersionRule(final def releaseNotes) {
    this.bumpVersionRule = releaseNotes
  }

  void simpleBump(String version) {
    if (version != "unspecified") {
      String[] split = version.split('\\.')
      def next = (split.last() as int) + 1

      def updated = split[0..-2].join('.')
      updated += ".${next}-SNAPSHOT"
      updated
    }
  }
}
