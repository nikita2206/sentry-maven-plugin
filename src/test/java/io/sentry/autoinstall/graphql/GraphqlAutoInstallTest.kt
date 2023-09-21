package io.sentry.autoinstall.graphql

import basePom
import createExtensionInFolder
import io.sentry.autoinstall.SentryInstaller
import org.apache.maven.shared.verifier.VerificationException
import org.apache.maven.shared.verifier.Verifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class GraphqlAutoInstallTest {

    @TempDir()
    lateinit var file: File

    fun getPOM(
        installGraphql: Boolean = true,
        graphqlVersion: String = "2.0.0",
        withExtension: Boolean = true,
        sentryGraphqlVersion: String = "6.25.2",
        installedSentryVersion: String? = null
    ): String {
        var dependencies =
            """
                    <dependency>
                        <groupId>com.graphql-java</groupId>
                        <artifactId>graphql-java</artifactId>
                        <version>$graphqlVersion</version>
                    </dependency>
                """.trimIndent()

        if (!installGraphql) {
            dependencies = dependencies.plus(
                """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-graphql</artifactId>
                        <version>$sentryGraphqlVersion</version>
                    </dependency>
                """.trimIndent()
            )
        }

        val pomContent = basePom(dependencies, installedSentryVersion)


        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        if (withExtension) {
            createExtensionInFolder(file)
        }

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when sentry-graphql is a direct dependency logs a message and does nothing`() {
        val path = getPOM(false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-graphql won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry-graphql with info message`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-graphql was successfully installed with version: ${SentryInstaller.SENTRY_VERSION}")
        verifier.verifyFilePresent("target/lib/sentry-graphql-${SentryInstaller.SENTRY_VERSION}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `auto-installed sentry-graphql version matches sentry version`() {
        val sentryVersion = "6.25.2"
        val path = getPOM(installedSentryVersion = sentryVersion)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-graphql was successfully installed with version: ${sentryVersion}")
        verifier.verifyFilePresent("target/lib/sentry-graphql-${sentryVersion}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
