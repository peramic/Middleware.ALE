/*****************************************************************************/
/*    FILE  NAME               	: UIntPattern                                */
/*    HOLDER OF RIGHTS OF USE	: HARTING KGaA                               */
/*    DATE OF FIRST RELEASE 	: MAY 2011                                   */
/*    AUTHOR                	: IT-AS RFID SW TEAM                         */
/*    DESCRIPTION           	: This file contains the UIntPattern         */
/*                            	  implementation                             */
/*---------------------------------------------------------------------------*/
/* CHANGE HISTORY :                                                          */
/*---------------------------------------------------------------------------*/
/* VERSION    DATE/                   DESCRIPTION OF CHANGE/                 */
/*            MODIFIED BY             FAULT REPORT NO                        */
/*---------------------------------------------------------------------------*/
/* 1.0.0.0    May 2011                Initial Create.                        */
/*            IT-AS RFID SW TEAM                                             */
/*****************************************************************************/

package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.tdt.TdtTagInfo;

/**
 * This class is used to bundle a set of pattern position to a coherent unit.
 * here each position of pattern will be validate. This class can handle
 * epc-pure and epc-tag pattern, uint pattern will be implemented soon
 */
public class BigIntPattern implements IPattern {

	private CommonField field;
	private BigIntPart part;

	/**
	 * Creates pattern instance. Validate the pattern.
	 * 
	 * @param type
	 *            The pattern type
	 * @param value
	 *            The part value
	 * @param field
	 *            The field definition to validate the value
	 * @throws ValidationException
	 *             if a specific value attached to a wildcard, group is given in
	 *             filter pattern type, the part value is invalid for field
	 *             definition.
	 */
	public BigIntPattern(PatternType type, String value, CommonField field)
			throws ValidationException {
		if ((field.getFieldFormat() == FieldFormat.HEX)
				|| (field.getFieldFormat() == FieldFormat.DECIMAL)) {
			// keep field
			this.field = field;
			// validate parts
			part = new BigIntPart(field, value);
		} else {
			throw new ValidationException(
					"Format for uint pattern should be hex or decimal");
		}
	}

	/**
	 * Indicates if value match this pattern.
	 * 
	 * @param info
	 *            The TDT tag info
	 * @param result
	 *            The read result
	 * @return True if pattern match, false otherwise
	 * */
	@Override
	public boolean match(TdtTagInfo info, Result result) {
		if (result.getState() == ResultState.SUCCESS) {
			if (result instanceof ReadResult) {
				return part.match(Fields.toBytes(field, (ReadResult) result));
			}
		}
		return false;
	}

	/**
	 * Returns the group name depending on pattern and value.
	 * 
	 * @param info
	 *            The TDT tag info
	 * @param result
	 *            The read result
	 * @return The group name or null if pattern does not match
	 */
	@Override
	public String name(TdtTagInfo info, Result result) {
		if (result.getState() == ResultState.SUCCESS) {
			if (result instanceof ReadResult) {
				return part.name(Fields.toBytes(field, (ReadResult) result));
			}
		}
		return null;
	}

	/**
	 * Returns null
	 * 
	 * @return null
	 */
	@Override
	public String next() {
		return null;
	}

	/**
	 * Returns if field and part are disjoint from local values
	 * 
	 * @param field
	 *            The field
	 * @param part
	 *            The part
	 * @return True, if field and part are disjoint, false otherwise
	 */
	boolean disjoint(CommonField field, BigIntPart part) {
		if (this.field.equals(field)) {
			return this.part.disjoint(part);
		}
		return true;
	}

	/**
	 * Returns if patterns are disjoint from local values
	 * 
	 * @param patterns
	 *            The patterns
	 * @return True, if all patterns are dis-join, false otherwise
	 */
	@Override
	public boolean disjoint(Iterable<IPattern> patterns) {
		for (IPattern pattern : patterns) {
			if (pattern instanceof BigIntPattern) {
				if (!((BigIntPattern) pattern).disjoint(field, part))
					return false;
			}
		}
		return true;
	}
}
