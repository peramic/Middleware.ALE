package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.config.CacheType;
import havis.middleware.ale.config.CachesType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.report.cc.data.Caches;
import havis.middleware.ale.service.cc.EPCCacheSpec;
import havis.middleware.ale.service.cc.EPCPatternList;
import havis.middleware.ale.service.mc.MCCacheSpec;
import havis.middleware.ale.service.mc.MCSpec;

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

public class CacheTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void add() throws ALEException {
        try {
            new Cache().add(new MCSpec());
            Assert.fail("Expected ALEException");
        } catch (ALEException e) {
            // ignore
        }

        final String specName = "name";
        final EPCCacheSpec cacheSpec = new EPCCacheSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };
        Cache cache = new Cache();
        String uuid = cache.add(new MCCacheSpec() {
            {
                setName(specName);
                setEnable(Boolean.FALSE);
                setSpec(cacheSpec);
                setPatterns(new EPCPatternList(new ArrayList<String>()));
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        });
        Assert.assertTrue(cache.toList().contains(uuid));

        MCCacheSpec mcSpec = cache.get(UUID.fromString(uuid));
        Assert.assertSame(cacheSpec, mcSpec.getSpec());
        Assert.assertNotNull(mcSpec.getPatterns());
        Assert.assertEquals(0, mcSpec.getPatterns().getPatterns().getPattern().size());
        Assert.assertEquals(specName, mcSpec.getName());
    }

    @Test
    public void addNameSpecEntries(@Mocked final CachesType type) throws ALEException {

        new NonStrictExpectations() {
            {
                type.getCache();
                result = new ArrayList<CacheType>();
            }
        };

        final String specName = "name";
        final EPCCacheSpec cacheSpec = new EPCCacheSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };

        Cache cache = new Cache();
        List<String> entries = new ArrayList<>();
        entries.add("entry");

        Assert.assertEquals(0, cache.toList().size());
        cache.add(specName, cacheSpec, entries);
        Assert.assertEquals(1, cache.toList().size());

        MCCacheSpec mcSpec = cache.get(UUID.fromString(cache.toList().get(0)));
        Assert.assertSame(cacheSpec, mcSpec.getSpec());
        Assert.assertNotNull(mcSpec.getPatterns());
        Assert.assertEquals(1, mcSpec.getPatterns().getPatterns().getPattern().size());
        Assert.assertSame("entry", mcSpec.getPatterns().getPatterns().getPattern().get(0));
        Assert.assertEquals(specName, mcSpec.getName());
    }

    @Test
    public void getMCCacheSpec() {
        final String specName = "name";
        final EPCCacheSpec cacheSpec = new EPCCacheSpec();
        for (final Boolean e : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            MCCacheSpec tmp = new Cache().get(new CacheType() {
                {
                    setName(specName);
                    setEnable(e);
                    setSpec(cacheSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(e, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(cacheSpec, tmp.getSpec());
        }
    }

    @Test
    public void getCacheType() {
        final String specName = "name";
        final EPCCacheSpec cacheSpec = new EPCCacheSpec();
        for (final Boolean e : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            CacheType tmp = new Cache().get(new MCCacheSpec() {
                {
                    setName(specName);
                    setEnable(e);
                    setSpec(cacheSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(e, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(cacheSpec, tmp.getSpec());
        }
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(Cache.getInstance());
    }

    @Test
    public void getList(@Mocked final CachesType type) {
        final ArrayList<CacheType> expected = new ArrayList<>();
        new NonStrictExpectations() {
            {
                type.getCache();
                result = expected;
            }
        };
        Assert.assertSame(expected, new Cache().getList());
    }

    @Test
    public void setEnable(@Mocked final Caches caches) throws ALEException {
        final String specName = "name";
        final EPCCacheSpec cacheSpec = new EPCCacheSpec();
        final List<String> patterns = new ArrayList<>();
        Cache cache = new Cache();
        CacheType tmp = cache.get(new MCCacheSpec(specName, false, cacheSpec, (new EPCPatternList(patterns))));

        new NonStrictExpectations() {
            {
                Caches.getInstance();
                result = caches;
            }
        };

        cache.setEnable(tmp, true);

        new Verifications() {
            {
                caches.define(specName, cacheSpec, patterns, false);
            }
        };

        cache.setEnable(tmp, false);

        new Verifications() {
            {
                caches.undefine(specName, false);
            }
        };
    }

    @Test
    public void setEnableNoSuchNameException(@Mocked final Caches caches) throws ALEException {
        final String specName = "name";
        final EPCCacheSpec cacheSpec = new EPCCacheSpec();
        final List<String> list = new ArrayList<>();
        Cache cache = new Cache();
        CacheType tmp = cache.get(new MCCacheSpec(specName, false, cacheSpec, new EPCPatternList(list)));
        new NonStrictExpectations() {
            {
                Caches.getInstance();
                result = caches;

                caches.undefine(withEqual(specName), false);
                result = new NoSuchNameException();
            }
        };

        cache.setEnable(tmp, false);
    }

    @Test
    public void update(@Mocked final Caches caches, @Mocked Config config, @Mocked final CachesType type) throws ALEException {

        new NonStrictExpectations() {
            {
                type.getCache();
                result = new ArrayList<>();
            }
        };

        final String specName = "name";
        final List<String> list = new ArrayList<>();

        final MCCacheSpec spec = new MCCacheSpec() {
            {
                setName(specName);
                setEnable(Boolean.TRUE);
                setSpec(new EPCCacheSpec() {
                    {
                        setSchemaVersion(new BigDecimal(1));
                        setCreationDate(new Date());
                    }
                });
                setPatterns(new EPCPatternList(list));
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };

        Cache cache = new Cache();
        cache.add(spec);

        new Verifications() {
            {
                caches.define(specName, spec.getSpec(), list, false);
                times = 1;

                Config.serialize();
                times = 1;
            }
        };

        cache.update(specName, list);

        new Verifications() {
            {
                Config.serialize();
                times = 2;
            }
        };
    }

    @Test
    public void updateNoSuchIdException(@Mocked final Caches caches, @Mocked Config config, @Mocked final CachesType type) throws ALEException {
        new NonStrictExpectations() {
            {
                type.getCache();
                result = new ArrayList<>();
            }
        };

        final String specName = "name";
        final List<String> list = new ArrayList<>();

        Cache cache = new Cache() {
            {
                names.put(specName, UUID.randomUUID());
            }
        };

        cache.update(specName, list);
    }
}
