package havis.middleware.ale.core.depot.service.lr;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.LogicalReaderType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.mc.MCLogicalReaderSpec;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogicalReaderTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void addLogicalReaderType() {
        final String readerName = "name1";
        final LRSpec readerSpec = new LRSpec() {
            {
                setSchemaVersion(BigDecimal.valueOf(1));
                setCreationDate(new Date());
            }
        };
        new LogicalReader().add(new LogicalReaderType() {
            {
                setName(readerName);
                setEnable(Boolean.FALSE);
                setSpec(readerSpec);
            }
        });
    }

    @Test
    public void addStringEcSpec() {
        final String readerName = "name2";
        final LRSpec readerSpec = new LRSpec() {
            {
                setSchemaVersion(BigDecimal.valueOf(1));
                setCreationDate(new Date());
            }
        };
        new LogicalReader().add(readerName, readerSpec);
    }

    @Test
    public void getMcLogicalReaderSpec() {
        final String readerName = "name3";
        final LRSpec readerSpec = new LRSpec();
        for (final Boolean isEnable : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            MCLogicalReaderSpec tmp = new LogicalReader().get(new LogicalReaderType() {
                {
                    setName(readerName);
                    setEnable(isEnable);
                    setSpec(readerSpec);
                }
            });
            Assert.assertEquals(tmp.getName(), readerName);
            Assert.assertEquals(Boolean.valueOf(tmp.isEnable()), isEnable);
            Assert.assertEquals(tmp.getSpec(), readerSpec);
        }
    }

    @Test
    public void getLogicalReaderType() {
        final String readerName = "name4";
        final LRSpec readerSpec = new LRSpec();
        for (final Boolean isEnable : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            LogicalReaderType tmp = new LogicalReader().get(new MCLogicalReaderSpec() {
                {
                    setName(readerName);
                    setEnable(isEnable);
                    setSpec(readerSpec);
                }
            });
            Assert.assertEquals(tmp.getName(), readerName);
            Assert.assertEquals(Boolean.valueOf(tmp.isEnable()), isEnable);
            Assert.assertEquals(tmp.getSpec(), readerSpec);
        }
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(LogicalReader.getInstance());
    }

    @Test
    public void getList() {
        Assert.assertNotNull(new LogicalReader().getList());
    }

    @Test
    public void setEnable(@Mocked final LR lr) {
        final String readerName = "name5";
        final LRSpec readerSpec = new LRSpec();
        LogicalReader reader = new LogicalReader();
        LogicalReaderType tmp = reader.get(new MCLogicalReaderSpec() {
            {
                setName(readerName);
                setSpec(readerSpec);
            }
        });
        try {
            new NonStrictExpectations() {
                {
                    LR.getInstance();
                    result = lr;
                }
            };

            reader.setEnable(tmp, true);

            new Verifications() {
                {
                    lr.define(readerName, readerSpec, false);
                }
            };

            reader.setEnable(tmp, false);

            new Verifications() {
                {
                    lr.undefine(readerName, false);
                }
            };
        } catch (ALEException e) {
            Assert.fail();
        }
    }

    @Test
    public void define(@Mocked final LR lr) {
        try {
            final String readerName = "name6";
            final LRSpec readerSpec = new LRSpec();
            new NonStrictExpectations() {
                {
                    LR.getInstance();
                    result = lr;
                }
            };
            for (final Boolean isEnable : new Boolean[] { Boolean.TRUE }) {
                new LogicalReader().define(new LogicalReaderType() {
                    {
                        setName(readerName);
                        setEnable(isEnable);
                        setSpec(readerSpec);
                    }
                });
                new Verifications() {
                    {
                        lr.define(readerName, readerSpec, false);
                    }
                };
            }
        } catch (ALEException e) {
            Assert.fail();
        }
    }

    @Test
    public void undefine(@Mocked final LR lr) {
        try {
            final String readerName = "name7";
            final LRSpec readerSpec = new LRSpec();
            new NonStrictExpectations() {
                {
                    LR.getInstance();
                    result = lr;
                }
            };
            for (final Boolean isEnable : new Boolean[] { Boolean.TRUE }) {
                new LogicalReader().undefine(new LogicalReaderType() {
                    {
                        setName(readerName);
                        setEnable(isEnable);
                        setSpec(readerSpec);
                    }
                });
                new Verifications() {
                    {
                        lr.undefine(readerName, false);
                    }
                };
            }
        } catch (ALEException e) {
            Assert.fail();
        }
    }

    @Test
    public void undefineNoSuchNameException(@Mocked final LR lr) {
        try {
            final String readerName = "name8";
            final LRSpec readerSpec = new LRSpec();

            final LogicalReader logicalReader = new LogicalReader();
            logicalReader.undefine(new LogicalReaderType() {
                {
                    setName(readerName);
                    setEnable(Boolean.TRUE);
                    setSpec(readerSpec);
                }
            });
            new Verifications() {
                {
                    lr.undefine(readerName, false);
                    times = 1;
                }
            };
        } catch (ALEException e) {
            Assert.fail();
        }
    }

    @Test
    public void updateStringLrSpec() {
        try {
            final String readerName = "updateName1";
            final LRSpec readerSpec = new LRSpec() {
                {
                    setSchemaVersion(BigDecimal.valueOf(1));
                }
            };
            LogicalReader reader = new LogicalReader();
            reader.add(readerName, readerSpec);
            final LRSpec readerSpec2 = new LRSpec() {
                {
                    setSchemaVersion(BigDecimal.valueOf(2));
                }
            };
            reader.update(readerName, readerSpec2);
            List<LogicalReaderType> list = reader.getList();
            for (LogicalReaderType logicalReaderType : list) {
                if (readerName.equals(logicalReaderType.getName())) {
                    Assert.assertEquals(readerSpec2.getSchemaVersion(), logicalReaderType.getSpec().getSchemaVersion());
                    return;
                }
            }
            Assert.fail();
        } catch (ALEException e) {
            Assert.fail();
        }
    }

    @Test
    public void updateMcLogicalReaderSpecLogicalReaderType(@Mocked final LR lr) {
        try {
            final String readerName = "updateMcName1";
            final LRSpec readerSpec = new LRSpec() {
                {
                    setSchemaVersion(BigDecimal.valueOf(1));
                    setCreationDate(new Date(1453460746));
                }
            };
            LogicalReader reader = new LogicalReader();
            reader.add(readerName, readerSpec);

            final MCLogicalReaderSpec readerSpec2 = new MCLogicalReaderSpec() {
                {
                    setSpec(new LRSpec() {
                        {
                            setSchemaVersion(BigDecimal.valueOf(2));
                        }
                    });
                }
            };

            new NonStrictExpectations() {
                {
                    LR.getInstance();
                    result = lr;
                }
            };

            boolean found = false;

            List<LogicalReaderType> list = reader.getList();
            for (LogicalReaderType logicalReaderType : list) {
                if (readerName.equals(logicalReaderType.getName())) {
                    Assert.assertTrue(reader.update(readerSpec2, logicalReaderType));
                    found = true;
                    break;
                }
            }

            Assert.assertTrue(found);

            new Verifications() {
                {
                    lr.update(readerName, readerSpec2.getSpec(), false);
                }
            };

            list = reader.getList();
            for (LogicalReaderType logicalReaderType : list) {
                if (readerName.equals(logicalReaderType.getName())) {
                    Assert.assertEquals(readerSpec2.getSpec().getSchemaVersion(), logicalReaderType.getSpec().getSchemaVersion());
                    return;
                }
            }

            Assert.fail();

        } catch (ALEException e) {
            Assert.fail();
        }
    }

    @Test
    public void updateMcLogicalReaderSpecLogicalReaderTypeNoSpec(@Mocked final LR lr) {
        try {
            final String readerName = "updateMcName2";
            final MCLogicalReaderSpec readerSpec = new MCLogicalReaderSpec() {
                {
                    setSpec(null);
                }
            };

            LogicalReader readerDepot = new LogicalReader();
            Assert.assertFalse(readerDepot.update(readerSpec, null));

            new Verifications() {
                {
                    lr.update(readerName, this.<LRSpec> withNotNull(), false);
                    times = 0;
                }
            };
        } catch (ALEException e) {
            Assert.fail();
        }
    }

    @Test
    public void updateUuidMcSpec(@Mocked final LR lr) throws Exception {
        final String readerName = "updateName3";
        final LRSpec readerSpec = new LRSpec() {
            {
                setSchemaVersion(BigDecimal.valueOf(1));
            }
        };
        LogicalReader reader = new LogicalReader();
        reader.add(readerName, readerSpec);
        final LRSpec readerSpec2 = new LRSpec() {
            {
                setSchemaVersion(BigDecimal.valueOf(2));
            }
        };
        final MCLogicalReaderSpec spec = new MCLogicalReaderSpec() {
            {
                setEnable(Boolean.TRUE);
                setName(readerName);
                setSpec(readerSpec2);
            }
        };
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;
            }
        };

        String guid = null;
        List<String> ids = reader.toList();
        for (String id : ids) {
            MCLogicalReaderSpec s = reader.get(UUID.fromString(id));
            if (readerName.equals(s.getName())) {
                guid = id;
                break;
            }
        }

        Assert.assertNotNull("Expected " + readerName + " in the depot", guid);

        reader.update(UUID.fromString(guid), spec);

        MCLogicalReaderSpec updated = reader.get(UUID.fromString(guid));
        Assert.assertNotNull(updated);
        Assert.assertEquals(BigDecimal.valueOf(2), updated.getSpec().getSchemaVersion());

        new Verifications() {
            {
                LRSpec spec;
                lr.update(readerName, spec = withCapture(), false);
                times = 1;

                Assert.assertEquals(BigDecimal.valueOf(2), spec.getSchemaVersion());
            }
        };
    }

    @Test
    public void updateUuidMcSpecNoSpec(@Mocked final LR lr) throws Exception {
        final String readerName = "updateName4";
        final LRSpec readerSpec = new LRSpec() {
            {
                setSchemaVersion(BigDecimal.valueOf(1));
            }
        };
        LogicalReader reader = new LogicalReader();
        reader.add(readerName, readerSpec);
        final MCLogicalReaderSpec spec = new MCLogicalReaderSpec() {
            {
                setEnable(Boolean.TRUE);
                setName(readerName);
                setSpec(null);
            }
        };
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;
            }
        };

        String guid = null;
        List<String> ids = reader.toList();
        for (String id : ids) {
            MCLogicalReaderSpec s = reader.get(UUID.fromString(id));
            if (readerName.equals(s.getName())) {
                guid = id;
                break;
            }
        }

        Assert.assertNotNull("Expected " + readerName + " in the depot", guid);

        try {
            reader.update(UUID.fromString(guid), spec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // nothing to do
        }

        new Verifications() {
            {
                lr.update(readerName, this.<LRSpec> withNotNull(), false);
                times = 0;
            }
        };
    }

    @Test
    public void updateUuidMcSpecDisabled(@Mocked final LR lr) throws Exception {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;
            }
        };

        // uses the super.update() method defined in Depot
        final String readerName = "updateName5";
        LogicalReader depot = new LogicalReader();
        final MCLogicalReaderSpec mcSpec1 = new MCLogicalReaderSpec() {
            {
                setEnable(Boolean.FALSE); // disabled
                setName(readerName);
                setSpec(new LRSpec() {
                    {
                        setSchemaVersion(BigDecimal.valueOf(1));
                    }
                });
            }
        };
        final MCLogicalReaderSpec mcSpec2 = new MCLogicalReaderSpec() {
            {
                setEnable(Boolean.FALSE); // stays disabled
                setName(readerName);
                setSpec(new LRSpec() {
                    {
                        setSchemaVersion(BigDecimal.valueOf(2));
                    }
                });
            }
        };
        final MCLogicalReaderSpec mcSpec3 = new MCLogicalReaderSpec() {
            {
                setEnable(Boolean.FALSE); // stays disabled
                setName(readerName);
                setSpec(new LRSpec() {
                    {
                        setSchemaVersion(BigDecimal.valueOf(3));
                    }
                });
            }
        };

        depot.add(mcSpec1); // not enabled

        String guid = null;
        List<String> ids = depot.toList();
        for (String id : ids) {
            MCLogicalReaderSpec s = depot.get(UUID.fromString(id));
            if (readerName.equals(s.getName())) {
                guid = id;
                break;
            }
        }

        Assert.assertNotNull("Expected " + readerName + " in the depot", guid);

        depot.update(UUID.fromString(guid), mcSpec2);

        MCLogicalReaderSpec updated = depot.get(UUID.fromString(guid));
        Assert.assertNotNull(updated);
        Assert.assertEquals(BigDecimal.valueOf(2), updated.getSpec().getSchemaVersion());

        depot.update(UUID.fromString(guid), mcSpec3);

        updated = depot.get(UUID.fromString(guid));
        Assert.assertNotNull(updated);
        Assert.assertEquals(BigDecimal.valueOf(3), updated.getSpec().getSchemaVersion());

        new Verifications() {
            {
                lr.update(readerName, this.<LRSpec>withNotNull(), false);
                times = 0;
            }
        };
    }
}
