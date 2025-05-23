package io.roastedroot.zerofs;

import static io.roastedroot.zerofs.Feature.FILE_CHANNEL;
import static io.roastedroot.zerofs.Feature.LINKS;
import static io.roastedroot.zerofs.Feature.SECURE_DIRECTORY_STREAM;
import static io.roastedroot.zerofs.Feature.SYMBOLIC_LINKS;
import static io.roastedroot.zerofs.PathNormalization.CASE_FOLD_ASCII;
import static io.roastedroot.zerofs.PathNormalization.NFC;
import static io.roastedroot.zerofs.PathNormalization.NFD;

import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable configuration for an in-memory file system. A {@code Configuration} is passed to a
 * method in {@link ZeroFs} such as {@link ZeroFs#newFileSystem(Configuration)} to create a new {@link
 * FileSystem} instance.
 *
 * @author Colin Decker
 */
public final class Configuration {

    /**
     * Returns the default configuration for a UNIX-like file system. A file system created with this
     * configuration:
     *
     * <ul>
     *   <li>uses {@code /} as the path name separator (see {@link PathType#unix()} for more
     *       information on the path format)
     *   <li>has root {@code /} and working directory {@code /work}
     *   <li>performs case-sensitive file lookup
     *   <li>supports only the {@linkplain BasicFileAttributeView basic} file attribute view, to avoid
     *       overhead for unneeded attributes
     *   <li>supports hard links, symbolic links, {@link SecureDirectoryStream} and {@link
     *       FileChannel}
     * </ul>
     *
     * <p>To create a modified version of this configuration, such as to include the full set of UNIX
     * file attribute views, {@linkplain #toBuilder() create a builder}.
     *
     * <p>Example:
     *
     * <pre>
     *   Configuration config = Configuration.unix().toBuilder()
     *       .setAttributeViews("basic", "owner", "posix", "unix")
     *       .setWorkingDirectory("/home/user")
     *       .build();  </pre>
     */
    public static Configuration unix() {
        return UnixHolder.UNIX;
    }

    private static final class UnixHolder {
        private static final Configuration UNIX =
                Configuration.builder(PathType.unix())
                        .setDisplayName("Unix")
                        .setRoots("/")
                        .setWorkingDirectory("/work")
                        .setAttributeViews("basic")
                        .setSupportedFeatures(
                                LINKS, SYMBOLIC_LINKS, SECURE_DIRECTORY_STREAM, FILE_CHANNEL)
                        .build();
    }

    /**
     * Returns the default configuration for a Mac OS X-like file system.
     *
     * <p>The primary differences between this configuration and the default {@link #unix()}
     * configuration are that this configuration does Unicode normalization on the display and
     * canonical forms of filenames and does case insensitive file lookup.
     *
     * <p>A file system created with this configuration:
     *
     * <ul>
     *   <li>uses {@code /} as the path name separator (see {@link PathType#unix()} for more
     *       information on the path format)
     *   <li>has root {@code /} and working directory {@code /work}
     *   <li>does Unicode normalization on paths, both for lookup and for {@code Path} objects
     *   <li>does case-insensitive (for ASCII characters only) lookup
     *   <li>supports only the {@linkplain BasicFileAttributeView basic} file attribute view, to avoid
     *       overhead for unneeded attributes
     *   <li>supports hard links, symbolic links and {@link FileChannel}
     * </ul>
     *
     * <p>To create a modified version of this configuration, such as to include the full set of UNIX
     * file attribute views or to use full Unicode case insensitivity, {@linkplain #toBuilder() create
     * a builder}.
     *
     * <p>Example:
     *
     * <pre>
     *   Configuration config = Configuration.osX().toBuilder()
     *       .setAttributeViews("basic", "owner", "posix", "unix")
     *       .setNameCanonicalNormalization(NFD, CASE_FOLD_UNICODE)
     *       .setWorkingDirectory("/Users/user")
     *       .build();  </pre>
     */
    public static Configuration osX() {
        return OsxHolder.OS_X;
    }

