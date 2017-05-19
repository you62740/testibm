package com.cats.list.views;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import com.cats.services.AbstractPageList;
import com.cats.utils.VanillaUtils;

@Component
@Service(value=AbstractPageList.class)
public class LinkViewPageList extends AbstractPageList {

	@Override
	public List<?> display() {
		return super.pages.stream()
				.map(e-> VanillaUtils.constructPageLinkModel(e))
				.collect(Collectors.toList());
	}

}
