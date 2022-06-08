package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.test.CheckMetazones;
import org.unicode.cldr.util.CLDRFile.DraftStatus;

/**
 * This contains additional tests in JUnit.
 *
 * @see {@link org.unicode.cldr.unittest.TestCLDRFile}
 * @see {@link CLDRFile}
 */
public class TestCLDRFile {

    static Factory factory = null;

    @BeforeAll
    public static void setUp() {
        factory = CLDRConfig.getInstance().getFullCldrFactory();
    }

    @ParameterizedTest
    @ValueSource(strings = { "de", "fr", "root", })
    public void TestExtraMetazonePaths(String locale) {
        CLDRFile f = factory.make(locale, true);
        assertNotNull(f, "CLDRFile for " + locale);
        Set<String> rawExtraPaths = f.getRawExtraPaths();
        assertNotNull(rawExtraPaths, "RawExtraPaths for " + locale);
        for (final String path : rawExtraPaths) {
            if (path.indexOf("/metazone") >= 0) {
                assertFalse(CheckMetazones.isDSTPathForNonDSTMetazone(path), "DST path for non-DST zone: " + locale + ":" + path);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = DraftStatus.class)
    public void TestDraftStatus(DraftStatus status) {
        final String asXpath = status.asXpath();
        if (status == DraftStatus.approved) {
            assertEquals("", asXpath, "for " + status);
        } else {
            assertNotEquals("", asXpath, "for " + status);
        }
        assertAll("misc tests for " + status,
            () -> assertEquals(status, DraftStatus.forXpath("//ldml/someLeaf" + asXpath)),
            () -> assertEquals(status, DraftStatus.forString(status.name())),
            () -> assertEquals(status, DraftStatus.forString(status.name().toUpperCase())));
    }

    /**
     * Test that we can read all XML files in common.
     * Comment out from the ValueSource any dirs that don't have XML that is suitable for CLDRFile.
     * @param subdir
     */
    @ParameterizedTest
    @ValueSource(strings = {
        // common stuff
        "common/bcp47",
        "common/subdivisions",
        "common/supplemental",
        "common/annotations",
        "common/collation",
        "common/rbnf",
        /*"common/testData",*/
        "common/annotationsDerived",
        /* common/dtd */
        "common/segments",
        "common/transforms",
        "common/bcp47",
        "common/main",
        "common/subdivisions",
        /*"common/uca",*/
        "common/casing",
        /*"common/properties",*/
        "common/supplemental",
        "common/validity",

        // Test a couple of others:
        "seed/main",
        "exemplars/main",
    })
    public void TestReadAllDTDs(final String subdir) {
        Path aPath = CLDRConfig.getInstance().getCldrBaseDirectory().toPath().resolve(subdir);
        Factory factory = Factory.make(aPath.toString(), ".*");
        assertNotNull(factory);

        // Just test one file from each dir.
        {
            final String id = factory.getAvailable().iterator().next(); // Get the first id.
            TestReadAllDTDs(subdir, factory, id);
        }

        // Test ALL files in each dir. Adds ~35s not including seed or exemplars.
        // for (final String id : factory.getAvailable()) {
        //     TestReadAllDTDs(subdir, factory, id);
        // }
    }

    private void TestReadAllDTDs(final String subdir, Factory factory, final String id) {
        CLDRFile file = factory.make(id, false);

        assertNotNull(file, id);
        for (final String xpath : file.fullIterable()) {
            assertNotNull(xpath, subdir + ":" + id + " xpath");
            /*final String value = */ file.getStringValue(xpath);
        }

        for (Iterator<String> i = file.iterator(); i.hasNext();) {
            final String xpath = i.next();
            assertNotNull(xpath, subdir + ":" + id + " xpath");
            /*final String value = */ file.getStringValue(xpath);
        }
        // This is to simulate what is in the LDML2JsonConverter
        final Comparator<String> comparator = DtdData.getInstance(file.getDtdType()).getDtdComparator(null);
        for (Iterator<String> it = file.iterator("", comparator); it.hasNext();) {
            final String xpath = it.next();
            assertNotNull(xpath, subdir + ":" + id + " xpath");
        }
    }

    @Test
    public void testSourceLocale() {
        final File unittest_dir = new File(CLDRPaths.UNITTEST_DATA_DIR);
        final File testcommonmain = new File(unittest_dir, "common/main");
        final File testfile = new File(testcommonmain, "hy.xml");
        final File rootfile = new File(testcommonmain, "root.xml");
        final File[] dirs = { testcommonmain };
        Factory myFactory = SimpleFactory.make(dirs, ".*");
        final CLDRFile hyFile = myFactory.make("hy", true);

        {
            final String xpath = "//ldml/localeDisplayNames/languages/language[@type=\"az\"]";
            // no location - code-fallback
            assertNull(hyFile.getSourceLocation(xpath), "expected null location for " + xpath);
        }
        {
            // found in hy.xml
            final String xpath = "//ldml/localeDisplayNames/languages/language[@type=\"aa\"]";
            XMLSource.SourceLocation location = hyFile.getSourceLocation(xpath);
            assertNotNull(location, "location for " + xpath);
            assertEquals(testfile.toPath().toString(), location.getSystem(), "system for " + xpath);
            assertEquals(17, location.getLine(), "line for " + xpath);
            assertEquals(43, location.getColumn(), "col for " + xpath);
        }
        {
            // found in hy.xml
            final String xpath = "//ldml/localeDisplayNames/languages/language[@type=\"ab\"]";
            XMLSource.SourceLocation location = hyFile.getSourceLocation(xpath);
            assertNotNull(location, "location for " + xpath);
            assertEquals(testfile.toPath().toString(), location.getSystem(), "system for " + xpath);
            assertEquals(18, location.getLine(), "line for " + xpath);
            assertEquals(64, location.getColumn(), "col for " + xpath);
        }
        {
            // found in root.xml
            final String xpath = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"pm\"]";
            XMLSource.SourceLocation location = hyFile.getSourceLocation(xpath);
            assertNotNull(location, "location for " + xpath);
            assertEquals(rootfile.toPath().toString(), location.getSystem(), "system for " + xpath);
            assertEquals(22, location.getLine(), "line for " + xpath);
            assertEquals(43, location.getColumn(), "col for " + xpath);
        }
    }
}
