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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.File;
import java.io.IOException;

/**
 * Packages all resources of the project into a XAR archive.
 *
 * <p>This goal is configured as part of the package phase in a custom lifecycle named "xar",
 * thus defining a new packaging type of the same name.</p>
 *
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
@Mojo(name = "xar")
public class XarMojo extends AbstractMojo {

    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    /**
     * The package file to be created.
     */
    @Parameter(
            defaultValue = "${project.build.directory}/${project.build.finalName}.xar",
            required = true)
    private File xarFile;

    @Parameter(
            defaultValue = "${project}",
            readonly = true,
            required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            zipArchiver.addDirectory(new File(project.getBuild().getOutputDirectory()));
            zipArchiver.setDestFile(xarFile);
            zipArchiver.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Error building XAR", e);
        }
    }
}
