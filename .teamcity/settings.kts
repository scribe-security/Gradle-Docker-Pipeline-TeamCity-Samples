import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot


/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2024.07"

project {
    subProject(SubProject)
}

object SubProject : Project({
    name = "ValintDemo"
    buildType(TestSuite1)
    buildType(TestReport)
    buildType(TestSuite2)
    buildType(BuildApp)
    buildType(BuildDockerImage)
})

object TestSuite1 : BuildType({
    name = "Test Suite 1"
    vcs {
        root(DslContext.settingsRoot)
    }
    steps {
        gradle {
            tasks = "test"
            gradleParams = "-Dorg.gradle.java.home=%env.JDK_11_0%"
            workingDir = "test1"
        }
    }
})

object TestSuite2 : BuildType({
    name = "Test Suite 2"
    vcs {
        root(DslContext.settingsRoot)
    }
    steps {
        gradle {
            tasks = "test"
            jdkHome = "%env.JDK_11%"
            workingDir = "test2"
        }
    }
})

object TestReport : BuildType({
    name = "TestReport"
    type = BuildTypeSettings.Type.COMPOSITE
})

object BuildApp : BuildType({
    name = "Build Application"
    artifactRules = "build/libs/todo.jar"
    vcs {
        root(DslContext.settingsRoot)
    }
    steps {
        gradle {
            tasks = "clean build"
            jdkHome = "%env.JDK_11%"
        }
    }
})

object BuildDockerImage : BuildType({
    name = "Build Docker Image"
    vcs {
        root(DslContext.settingsRoot)
    }
    params {
        param("env.SCRIBE_TOKEN", "%env.SCRIBE_TOKEN%")
    }
    
    }
    steps {
        dockerCommand {
            commandType = build {
                source = file {
                    path = "./docker/Dockerfile"
                }
                contextDir = "."
                namesAndTags = "mkjetbrains/todo-backend:%build.number%"
                commandArgs = "--pull"
            }
        }
        script {
            name = "Generate a Docker SBOM"
            scriptContent = "/home/guyc/.scribe/bin/valint bom "+"mkjetbrains/todo-backend:%build.number%" + " -P %SCRIBE_TOKEN%" 
        }    
    }
})
