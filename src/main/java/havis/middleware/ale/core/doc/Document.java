package havis.middleware.ale.core.doc;

import java.io.InputStream;

interface Document {

	String getName();

	long getSize();

	String getMimetype();

	InputStream getContent();

}
