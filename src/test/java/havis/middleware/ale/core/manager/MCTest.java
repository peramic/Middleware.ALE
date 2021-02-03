package havis.middleware.ale.core.manager;


import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchPathException;
import havis.middleware.ale.base.exception.NoSuchPropertyException;
import havis.middleware.ale.config.ConfigType;
import havis.middleware.ale.config.service.mc.Path;
import havis.middleware.ale.config.service.mc.Property;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.Depot;
import havis.middleware.ale.core.depot.service.cc.Association;
import havis.middleware.ale.core.depot.service.cc.Cache;
import havis.middleware.ale.core.depot.service.cc.CommandCycle;
import havis.middleware.ale.core.depot.service.cc.Random;
import havis.middleware.ale.core.depot.service.ec.EventCycle;
import havis.middleware.ale.core.depot.service.lr.LogicalReader;
import havis.middleware.ale.core.depot.service.pc.PortCycle;
import havis.middleware.ale.core.depot.service.tm.TagMemory;
import havis.middleware.ale.core.reader.Reader;
import havis.middleware.ale.core.report.IReports;
import havis.middleware.ale.service.cc.CCParameterListEntry;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.ale.service.mc.MCAssociationSpec;
import havis.middleware.ale.service.mc.MCCacheSpec;
import havis.middleware.ale.service.mc.MCCommandCycleSpec;
import havis.middleware.ale.service.mc.MCConnectorSpec;
import havis.middleware.ale.service.mc.MCEventCycleSpec;
import havis.middleware.ale.service.mc.MCLogicalReaderSpec;
import havis.middleware.ale.service.mc.MCPCOpSpecs;
import havis.middleware.ale.service.mc.MCPortCycleSpec;
import havis.middleware.ale.service.mc.MCProperty;
import havis.middleware.ale.service.mc.MCRandomSpec;
import havis.middleware.ale.service.mc.MCSpec;
import havis.middleware.ale.service.mc.MCSubscriberSpec;
import havis.middleware.ale.service.mc.MCTagMemorySpec;
import havis.middleware.ale.service.mc.MCVersionSpec;
import havis.middleware.ale.service.pc.PCOpReport;
import havis.middleware.ale.service.pc.PCOpSpec;
import havis.middleware.ale.service.pc.PCOpSpecs;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.service.pc.PCSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;


public class MCTest {
	
	@Test
	public void getInstance(){
		MC mc = MC.getInstance();
		
		Assert.assertSame(mc, MC.getInstance());
	}
	
