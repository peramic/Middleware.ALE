package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.IFieldSpec;
import havis.middleware.ale.service.tm.TMFixedFieldListSpec;
import havis.middleware.ale.service.tm.TMFixedFieldSpec;
import havis.middleware.ale.service.tm.TMSpec;
import havis.middleware.ale.service.tm.TMVariableFieldListSpec;
import havis.middleware.ale.service.tm.TMVariableFieldSpec;
import havis.middleware.misc.TdtWrapper;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;
import havis.middleware.utils.data.Calculator;
import havis.middleware.utils.data.Converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class allows to define fields by {@link TMSpec} or {@link IFieldSpec}.
 * It contains all necessary information about reserved field names from ALE 1.1
 * (6.1).
 */
public class Fields {

	static Lock lock = new ReentrantLock();

	@SuppressWarnings("serial")
	final static List<String> RESERVED = new ArrayList<String>() {
		{
			for (String s : new String[] { "epc", "killPwd", "accessPwd",
					"epcBank", "tidBank", "userBank", "afi", "nsi" }) {
				add(s);
			}
		}
	};

	static Pattern GENERIC = Pattern
			.compile("^@(?<bank>(0|[1-9][0-9]*))\\.(?<length>([1-9][0-9]*))(.(?<offset>(0|[1-9][0-9]*)))?$");

	static Pattern OID_FIELD = Pattern.compile("^@(?<bank>(0|[1-9][0-9]*))\\.(?<oid>.*)$");
	static Pattern OID_VALUE;

	static Pattern FORMAT_HEX = Pattern.compile("^x(?<hex>[\\da-fA-F]+)$");
	static Pattern FORMAT_BITS = Pattern
			.compile("^(?<bits>\\d+):x(?<hex>[\\da-fA-F]+)$");
	static Pattern FORMAT_RAW = Pattern
			.compile("^urn:epc:raw:(?<length>\\d+).(?<x>x?)(?<hex>[\\da-fA-F]+)$");

	Map<String, CommonField> fields;

