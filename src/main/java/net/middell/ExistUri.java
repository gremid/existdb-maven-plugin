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

import java.net.URI;

/**
 * Utility methods for handling eXist-DB URIs.
 *
 * <p>As eXist-specific URIs with their <code>xmldb:exist</code> scheme are not RFC-2936-conformant,
 * methods of this class add/strip the scheme in order to allow URI handling via {@link URI}.</p>
 *
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public final class ExistUri {

    /**
     * Scheme prefix of eXist-DB URIs.
     */
    public static final String SCHEME_PREFIX = "xmldb:exist:";

    /**
     * Creates a URI from a string, optionally stripping the
     * {@link #SCHEME_PREFIX scheme} of exist-DB URIs.
     *
     * @param uri the string to be converted into a URI
     * @return a URI, optionally scheme-relative in the case of an exist-DB URI
     */
    public static URI create(final String uri) {
        if (uri.startsWith(SCHEME_PREFIX)) {
            return URI.create(uri.substring(SCHEME_PREFIX.length()));
        }
        return URI.create(uri);
    }

    /**
     * Serializes a URI to a string, optionally adding the
     * {@link #SCHEME_PREFIX scheme} of exist-DB URIs, should it be
     * scheme-relative.
     *
     * @param uri the URI to be serialized
     * @return a string representation of the URI
     */
    public static String toString(final URI uri) {
        final String uriStr = uri.toString();
        if (uriStr.startsWith("//")) {
            return SCHEME_PREFIX + uriStr;
        }
        return uriStr;
    }

    /**
     * Hidden constructor.
     */
    private ExistUri() {
    }
}
