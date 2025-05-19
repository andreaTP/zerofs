package io.roastedroot.zerofs.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.roastedroot.zerofs.core.PathSubject.paths;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PathService}.
 *
 * @author Colin Decker
 */
public class PathServiceTest {

    private static final Set<PathNormalization> NO_NORMALIZATIONS = Set.of();

    private final PathService service = fakeUnixPathService();

    @Test
    public void testBasicProperties() {
        assertEquals("/", service.getSeparator());
        assertThat(fakeWindowsPathService().getSeparator()).isEqualTo("\\");
    }

    @Test
    public void testPathCreation() {
        assertAbout(paths())
                .that(service.emptyPath())
                .hasRootComponent(null)
                .and()
                .hasNameComponents("");

        assertAbout(paths())
                .that(service.createRoot(service.name("/")))
                .isAbsolute()
                .and()
                .hasRootComponent("/")
                .and()
                .hasNoNameComponents();

        assertAbout(paths())
                .that(service.createFileName(service.name("foo")))
                .hasRootComponent(null)
                .and()
                .hasNameComponents("foo");

        ZeroFsPath relative =
                service.createRelativePath(service.names(List.of("foo", "bar")));
        assertAbout(paths())
                .that(relative)
                .hasRootComponent(null)
                .and()
                .hasNameComponents("foo", "bar");

        ZeroFsPath absolute =
                service.createPath(
                        service.name("/"), service.names(List.of("foo", "bar")));
        assertAbout(paths())
                .that(absolute)
                .isAbsolute()
                .and()
                .hasRootComponent("/")
                .and()
                .hasNameComponents("foo", "bar");
    }

    @Test
    public void testPathCreation_emptyPath() {
        // normalized to empty path with single empty string name
        assertAbout(paths())
                .that(service.createPath(null, List.<Name>of()))
                .hasRootComponent(null)
                .and()
                .hasNameComponents("");
    }

    @Test
    public void testPathCreation_parseIgnoresEmptyString() {
        // if the empty string wasn't ignored, the resulting path would be "/foo" since the empty
        // string would be joined with foo
        assertAbout(paths())
                .that(service.parsePath("", "foo"))
                .hasRootComponent(null)
                .and()
                .hasNameComponents("foo");
    }

    @Test
    public void testToString() {
        // not much to test for this since it just delegates to PathType anyway
        ZeroFsPath path =
                new ZeroFsPath(
                        service, null, List.of(Name.simple("foo"), Name.simple("bar")));
        assertThat(service.toString(path)).isEqualTo("foo/bar");

        path = new ZeroFsPath(service, Name.simple("/"), List.of(Name.simple("foo")));
        assertThat(service.toString(path)).isEqualTo("/foo");
    }

    @Test
    public void testHash_usingDisplayForm() {
        PathService pathService = fakePathService(PathType.unix(), false);

        ZeroFsPath path1 =
                new ZeroFsPath(pathService, null, List.of(Name.create("FOO", "foo")));
        ZeroFsPath path2 =
                new ZeroFsPath(pathService, null, List.of(Name.create("FOO", "FOO")));
        ZeroFsPath path3 =
                new ZeroFsPath(
                        pathService,
                        null,
                        List.of(Name.create("FOO", "9874238974897189741")));

        assertThat(pathService.hash(path1)).isEqualTo(pathService.hash(path2));
        assertThat(pathService.hash(path2)).isEqualTo(pathService.hash(path3));
    }

    @Test
    public void testHash_usingCanonicalForm() {
        PathService pathService = fakePathService(PathType.unix(), true);

        ZeroFsPath path1 =
                new ZeroFsPath(pathService, null, List.of(Name.create("foo", "foo")));
        ZeroFsPath path2 =
                new ZeroFsPath(pathService, null, List.of(Name.create("FOO", "foo")));
        ZeroFsPath path3 =
                new ZeroFsPath(
                        pathService,
                        null,
                        List.of(Name.create("28937497189478912374897", "foo")));

        assertThat(pathService.hash(path1)).isEqualTo(pathService.hash(path2));
        assertThat(pathService.hash(path2)).isEqualTo(pathService.hash(path3));
    }

    @Test
    public void testCompareTo_usingDisplayForm() {
        PathService pathService = fakePathService(PathType.unix(), false);

        ZeroFsPath path1 = new ZeroFsPath(pathService, null, List.of(Name.create("a", "z")));
        ZeroFsPath path2 = new ZeroFsPath(pathService, null, List.of(Name.create("b", "y")));
        ZeroFsPath path3 = new ZeroFsPath(pathService, null, List.of(Name.create("c", "x")));

        assertThat(pathService.compare(path1, path2)).isEqualTo(-1);
        assertThat(pathService.compare(path2, path3)).isEqualTo(-1);
    }