	static Fields instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new Fields();
		OID_VALUE = Pattern.compile("^(?<oid>" + Config.getInstance().getGlobal().getUrn().getOid() + ")$");
	}

	/**
	 * Creates a new instance
	 */
	public Fields() {
		fields = new HashMap<String, CommonField>();
	}

	/**
	 * Retrieving the static instance
	 *
	 * @return The static instance
	 */
	public static Fields getInstance() {
		return instance;
	}

	public static void lock() {
		lock.lock();
	}

	public static void unlock() {
		lock.unlock();
	}

	/**
	 * Retrieving the {@link CommonField} with specific name. If field name can
	 * not be found null will returned. If given value is null the field name
	 * will removed
	 *
	 * @param name
	 *            The name of the field
	 * @return The common field object
	 */
	public CommonField get(String name) {
		try {
			lock();
			return fields.get(name);
		} finally {
			unlock();
		}

	}

	public void set(String name, CommonField field) {
		try {
			lock();
			// remove key if value is null
			if (field == null)
				fields.remove(name);
			else
				fields.put(name, field);
		} finally {
			unlock();
		}
	}

	/**
	 * Validates if field with specific name exists.
	 *
	 * @param name
	 *            The name of the field
	 * @return Returns true if field exists false otherwise
	 */
	public boolean containsKey(String name) {
		try {
			lock();
			return fields.containsKey(name);
		} finally {
			unlock();
		}
	}

	/**
	 * Returns the {@link FieldDatatype} by string.
	 *
	 * @param fieldname
	 *            Name of the field
	 * @param datatype
	 *            String representation of data type
	 * @return field data type or null if data type parameter is null
	 * @throws ValidationException
	 *             if data type is not known
	 */
	public static FieldDatatype getDatatype(String fieldname, String datatype)
			throws ValidationException {
		if (datatype == null) {
			return null;
		} else {
			switch (datatype) {
			case "bits":
				return FieldDatatype.BITS;
			case "epc":
				return FieldDatatype.EPC;
			case "uint":
				return FieldDatatype.UINT;
			case "iso-15962-string":
				return FieldDatatype.ISO;
			default:
				throw new ValidationException("Unknown datatype"
						+ (fieldname == null ? "" : " for " + fieldname) + " '"
						+ datatype + "'");
			}
		}
	}

	/**
	 * Returns the {@link FieldFormat} by {@link IFieldSpec} and
	 * {@link FieldDatatype}. Checks if the value of the format parameter in is
	 * valid for standard.
	 *
	 * @param fieldname
	 *            The field name
	 * @param datatype
	 *            The field format
	 * @param format
	 *            The field data type
	 * @param standard
	 *            The default field data type to evaluate
	 * @return field format or null if standard field data type is null
	 * @throws ValidationException
	 *             if field format is not known for given data type
	 */
	public static FieldFormat getFormat(String fieldname, String datatype,
			String format, FieldDatatype standard) throws ValidationException {
		switch (standard) {
		case EPC:
			if ((datatype == null) || (datatype.equals("epc"))) {
				if (format == null) {
					return null;
				} else {
					switch (format) {
					case "epc-pure":
						return FieldFormat.EPC_PURE;
					case "epc-tag":
						return FieldFormat.EPC_TAG;
					case "epc-hex":
						return FieldFormat.EPC_HEX;
					case "epc-decimal":
						return FieldFormat.EPC_DECIMAL;
					default:
						throw new ValidationException("Unknown format '"
								+ format + "' for datatype epc");
					}
				}
			} else {
				throw new ValidationException(
						"Datatype for epc shall be epc not '" + datatype + "'");
			}
		case UINT:
			if ((datatype == null) || ("uint".equals(datatype))) {
				if (format == null) {
					return null;
				} else {
					switch (format) {
					case "hex":
						return FieldFormat.HEX;
					case "decimal":
						return FieldFormat.DECIMAL;
					default:
						throw new ValidationException("Unknown format '"
								+ format + "' for datatype uint");
					}
				}
			} else {
				throw new ValidationException("Datatype"
						+ (fieldname == null ? "" : " for " + fieldname)
						+ " shall be uint not '" + datatype + "'");
			}
		case BITS:
			if ((datatype == null) || ("bits".equals(datatype))) {
				if (format == null) {
					return null;
				} else {
					switch (format) {
					case "hex":
						return FieldFormat.HEX;
					default:
						throw new ValidationException("Unknown format '"
								+ format + "' for datatype bits");
					}
				}
			} else {
				throw new ValidationException("Datatype"
						+ (fieldname == null ? "" : " for " + fieldname)
						+ " shall be bits not '" + datatype + "'");
			}
		case ISO:
			if ((datatype == null) || ("iso-15962-string".equals(datatype))) {
				if ((format == null) || ("string".equals(format))) {
					return FieldFormat.STRING;
				} else {
					throw new ValidationException(
							"Unknown format '"
									+ format
									+ "' for datatype iso-15962-string. Format shall be string");
				}
			} else {
				throw new ValidationException("Datatype"
						+ (fieldname == null ? "" : " for " + fieldname)
						+ " shall be iso-15962-string not '" + datatype + "'");
			}
		default:
			return null;
		}
	}

	/**
	 * Returns the {@link FieldFormat} by {@link IFieldSpec} and
	 * {@link FieldDatatype}. Checks if the value of the format parameter in
	 * specification is valid for data type.
	 *
	 * @param spec
	 *            The {@link IFieldSpec} witch data type and format parameter
	 * @param datatype
	 *            The field data type to evaluate
	 * @return field format or null if data type is null
	 * @throws ValidationException
	 *             if field format is not known for given data type
	 */
	public static FieldFormat getFormat(IFieldSpec spec, FieldDatatype datatype)
			throws ValidationException {
		return getFormat(spec.getFieldname(), spec.getDatatype(),
				spec.getFormat(), datatype);
	}

	/**
	 * Returns a {@link CommonField} instance. Sets the format to given default
	 * if format is not set in specification.
	 *
	 * @param spec
	 *            The field specification to determine the field format
	 * @param datatype
	 *            The data type to set
	 * @param format
	 *            The default field format if specification does not contain a
	 *            type
	 * @param bank
	 *            The field bank
	 * @return A new instance of a common field
	 * @throws ValidationException
	 */
	CommonField getField(IFieldSpec spec, FieldDatatype datatype,
			FieldFormat format, int bank) throws ValidationException {
		return getField(spec, datatype, format, bank, 0, 0);
	}

	/**
	 * Returns a {@link CommonField} instance. Sets the format to given default
	 * if format is not set in specification.
	 *
	 * @param spec
	 *            The field specification to determine the field format
	 * @param datatype
	 *            The data type to set
	 * @param format
	 *            The default field format if specification does not contain a
	 *            type
	 * @param bank
	 *            The field bank
	 * @param length
	 *            The length of field data
	 * @return A new instance of a common field
	 * @throws ValidationException
	 */
	CommonField getField(IFieldSpec spec, FieldDatatype datatype,
			FieldFormat format, int bank, int length)
			throws ValidationException {
		return getField(spec, datatype, format, bank, length, 0);
	}

	/**
	 * Returns a {@link CommonField} instance. Sets the format to given default
	 * if format is not set in specification.
	 *
	 * @param spec
	 *            The field specification to determine the field format
	 * @param datatype
	 *            The data type to set
	 * @param format
	 *            The default field format if specification does not contain a
	 *            type
	 * @param bank
	 *            The field bank
	 * @param length
	 *            The length of field data
	 * @param offset
	 *            The field data offset
	 * @return A new instance of a common field
	 * @throws ValidationException
	 */
	CommonField getField(IFieldSpec spec, FieldDatatype datatype,
			FieldFormat format, int bank, int length, int offset)
			throws ValidationException {
		FieldFormat _format = getFormat(spec, datatype);
		return new CommonField(spec.getFieldname(), datatype,
				_format == null ? format : _format, bank, length, offset,
				"epc".equals(spec.getFieldname()));
	}

	static FieldFormat getFormat(FieldDatatype datatype)
			throws ValidationException {
		switch (datatype) {
		case BITS:
			return FieldFormat.HEX;
		case UINT:
			return FieldFormat.HEX;
		case EPC:
			return FieldFormat.EPC_TAG;
		case ISO:
			return FieldFormat.STRING;
		default:
			throw new ValidationException(null);
		}
	}

	/**
	 * Returns a {@link CommonField} instance. Sets the data type and format to
	 * given default if missed in specification.
	 *
	 * @param spec
	 *            The field specification to determine the field format
	 * @return The common field
	 * @throws ValidationException
	 */
	public static CommonField getField(IFieldSpec spec)
			throws ValidationException {
		return instance.get(spec);
	}

	/**
	 * Returns the static field data if field name is reserved, returns the
	 * predefined field and override data type an format or returns a newly
	 * created common field if field name starts with @ and contains information
	 * about nak, length and offset or OID.
	 *
	 * @param spec
	 *            The field specification
	 * @return A common field instance
	 *         {@code If field name is epcBank a common field with bank 1 and length 0 will returned. A field name of @1.0 also returns a common field with bank 1 and length 0. A field name of @3.oid:... returns a common field with bank 3 with ISO 15962 data type}
	 * @throws ValidationException
	 */
	public CommonField get(IFieldSpec spec) throws ValidationException {
		if (spec == null) {
			throw new ValidationException("Field specification is null");
		} else {
			if (Name.isValid(spec.getFieldname(), false)) {
				switch (spec.getFieldname()) {
				case "epc": // @1.0.32
					return getField(spec, FieldDatatype.EPC,
							FieldFormat.EPC_TAG, 1, 0, 16);
				case "killPwd": // @0.32
					return getField(spec, FieldDatatype.UINT, FieldFormat.HEX,
							0, 32);
				case "accessPwd": // @0.32.32
					return getField(spec, FieldDatatype.UINT, FieldFormat.HEX,
							0, 32, 32);
				case "epcBank": // @1.0
					return getField(spec, FieldDatatype.BITS, FieldFormat.HEX,
							1);
				case "tidBank": // @2.0
					return getField(spec, FieldDatatype.BITS, FieldFormat.HEX,
							2);
				case "userBank": // @3.0
					return getField(spec, FieldDatatype.BITS, FieldFormat.HEX,
							3);
				case "afi": // @1.8.24
					return getField(spec, FieldDatatype.UINT, FieldFormat.HEX,
							1, 8, 24);
				case "nsi": // @1.9.23
					return getField(spec, FieldDatatype.UINT, FieldFormat.HEX,
							1, 9, 23);
				default:
					CommonField field = fields.get(spec.getFieldname());
					if (field != null) {
						return new ShadowField(field, spec);
					} else {
						if (spec.getFieldname().startsWith("@")) {
							Matcher match = GENERIC.matcher(spec.getFieldname());
							if (match.matches()) {
								String offset = match.group("offset");
								FieldDatatype datatype = getDatatype(spec.getFieldname(), spec.getDatatype());
								return getField(
										spec,
										datatype == null ? FieldDatatype.UINT
												: datatype,
										FieldFormat.HEX,
										Integer.parseInt(match.group("bank")),
										Integer.parseInt(match.group("length")),
										offset == null || offset.length() == 0 ? 0 : Integer.parseInt(offset));
							} else {
								match = OID_FIELD.matcher(spec.getFieldname());
								if (match.matches()) {
									int bank = Integer.parseInt(match.group("bank"));
									String oid = match.group("oid");
									match = OID_VALUE.matcher(oid);
									if (match.matches()) {
										return new VariableField(spec.getFieldname(), bank, oid);
									}
								}
								throw new ValidationException("Invalid fieldname '" + spec.getFieldname() + "'");
							}
						} else {
							// #ISSUE 1899 Fixed confusing error message when using inactive TM fields
							throw new ValidationException("Unknown or inactive field with name '" + spec.getFieldname() + "'");
						}
					}
				}
			} else {
				return null;
			}
		}
	}

	/**
	 * Returns a list of fields defined by {@link TMSpec} specification.
	 *
	 * @param spec
	 *            The tag management specification
	 * @return The common field array
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	public CommonField[] get(TMSpec spec) throws ImplementationException,
			ValidationException {
		List<CommonField> list = new ArrayList<CommonField>();
		if (spec instanceof TMFixedFieldListSpec) {
			for (TMFixedFieldSpec field : ((TMFixedFieldListSpec) spec)
					.getFixedFields().getFixedField()) {
				list.add(new FixedField(field));
			}
		} else if (spec instanceof TMVariableFieldListSpec) {
			for (TMVariableFieldSpec field : ((TMVariableFieldListSpec) spec)
					.getVariableFields().getVariableField()) {
				list.add(new VariableField(field));
			}
		} else {
			throw new ImplementationException(
					"Unknown tag memory field list type '"
							+ spec.getClass().getName() + "'");
		}
		return list.toArray(new CommonField[0]);
	}

	/**
	 * Returns the list of names defined by {@link TMSpec}.
	 *
	 * @param spec
	 *            The tag management specification
	 * @return A list of field names
	 * @throws ImplementationException
	 */
	public static String[] getNames(TMSpec spec) throws ImplementationException {
		List<String> list = new ArrayList<String>();
		if (spec instanceof TMFixedFieldListSpec) {
			for (TMFixedFieldSpec field : ((TMFixedFieldListSpec) spec)
					.getFixedFields().getFixedField()) {
				list.add(field.getFieldname());
			}
		} else if (spec instanceof TMVariableFieldListSpec) {
			for (TMVariableFieldSpec field : ((TMVariableFieldListSpec) spec)
					.getVariableFields().getVariableField()) {
				list.add(field.getFieldname());
			}
		} else {
			throw new ImplementationException(
					"Unknown tag memory field list type '"
							+ spec.getClass().getName() + "'");
		}
		return list.toArray(new String[0]);
	}

	public static byte[] toBytes(CommonField field, ReadResult result) {
		return Calculator.strip(result.getData(), field.getOffset() % 16, field.getLength());
	}

	public static long toLong(CommonField field, ReadResult result) {
		long l = 0;
		byte[] data = toBytes(field, result);
		for (byte b : data) {
			l = (b & 0xFF) + (l << 8);
		}
		return l >> (8 - field.getLength() % 8) % 8;
	}

	public static Bytes toBytes(CommonField field, String data)
			throws ValidationException {
		return toBytes(field.getFieldDatatype(), field.getFieldFormat(), data);
	}

	@SuppressWarnings("incomplete-switch")
	public static Bytes toBytes(FieldDatatype datatype, FieldFormat format,
			String data) throws ValidationException {
		if (data == null) {
			throw new ValidationException("Data value is not specified");
		} else {
			switch (datatype) {
			case EPC:
				switch (format) {
				case EPC_TAG:
					try {
						byte[] bytes = TagDecoder.getInstance().decodeUrn(data);
						return new Bytes(datatype, bytes, bytes.length * 8);
					} catch (Exception e) {
						throw new ValidationException("Data epc tag value '"
								+ data + "' is not valid");
					}
				case EPC_HEX:
				case EPC_DECIMAL:
					Matcher match = FORMAT_RAW.matcher(data);
					if (match.matches()) {
						if ((format == FieldFormat.EPC_HEX)
								&& (match.group("x").equals("x"))
								|| (format == FieldFormat.EPC_DECIMAL)
								&& (match.group("x").equals(""))) {
							try {
								try {
									int length = Integer.parseInt(match
											.group("length"));
									if ((length % 16) != 0)
										throw new Exception(
												"Length not modulo 16");
									if (((length / 16) + 2) > 31)
										throw new Exception("Length to big");
									byte[] bytes = Converter.hexToBytes(
											match.group("hex"), length);
									return new Bytes(datatype, bytes, length);
								} catch (NumberFormatException e) {
									throw new Exception(
											"Length is not a valid number");
								}
							} catch (Exception e) {
								throw new ValidationException(
										"Data epc raw length '" + data
												+ "' is not valid. "
												+ e.getMessage());
							}
						} else {
							throw new ValidationException(
									"Data epc raw value '" + data
											+ "' is not valid");
						}
					} else {
						throw new ValidationException("Data epc raw value '"
								+ data + "' is not valid");
					}
				case EPC_PURE:
					throw new ValidationException(
							"Field format epc_pure is not valid for write operation");
				}
				break;
			case UINT:
				switch (format) {
				case HEX:
					Matcher match = FORMAT_HEX.matcher(data);
					if (match.matches()) {
						String hex = match.group("hex");
						AtomicInteger length = new AtomicInteger(
								hex.length() * 4);
						return new Bytes(datatype, Calculator.trunc(
								Converter.hexToBytes(hex, length.intValue()),
								length), length.intValue());
					} else {
						throw new ValidationException("Data uint value '"
								+ data + "' is not in hex format");
					}
				case DECIMAL:
					try {
						AtomicInteger length = new AtomicInteger(0);
						return new Bytes(datatype, Converter.decToBytes(data, length), length.intValue());
					} catch (Exception e) {
						throw new ValidationException("Data uint value '"
								+ data + "' is not in decimal format");
					}
				}
				break;
			case BITS:
				switch (format) {
				case HEX:
					Matcher match = FORMAT_BITS.matcher(data);
					if (match.matches()) {
						String bits = match.group("bits");
						try {
							int length = Integer.parseInt(bits);
							return new Bytes(datatype, Converter.hexToBytes(
									match.group("hex"), length), length);
						} catch (NumberFormatException e) {
							throw new ValidationException("Data bits count '"
									+ bits
									+ "' should be a 32 bit signed integer");
						}
					} else {
						throw new ValidationException("Data bits value '"
								+ data + "' is not in hex format");
					}
				}
				break;
			}
			return null;
		}
	}

	/**
	 * Returns the string representation of byte array by {@link FieldDatatype}
	 * data type and {@link FieldFormat} format.
	 *
	 * @param field
	 *            The base field which contains data type, format and length
	 *            informations
	 * @param bytes
	 *            The byte array to decode
	 * @return The string representation of the byte data
	 * @throws ImplementationException
	 */
	public static String toString(CommonField field, byte[] bytes) {
		if (field.isAdvanced()) {
			bytes = Calculator.strip(bytes,
					field.getOffset() % 16 + (field.isEpc() ? 16 : 0),
					field.getLength());
		}
		return toString(field.getFieldDatatype(), field.getFieldFormat(),
				new Bytes(field.getFieldDatatype(), bytes,
						field.getLength() > 0 ? field.getLength()
								: bytes.length * 8 - field.getOffset() % 16));
	}

	/**
	 * Returns the hex string trunk to size i.e. 4 or 8 bits
	 *
	 * @param bytes
	 *            The bytes
	 * @param size
	 *            The size
	 * @return The hex string
	 */
	public static String toString(Bytes bytes, int size) {
		return Converter.toString(bytes.getValue(), bytes.getLength(),
				(byte) size);
	}

	/**
	 * Returns the string representation of byte array by {@link FieldDatatype}
	 * data type and {@link FieldFormat} format.
	 *
	 * @param datatype
	 *            The data type
	 * @param format
	 *            The format
	 * @param bytes
	 *            The byte array to decode
	 * @return The string representation of the byte data
	 * @throws ImplementationException
	 */
	@SuppressWarnings("incomplete-switch")
	public static String toString(FieldDatatype datatype, FieldFormat format,
			Bytes bytes) {
		switch (datatype) {
		case BITS:
			switch (format) {
			case HEX:
				return (bytes.getLength() > 0 ? bytes.getLength()
						+ ":x"
						+ toString(bytes, 4)
								.substring(
										0,
										(bytes.getLength() + (4 - bytes
												.getLength() % 4) % 4) / 4)
						: bytes.getLength() * 8 + ":x" + toString(bytes, 4));
			}
			break;
		case EPC:
			try {
				TdtTagInfo info = TdtWrapper.getTdt().translate(bytes.getValue());
				if (info != null) {
					switch (format) {
					case EPC_PURE:
						return info.getUriId();
					case EPC_TAG:
						return info.getUriTag();
					case EPC_HEX:
						return info.getUriRawHex();
					case EPC_DECIMAL:
						return info.getUriRawDecimal();
					}
				}
			} catch (TdtTranslationException e) {
				Exits.Log.logp(Exits.Level.Error, Exits.Common.Name, Exits.Common.Error, "Parsing of field failed: " + e.getMessage(), e);
			}
			break;
		case ISO:
			return "x" + toString(bytes, 8);
		case UINT:
			switch (format) {
			case DECIMAL:
				long i = 0;
				for (short b : bytes.getValue())
					i = (i << 8) + (b & 0xFF);
				i >>= ((8 - bytes.getLength() % 8) % 8);
				return Long.toString(i);
			case HEX:
				String s = trimLeadingZeros(toString(bytes, 8));
				return "x" + (s.length() > 0 ? s : "0");
			}
			break;
		}
		return null;
	}

	private static String trimLeadingZeros(String hex) {
		if (hex != null) {
			for (int i = 0; i < hex.length(); i++) {
				char c = hex.charAt(i);
				if (c != '0') {
					return hex.substring(i);
				}
			}
			return "";
		}
		return hex;
	}

	public void dispose() {
		try {
			lock();
			fields.clear();
		} finally {
			unlock();
		}
	}
}
