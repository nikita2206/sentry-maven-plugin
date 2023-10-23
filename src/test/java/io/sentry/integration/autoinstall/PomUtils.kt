import io.sentry.autoinstall.Constants
import org.apache.maven.it.Verifier
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

fun installMavenWrapper(
    file: File,
    version: String,
) {
    val mavenVersionToUse = System.getProperty("maven.test.version") ?: version

    val emptyPom =
        """
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
            <groupId>io.sentry.autoinstall</groupId>
            <artifactId>installmaven</artifactId>
            <version>1.0-SNAPSHOT</version>
        </project>
        """.trimIndent()

    Files.write(Path("${file.absolutePath}/pom.xml"), emptyPom.toByteArray(), StandardOpenOption.CREATE)
    Verifier(file.absolutePath).apply {
        addCliOption("-N")
        addCliOption("-Dmaven=$mavenVersionToUse")
        executeGoal("wrapper:wrapper")
    }
    Files.delete(Path("${file.absolutePath}/pom.xml"))
}

fun basePom(
    dependencies: String?,
    installedSentryVersion: String? = null,
    pluginConfiguration: String = "",
): String {
    val sentryPlugin =
        """
        <plugin>
            <groupId>io.sentry</groupId>
            <artifactId>sentry-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <extensions>true</extensions>
            $pluginConfiguration
        </plugin>
        """.trimIndent()

    val sentryDependency =
        installedSentryVersion?.let {
            """
            <dependency>
                <groupId>${Constants.SENTRY_GROUP_ID}</groupId>
                <artifactId>${Constants.SENTRY_ARTIFACT_ID}</artifactId>
                <version>$it</version>
            </dependency>
            """.trimIndent()
        } ?: ""

    return """
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <groupId>io.sentry.autoinstall</groupId>
            <artifactId>installsentry</artifactId>
            <version>1.0-SNAPSHOT</version>

            <packaging>jar</packaging>

            <properties>
                <maven.compiler.source>11</maven.compiler.source>
                <maven.compiler.target>11</maven.compiler.target>
            </properties>

            <dependencies>
               $dependencies
               $sentryDependency
            </dependencies>

            <build>
                <plugins>
                    $sentryPlugin
                    <plugin>
                        <artifactId>maven-dependency-plugin</artifactId>
                        <executions>
                          <execution>
                            <phase>install</phase>
                              <goals>
                                <goal>copy-dependencies</goal>
                              </goals>
                              <configuration>
                                 <outputDirectory>target/lib</outputDirectory>
                              </configuration>
                            </execution>
                          </executions>
                    </plugin>
                </plugins>
            </build>
        </project>
        """.trimIndent()
}
