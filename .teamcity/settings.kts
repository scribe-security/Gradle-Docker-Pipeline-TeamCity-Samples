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

version = "2024.12"

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
    params {
        param("env.SCRIBE_TOKEN", "")
        param("env.APP_ID", "12b4f8a4-b9a2-4e89-a886-3064b9e98ef6")
        param("env.CLIENT_SECRET", "")
        param("env.TENANT_ID", "baa95f6e-53b4-4508-a093-6fce300256b4")
    }
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
        script {
            name = "Login To Azure Key Vault"
            
            scriptContent = """
                echo '----------Key Vault---------------'
                C:\Users\ContainerUser\Azure\CLI2\wbin\az login --service-principal -u %env.APP_ID% -p %env.CLIENT_SECRET% --tenant %env.TENANT_ID%
                
                
            """.trimIndent()
        }


        script {
            name = "Generate a Source SBOM 1"
            
            scriptContent = """
                echo '-----------Valint-----------------'
                C:\Users\ContainerUser\valint bom dir:$(pwd) -vv %env.SCRIBE_TOKEN% --product-key Team-City-Demo --product-version 1.0.4 -o attest --kms azurekms://guys-keys.vault.azure.net/code-signer-one
                
            """.trimIndent()
        }
    }
})


object BuildDockerImage : BuildType({
    name = "Build Docker Image"
    vcs {
        root(DslContext.settingsRoot)
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
            name = "Generate a Docker SBOM 12"
            
            scriptContent = """
                az login --service-principal -u %env.APP_ID% -p %env.CLIENT_SECRET% --tenant %env.TENANT_ID%
                /home/guyc/.scribe/bin/valint bom mkjetbrains/todo-backend:%build.number% -vv %env.SCRIBE_TOKEN% --product-key Team-City-Demo --product-version 1.0.4 -o attest --kms azurekms://guys-keys.vault.azure.net/code-signer-one
                printenv
            """.trimIndent()
        }    
    }
})


