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

import org.apache.maven.plugins.annotations.Parameter;

/**
 * A mapping between a source URI and a target path.
 *
 * <p>One or more such mappings define how the synchronization between an eXist-db instance and
 * the local filesystem is carried out.</p>
 *
 * @see SyncMojo
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class SyncMapping {

    /**
     * The source URI of a resource or collection in an eXist-db instance which is mapped to a path
     * in the local filesystem.
     */
    @Parameter(required = true)
    public String source;

    /**
     * The path in the local filesystem, a resource or collection in an eXist-db instance is
     * mapped to.
     */
    @Parameter(required = true)
    public String target;

}
