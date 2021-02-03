package havis.middleware.ale.core.depot.service.pc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.config.PCType;
import havis.middleware.ale.config.PortCycleType;
import havis.middleware.ale.config.PortCyclesType;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.config.SubscribersType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.PC;
import havis.middleware.ale.service.mc.MCPortCycleSpec;
import havis.middleware.ale.service.mc.MCSpec;
import havis.middleware.ale.service.mc.MCSubscriberSpec;
import havis.middleware.ale.service.pc.PCSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PortCycleTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(PortCycle.getInstance());
    }

    @Test
    public void add() throws ALEException {
        try {
            new PortCycle().add(new MCSpec());
            Assert.fail("Expected ALEException");
        } catch (ALEException e) {
            // ignore
        }

        final String specName = "name";
        final PCSpec pcSpec = new PCSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };

        PortCycle portCycle = new PortCycle();
        String uuid = portCycle.add(new MCPortCycleSpec() {
            {
                setName(specName);
                setEnable(Boolean.FALSE);
                setSpec(pcSpec);
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        });

        Assert.assertTrue(portCycle.toList().contains(uuid));
        MCPortCycleSpec mcSpec = portCycle.get(UUID.fromString(uuid));
        Assert.assertSame(pcSpec, mcSpec.getSpec());
        Assert.assertEquals(specName, mcSpec.getName());

        List<String> subscribers = portCycle.toList(UUID.fromString(uuid));
        Assert.assertNotNull(subscribers);
        Assert.assertEquals(0, subscribers.size());
    }

    @Test
    public void addNameSpec(@Mocked final PortCyclesType type) throws ALEException {

        new NonStrictExpectations() {
            {
                type.getPortCycle();
                result = new ArrayList<PortCycleType>();
            }
        };

        final String specName = "name";
        final PCSpec pcSpec = new PCSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };

        PortCycle portCycle = new PortCycle();

        Assert.assertEquals(0, portCycle.toList().size());
        portCycle.add(specName, pcSpec);
        Assert.assertEquals(1, portCycle.toList().size());

        MCPortCycleSpec mcSpec = portCycle.get(UUID.fromString(portCycle.toList().get(0)));
        Assert.assertSame(pcSpec, mcSpec.getSpec());
        Assert.assertEquals(specName, mcSpec.getName());

        List<String> subscribers = portCycle.toList(UUID.fromString(portCycle.toList().get(0)));
        Assert.assertNotNull(subscribers);
        Assert.assertEquals(0, subscribers.size());
    }

    @Test
    public void getMCPortCycleSpec() {
        final String specName = "name";
        final PCSpec pcSpec = new PCSpec();
        for (final Boolean isEnabled : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            MCPortCycleSpec tmp = new PortCycle().get(new PortCycleType() {
                {
                    setName(specName);
                    setEnable(isEnabled);
                    setSpec(pcSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(isEnabled, Boolean.valueOf(tmp.isEnable()));
            Assert.assertSame(pcSpec, tmp.getSpec());
        }
    }

    @Test
    public void getPortCycleType() {
        final String specName = "name";
        final PCSpec pcSpec = new PCSpec();
        for (final Boolean isEnabled : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            PortCycleType tmp = new PortCycle().get(new MCPortCycleSpec() {
                {
                    setName(specName);
                    setEnable(isEnabled);
                    setSpec(pcSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(isEnabled, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(pcSpec, tmp.getSpec());
        }
    }

    @Test
    public void getList(@Mocked final PCType type) throws NoSuchIdException {
        List<PortCycleType> expected = new ArrayList<>();
        final PortCyclesType pcTypes = new PortCyclesType();
        PortCycleType pcType = new PortCycleType("name", false, new PCSpec());
        pcType.setSubscribers(new SubscribersType());
        SubscriberType sbType = new SubscriberType();
        sbType.setEnable(Boolean.FALSE);
        sbType.setName("sbName");
        sbType.setUri("uri");
        pcType.getSubscribers().getSubscriber().add(sbType);
        pcTypes.getPortCycle().add(pcType);
        expected.add(pcType);
        new NonStrictExpectations() {
            {
                type.getPortCycles();
                result = pcTypes;
            }
        };
        PortCycle portCycle = new PortCycle();
        Assert.assertEquals(expected, portCycle.getList());

        List<String> list = portCycle.toList();
        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());
        List<String> sList = portCycle.toList(UUID.fromString(list.get(0)));
        Assert.assertNotNull(sList);
        Assert.assertEquals(1, sList.size());
        MCSubscriberSpec mcSubscriberSpec = portCycle.get(UUID.fromString(sList.get(0)), UUID.fromString(list.get(0)));
        Assert.assertNotNull(mcSubscriberSpec);
        Assert.assertEquals("uri", mcSubscriberSpec.getName()); // uri is used as name
        Assert.assertEquals("uri", mcSubscriberSpec.getUri());
        Assert.assertFalse(mcSubscriberSpec.isEnable());
    }

    @Test
    public void define(@Mocked final PC pc) throws ALEException {
        final String specName = "name";
        final PCSpec pcSpec = new PCSpec();
        PortCycle portCycle = new PortCycle();
        PortCycleType tmp = portCycle.get(new MCPortCycleSpec(specName, false, pcSpec));
        new NonStrictExpectations() {
            {
                PC.getInstance();
                result = pc;
            }
        };

        portCycle.define(tmp);

        new Verifications() {
            {
                pc.define(withEqual(specName), withEqual(pcSpec), false);
                times = 1;
            }
        };
    }

    @Test
    public void undefine(@Mocked final PC pc) throws ALEException {
        final String specName = "name";
        final PCSpec pcSpec = new PCSpec();
        PortCycle portCycle = new PortCycle();
        PortCycleType tmp = portCycle.get(new MCPortCycleSpec(specName, false, pcSpec));
        new NonStrictExpectations() {
            {
                PC.getInstance();
                result = pc;
            }
        };

        portCycle.undefine(tmp);

        new Verifications() {
            {
                pc.undefine(withEqual(specName), false);
                times = 1;
            }
        };
    }
}
