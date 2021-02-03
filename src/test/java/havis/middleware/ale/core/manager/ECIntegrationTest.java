package havis.middleware.ale.core.manager;

import havis.middleware.ale.Helper;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.Reader;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.subscriber.Subscriber;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.service.ec.ECReport;
import havis.middleware.ale.service.ec.ECReportGroup;
import havis.middleware.ale.service.ec.ECReportGroupListMember;
import havis.middleware.ale.service.ec.ECReportMemberField;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.ale.service.ec.ObjectFactory;
import havis.middleware.utils.threading.Pipeline;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ECIntegrationTest {

    @BeforeClass
    public static void init() {
        ECIntegrationTestResetter.reset();
        ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    private Tag createTag(String urn, String... dataHex) {

        Tag tag = TagDecoder.getInstance().fromUrn(urn);
        tag.setTid(new byte[] { 0x00 }); // any TID will suffice
        HashMap<Integer, Result> result = new HashMap<>();
        int i = 1;
        for (String hex : dataHex) {
            byte[] data = DatatypeConverter.parseHexBinary(hex);
            result.put(Integer.valueOf(i++), new ReadResult(ResultState.SUCCESS, data));
        }
        tag.setResult(result);
        return tag;
    }

    private Tag createRawTag(String primaryKeyHex, String dataHex, String tidHex) {
        return createRawTag(primaryKeyHex, dataHex, tidHex, null);
    }

    private Tag createRawTag(String primaryKeyHex, String dataHex, String tidHex, String userBankHex, String... additionalValueHex) {
        byte[] epc = DatatypeConverter.parseHexBinary(dataHex.substring(4));
        byte[] data = DatatypeConverter.parseHexBinary(dataHex);
        byte[] tid = null;
        Tag tag = new Tag(epc);
        if (tidHex != null) {
            tid = DatatypeConverter.parseHexBinary(tidHex);
            tag.setTid(tid);
        }
        byte[] primaryKey = null;
        if (primaryKeyHex != null) {
            primaryKey = DatatypeConverter.parseHexBinary(primaryKeyHex);
        }
        HashMap<Integer, Result> result = new HashMap<>();
        int i = 1;
        if (primaryKey != null && !Arrays.equals(primaryKey, epc) && !Arrays.equals(primaryKey, tid)) {
            // only add primary key value if the primary key is not the epc or
            // tid
            result.put(Integer.valueOf(i++), new ReadResult(ResultState.SUCCESS, primaryKey));
        }
        if (data != null) {
            result.put(Integer.valueOf(i++), new ReadResult(ResultState.SUCCESS, data));
        }
        if (tid != null) {
            result.put(Integer.valueOf(i++), new ReadResult(ResultState.SUCCESS, tid));
        }
        if (userBankHex != null) {
            result.put(Integer.valueOf(i++), new ReadResult(ResultState.SUCCESS, DatatypeConverter.parseHexBinary(userBankHex)));
        }
        if (additionalValueHex != null && additionalValueHex.length > 0) {
            for (String value : additionalValueHex) {
                result.put(Integer.valueOf(i++), new ReadResult(ResultState.SUCCESS, DatatypeConverter.parseHexBinary(value)));
            }
        }
        tag.setResult(result);
        return tag;
    }

    /**
     * A test for define
     *
     * @throws FileNotFoundException
     * @throws ValidationException
     * @throws ImplementationException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void defineTest(final @Mocked Reader reader, final @Mocked ReaderController controller) throws FileNotFoundException, ImplementationException,
            ValidationException {

        new NonStrictExpectations() {
            {
                reader.get(anyString, anyString, (Map<String, String>) any);
                result = controller;
            }
        };

        // Step 001
        ECIntegrationTestHelper.define("001", new ECSpec(), new ValidationException("Failed to define event cycle '001': No boundary specification given"));

        // Step 002
        ECIntegrationTestHelper.define("/EC/Validate", "002", new ValidationException(
                "Failed to define event cycle '002': The trigger '' is invalid. Trigger uri could not be a empty string"));

        // Step 003
        ECIntegrationTestHelper.define("/EC/Validate", "003", new ValidationException(
                "Failed to define event cycle '003': The trigger 'urn:' is invalid. Trigger is not supported"));

        // Step 004
        ECIntegrationTestHelper.define("/EC/Validate", "004", new ValidationException(
                "Failed to define event cycle '004': The trigger 'urn:epcglobal:ale:trigger:x:' is invalid. EPCglobal trigger is not supported"));

        // Step 005
        ECIntegrationTestHelper.define("/EC/Validate", "005", new ValidationException(
                "Failed to define event cycle '005': The trigger 'urn:epcglobal:ale:trigger:rtc:' is invalid. No period or offset given in rtc trigger"));

        // Step 006
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "006",
                        new ValidationException(
                                "Failed to define event cycle '006': The trigger 'urn:epcglobal:ale:trigger:rtc:null.null' is invalid. No period or offset given in rtc trigger"));

        // Step 007
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "007",
                        new ValidationException(
                                "Failed to define event cycle '007': The trigger 'urn:epcglobal:ale:trigger:rtc:0.0' is invalid. Period of rtc trigger not greater then zero or greater then seconds of a day"));

        // Step 008
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "008",
                        new ValidationException(
                                "Failed to define event cycle '008': The trigger 'urn:epcglobal:ale:trigger:rtc:86400001.0' is invalid. Period of rtc trigger not greater then zero or greater then seconds of a day"));

        // Step 009
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "009",
                        new ValidationException(
                                "Failed to define event cycle '009': The trigger 'urn:epcglobal:ale:trigger:rtc:8640000.8640000' is invalid. Offset of rtc trigger less then zero or not less then period"));

        // Step 010
        // offset less then zero
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "010",
                        new ValidationException(
                                "Failed to define event cycle '010': The trigger 'urn:epcglobal:ale:trigger:rtc:1000.-1' is invalid. No period or offset given in rtc trigger"));

        // Step 011
        // offset not less then period
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "011",
                        new ValidationException(
                                "Failed to define event cycle '011': The trigger 'urn:epcglobal:ale:trigger:rtc:1000.1001' is invalid. Offset of rtc trigger less then zero or not less then period"));

        // Step 012
        // Time without unit
        ECIntegrationTestHelper.define("/EC/Validate", "012", new ValidationException(
                "Failed to define event cycle '012': Stable set interval of event cycle '012' is invalid. Time unit must be MS"));

        // Step 013
        // Time less then zero
        ECIntegrationTestHelper.define("/EC/Validate", "013", new ValidationException(
                "Failed to define event cycle '013': Stable set interval of event cycle '013' is invalid. Time value must not be negative"));

        // Step 014
        ECIntegrationTestHelper.define("/EC/Validate", "014", new ValidationException("Failed to define event cycle '014': No stop condition given"));

        // Define reader
        LRIntegrationTestHelper.define("/EC/LRSpec", "001");

        // Step 020
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "020",
                        new ValidationException(
                                "Failed to define event cycle '020': Report specification 'current' is invalid. Group is invalid. Pattern 'urn:epc:pat:sgtin-96:1.392177.1234567.4711' is invalid. Pattern is not disjoint"));

        // Step 021
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "021",
                        new ValidationException(
                                "Failed to define event cycle '021': Report specification 'current' is invalid. Group is invalid. Pattern 'urn:epc:pat:sgtin-96:1.392177.1234567.[4718-4719]' is invalid. Pattern is not disjoint"));

        // Step 022
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "022",
                        new ValidationException(
                                "Failed to define event cycle '022': Report specification 'current' is invalid. Group is invalid. Pattern 'urn:epc:pat:sgtin-96:1.392177.*.*' is invalid. Pattern is not disjoint"));

        // Step 025
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "025",
                        new ValidationException(
                                "Failed to define event cycle '025': Report specification 'current' is invalid. Group is invalid. Pattern 'x9' is invalid. Pattern is not disjoint"));

        // Step 026
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "026",
                        new ValidationException(
                                "Failed to define event cycle '026': Report specification 'current' is invalid. Group is invalid. Pattern '&x12=x1b' is invalid. Pattern is not disjoint"));

        // Step 027
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "027",
                        new ValidationException(
                                "Failed to define event cycle '027': Report specification 'current' is invalid. Group is invalid. Pattern '[7-9]' is invalid. Pattern is not disjoint"));

        // Step 028
        ECIntegrationTestHelper
                .define("/EC/Validate",
                        "028",
                        new ValidationException(
                                "Failed to define event cycle '028': Report specification 'current' is invalid. Group is invalid. Pattern '17' is invalid. Pattern is not disjoint"));

        // RM42
        ECIntegrationTestHelper.define("/EC/Validate", "RM42", new ValidationException(
                "Failed to define event cycle 'RM42': Report specification 'current' is invalid. The output already contains the field 'epc'"));

        // Duplicate names
        String name = "001";
        ECIntegrationTestHelper.define("/EC/ECSpec/", name);
        ECIntegrationTestHelper.define("/EC/ECSpec/", name, new DuplicateNameException("Event cycle '" + name + "' already defined"));
        ECIntegrationTestHelper.undefine(name);

        // Undefine reader
        LRIntegrationTestHelper.undefine("001");
    }

    /**
     * A test for getECSpec
     *
     * @throws FileNotFoundException
     * @throws ValidationException
     * @throws ImplementationException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getECSpecTest(final @Mocked Reader reader, final @Mocked ReaderController controller) throws FileNotFoundException, ImplementationException,
            ValidationException {

        new NonStrictExpectations() {
            {
                reader.get(anyString, anyString, (Map<String, String>) any);
                result = controller;
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "001");

        Map<String, ECSpec> dict = new HashMap<String, ECSpec>();

        for (String name : new String[] { "001", "002", "003" }) {
            dict.put(name, ECIntegrationTestHelper.define("/EC/ECSpec/", name));
        }

        for (Entry<String, ECSpec> pair : dict.entrySet()) {
            Assert.assertEquals(ECIntegrationTestHelper.getECSpec(pair.getKey()), pair.getValue());
        }

        for (String name : dict.keySet()) {
            ECIntegrationTestHelper.undefine(name);
        }

        LRIntegrationTestHelper.undefine("001");

        ECIntegrationTestHelper.getECSpec("000", new NoSuchNameException("Could not get specification for unknown event cycle '000'"));
    }

    /**
     * A test for getECSpecNames
     *
     * @throws FileNotFoundException
     * @throws ValidationException
     * @throws ImplementationException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getECSpecNamesTest(final @Mocked Reader reader, final @Mocked ReaderController controller) throws FileNotFoundException,
            ImplementationException, ValidationException {

        new NonStrictExpectations() {
            {
                reader.get(anyString, anyString, (Map<String, String>) any);
                result = controller;
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "001");

        String[] names = new String[] { "001", "002", "003" };
        for (String name : names) {
            ECIntegrationTestHelper.define("/EC/ECSpec/", name);
        }
        List<String> list = ECIntegrationTestHelper.getECSpecNames();
        Assert.assertTrue(Arrays.equals(names, list.toArray(new String[] {})));

        for (String name : names) {
            ECIntegrationTestHelper.undefine(name);
        }

        LRIntegrationTestHelper.undefine("001");
    }

    /**
     * A test for getStandardVersion
     */
    @Test
    public void getStandardVersionTest() {
        ECIntegrationTestHelper.getStandardVersion();
    }

    /**
     * A test for getSubscribers
     *
     * @throws FileNotFoundException
     * @throws InterruptedException
     * @throws ValidationException
     * @throws ImplementationException
     * @throws URISyntaxException
     * @throws InvalidURIException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getSubscribersTest(final @Mocked Reader reader, final @Mocked ReaderController controller, final @Mocked Subscriber subscriber,
            final @Mocked SubscriberController c1, final @Mocked SubscriberController c2, final @Mocked SubscriberController c3) throws FileNotFoundException,
            InterruptedException, ImplementationException, ValidationException, InvalidURIException, URISyntaxException {

        new NonStrictExpectations() {
            {
                reader.get("001", "Test", (Map<String, String>) any);
                result = controller;

                subscriber.get(new URI("test://1"), null, ECReports.class);
                result = c1;
                c1.getURI();
                result = new URI("test://1");

                subscriber.get(new URI("test://2"), null, ECReports.class);
                result = c2;
                c2.getURI();
                result = new URI("test://2");

                subscriber.get(new URI("test://3"), null, ECReports.class);
                result = c3;
                c3.getURI();
                result = new URI("test://3");
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "001");

        String[] uris = new String[] { "test://1", "test://2", "test://3" };

        String spec = "001";
        ECIntegrationTestHelper.define("/EC/ECSpec/", spec);

        for (String uri : uris) {
            ECIntegrationTestHelper.subscribe(spec, uri);
        }

        Assert.assertTrue(Arrays.equals(uris, ECIntegrationTestHelper.getSubscribers(spec).toArray(new String[] {})));

        for (String uri : uris) {
            ECIntegrationTestHelper.unsubscribe(spec, uri);
        }

        ECIntegrationTestHelper.subscribe(spec, uris[0]);
        Thread.sleep(2500);
        ECIntegrationTestHelper.unsubscribe(spec, uris[0]);

        ECIntegrationTestHelper.undefine(spec);

        LRIntegrationTestHelper.undefine("001");

        ECIntegrationTestHelper.getSubscribers("000", new NoSuchNameException("Could not get subscribers for unknown event cycle '000'"));
    }

    /**
     * A test for getVendorVersion
     */
    @Test
    public void getVendorVersionTest() {
        ECIntegrationTestHelper.getVendorVersion();
    }

    /**
     * A test for immediate
     *
     * @throws ValidationException
     * @throws ImplementationException
     *
     */
    @SuppressWarnings("unchecked")
    @Test
    public void immediateTest(final @Mocked Reader reader, final @Mocked ReaderController controller) throws ImplementationException, ValidationException {

        final List<Caller<Tag>> callers = new ArrayList<>();

        // delegate for the caller object passed when defining the reader
        final Delegate<TagOperation> delegate = new Delegate<TagOperation>() {
            @SuppressWarnings("unused")
            public void invoked(TagOperation operation) {
                if (callers.size() == 1) {
                    scanTags(callers.get(callers.size() - 1), controller, createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "30397EFC44B5A24000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4712", "30397EFC44B5A24000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4720", "30397EFC44B5A24000001270"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4711", "30397EFC44B5A1C000001267"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4712", "30397EFC44B5A1C000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4713", "30397EFC44B5A1C000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4714", "30397EFC44B5A1C00000126A"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4715", "30397EFC44B5A1C00000126B"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4716", "30397EFC44B5A1C00000126C"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4719", "30397EFC44B5A1C00000126F"));
                }
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("002", "Test", (Map<String, String>) any);
                result = controller;

                controller.define((TagOperation) any, withCapture(callers), anyString);
                controller.enable(with(delegate));
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "002");
        ECReports reports = ECIntegrationTestHelper.immediate("/EC/ECSpec", "010");
        LRIntegrationTestHelper.undefine("002");

        ECReports expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "010", ECReports.class);

        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 5);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());

        expected.setDate(reports.getDate());
        expected.setCreationDate(reports.getCreationDate());

        assertReports(reports, expected);

        ECIntegrationTestHelper.immediate(new ECSpec(), new ValidationException("Failed to define event cycle: No boundary specification given"));
    }

    @Test
    public void pollTwiceTest(final @Mocked Reader reader, final @Mocked ReaderController controller) throws IOException, ImplementationException,
            ValidationException {

        final List<Caller<Tag>> callers = new ArrayList<>();

        // delegate for the caller object passed when defining the reader
        final Delegate<TagOperation> delegate = new Delegate<TagOperation>() {
            @SuppressWarnings("unused")
            public void invoked(TagOperation operation) {
                if (callers.size() <= 2) {
                    scanTags(callers.get(callers.size() - 1), controller, createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "30397EFC44B5A24000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4712", "30397EFC44B5A24000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4720", "30397EFC44B5A24000001270"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4711", "30397EFC44B5A1C000001267"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4712", "30397EFC44B5A1C000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4713", "30397EFC44B5A1C000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4714", "30397EFC44B5A1C00000126A"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4715", "30397EFC44B5A1C00000126B"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4716", "30397EFC44B5A1C00000126C"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4719", "30397EFC44B5A1C00000126F"));
                }
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("002", "Test", this.<HashMap<String, String>> withNotNull());
                result = controller;

                controller.define((TagOperation) any, withCapture(callers), anyString);
                controller.enable(with(delegate));
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "002");

        String name = "090";
        ECIntegrationTestHelper.define("/EC/ECSpec/", name);
        ECReports actual1 = ECIntegrationTestHelper.poll(name);
        ECReports actual2 = ECIntegrationTestHelper.poll(name);
        ECIntegrationTestHelper.undefine(name);
        LRIntegrationTestHelper.undefine("002");

        ECReports expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "090", ECReports.class);

        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - actual1.getTotalMilliseconds()) < 25);
        expected.setTotalMilliseconds(actual1.getTotalMilliseconds());

        expected.setDate(actual1.getDate());
        expected.setSpecName(name);
        expected.setCreationDate(actual1.getCreationDate());

        assertReports(actual1, expected);

        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - actual2.getTotalMilliseconds()) < 25);
        expected.setTotalMilliseconds(actual2.getTotalMilliseconds());

        expected.setDate(actual2.getDate());
        expected.setSpecName(name);
        expected.setCreationDate(actual2.getCreationDate());

        assertReports(actual2, expected);

        ECIntegrationTestHelper.poll(name, new NoSuchNameException("Could not poll on unknown event cycle '" + name + "'"));
    }

    /**
     * A test for poll
     *
     * @throws IOException
     * @throws ValidationException
     * @throws ImplementationException
     */
    @Test
    public void pollTest(final @Mocked Reader reader, final @Mocked ReaderController controller) throws IOException, ImplementationException,
            ValidationException {

        final List<Caller<Tag>> callers = new ArrayList<>();

        // delegate for the caller object passed when defining the reader
        final Delegate<TagOperation> delegate = new Delegate<TagOperation>() {
            @SuppressWarnings("unused")
            public void invoked(TagOperation operation) {
                if (callers.size() <= 2) {
                    scanTags(callers.get(callers.size() - 1), controller, createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "30397EFC44B5A24000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4712", "30397EFC44B5A24000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4720", "30397EFC44B5A24000001270"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4711", "30397EFC44B5A1C000001267"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4712", "30397EFC44B5A1C000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4713", "30397EFC44B5A1C000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4714", "30397EFC44B5A1C00000126A"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4715", "30397EFC44B5A1C00000126B"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4716", "30397EFC44B5A1C00000126C"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4719", "30397EFC44B5A1C00000126F"));
                }
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("002", "Test", this.<HashMap<String, String>> withNotNull());
                result = controller;

                controller.define((TagOperation) any, withCapture(callers), anyString);
                controller.enable(with(delegate));
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "002");

        String name = "010";
        ECIntegrationTestHelper.define("/EC/ECSpec/", name);
        ECReports actual = ECIntegrationTestHelper.poll(name);
        ECIntegrationTestHelper.undefine(name);
        LRIntegrationTestHelper.undefine("002");

        ECReports expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "010", ECReports.class);

        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - actual.getTotalMilliseconds()) < 25);
        expected.setTotalMilliseconds(actual.getTotalMilliseconds());

        expected.setDate(actual.getDate());
        expected.setSpecName(name);
        expected.setCreationDate(actual.getCreationDate());

        assertReports(actual, expected);

        ECIntegrationTestHelper.poll(name, new NoSuchNameException("Could not poll on unknown event cycle '" + name + "'"));
    }

    private void assertReports(ECReports actual, ECReports expected) {
    	boolean success = false;
    	try {
            Assert.assertEquals(expected.getALEID(), actual.getALEID());
            Assert.assertEquals(expected.getInitiationCondition(), actual.getInitiationCondition());
            Assert.assertEquals(expected.getInitiationTrigger(), actual.getInitiationTrigger());
            Assert.assertEquals(expected.getSchemaURL(), actual.getSchemaURL());
            Assert.assertEquals(expected.getSpecName(), actual.getSpecName());
            Assert.assertEquals(expected.getTerminationCondition(), actual.getTerminationCondition());
            Assert.assertEquals(expected.getTerminationTrigger(), actual.getTerminationTrigger());
            Assert.assertEquals(expected.getAny(), actual.getAny());
            Assert.assertEquals(expected.getCreationDate(), actual.getCreationDate());
            Assert.assertEquals(expected.getDate(), actual.getDate());
            Assert.assertEquals(expected.getSchemaVersion().floatValue(), 0, actual.getSchemaVersion().floatValue());

            Assert.assertEquals(expected.getReports().getReport().size(), actual.getReports().getReport().size());

            Iterator<ECReport> actualReportIterator = actual.getReports().getReport().iterator();
            Iterator<ECReport> expectedReportIterator = expected.getReports().getReport().iterator();

            while (expectedReportIterator.hasNext()) {
                ECReport actualReport = actualReportIterator.next();
                ECReport expectedReport = expectedReportIterator.next();

                Assert.assertEquals(expectedReport.getReportName(), actualReport.getReportName());
                Assert.assertEquals(expectedReport.getGroup().size(), actualReport.getGroup().size());
    
                Iterator<ECReportGroup> actualReportGroupIterator = actualReport.getGroup().iterator();
                Iterator<ECReportGroup> expectedReportGroupIterator = expectedReport.getGroup().iterator();

                while (expectedReportGroupIterator.hasNext()) {
                    ECReportGroup actualReportGroup = actualReportGroupIterator.next();
                    ECReportGroup expectedReportGroup = expectedReportGroupIterator.next();

                    Assert.assertEquals(expectedReportGroup.getGroupList().getMember().size(), actualReportGroup.getGroupList().getMember().size());

                    Iterator<ECReportGroupListMember> actualReportGroupMemberIterator = actualReportGroup.getGroupList().getMember().iterator();
                    Iterator<ECReportGroupListMember> expectedReportGroupMemberIterator = expectedReportGroup.getGroupList().getMember().iterator();
    
                    while (expectedReportGroupMemberIterator.hasNext()) {
                        ECReportGroupListMember actualMember = actualReportGroupMemberIterator.next();
                        ECReportGroupListMember expectedMember = expectedReportGroupMemberIterator.next();

                        if (expectedMember.getTag() == null) {
                            Assert.assertNull(actualMember.getTag());
                        } else {
                            Assert.assertEquals(expectedMember.getTag().getValue(), actualMember.getTag().getValue());
                        }

                        if (expectedMember.getEpc() == null) {
                            Assert.assertNull(actualMember.getEpc());
                        } else {
                            Assert.assertEquals(expectedMember.getEpc().getValue(), actualMember.getEpc().getValue());
                        }

                        if (expectedMember.getExtension() == null) {
                            Assert.assertNull(actualMember.getExtension());
                        } else {

                            Iterator<ECReportMemberField> actualFieldIterator = actualMember.getExtension().getFieldList().getField().iterator();
                            Iterator<ECReportMemberField> expectedFieldIterator = expectedMember.getExtension().getFieldList().getField().iterator();

                            while (expectedFieldIterator.hasNext()) {
                                ECReportMemberField actualField = actualFieldIterator.next();
                                ECReportMemberField expectedField = expectedFieldIterator.next();

                                Assert.assertEquals(expectedField.getName(), actualField.getName());
                                Assert.assertEquals(expectedField.getValue(), actualField.getValue());
                            }
                        }
                    }
                }
            }
            success = true;
    	} finally {
    		if (!success) {
    			System.out.println("Actual report:");
    			StringWriter writer = new StringWriter();
    			try {
					ECIntegrationTestReportsHelper.createMarshaller(ECReports.class).marshal(new ObjectFactory().createECReports(actual), writer);
				} catch (JAXBException e) {
					// ignore
				}
    			System.out.println(writer.toString());
    			System.out.println();
    		}
    	}
    }

    /**
     * A test for subscribe
     *
     * @throws IOException
     * @throws JAXBException
     * @throws ValidationException
     * @throws ImplementationException
     * @throws URISyntaxException
     * @throws InvalidURIException
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void subscribeTest(final @Mocked Reader reader, final @Mocked ReaderController controller, final @Mocked Subscriber subscriber,
            final @Mocked SubscriberController c1) throws IOException, JAXBException, ImplementationException, ValidationException, InvalidURIException,
            URISyntaxException, InterruptedException {

        final String subscriberUrl = "http://localhost:9999/";
        final List<Caller<Tag>> callers = new ArrayList<>();

        // delegate for the caller object passed when defining the reader
        final Delegate<TagOperation> scanDelegate = new Delegate<TagOperation>() {
            @SuppressWarnings("unused")
            public void invoked(TagOperation operation) {
                if (callers.size() == 1) {
                    scanTags(callers.get(callers.size() - 1), controller, createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "30397EFC44B5A24000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4712", "30397EFC44B5A24000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4720", "30397EFC44B5A24000001270"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4711", "30397EFC44B5A1C000001267"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4712", "30397EFC44B5A1C000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4713", "30397EFC44B5A1C000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4714", "30397EFC44B5A1C00000126A"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4715", "30397EFC44B5A1C00000126B"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4716", "30397EFC44B5A1C00000126C"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4719", "30397EFC44B5A1C00000126F"));
                }
            }
        };

        // delegate for the report object passed when queuing a report
        final Delegate<ECReports> enqueueDelegate = new Delegate<ECReports>() {
            @SuppressWarnings("unused")
            public void invoked(ECReports report) throws IOException, JAXBException {

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    Marshaller marshaller = Helper.createMarshaller(ECReports.class);
                    marshaller.marshal(new ObjectFactory().createECReports(report), os);
                } finally {
                    os.close();
                }

                byte[] data = os.toByteArray();

                HttpURLConnection connection = null;
                try {
                    URL url = new URL(subscriberUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    OutputStream outputStream = connection.getOutputStream();
                    try {
                        outputStream.write(data);
                    } finally {
                        outputStream.close();
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    try {
                        while (reader.readLine() != null) {
                        }
                    } finally {
                        reader.close();
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("002", "Test", (Map<String, String>) any);
                result = controller;

                controller.define((TagOperation) any, withCapture(callers), anyString);
                controller.enable(with(scanDelegate));

                subscriber.get(new URI(subscriberUrl), null, ECReports.class);
                result = c1;

                c1.getURI();
                result = new URI(subscriberUrl);

                c1.getActive();
                result = Boolean.TRUE;

                c1.enqueue(with(enqueueDelegate));
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "002");

        String name = "010";
        ECIntegrationTestHelper.define("/EC/ECSpec", name);

        String uri = "http://localhost:9999/";
        Callback<ECReports> callback = new Callback<ECReports>(new URL(uri), ECReports.class);
        ECIntegrationTestHelper.subscribe(name, uri);

        ECIntegrationTestHelper.subscribe(name, uri, new DuplicateSubscriptionException("URI '" + uri + "' already subscribed to event cycle '" + name + "'"));

        ECReports reports = callback.dequeue();
        ECIntegrationTestHelper.unsubscribe(name, uri);
        callback.dispose();

        ECIntegrationTestHelper.undefine(name);

        LRIntegrationTestHelper.undefine("002");

        ECReports expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "011", ECReports.class);

        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 5);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());

        expected.setDate(reports.getDate());
        expected.setSpecName(name);
        expected.setCreationDate(reports.getCreationDate());

        assertReports(reports, expected);

        ECIntegrationTestHelper.subscribe(name, uri, new NoSuchNameException("Could not subscribe to unknown event cycle '" + name + "'"));
    }

    public class Poll {

        Pipeline<ECReports> reports = new Pipeline<ECReports>();

        public Poll(final String name) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    reports.enqueue(ECIntegrationTestHelper.poll(name));
                }
            }).start();
        }

        public ECReports getReports() {
            synchronized (this) {
                return reports.dequeue();
            }
        }
    }

    /**
     * A test for undefine
     *
     * @throws FileNotFoundException
     * @throws InterruptedException
     * @throws ValidationException
     * @throws ImplementationException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void undefineTest(@Mocked final Reader reader, @Mocked final ReaderController controller) throws FileNotFoundException, InterruptedException,
            ImplementationException, ValidationException {

        final List<Caller<Tag>> callers = new ArrayList<>();

        // delegate for the caller object passed when defining the reader
        final Delegate<TagOperation> delegate = new Delegate<TagOperation>() {
            @SuppressWarnings("unused")
            public void invoked(TagOperation operation) {
                if (callers.size() == 1) {
                    scanTags(callers.get(callers.size() - 1), controller, createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4711", "30397EFC44B5A1C000001267"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4712", "30397EFC44B5A1C000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4713", "30397EFC44B5A1C000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234568.4713", "30397EFC44B5A20000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "30397EFC44B5A24000001269"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4714", "30397EFC44B5A1C00000126A"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4715", "30397EFC44B5A1C00000126B"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4716", "30397EFC44B5A1C00000126C"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4717", "30397EFC44B5A1C00000126D"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4718", "30397EFC44B5A1C00000126E"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4719", "30397EFC44B5A1C00000126F"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4712", "30397EFC44B5A24000001268"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4720", "30397EFC44B5A24000001270"));
                }
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("002", "Test", (Map<String, String>) any);
                result = controller;

                controller.define((TagOperation) any, withCapture(callers), anyString);
                controller.enable(with(delegate));
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "002");

        String name = "020";
        ECIntegrationTestHelper.define("/EC/ECSpec", name);

        Poll poll = new Poll(name);

        Thread.sleep(2500);

        ECIntegrationTestHelper.undefine(name);

        LRIntegrationTestHelper.undefine("002");

        ECReports reports = poll.getReports();

        ECReports expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "002", ECReports.class);

        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 50);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());

        expected.setDate(reports.getDate());
        expected.setSpecName(name);
        expected.setCreationDate(reports.getCreationDate());

        assertReports(reports, expected);

        ECIntegrationTestHelper.undefine(name, new NoSuchNameException("Could not undefine a unknown event cycle '" + name + "'"));
    }

    /**
     * A test for unsubscribe
     *
     * @throws FileNotFoundException
     * @throws ValidationException
     * @throws ImplementationException
     * @throws URISyntaxException
     * @throws InvalidURIException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void unsubscribeTest(@Mocked final Reader reader, @Mocked final ReaderController controller, final @Mocked Subscriber subscriber,
            final @Mocked SubscriberController c1) throws FileNotFoundException, ImplementationException, ValidationException, InvalidURIException,
            URISyntaxException {

        new NonStrictExpectations() {
            {
                reader.get("001", "Test", (Map<String, String>) any);
                result = controller;

                subscriber.get(new URI("test:///"), null, ECReports.class);
                result = c1;

                c1.getURI();
                result = new URI("test:///");
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "001");

        String name = "001";
        ECIntegrationTestHelper.define("/EC/ECSpec/", name);

        String uri = "test:///";

        ECIntegrationTestHelper.subscribe(name, uri);
        ECIntegrationTestHelper.unsubscribe(name, uri);

        ECIntegrationTestHelper.unsubscribe(name, uri, new NoSuchSubscriberException("URI '" + uri + "' not subscribed on event cycle '" + name + "'"));

        ECIntegrationTestHelper.undefine(name);

        LRIntegrationTestHelper.undefine("001");

        ECIntegrationTestHelper.unsubscribe(name, uri, new NoSuchNameException("Could not unsubscribe URI '" + uri + "' from unknown event cycle '" + name
                + "'"));
    }

    /**
     * A test for extension field
     *
     * @throws FileNotFoundException
     * @throws ValidationException
     * @throws ImplementationException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void fieldTest(final @Mocked Reader reader, final @Mocked ReaderController controller) throws FileNotFoundException, ImplementationException,
            ValidationException {
        final List<Caller<Tag>> callers = new ArrayList<>();

        // delegate for the caller object passed when defining the reader
        final Delegate<TagOperation> delegate = new Delegate<TagOperation>() {
            @SuppressWarnings("unused")
            public void invoked(TagOperation operation) {
                if (callers.size() == 1) {
                    scanTags(
                            callers.get(callers.size() - 1),
                            controller,
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4711", "300030397EFC44B5A1C000001267", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001267", "30397EFC44B5A1C000001267", "1267", "1267", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4712", "300030397EFC44B5A1C000001268", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001268", "30397EFC44B5A1C000001268", "1268", "1268", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4713", "300030397EFC44B5A1C000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001269", "30397EFC44B5A1C000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234568.4713", "300030397EFC44B5A20000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A20000001269", "30397EFC44B5A20000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "300030397EFC44B5A24000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001269", "30397EFC44B5A24000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4714", "300030397EFC44B5A1C00000126A", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126A", "30397EFC44B5A1C00000126A", "126A", "126A", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4715", "300030397EFC44B5A1C00000126B", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126B", "30397EFC44B5A1C00000126B", "126B", "126B", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4716", "300030397EFC44B5A1C00000126C", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126C", "30397EFC44B5A1C00000126C", "126C", "126C", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4717", "300030397EFC44B5A1C00000126D", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126D", "30397EFC44B5A1C00000126D", "126D", "126D", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4718", "300030397EFC44B5A1C00000126E", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126E", "30397EFC44B5A1C00000126E", "126E", "126E", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4719", "300030397EFC44B5A1C00000126F", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126F", "30397EFC44B5A1C00000126F", "126F", "126F", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4712", "300030397EFC44B5A24000001268", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001268", "30397EFC44B5A24000001268", "1268", "1268", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "300030397EFC44B5A24000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001269", "30397EFC44B5A24000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4720", "300030397EFC44B5A24000001270", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001270", "30397EFC44B5A24000001270", "1270", "1270", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4711", "300030397EFC44B5A1C000001267", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001267", "30397EFC44B5A1C000001267", "1267", "1267", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4712", "300030397EFC44B5A1C000001268", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001268", "30397EFC44B5A1C000001268", "1268", "1268", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4713", "300030397EFC44B5A1C000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001269", "30397EFC44B5A1C000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234568.4713", "300030397EFC44B5A20000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A20000001269", "30397EFC44B5A20000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "300030397EFC44B5A24000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001269", "30397EFC44B5A24000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4714", "300030397EFC44B5A1C00000126A", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126A", "30397EFC44B5A1C00000126A", "126A", "126A", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4715", "300030397EFC44B5A1C00000126B", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126B", "30397EFC44B5A1C00000126B", "126B", "126B", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4716", "300030397EFC44B5A1C00000126C", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126C", "30397EFC44B5A1C00000126C", "126C", "126C", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4717", "300030397EFC44B5A1C00000126D", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126D", "30397EFC44B5A1C00000126D", "126D", "126D", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4718", "300030397EFC44B5A1C00000126E", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126E", "30397EFC44B5A1C00000126E", "126E", "126E", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4719", "300030397EFC44B5A1C00000126F", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126F", "30397EFC44B5A1C00000126F", "126F", "126F", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4712", "300030397EFC44B5A24000001268", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001268", "30397EFC44B5A24000001268", "1268", "1268", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "300030397EFC44B5A24000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001269", "30397EFC44B5A24000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4720", "300030397EFC44B5A24000001270", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001270", "30397EFC44B5A24000001270", "1270", "1270", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4711", "300030397EFC44B5A1C000001267", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001267", "30397EFC44B5A1C000001267", "1267", "1267", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4712", "300030397EFC44B5A1C000001268", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001268", "30397EFC44B5A1C000001268", "1268", "1268", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4713", "300030397EFC44B5A1C000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C000001269", "30397EFC44B5A1C000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234568.4713", "300030397EFC44B5A20000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A20000001269", "30397EFC44B5A20000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "300030397EFC44B5A24000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001269", "30397EFC44B5A24000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4714", "300030397EFC44B5A1C00000126A", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126A", "30397EFC44B5A1C00000126A", "126A", "126A", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4715", "300030397EFC44B5A1C00000126B", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126B", "30397EFC44B5A1C00000126B", "126B", "126B", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4716", "300030397EFC44B5A1C00000126C", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126C", "30397EFC44B5A1C00000126C", "126C", "126C", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4717", "300030397EFC44B5A1C00000126D", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126D", "30397EFC44B5A1C00000126D", "126D", "126D", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4718", "300030397EFC44B5A1C00000126E", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126E", "30397EFC44B5A1C00000126E", "126E", "126E", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234567.4719", "300030397EFC44B5A1C00000126F", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A1C00000126F", "30397EFC44B5A1C00000126F", "126F", "126F", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4712", "300030397EFC44B5A24000001268", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001268", "30397EFC44B5A24000001268", "1268", "1268", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4713", "300030397EFC44B5A24000001269", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001269", "30397EFC44B5A24000001269", "1269", "1269", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"),
                            createTag("urn:epc:tag:sgtin-96:1.392177.1234569.4720", "300030397EFC44B5A24000001270", "E200600305AB8739",
                                    "BBB4300030397EFC44B5A24000001270", "30397EFC44B5A24000001270", "1270", "1270", "00FFEF0123456789", "BBB43000", "3000",
                                    "3000"));
                }
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("002", "Test", (Map<String, String>) any);
                result = controller;

                controller.define((TagOperation) any, withCapture(callers), anyString);
                controller.enable(with(delegate));
            }
        };

        TMIntegrationTestHelper.define("/EC/TMSpec", "001");

        LRIntegrationTestHelper.define("/EC/LRSpec", "002");

        ECReports reports = ECIntegrationTestHelper.immediate("/EC/ECSpec", "030");

        LRIntegrationTestHelper.undefine("002");

        ECReports expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "030", ECReports.class);

        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 50);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());

        expected.setDate(reports.getDate());
        expected.setCreationDate(reports.getCreationDate());

        assertReports(reports, expected);

        TMIntegrationTestHelper.undefineTMSpec("001");
    }

    private void scanTags(final Caller<Tag> caller, final ReaderController controller, final Tag... tags) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                for (Tag tag : tags) {
                    caller.invoke(tag, controller);
                }
            }
        }, 10);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void primaryKeyTest(final @Mocked Reader reader, final @Mocked ReaderController controller) throws ImplementationException, ValidationException {

        final List<Caller<Tag>> callers = new ArrayList<>();

        // delegate for the caller object passed when defining the reader
        final Delegate<TagOperation> delegate = new Delegate<TagOperation>() {
            @SuppressWarnings("unused")
            public void invoked(TagOperation operation) {
                switch (callers.size()) {
                case 1: // first immediate call
                    scanTags(callers.get(callers.size() - 1), controller, createRawTag(null, "2233445566778899AABBCCDDEEFF", "00"));
                    break;
                case 2: // second immediate call
                    scanTags(callers.get(callers.size() - 1), controller, createRawTag("FEDCBA9876543210", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543210"),
                            createRawTag("FEDCBA9876543211", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543211"),
                            createRawTag("FEDCBA9876543212", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543212"),
                            createRawTag("FEDCBA9876543213", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543213"),
                            createRawTag("FEDCBA9876543214", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543214"),
                            createRawTag("FEDCBA9876543215", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543215"));
                    break;
                case 3: // third immediate call
                    scanTags(
                            callers.get(callers.size() - 1),
                            controller,
                            createRawTag("AAAA", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543210",
                                    "AAAAFF0000000000000000000000000000000000000000000000000000000000", "AAAA"),
                            createRawTag("BBBB", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543211",
                                    "BBBBFF0000000000000000000000000000000000000000000000000000000000", "BBBB"),
                            createRawTag("CCCC", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543212",
                                    "CCCC0F0000000000000000000000000000000000000000000000000000000000", "CCCC"));
                    break;
                case 4: // fourth immediate call
                    scanTags(
                            callers.get(callers.size() - 1),
                            controller,
                            createRawTag("F0", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543210",
                                    "AAAAFF0000000000000000000000000000000000000000000000000000000000", "F0"),
                            createRawTag("0F", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543212",
                                    "CCCC0F0000000000000000000000000000000000000000000000000000000000", "0F"));
                    break;
                default:
                    break;
                }
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("007", "Test", (Map<String, String>) any);
                result = controller;

                controller.define((TagOperation) any, withCapture(callers), anyString);
                controller.enable(with(delegate));
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "007");

        ECReports reports;
        ECReports expected;

        reports = ECIntegrationTestHelper.immediate("/EC/ECSpec", "070");
        expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "070", ECReports.class);
        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 5);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());
        expected.setDate(reports.getDate());
        expected.setCreationDate(reports.getCreationDate());

        assertReports(reports, expected);

        reports = ECIntegrationTestHelper.immediate("/EC/ECSpec", "071");
        expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "071", ECReports.class);
        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 5);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());
        expected.setDate(reports.getDate());
        expected.setCreationDate(reports.getCreationDate());
        assertReports(reports, expected);

        reports = ECIntegrationTestHelper.immediate("/EC/ECSpec", "072");
        expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "072", ECReports.class);
        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 5);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());
        expected.setDate(reports.getDate());
        expected.setCreationDate(reports.getCreationDate());
        assertReports(reports, expected);

        reports = ECIntegrationTestHelper.immediate("/EC/ECSpec", "073");
        expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "073", ECReports.class);
        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 5);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());
        expected.setDate(reports.getDate());
        expected.setCreationDate(reports.getCreationDate());
        assertReports(reports, expected);

        LRIntegrationTestHelper.undefine("007");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOID(final @Mocked Reader reader, final @Mocked ReaderController controller) throws FileNotFoundException, ImplementationException,
            ValidationException {
        final List<Caller<Tag>> callers = new ArrayList<>();

        // delegate for the caller object passed when defining the reader
        final Delegate<TagOperation> delegate = new Delegate<TagOperation>() {
            @SuppressWarnings("unused")
            public void invoked(TagOperation operation) {
                scanTags(
                        callers.get(callers.size() - 1),
                        controller,
                        createRawTag("F0", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543210",
                                "896EC0B81ED46DA6D2E98C3CD3859D39E717F914E0308FB8FD8265C0000000000000000000000000000000000000000000000000000000000000000000000000", "896EC0B81ED46DA6D2E98C3CD3859D39E717F914E0308FB8FD8265C0000000000000000000000000000000000000000000000000000000000000000000000000"),
                        createRawTag("0F", "2233445566778899AABBCCDDEEFF", "FEDCBA9876543212",
                                "896EC0B81ED46DA6D2E98C3CD3859D39E717F914E0308FB8FD8265C0000000000000000000000000000000000000000000000000000000000000000000000000", "896EC0B81ED46DA6D2E98C3CD3859D39E717F914E0308FB8FD8265C0000000000000000000000000000000000000000000000000000000000000000000000000"));
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("007", "Test", (Map<String, String>) any);
                result = controller;

                controller.define((TagOperation) any, withCapture(callers), anyString);
                controller.enable(with(delegate));
            }
        };

        LRIntegrationTestHelper.define("/EC/LRSpec", "007");

        ECIntegrationTestHelper.define("/EC/ECSpec", "080");
        ECReports reports = ECIntegrationTestHelper.poll("080");
        ECReports expected = ECIntegrationTestReportsHelper.get("/EC/ECReports", "080", ECReports.class);
        Assert.assertTrue(Math.abs(expected.getTotalMilliseconds() - reports.getTotalMilliseconds()) < 5);
        expected.setTotalMilliseconds(reports.getTotalMilliseconds());
        expected.setDate(reports.getDate());
        expected.setCreationDate(reports.getCreationDate());
        assertReports(reports, expected);
        ECIntegrationTestHelper.undefine("080");

        LRIntegrationTestHelper.undefine("007");
    }

}
