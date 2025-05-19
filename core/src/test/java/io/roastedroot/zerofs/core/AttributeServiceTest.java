package io.roastedroot.zerofs.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AttributeService}.
 *
 * @author Colin Decker
 */
public class AttributeServiceTest {

    private AttributeService service;

    private final FakeFileTimeSource fileTimeSource = new FakeFileTimeSource();

    @BeforeEach
    public void setUp() {
        Set<AttributeProvider> providers =
                Set.of(
                        StandardAttributeProviders.get("basic"),
                        StandardAttributeProviders.get("owner"),
                        new TestAttributeProvider());
        service = new AttributeService(providers, Map.<String, Object>of());
    }

    private File createFile() {
        return Directory.create(0, fileTimeSource.now());
    }

    @Test
    public void testSupportedFileAttributeViews() {
        assertEquals(Set.of("basic", "test", "owner"), service.supportedFileAttributeViews());
    }

    @Test
    public void testSupportsFileAttributeView() {
        assertTrue(service.supportsFileAttributeView(BasicFileAttributeView.class));
        assertTrue(service.supportsFileAttributeView(TestAttributeView.class));
        assertFalse(service.supportsFileAttributeView(PosixFileAttributeView.class));
    }

    @Test
    public void testSetInitialAttributes() {
        File file = createFile();
        service.setInitialAttributes(file);

        assertEquals(Set.of("bar", "baz"), file.getAttributeNames("test"));
        assertEquals(Set.of("owner"), file.getAttributeNames("owner"));

        assertTrue(service.getAttribute(file, "basic:lastModifiedTime") instanceof FileTime);
        assertEquals(0L, file.getAttribute("test", "bar"));
        assertEquals(1, file.getAttribute("test", "baz"));
    }

    @Test
    public void testGetAttribute() {
        File file = createFile();
        service.setInitialAttributes(file);

        assertEquals("hello", service.getAttribute(file, "test:foo"));
        assertEquals("hello", service.getAttribute(file, "test", "foo"));
        assertEquals(false, service.getAttribute(file, "basic:isRegularFile"));
        assertEquals(true, service.getAttribute(file, "isDirectory"));
        assertEquals(1, service.getAttribute(file, "test:baz"));
    }

