package ar.edu.itba.pdc.duta.proxy.filter.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.log4j.Logger;

import ar.edu.itba.pdc.duta.admin.Stats;
import ar.edu.itba.pdc.duta.http.model.MediaType;
import ar.edu.itba.pdc.duta.http.model.Message;
import ar.edu.itba.pdc.duta.http.model.MessageHeader;
import ar.edu.itba.pdc.duta.proxy.filter.Filter;
import ar.edu.itba.pdc.duta.proxy.filter.FilterPart;
import ar.edu.itba.pdc.duta.proxy.filter.Interest;
import ar.edu.itba.pdc.duta.proxy.operation.Operation;

public class L33tFilter implements Filter {
	
	{
		Stats.registerFilterType(L33tFilter.class);
	}

	@Override
	public FilterPart getRequestPart() {
		return null;
	}

	@Override
	public FilterPart getResponsePart() {
		return new ResponsePart();
	}
	
	private static class ResponsePart extends FilterPart {

		private Logger logger = Logger.getLogger(ResponsePart.class);
		
		private Charset charset;

		@Override
		public Interest checkInterest(MessageHeader header) {
			
			boolean isText = false;
			MediaType contentType = new MediaType(header.getField("Content-Type"));
			if (contentType.getType().equalsIgnoreCase("text/plain")) {
				
				String encoding = contentType.getParameter("charset");
				if (encoding == null) {
					encoding = "ISO-8859-1";
				}
				
				if (Charset.isSupported(encoding)) {
					charset = Charset.forName(encoding);
					isText = true;
				} else {
					isText = false;
					logger.warn("Skipping l33t filtering due to unsupported encoding " + encoding);
				}
			}
			
			
			return new Interest(false, false, isText);
		}
		
		
		@Override
		public Message filter(Operation op, Message msg) {

			List<ByteBuffer> buffers = msg.getBody();

			int size = 0;
			for (ByteBuffer byteBuffer : buffers) {
				size += byteBuffer.limit();
			}
			
			byte[] bytes = new byte[size];
			int i = 0;
			for (ByteBuffer buffer : buffers) {
				buffer.get(bytes, i, buffer.remaining());
				buffer.rewind();
				i += buffer.remaining();
			}
			
			String body = new String(bytes, charset);
			
			body = body.replace('e', '3')
				.replace('a', '4')
				.replace('i', '1')
				.replace('o', '0');
			
			msg.setBody(ByteBuffer.wrap(body.getBytes(charset)));
		
			return null;
		}
	}

}
