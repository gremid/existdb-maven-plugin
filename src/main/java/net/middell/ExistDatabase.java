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

import org.exist.xmldb.DatabaseImpl;
import org.exist.xmldb.EXistResource;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.XMLDBException;

import java.net.URI;
import java.util.stream.Stream;

/**
 * Facilitates authenticated access to collections and resources in
 * an <a href="http://exist-db.org/" title="Homepage">eXist XML database</a>.
 *
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class ExistDatabase {

    /**
     * Resources in eXist databases contain either XML or binary content.
     */
    public enum ResourceType {
        BINARY, XML
    }

    static {
        try {
            DatabaseManager.registerDatabase(new DatabaseImpl());
        } catch (XMLDBException e) {
            e.printStackTrace();
        }
    }

    private final String user;
    private final String password;

    /**
     * Creates a database accessor with the given credentials.
     *
     * @param user     eXist-DB user/account for authenticated access
     * @param password the accounts's password
     */
    public ExistDatabase(String user, String password) {
        this.user = user;
        this.password = password;
    }

    /**
     * Creates a handle for access to a collection in the database.
     *
     * @param uri the {@link ExistUri eXist-specific URI} of the collection to wrap
     * @return a handle for accessing the collection
     * @throws XMLDBException propagated from {@link Collection#Collection(URI)}
     */
    public Collection collection(URI uri) throws XMLDBException {
        return new Collection(uri);
    }

    /**
     * Creates a handle for access to a resource in the database.
     *
     * @param collection handle of the resource's parent collection
     * @param name       the unique name of the resource to wrap
     * @return a handle for accessing the resource
     * @throws XMLDBException propagated from {@link Resource#Resource(Collection, String)}
     */
    public Resource resource(Collection collection, String name) throws XMLDBException {
        return new Resource(collection, name);
    }

    /**
     * Handle for an eXist-db resource.
     *
     * <p>Resource handles allow for implicit cleanup of database resources
     * by implementing {@link AutoCloseable}.</p>
     */
    public class Resource implements AutoCloseable {

        private final org.xmldb.api.base.Resource resource;

        /**
         * Creates a resource handle, based on the parent collection and the resource's name.
         *
         * @param collection the resource's parent
         * @param name       the unique name of the resource
         * @throws XMLDBException propagated from
         *                        {@link org.xmldb.api.base.Collection#getResource(String)}
         */
        public Resource(Collection collection, String name) throws XMLDBException {
            this.resource = collection.collection.getResource(name);
            if (resource == null) {
                throw new IllegalArgumentException(collection.uri.resolve(name).toString());
            }
        }

        /**
         * The time this resource was last modified.
         *
         * @return a UNIX timestamp in milliseconds
         * @throws XMLDBException propagated from {@link EXistResource#getLastModificationTime()}
         */
        public long lastModified() throws XMLDBException {
            return ((EXistResource) resource).getLastModificationTime().getTime();
        }

        /**
         * Determines whether the resource contains XML or binary content.
         *
         * @return the content type
         * @throws XMLDBException propagated from
         *                        {@link org.xmldb.api.base.Resource#getResourceType()}
         */
        public ResourceType type() throws XMLDBException {
            switch (resource.getResourceType()) {
                case "BinaryResource":
                    return ResourceType.BINARY;
                default:
                    return ResourceType.XML;
            }
        }

        /**
         * The XML content of the resource.
         *
         * @return the XML content as a string
         * @throws XMLDBException propgated from {@link org.xmldb.api.base.Resource#getContent()}
         */
        public String content() throws XMLDBException {
            return resource.getContent().toString();
        }

        /**
         * The binary content of the resource.
         * @return the content as a byte array
         * @throws XMLDBException propgated from {@link org.xmldb.api.base.Resource#getContent()}
         */
        public byte[] binaryContent() throws XMLDBException {
            return (byte[]) resource.getContent();
        }

        @Override
        public void close() throws Exception {
            if (resource != null) {
                ((EXistResource) resource).freeResources();
            }
        }
    }

    /**
     * Handle for an eXist-db collection.
     *
     * <p>Collection handles allow for implicit cleanup of database resources
     * by implementing {@link AutoCloseable}.</p>
     */

    public class Collection implements AutoCloseable {

        private final URI uri;
        private final org.xmldb.api.base.Collection collection;

        private Collection(URI uri) throws XMLDBException {
            if (!uri.getPath().endsWith("/")) {
                throw new IllegalArgumentException(uri.toString());
            }
            this.uri = uri;
            this.collection = DatabaseManager.getCollection(ExistUri.toString(uri), user, password);
        }

        /**
         * The sorted array of collection contained in this collection.
         *
         * @return the names of all child collections
         * @throws XMLDBException propagated from
         *                        {@link org.xmldb.api.base.Collection#listChildCollections()}
         */
        public String[] collections() throws XMLDBException {
            return Stream.of(collection.listChildCollections()).sorted().toArray(String[]::new);
        }

        /**
         * The sorted array of resources contained in this collection.
         *
         * @return the names of all child resources
         * @throws XMLDBException propagated from
         *                        {@link org.xmldb.api.base.Collection#listResources()}
         */
        public String[] resources() throws XMLDBException {
            return Stream.of(collection.listResources()).sorted().toArray(String[]::new);
        }

        @Override
        public void close() throws Exception {
            collection.close();
        }
    }
}
