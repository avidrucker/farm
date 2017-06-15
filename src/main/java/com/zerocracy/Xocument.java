/**
 * Copyright (c) 2016-2017 Zerocracy
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
package com.zerocracy;

import com.jcabi.xml.StrictXML;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XSL;
import com.jcabi.xml.XSLDocument;
import com.zerocracy.jstk.Item;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.cactoos.Func;
import org.cactoos.func.StickyFunc;
import org.cactoos.func.UncheckedFunc;
import org.cactoos.io.InputAsBytes;
import org.cactoos.io.InputAsLSInput;
import org.cactoos.io.PathAsInput;
import org.cactoos.io.StickyInput;
import org.cactoos.io.UrlAsInput;
import org.cactoos.text.BytesAsText;
import org.cactoos.text.UncheckedText;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xembly.Directive;
import org.xembly.Directives;
import org.xembly.Xembler;

/**
 * XML document.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class Xocument {

    /**
     * Current DATUM version.
     */
    private static final String VERSION = "0.23";

    /**
     * Compressing XSL.
     */
    private static final XSL COMPRESS = XSLDocument.make(
        Xocument.class.getResource("compress.xsl")
    );

    /**
     * Locator of XSD resources.
     */
    private static final Func<String, LSInput> LOCATOR = new StickyFunc<>(
        (Func<String, LSInput>) loc -> {
            final String[] parts = loc.split(" ");
            return new InputAsLSInput(
                new StickyInput(new UrlAsInput(parts[3])),
                parts[2], parts[3], parts[4]
            );
        }
    );

    /**
     * Resolver.
     */
    private static final LSResourceResolver RESOLVER =
        (type, namespace, pid, sid, base) ->
            new UncheckedFunc<>(Xocument.LOCATOR).apply(
                String.format(
                    "%s %s %s %s %s",
                    type, namespace, pid, sid, base
                )
            );

    /**
     * File.
     */
    private final Path file;

    /**
     * Ctor.
     * @param item Item
     * @throws IOException If fails
     */
    public Xocument(final Item item) throws IOException {
        this(item.path());
    }

    /**
     * Ctor.
     * @param path File
     */
    public Xocument(final Path path) {
        this.file = path;
    }

    @Override
    public String toString() {
        return new UncheckedText(
            new BytesAsText(
                new InputAsBytes(
                    new PathAsInput(this.file)
                )
            )
        ).asString();
    }

    /**
     * Bootstrap it.
     * @param xsd Path of XSD
     * @return This
     * @throws IOException If fails
     */
    public Xocument bootstrap(final String xsd)
        throws IOException {
        final String root = StringUtils.substringAfterLast(xsd, "/");
        final String uri = String.format(
            // @checkstyle LineLength (1 line)
            "https://raw.githubusercontent.com/zerocracy/datum/%s/xsd/%s.xsd",
            Xocument.VERSION, xsd
        );
        if (!Files.exists(this.file) || Files.size(this.file) == 0L) {
            Files.write(
                this.file,
                String.format(
                    // @checkstyle LineLength (1 line)
                    "<%s xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='%s'/>",
                    root, uri
                ).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE
            );
        }
        final XML xml = new XMLDocument(this.file.toFile());
        final String schema = xml.xpath(
            String.format("/%s/@xsi:noNamespaceSchemaLocation", root)
        ).get(0);
        if (!schema.equals(uri)) {
            this.modify(
                new Directives().xpath(String.format("/%s", root)).attr(
                    "xsi:noNamespaceSchemaLocation", uri
                )
            );
        }
        return this;
    }

    /**
     * Query it.
     * @param xpath Query string
     * @return Found texts
     * @throws IOException If fails
     */
    public List<String> xpath(final String xpath) throws IOException {
        final XML xml = new StrictXML(
            new XMLDocument(this.file.toFile()),
            Xocument.RESOLVER
        );
        return xml.xpath(xpath);
    }

    /**
     * Query it.
     * @param xpath Query string
     * @return Found nodes
     * @throws IOException If fails
     */
    public List<XML> nodes(final String xpath) throws IOException {
        final XML xml = new StrictXML(
            new XMLDocument(this.file.toFile()),
            Xocument.RESOLVER
        );
        return xml.nodes(xpath);
    }

    /**
     * Modify it.
     * @param dirs Directives
     * @throws IOException If fails
     */
    public void modify(final Iterable<Directive> dirs) throws IOException {
        final XML before = new XMLDocument(this.toString());
        final Node node = before.node();
        new Xembler(dirs).applyQuietly(node);
        final String after = new StrictXML(
            Xocument.COMPRESS.transform(new XMLDocument(node)),
            Xocument.RESOLVER
        ).toString();
        if (!before.toString().equals(after)) {
            Files.write(this.file, after.getBytes(StandardCharsets.UTF_8));
        }
    }

}
