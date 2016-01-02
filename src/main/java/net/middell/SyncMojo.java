/*
 * This file is part of eXist-db Maven Plugin.
 *
 * eXist-db Maven Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * eXist-db Maven Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eXist-db Maven Plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.middell;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Synchronizes the local filesystem with a set of collections and/or resources
 * in an eXist-db instance.
 *
 * <p>Sample configuration:</p>
 *
 * <pre>{@literal
<configuration>
  <serverId>exist</serverId>
  <syncMappings>
    <syncMapping>
      <source>xmldb:exist://localhost.de:8080/exist/xmlrpc/db/apps/sample/</source>
      <target>src/main/xml/</target>
    </syncMapping>
  </syncMappings>
</configuration>
 * }</pre>
 *
 * <p>Corresponding server settings in <code>$HOME/.m2/settings.xml</code>:</p>
 *
 * <pre>{@literal
<server>
  <id>exist</id>
  <username>admin</username>
  <password>secret</password>
</server>}</pre>
 *
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
@Mojo(name = "sync")
public class SyncMojo extends AbstractMojo {

    /**
     * The id of the <code>&lt;server/&gt;</code> entry in the Maven settings which provides
     * username and password credentials for accessing the eXist-db instance.
     *
     * <p>Should this identifier not have been configured, this goal emits a warning and exits.</p>
     */
    @Parameter
    private String serverId;

    /**
     * An optional base URI of all collections/resources to be synchronized.
     *
     * <p>All {@link #syncMappings mapped URIs} are resolved against this base in case
     * it has been provided.</p>
     */
    @Parameter
    private String syncBase;

    /**
     * A list of mappings between eXist-db collection/resources, specified via URIs, and local
     * filesystem paths.
     *
     * <p>Relative filesystem paths are interpreted relative to a project's base directory.</p>
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Parameter
    private List<SyncMapping> syncMappings;

    /**
     * An optional regular expression which is matched against eXist-db URIs and allows for the
     * exclusion of resources otherwise synced.
     *
     * <p>Per default, descriptor files are excluded.</p>
     */
    @Parameter(defaultValue = ".*?(repo)|(expath\\-pkg)\\.xml$")
    private String syncExclusion;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${settings}", required = true, readonly = true)
    private Settings settings;

    @Override
    public final void execute() throws MojoExecutionException {
        try {
            if (serverId == null || syncMappings == null) {
                getLog().warn("No server and/or sync mappings defined.");
                return;
            }

            final ExistDatabase db = database();

            Predicate<String> syncExclusionPredicate = null;
            if (syncExclusion != null) {
                syncExclusionPredicate = Pattern.compile(syncExclusion).asPredicate();
            }

            final SortedMap<URI, File> mappings = initMappings();
            final Path[] mappedPaths = mappings.values().stream().map(File::toPath)
                    .toArray(Path[]::new);

            while (!mappings.isEmpty()) {
                final URI source = mappings.firstKey();
                final File target = mappings.remove(source);

                if (!target.isDirectory() && !target.mkdirs()) {
                    throw new MojoExecutionException(
                            String.format("Cannot create directory '%s'", target)
                    );
                }

                @SuppressWarnings("ConstantConditions")
                final Map<String, File> targetChildren = Stream.of(target.listFiles())
                        .collect(Collectors.toMap(File::getName, Function.identity()));

                try (ExistDatabase.Collection collection = db.collection(source)) {
                    for (String name : collection.resources()) {
                        final URI sourceUri = source.resolve(name);
                        final File targetFile = Optional
                                .ofNullable(targetChildren.remove(name))
                                .orElseGet(() -> new File(target, name));

                        if (syncExclusionPredicate != null) {
                            if (syncExclusionPredicate.test(sourceUri.toString())) {
                                getLog().info(String.format("Skipping '%s'", sourceUri));
                                continue;
                            }
                        }

                        try (ExistDatabase.Resource resource = db.resource(collection, name)) {
                            final boolean needsUpdate = !targetFile.exists()
                                    || targetFile.lastModified() < resource.lastModified();

                            if (needsUpdate) {
                                final Path targetPath = targetFile.toPath();
                                switch (resource.type()) {
                                    case BINARY:
                                        Files.write(targetPath, resource.binaryContent());
                                        break;
                                    default:
                                        Files.write(
                                                targetPath,
                                                Collections.singleton(resource.content()),
                                                StandardCharsets.UTF_8
                                        );
                                        break;
                                }
                                getLog().info(String.format("[%s] -> [%s]", sourceUri, targetFile));
                            }
                        }
                    }
                    for (String name : collection.collections()) {
                        mappings.put(
                                source.resolve(name + "/"),
                                Optional.ofNullable(targetChildren.remove(name))
                                        .orElseGet(() -> new File(target, name))
                        );
                    }
                    for (File file : targetChildren.values()) {
                        final Path path = file.getCanonicalFile().toPath();
                        if (Stream.of(mappedPaths).anyMatch(mp -> mp.startsWith(path))) {
                            continue;
                        }

                        getLog().info(String.format("Deleting '%s'", file));
                        deleteRecursively(file);
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException(
                    String.format("Error while syncing '%s'", syncBase),
                    e
            );
        }
    }

    private void deleteRecursively(File file) throws IOException {
        Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (!file.toFile().isDirectory()) {
                    Files.delete(file);
                }
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    private SortedMap<URI, File> initMappings() throws IOException {
        final SortedMap<URI, File> syncMap = new TreeMap<>(
                Comparator.<URI>naturalOrder().reversed()
        );

        final File projectBase = project.getBasedir().getCanonicalFile();
        final String projectBasePath = projectBase.getPath();

        final URI syncBaseUri = (syncBase == null ? null : ExistUri.create(syncBase));
        for (SyncMapping mapping : syncMappings) {
            URI sourceUri = ExistUri.create(mapping.source);
            if (syncBaseUri != null) {
                sourceUri = syncBaseUri.resolve(sourceUri);
            }
            final File targetFile = new File(projectBase, mapping.target).getCanonicalFile();
            if (targetFile.toPath().startsWith(projectBasePath)) {
                syncMap.put(sourceUri, targetFile);
            } else {
                getLog().warn(String.format("'%s' is not in project dir! Skipped.", targetFile));
            }
        }
        return syncMap;
    }

    private ExistDatabase database() throws MojoExecutionException {
        final Server server = Optional.ofNullable(settings.getServer(serverId))
                .orElseThrow(() -> new MojoExecutionException(
                        String.format("Server '%s' not found", serverId)
                ));

        return new ExistDatabase(
                Optional.ofNullable(server.getUsername())
                        .orElseThrow(() -> new MojoExecutionException(
                                String.format("Server '%s' has no username defined", serverId)
                        )),
                Optional.ofNullable(server.getPassword())
                        .orElseThrow(() -> new MojoExecutionException(
                                String.format("Server '%s' has no password defined", serverId)
                        ))
        );
    }

}
