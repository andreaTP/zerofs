package io.roastedroot.zerofs.core;

import java.nio.file.attribute.BasicFileAttributes;

/** @author Colin Decker */
public interface TestAttributes extends BasicFileAttributes {

    String foo();

    long bar();

    int baz();
}
