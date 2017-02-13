/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that compares semantic versions from one to another.
 *
 * @since 1.0.0
 */
public class VersionComparator {

    private int major;
    private int minor;
    private int revision;
    private boolean isBeta;
    private int betaNumber;

    public VersionComparator(String version) {
        int[] versions = parse(version);
        major = versions[0];
        minor = versions[1];
        revision = versions[2];

        if (versions[3] > 0) {
            isBeta = true;
            betaNumber = versions[3];
        }
    }

    private int[] parse(String version) {
        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(\\s*[Bb]eta\\s*(\\d*))?").matcher(version);
        if (!m.matches())
            throw new IllegalArgumentException("Malformed version string");

        return new int[] { Integer.parseInt(m.group(1)),  // major
                Integer.parseInt(m.group(2)),             // minor
                Integer.parseInt(m.group(3)),             // rev.
                m.group(4) == null ? 0                    // no beta suffix
                        : m.group(5).isEmpty() ? 1        // "beta"
                        : Integer.parseInt(m.group(5))    // "beta3"
        };
    }

    public boolean isNewerThan(VersionComparator comparator) {
        if (this.major > comparator.getMajor())
            return true;
        else if (this.major == comparator.getMajor() && this.minor > comparator.getMinor())
            return true;
        else if (this.major == comparator.getMajor() && this.minor == comparator.getMinor() && this.revision > comparator.getRevision())
            return true;
        else if (this.major == comparator.getMajor() && this.minor == comparator.getMinor() && this.revision == comparator.getRevision() && this.betaNumber > comparator.getBetaNumber())
            return true;

        return false;
    }

    public boolean isOlderThan(VersionComparator comparator) {
        if (this.major < comparator.getMajor())
            return true;
        else if (this.major == comparator.getMajor() && this.minor < comparator.getMinor())
            return true;
        else if (this.major == comparator.getMajor() && this.minor == comparator.getMinor() && this.revision < comparator.getRevision())
            return true;
        else if (this.major == comparator.getMajor() && this.minor == comparator.getMinor() && this.revision == comparator.getRevision() && this.betaNumber < comparator.getBetaNumber())
            return true;

        return false;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof VersionComparator) {
            VersionComparator comparator =  (VersionComparator)object;
            return this.major == comparator.getMajor() &&
                    this.minor == comparator.getMinor() &&
                    this.revision == comparator.getRevision() &&
                    this.isBeta == comparator.isBeta() &&
                    this.betaNumber == comparator.getBetaNumber();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 1000*(major+1) + 100*(minor+1) + 10*(revision+1) + (betaNumber+1);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return revision;
    }

    public boolean isBeta() {
        return isBeta;
    }

    public int getBetaNumber() {
        return betaNumber;
    }

    @Override
    public String toString() {
        // Do not change this. Upgrade logic depends on the format and that the format can be parsed by this class
        StringBuilder sb = new StringBuilder();
        sb.append(major).append(".").append(minor).append(".").append(revision);
        if (isBeta) {
            sb.append(" Beta ").append(betaNumber);
        }
        return sb.toString();
    }

}