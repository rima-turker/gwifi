package org.fiz.ise.gwifi.dataset.LINE.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fiz.ise.gwifi.util.TimeUtil;

@Deprecated
public class TEST {

	public static void main(String[] args) {
		Set<String> s = new HashSet<>();
		s.add("rima");
		s.add("turker");
		s.add("sevda");
		s.add("fuad");
		
		Set<String> t = new HashSet<>();
		t.add("rima");
		t.add("fuad");
		
		Set<String> z = new HashSet<>();
		z.add("rima");
		z.add("turker");
		z.add("sevda");
		z.add("fuad");
		
		
		s.retainAll(t);
		System.out.println(s);
		
		s.retainAll(z);
		
		
		//setTemp.retainAll(setMain);
		
		
		Map<String, Long> map = new HashMap<>();
		//map.put("rima", 2L);			
		
		List<String> lst = new ArrayList<>();
		
		
		Set<Integer> set = new HashSet<>();
		Long start = TimeUtil.getStart();
		for (int i = 0; i < 5000000; i++) {
			for (int j = 0; j < 5000000; j++) {
//				lst.add("rima");
//				lst.add("turker");
//				lst.add("rima");
//				lst.add("turker");
//				lst.add("ev");
//				lst.add("bank");
				
				set.add((i*j)+1);
			}
			System.out.println(i);
		}
	
		System.out.println("Filling the list "+TimeUtil.getEnd(TimeUnit.NANOSECONDS, start));
		start = TimeUtil.getStart();
		lst.stream().parallel().collect(Collectors.groupingBy(p -> p, ()-> map,Collectors.counting()));
		
		//localList.stream().collect(Collectors.groupingBy(p -> p, ()-> localMap,Collectors.counting()));
		System.out.println("Conver collections "+TimeUtil.getEnd(TimeUnit.NANOSECONDS, start));
//		map.forEach((k,v)->System.out.println("Item : " + k + " Count : " + v));
		
		map.clear();
		start = TimeUtil.getStart();
		for(String str:lst)
		{
			map.put(str, map.getOrDefault(str, 0L) + 1);
		}
		System.out.println("Conver classical map "+TimeUtil.getEnd(TimeUnit.NANOSECONDS, start));
		
		
		map.clear();
		start = TimeUtil.getStart();
		for(String str:lst)
		{
			Long responce = map.get(str);
			if (responce==null) {
				map.put(str,1L);
			}
			else
			{
				map.put(str,map.get(str)+1L);
			}
		}
		System.out.println("Conver classical classical map "+TimeUtil.getEnd(TimeUnit.NANOSECONDS, start));
		//map.forEach((k,v)->System.out.println("Item : " + k + " Count : " + v));
		
		
//		lst.stream().collect(Collectors.groupingBy(p -> p, ()-> map,Collectors.counting()));
//		
//		for(String str:lst) {
//			map.put(str, map.getOrDefault(str, (long)0) + 1);
//		}
//		map.forEach((k,v)->System.out.println("Item : " + k + " Count : " + v));
		
//		List<Article> lst = new ArrayList<>();
//		lst.add("Albert");
		
		

	}
//	private void handleSequential(List<String> articleList, int i2) {
//		final Map<String,Long> localMap = new ConcurrentHashMap<>();
//		final List<String> pairs = new ArrayList<>();
//		long start = TimeUtil.getStart();
//		for(final String article: articleList) {
//			final String[] linksOut = article.getLinksOut();
//
//			for(int i=0;i<linksOut.length;i++) {
//				for(int j=i+1;j<linksOut.length;j++) {					
//					pairs.add(linksOut[i].getId()<linksOut[j].getId()?linksOut[i].getId()+"\t"+linksOut[j].getId():linksOut[j].getId()+"\t"+linksOut[i].getId());
//				}
//			}
//		}
//		pairs.stream().collect(Collectors.groupingBy(p -> p, ()-> localMap,Collectors.counting()));
//		localMap.forEach((k,v)->System.out.println("Item : " + k + " Count : " + v));
////		map.putAll(localMap);
////		pairs.stream().collect(Collectors.groupingBy(p -> p, ()-> map,Collectors.counting()));
//	}
}
