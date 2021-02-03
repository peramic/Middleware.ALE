package havis.middleware.ale.core;

import havis.middleware.ale.base.operation.tag.Tag.Decoder;
import havis.middleware.misc.TdtWrapper;

public class ISODecoder implements Decoder<byte[]> {

	private static ISODecoder instance = new ISODecoder();

	public static ISODecoder getInstance() {
		return instance;
	}

	private ISODecoder() {
	}

	@Override
	public Object decode(int bank, byte[] data) {
		return TdtWrapper.getItemDataDecoder().decode(bank, data);
	}
}
