package ar.edu.itba.pdc.duta.proxy;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import ar.edu.itba.pdc.duta.admin.Stats;
import ar.edu.itba.pdc.duta.http.model.MessageHeader;
import ar.edu.itba.pdc.duta.http.model.RequestHeader;
import ar.edu.itba.pdc.duta.http.parser.ParseException;
import ar.edu.itba.pdc.duta.http.parser.RequestParser;
import ar.edu.itba.pdc.duta.net.AbstractChannelHandler;
import ar.edu.itba.pdc.duta.net.buffer.DataBuffer;
import ar.edu.itba.pdc.duta.net.buffer.WrappedDataBuffer;
import ar.edu.itba.pdc.duta.proxy.operation.Operation;

public class RequestChannelHandler extends AbstractChannelHandler {

	private static Logger logger = Logger.getLogger(RequestChannelHandler.class);

	private RequestParser parser;

	private Operation op;

	private DataBuffer buffer;

	@Override
	public void read(SocketChannel channel) throws IOException {

		if (op == null) {

			op = new Operation(this);
			parser = new RequestParser();
		}

		buffer = op.getRequestBuffer();

		int read = buffer.readFrom(channel);
		
		if (read == -1) {
			abort();
			return;
		}

		Stats.addClientTraffic(read);

		if (parser != null) {
			processHeader();
		}

		if (op != null && parser == null && buffer.hasReadableBytes()) {
			
			//TODO: If it is complete, a new one should start and be queue'd
			if  (!op.isRequestComplete()) {
				op.addRequestData(buffer);
			} else {
				logger.warn("Got unexpected data for a request");
				logger.warn(buffer.toString());
			}
		}
	}

	private void processHeader() {

		try {
			parser.parse(buffer);
		} catch (ParseException e) {
			logger.error("Aborting request due to malformed headers", e);
			close();
			return;
		} catch (IOException e) {
			logger.error("Failed to read headers, aborting", e);
			close();
			return;
		}

		MessageHeader header = parser.getHeader();

		if (header != null) {

			logger.debug("Have full header, giving to op...");
			logger.debug(header);

			op.setRequestHeader((RequestHeader) header);

			parser = null;
			if (buffer.hasReadableBytes()) {
				buffer = new WrappedDataBuffer(buffer, buffer.getReadIndex(), buffer.remaining());
			}
		}
	}

	@Override
	public void close() {
		super.close();
		Stats.closeInbound();
	}
	
	@Override
	public void abort() {
		op.abort();
		op = null;
		
		close();
	}
	

	@Override
	public void wroteBytes(long bytes) {
		Stats.addClientTraffic(bytes);
	}

	public void operationComplete() {
		logger.debug("Detaching from op...");
		op = null;
	}
}
