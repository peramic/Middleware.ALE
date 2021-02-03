package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.tdt.LevelTypeList;
import havis.middleware.tdt.LevelX;
import havis.middleware.tdt.OptionX;

import java.util.ArrayList;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Test;

/**
 * testClass to mock schemes.getLevel() method in Patterns class
 */
public class PatternsMatchExceptionTest {

	@Test(expected = ValidationException.class)
	public void patternMatchExceptionTest(@Mocked final LevelX level) throws ValidationException {
		new NonStrictExpectations(){
			{
				level.getType();
				result = LevelTypeList.PURE_IDENTITY;

				level.getPrefixMatch();
				result = "urn:epc:id:gid";

				level.getOptions();
				result = new ArrayList<OptionX>();
			}
		};
		List<String> list = new ArrayList<>();
		list.add("urn:epc:idpat:gid:");
		new Patterns(PatternType.GROUP, list);
	}
}
