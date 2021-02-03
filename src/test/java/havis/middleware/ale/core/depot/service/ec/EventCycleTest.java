package havis.middleware.ale.core.depot.service.ec;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.EventCycleType;
import havis.middleware.ale.config.EventCyclesType;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.config.SubscribersType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.EC;
import havis.middleware.ale.service.ec.ECBoundarySpec;
import havis.middleware.ale.service.ec.ECReportOutputSpec;
import havis.middleware.ale.service.ec.ECReportSetSpec;
import havis.middleware.ale.service.ec.ECReportSpec;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.ale.service.mc.MCEventCycleSpec;
import havis.middleware.ale.service.mc.MCProperty;
import havis.middleware.ale.service.mc.MCSpec;
import havis.middleware.ale.service.mc.MCSubscriberSpec;
import havis.middleware.ale.service.mc.MCSubscriberSpec.Properties;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class EventCycleTest {

    @BeforeClass
    public static void init() {
    	Config.getInstance();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void addEventCycleType() {
        EventCycle cycle = new EventCycle();
        final String ecName = "name";
        final ECSpec ecSpec = new ECSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
                setLogicalReaders(new LogicalReaders() {
                    {
                        logicalReader = Arrays.asList(new String[] { "reader" });
                    }
                });
                setBoundarySpec(new ECBoundarySpec());
                setReportSpecs(new ReportSpecs() {
                    {
                        reportSpec = Arrays.asList(new ECReportSpec[] { new ECReportSpec() {
                            {
                                setReportSet(new ECReportSetSpec() {
                                    {
                                        setSet("set");
                                    }
                                });
                                setOutput(new ECReportOutputSpec() {
                                    {
                                    }
                                });
                                setReportName("name");
                            }
                        } });
                    }
                });
            }
        };
        cycle.add(new EventCycleType() {
            {
                setName(ecName);
                setEnable(Boolean.FALSE);
                setSpec(ecSpec);
            }
        });
    }

    @Test
    public void addStringECSpec() {
        EventCycle cycle = new EventCycle();
        final String name = "name";
        final ECSpec spec = new ECSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
                setLogicalReaders(new LogicalReaders() {
                    {
                        logicalReader = Arrays.asList(new String[] { "reader" });
                    }
                });
                setBoundarySpec(new ECBoundarySpec());
                setReportSpecs(new ReportSpecs() {
                    {
                        reportSpec = Arrays.asList(new ECReportSpec[] { new ECReportSpec() {
                            {
                                setReportSet(new ECReportSetSpec() {
                                    {
                                        setSet("set");
                                    }
                                });
                                setOutput(new ECReportOutputSpec() {
                                    {
                                    }
                                });
                                setReportName("name");
                            }
                        } });
                    }
                });
            }
        };
        cycle.add(name, spec);
    }



    @Test
    public void getMCEventCycleSpec() {
        EventCycle cycle = new EventCycle();
        final String typeName = "name";
        final ECSpec typeSpec = new ECSpec();
        for (final Boolean isEnabled : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            MCEventCycleSpec tmp = cycle.get(new EventCycleType() {
                {
                    setName(typeName);
                    setEnable(isEnabled);
                    setSpec(typeSpec);
                }
            });
            Assert.assertEquals(typeName, tmp.getName());
            Assert.assertEquals(isEnabled, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(typeSpec, tmp.getSpec());
        }
    }

    @Test
    public void getEventCycleType() {
        EventCycle cycle = new EventCycle();
        final String cycleName = "name";
        final ECSpec cycleSpec = new ECSpec();
        for (final Boolean isEnabled : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            EventCycleType tmp = cycle.get(new MCEventCycleSpec() {
                {
                    setName(cycleName);
                    setEnable(isEnabled);
                    setSpec(cycleSpec);
                }
            });
            Assert.assertEquals(cycleName, tmp.getName());
            Assert.assertEquals(isEnabled, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(cycleSpec, tmp.getSpec());
        }
    }



    @Test
    public void getInstance() {
        Assert.assertNotNull(EventCycle.getInstance());
    }

    @Test
    public void getList(@Mocked final EventCyclesType type) {
        EventCycle cycle = new EventCycle();
        EventCycleType eventCycleType = new EventCycleType();
        eventCycleType.setName("test");
        final ArrayList<EventCycleType> expected = new ArrayList<>(Arrays.asList(eventCycleType));
        new NonStrictExpectations() {
            {
                type.getEventCycle();
                result = expected;
            }

        };
        Assert.assertNotNull(cycle.getList());
        Assert.assertEquals(1, cycle.getList().size());
        Assert.assertEquals("test", cycle.getList().get(0).getName());
    }

    @Test
    public void define(@Mocked final EC ec) {
        EventCycle cycle = new EventCycle();
        try {
            final String typeName = "name";
            final ECSpec typeSpec = new ECSpec();
            new NonStrictExpectations() {
                {
                    EC.getInstance();
                    result = ec;
                }
            };
            for (final Boolean e : new Boolean[] { Boolean.TRUE }) {
                EventCycleType eventCycleType = new EventCycleType();
                eventCycleType.setName(typeName);
                eventCycleType.setEnable(e);
                eventCycleType.setSpec(typeSpec);
                cycle.define(eventCycleType);
                new Verifications() {
                    {
                        ec.define(typeName, typeSpec, false);
                        times = 1;
                    }
                };
            }
        } catch (ALEException e) {
            Assert.fail();
        }
    }

    @Test
    public void undefine(@Mocked final EC ec) {
        EventCycle cycle = new EventCycle();
        try {
            final String typeName = "name";
            final ECSpec typeSpec = new ECSpec();
            new NonStrictExpectations() {
                {
                    EC.getInstance();
                    result = ec;
                }
            };
            for (final Boolean e : new Boolean[] { Boolean.TRUE }) {
                EventCycleType eventCycleType = new EventCycleType();
                eventCycleType.setName(typeName);
                eventCycleType.setEnable(e);
                eventCycleType.setSpec(typeSpec);
                cycle.undefine(eventCycleType);
                new Verifications() {
                    {
                        ec.undefine(typeName, false);
                        times = 1;
                    }
                };
            }
        } catch (ALEException e) {
            Assert.fail();
        }
    }

    // Methods inherited from base classes Cycle and Depot

    @Test
    public void initWithDefine(@Mocked final EventCyclesType type, @Mocked final EC ec) throws NoSuchIdException, ValidationException, DuplicateNameException,
            ImplementationException, InvalidURIException, DuplicateSubscriptionException, NoSuchNameException {
        final EventCycleType eventCycleType = new EventCycleType();
        eventCycleType.setName("testCycle");
        eventCycleType.setEnable(Boolean.TRUE);
        eventCycleType.setSpec(new ECSpec());
        eventCycleType.setSubscribers(new SubscribersType());
        SubscriberType subscriber = new SubscriberType();
        subscriber.setEnable(Boolean.TRUE);
        subscriber.setUri("http://test");
        subscriber.setName(subscriber.getUri());
        final PropertiesType props = new PropertiesType();
        subscriber.setProperties(props);
        eventCycleType.getSubscribers().getSubscriber().add(subscriber);
        final ArrayList<EventCycleType> cycleTypeList = new ArrayList<>(Arrays.asList(eventCycleType));
        new NonStrictExpectations() {
            {
                type.getEventCycle();
                result = cycleTypeList;

                ec.define("testCycle", eventCycleType.getSpec(), false);
                ec.subscribe("testCycle", "http://test", props, false);
            }
        };

        EventCycle cycle = new EventCycle(); // make sure we create a new event
                                             // cycle
        cycle.init();

        new Verifications() {
            {
                ec.define("testCycle", eventCycleType.getSpec(), false);
                times = 1;

                ec.subscribe("testCycle", "http://test", props, false);
                times = 1;
            }
        };
    }

    @Test
    public void initWithException(@Mocked final EventCyclesType type, @Mocked final EC ec) throws NoSuchIdException, ValidationException, DuplicateNameException,
            ImplementationException, InvalidURIException, DuplicateSubscriptionException, NoSuchNameException {
        final EventCycleType eventCycleType = new EventCycleType();
        eventCycleType.setName("testCycle");
        eventCycleType.setEnable(Boolean.TRUE);
        eventCycleType.setSpec(new ECSpec());
        eventCycleType.setSubscribers(new SubscribersType());
        SubscriberType subscriber = new SubscriberType();
        subscriber.setEnable(Boolean.TRUE);
        subscriber.setUri("http://test");
        subscriber.setName(subscriber.getUri());
        final PropertiesType props = new PropertiesType();
        subscriber.setProperties(props);
        eventCycleType.getSubscribers().getSubscriber().add(subscriber);
        final ArrayList<EventCycleType> cycleTypeList = new ArrayList<>(Arrays.asList(eventCycleType));
        new NonStrictExpectations() {
            {
                type.getEventCycle();
                result = cycleTypeList;

                ec.define("testCycle", eventCycleType.getSpec(), false);
                result = new ALEException("failed");
                ec.subscribe("testCycle", "http://test", props, false);
            }
        };

        EventCycle cycle = new EventCycle(); // make sure we create a new event cycle
        cycle.init();

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        new Verifications() {
            {
                ec.define("testCycle", eventCycleType.getSpec(), false);
                times = 1;

                ec.subscribe("testCycle", "http://test", props, false);
                times = 0;
            }
        };
    }

    private EventCycle createCycleWithSingleEntry(final EventCyclesType type) throws NoSuchIdException, ValidationException, DuplicateNameException, ImplementationException, InvalidURIException, DuplicateSubscriptionException, NoSuchNameException {
        return createCycleWithSingleEntry(type, true);
    }

    private EventCycle createCycleWithSingleEntry(final EventCyclesType type, boolean enabled) throws NoSuchIdException, ValidationException, DuplicateNameException,
            ImplementationException, InvalidURIException, DuplicateSubscriptionException, NoSuchNameException {
        final EventCycleType eventCycleType = new EventCycleType();
        eventCycleType.setName("testCycle");
        eventCycleType.setEnable(Boolean.valueOf(enabled));
        eventCycleType.setSpec(new ECSpec());
        eventCycleType.setSubscribers(new SubscribersType());
        SubscriberType subscriber = new SubscriberType();
        subscriber.setEnable(Boolean.valueOf(enabled));
        subscriber.setUri("http://test");
        subscriber.setName(subscriber.getUri());
        final PropertiesType props = new PropertiesType();
        subscriber.setProperties(props);
        eventCycleType.getSubscribers().getSubscriber().add(subscriber);
        final ArrayList<EventCycleType> cycleTypeList = new ArrayList<>(Arrays.asList(eventCycleType));
        new NonStrictExpectations() {
            {
                type.getEventCycle();
                result = cycleTypeList;
            }
        };

        EventCycle cycle = new EventCycle(); // make sure we create a new event
                                             // cycle
        cycle.init(); // single cycle loaded
        return cycle;
    }

    @Test
    public void removeByName(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ImplementationException, ValidationException,
            DuplicateNameException, InvalidURIException, DuplicateSubscriptionException, NoSuchNameException, NoSuchIdException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        UUID removedId = cycle.remove("testCycle");

        Assert.assertEquals(id, removedId.toString());

        ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(0, ids.size());
    }

    @Test
    public void removeByUuid(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        cycle.remove(UUID.fromString(id));

        ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(0, ids.size());

        new Verifications() {
            {
                ec.undefine("testCycle", false);
                times = 1;
            }
        };
    }

    @Test
    public void removeByUuidWithException(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        new NonStrictExpectations() {
            {
                ec.undefine("testCycle", false);
                result = new NoSuchNameException("No such name");
            }
        };

        cycle.remove(UUID.fromString(id));

        ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(0, ids.size());

        new Verifications() {
            {
                ec.undefine("testCycle", false);
                times = 1;
            }
        };
    }

    @Test
    public void removeSubscriberById(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        cycle.remove(UUID.fromString(subscriberId), UUID.fromString(id));

        ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        id = ids.get(0);
        Assert.assertNotNull(ids.get(0));

        subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(0, subscriberIds.size());
    }

    @Test
    public void removeSubscriberByUri(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        cycle.remove("testCycle", new URI("http://test"));

        ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        id = ids.get(0);
        Assert.assertNotNull(ids.get(0));

        subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(0, subscriberIds.size());
    }

    @Test
    public void addSubscriberByUri(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        cycle.add("testCycle", new URI("http://test2"));

        ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        id = ids.get(0);
        Assert.assertNotNull(id);

        subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(2, subscriberIds.size());
        Assert.assertNotNull(subscriberIds.get(0));
        Assert.assertNotNull(subscriberIds.get(1));

        new Verifications() {
            {
                ec.subscribe("testCycle", "http://test2", null, false);
                times = 0;
            }
        };
    }

    @Test
    public void addSubscriberBySpec(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        final MCSubscriberSpec spec = new MCSubscriberSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setUri("http://test2");
        spec.setName(spec.getUri());
        Properties props = new Properties();
        props.getProperty().add(new MCProperty("test1", "value1"));
        props.getProperty().add(new MCProperty("test2", "value2"));
		spec.setProperties(props);

        cycle.add(spec, UUID.fromString(id));

        ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        id = ids.get(0);
        Assert.assertNotNull(id);

        subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(2, subscriberIds.size());
        Assert.assertNotNull(subscriberIds.get(0));
        Assert.assertNotNull(subscriberIds.get(1));

        new Verifications() {
            {
            	PropertiesType properties;
                ec.subscribe("testCycle", "http://test2", properties = withCapture(), false);
                times = 1;
                
                Assert.assertNotNull(properties);
                Assert.assertEquals(2, properties.getProperty().size());
                Assert.assertEquals("test1", properties.getProperty().get(0).getName());
                Assert.assertEquals("value1", properties.getProperty().get(0).getValue());
                Assert.assertEquals("test2", properties.getProperty().get(1).getName());
                Assert.assertEquals("value2", properties.getProperty().get(1).getValue());
            }
        };
    }

    @Test
    public void addSubscriberByMcSpecValidationException(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        final MCSpec spec = new MCSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setName("http://test2");

        try {
            cycle.add(spec, UUID.fromString(id));
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
        }

        ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        id = ids.get(0);
        Assert.assertNotNull(id);

        subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        Assert.assertNotNull(subscriberIds.get(0));

        new Verifications() {
            {
                ec.subscribe("testCycle", "http://test2", null, false);
                times = 0;
            }
        };
    }

    @Test
    public void addMCSpec(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        final MCEventCycleSpec spec = new MCEventCycleSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setName("testCycle2");
        spec.setSpec(new ECSpec());

        cycle.add(spec);

        new Verifications() {
            {
                ec.define("testCycle2", spec.getSpec(), false);
                times = 1;
            }
        };
    }

    @Test
    public void addMCSpecValidationException(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        final MCSubscriberSpec spec = new MCSubscriberSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setName("testSubscriber2");

        try {
            cycle.add(spec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
        }
    }

    @Test
    public void updateDisable(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertTrue(cycle.get(UUID.fromString(id)).isEnable());

        MCEventCycleSpec spec = new MCEventCycleSpec();
        spec.setEnable(Boolean.FALSE);
        spec.setName("testCycle");
        spec.setSpec(new ECSpec());

        cycle.update(UUID.fromString(id), spec);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());

        new Verifications() {
            {
                ec.undefine("testCycle", false);
                times = 1;
            }
        };
    }

    @Test
    public void updateDisableWithMcSpec(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertTrue(cycle.get(UUID.fromString(id)).isEnable());

        MCSpec spec = new MCSpec();
        spec.setEnable(Boolean.FALSE);
        spec.setName("testCycle");

        cycle.update(UUID.fromString(id), spec);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());

        new Verifications() {
            {
                ec.undefine("testCycle", false);
                times = 1;
            }
        };
    }

    @Test
    public void updateEnable(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());

        final MCEventCycleSpec spec = new MCEventCycleSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setName("testCycle");
        spec.setSpec(new ECSpec());

        cycle.update(UUID.fromString(id), spec);

        Assert.assertTrue(cycle.get(UUID.fromString(id)).isEnable());

        new Verifications() {
            {
                ec.define("testCycle", spec.getSpec(), false);
                times = 1;
            }
        };
    }

    @Test
    public void updateEnableWithMcSpec(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());

        final MCSpec spec = new MCSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setName("testCycle");

        cycle.update(UUID.fromString(id), spec);

        Assert.assertTrue(cycle.get(UUID.fromString(id)).isEnable());

        new Verifications() {
            {
                ec.define("testCycle", (ECSpec) any, false);
                times = 1;
            }
        };
    }

    @Test
    public void updateKeepEnabled(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertTrue(cycle.get(UUID.fromString(id)).isEnable());

        final MCEventCycleSpec spec = new MCEventCycleSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setName("testCycle");
        spec.setSpec(new ECSpec());

        try {
            cycle.update(UUID.fromString(id), spec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
        }

        Assert.assertTrue(cycle.get(UUID.fromString(id)).isEnable());

        new Verifications() {
            {
                ec.define("testCycle", spec.getSpec(), false);
                times = 0;
                ec.undefine("testCycle", false);
                times = 0;
            }
        };
    }

    @Test
    public void updateKeepDisabled(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());

        final MCEventCycleSpec spec = new MCEventCycleSpec();
        spec.setEnable(Boolean.FALSE);
        spec.setName("testCycle");
        spec.setSpec(new ECSpec());

        cycle.update(UUID.fromString(id), spec);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());

        new Verifications() {
            {
                ec.define("testCycle", spec.getSpec(), false);
                times = 0;
                ec.undefine("testCycle", false);
                times = 0;
            }
        };
    }

    @Test
    public void updateName(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());
        Assert.assertEquals("testCycle", cycle.get(UUID.fromString(id)).getName());

        final MCEventCycleSpec spec = new MCEventCycleSpec();
        spec.setEnable(Boolean.FALSE);
        spec.setName("testCycleNewName");
        spec.setSpec(new ECSpec());

        cycle.update(UUID.fromString(id), spec);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());
        Assert.assertEquals("testCycleNewName", cycle.get(UUID.fromString(id)).getName());

        new Verifications() {
            {
                ec.define("testCycle", spec.getSpec(), false);
                times = 0;
                ec.undefine("testCycle", false);
                times = 0;
            }
        };
    }

    @Test
    public void updateNameWithMcSpec(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());
        Assert.assertEquals("testCycle", cycle.get(UUID.fromString(id)).getName());

        final MCSpec spec = new MCSpec();
        spec.setEnable(Boolean.FALSE);
        spec.setName("testCycleNewName");

        cycle.update(UUID.fromString(id), spec);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());
        Assert.assertEquals("testCycleNewName", cycle.get(UUID.fromString(id)).getName());

        new Verifications() {
            {
                ec.define("testCycle", (ECSpec) any, false);
                times = 0;
                ec.undefine("testCycle", false);
                times = 0;
            }
        };
    }

    @Test
    public void updateNameWithMcSpecAndException(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());
        Assert.assertEquals("testCycle", cycle.get(UUID.fromString(id)).getName());

        final MCSpec spec = new MCSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setName("testCycleNewName");

        new NonStrictExpectations() {
            {
                ec.define("testCycleNewName", (ECSpec) any, false);
                result = new ALEException("failed");
            }
        };

        try {
            cycle.update(UUID.fromString(id), spec);
            Assert.fail("Expected ALEException");
        } catch (ALEException e) {
        }

        // name unchanged and stays disabled
        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());
        Assert.assertEquals("testCycle", cycle.get(UUID.fromString(id)).getName());
    }

    @Test
    public void updateNoName(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());
        Assert.assertEquals("testCycle", cycle.get(UUID.fromString(id)).getName());

        final MCEventCycleSpec spec = new MCEventCycleSpec();
        spec.setEnable(Boolean.FALSE);
        spec.setName(null);
        spec.setSpec(new ECSpec());

        cycle.update(UUID.fromString(id), spec);

        Assert.assertFalse(cycle.get(UUID.fromString(id)).isEnable());
        Assert.assertEquals(null, cycle.get(UUID.fromString(id)).getName());

        new Verifications() {
            {
                ec.define("testCycle", spec.getSpec(), false);
                times = 0;
                ec.undefine("testCycle", false);
                times = 0;
            }
        };
    }

    @Test
    public void updateSubscriberUri(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        final MCSubscriberSpec spec = new MCSubscriberSpec();
        spec.setEnable(Boolean.FALSE);
        spec.setUri("http://testNew");
        spec.setName(spec.getUri());

        cycle.update(UUID.fromString(subscriberId), spec, UUID.fromString(id));
    }

    @Test
    public void updateSubscriberEnable(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type, false);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        final MCSubscriberSpec spec = new MCSubscriberSpec();
        spec.setEnable(Boolean.TRUE);
        spec.setUri("http://test");
        spec.setName(spec.getUri());
        Properties props = new Properties();
        props.getProperty().add(new MCProperty("test1", "value1"));
        props.getProperty().add(new MCProperty("test2", "value2"));
		spec.setProperties(props);

        cycle.update(UUID.fromString(subscriberId), spec, UUID.fromString(id));

        new Verifications() {
            {
				PropertiesType properties;
				ec.subscribe("testCycle", "http://test", properties = withCapture(), false);
				times = 1;

				Assert.assertNotNull(properties);
				Assert.assertEquals(2, properties.getProperty().size());
				Assert.assertEquals("test1", properties.getProperty().get(0).getName());
				Assert.assertEquals("value1", properties.getProperty().get(0).getValue());
				Assert.assertEquals("test2", properties.getProperty().get(1).getName());
				Assert.assertEquals("value2", properties.getProperty().get(1).getValue());
            }
        };
    }

    @Test
    public void getSubscriber(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        MCSubscriberSpec actual = cycle.get(UUID.fromString(subscriberId), UUID.fromString(id));

        Assert.assertNotNull(actual);
        Assert.assertEquals("http://test", actual.getName());
        Assert.assertEquals("http://test", actual.getUri());
        Assert.assertTrue(actual.isEnable());
    }

    @Test
    public void getSubscriberNoSuchIdException(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        List<String> subscriberIds = cycle.toList(UUID.fromString(id));
        Assert.assertNotNull(subscriberIds);
        Assert.assertEquals(1, subscriberIds.size());
        String subscriberId = subscriberIds.get(0);
        Assert.assertNotNull(subscriberId);

        try {
            cycle.get(UUID.fromString(subscriberId), UUID.randomUUID());
            Assert.fail("Expected NoSuchIdException");
        } catch (NoSuchIdException e) {
        }
    }

    @Test
    public void getCycle(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        List<String> ids = cycle.toList();
        Assert.assertNotNull(ids);
        Assert.assertEquals(1, ids.size());
        String id = ids.get(0);
        Assert.assertNotNull(id);

        MCEventCycleSpec actual = cycle.get(UUID.fromString(id));

        Assert.assertNotNull(actual);
        Assert.assertEquals("testCycle", actual.getName());
        Assert.assertNotNull(actual.getSpec());
        Assert.assertTrue(actual.isEnable());
    }

    @Test
    public void getCycleException(@Mocked final EventCyclesType type, @Mocked final EC ec) throws ALEException, URISyntaxException {
        EventCycle cycle = createCycleWithSingleEntry(type);

        try {
            cycle.get(UUID.randomUUID());
            Assert.fail("Expected NoSuchIdException");
        } catch (NoSuchIdException e) {
        }
    }
}