    private static final class OsxHolder {
        private static final Configuration OS_X =
                unix().toBuilder()
                        .setDisplayName("OSX")
                        .setNameDisplayNormalization(NFC) // matches JDK 1.7u40+ behavior
                        .setNameCanonicalNormalization(
                                NFD, CASE_FOLD_ASCII) // NFD is default in HFS+
                        .setSupportedFeatures(LINKS, SYMBOLIC_LINKS, FILE_CHANNEL)
                        .build();
    }

    /**
     * Returns the default configuration for a Windows-like file system. A file system created with
     * this configuration:
     *
     * <ul>
     *   <li>uses {@code \} as the path name separator and recognizes {@code /} as a separator when
     *       parsing paths (see {@link PathType#windows()} for more information on path format)
     *   <li>has root {@code C:\} and working directory {@code C:\work}
     *   <li>performs case-insensitive (for ASCII characters only) file lookup
     *   <li>creates {@code Path} objects that use case-insensitive (for ASCII characters only)
     *       equality
     *   <li>supports only the {@linkplain BasicFileAttributeView basic} file attribute view, to avoid
     *       overhead for unneeded attributes
     *   <li>supports hard links, symbolic links and {@link FileChannel}
     * </ul>
     *
     * <p>To create a modified version of this configuration, such as to include the full set of
     * Windows file attribute views or to use full Unicode case insensitivity, {@linkplain
     * #toBuilder() create a builder}.
     *
     * <p>Example:
     *
     * <pre>
     *   Configuration config = Configuration.windows().toBuilder()
     *       .setAttributeViews("basic", "owner", "dos", "acl", "user")
     *       .setNameCanonicalNormalization(CASE_FOLD_UNICODE)
     *       .setWorkingDirectory("C:\\Users\\user") // or "C:/Users/user"
     *       .build();  </pre>
     */
    public static Configuration windows() {
        return WindowsHolder.WINDOWS;
    }

    private static final class WindowsHolder {
        private static final Configuration WINDOWS =
                Configuration.builder(PathType.windows())
                        .setDisplayName("Windows")
                        .setRoots("C:\\")
                        .setWorkingDirectory("C:\\work")
                        .setNameCanonicalNormalization(CASE_FOLD_ASCII)
                        .setPathEqualityUsesCanonicalForm(
                                true) // matches real behavior of WindowsPath
                        .setAttributeViews("basic")
                        .setSupportedFeatures(LINKS, SYMBOLIC_LINKS, FILE_CHANNEL)
                        .build();
    }

    /**
     * Returns a default configuration appropriate to the current operating system.
     *
     * <p>More specifically, if the operating system is Windows, {@link Configuration#windows()} is
     * returned; if the operating system is Mac OS X, {@link Configuration#osX()} is returned;
     * otherwise, {@link Configuration#unix()} is returned.
     *
     * <p>This is the configuration used by the {@code ZeroFs.newFileSystem} methods that do not take a
     * {@code Configuration} parameter.
     *
     * @since 1.1
     */
    public static Configuration forCurrentPlatform() {
        String os = System.getProperty("os.name");

        if (os.contains("Windows")) {
            return windows();
        } else if (os.contains("OS X")) {
            return osX();
        } else {
            return unix();
        }
    }

    /** Creates a new mutable {@link Configuration} builder using the given path type. */
    public static Builder builder(PathType pathType) {
        return new Builder(pathType);
    }

    // Path configuration
    final PathType pathType;
    final Set<PathNormalization> nameDisplayNormalization;
    final Set<PathNormalization> nameCanonicalNormalization;
    final boolean pathEqualityUsesCanonicalForm;

    // Disk configuration
    final int blockSize;
    final long maxSize;
    final long maxCacheSize;

    // Attribute configuration
    final Set<String> attributeViews;
    final Set<AttributeProvider> attributeProviders;
    final Map<String, Object> defaultAttributeValues;
    final FileTimeSource fileTimeSource;

    // Watch service
    final WatchServiceConfiguration watchServiceConfig;

    // Other
    final Set<String> roots;
    final String workingDirectory;
    final Set<Feature> supportedFeatures;
    private final String displayName;

