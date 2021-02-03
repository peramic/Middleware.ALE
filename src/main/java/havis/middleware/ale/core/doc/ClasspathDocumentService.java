package havis.middleware.ale.core.doc;

import havis.middleware.ale.service.doc.DocumentService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for documents from the class path
 */
public class ClasspathDocumentService implements DocumentService {

	private static final ClasspathDocumentService instance = new ClasspathDocumentService();

	private static final int BUFFER_SIZE = 8192;

	private Map<String, Document> documents = new HashMap<>();

	/**
	 * Document name for the vendor specification file
	 */
	public static final String VENDOR_SPECIFICATION_NAME = "ALE_Vendor_Specification.pdf";

	/**
	 * @return an instance of the document service
	 */
	public static DocumentService getInstance() {
		return instance;
	}

	private ClasspathDocumentService() {
		Document document = new Document() {
			@Override
			public String getName() {
				return VENDOR_SPECIFICATION_NAME;
			}

			@Override
			public String getMimetype() {
				return "application/pdf";
			}

			@Override
			public InputStream getContent() {
				return this.getClass().getResourceAsStream(getName());
			}

			@Override
			public long getSize() {
				try {
					return this.getClass().getResource(getName()).openConnection().getContentLengthLong();
				} catch (IOException e) {
					return 0;
				}
			}
		};
		this.documents.put(document.getName(), document);
	}

	@Override
	public boolean hasDocument(String name) {
		return this.documents.containsKey(name);
	}

	@Override
	public boolean writeContent(String name, OutputStream stream) {
		if (this.documents.containsKey(name)) {
			return write(this.documents.get(name).getContent(), stream);
		}
		return false;
	}

	@Override
	public InputStream getContent(String name) {
		if (this.documents.containsKey(name)) {
			return this.documents.get(name).getContent();
		}
		return null;
	}

	private static boolean write(InputStream in, OutputStream out) {
		if (in != null && out != null) {
			try {
				byte[] buf = new byte[BUFFER_SIZE];
				int n;
				while ((n = in.read(buf)) > 0) {
					out.write(buf, 0, n);
				}
			} catch (IOException e) {
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public String getMimetype(String name) {
		if (this.documents.containsKey(name)) {
			return this.documents.get(name).getMimetype();
		}
		return null;
	}

	@Override
	public long getSize(String name) {
		if (this.documents.containsKey(name)) {
			return this.documents.get(name).getSize();
		}
		return -1;
	}
}
