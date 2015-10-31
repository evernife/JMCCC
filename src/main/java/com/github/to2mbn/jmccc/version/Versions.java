package com.github.to2mbn.jmccc.version;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.json.JSONException;
import com.github.to2mbn.jmccc.option.MinecraftDirectory;

/**
 * A tool class for resolving versions.
 * 
 * @author yushijinhun
 */
public final class Versions {

    private static final VersionParser PARSER = new VersionParser();

    /**
     * Resolves the version.
     * 
     * @param minecraftDir the minecraft directory
     * @param version the version name
     * @return the version object, null if <code>version==null</code>, or the version does not exist
     * @throws IOException if an I/O error has occurred during resolving version
     * @throws NullPointerException if <code>minecraftDir==null</code>
     */
    public static Version resolveVersion(MinecraftDirectory minecraftDir, String version) throws IOException {
        Objects.requireNonNull(minecraftDir);
        if (version == null) {
            return null;
        }

        if (doesVersionExists(minecraftDir, version)) {
            try {
                return PARSER.parse(minecraftDir, version);
            } catch (JSONException e) {
                throw new IOException("unable to resolve json", e);
            }
        } else {
            return null;
        }
    }

    /**
     * Returns a set of versions in the given minecraft directory.
     * <p>
     * This method returns a non-threaded safe, unordered set.
     * 
     * @param minecraftDir the minecraft directory
     * @return a set of versions
     * @throws NullPointerException if <code>minecraftDir==null</code>
     */
    public static Set<String> getVersions(MinecraftDirectory minecraftDir) {
        Objects.requireNonNull(minecraftDir);
        Set<String> versions = new HashSet<>();

        // null if the 'versions' dir not exists
        File[] subdirs = minecraftDir.getVersions().listFiles();
        if (subdirs != null) {
            for (File file : subdirs) {
                if (file.isDirectory() && doesVersionExists(minecraftDir, file.getName())) {
                    versions.add(file.getName());
                }
            }
        }
        return versions;
    }

    private static boolean doesVersionExists(MinecraftDirectory minecraftDir, String version) {
        return minecraftDir.getVersionJson(version).isFile();
    }

    private Versions() {
    }

}