    /** Creates an immutable configuration object from the given builder. */
    private Configuration(Builder builder) {
        this.pathType = builder.pathType;
        this.nameDisplayNormalization = builder.nameDisplayNormalization;
        this.nameCanonicalNormalization = builder.nameCanonicalNormalization;
        this.pathEqualityUsesCanonicalForm = builder.pathEqualityUsesCanonicalForm;
        this.blockSize = builder.blockSize;
        this.maxSize = builder.maxSize;
        this.maxCacheSize = builder.maxCacheSize;
        this.attributeViews = builder.attributeViews;
        this.attributeProviders =
                builder.attributeProviders == null
                        ? Set.<AttributeProvider>of()
                        : Set.copyOf(builder.attributeProviders);
        this.defaultAttributeValues =
                builder.defaultAttributeValues == null
                        ? Map.<String, Object>of()
                        : Map.copyOf(builder.defaultAttributeValues);
        this.fileTimeSource = builder.fileTimeSource;
        this.watchServiceConfig = builder.watchServiceConfig;
        this.roots = builder.roots;
        this.workingDirectory = builder.workingDirectory;
        this.supportedFeatures = builder.supportedFeatures;
        this.displayName = builder.displayName;
    }

    @Override
    public String toString() {
        return "Configuration{"
                + "pathType="
                + pathType
                + ", nameDisplayNormalization="
                + nameDisplayNormalization
                + ", nameCanonicalNormalization="
                + nameCanonicalNormalization
                + ", pathEqualityUsesCanonicalForm="
                + pathEqualityUsesCanonicalForm
                + ", blockSize="
                + blockSize
                + ", maxSize="
                + maxSize
                + ", maxCacheSize="
                + maxCacheSize
                + ", attributeViews="
                + attributeViews
                + ", attributeProviders="
                + attributeProviders
                + ", defaultAttributeValues="
                + defaultAttributeValues
                + ", fileTimeSource="
                + fileTimeSource
                + ", watchServiceConfig="
                + watchServiceConfig
                + ", roots="
                + roots
                + ", workingDirectory='"
                + workingDirectory
                + '\''
                + ", supportedFeatures="
                + supportedFeatures
                + ", displayName='"
                + displayName
                + '\''
                + '}';
    }

    /**
     * Returns a new mutable builder that initially contains the same settings as this configuration.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** Mutable builder for {@link Configuration} objects. */
    public static final class Builder {

        /** 8 KB. */
        public static final int DEFAULT_BLOCK_SIZE = 8192;

        /** 4 GB. */
        public static final long DEFAULT_MAX_SIZE = 4L * 1024 * 1024 * 1024;

        /** Equal to the configured max size. */
        public static final long DEFAULT_MAX_CACHE_SIZE = -1;

        // Path configuration
        private final PathType pathType;
        private Set<PathNormalization> nameDisplayNormalization = Set.of();
        private Set<PathNormalization> nameCanonicalNormalization = Set.of();
        private boolean pathEqualityUsesCanonicalForm = false;

        // Disk configuration
        private int blockSize = DEFAULT_BLOCK_SIZE;
        private long maxSize = DEFAULT_MAX_SIZE;
        private long maxCacheSize = DEFAULT_MAX_CACHE_SIZE;

        // Attribute configuration
        private Set<String> attributeViews = Set.of();
        private Set<AttributeProvider> attributeProviders = null;
        private Map<String, Object> defaultAttributeValues;
        private FileTimeSource fileTimeSource = SystemFileTimeSource.INSTANCE;

        // Watch service
        private WatchServiceConfiguration watchServiceConfig = WatchServiceConfiguration.DEFAULT;

        // Other
        private Set<String> roots = Set.of();
        private String workingDirectory;
        private Set<Feature> supportedFeatures = Set.of();
        private String displayName;

        private Builder(PathType pathType) {
            this.pathType = Objects.requireNonNull(pathType);
        }

