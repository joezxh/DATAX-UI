package net.iharding.modules.etl.controller;

import org.guess.core.web.BaseController;

import net.iharding.modules.meta.model.ColumnPair;
import net.iharding.modules.meta.service.ColumnPairService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
* 
* @ClassName: TaskColumn
* @Description: TaskColumnController
* @author Joe.zhang
* @date  2016-4-3 23:30:55
*
*/
@Controller
@RequestMapping("/etl/taskColumn")
public class TaskColumnController extends BaseController<ColumnPair>{

	{
		editView = "/etl/taskColumn/edit";
		listView = "/etl/taskColumn/list";
		showView = "/etl/taskColumn/show";
	}
	
	@Autowired
	private ColumnPairService taskColumnService;
}