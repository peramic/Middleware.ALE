package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.ECFilterListMember.PatList;
import havis.middleware.ale.service.ec.ECFilterSpec;
import havis.middleware.ale.service.ec.ECFilterSpec.ExcludePatterns;
import havis.middleware.ale.service.ec.ECFilterSpec.IncludePatterns;
import havis.middleware.ale.service.ec.ECFilterSpecExtension;
import havis.middleware.ale.service.ec.ECFilterSpecExtension.FilterList;
import havis.middleware.tdt.TdtTranslationException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mockit.Mocked;
import mockit.Verifications;

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
    public void filterSuperClassEmpty() throws ValidationException {
    	ECFilterSpec spec = new ECFilterSpec();
        Filter filter = new Filter(spec);

        Collection<Operation> operations = filter.getOperations();
        Assert.assertNotNull(operations);
        Assert.assertEquals(0, operations.size());
    }

    @Test
    public void excludeTest() throws ValidationException, TdtTranslationException {
        Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tag.setResult(new HashMap<Integer, Result>());

        ECFilterSpec spec = new ECFilterSpec();
        spec.setExcludePatterns(new ExcludePatterns());
        spec.getExcludePatterns().getExcludePattern().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        Filter filter = new Filter(spec);
        Assert.assertEquals(Boolean.FALSE, filter.match(tag));
    }

    @Test
    public void excludeSuperClassTest() throws ValidationException, TdtTranslationException {
        Tag tagMatch = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tagMatch.setResult(new HashMap<Integer, Result>());
        Tag tagErrorResult = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
        Map<Integer, Result> errorResult = new HashMap<Integer, Result>();
        errorResult.put(Integer.valueOf(0), new Result(ResultState.MISC_ERROR_TOTAL));
        tagErrorResult.setResult(errorResult);

        ECFilterSpec spec = new ECFilterSpec();
        spec.setExtension(new ECFilterSpecExtension());
        spec.getExtension().setFilterList(new FilterList());
        ECFilterListMember member = new ECFilterListMember();
        member.setPatList(new PatList());
        member.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member.setIncludeExclude("EXCLUDE");
        member.setFieldspec(new ECFieldSpec("epc"));
        spec.getExtension().getFilterList().getFilter().add(member);
        Filter filter = new Filter(spec);
        Assert.assertEquals(Boolean.FALSE, filter.match(tagMatch));
        Assert.assertNull(filter.match(tagErrorResult));
    }

    @Test
    public void includeTest() throws ValidationException, TdtTranslationException {
        Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tag.setResult(new HashMap<Integer, Result>());

        ECFilterSpec spec = new ECFilterSpec();
        spec.setIncludePatterns(new IncludePatterns());
        spec.getIncludePatterns().getIncludePattern().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        Filter filter = new Filter(spec);
        Assert.assertEquals(Boolean.TRUE, filter.match(tag));
    }

    @Test
    public void includeSuperClassTest() throws ValidationException, TdtTranslationException {
        Tag tagMatch = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tagMatch.setResult(new HashMap<Integer, Result>());
        Tag tagNoMatch = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
        tagNoMatch.setResult(new HashMap<Integer, Result>());
        Tag tagErrorResult = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
        Map<Integer, Result> errorResult = new HashMap<Integer, Result>();
        errorResult.put(Integer.valueOf(0), new Result(ResultState.MISC_ERROR_TOTAL));
        tagErrorResult.setResult(errorResult);

        ECFilterSpec spec = new ECFilterSpec();
        spec.setExtension(new ECFilterSpecExtension());
        spec.getExtension().setFilterList(new FilterList());
        ECFilterListMember member = new ECFilterListMember();
        member.setPatList(new PatList());
        member.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member.setIncludeExclude("INCLUDE");
        member.setFieldspec(new ECFieldSpec("epc"));
        spec.getExtension().getFilterList().getFilter().add(member);
        Filter filter = new Filter(spec);
        Assert.assertEquals(Boolean.TRUE, filter.match(tagMatch));
        Assert.assertEquals(Boolean.FALSE, filter.match(tagNoMatch));
        Assert.assertNull(filter.match(tagErrorResult));
    }

    @Test
    public void getOperations() throws ValidationException {
        ECFilterSpec spec = new ECFilterSpec();
        spec.setExtension(new ECFilterSpecExtension());
        spec.getExtension().setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        spec.getExtension().getFilterList().getFilter().add(member1);
        ECFilterListMember member2 = new ECFilterListMember();
        member2.setPatList(new PatList());
        member2.getPatList().getPat().add("X");
        member2.setIncludeExclude("EXCLUDE");
        member2.setFieldspec(new ECFieldSpec("killPwd"));
        spec.getExtension().getFilterList().getFilter().add(member2);
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
    public void dispose(@Mocked final Patterns patterns) throws ValidationException {
        ECFilterSpec spec = new ECFilterSpec();
        spec.setExtension(new ECFilterSpecExtension());
        spec.getExtension().setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        spec.getExtension().getFilterList().getFilter().add(member1);
        ECFilterListMember member2 = new ECFilterListMember();
        member2.setPatList(new PatList());
        member2.getPatList().getPat().add("X");
        member2.setIncludeExclude("EXCLUDE");
        member2.setFieldspec(new ECFieldSpec("killPwd"));
        spec.getExtension().getFilterList().getFilter().add(member2);
        Filter filter = new Filter(spec);
        filter.dispose();

        new Verifications() {
            {
                patterns.dispose();
                times = 2;
            }
        };
    }
}
