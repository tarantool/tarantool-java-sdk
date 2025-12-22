/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivan Bannikov
 * @author Aleksandr Pavlyuk
 *
 * <p>
 * Class representing Tarantool version. Holds info about major, minor, patch version, also
 * build tag (usually short hash).
 * </p>
 */
public class TarantoolVersion {
    /**
     * Regular expression for parsing version string from box.info.version
     */
    private static final String regex = "(\\d+)\\.(\\d+)\\.(\\d+)(?:-(.*))?";

    /**
     * Compiled regular expression, matcher
     */
    private static final Pattern pattern = Pattern.compile(regex);

    /**
     * Field with major version
     */
    private final int major;

    /**
     * Field with minor version
     */
    private final int minor;

    /**
     * Field with patch version
     */
    private final int patch;

    /**
     * Field with build tag
     * */
    private final String build;

    /**
     * <p>This constructor creates instance of {@link TarantoolVersion} with passed version info.</p>
     *
     * @param major   Major version
     * @param minor   Minor version
     * @param patch   Patch Version
     * @param build   String build tag
     **/
    public TarantoolVersion(int major, int minor, int patch, String build) {
      this.major = major;
      this.minor = minor;
      this.patch = patch;
      this.build = build;
    }

    /**
     * <p>Returns major version</p>
     *
     * @return {@link #major} value
     */
    public int getMajor() {
      return major;
    }

    /**
     * <p>Returns minor version</p>
     *
     * @return {@link #minor} value
     */
    public int getMinor() {
      return minor;
    }

    /**
     * <p>Returns patch version</p>
     *
     * @return {@link #patch} value
     */
    public int getPatch() {
      return patch;
    }

    /**
     * <p>Returns build tag</p>
     *
     * @return {@link #build} value
     */
    public String getBuild() {
      return build;
    }

    /**
     * <p>
     * Method for parsing version string
     * </p>
     *
     * @param tarantoolVersion string with version info, usually retrieved from `box.info.version`
     * @return instance of {@link TarantoolVersion} class.
     * @throws IllegalArgumentException when version string is not correct
     */
    public static TarantoolVersion parse(String tarantoolVersion) {
      Matcher matcher = pattern.matcher(tarantoolVersion);
      if (matcher.matches()) {
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String build = matcher.group(4);
        return new TarantoolVersion(major, minor, patch, build);
      }

      throw new IllegalArgumentException("Incorrect string with Tarantool version");
    }
}
