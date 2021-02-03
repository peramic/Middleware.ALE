package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.ECFilterListMember.PatList;
import havis.middleware.ale.service.cc.CCFilterSpec;
import havis.middleware.ale.service.cc.CCFilterSpec.FilterList;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FilterTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void filter() throws ValidationException {
        CCFilterSpec spec = new CCFilterSpec();
        spec.setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        spec.getFilterList().getFilter().add(member1);
        ECFilterListMember member2 = new ECFilterListMember();
        member2.setPatList(new PatList());
        member2.getPatList().getPat().add("X");
        member2.setIncludeExclude("EXCLUDE");
        member2.setFieldspec(new ECFieldSpec("killPwd"));
        spec.getFilterList().getFilter().add(member2);
        Filter filter = new Filter(spec);

        Collection<Operation> operations = filter.getOperations();
        Assert.assertNotNull(operations);
        Assert.assertEquals(2, operations.size());
        Iterator<Operation> iterator = operations.iterator();
        Operation op1 = iterator.next();
        Assert.assertEquals(0, op1.getId());
        Assert.assertEquals(OperationType.READ, op1.getType());
        Assert.assertEquals("epc", op1.getField().getName());
        Operation op2 = iterator.next();
        Assert.assertEquals(0, op2.getId());
        Assert.assertEquals(OperationType.READ, op2.getType());
        Assert.assertEquals("killPwd", op2.getField().getName());
    }
    
    @Test
    public void filterEmpty() throws ValidationException {
        CCFilterSpec spec = new CCFilterSpec();
        Filter filter = new Filter(spec);

        Collection<Operation> operations = filter.getOperations();
        Assert.assertNotNull(operations);
        Assert.assertEquals(0, operations.size());
    }
}
