package ar.edu.itba.pdc.duta.proxy.filter;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import nl.bitwalker.useragentutils.Browser;
import nl.bitwalker.useragentutils.OperatingSystem;
import ar.edu.itba.pdc.duta.admin.AdminFilter;
import ar.edu.itba.pdc.duta.admin.Stats;
import ar.edu.itba.pdc.duta.http.model.MessageHeader;
import ar.edu.itba.pdc.duta.net.Server;
import ar.edu.itba.pdc.duta.proxy.filter.http.HttpFilter;

public class Filters {

	private Filter adminFilter = new AdminFilter();

	private Filter httpFilter = new HttpFilter();


	private ConcurrentMap<Object, BlockingQueue<Integer>> filterMultimap = new ConcurrentHashMap<Object, BlockingQueue<Integer>>();

	private Map<Integer, Filter> filterIds = new HashMap<Integer, Filter>();

	private ConcurrentMap<Integer, Set<Object>> filterMatches = new ConcurrentHashMap<Integer, Set<Object>>();

	private AtomicInteger id = new AtomicInteger();

	public List<Filter> getFilterList(SocketChannel channel, MessageHeader header) {

		List<Filter> ret = new ArrayList<Filter>();

		ret.add(httpFilter);

		if (channel.socket().getLocalPort() == Server.adminPort) {

			ret.add(adminFilter);
			return ret;
		}

		Set<Integer> matchingFilters = new HashSet<Integer>();

		String ua = header.getField("User-Agent");
		if (ua != null) { 
			BlockingQueue<Integer> browserList = filterMultimap.get(Browser.parseUserAgentString(ua));
			if (browserList != null) {
				matchingFilters.addAll(browserList);
			}
			
			BlockingQueue<Integer> osList = filterMultimap.get(OperatingSystem.parseUserAgentString(ua));
			if (osList != null) {
				matchingFilters.addAll(osList);
			}
		}

		BlockingQueue<Integer> ipList = filterMultimap.get(channel.socket().getLocalAddress().getHostAddress());
		if (ipList != null) {
			matchingFilters.addAll(ipList);
		}

		for (Integer id : matchingFilters) {
			Filter filter = filterIds.get(id);
			ret.add(filter);
			Stats.applyFilter(filter.getClass());
		}

		Collections.sort(ret, new Comparator<Filter>() {

			@Override
			public int compare(Filter f1, Filter f2) {

				return f2.getPriority() - f1.getPriority();
			}
		});

		return ret;
	}


	public int addFilter(Set<Object> matches, Filter filter) {

		int id = this.id.incrementAndGet();

		filterMatches.put(id, matches);
		filterIds.put(id, filter);

		for (Object match : matches) {

			filterMultimap.putIfAbsent(match, new LinkedBlockingQueue<Integer>());
			filterMultimap.get(match).add(id);
		}

		return id;
	}


	public boolean removeFilter(int id) {

		Set<Object> matches = filterMatches.remove(id);

		if (matches == null) {
			return false;
		}

		for (Object match : matches) {
			filterMultimap.get(match).remove(id);
		}

		return filterIds.remove(id) != null;
	}
}