	@Test
	public void set(final @Mocked Document file){
		MC mc = MC.getInstance();
		final String name = "name";
		final Map<String,Document> expected = new HashMap<String, Document>();
		expected.put(name, file);
		
		mc.set(name, file);
		
		Map<String,Document> actual = Deencapsulation.getField(mc, "files");
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void get(final @Mocked Document file){
		MC.reset();
		MC mc = MC.getInstance();
		final String name = "name";
		
		mc.set(name,file);
		
		Assert.assertSame(file, mc.get(name));
	}
	
	@Test
	public void addWithLR(final @Mocked MCLogicalReaderSpec spec, final @Mocked LogicalReader logicalReader) throws Exception{
		validateAdd(spec, logicalReader, Path.Service.LR.LogicalReader);
	}
	
	@Test
	public void addWithEC(final @Mocked MCEventCycleSpec spec, final @Mocked EventCycle cycle) throws Exception{
		validateAdd(spec, cycle, Path.Service.EC.EventCycle);
	}
	
	@Test
	public void addWithPC(final @Mocked MCPortCycleSpec spec, final @Mocked PortCycle cycle) throws Exception{
		validateAdd(spec, cycle, Path.Service.PC.PortCycle);
	}
	
	@Test
	public void addWithCC(final @Mocked MCCommandCycleSpec spec, final @Mocked CommandCycle cycle) throws Exception{
		validateAdd(spec, cycle, Path.Service.CC.CommandCycle);
	}
	
	@Test
	public void addWithTM(final @Mocked MCTagMemorySpec spec, final @Mocked TagMemory tagMemory) throws Exception{
		validateAdd(spec, tagMemory, Path.Service.TM.TagMemory);
	}
	
	@Test
	public void addWithEcSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked EventCycle cycle) throws Exception{
		final String expected = UUID.randomUUID().toString();
		new NonStrictExpectations() {
			{
				cycle.add(this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.EC.Subscriber;
		String parent = UUID.randomUUID().toString();
		
		final String actual = mc.add(path, spec, parent);
		
		new Verifications() {
			{
				cycle.add(this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				times = 1;	
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void addWithCcSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked CommandCycle cycle) throws Exception{
		final String expected = UUID.randomUUID().toString();
		new NonStrictExpectations() {
			{
				cycle.add(this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Subscriber;
		String parent = UUID.randomUUID().toString();
		
		final String actual = mc.add(path, spec, parent);
		
		new Verifications() {
			{
				cycle.add(this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				times = 1;	
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void addWithCcCache(final @Mocked MCCacheSpec spec, final @Mocked Cache cache) throws Exception{
		final String expected = UUID.randomUUID().toString();
		new NonStrictExpectations() {
			{
				cache.add(this.<MCCacheSpec>withNotNull());
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Cache;
		
		final String actual = mc.add(path, spec);
		
		new Verifications() {
			{
				cache.add(this.<MCCacheSpec>withNotNull());
				times = 1;	
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void addWithCcAssociation(final @Mocked MCAssociationSpec spec, final @Mocked Association association) throws Exception{
		final String expected = UUID.randomUUID().toString();
		new NonStrictExpectations() {
			{
				association.add(this.<MCAssociationSpec>withNotNull());
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Association;
		
		mc.add(path, spec);
		
		new Verifications() {
			{
				association.add(this.<MCAssociationSpec>withNotNull());
				times = 1;	
			}
		};
	}
	
	@Test
	public void addWithCcRandom(final @Mocked MCRandomSpec spec, final @Mocked Random random) throws Exception{
		final String expected = UUID.randomUUID().toString();
		new NonStrictExpectations() {
			{
				random.add(this.<MCRandomSpec>withNotNull());
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Random;
		
		final String actual = mc.add(path, spec);
		
		new Verifications() {
			{
				random.add(this.<MCRandomSpec>withNotNull());
				times = 1;	
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void addWithPcSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked PortCycle cycle) throws Exception{
		final String expected = UUID.randomUUID().toString();
		new NonStrictExpectations() {
			{
				cycle.add(this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.PC.Subscriber;
		String parent = UUID.randomUUID().toString();
		
		final String actual = mc.add(path, spec, parent);
		
		new Verifications() {
			{
				cycle.add(this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				times = 1;	
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void addWithNoSuchPathException(final @Mocked MCEventCycleSpec spec, final @Mocked EventCycle cycle) throws Exception{
		String path = "someWrongPath";
		MC.reset();
		MC mc = MC.getInstance();
		
		try{
			mc.add(path, spec);
			Assert.fail("NoSuchPathException expected");
		}catch(NoSuchPathException e){
			Assert.assertEquals("Unknown path 'someWrongPath'", e.getMessage());
		}
	}
	
	@Test
	public void addWithALEException(final @Mocked MCEventCycleSpec spec, final @Mocked EventCycle cycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.EC.EventCycle;
		new NonStrictExpectations() {
			{
				cycle.add(this.<MCEventCycleSpec>withNotNull());
				result =  new ALEException();
			}
		};
		
		try{
			mc.add(path, spec);
			Assert.fail("ALEException expected");
		}catch(ALEException e){
		}
	}

	private void validateAdd(final MCSpec spec, final Depot<?, ?> depot, String path) throws Exception{
		final String expected = UUID.randomUUID().toString();
		new NonStrictExpectations() {
			{
				depot.add(this.<MCSpec>withNotNull());
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		
		final String actual = mc.add(path, spec);
		
		new Verifications() {
			{
				depot.add(this.<MCSpec>withNotNull());
				times = 1;	
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void remove(final @Mocked MCLogicalReaderSpec spec, final @Mocked LogicalReader reader) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String path = Path.Service.LR.LogicalReader;
		
		mc.remove(path,id);
		
		new Verifications() {
			{
				reader.remove(this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithTM(final @Mocked MCTagMemorySpec spec, final @Mocked TagMemory tagMemory) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String path = Path.Service.TM.TagMemory;
		
		mc.remove(path,id);
		
		new Verifications() {
			{
				tagMemory.remove(this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithEC(final @Mocked MCEventCycleSpec spec, final @Mocked EventCycle eventCycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String path = Path.Service.EC.EventCycle;
		
		mc.remove(path,id);
		
		new Verifications() {
			{
				eventCycle.remove(this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithECSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked EventCycle eventCycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String parent = UUID.randomUUID().toString();
		String path = Path.Service.EC.Subscriber;
		
		mc.remove(path,id,parent);
		
		new Verifications() {
			{
				eventCycle.remove(this.<UUID>withNotNull(),this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithCC(final @Mocked MCCommandCycleSpec spec, final @Mocked CommandCycle commandCycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String path = Path.Service.CC.CommandCycle;
		
		mc.remove(path,id);
		
		new Verifications() {
			{
				commandCycle.remove(this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithCCSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked CommandCycle commandCycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String parent = UUID.randomUUID().toString();
		String path = Path.Service.CC.Subscriber;
		
		mc.remove(path,id,parent);
		
		new Verifications() {
			{
				commandCycle.remove(this.<UUID>withNotNull(),this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithCCCache(final @Mocked MCCacheSpec spec, final @Mocked Cache cache, final @Mocked CommandCycle commandCycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String path = Path.Service.CC.Cache;
		
		mc.remove(path,id);
		
		new Verifications() {
			{
				cache.remove(this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithCCAssoc(final @Mocked MCAssociationSpec spec, final @Mocked Association association, final @Mocked CommandCycle commandCycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String path = Path.Service.CC.Association;
		
		mc.remove(path,id);
		
		new Verifications() {
			{
				association.remove(this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithCCRandom(final @Mocked MCRandomSpec spec, final @Mocked Random association, final @Mocked CommandCycle commandCycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String path = Path.Service.CC.Random;
		
		mc.remove(path,id);
		
		new Verifications() {
			{
				association.remove(this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithPC(final @Mocked MCPortCycleSpec spec, final @Mocked PortCycle portCycle) throws Exception{
		MC.reset();
		final MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String path = Path.Service.PC.PortCycle;
		
		mc.remove(path,id);
		
		new Verifications() {
			{
				portCycle.remove(this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithPCSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked PortCycle portCycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = UUID.randomUUID().toString();
		String parent = UUID.randomUUID().toString();
		String path = Path.Service.PC.Subscriber;
		
		mc.remove(path,id,parent);
		
		new Verifications() {
			{
				portCycle.remove(this.<UUID>withNotNull(),this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void removeWithUnknownPath() throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = "someUnknowPath";
		
		try{
			mc.remove(path,null);
			Assert.fail("Expected NoSuchPathException");
		}catch(NoSuchPathException e){
			Assert.assertEquals("Unknown path 'someUnknowPath'", e.getMessage());
		}
	}
	
	@Test
	public void removeWithALEException(final @Mocked EventCycle cycle) throws Exception{
		final String reason = "someReason";
		new NonStrictExpectations() {
			{	
				cycle.remove(this.<UUID>withNotNull());
				result = new ALEException(reason);
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.EC.EventCycle;
		String id = UUID.randomUUID().toString();
		
		try{
			mc.remove(path, id);
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals(reason, e.getMessage());
		}
	}
	
	@Test
	public void getWithLR(final @Mocked MCLogicalReaderSpec spec, final @Mocked LogicalReader cycle) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.LR.LogicalReader;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCLogicalReaderSpec actual = (MCLogicalReaderSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cycle.get(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithTM(final @Mocked MCTagMemorySpec spec, final @Mocked TagMemory cycle) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.TM.TagMemory;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCTagMemorySpec actual = (MCTagMemorySpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cycle.get(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithEc(final @Mocked MCEventCycleSpec spec, final @Mocked EventCycle cycle) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.EC.EventCycle;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCEventCycleSpec actual = (MCEventCycleSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cycle.get(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithCc(final @Mocked MCCommandCycleSpec spec, final @Mocked CommandCycle cycle) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.CommandCycle;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCCommandCycleSpec actual = (MCCommandCycleSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cycle.get(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithPc(final @Mocked MCPortCycleSpec spec, final @Mocked PortCycle cycle) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.PC.PortCycle;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCPortCycleSpec actual = (MCPortCycleSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cycle.get(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithCcCache(final @Mocked MCCacheSpec spec, final @Mocked Cache cache) throws Exception{
		new NonStrictExpectations() {
			{
				cache.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Cache;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCCacheSpec actual = (MCCacheSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cache.get(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithCcAssociation(final @Mocked MCAssociationSpec spec, final @Mocked Association association) throws Exception{
		new NonStrictExpectations() {
			{
				association.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Association;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCAssociationSpec actual = (MCAssociationSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				association.get(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	

	@Test
	public void getWithCcRandom(final @Mocked MCRandomSpec spec, final @Mocked Random random) throws Exception{
		new NonStrictExpectations() {
			{
				random.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Random;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCRandomSpec actual = (MCRandomSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				random.get(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}

	
	@Test
	public void getWithEcSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked EventCycle cycle) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull(), this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.EC.Subscriber;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCSubscriberSpec actual = (MCSubscriberSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cycle.get(this.<UUID>withNotNull(), this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithCcSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked CommandCycle cycle) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull(), this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Subscriber;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCSubscriberSpec actual = (MCSubscriberSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cycle.get(this.<UUID>withNotNull(), this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithPcSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked PortCycle cycle) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull(), this.<UUID>withNotNull());
				result = spec;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.PC.Subscriber;
		String id = UUID.randomUUID().toString();
		String parent = id;
		
		final MCSubscriberSpec actual = (MCSubscriberSpec) mc.get(path, id, parent);
		
		new Verifications() {
			{
				cycle.get(this.<UUID>withNotNull(), this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(spec, actual);
			}
		};
	}
	
	@Test
	public void getWithLrVersion(final @Mocked MCVersionSpec spec) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.LR.Version;
		
		mc.get(path, null, null);
		
		new Verifications() {
			{
				String standard;
				new MCVersionSpec(standard = this.withCapture(),null);
				times = 1;
				
				Assert.assertEquals("1.1", standard);
			}
		};
	}
	
	@Test
	public void getWithTmVersion(final @Mocked MCVersionSpec spec) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.TM.Version;
		
		mc.get(path, null, null);
		
		new Verifications() {
			{
				String standard;
				new MCVersionSpec(standard = this.withCapture(),null);
				times = 1;
				
				Assert.assertEquals("1.1", standard);
			}
		};
	}
	
	@Test
	public void getWithEcVersion(final @Mocked MCVersionSpec spec) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.EC.Version;
		
		mc.get(path, null, null);
		
		new Verifications() {
			{
				String standard;
				new MCVersionSpec(standard = this.withCapture(),null);
				times = 1;
				
				Assert.assertEquals("1.1", standard);
			}
		};
	}
	
	@Test
	public void getWithPcVersion(final @Mocked MCVersionSpec spec) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.PC.Version;
		
		mc.get(path, null, null);
		
		new Verifications() {
			{
				String standard;
				new MCVersionSpec(standard = this.withCapture(),null);
				times = 1;
				
				Assert.assertEquals("1.1", standard);
			}
		};
	}
	
	@Test
	public void getWithCcVersion(final @Mocked MCVersionSpec spec) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Version;
		
		mc.get(path, null, null);
		
		new Verifications() {
			{
				String standard;
				new MCVersionSpec(standard = this.withCapture(),null);
				times = 1;
				
				Assert.assertEquals("1.1", standard);
			}
		};
	}
	
	@Test
	public void getWithReaderConnector(final @Mocked MCConnectorSpec spec) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Connector.Reader;
		final String id = UUID.randomUUID().toString();
		
		mc.get(path, id, null);
		
		new Verifications() {
			{	
				new MCConnectorSpec(this.<String>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void getWithSubscriberConnector(final @Mocked MCConnectorSpec spec) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Connector.Subscriber;
		final String id = UUID.randomUUID().toString();
		
		mc.get(path, id, null);
		
		new Verifications() {
			{	
				new MCConnectorSpec(this.<String>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void getWithUnknownPath() throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = "somePath";

		 try {
			 mc.get(path, null, null);
	         Assert.fail("Expected NoSuchPathException");
	     } catch (NoSuchPathException e) {
	         Assert.assertEquals("Unknown path 'somePath'", e.getMessage());
	     }	
	}
	
	@Test
	public void getWithNoSuchIdException(final @Mocked MCLogicalReaderSpec spec, final @Mocked LogicalReader cycle, final @Mocked NoSuchIdException exception) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull());
				result = exception;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.LR.LogicalReader;
		String id = UUID.randomUUID().toString();
		
		try{
			mc.get(path, id);
			Assert.fail("Expected NoSuchIdException");
		}catch(NoSuchIdException e){
			//ignore
		}
	}
	
	@Test
	public void getWithNoSuchPathException(final @Mocked MCLogicalReaderSpec spec, final @Mocked LogicalReader cycle, final @Mocked NoSuchPathException exception) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.get(this.<UUID>withNotNull());
				result = exception;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.LR.LogicalReader;
		String id = UUID.randomUUID().toString();
		
		try{
			mc.get(path, id);
			Assert.fail("Expected NoSuchPathException");
		}catch(NoSuchPathException e){
			//ignore
		}
	}
	
	@Test
	public void getWithALEException(final @Mocked MCLogicalReaderSpec spec, final @Mocked LogicalReader cycle) throws Exception{
		final String reason = "someReason";
		new NonStrictExpectations() {
			{	
				cycle.get(this.<UUID>withNotNull());
				result = new ALEException(reason);
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.LR.LogicalReader;
		String id = UUID.randomUUID().toString();
		
		try{
			mc.get(path, id);
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals(reason, e.getMessage());
		}
	}
	
	@Test
	public void listWithLR(final @Mocked LogicalReader reader, final @Mocked ArrayList<String> expected) throws Exception{
		listWithCycle(reader, expected, Path.Service.LR.LogicalReader);
	}	
	
	@Test
	public void listWithTM(final @Mocked MCTagMemorySpec spec, final @Mocked TagMemory tagMemory, final @Mocked ArrayList<String> expected) throws Exception{
		listWithCycle(tagMemory, expected, Path.Service.TM.TagMemory);
	}	
	
	@Test
	public void listWithEC(final @Mocked EventCycle cycle, final @Mocked ArrayList<String> expected) throws Exception{
		listWithCycle(cycle, expected, Path.Service.EC.EventCycle);
	}	
	
	@Test
	public void listWithCC(final @Mocked CommandCycle cycle, final @Mocked ArrayList<String> expected) throws Exception{
		listWithCycle(cycle, expected, Path.Service.CC.CommandCycle);
	}
	
	@Test
	public void listWithCache(final @Mocked Cache cache, final @Mocked ArrayList<String> expected) throws Exception{
		listWithCycle(cache, expected, Path.Service.CC.Cache);
	}
	
	@Test
	public void listWithAssoc(final @Mocked Association assoc, final @Mocked ArrayList<String> expected) throws Exception{
		listWithCycle(assoc, expected, Path.Service.CC.Association);
	}
	
	@Test
	public void listWithRandom(final @Mocked MCRandomSpec spec, final @Mocked Random cycle, final @Mocked ArrayList<String> expected) throws Exception{
		listWithCycle(cycle, expected, Path.Service.CC.Random);
	}
	
	@Test
	public void listWithRandom(final @Mocked PortCycle cycle, final @Mocked ArrayList<String> expected) throws Exception{
		listWithCycle(cycle, expected, Path.Service.PC.PortCycle);
	}
	
	private void listWithCycle(final Depot<?, ?> depot, final ArrayList<String> expected, final String path) throws NoSuchIdException, NoSuchPathException, ImplementationException{
		new NonStrictExpectations() {
			{
				depot.toList();
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String parent = UUID.randomUUID().toString();
		
		final ArrayList<String> actual = (ArrayList<String>) mc.list(path, parent);
		
		new Verifications() {
			{
				depot.toList();
				times = 1;
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void listWithECSubscriber(final @Mocked EventCycle cycle, final @Mocked ArrayList<String> expected) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.toList(this.<UUID>withNotNull());
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.EC.Subscriber;
		String parent = UUID.randomUUID().toString();
		
		final ArrayList<String> actual = (ArrayList<String>) mc.list(path, parent);
		
		new Verifications() {
			{
				cycle.toList(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(expected, actual);
			}
		};
	}

	@Test
	public void listWithCCSubscriber(final @Mocked CommandCycle cycle, final @Mocked ArrayList<String> expected) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.toList(this.<UUID>withNotNull());
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.CC.Subscriber;
		String parent = UUID.randomUUID().toString();
		
		final ArrayList<String> actual = (ArrayList<String>) mc.list(path, parent);
		
		new Verifications() {
			{
				cycle.toList(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void listWithPCSubscriber(final @Mocked PortCycle cycle, final @Mocked ArrayList<String> expected) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.toList(this.<UUID>withNotNull());
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.PC.Subscriber;
		String parent = UUID.randomUUID().toString();
		
		final ArrayList<String> actual = (ArrayList<String>) mc.list(path, parent);
		
		new Verifications() {
			{
				cycle.toList(this.<UUID>withNotNull());
				times = 1;
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void listWithReader(final @Mocked Reader reader, final @Mocked ArrayList<String> expected) throws Exception{
		new NonStrictExpectations() {
			{
				Reader.getInstance().getTypes();
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Connector.Reader;
		String parent = UUID.randomUUID().toString();
		
		final ArrayList<String> actual = (ArrayList<String>) mc.list(path, parent);
		
		new Verifications() {
			{
				Reader.getInstance().getTypes();
				times = 1;
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void listWithSubscriber(final @Mocked havis.middleware.ale.core.subscriber.Subscriber subscriber, final @Mocked ArrayList<String> expected) throws Exception{
		new NonStrictExpectations() {
			{
				havis.middleware.ale.core.subscriber.Subscriber.getInstance().getTypes();
				result = expected;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Connector.Subscriber;
		String parent = UUID.randomUUID().toString();
		
		final ArrayList<String> actual = (ArrayList<String>) mc.list(path, parent);
		
		new Verifications() {
			{
				havis.middleware.ale.core.subscriber.Subscriber.getInstance().getTypes();
				times = 1;
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void listWithUnknownPath() throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path = "somePath";

		 try {
			 mc.list(path);
	         Assert.fail("Expected NoSuchPathException");
	     } catch (NoSuchPathException e) {
	         Assert.assertEquals("Unknown path 'somePath'", e.getMessage());
	     }	
	}
	
	@Test
	public void listWithALEException(final @Mocked LogicalReader cycle, final @Mocked ALEException exception) throws Exception{
		new NonStrictExpectations() {
			{
				cycle.toList();
				result = exception;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.LR.LogicalReader;
		
		try{
			mc.list(path);
			Assert.fail("Expected ALEException");
		}catch(ALEException e){
			//ignore
		}
	}

	@Test
	public void setPropertyWithAleId(final @Mocked ConfigType configType) throws Exception{
		final String value = "ALEID";
		new NonStrictExpectations() {
			{
				configType.getGlobal().getAleid();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ALEID;
		final String actual = configType.getGlobal().getAleid();
		
		mc.setProperty(name, value);
		
		new Verifications() {
			{
				configType.getGlobal().setAleid(this.<String>withNotNull());
				times = 1;

				Assert.assertEquals(value, actual);
			}
		};
	}
	
	@Test
	public void setPropertyWithMaxThreads(final @Mocked ConfigType configType) throws Exception{
		final Long value = Long.valueOf(1);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getMaxThreads();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.MaxThreads;
		final Long actual = Long.valueOf(configType.getGlobal().getMaxThreads());
		
		mc.setProperty(name, value.toString());
		
		
		new Verifications() {
			{
				configType.getGlobal().setMaxThreads(value.longValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	}
	
	@Test
	public void setPropertyWithMaxThreadsAndInvalidValue(final @Mocked ConfigType configType) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.MaxThreads;
		final Integer value = Integer.valueOf(0);
	
		try{
			mc.setProperty(name, value.toString());
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals("Invalid value for property '/Global/MaxThreads'. Maximum thread count shall be greater then 0.", e.getMessage());
		}
	}
	
	@Test
	public void setPropertyWithThreadTimeout(final @Mocked ConfigType configType) throws Exception{
		final Long value = Long.valueOf(1);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getThreadTimeout();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ThreadTimeout;
		final Long actual = Long.valueOf(configType.getGlobal().getThreadTimeout());
		
		mc.setProperty(name, value.toString());

		new Verifications() {
			{
				configType.getGlobal().setThreadTimeout(value.longValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	}
	
	@Test
	public void setPropertyWithThreadTimeoutAndInvalidValue(final @Mocked ConfigType configType) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ThreadTimeout;
		final Integer value = Integer.valueOf(0);
	
		try{
			mc.setProperty(name, value.toString());
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals("Invalid value for property '/Global/ThreadTimeout'. Thread timeout shall be greater then 0.", e.getMessage());
		}
	}
	
	@Test
	public void setPropertyWithQueueWarningTimeout(final @Mocked ConfigType configType) throws Exception{
		final Long value = Long.valueOf(1);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getQueueWarningTimeout();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.QueueWarningTimeout;
		final Long actual = Long.valueOf(configType.getGlobal().getQueueWarningTimeout());
		
		mc.setProperty(name, value.toString());
		
		new Verifications() {
			{
				configType.getGlobal().setQueueWarningTimeout(value.longValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	} 
	
	@Test
	public void setPropertyWithQueueWarningTimeoutAndInvalidValue(final @Mocked ConfigType configType) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.QueueWarningTimeout;
		final Integer value = Integer.valueOf(0);
	
		try{
			mc.setProperty(name, value.toString());
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals("Invalid value for property '/Global/QueueWarningTimeout'. Queue warning timeout shall be greater then 0.", e.getMessage());
		}
	}
	
	@Test
	public void setPropertyWithPersistMode(final @Mocked Config config) throws Exception{
		final Boolean value = Boolean.TRUE;
		new NonStrictExpectations() {
			{
				Config.isPersistMode();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.PersistMode;
		final Boolean actual = Boolean.valueOf(Config.isPersistMode());
		
		mc.setProperty(name, value.toString());
		
		
		new Verifications() {
			{
				Config.setPersistMode(value.booleanValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	} 
	
	@Test
	public void setPropertyWithDuration(final @Mocked ConfigType configType) throws Exception{
		final Integer value = Integer.valueOf(100);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getReaderCycle().getDuration();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Duration;
		final Integer actual = Integer.valueOf(configType.getGlobal().getReaderCycle().getDuration());
		
		mc.setProperty(name, value.toString());
	
		new Verifications() {
			{
				configType.getGlobal().getReaderCycle().setDuration(value.intValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	} 
	
	@Test
	public void setPropertyWithDurationAndInvalidValue(final @Mocked ConfigType configType) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Duration;
		final Integer value = Integer.valueOf(-1);
	
		try{
			mc.setProperty(name, value.toString());
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals("Invalid value for property '/Global/ReaderCycle/Duration'. Duration of reader cycle shall be between 0 and 1000 milliseconds.", e.getMessage());
		}
	}
	
	@Test
	public void setPropertyWithCount(final @Mocked ConfigType configType) throws Exception{
		final Integer value = Integer.valueOf(100);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getReaderCycle().getCount();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Count;
		final Integer actual = Integer.valueOf(configType.getGlobal().getReaderCycle().getCount());
		
		mc.setProperty(name, value.toString());
		
		
		new Verifications() {
			{
				configType.getGlobal().getReaderCycle().setCount(value.intValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	} 
	
	@Test
	public void setPropertyWithCountAndInvalidValue(final @Mocked ConfigType configType) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Count;
		final Integer value = Integer.valueOf(-1);
	
		try{
			mc.setProperty(name, value.toString());
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals("Invalid value for property '/Global/ReaderCycle/Count'. Count of tags shall be between 0 and 65535.", e.getMessage());
		}
	}
	
	@Test
	public void setPropertyWithLifetime(final @Mocked ConfigType configType, final @Mocked Config config) throws Exception{
		final Long value = Long.valueOf(1000);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getReaderCycle().getLifetime();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Lifetime;
		final Long actual = Long.valueOf(configType.getGlobal().getReaderCycle().getLifetime());
		
		mc.setProperty(name, value.toString());
		
		
		new Verifications() {
			{
				configType.getGlobal().getReaderCycle().setLifetime(value.longValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	} 
	
	@Test
	public void setPropertyWithLifetimeAndInvalidValue(final @Mocked ConfigType configType) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Lifetime;
		final Long value = Long.valueOf(-1);
	
		try{
			mc.setProperty(name, value.toString());
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals("Invalid value for property '/Global/ReaderCycle/Lifetime'. Liftime of tags shall be greater then zero and lesser then seconds of a day.", e.getMessage());
		}
	}
	
	@Test
	public void setPropertyWithExtendedMode(final @Mocked Config config) throws Exception{
		final Boolean value = Boolean.TRUE;
		new NonStrictExpectations() {
			{
				Config.isExtendedMode();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.ExtendedMode;
		final Boolean actual = Boolean.valueOf(Config.isExtendedMode());
		
		mc.setProperty(name, value.toString());
		
		
		new Verifications() {
			{
				Config.setExtendedMode(value.booleanValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	} 
	
	@Test
	public void setPropertyWithConnectTimeout(final @Mocked ConfigType configType) throws Exception{
		final Integer value = Integer.valueOf(1000);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getSubscriber().getConnectTimeout();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.Subscriber.ConnectTimeout;
		final Integer actual = Integer.valueOf(configType.getGlobal().getSubscriber().getConnectTimeout());
		
		mc.setProperty(name, value.toString());
		
		
		new Verifications() {
			{
				configType.getGlobal().getSubscriber().setConnectTimeout(value.intValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	} 
	
	@Test
	public void setPropertyWithConnectTimeoutAndInvalidValue(final @Mocked ConfigType configType) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.Subscriber.ConnectTimeout;
		final Integer value = Integer.valueOf(-1);
	
		try{
			mc.setProperty(name, value.toString());
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals("Invalid value for property '/Global/Subscriber/ConnectTimeout'. Subscriber connect timeout must not be negative", e.getMessage());
		}
	}
	
	@Test
	public void setPropertyWithHttpSecurity(final @Mocked ConfigType configType) throws Exception{
		final Boolean value = Boolean.TRUE;
		new NonStrictExpectations() {
			{
				configType.getGlobal().getSubscriber().isHttpsSecurity();
				result = value;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.Subscriber.HttpsSecurity;
		final Boolean actual = Boolean.valueOf(configType.getGlobal().getSubscriber().isHttpsSecurity());
		
		mc.setProperty(name, value.toString());
	
		new Verifications() {
			{
				configType.getGlobal().getSubscriber().setHttpsSecurity(value.booleanValue());
				times = 1;
				
				Assert.assertEquals(value, actual);
			}
		};
	} 
	
	@Test
	public void setPropertyWithUnknownPath(final @Mocked ConfigType configType) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = "someUnknownPath";
		final Integer value = Integer.valueOf(100);
	
		try{
			mc.setProperty(name, value.toString());
			Assert.fail("Expected NoSuchPropertyException");
		}catch(NoSuchPropertyException e){
			Assert.assertEquals("Unknown property 'someUnknownPath'", e.getMessage());
		}
	}
	
	@Test
	public void getStandardVersion(){
		MC.reset();
		MC mc = MC.getInstance();
		
		final String actual = mc.getStandardVersion();
		
		new Verifications() {
			{
				Assert.assertEquals("1.1", actual);
			}
		};
	}
	
	@Test
	public void uuid() throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = "1111-2222-3333-4444-5555";
		
		final UUID actual = mc.uuid(id);
		
		new Verifications() {
			{
				Assert.assertEquals("00001111-2222-3333-4444-000000005555", actual.toString());
			}
		};
	}
	
	@Test
	public void uuidWithInvalidIdentifier() throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String id = "someInvalidId";
	
		try{
			mc.uuid(id);
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals("Invalid identifier 'someInvalidId'", e.getMessage());
		}
	}
	
	@Test
	public void getProperty(final @Mocked ConfigType configType) throws Exception{
		final String expected = "Name";
		new NonStrictExpectations() {
			{
				configType.getGlobal().getName();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.Name;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				configType.getGlobal().getName();
				times = 1;
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithAleId(final @Mocked ConfigType configType) throws Exception{
		final String expected = "ALEID";
		new NonStrictExpectations() {
			{
				configType.getGlobal().getAleid();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ALEID;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				configType.getGlobal().getAleid();
				times = 1;
				
				Assert.assertEquals(expected, actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithMaxThreads(final @Mocked ConfigType configType) throws Exception{
		final Long expected = Long.valueOf(2);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getMaxThreads();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.MaxThreads;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				Long.toString(configType.getGlobal().getMaxThreads());
				times = 1;
				
				Assert.assertEquals(expected.toString(), actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithPersistMode(final @Mocked Config config) throws Exception{
		final Boolean expected = Boolean.TRUE;
		new NonStrictExpectations() {
			{
				Config.isPersistMode();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.PersistMode;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				Boolean.toString(Config.isPersistMode());
				times = 1;

				Assert.assertEquals(expected.toString(), actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithDuration(final @Mocked ConfigType configType) throws Exception{
		final Integer expected = Integer.valueOf(100);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getReaderCycle().getDuration();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Duration;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				Integer.toString(configType.getGlobal().getReaderCycle().getDuration());
				times = 1;
				
				Assert.assertEquals(expected.toString(), actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithCount(final @Mocked ConfigType configType) throws Exception{
		final Integer expected = Integer.valueOf(10);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getReaderCycle().getCount();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Count;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				Integer.toString(configType.getGlobal().getReaderCycle().getCount());
				times = 1;
				
				Assert.assertEquals(expected.toString(), actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithLifetime(final @Mocked ConfigType configType) throws Exception{
		final Long expected = Long.valueOf(10);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getReaderCycle().getLifetime();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.Lifetime;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				Long.toString(configType.getGlobal().getReaderCycle().getLifetime());
				times = 1;
				
				Assert.assertEquals(expected.toString(), actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithExtendedMode(final @Mocked Config config) throws Exception{
		final Boolean expected = Boolean.TRUE;
		new NonStrictExpectations() {
			{
				Config.isExtendedMode();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.ReaderCycle.ExtendedMode;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				Boolean.toString(Config.isExtendedMode());
				times = 1;
				
				Assert.assertEquals(expected.toString(), actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithConnectTimeout(final @Mocked ConfigType configType) throws Exception{
		final Integer expected = Integer.valueOf(2000);
		new NonStrictExpectations() {
			{
				configType.getGlobal().getSubscriber().getConnectTimeout();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.Subscriber.ConnectTimeout;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				Integer.toString(configType.getGlobal().getSubscriber().getConnectTimeout());
				times = 1;
				
				Assert.assertEquals(expected.toString(), actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithHttpSecurity(final @Mocked ConfigType configType) throws Exception{
		final Boolean expected = Boolean.TRUE;
		new NonStrictExpectations() {
			{
				configType.getGlobal().getSubscriber().isHttpsSecurity();
				result = expected;
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String name = Property.Global.Subscriber.HttpsSecurity;
		
		final String actual = mc.getProperty(name);
		
		new Verifications() {
			{
				Boolean.toString(configType.getGlobal().getSubscriber().isHttpsSecurity());
				times = 1;
				
				Assert.assertEquals(expected.toString(), actual);
			}
		};
	}
	
	@Test
	public void getPropertyWithUnknownProperty() throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String name = "someUnknownProperty";
		
		try{
			mc.getProperty(name);
			Assert.fail("Expected NoSuchPropertyException");
		}catch(NoSuchPropertyException e){
			Assert.assertEquals("Unknown property 'someUnknownProperty'", e.getMessage());
		}
	}
	
	@Test
	public void getProperties(final @Mocked ConfigType configType) throws Exception{
		final String name = "Name";
		new NonStrictExpectations() {
			{
				configType.getGlobal().getName();
				result = name;
			}
		};
		MC.reset();
		MC mc = MC.getInstance();
		String propertyName = Property.Global.Name;
		List<String> propertyNames = new ArrayList<String>();
			propertyNames.add(propertyName);
		final MCProperty property = new MCProperty();
			property.setName(propertyName);
			property.setValue(mc.getProperty(propertyName));
		
		final ArrayList<MCProperty> actual = (ArrayList<MCProperty>) mc.getProperties(propertyNames);
		
		new Verifications() {
			{	
				String actualPropertyName = actual.get(0).getName();
				String expectedPropertyName = property.getName();
				Assert.assertEquals(expectedPropertyName, actualPropertyName);
			}
		};
	}
	
	@Test
	public void update(final @Mocked MCEventCycleSpec spec, final @Mocked EventCycle cycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.EC.EventCycle;
		String id = UUID.randomUUID().toString(); 
		
		mc.update(path, id, spec);
		
		new Verifications() {
			{
				cycle.update(this.<UUID>withNotNull(), this.<MCEventCycleSpec>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithLR(final @Mocked MCLogicalReaderSpec spec, final @Mocked LogicalReader reader) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.LR.LogicalReader;
		String id = UUID.randomUUID().toString(); 
		
		mc.update(path, id, spec);
		
		new Verifications() {
			{
				reader.update(this.<UUID>withNotNull(), this.<MCLogicalReaderSpec>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithTM(final @Mocked MCTagMemorySpec spec, final @Mocked TagMemory tagMemory) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.TM.TagMemory;
		String id = UUID.randomUUID().toString(); 
		
		mc.update(path, id, spec);
		
		new Verifications() {
			{
				tagMemory.update(this.<UUID>withNotNull(), this.<MCTagMemorySpec>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithECSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked EventCycle cycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.EC.Subscriber;
		String id = UUID.randomUUID().toString(); 
		String parent = UUID.randomUUID().toString();
		
		mc.update(path, id, spec, parent);
	
		new Verifications() {
			{
				cycle.update(this.<UUID>withNotNull(), this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithCc(final @Mocked MCCommandCycleSpec spec, final @Mocked CommandCycle cycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.CC.CommandCycle;
		String id = UUID.randomUUID().toString(); 
		
		mc.update(path, id, spec);
		
		new Verifications() {
			{
				cycle.update(this.<UUID>withNotNull(), this.<MCCommandCycleSpec>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithCcSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked CommandCycle cycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.CC.Subscriber;
		String id = UUID.randomUUID().toString(); 
		String parent = UUID.randomUUID().toString();
		
		mc.update(path, id, spec, parent);
		
		new Verifications() {
			{
				cycle.update(this.<UUID>withNotNull(), this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithCcCache(final @Mocked MCCacheSpec spec, final @Mocked Cache cache) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.CC.Cache;
		String id = UUID.randomUUID().toString(); 
		
		mc.update(path, id, spec);
		
		new Verifications() {
			{
				cache.update(this.<UUID>withNotNull(), this.<MCSubscriberSpec>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithCcAssociation(final @Mocked MCAssociationSpec spec, final @Mocked Association association) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.CC.Association;
		String id = UUID.randomUUID().toString(); 
		
		mc.update(path, id, spec);
		
		new Verifications() {
			{
				association.update(this.<UUID>withNotNull(), this.<MCSubscriberSpec>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithCcRandom(final @Mocked MCRandomSpec spec, final @Mocked Random random) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.CC.Random;
		String id = UUID.randomUUID().toString(); 
		
		mc.update(path, id, spec);
		
		new Verifications() {
			{
				random.update(this.<UUID>withNotNull(), this.<MCSubscriberSpec>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithPc(final @Mocked MCPortCycleSpec spec, final @Mocked PortCycle cycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.PC.PortCycle;
		String id = UUID.randomUUID().toString(); 
		
		mc.update(path, id, spec);
		
		new Verifications() {
			{
				cycle.update(this.<UUID>withNotNull(), this.<MCCommandCycleSpec>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithPcSubscriber(final @Mocked MCSubscriberSpec spec, final @Mocked PortCycle cycle) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  Path.Service.PC.Subscriber;
		String id = UUID.randomUUID().toString(); 
		String parent = UUID.randomUUID().toString();
		
		mc.update(path, id, spec, parent);
		
		new Verifications() {
			{
				cycle.update(this.<UUID>withNotNull(), this.<MCSubscriberSpec>withNotNull(), this.<UUID>withNotNull());
				times = 1;
			}
		};
	}
	
	@Test
	public void updateWithUnknownPath(final @Mocked MCSubscriberSpec spec) throws Exception{
		MC.reset();
		MC mc = MC.getInstance();
		String path =  "someUnknownPath";
		String id = UUID.randomUUID().toString(); 
		String parent = UUID.randomUUID().toString();
		
		try{
			mc.update(path, id, spec, parent);
			Assert.fail("Expected NoSuchPathException");
		}catch(NoSuchPathException e){
			Assert.assertEquals("Unknown path 'someUnknownPath'", e.getMessage());
		}
	}
	
	@Test
	public void updateWithALEException(final @Mocked MCSubscriberSpec spec, final @Mocked LogicalReader reader) throws Exception{
		final String message = "someException";
		new NonStrictExpectations() {
			{
				reader.update(this.<UUID>withNotNull(), this.<MCLogicalReaderSpec>withNotNull());
				result = new ALEException(message);
			}
		};
		
		MC.reset();
		MC mc = MC.getInstance();
		String path = Path.Service.LR.LogicalReader;
		String id = UUID.randomUUID().toString(); 
		String parent = UUID.randomUUID().toString();
		
		try{
			mc.update(path, id, spec, parent);
			Assert.fail("Expected ImplementationException");
		}catch(ImplementationException e){
			Assert.assertEquals(message, e.getMessage());
		}
	}
	
	@Test 
	public void execute(final @Mocked MCEventCycleSpec spec, final @Mocked EC ec, final @Mocked EventCycle cycle) throws Exception{
		final ECReports reports = new ECReports();
		final String name = "ecSpec";
		new NonStrictExpectations() {
			{
				spec.isEnable();
				result = Boolean.TRUE;
				
				spec.getName();
				result = name; 
				
				ec.poll(this.<String>withNotNull());
				result = reports;
				
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		
		MC.reset();
		final MC mc = MC.getInstance();
		String path = Path.Service.EC.EventCycle;
		String id = UUID.randomUUID().toString();
		
		mc.execute(path, id);
		
		new Verifications() {
			{
				spec.isEnable(); 
				times = 1;
				
				ec.poll(this.<String>withNotNull());
				times = 1;			
			}
		};
	}

	@Test 
	public void executePc(final @Mocked PC pc) throws Exception{
		final MCPCOpSpecs specs = new MCPCOpSpecs();
		specs.setSpecs(new PCOpSpecs());
		PCOpSpec op = new PCOpSpec();
		op.setOpName("Test");
		specs.getSpecs().getOpSpec().add(op);
		final List<PCOpReport> reports = new ArrayList<>();
		new NonStrictExpectations() {
			{
				pc.execute(this.<List<PCOpSpec>>withNotNull());
				result = reports;
			}
		};
		
		MC.reset();
		final MC mc = MC.getInstance();
		
		List<PCOpReport> result = mc.execute(specs);
		Assert.assertSame(reports, result);
		
		new Verifications() {
			{
				pc.execute(withSameInstance(specs.getSpecs().getOpSpec()));
				times = 1;			
			}
		};
	}

	@Test 
	public void executeWithEcAndSpecDisabled(final @Mocked MCEventCycleSpec spec, final @Mocked EC ec, final @Mocked EventCycle cycle) throws Exception{
		final ECReports reports = new ECReports();
		final ECSpec ecSpec = new ECSpec();
		new NonStrictExpectations() {
			{
				spec.isEnable();
				result = Boolean.FALSE;
				
				spec.getSpec();
				result = ecSpec; 
				
				ec.immediate(this.<ECSpec>withNotNull());
				result = reports;
				
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		
		MC.reset();
		final MC mc = MC.getInstance();
		String path = Path.Service.EC.EventCycle;
		String id = UUID.randomUUID().toString();
		
		mc.execute(path, id);
		
		new Verifications() {
			{
				spec.isEnable(); 
				times = 1;
				
				ec.immediate(this.<ECSpec>withNotNull());
				times = 1;	
				
				ec.poll(this.<String>withNotNull());
				times = 0;
			}
		};
	}
	
	@Test 
	public void executeWithCc(final @Mocked MCCommandCycleSpec spec, final @Mocked CC cc, final @Mocked CommandCycle cycle) throws Exception{
		final CCReports reports = new CCReports();
		final String name = "ccSpec";
		new NonStrictExpectations() {
			{
				spec.isEnable();
				result = Boolean.TRUE;
				
				spec.getName();
				result = name; 
				
				cc.poll(this.<String>withNotNull(), this.<List<CCParameterListEntry>>withNull());
				result = reports;
				
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		
		MC.reset();
		final MC mc = MC.getInstance();
		String path = Path.Service.CC.CommandCycle;
		String id = UUID.randomUUID().toString();
		
		mc.execute(path, id);
		
		new Verifications() {
			{
				spec.isEnable(); 
				times = 1;
				
				cc.poll(this.<String>withNotNull(), this.<List<CCParameterListEntry>>withNull());
				times = 1;	
				
				cc.immediate(this.<CCSpec>withNotNull());
				times = 0;	
			}
		};
	}
	
	@Test 
	public void executeWithCcAndSpecDisabled(final @Mocked MCCommandCycleSpec spec, final @Mocked CC cc, final @Mocked CommandCycle cycle) throws Exception{
		final CCReports reports = new CCReports();
		final CCSpec ccSpec = new CCSpec();
		new NonStrictExpectations() {
			{
				spec.isEnable();
				result = Boolean.FALSE;
				
				spec.getSpec();
				result = ccSpec; 
				
				cc.immediate(this.<CCSpec>withNotNull());
				result = reports;
				
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		
		MC.reset();
		final MC mc = MC.getInstance();
		String path = Path.Service.CC.CommandCycle;
		String id = UUID.randomUUID().toString();
		
		mc.execute(path, id);
		
		new Verifications() {
			{
				spec.isEnable(); 
				times = 1;
				
				cc.immediate(this.<CCSpec>withNotNull());
				times = 1;		
				
				cc.poll(this.<String>withNotNull(), this.<List<CCParameterListEntry>>withNull());
				times = 0;
			}
		};
	}
	
	@Test 
	public void executeWithPc(final @Mocked MCPortCycleSpec spec, final @Mocked PC pc, final @Mocked PortCycle cycle) throws Exception{
		final PCReports reports = new PCReports();
		final String name = "pcSpec";
		new NonStrictExpectations() {
			{
				spec.isEnable();
				result = Boolean.TRUE;
				
				spec.getName();
				result = name; 
				
				pc.poll(this.<String>withNotNull());
				result = reports;
				
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		
		MC.reset();
		final MC mc = MC.getInstance();
		String path = Path.Service.PC.PortCycle;
		String id = UUID.randomUUID().toString();
		
		mc.execute(path, id);
		
		new Verifications() {
			{
				spec.isEnable(); 
				times = 1;
				
				pc.poll(this.<String>withNotNull());
				times = 1;		
				
				pc.immediate(this.<PCSpec>withNotNull());
				times = 0;	
			}
		};
	}
	
	@Test 
	public void executeWithPcAndSpecDisabled(final @Mocked MCPortCycleSpec spec, final @Mocked PC pc, final @Mocked PortCycle cycle) throws Exception{
		final PCReports reports = new PCReports();
		final PCSpec pcSpec = new PCSpec();
		new NonStrictExpectations() {
			{
				spec.isEnable();
				result = Boolean.FALSE;
				
				spec.getSpec();
				result = pcSpec; 
				
				pc.immediate(this.<PCSpec>withNotNull());
				result = reports;
				
				cycle.get(this.<UUID>withNotNull());
				result = spec;
			}
		};
		
		MC.reset();
		final MC mc = MC.getInstance();
		String path = Path.Service.PC.PortCycle;
		String id = UUID.randomUUID().toString();
		
		mc.execute(path, id);
		
		new Verifications() {
			{
				spec.isEnable(); 
				times = 1;
				
				pc.immediate(this.<PCSpec>withNotNull());
				times = 1;
				
				pc.poll(this.<String>withNotNull());
				times = 0;
			}
		};
	}
	
	@Test 
	public void executeWithUnknownPath() throws Exception{
		MC.reset();
		final MC mc = MC.getInstance();
		String path = "someUnknownPath";
		String id = UUID.randomUUID().toString();
		final IReports<?, ?> reports =  (IReports<?, ?>) mc.execute(path, id);
		
		new Verifications() {
			{
				Assert.assertNull(reports);
			}
		};
	}
}
