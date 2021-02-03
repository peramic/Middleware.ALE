package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.config.RandomType;
import havis.middleware.ale.config.RandomsType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.report.cc.data.Randoms;
import havis.middleware.ale.service.cc.RNGSpec;
import havis.middleware.ale.service.mc.MCRandomSpec;
import havis.middleware.ale.service.mc.MCSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class RandomTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void add() throws ALEException {
        try {
            new Random().add(new MCSpec());
            Assert.fail("Expected ALEException");
        } catch (ALEException e) {
            // ignore
        }
        final String specName = "name";
        final RNGSpec rSpec = new RNGSpec() {
            {
                setLength(1);
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };
        Random random = new Random();
        String uuid = random.add(new MCRandomSpec() {
            {
                setName(specName);
                setEnable(Boolean.FALSE);
                setSpec(rSpec);
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        });
        Assert.assertTrue(random.toList().contains(uuid));

        MCRandomSpec mcSpec = random.get(UUID.fromString(uuid));
        Assert.assertFalse(mcSpec.isEnable());
        Assert.assertSame(rSpec, mcSpec.getSpec());
        Assert.assertEquals(specName, mcSpec.getName());
    }

    @Test
    public void addNameSpec(@Mocked final RandomsType type) throws ALEException {

        new NonStrictExpectations() {
            {
                type.getRandom();
                result = new ArrayList<RandomType>();
            }
        };

        final String specName = "name";
        final RNGSpec randomSpec = new RNGSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };

        Random random = new Random();

        Assert.assertEquals(0, random.toList().size());
        random.add(specName, randomSpec);
        Assert.assertEquals(1, random.toList().size());

        MCRandomSpec mcSpec = random.get(UUID.fromString(random.toList().get(0)));
        Assert.assertSame(randomSpec, mcSpec.getSpec());
        Assert.assertEquals(specName, mcSpec.getName());
    }

    @Test
    public void getMCRandomSpec() {
        final String specName = "name";
        final RNGSpec rSpec = new RNGSpec();
        for (final Boolean e : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            MCRandomSpec tmp = new Random().get(new RandomType() {
                {
                    setName(specName);
                    setEnable(e);
                    setSpec(rSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(e, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(rSpec, tmp.getSpec());
        }
    }

    @Test
    public void getRandomType() {
        final String specName = "name";
        final RNGSpec rSpec = new RNGSpec();
        for (final Boolean e : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            RandomType tmp = new Random().get(new MCRandomSpec() {
                {
                    setName(specName);
                    setEnable(e);
                    setSpec(rSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(e, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(rSpec, tmp.getSpec());
        }
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(Random.getInstance());
    }

    @Test
    public void getList(@Mocked final RandomsType type) {
        final ArrayList<RandomType> expected = new ArrayList<>();
        new NonStrictExpectations() {
            {
                type.getRandom();
                result = expected;
            }
        };
        Assert.assertEquals(expected, new Random().getList());
    }

    @Test
    public void setEnable(@Mocked final Randoms randoms) throws ALEException {
        final String specName = "name";
        final RNGSpec rSpec = new RNGSpec();
        Random random = new Random();
        RandomType tmp = random.get(new MCRandomSpec() {
            {
                setName(specName);
                setSpec(rSpec);
            }
        });
        new NonStrictExpectations() {
            {
                Randoms.getInstance();
                result = randoms;
            }
        };

        random.setEnable(tmp, true);

        new Verifications() {
            {
                randoms.define(specName, rSpec, false);
            }
        };

        random.setEnable(tmp, false);

        new Verifications() {
            {
                randoms.undefine(specName, false);
            }
        };
    }

    @Test
    public void setEnableNoSuchNameException(@Mocked final Randoms randoms) throws ALEException {
        final String specName = "name";
        final RNGSpec randomSpec = new RNGSpec();
        Random random = new Random();
        RandomType tmp = random.get(new MCRandomSpec(specName, false, randomSpec));
        new NonStrictExpectations() {
            {
                Randoms.getInstance();
                result = randoms;

                randoms.undefine(withEqual(specName), false);
                result = new NoSuchNameException();
            }
        };

        random.setEnable(tmp, false);
    }
}