        private Builder(Configuration configuration) {
            this.pathType = configuration.pathType;
            this.nameDisplayNormalization = configuration.nameDisplayNormalization;
            this.nameCanonicalNormalization = configuration.nameCanonicalNormalization;
            this.pathEqualityUsesCanonicalForm = configuration.pathEqualityUsesCanonicalForm;
            this.blockSize = configuration.blockSize;
            this.maxSize = configuration.maxSize;
            this.maxCacheSize = configuration.maxCacheSize;
            this.attributeViews = configuration.attributeViews;
            this.attributeProviders =
                    configuration.attributeProviders.isEmpty()
                            ? null
                            : new HashSet<>(configuration.attributeProviders);
            this.defaultAttributeValues =
                    configuration.defaultAttributeValues.isEmpty()
                            ? null
                            : new HashMap<>(configuration.defaultAttributeValues);
            this.fileTimeSource = configuration.fileTimeSource;
            this.watchServiceConfig = configuration.watchServiceConfig;
            this.roots = configuration.roots;
            this.workingDirectory = configuration.workingDirectory;
            this.supportedFeatures = configuration.supportedFeatures;
            // displayName intentionally not copied from the Configuration
        }

        /**
         * Sets the normalizations that will be applied to the display form of filenames. The display
         * form is used in the {@code toString()} of {@code Path} objects.
         */
        public Builder setNameDisplayNormalization(
                PathNormalization first, PathNormalization... more) {
            List<PathNormalization> list = new ArrayList<>(more.length + 1);
            list.add(first);
            list.addAll(List.of(more));
            this.nameDisplayNormalization = checkNormalizations(list);
            return this;
        }

        /**
         * Returns the normalizations that will be applied to the canonical form of filenames in the
         * file system. The canonical form is used to determine the equality of two filenames when
         * performing a file lookup.
         */
        public Builder setNameCanonicalNormalization(
                PathNormalization first, PathNormalization... more) {
            List<PathNormalization> list = new ArrayList<>();
            list.add(first);
            list.addAll(List.of(more));
            this.nameCanonicalNormalization = checkNormalizations(list);
            return this;
        }

        private Set<PathNormalization> checkNormalizations(List<PathNormalization> normalizations) {
            PathNormalization none = null;
            PathNormalization normalization = null;
            PathNormalization caseFold = null;
            for (PathNormalization n : normalizations) {
                Objects.requireNonNull(n);
                checkNormalizationNotSet(n, none);

                switch (n) {
                    case NONE:
                        none = n;
                        break;
                    case NFC:
                    case NFD:
                        checkNormalizationNotSet(n, normalization);
                        normalization = n;
                        break;
                    case CASE_FOLD_UNICODE:
                    case CASE_FOLD_ASCII:
                        checkNormalizationNotSet(n, caseFold);
                        caseFold = n;
                        break;
                    default:
                        throw new AssertionError(); // there are no other cases
                }
            }

            if (none != null) {
                return Set.of();
            }
            return Set.copyOf(normalizations);
        }

        private static void checkNormalizationNotSet(PathNormalization n, PathNormalization set) {
            if (set != null) {
                throw new IllegalArgumentException(
                        "can't set normalization " + n + ": normalization " + set + " already set");
            }
        }

        /**
         * Sets whether {@code Path} objects in the file system use the canonical form (true) or the
         * display form (false) of filenames for determining equality of two paths.
         *
         * <p>The default is false.
         */
        public Builder setPathEqualityUsesCanonicalForm(boolean useCanonicalForm) {
            this.pathEqualityUsesCanonicalForm = useCanonicalForm;
            return this;
        }

        /**
         * Sets the block size (in bytes) for the file system to use. All regular files will be
         * allocated blocks of the given size, so this is the minimum granularity for file size.
         *
         * <p>The default is 8192 bytes (8 KB).
         */
        public Builder setBlockSize(int blockSize) {
            if (blockSize <= 0) {
                throw new IllegalArgumentException(
                        String.format("blockSize (%s) must be positive", blockSize));
            }
            this.blockSize = blockSize;
            return this;
        }

