package havis.middleware.ale.core;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.Tag.Decoder;
import havis.middleware.ale.base.operation.tag.Tag.Property;
import havis.middleware.misc.TdtWrapper;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;

public class TagDecoder implements Decoder<byte[]> {

	private static TagDecoder instance = new TagDecoder();

	public static TagDecoder getInstance() {
		return instance;
	}

	private TagDecoder() {
	}

	public Tag enable(Tag tag) {
		tag.setTagInfoDecoder(this);
		return tag;
	}

	@Override
	public Object decode(int bank, byte[] data) {
		try {
			return TdtWrapper.getTdt().translate(data);
		} catch (TdtTranslationException e) {
			return null;
		}
	}

	public byte[] decodeUrn(String urn) throws TdtTranslationException {
		TdtTagInfo info = TdtWrapper.getTdt().translate(urn);
		return info.getEpcData();
	}

	public Tag fromUrn(String urn, byte[] tid) {
		TdtTagInfo tagInfo = null;
		byte[] epc = null;
		try {
			tagInfo = TdtWrapper.getTdt().translate(urn);
			epc = tagInfo.getEpcData();
		} catch (TdtTranslationException e) {
			// ignore
		}

		Tag tag = new Tag(epc, tid);
		tag.setTagInfoDecoder(this);
		tag.setProperty(Property.TAG_INFO, tagInfo);
		return tag;
	}

	public Tag fromUrn(String urn) {
		return fromUrn(urn, null);
	}
}
