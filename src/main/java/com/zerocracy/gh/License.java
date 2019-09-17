/*
 * Copyright (c) 2016-2019 Zerocracy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to read
 * the Software only. Permissions is hereby NOT GRANTED to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.zerocracy.gh;

import com.jcabi.github.Repo;
import java.io.IOException;
import java.util.regex.Pattern;
import org.cactoos.Text;

/**
 * Github repo license.
 *
 * @since 1.0
 */
public final class License {

    /**
     * Open source license pattern.
     */
    // @checkstyle LineLength (2 line)
    private static final Pattern OSS_PTN = Pattern.compile(
        "mit|l?gpl(?:-\\d\\.\\d)?|apache-2\\.0|bsd-\\d-clause(?:-clear)?|wtfpl",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * License key.
     */
    private final Text key;

    /**
     * Ctor.
     * @param repo Github repo
     */
    public License(final Repo repo) {
        this(new LicenseKey(repo));
    }

    /**
     * Ctor.
     *
     * @param key License key
     */
    public License(final Text key) {
        this.key = key;
    }

    /**
     * Check of the license is open sourced.
     * @return True if open sourced
     */
    public boolean oss() throws IOException {
        return License.OSS_PTN.matcher(this.key.asString()).matches();
    }
}
