package io.roastedroot.zerofs.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import org.junit.jupiter.api.Test;

public class PathTypeTest {

    private static final FakePathType type = new FakePathType();
    static final URI fileSystemUri = URI.create("zerofs://foo");

    @Test
    public void testBasicProperties() {
        assertEquals(type.getSeparator(), "/");
        assertEquals(type.getOtherSeparators(), "\\");
    }

    @Test
    public void testParsePath() {
        PathType.ParseResult path = type.parsePath("foo/bar/baz/one\\two");
        assertNull(path.root());
        String[] names = path.names();
        assertEquals(5, names.length);
        assertEquals(names[0], "foo");
        assertEquals(names[1], "bar");
        assertEquals(names[2], "baz");
        assertEquals(names[3], "one");
        assertEquals(names[4], "two");

        PathType.ParseResult path2 = type.parsePath("$one//\\two");
        assertEquals(path2.root(), "$");
        String[] names2 = path2.names();
        assertEquals(2, names2.length);
        assertEquals(names2[0], "one");
        assertEquals(names2[1], "two");
    }

    @Test
    public void testToString() {
        PathType.ParseResult path = type.parsePath("foo/bar\\baz");
        assertEquals("foo/bar/baz", type.toString(path.root(), path.names()));

        PathType.ParseResult path2 = type.parsePath("$/foo/bar");
        assertEquals("$foo/bar", type.toString(path2.root(), path2.names()));
    }

    //
    //    @Test
    //    public void testToUri() {
    //        URI fileUri = type.toUri(fileSystemUri, "$", ImmutableList.of("foo", "bar"), false);
    //        assertThat(fileUri.toString()).isEqualTo("jimfs://foo/$/foo/bar");
    //        assertThat(fileUri.getPath()).isEqualTo("/$/foo/bar");
    //
    //        URI directoryUri = type.toUri(fileSystemUri, "$", ImmutableList.of("foo", "bar"),
    // true);
    //        assertThat(directoryUri.toString()).isEqualTo("jimfs://foo/$/foo/bar/");
    //        assertThat(directoryUri.getPath()).isEqualTo("/$/foo/bar/");
    //
    //        URI rootUri = type.toUri(fileSystemUri, "$", ImmutableList.<String>of(), true);
    //        assertThat(rootUri.toString()).isEqualTo("jimfs://foo/$/");
    //        assertThat(rootUri.getPath()).isEqualTo("/$/");
    //    }
    //
    //    @Test
    //    public void testToUri_escaping() {
    //        URI fileUri = type.toUri(fileSystemUri, "$", ImmutableList.of("foo", "bar baz"),
    // false);
    //        assertThat(fileUri.toString()).isEqualTo("jimfs://foo/$/foo/bar%20baz");
    //        assertThat(fileUri.getRawPath()).isEqualTo("/$/foo/bar%20baz");
    //        assertThat(fileUri.getPath()).isEqualTo("/$/foo/bar baz");
    //    }
    //
    //    @Test
    //    public void testUriRoundTrips() {
    //        assertUriRoundTripsCorrectly(type, "$");
    //        assertUriRoundTripsCorrectly(type, "$foo");
    //        assertUriRoundTripsCorrectly(type, "$foo/bar/baz");
    //        assertUriRoundTripsCorrectly(type, "$foo bar");
    //        assertUriRoundTripsCorrectly(type, "$foo/bar baz");
    //    }
    //
    //    static void assertParseResult(ParseResult result, @Nullable String root, String... names)
    // {
    //        assertThat(result.root()).isEqualTo(root);
    //        assertThat(result.names()).containsExactly((Object[]) names).inOrder();
    //    }
    //
    //    static void assertUriRoundTripsCorrectly(PathType type, String path) {
    //        ParseResult result = type.parsePath(path);
    //        URI uri = type.toUri(fileSystemUri, result.root(), result.names(), false);
    //        ParseResult parsedUri = type.fromUri(uri);
    //        assertThat(parsedUri.root()).isEqualTo(result.root());
    //        assertThat(parsedUri.names()).containsExactlyElementsIn(result.names()).inOrder();
    //    }

    /** Arbitrary path type with $ as the root, / as the separator and \ as an alternate separator. */
    private static final class FakePathType extends PathType {

        protected FakePathType() {
            super(false, '/', '\\');
        }

        @Override
        public ParseResult parsePath(String path) {
            String root = null;
            if (path.startsWith("$")) {
                root = "$";
                path = path.substring(1);
            }

            return new ParseResult(root, split(path));
        }

        @Override
        public String toString(String root, String[] names) {
            StringBuilder builder = new StringBuilder();
            if (root != null) {
                builder.append(root);
            }
            builder.append(join(names));
            return builder.toString();
        }

        @Override
        public String toUriPath(String root, String[] names, boolean directory) {
            StringBuilder builder = new StringBuilder();
            builder.append('/').append(root);
            for (String name : names) {
                builder.append('/').append(name);
            }
            if (directory) {
                builder.append('/');
            }
            return builder.toString();
        }

        @Override
        public ParseResult parseUriPath(String uriPath) {
            if (!uriPath.startsWith("/$")) {
                throw new IllegalArgumentException(
                        String.format("uriPath (%s) must start with /$", uriPath));
            }
            return parsePath(uriPath.substring(1));
        }
    }
}
