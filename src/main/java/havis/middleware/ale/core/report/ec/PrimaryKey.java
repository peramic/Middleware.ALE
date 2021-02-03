package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.utils.data.Comparison;

/**
 * Implements primary key
 */
public class PrimaryKey {

	private Tag tag;
	private Iterable<CommonField> fields;
	private int hashCode = 0;

	/**
	 * Creates a new instance
	 *
	 * @param tag
	 *            The tag
	 * @param fields
	 *            The fields
	 */
	public PrimaryKey(Tag tag, Iterable<CommonField> fields) {
		this.tag = tag;
		this.fields = fields;
	}

	/**
	 * Checks if for all primary key fields the tag has a corresponding value
	 *
	 * @return Returns true if the tag has all corresponding values
	 */
	public boolean match() {
		if (fields != null) {
			int id = 1;
			for (CommonField field : fields) {
				switch (field.getName()) {
				case "epc":
					break;
				default:
					if (Tag.isExtended() && "tidBank".equals(field.getName())) {
						break;
					}
					Result result;
					if (field.getFieldDatatype() == FieldDatatype.ISO
							|| (result = tag.getResult().get(Integer.valueOf(id))) == null
							|| (result.getState() != ResultState.SUCCESS)
							|| !(result instanceof ReadResult)) {
						return false;
					}
					id++;
					break;
				}
			}
		}
		return true;
	}

	/**
	 * Compares value of tag with current tag
	 *
	 * @param primaryKey
	 *            The primary key
	 * @return True, if value is equal, false otherwise
	 */
	protected boolean equals(PrimaryKey primaryKey) {
		if ((primaryKey.tag.getResult() != null)
				&& (this.tag.getResult() != null)) {
			if (fields == null) {
				return this.tag.getEpc().length == primaryKey.tag.getEpc().length
						&& Comparison.equal(this.tag.getEpc(),
								primaryKey.tag.getEpc(),
								this.tag.getEpc().length * 8);
			} else {
				int id = 1;
				for (CommonField field : fields) {
					if ("epc".equals(field.getName())) {
                        if (this.tag.getEpc().length != primaryKey.tag.getEpc().length
                                || !Comparison.equal(this.tag.getEpc(), primaryKey.tag.getEpc(), this.tag.getEpc().length * 8))
							return false;
					} else {
						if (Tag.isExtended()
								&& "tidBank".equals(field.getName())) {
                            if (this.tag.getTid().length != primaryKey.tag.getTid().length
                                    || !Comparison.equal(this.tag.getTid(), primaryKey.tag.getTid(), this.tag.getTid().length * 8))
                                return false;
						} else {
							Result left = primaryKey.tag.getResult().get(Integer.valueOf(id));
							Result right = this.tag.getResult().get(Integer.valueOf(id));
							if (left != null && right != null) {
								if ((left.getState() == ResultState.SUCCESS)
										&& (right.getState() == ResultState.SUCCESS)) {
									if ((left instanceof ReadResult)
											&& (right instanceof ReadResult)) {
										if (field.getLength() == 0) {
											if (((ReadResult) left).getData().length != ((ReadResult) right)
													.getData().length)
												return false;
											if (!Comparison
													.equal(((ReadResult) left)
															.getData(),
															((ReadResult) right)
																	.getData(),
															((ReadResult) left)
																	.getData().length * 8,
															field.getOffset() % 16))
												return false;
										} else {
											if (!Comparison.equal(
													((ReadResult) left)
															.getData(),
													((ReadResult) right)
															.getData(), field
															.getLength(), field
															.getOffset() % 16))
												return false;
										}
									} else {
										return false;
									}
								} else {
									if (left.getState() != right.getState())
										return false;
								}
							} else {
								if (left != right)
									return false;
							}
							id++;
						}
					}
				}
			}
		} else {
		    // if only one result is null
			if (primaryKey.tag.getResult() != this.tag.getResult())
				return false;
		}
		return true;
	}

	/**
	 * Returns true if object is equal to this
	 *
	 * @return True, if equal, false otherwise
	 */
	@Override
    public boolean equals(Object obj) {
		return (obj == this) || ((obj instanceof PrimaryKey) && equals((PrimaryKey) obj));
	}

	/**
	 * Returns the hash code
	 *
	 * @return The hash code
	 */
	@Override
    public int hashCode() {
		if (hashCode == 0) {
			if (tag.getResult() != null) {
				if (fields == null) {
				    if (tag.getEpc() != null) {
				        hashCode = Comparison.hashCode(tag.getEpc());
				    }
				} else {
					int id = 0;
					for (CommonField field : fields) {
						if ("epc".equals(field.getName())) {
							hashCode ^= Comparison.hashCode(tag.getEpc());
						} else {
							if (Tag.isExtended()
									&& "tidBank".equals(field.getName())) {
								hashCode ^= Comparison.hashCode(tag.getTid());
							} else {
								Result result;
								if ((result = tag.getResult().get(Integer.valueOf(++id))) != null) {
									if ((result.getState() == ResultState.SUCCESS)
											&& (result instanceof ReadResult)
											&& (((ReadResult) result).getData() != null)) {
										byte[] data = ((ReadResult) result)
												.getData();
										if (field.getLength() % 16 > 0
												|| field.getOffset() % 16 > 0) {
											hashCode ^= Comparison.hashCode(
															data,
															field.getOffset() % 16,
															field.getLength() > 0 ? field.getLength() : data.length * 8);
										} else {
											hashCode ^= Comparison
													.hashCode(data);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "PrimaryKey [tag=" + tag + ", fields=" + fields + "]";
	}
}