    //  @Test
    //  public void testGetAttribute_fromInheritedProvider() {
    //    File file = createFile();
    //    assertThat(service.getAttribute(file, "test:isRegularFile")).isEqualTo(false);
    //    assertThat(service.getAttribute(file, "test:isDirectory")).isEqualTo(true);
    //    assertThat(service.getAttribute(file, "test", "fileKey")).isEqualTo(0);
    //  }
    //
    //  @Test
    //  public void testGetAttribute_failsForAttributesNotDefinedByProvider() {
    //    File file = createFile();
    //    try {
    //      service.getAttribute(file, "test:blah");
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //    }
    //
    //    try {
    //      // baz is defined by "test", but basic doesn't inherit test
    //      service.getAttribute(file, "basic", "baz");
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //    }
    //  }
    //
    //  @Test
    //  public void testSetAttribute() {
    //    File file = createFile();
    //    service.setAttribute(file, "test:bar", 10L, false);
    //    assertThat(file.getAttribute("test", "bar")).isEqualTo(10L);
    //
    //    service.setAttribute(file, "test:baz", 100, false);
    //    assertThat(file.getAttribute("test", "baz")).isEqualTo(100);
    //  }
    //
    //  @Test
    //  public void testSetAttribute_forInheritedProvider() {
    //    File file = createFile();
    //    service.setAttribute(file, "test:lastModifiedTime", FileTime.fromMillis(0), false);
    //    assertThat(file.getAttribute("test", "lastModifiedTime")).isNull();
    //    assertThat(service.getAttribute(file, "basic:lastModifiedTime"))
    //        .isEqualTo(FileTime.fromMillis(0));
    //  }
    //
    //  @Test
    //  public void testSetAttribute_withAlternateAcceptedType() {
    //    File file = createFile();
    //    service.setAttribute(file, "test:bar", 10F, false);
    //    assertThat(file.getAttribute("test", "bar")).isEqualTo(10L);
    //
    //    service.setAttribute(file, "test:bar", BigInteger.valueOf(123), false);
    //    assertThat(file.getAttribute("test", "bar")).isEqualTo(123L);
    //  }
    //
    //  @Test
    //  public void testSetAttribute_onCreate() {
    //    File file = createFile();
    //    service.setInitialAttributes(file, new BasicFileAttribute<>("test:baz", 123));
    //    assertThat(file.getAttribute("test", "baz")).isEqualTo(123);
    //  }
    //
    //  @Test
    //  public void testSetAttribute_failsForAttributesNotDefinedByProvider() {
    //    File file = createFile();
    //    service.setInitialAttributes(file);
    //
    //    try {
    //      service.setAttribute(file, "test:blah", "blah", false);
    //      fail();
    //    } catch (UnsupportedOperationException expected) {
    //    }
    //
    //    try {
    //      // baz is defined by "test", but basic doesn't inherit test
    //      service.setAttribute(file, "basic:baz", 5, false);
    //      fail();
    //    } catch (UnsupportedOperationException expected) {
    //    }
    //
    //    assertThat(file.getAttribute("test", "baz")).isEqualTo(1);
    //  }
    //
    //  @Test
    //  public void testSetAttribute_failsForArgumentThatIsNotOfCorrectType() {
    //    File file = createFile();
    //    service.setInitialAttributes(file);
    //    try {
    //      service.setAttribute(file, "test:bar", "wrong", false);
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //    }
    //
    //    assertThat(file.getAttribute("test", "bar")).isEqualTo(0L);
    //  }
    //
    //  @Test
    //  public void testSetAttribute_failsForNullArgument() {
    //    File file = createFile();
    //    service.setInitialAttributes(file);
    //    try {
    //      service.setAttribute(file, "test:bar", null, false);
    //      fail();
    //    } catch (NullPointerException expected) {
    //    }
    //
    //    assertThat(file.getAttribute("test", "bar")).isEqualTo(0L);
    //  }
    //
    //  @Test
    //  public void testSetAttribute_failsForAttributeThatIsNotSettable() {
    //    File file = createFile();
    //    try {
    //      service.setAttribute(file, "test:foo", "world", false);
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //    }
    //
    //    assertThat(file.getAttribute("test", "foo")).isNull();
    //  }
    //
    //  @Test
    //  public void testSetAttribute_onCreate_failsForAttributeThatIsNotSettableOnCreate() {
    //    File file = createFile();
    //    try {
    //      service.setInitialAttributes(file, new BasicFileAttribute<>("test:foo", "world"));
    //      fail();
    //    } catch (UnsupportedOperationException expected) {
    //      // it turns out that UOE should be thrown on create even if the attribute isn't settable
    //      // under any circumstances
    //    }
    //
    //    try {
    //      service.setInitialAttributes(file, new BasicFileAttribute<>("test:bar", 5));
    //      fail();
    //    } catch (UnsupportedOperationException expected) {
    //    }
    //  }
    //
    //  @SuppressWarnings("ConstantConditions")
    //  @Test
    //  public void testGetFileAttributeView() throws IOException {
    //    final File file = createFile();
    //    service.setInitialAttributes(file);
    //
    //    FileLookup fileLookup =
    //        new FileLookup() {
    //          @Override
    //          public File lookup() throws IOException {
    //            return file;
    //          }
    //        };
    //
    //    assertThat(service.getFileAttributeView(fileLookup, TestAttributeView.class)).isNotNull();
    //    assertThat(service.getFileAttributeView(fileLookup,
    // BasicFileAttributeView.class)).isNotNull();
    //
    //    TestAttributes attrs =
    //        service.getFileAttributeView(fileLookup, TestAttributeView.class).readAttributes();
    //    assertThat(attrs.foo()).isEqualTo("hello");
    //    assertThat(attrs.bar()).isEqualTo(0);
    //    assertThat(attrs.baz()).isEqualTo(1);
    //  }
    //
    //  @Test
    //  public void testGetFileAttributeView_isNullForUnsupportedView() {
    //    final File file = createFile();
    //    FileLookup fileLookup =
    //        new FileLookup() {
    //          @Override
    //          public File lookup() throws IOException {
    //            return file;
    //          }
    //        };
    //    assertThat(service.getFileAttributeView(fileLookup,
    // PosixFileAttributeView.class)).isNull();
    //  }
    //
    //  @Test
    //  public void testReadAttributes_asMap() {
    //    File file = createFile();
    //    service.setInitialAttributes(file);
    //
    //    ImmutableMap<String, Object> map = service.readAttributes(file, "test:foo,bar,baz");
    //    assertThat(map).isEqualTo(ImmutableMap.of("foo", "hello", "bar", 0L, "baz", 1));
    //
    //    FileTime time = fileTimeSource.now();
    //
    //    map = service.readAttributes(file, "test:*");
    //    assertThat(map)
    //        .isEqualTo(
    //            ImmutableMap.<String, Object>builder()
    //                .put("foo", "hello")
    //                .put("bar", 0L)
    //                .put("baz", 1)
    //                .put("fileKey", 0)
    //                .put("isDirectory", true)
    //                .put("isRegularFile", false)
    //                .put("isSymbolicLink", false)
    //                .put("isOther", false)
    //                .put("size", 0L)
    //                .put("lastModifiedTime", time)
    //                .put("lastAccessTime", time)
    //                .put("creationTime", time)
    //                .build());
    //
    //    map = service.readAttributes(file, "basic:*");
    //    assertThat(map)
    //        .isEqualTo(
    //            ImmutableMap.<String, Object>builder()
    //                .put("fileKey", 0)
    //                .put("isDirectory", true)
    //                .put("isRegularFile", false)
    //                .put("isSymbolicLink", false)
    //                .put("isOther", false)
    //                .put("size", 0L)
    //                .put("lastModifiedTime", time)
    //                .put("lastAccessTime", time)
    //                .put("creationTime", time)
    //                .build());
    //  }
    //
    //  @Test
    //  public void testReadAttributes_asMap_failsForInvalidAttributes() {
    //    File file = createFile();
    //    try {
    //      service.readAttributes(file, "basic:fileKey,isOther,*,creationTime");
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //      assertThat(expected.getMessage()).contains("invalid attributes");
    //    }
    //
    //    try {
    //      service.readAttributes(file, "basic:fileKey,isOther,foo");
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //      assertThat(expected.getMessage()).contains("invalid attribute");
    //    }
    //  }
    //
    //  @Test
    //  public void testReadAttributes_asObject() {
    //    File file = createFile();
    //    service.setInitialAttributes(file);
    //
    //    BasicFileAttributes basicAttrs = service.readAttributes(file, BasicFileAttributes.class);
    //    assertThat(basicAttrs.fileKey()).isEqualTo(0);
    //    assertThat(basicAttrs.isDirectory()).isTrue();
    //    assertThat(basicAttrs.isRegularFile()).isFalse();
    //
    //    TestAttributes testAttrs = service.readAttributes(file, TestAttributes.class);
    //    assertThat(testAttrs.foo()).isEqualTo("hello");
    //    assertThat(testAttrs.bar()).isEqualTo(0);
    //    assertThat(testAttrs.baz()).isEqualTo(1);
    //
    //    file.setAttribute("test", "baz", 100);
    //    assertThat(service.readAttributes(file, TestAttributes.class).baz()).isEqualTo(100);
    //  }
    //
    //  @Test
    //  public void testReadAttributes_failsForUnsupportedAttributesType() {
    //    File file = createFile();
    //    try {
    //      service.readAttributes(file, PosixFileAttributes.class);
    //      fail();
    //    } catch (UnsupportedOperationException expected) {
    //    }
    //  }
    //
    //  @Test
    //  public void testIllegalAttributeFormats() {
    //    File file = createFile();
    //    try {
    //      service.getAttribute(file, ":bar");
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //      assertThat(expected.getMessage()).contains("attribute format");
    //    }
    //
    //    try {
    //      service.getAttribute(file, "test:");
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //      assertThat(expected.getMessage()).contains("attribute format");
    //    }
    //
    //    try {
    //      service.getAttribute(file, "basic:test:isDirectory");
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //      assertThat(expected.getMessage()).contains("attribute format");
    //    }
    //
    //    try {
    //      service.getAttribute(file, "basic:fileKey,size");
    //      fail();
    //    } catch (IllegalArgumentException expected) {
    //      assertThat(expected.getMessage()).contains("single attribute");
    //    }
    //  }
}
