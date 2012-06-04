package ar.edu.itba.pdc.duta.proxy.filter.http;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ar.edu.itba.pdc.duta.admin.Stats;
import ar.edu.itba.pdc.duta.http.Grammar;
import ar.edu.itba.pdc.duta.http.model.Message;
import ar.edu.itba.pdc.duta.http.model.MessageHeader;
import ar.edu.itba.pdc.duta.http.model.RequestHeader;
import ar.edu.itba.pdc.duta.http.model.ResponseHeader;
import ar.edu.itba.pdc.duta.proxy.filter.Filter;
import ar.edu.itba.pdc.duta.proxy.filter.FilterPart;
import ar.edu.itba.pdc.duta.proxy.filter.Interest;
import ar.edu.itba.pdc.duta.proxy.operation.Operation;

public class URIFilter implements Filter {
	
	{
		Stats.registerFilterType(URIFilter.class);
	}

	private final Pattern pattern; 
	private boolean blocked=false;
	
	public URIFilter(String uriBlocked) {
		super();
		Pattern p;
		try{
			p = Pattern.compile(uriBlocked);
		} catch (PatternSyntaxException pE) {
			//invalid regular expression
			p = null;
		}
		this.pattern = p;
	}

	@Override
	public FilterPart getRequestPart() {
		return new RequestPart();
	}

	@Override
	public FilterPart getResponsePart() {
		return new  ResponsePart();
	}
	
	private class RequestPart extends FilterPart {

		@Override
		public Interest checkInterest(MessageHeader header) {
			return new Interest(true, false, false);
		}

		@Override
		public Message processHeader(Operation op, MessageHeader header) {
			
			if(pattern == null) {
				return null;
			}
			
			RequestHeader request = (RequestHeader) header;
			
			String url = header.getField("host") + request.getRequestURI();
			
			if(pattern.matcher(url).find()){
				blocked = true;
			}
			
			return null;
		}

	}
	
	private  class ResponsePart extends FilterPart {
		
		@Override
		public Interest checkInterest(MessageHeader header) {
			return new Interest(true, false, false);
		}
		
		@Override
		public Message processHeader(Operation op, MessageHeader header) {
			if(blocked) {
				return block();
			}
			return null;
		}
		
		private Message block() {
			Map<String, String> fields = new HashMap<String, String>();
			
			fields.put("Date", new Date().toString());
			fields.put("Content-Length", "0");
			
			ResponseHeader headers = new ResponseHeader(Grammar.HTTP11, 404, "Not Found", fields);
			return new Message(headers);
		}
		
	}

}