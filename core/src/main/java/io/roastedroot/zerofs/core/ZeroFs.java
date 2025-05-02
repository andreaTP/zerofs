package io.roastedroot.zerofs.core;

import java.util.UUID;
import java.util.logging.Logger;

public final class ZeroFs {

    /** The URI scheme for the ZeroFs file system ("zerofs"). */
    public static final String URI_SCHEME = "zerofs";

    private static final Logger LOGGER = Logger.getLogger(ZeroFs.class.getName());

    private static String newRandomFileSystemName() {
        return UUID.randomUUID().toString();
    }
}