    @Test
    public void testCompareTo_usingCanonicalForm() {
        PathService pathService = fakePathService(PathType.unix(), true);

        ZeroFsPath path1 = new ZeroFsPath(pathService, null, List.of(Name.create("a", "z")));
        ZeroFsPath path2 = new ZeroFsPath(pathService, null, List.of(Name.create("b", "y")));
        ZeroFsPath path3 = new ZeroFsPath(pathService, null, List.of(Name.create("c", "x")));

        assertThat(pathService.compare(path1, path2)).isEqualTo(1);
        assertThat(pathService.compare(path2, path3)).isEqualTo(1);
    }

    @Test
    public void testPathMatcher() {
        assertThat(service.createPathMatcher("regex:foo"))
                .isInstanceOf(PathMatchers.RegexPathMatcher.class);
        assertThat(service.createPathMatcher("glob:foo"))
                .isInstanceOf(PathMatchers.RegexPathMatcher.class);
    }

    @Test
    public void testPathMatcher_usingCanonicalForm_usesCanonicalNormalizations() {
        // https://github.com/google/ZeroFs/issues/91
        // This matches the behavior of Windows (the only built-in configuration that uses canonical
        // form for equality). There, PathMatchers should do case-insensitive matching despite
        // Windows
        // not normalizing case for display.
        assertCaseInsensitiveMatches(
                new PathService(
                        PathType.unix(),
                        NO_NORMALIZATIONS,
                        ImmutableSet.of(CASE_FOLD_ASCII),
                        true));
        assertCaseSensitiveMatches(
                new PathService(
                        PathType.unix(),
                        ImmutableSet.of(CASE_FOLD_ASCII),
                        NO_NORMALIZATIONS,
                        true));
    }

    @Test
    public void testPathMatcher_usingDisplayForm_usesDisplayNormalizations() {
        assertCaseInsensitiveMatches(
                new PathService(
                        PathType.unix(),
                        ImmutableSet.of(CASE_FOLD_ASCII),
                        NO_NORMALIZATIONS,
                        false));
        assertCaseSensitiveMatches(
                new PathService(
                        PathType.unix(),
                        NO_NORMALIZATIONS,
                        ImmutableSet.of(CASE_FOLD_ASCII),
                        false));
    }

    private static void assertCaseInsensitiveMatches(PathService service) {
        List<PathMatcher> matchers =
                List.of(
                        service.createPathMatcher("glob:foo"),
                        service.createPathMatcher("glob:FOO"));

        ZeroFsPath lowerCasePath = singleNamePath(service, "foo");
        ZeroFsPath upperCasePath = singleNamePath(service, "FOO");
        ZeroFsPath nonMatchingPath = singleNamePath(service, "bar");

        for (PathMatcher matcher : matchers) {
            assertThat(matcher.matches(lowerCasePath)).isTrue();
            assertThat(matcher.matches(upperCasePath)).isTrue();
            assertThat(matcher.matches(nonMatchingPath)).isFalse();
        }
    }

    private static void assertCaseSensitiveMatches(PathService service) {
        PathMatcher matcher = service.createPathMatcher("glob:foo");

        ZeroFsPath lowerCasePath = singleNamePath(service, "foo");
        ZeroFsPath upperCasePath = singleNamePath(service, "FOO");

        assertThat(matcher.matches(lowerCasePath)).isTrue();
        assertThat(matcher.matches(upperCasePath)).isFalse();
    }

    public static PathService fakeUnixPathService() {
        return fakePathService(PathType.unix(), false);
    }

    public static PathService fakeWindowsPathService() {
        return fakePathService(PathType.windows(), false);
    }

    public static PathService fakePathService(PathType type, boolean equalityUsesCanonicalForm) {
        PathService service =
                new PathService(
                        type, NO_NORMALIZATIONS, NO_NORMALIZATIONS, equalityUsesCanonicalForm);
        service.setFileSystem(FILE_SYSTEM);
        return service;
    }

    private static ZeroFsPath singleNamePath(PathService service, String name) {
        return new ZeroFsPath(service, null, List.of(Name.create(name, name)));
    }

    private static final FileSystem FILE_SYSTEM;

    static {
        try {
            FILE_SYSTEM =
                    ZeroFsFileSystems.newFileSystem(
                            new ZeroFsFileSystemProvider(),
                            URI.create("ZeroFs://foo"),
                            Configuration.unix());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
