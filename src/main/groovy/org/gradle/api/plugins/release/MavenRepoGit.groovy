package org.gradle.api.plugins.release

import honey.vcs.git.Git
import org.gradle.api.Project

class MavenRepoGit {
  Project project
  Project rootProject

  String repoPath
  String versionPath

  File repoDir
  File versionDir
  Git git

  MavenRepoGit(Project project, Project rootProject, String repoUrl) {
    this.project = project
    this.rootProject = rootProject

    repoPath = "$rootProject.buildDir/$rootProject.name-repo"
    versionPath = "$repoPath/$project.group/$project.name/$project.version"

    repoDir = new File(repoPath)
    versionDir = new File(versionPath)

    git = new Git(repoDir, repoUrl)

    println("checking out repository branch...")
    git.checkoutBranch("repository", false, true)
  }

  def publishToGit(){
    def addPath = versionPath.substring(repoPath.length() + 1)

    println("adding path $addPath ($versionPath) to commit...")

    git.addAll(addPath)

    println git.logShortStatus()

    git.commit("publish version $version")

    println("pushing to git...")

    git.push(true,true)
  }
}