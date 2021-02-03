package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.config.AssociationType;
import havis.middleware.ale.config.AssociationsType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.report.cc.data.Associations;
import havis.middleware.ale.service.cc.AssocTableEntry;
import havis.middleware.ale.service.cc.AssocTableEntryList;
import havis.middleware.ale.service.cc.AssocTableEntryList.Entries;
import havis.middleware.ale.service.cc.AssocTableSpec;
import havis.middleware.ale.service.mc.MCAssociationSpec;
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

public class AssociationTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(Association.getInstance());
    }

    @Test
    public void add() throws ALEException {
        try {
            new Association().add(new MCSpec());
            Assert.fail("Expected ALEException");
        } catch (ALEException e) {
            // ignore
        }

        final String specName = "name";
        final AssocTableSpec assocTableSpec = new AssocTableSpec() {
            {
                setDatatype("datatype");
                setFormat("format");
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };

        Association association = new Association();
        String uuid = association.add(new MCAssociationSpec() {
            {
                setName(specName);
                setEnable(Boolean.FALSE);
                setSpec(assocTableSpec);
                setEntries(new AssocTableEntryList(new ArrayList<AssocTableEntry>()));
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        });

        Assert.assertTrue(association.toList().contains(uuid));
        MCAssociationSpec mcSpec = association.get(UUID.fromString(uuid));
        Assert.assertSame(assocTableSpec, mcSpec.getSpec());
        Assert.assertNotNull(mcSpec.getEntries());
        Assert.assertEquals(0, mcSpec.getEntries().getEntries().getEntry().size());
        Assert.assertEquals(specName, mcSpec.getName());
    }

    @Test
    public void addNameSpecEntries(@Mocked final AssociationsType type) throws ALEException {

        new NonStrictExpectations() {
            {
                type.getAssociation();
                result = new ArrayList<AssociationType>();
            }
        };

        final String specName = "name";
        final AssocTableSpec assocTableSpec = new AssocTableSpec() {
            {
                setDatatype("datatype");
                setFormat("format");
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };

        Association association = new Association();
        List<AssocTableEntry> entries = new ArrayList<AssocTableEntry>();
        AssocTableEntry entry = new AssocTableEntry();
        entry.setKey("key");
        entry.setValue("value");
        entries.add(entry);

        Assert.assertEquals(0, association.toList().size());
        association.add(specName, assocTableSpec, entries);
        Assert.assertEquals(1, association.toList().size());

        MCAssociationSpec mcSpec = association.get(UUID.fromString(association.toList().get(0)));
        Assert.assertSame(assocTableSpec, mcSpec.getSpec());
        Assert.assertNotNull(mcSpec.getEntries());
        Assert.assertEquals(1, mcSpec.getEntries().getEntries().getEntry().size());
        Assert.assertSame(entry, mcSpec.getEntries().getEntries().getEntry().get(0));
        Assert.assertEquals(specName, mcSpec.getName());
    }

    @Test
    public void getMCAssociationSpec() {
        final String specName = "name";
        final AssocTableSpec assocTableSpec = new AssocTableSpec();
        for (final Boolean isEnabled : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            MCAssociationSpec tmp = new Association().get(new AssociationType() {
                {
                    setName(specName);
                    setEnable(isEnabled);
                    setSpec(assocTableSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(isEnabled, Boolean.valueOf(tmp.isEnable()));
            Assert.assertSame(assocTableSpec, tmp.getSpec());
        }
    }

    @Test
    public void getAssociationType() {
        final String specName = "name";
        final AssocTableSpec assocTableSpec = new AssocTableSpec();
        for (final Boolean isEnabled : new Boolean[] { Boolean.TRUE, Boolean.FALSE }) {
            AssociationType tmp = new Association().get(new MCAssociationSpec() {
                {
                    setName(specName);
                    setEnable(isEnabled);
                    setSpec(assocTableSpec);
                }
            });
            Assert.assertEquals(specName, tmp.getName());
            Assert.assertEquals(isEnabled, Boolean.valueOf(tmp.isEnable()));
            Assert.assertEquals(assocTableSpec, tmp.getSpec());
        }
    }

    @Test
    public void getList(@Mocked final AssociationsType type) {
        final ArrayList<AssociationsType> expected = new ArrayList<>();
        new NonStrictExpectations() {
            {
                type.getAssociation();
                result = expected;
            }
        };
        Association association = new Association();
        Assert.assertSame(expected, association.getList());
    }

    @Test
    public void setEnable(@Mocked final Associations associations) throws ALEException {
        final String specName = "name";
        final AssocTableSpec assocTableSpec = new AssocTableSpec();
        final List<AssocTableEntry> list = new ArrayList<>();
        Association association = new Association();
        AssociationType tmp = association.get(new MCAssociationSpec(specName, false, assocTableSpec, new AssocTableEntryList(list)));
        new NonStrictExpectations() {
            {
                Associations.getInstance();
                result = associations;
            }
        };

        association.setEnable(tmp, true);

        new Verifications() {
            {
                associations.define(withEqual(specName), withEqual(assocTableSpec), withEqual(list), false);
                times = 1;
            }
        };

        association.setEnable(tmp, false);

        new Verifications() {
            {
                associations.undefine(withEqual(specName), false);
                times = 1;
            }
        };
    }

    @Test
    public void setEnableNoSuchNameException(@Mocked final Associations associations) throws ALEException {
        final String specName = "name";
        final AssocTableSpec assocTableSpec = new AssocTableSpec();
        final List<AssocTableEntry> list = new ArrayList<>();
        Association association = new Association();
        AssociationType tmp = association.get(new MCAssociationSpec(specName, false, assocTableSpec, new AssocTableEntryList(list)));
        new NonStrictExpectations() {
            {
                Associations.getInstance();
                result = associations;

                associations.undefine(withEqual(specName), false);
                result = new NoSuchNameException();
            }
        };

        association.setEnable(tmp, false);
    }

    @Test
    public void update(@Mocked final Associations associations, @Mocked Config config, @Mocked final AssociationsType type) throws ALEException {

        new NonStrictExpectations() {
            {
                type.getAssociation();
                result = new ArrayList<>();
            }
        };

        final String specName = "name";
        final List<AssocTableEntry> list = new ArrayList<>();

        final MCAssociationSpec spec = new MCAssociationSpec() {
            {
                setName(specName);
                setEnable(Boolean.TRUE);
                setSpec(new AssocTableSpec() {
                    {
                        setDatatype("datatype");
                        setFormat("format");
                        setSchemaVersion(new BigDecimal(1));
                        setCreationDate(new Date());
                    }
                });
                setEntries(new AssocTableEntryList());
                getEntries().setEntries(new Entries());
                setSchemaVersion(new BigDecimal(1));
                setCreationDate(new Date());
            }
        };

        Association association = new Association();
        association.add(spec);

        new Verifications() {
            {
                associations.define(specName, spec.getSpec(), list, false);
                times = 1;

                Config.serialize();
                times = 1;
            }
        };

        association.update(specName, list);

        new Verifications() {
            {
                Config.serialize();
                times = 2;
            }
        };
    }

    @Test
    public void updateNoSuchIdException(@Mocked final Associations associations, @Mocked Config config, @Mocked final AssociationsType type) throws ALEException {
        new NonStrictExpectations() {
            {
                type.getAssociation();
                result = new ArrayList<>();
            }
        };

        final String specName = "name";
        final List<AssocTableEntry> list = new ArrayList<>();

        Association association = new Association() {
            {
                names.put(specName, UUID.randomUUID());
            }
        };

        association.update(specName, list);
    }
}
