package havis.middleware.ale.core.manager;

import havis.middleware.ale.Helper;
import havis.middleware.utils.threading.Pipeline;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Callback<T> implements HttpHandler {

	Pipeline<T> pipeline = new Pipeline<T>();
	Unmarshaller unmarshaller;
    Class<T> clazz;

	public Callback(URL url, Class<T> clazz) throws IOException, JAXBException {
		this.unmarshaller = Helper.createUnmarshaller(clazz);
        this.clazz = clazz;

		HttpServer server = HttpServer.create(new InetSocketAddress(url.getPort()), 0);
		server.createContext(url.getPath(), this);
		server.setExecutor(null);
		server.start();
	}

	public void callbackResults(T t) {
		pipeline.enqueue(t);
	}

	public T dequeue() {
		return pipeline.dequeue();
	}

	public void dispose() {
	}

	@Override
    public void handle(HttpExchange exchange) throws IOException {
		try {
			T t = unmarshaller.unmarshal(new StreamSource(exchange.getRequestBody()), clazz).getValue();
			pipeline.enqueue(t);
			exchange.sendResponseHeaders(200, 0);
			exchange.close();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
