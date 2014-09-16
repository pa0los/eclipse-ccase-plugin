package net.sourceforge.eclipseccase.diff;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.sourceforge.clearcase.ClearCaseException;
import net.sourceforge.clearcase.commandline.CommandLauncher;
import net.sourceforge.eclipseccase.ClearCasePlugin;
import net.sourceforge.eclipseccase.ClearCasePreferences;
/**
 * Class implements the commands for Kdiff3 to perform compare and
 * merge operations.
 * 
 * @author mikael petterson
 *
 */
public class KdiffCommands extends AbstractExternalToolCommands {

	public KdiffCommands() {

	}

	// kdiff3 file1 file2
	public void twoWayDiff(String file1, String file2) {
		String[] command = new String[] { getExec(), file1, file2 };
		CommandLauncher launcher = new CommandLauncher();
		launcher.execute(command, null, null, null);
	}

	public void threeWayDiff(String file1, String file2, String base) {
		String[] command = new String[] { getExec(), file1, file2, base };
		CommandLauncher launcher = new CommandLauncher();
		launcher.execute(command, null, null, null);
	}

	public String getExec() {
		String selectedTool = ClearCasePreferences.getExtDiffTool();
		Map<String, String> toolPathMap = PreferenceHelper
				.strToMap(ClearCasePreferences.getExtDiffExecPath());
		return PreferenceHelper.getExecPath(selectedTool, toolPathMap);

	}
	
	public String getMergeExec(){
		String selectedTool = ClearCasePreferences.getExtMergeTool();
		Map<String, String> toolPathMap = PreferenceHelper
				.strToMap(ClearCasePreferences.getExtMergeExecPath());
		return PreferenceHelper.getExecPath(selectedTool, toolPathMap);
	}

	@Override
	public Status twoWayMerge(String file1, String file2) {
		String [] errMsg = null;
		String[] command = new String[] { getMergeExec(), file1, file2, "-o", file2 };
		CommandLauncher launcher = new CommandLauncher();
		launcher.execute(command, null, null, null);
		//Check if command was ok.
		if(launcher.getExitValue() != 0){
			errMsg = launcher.getErrorOutput();
			if(errMsg == null){
				return new Status(IStatus.ERROR, "Plugin id here", Messages.getString("KdiffCommands.threeWayMerge.fail.unknown"));
			}else{
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < errMsg.length; i++) {
					sb.append(errMsg[i]);
					sb.append('\n');
				}
				return new Status(IStatus.ERROR, ClearCasePlugin.PLUGIN_ID,sb.toString());
			}
		}
		
		//everything was ok!
		return new Status(IStatus.OK, ClearCasePlugin.PLUGIN_ID,  Messages.getString("KdiffCommands.threeWayMerge.ok"));
		
	}

	@Override
	public Status threeWayMerge(String file1, String file2, String base) {
		String [] errMsg = null;
		String[] command = new String[] { getMergeExec(), file1, file2,base,"-o", file2};
		CommandLauncher launcher = new CommandLauncher();
		try{
		launcher.execute(command, null, null, null);
		//Not sure if we need to catch this!
		}catch (ClearCaseException cce) {
			return new Status(IStatus.ERROR,ClearCasePlugin.PLUGIN_ID,cce.getMessage(),cce);
		}
		//Check if command was ok.
		if(launcher.getExitValue() != 0){
			errMsg = launcher.getErrorOutput();
			if(errMsg == null){
				return new Status(IStatus.ERROR, ClearCasePlugin.PLUGIN_ID, Messages.getString("KdiffCommands.threeWayMerge.fail.unknown"));
			}else{
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < errMsg.length; i++) {
					sb.append(errMsg[i]);
					sb.append('\n');
				}
				return new Status(IStatus.ERROR, ClearCasePlugin.PLUGIN_ID,sb.toString());
			}
		}
		
		//everything was ok!
		return new Status(IStatus.OK, ClearCasePlugin.PLUGIN_ID,  Messages.getString("KdiffCommands.threeWayMerge.ok"));
		
	}
	

}
