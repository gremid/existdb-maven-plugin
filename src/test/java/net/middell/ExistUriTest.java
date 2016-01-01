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

import org.hamcrest.core.StringStartsWith;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class ExistUriTest {

    @Test
    public void schemeIsNull() {
        Assert.assertNull(
                ExistUri.create("xmldb:exist://localhost/db/apps").toString(),
                ExistUri.create("xmldb:exist://localhost/db/apps").getScheme()
        );
    }

    @Test
    public void isSchemeRelative() {
        Assert.assertThat(
                ExistUri.create("xmldb:exist://localhost/db/apps").toString(),
                StringStartsWith.startsWith("//")
        );
    }

    @Test
    public void roundtrip() {
        roundtrip("xmldb:exist://localhost/db/apps/");
        roundtrip("xmldb:exist:///db/apps/");
        roundtrip("xmldb:exist://xmldb.test.com:8080/");
    }

    protected void roundtrip(String uri) {
        final URI existUri = ExistUri.create(uri);
        Assert.assertEquals(uri, ExistUri.toString(existUri));
    }
}