        /**
         * Sets the maximum size (in bytes) for the file system's in-memory file storage. This maximum
         * size determines the maximum number of blocks that can be allocated to regular files, so it
         * should generally be a multiple of the {@linkplain #setBlockSize(int) block size}. The actual
         * maximum size will be the nearest multiple of the block size that is less than or equal to the
         * given size.
         *
         * <p><b>Note:</b> The in-memory file storage will not be eagerly initialized to this size, so
         * it won't use more memory than is needed for the files you create. Also note that in addition
         * to this limit, you will of course be limited by the amount of heap space available to the JVM
         * and the amount of heap used by other objects, both in the file system and elsewhere.
         *
         * <p>The default is 4 GB.
         */
        public Builder setMaxSize(long maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException(
                        String.format("maxSize (%s) must be positive", maxSize));
            }
            this.maxSize = maxSize;
            return this;
        }

        /**
         * Sets the maximum amount of unused space (in bytes) in the file system's in-memory file
         * storage that should be cached for reuse. By default, this will be equal to the {@linkplain
         * #setMaxSize(long) maximum size} of the storage, meaning that all space that is freed when
         * files are truncated or deleted is cached for reuse. This helps to avoid lots of garbage
         * collection when creating and deleting many files quickly. This can be set to 0 to disable
         * caching entirely (all freed blocks become available for garbage collection) or to some other
         * number to put an upper bound on the maximum amount of unused space the file system will keep
         * around.
         *
         * <p>Like the maximum size, the actual value will be the closest multiple of the block size
         * that is less than or equal to the given size.
         */
        public Builder setMaxCacheSize(long maxCacheSize) {
            if (maxCacheSize < 0) {
                throw new IllegalArgumentException(
                        String.format("maxCacheSize (%s) may not be negative", maxCacheSize));
            }
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        /**
         * Sets the attribute views the file system should support. By default, the following views may
         * be specified:
         *
         * <table>
         *   <tr>
         *     <td><b>Name</b></td>
         *     <td><b>View Interface</b></td>
         *     <td><b>Attributes Interface</b></td>
         *   </tr>
         *   <tr>
         *     <td>{@code "basic"}</td>
         *     <td>{@link java.nio.file.attribute.BasicFileAttributeView BasicFileAttributeView}</td>
         *     <td>{@link java.nio.file.attribute.BasicFileAttributes BasicFileAttributes}</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "owner"}</td>
         *     <td>{@link java.nio.file.attribute.FileOwnerAttributeView FileOwnerAttributeView}</td>
         *     <td>--</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "posix"}</td>
         *     <td>{@link java.nio.file.attribute.PosixFileAttributeView PosixFileAttributeView}</td>
         *     <td>{@link java.nio.file.attribute.PosixFileAttributes PosixFileAttributes}</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "unix"}</td>
         *     <td>--</td>
         *     <td>--</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "dos"}</td>
         *     <td>{@link java.nio.file.attribute.DosFileAttributeView DosFileAttributeView}</td>
         *     <td>{@link java.nio.file.attribute.DosFileAttributes DosFileAttributes}</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "acl"}</td>
         *     <td>{@link java.nio.file.attribute.AclFileAttributeView AclFileAttributeView}</td>
         *     <td>--</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "user"}</td>
         *     <td>{@link java.nio.file.attribute.UserDefinedFileAttributeView UserDefinedFileAttributeView}</td>
         *     <td>--</td>
         *   </tr>
         * </table>
         *
         * <p>If any other views should be supported, attribute providers for those views must be
         * {@linkplain #addAttributeProvider(AttributeProvider) added}.
         */
        public Builder setAttributeViews(String first, String... more) {
            List<String> list = new ArrayList();
            list.add(first);
            list.addAll(List.of(more));
            this.attributeViews = Set.copyOf(list);
            return this;
        }

        /** Adds an attribute provider for a custom view for the file system to support. */
        public Builder addAttributeProvider(AttributeProvider provider) {
            Objects.requireNonNull(provider);
            if (attributeProviders == null) {
                attributeProviders = new HashSet<>();
            }
            attributeProviders.add(provider);
            return this;
        }

        /**
         * Sets the default value to use for the given file attribute when creating new files. The
         * attribute must be in the form "view:attribute". The value must be of a type that the provider
         * for the view accepts.
         *
         * <p>For the included attribute views, default values can be set for the following attributes:
         *
         * <table>
         *   <tr>
         *     <th>Attribute</th>
         *     <th>Legal Types</th>
         *   </tr>
         *   <tr>
         *     <td>{@code "owner:owner"}</td>
         *     <td>{@code String} (user name)</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "posix:group"}</td>
         *     <td>{@code String} (group name)</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "posix:permissions"}</td>
         *     <td>{@code String} (format "rwxrw-r--"), {@code Set<PosixFilePermission>}</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "dos:readonly"}</td>
         *     <td>{@code Boolean}</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "dos:hidden"}</td>
         *     <td>{@code Boolean}</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "dos:archive"}</td>
         *     <td>{@code Boolean}</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "dos:system"}</td>
         *     <td>{@code Boolean}</td>
         *   </tr>
         *   <tr>
         *     <td>{@code "acl:acl"}</td>
         *     <td>{@code List<AclEntry>}</td>
         *   </tr>
         * </table>
         */
        public Builder setDefaultAttributeValue(String attribute, Object value) {
            if (!ATTRIBUTE_PATTERN.matcher(attribute).matches()) {
                throw new IllegalArgumentException(
                        String.format(
                                "attribute (%s) must be of the form \"view:attribute\"",
                                attribute));
            }
            Objects.requireNonNull(value);

            if (defaultAttributeValues == null) {
                defaultAttributeValues = new HashMap<>();
            }

            defaultAttributeValues.put(attribute, value);
            return this;
        }

        private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("[^:]+:[^:]+");

        /**
         * Sets the {@link FileTimeSource} that will supply the current time for this file system.
         *
         * @since 1.3
         */
        public Builder setFileTimeSource(FileTimeSource source) {
            this.fileTimeSource = Objects.requireNonNull(source);
            return this;
        }

        /**
         * Sets the roots for the file system.
         *
         * @throws InvalidPathException if any of the given roots is not a valid path for this builder's
         *     path type
         * @throws IllegalArgumentException if any of the given roots is a valid path for this builder's
         *     path type but is not a root path with no name elements
         */
        public Builder setRoots(String first, String... more) {
            List<String> roots = new ArrayList<>();
            roots.add(first);
            roots.addAll(List.of(more));
            for (String root : roots) {
                PathType.ParseResult parseResult = pathType.parsePath(root);
                if (!parseResult.isRoot()) {
                    throw new IllegalArgumentException(String.format("invalid root: %s", root));
                }
            }
            this.roots = Set.copyOf(roots);
            return this;
        }

        /**
         * Sets the path to the working directory for the file system. The working directory must be an
         * absolute path starting with one of the configured roots.
         *
         * @throws InvalidPathException if the given path is not valid for this builder's path type
         * @throws IllegalArgumentException if the given path is valid for this builder's path type but
         *     is not an absolute path
         */
        public Builder setWorkingDirectory(String workingDirectory) {
            PathType.ParseResult parseResult = pathType.parsePath(workingDirectory);
            if (!parseResult.isAbsolute()) {
                throw new IllegalArgumentException(
                        String.format(
                                "working directory must be an absolute path: %s",
                                workingDirectory));
            }
            this.workingDirectory = Objects.requireNonNull(workingDirectory);
            return this;
        }

        /**
         * Sets the given features to be supported by the file system. Any features not provided here
         * will not be supported.
         */
        public Builder setSupportedFeatures(Feature... features) {
            supportedFeatures = Set.copyOf(List.of(features));
            return this;
        }

        /**
         * Sets the configuration that {@link WatchService} instances created by the file system should
         * use. The default configuration polls watched directories for changes every 5 seconds.
         *
         * @since 1.1
         */
        public Builder setWatchServiceConfiguration(WatchServiceConfiguration config) {
            this.watchServiceConfig = Objects.requireNonNull(config);
            return this;
        }

        private Builder setDisplayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName);
            return this;
        }

        /** Creates a new immutable configuration object from this builder. */
        public Configuration build() {
            return new Configuration(this);
        }
    }
}
