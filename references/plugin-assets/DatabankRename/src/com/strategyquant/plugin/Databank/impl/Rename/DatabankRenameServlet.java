package com.strategyquant.plugin.Databank.impl.Rename;

import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.channels.ProjectChannels;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;

class DatabankRenameServlet extends HttpJSONServlet {
	
	private static final Logger Log = LoggerFactory.getLogger(DatabankRenameServlet.class);
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	/*
	 * Handles the incoming request
	 * 
	 * @param command the HTTP pathname
	 * @param parameterMap the HTTP paremeters
	 * @param method the HTTP method type
	 * @return JSON response
	 */
	@Override
	protected String execute(String command, Map<String, String[]> parameterMap, String method) throws Exception {	
		switch(command) {
			case "rename": return onRename(parameterMap);		
			default: 
				throw new Exception("Unknown command '"+command+"'.");
		}
	}

	//------------------------------------------------------------------------
	
	/*
	 * Renames the strategies
	 * 
	 * @param args the HTTP paremeters
	 * @return JSON response
	 */
	private String onRename(Map<String, String[]> args) throws Exception {
		checkParamExists(args, new String[]{"projectName", "databankName", "strategies"});
		
		JSONObject response = new JSONObject();
		
		String projectName = tryGetParamValue(args, "projectName");
		String databankName = tryGetParamValue(args, "databankName");
		String strategies = getParam(args, "strategies", "all");
		
		//project
		SQProject project = ProjectEngine.get(projectName);
		if(!ProjectEngine.projectExists(projectName)) {
			throw new Exception("Project '"+projectName+"' doesn't exist.");
		}
		
		if(!project.getDatabanks().containsKey(databankName)){
			throw new Exception("Databank '"+databankName+"' doesn't exist.");
		}

		//databank
		Databank databank = project.getDatabanks().get(databankName);
		
		//strategies
		String stgs[] = strategies.split(",");
		if(stgs.length == 1 && stgs[0].equals("all")){
			stgs = new String[databank.size()];
			databank.getRecordKeys().toArray(stgs);
		}
		
		String name = null, prefix = "", postfix = "";
		
		if(stgs.length==1) { //only one strategy selected, rename it
			name = tryGetParamValue(args, "name");
		} else {
			//two or more strategies selected, add prefix / postfix to its original name
			try { prefix = tryGetParamValue(args, "prefix"); } catch(Exception e) {}
			try { postfix = tryGetParamValue(args, "postfix"); } catch(Exception e) {}
		}
		
		String lockName = "RenameStrategies";
		String actionName = "RenameStrategies";
		
		double count = 0; 
		
		//displaying progress dialog
		project.publisher.resetLastData(ProjectChannels.PROGRESS_CHANNEL);
		project.getProgress().update(actionName, 0, null);
		
		ResultsGroup rg, newRg;
			
		for(int i=0; i<stgs.length; i++){
			String strategyName = stgs[i];
			
			rg = null;
			
			try {
				rg = databank.getLocked(strategyName, lockName);
				newRg = rg.clone();
				
				if(stgs.length==1) {
					newRg.setName(name);
				} else {
					newRg.setName(prefix+newRg.getName()+postfix);
				}	
				
				//renaming strategy
				databank.remove(strategyName, true, true, false, true, lockName);
				databank.add(newRg, true);
			} catch(Exception e) {
				Log.error("Error while renaming strategy '" + strategyName + "'", e);
			} finally {
				if(rg != null) {
					rg.releaseLock(lockName);
				}
				
				count++;
				int percent = (int) (count / stgs.length * 100);
				project.getProgress().update("RenameStrategies", percent, null);
			}
		}
		
		//closing progress dialog
		project.getProgress().update(actionName, 100, null);

		//sending json response
		response.put("success", "ok");
		return response.toString();
	}
	
}
