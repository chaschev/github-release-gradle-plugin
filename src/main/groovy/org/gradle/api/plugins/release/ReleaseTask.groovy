package org.gradle.api.plugins.release

import org.eclipse.jgit.api.Git
import org.gradle.StartParameter
import org.gradle.api.Project
import org.gradle.api.plugins.github.DraftReleaseTask
import org.gradle.api.tasks.GradleBuild
import org.gradle.initialization.GradleLauncherFactory
import org.gradle.util.ConfigureUtil

import javax.inject.Inject


class ReleaseTask extends GradleBuild {
  public static final String GROUP_RELEASE = 'Github Release'
  def String tagPrefix = "r"
  def String releaseVersion
  def String nextVersion
  def boolean tagRelease = true
  def boolean createGitHubRelease = true
  def boolean pushChangeToGit = true
  def boolean publishToGit = true

  def Git git
  MavenRepoGit mavenRepoGit
  UpdateSpec update = new UpdateSpec()
  GithubReleaseSpec githubRelease = new GithubReleaseSpec()
  DraftReleaseTask ghReleaseTask
  String remote

  @Inject
  ReleaseTask(StartParameter currentBuild, GradleLauncherFactory gradleLauncherFactory) {
    super(currentBuild, gradleLauncherFactory)
    group = GROUP_RELEASE
    description = 'Verify project, release, and update version to next.'
    ghReleaseTask = project.task('draftGhRelease', type: DraftReleaseTask)

    tasks = [
      'updateToReleaseVersion',
      'pushToRemote',
      'updateToNextVersion',
    ]

    ghReleaseTask.releaseTask = this

    git = Git.open(new File('.'))
    remote = git.repository.config.getString('remote', 'origin', 'url')

    mavenRepoGit = new MavenRepoGit(project, project, remote)

    project.task('updateToReleaseVersion', group: 'release',
      description: 'Updates version to release variant.') << this.&updateToReleaseVersion

    project.task('pushToRemote', group: 'release',
      description: 'Pushes changes to remote repository.') << this.&pushToRemote

    project.task('publishToGit', group: 'release',
      description: 'Publishes Java build artifacts to Git') << this.&publishToGit

    def updateToNextVersion = project.task('updateToNextVersion', group: 'release',
      description: 'Updates version to next, using x.x.x+1 pattern.') << this.&updateToNextVersion

    updateToNextVersion.dependsOn(ghReleaseTask)
  }

  def updateToReleaseVersion() {
    updateVersions(project.version, releaseVersion)

    git.commit()
      .setMessage("Release $releaseVersion")
      .call()

    if (tagRelease) {
      git.tag()
        .setName(tagName())
        .setMessage("Release ${releaseVersion}")
        .call()
    }
  }

  def tagName() {
    tagPrefix + releaseVersion
  }

  def updateToNextVersion() {
    updateVersions(releaseVersion, nextVersion)

    git.commit()
      .setMessage("Update to next development version: ${nextVersion}")
      .call()
  }

  def updateVersions(oldVersion, String newVersion) {
    update.files.each {
      project.ant.replaceregexp(file: it, match: oldVersion, replace: newVersion)
      git.add()
        .addFilepattern(it.path)
        .call()
    }
    update.projects*.version = newVersion
  }

  def pushToRemote() {
    if (pushChangeToGit) {
      git.push()
        .call()
    }
  }

  def publishToGit() {
    if (publishToGit) {
      mavenRepoGit.publishToGit()
    }
  }

  void githubRelease(Closure closure) {
    ConfigureUtil.configure(closure, this.githubRelease)
  }

  void tagPrefix(value) {
    tagPrefix = call(value)
  }

  void version(final def block) {
    this.releaseVersion = call(block) - '-SNAPSHOT'
    nextVersion = update.bumpVersionRule(project, releaseVersion)
  }

  def pushChanges(boolean create) {
    pushChangeToGit = create;
  }

  def createRelease(boolean create) {
    createGitHubRelease = create;
  }

  def createTag(boolean create) {
    tagRelease = create
  }

  def bumpVersion(String old) {


  }

  void update(Closure closure) {
    ConfigureUtil.configure(closure, this.update)
  }

  private static Object call(def c) {
    c instanceof Closure ? c.call() : c
  }
}
