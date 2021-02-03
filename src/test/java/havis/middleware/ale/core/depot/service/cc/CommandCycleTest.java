package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.config.CommandCycleType;
import havis.middleware.ale.config.CommandCyclesType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.CC;
import havis.middleware.ale.service.cc.CCBoundarySpec;
import havis.middleware.ale.service.cc.CCCmdSpec;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.ale.service.mc.MCCommandCycleSpec;

import java.math.BigDecimal;
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

public class CommandCycleTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void addCommandCycleType() throws NoSuchIdException {
        final String specName = "name";
        final CCSpec ccSpec = new CCSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
                setLogicalReaders(new LogicalReaders() {
                    {
                        logicalReader = Arrays.asList(new String[] { "reader" });
                    }
                });
                setBoundarySpec(new CCBoundarySpec());
                setCmdSpecs(new CmdSpecs() {
                    {
                        cmdSpec = Arrays.asList(new CCCmdSpec[] { new CCCmdSpec() {
                            {
                                setName(specName);
                            }
                        } });
                    }
                });
            }
        };
        CommandCycle commandCycle = new CommandCycle();
        UUID uuid = commandCycle.add(new CommandCycleType() {
            {
                setName(specName);
                setEnable(Boolean.FALSE);
                setSpec(ccSpec);
            }
        });

        MCCommandCycleSpec mcCommandCycleSpec = commandCycle.get(uuid);
        Assert.assertEquals(specName, mcCommandCycleSpec.getName());
        Assert.assertFalse(mcCommandCycleSpec.isEnable());
        Assert.assertSame(ccSpec, mcCommandCycleSpec.getSpec());
    }

    @Test
    public void addStringECSpec(@Mocked final CommandCyclesType type) throws NoSuchIdException {
        new NonStrictExpectations() {
            {
                type.getCommandCycle();
                result = new ArrayList<CommandCycleType>();
            }
        };

        final String specName = "name";
        final CCSpec ccSpec = new CCSpec() {
            {
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
                setLogicalReaders(new LogicalReaders() {
                    {
                        logicalReader = Arrays.asList(new String[] { "reader" });
                    }
                });
                setBoundarySpec(new CCBoundarySpec());
                setCmdSpecs(new CmdSpecs() {
                    {
                        cmdSpec = Arrays.asList(new CCCmdSpec[] { new CCCmdSpec() {
                            {
                                setName(specName);
                            }
                        } });
                    }
                });
            }
        };
        CommandCycle commandCycle = new CommandCycle();
        commandCycle.add(specName, ccSpec);

        List<String> list = commandCycle.toList();
        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());

        MCCommandCycleSpec mcCommandCycleSpec = commandCycle.get(UUID.fromString(list.get(0)));
        Assert.assertEquals(specName, mcCommandCycleSpec.getName());
        Assert.assertTrue(mcCommandCycleSpec.isEnable());
        Assert.assertSame(ccSpec, mcCommandCycleSpec.getSpec());
    }

    @Test
    public void getMCCommandCycleSpec() {
        final String specName = "name";
        final CCSpec ccSpec = new CCSpec();
        for (final Boolean e : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            MCCommandCycleSpec tmp = new CommandCycle().get(new CommandCycleType() {
                {
                    setName(specName);
                    setEnable(e);
                    setSpec(ccSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(e, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(ccSpec, tmp.getSpec());
        }
    }

    @Test
    public void getCommandCycleType() {
        final String specName = "name";
        final CCSpec ccSpec = new CCSpec();
        for (final Boolean e : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            CommandCycleType tmp = new CommandCycle().get(new MCCommandCycleSpec() {
                {
                    setName(specName);
                    setEnable(e);
                    setSpec(ccSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(e, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(ccSpec, tmp.getSpec());
        }
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(CommandCycle.getInstance());
    }

    @Test
    public void getList(@Mocked final CommandCyclesType type) {
        final ArrayList<CommandCycleType> expected = new ArrayList<>();
        new NonStrictExpectations() {
            {
                type.getCommandCycle();
                result = expected;
            }
        };
        Assert.assertEquals(expected, new CommandCycle().getList());
    }

    @Test
    public void define(@Mocked final CC cc) throws ALEException {
        final String specName = "name";
        final CCSpec ccSpec = new CCSpec();
        new NonStrictExpectations() {
            {
                CC.getInstance();
                result = cc;
            }
        };
        new CommandCycle().define(new CommandCycleType() {
            {
                setName(specName);
                setEnable(Boolean.TRUE);
                setSpec(ccSpec);
            }
        });
        new Verifications() {
            {
                cc.define(specName, ccSpec, false);
            }
        };
    }

    @Test
    public void undefine(@Mocked final CC cc) throws ALEException {
        final String specName = "name";
        final CCSpec ccSpec = new CCSpec();
        new NonStrictExpectations() {
            {
                CC.getInstance();
                result = cc;
            }
        };
        new CommandCycle().undefine(new CommandCycleType() {
            {
                setName(specName);
                setEnable(Boolean.TRUE);
                setSpec(ccSpec);
            }
        });
        new Verifications() {
            {
                cc.undefine(specName, false);
            }
        };
    }
}
