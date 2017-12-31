<title>${project.name}</title>

<STYLE>
body{
	margin: 0;
	color: #888888;
	font-size: 15px;
	font-size: 10pt;
	min-width: 1024px;
	font:normal normal 100% arial, Serif;
	font-family: 'HP Simplified';
}
table, td, div, span{
	color: #888888;
	font-size: 15px;
	font-size: 10pt;
	font:normal normal 100% arial, Serif;
	font-family: 'HP Simplified';
}
th {
    text-align: left;
	color: #333333;
	font:normal normal 100% arial, Serif;
	font-family: 'HP Simplified';
	font-weight: normal;
}

.bgcolor--failed {
    background-color: #ff454f;
}
.bgcolor--ignored {
    background-color: #ffb136;
}
.bgcolor--passed {
    background-color: #01a982;
}
.bgcolor--machine {
    background-color: #ff454f;
}
.bgcolor--dark-grey {
    background-color: #3f5262;
}
.bgcolor--grey {
    background-color: #dfe5e9;
}

.color--failed{
	color: #ff454f;
}
.color--ignored {
    color: #ffb136;
}
.color--passed {
    color: #01a982;
}
a.color--link{
	color: #3aadda;
}

.text--xl{
    font-size: 28px;
    font-size: 16pt;
}
.text--lg{
    font-size: 22px;
    font-size: 14pt;
}
.text--md{
    font-size: 18px;
    font-size: 12pt;
}
.text--sm{
    font-size: 15px;
    font-size: 10pt;
}
</STYLE>
<BODY>
<%

import  hudson.Util
import  hudson.Functions
import  hudson.model.Result;
import  hudson.matrix.MatrixBuild
import  hudson.matrix.MatrixRun
import  hudson.matrix.MatrixConfiguration
import  hudson.matrix.MatrixProject
import  hudson.matrix.Combination
import  hudson.tasks.junit.CaseResult

if (build.result == Result.SUCCESS) {
	result_img = "static/e59dfe28/images/32x32/blue.gif"
} else if (build.result == Result.FAILURE) {
	result_img = "static/e59dfe28/images/32x32/red.gif"
} else {
	result_img = "static/e59dfe28/images/32x32/yellow.gif"
}

if (build.builtOnStr == '') {
	built_on = 'master'
} else {
	built_on = build.builtOnStr
}

def healthIconSize = "16x16"
def healthReports = project.buildHealthReports

int total_builds = 0
int total_failed = 0
int total_passed = 0
int total_failed_case = 0
int total_passed_case = 0
int total_skipped_case = 0
int total_all_test_case = 0

int failed_row = 0
int total_row = 0

List<String> axis_1_values = new ArrayList<String>();
List<String> axis_2_values = new ArrayList<String>();

List<String> skipping_machines = new ArrayList<String>();

HashMap<String,String> machines_runs = new HashMap<String,String>();
//HashMap<String,String> test_names = new HashMap<String,String>();
Map<String,String> test_names = new TreeMap<String,String>();
Map<String, List<String>> failed_test_names = new TreeMap<String, ArrayList<String>>();
Map<String, List<String>> skipped_test_names = new TreeMap<String, ArrayList<String>>();
//HashMap<String,List<String>> failed_test_names = new HashMap<String,ArrayList<String>>();
//HashMap<String,List<String>> skipped_test_names = new HashMap<String,ArrayList<String>>();

Map<String, Boolean> builds_vals = new HashMap<String, Boolean>();
Map<String, CaseResult> test_vals = new HashMap<String, hudson.tasks.junit.CaseResult>();

def matrix_build = build
def runs = matrix_build.getExactRuns()

hudson.matrix.MatrixProject matrix_project = project
def matrix_axis = matrix_project.getAxes()

String buildId = null;
String subject = null;
boolean isSameBuild = true;

def axis1 = matrix_axis.find(matrix_project.getUserAxes()[0].getName())
//def axis2 = matrix_axis.find(matrix_project.getUserAxes()[1].getName())

//this part goes over all the runs in the matrix (all the entries in the table) and define a new hashmap <String entry_name, Boolean entry_value>
// we will later use this hashmap when we will print the table to the user

for (hudson.matrix.MatrixRun run : runs) {		
		String axis_1_value
		String axis_2_value
		run_configuration = run.getParent()
		url = run.getUrl()
		
		configuration_combination = run_configuration.getCombination()

		axis_1_value = configuration_combination.get(axis1)
		axis_2_value = "";//configuration_combination.get(axis2)
		
		machines_runs.put(axis_1_value,run);
		
		//build the model of report subject
		if (subject == null){
			subject = run.getEnvironment().get("SUBJECT");
		}
		
		//if this run doesn't have results then skip it
		if (run.getTestResultAction() == null){
			skipping_machines.add(axis_1_value);
			continue;
		}
			
		if (!axis_1_values.contains(axis_1_value)){
			axis_1_values.add(axis_1_value);
		}

		// if (!axis_2_values.contains(axis_2_value)){
		// axis_2_values.add(axis_2_value);
		// }

		total_builds = total_builds + 1;

		if(run.getResult() != Result.SUCCESS){
			total_failed = total_failed + 1;
		}
		else{
			total_passed = total_passed +1;
		}

        builds_vals.put(axis_1_value+"_"+axis_2_value,run.getResult()== Result.SUCCESS);
				
		total_failed_case += run.getTestResultAction().getFailCount();
		total_skipped_case += run.getTestResultAction().getSkipCount();
		total_all_test_case += run.getTestResultAction().getTotalCount();

		//def allTests = new ArrayList();
		run.getTestResultAction().getResult().each{ junitResult ->
		junitResult.getChildren().each
			{ packageResult ->
				packageResult.getChildren().each{  classResult ->
					classResult.getChildren().each{		testResult ->
						if (testResult.isSkipped()){
							if (skipped_test_names.get(testResult.getFullName()) == null){
								skipped_test_names.put(testResult.getFullName(),new ArrayList<String>());	
							}
							skipped_test_names.get(testResult.getFullName()).add(axis_1_value);
						}
						else if (testResult.isFailed()){
							if (failed_test_names.get(testResult.getFullName()) == null){
								failed_test_names.put(testResult.getFullName(),new ArrayList<String>());	
							}
							failed_test_names.get(testResult.getFullName()).add(axis_1_value);
							test_names.put(testResult.getFullName(),getTestName(testResult));
						}
						else{
							test_names.put(testResult.getFullName(),getTestName(testResult));
						}
					
						test_vals.put(getKey(axis_1_value,testResult),testResult);
					}
				}
			}
		}
				 
		//build the model of LR builds (buildId & isSameBuild)
		if (buildId == null){
			buildId = run.getEnvironment().get("BuildID")
		} else if (buildId != run.getEnvironment().get("BuildID")){
			isSameBuild = false;
		}
}

//build the model of report subject
if (subject == null){
	subject = build.getParent().getDescription();
}
%>

	<div>
		<!-- header -->
		<table border="0" cellpadding="0" cellspacing="0" style="width:100%">
			<tr>
				<td class="bgcolor--dark-grey" style="padding-left: 20px; padding-top:9px; padding-bottom:9px;">
					<div style="font-size: 28px; font-size: 16pt; color: white;">${subject}</div>
				</td>
			</tr>
		</table>
		
		<!-- Test result -->
		<table border="0" cellpadding="0" cellspacing="0" style="width:100%">
			<tr>
				<td class="text--lg" style="border-bottom: solid 1px #ccc; padding-left: 20px; padding-top:8px; padding-bottom: 8px;">
					<% if ((total_failed_case == 0) && (total_skipped_case == 0) && (skipping_machines.isEmpty())) {%>
					<div style="padding-left: 10px; border-left: solid 3px #01a982; color: #01a982; font-weight: bold;">SUCCESS</div>
					<%} else if ((total_failed_case > 0) || (!skipping_machines.isEmpty())) {%>
					<div style="padding-left: 10px; border-left: solid 3px #ff454f; color: #ff454f; font-weight: bold;">FAILURE</div>
					<%} else {%>
					<div style="padding-left: 10px; border-left: solid 3px #ffb136; color: #ffb136; font-weight: bold;">WARNING</div>
					<%}%>
				</td>
			</tr>
		</table>
		
		<table style="padding-left: 20px; padding-right: 20px; padding-top: 25px; width: 100%;">
			<tbody>
				<!-- statuses -->
				<tr>
					<td>
						<table border="0" cellpadding="0" cellspacing="0">
							<tr>
								<!-- Execution Summary -->
								<% if ((total_failed_case > 0) || (total_skipped_case > 0) || ((total_all_test_case - total_skipped_case - total_failed_case) > 0)){ %>
								<td>
									<div class="text--md" style="color: #333333;">Execution Summary</div>
									<table border="0" cellpadding="0" cellspacing="0" style="height: 58px;">
										<tr><td style="height: 10px;"></td></tr>
										<tr>
											<% if (total_failed_case > 0){ %>
											<td class="bgcolor--failed" style="width: 78px; padding: 10px 10px 10px 10px;">
												<div class="text--xl" style="text-align: center; font-weight: bold; color: white;">${total_failed_case}</div>
												<div class="text--md" style="text-align: center; font-weight: lighter; color: white;">Failed</div>
											</td>
											<td style="width: 6px;"></td>
											<%}%>
											<% if (total_skipped_case > 0){ %>											
											<td class="bgcolor--ignored" style="width: 78px; padding: 10px 10px 10px 10px;">
												<div class="text--xl" style="text-align: center; font-weight: bold; color: white;">${total_skipped_case}</div>
												<div class="text--md" style="text-align: center; font-weight: lighter; color: white;">Ignored</div>
											</td> 
											<td style="width: 6px;"></td>
											<%}%>
											<% if ((total_all_test_case - total_skipped_case - total_failed_case) > 0){ %>
											<td class="bgcolor--passed" style="width: 78px; padding: 10px 10px 10px 10px;">
												<div class="text--xl" style="text-align: center; font-weight: bold; color: white;">${total_all_test_case - total_skipped_case - total_failed_case}</div>
												<div class="text--md" style="text-align: center;font-weight: lighter; color: white;">Passed</div>
											</td>
											<td style="width: 6px;"></td>
											<%}%>
										</tr>
									</table>
								</td>
								<td style="min-width: 19px;"></td>
								<%}%>
								
								<!-- Machines -->
								<% if (!skipping_machines.isEmpty()){ %>
								<td>
									<span class="text--md" style="color: #333333;">Machines</span>
									<table border="0" cellpadding="0" cellspacing="0" style="height: 58px">
										<tr><td style="height: 10px;"></td></tr>
										<tr>
											<td class="bgcolor--machine" style="padding: 10px 10px 10px 10px; width: 78px;">
												<div class="text--xl" style="text-align: center; color: white; font-weight: bold;">${skipping_machines.size()}/${machines_runs.size()}</div>
												<div class="text--md" style="text-align: center; color: white; font-weight: lighter;">N/A</div>
											</td>
										</tr>
									</table>
								</td>
								<%}%>
							</tr>
						</table>
					</td>
				</tr>
				
				<!-- date/duration/url -->
				<tr>
					<td style="padding-top: 10px;">
						<table border="0" cellpadding="0" cellspacing="10" style="width:100%; border: 2px solid #cfd3d6;">
							<tr>
								<td style="border-right: solid 1px #efefef; width: 150px; padding-left: 5px;">
									<div class="text--md" style="color: #333333;">Date</div>
									<div class="text--lg">${new java.text.SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(build.getTimestamp().getTime())}</div>
								</td>
								<td style="border-right: solid 1px #efefef; width: 250px; padding-left: 20px;">
									<div class="text--md" style="color: #333333;">Duration</div>
										<%
									String duration="";
									if (build.durationString.indexOf("and counting")!=-1){
										duration=build.durationString.substring(0,build.durationString.indexOf("and counting"));
									}
									else{

										duration=build.durationString;
									}
								 
									 %>
									<div class="text--lg">${duration}</div>
								</td> 
								<td style="padding-left: 20px;">
									<div class="text--md" style="color: #333333;">Job URL</div>
									<div class="text--sm"><a class="color--link" href="${rooturl}${build.url}">${rooturl}${build.url}</a></div>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				
				<!-- build info -->
				<tr>
					<td style="padding-top: 10px; font-size: 14px; font-size: 10pt;">
						
						<%if (buildId != null){
							if (isSameBuild){ %>
								<span>LoadRunner build <a class="color--link" href="\\\\mydastr01.hpeswlab.net\\products\\LT\\LT-LR\\win32_release\\${buildId}">${buildId}</a></span>
							<%}
							else{%>
								<span style="padding-right: 5px;">LoadRunner build:</span>
								<%int i=0;
								for(i = 0; i<(axis_1_values.size()); i++){
								    String machine = axis_1_values[i];
									String machineBuildId = machines_runs.get(machine).getEnvironment().get("BuildID");
									if (machineBuildId == null || machineBuildId.length() == 0 ) continue; %>					
									<span>${machine}</span>
									<span><a class="color--link" href="\\\\mydastr01.hpeswlab.net\\products\\LT\\LT-LR\\win32_release\\${machineBuildId}">${machineBuildId}</a></span>
									<%if (i<axis_1_values.size()-1){%><span style="padding-right: 5px;">,</span><%}%>									
								<%}%>
							<%}%>
							<span style="color: #efefef; padding-left: 10px; margin-right: 10px;">|</span>
						<%}%>
						<span><% build.causes.each() { cause -> %> ${cause.shortDescription} <%  } %></span>
					</td>
				</tr>
				
				<!-- Failed Tests -->
				<% if (failed_test_names.keySet().size()>0){ %>
				<tr>
					<td style="padding-top: 35px;">						
						<div class="text--lg" style="margin-bottom: 10px; color: #333333;">Failed Tests</div>
														
						<table border="0" cellpadding="10" cellspacing="0" style="width:100%;">
							<thead class="bgcolor--grey text--sm" style="color: #333333;">
								<th>Test Name</th>
								<th>Environment</th> 
								<th>Age</th>
								<th>Details</th>
								<th>Logs</th>
							</thead>
							<tbody>
								<%for (String test_name: failed_test_names.keySet()){
									List<String> machines = failed_test_names.get(test_name);
									for (String machine: machines ){
										CaseResult result = test_vals.get(machine+"_"+test_name);
										String message = "";
										String stackTrace = "";
										if (result.getErrorStackTrace() != null){
											
											stackTrace = result.getErrorStackTrace();

											if ((stackTrace.indexOf("Exception:")!=-1) && (stackTrace.indexOf("Source:")!=-1)){
												message = stackTrace.substring(stackTrace.indexOf("Exception:")+("Exception:").length(),stackTrace.indexOf("Source:"));
											}

											if ((stackTrace.indexOf("---->")!=-1) && (stackTrace.indexOf("+++++++++++++++++++")!=-1)){
												message = stackTrace.substring(stackTrace.indexOf('---->')+('---->').length(),stackTrace.indexOf('+++++++++++++++++++')-1);
											 
											}
									}	 									
								%>
										<tr>
											<td style="border-bottom:solid #efefef 1px;">
												<div class="color--link">${getTestName(result)}</div>
											</td>
											<td style="border-bottom:solid #efefef 1px;">
												<div>${printMachine(machines_runs,machine)}</div>
											</td> 
											<td style="border-bottom:solid #efefef 1px;">
												<div>${result.getAge()}</div>
											</td>
											<td style="border-bottom:solid #efefef 1px;">
												<div>${message}</div>
											</td>
											<td style="border-bottom:solid #efefef 1px;">
												<div><a class="color--link" href="${rooturl+build.url+getUrl(machine,result)}" style="text-decoration: none;">View</a></div>
											</td>
										</tr>
									<%}%>
								<%}%>
							</tbody>
						</table>				
					</td>
				</tr>
				<%}%>
				
				<!-- Failed Machines -->
				<% if (!skipping_machines.isEmpty()){ %>
				<tr>
					<td style="padding-top: 20px;">
						<div class="text--lg" style="color: #333333;">Failed Machines</div>
						
						<div style="margin-top: 10px; font-size: 14px; font-size: 10pt">
							<%int i=0;
							for(i = 0; i<(skipping_machines.size()-1); i++){ %>							
							<span>${printMachine(machines_runs,skipping_machines[i])}</span>
							<span style="color: #efefef; padding-left: 10px; margin-right: 10px;">|</span>
							<%}%>
							<span>${printMachine(machines_runs,skipping_machines[i])}</span>
						</div>
					</td>
				</tr>
				<%}%>
				
				<!-- Execution Details -->
				<% if (!axis_1_values.isEmpty()){ %>
				<tr>
					<td style="padding-top: 35px;">
						<div class="text--lg" style="margin-bottom: 10px; color: #333333;">Execution Details</div>
						
						<table border="0" cellpadding="0" cellspacing="0" style="width:100%;">
							<thead class="bgcolor--grey text--sm" style="color: #333333;">
								<th style="padding: 9px 10px 9px 10px; font-size: 17px; font-weight: bold;">Test / Machines</th>
								<th style="width: 10px; background-color: white;"></th>
								<%for (String name: axis_1_values){%>
								<th style="padding: 9px 10px 9px 10px; font-size: 17px; font-weight: bold;"> ${printMachine(machines_runs,name)}</th> 
								<%}%>
							</thead>
							<tbody style="">
								<%
								for (String test_name: test_names.keySet()){
									printTableRow(total_row,axis_1_values,test_name,test_vals,test_names.get(test_name));
									total_row = total_row + 1;
								}
								%>
							</tbody>
						</table>
					</td>
				</tr>		
				<%}%>
				
				<!-- Skipped Tests -->
				<% if (skipped_test_names.keySet().size()>0){ %>
				<tr>
					<td style="padding-top: 35px;">
							<div class="text--lg" style="margin-bottom: 10px; color: #333333;">Skipped Tests</div>
							<table border="0" cellpadding="10" cellspacing="0" style="width:100%;">
								<thead class="bgcolor--grey text--sm" style="color: #333333;">
									<th style="width: 30%">Test Name</th>
									<th>VM</th>
									<th>Details</th>
								</thead>
								<tbody>
									<%for (String test_name: skipped_test_names.keySet()){
										List<String> machines = skipped_test_names.get(test_name);
										for (String machine: machines ){
											CaseResult result = test_vals.get(machine+"_"+test_name);
											String message = "";
											if (result.getSkippedMessage() != null){
												message = result.getSkippedMessage();
											}
											%>
											<tr>
												<td style="border-bottom:solid #efefef 1px;">
													<a class="color--link" href="${rooturl+build.url+getUrl(machine,result)}" style="text-decoration: none;" >${getTestName(result)}</a>
												</td>
												<td style="border-bottom:solid #efefef 1px;">
													<div>${printMachine(machines_runs,machine)}</div>
												</td>
												<td style="border-bottom:solid #efefef 1px;">${message}</tr>
										<%}%>
									<%}%>
								</tbody>
							</table>
						</div>
					</td>
				</tr>
				<%}%>
			</tbody>	
		</table>
	</div>

	<%		
		String printMachine(HashMap<String,MatrixRun> machines_runs,String name){
			if (machines_runs.get(name) == null || machines_runs.get(name).getEnvironment().get("OS_NAME") == null)
				return name;
			
			return machines_runs.get(name).getEnvironment().get("OS_NAME").replace("Microsoft Windows","Win").replace("Enterprise","Ent.").replace("Server","Srv").replace("Professional","Pro").replace("Standard","Std") + " ("+name+")";				
		}
		
		String getTestName(CaseResult result){
			if (result.getName().contains("Iteration"))
				return result.getParent().getName() + " - " + result.getName().substring(result.getName().indexOf("Iteration"));
			else
				return result.getName();			
		}
		
		String getKey(String machine,CaseResult result){
			return machine+"_"+result.getFullName();		
		}
		
		String getUrl(String machine, CaseResult result){
			def url = "/"+result.getParent().getName()+"/"+result.getName()+"/";
			return "configuration="+machine+"/testReport/"+result.getParent().getParent().getName()+url.replace(" ","_").replace(":","_").replace("-","_").replace(".","_");		
		}
		
		void printTableRow(total_row,axis_1_values,test_name,test_vals,parentName){
			%>  <tr>
				<td style="border-bottom:solid #efefef 1px; padding: 10px 10px 10px 10px; vertical-align:middle;">${parentName}</td>
				<td style="border-bottom:solid #efefef 1px;"> </td>
				<%
				for (String axis1_value: axis_1_values){
					CaseResult result=test_vals.get(axis1_value+"_"+test_name);
				%>
					<td style="border-bottom:solid #efefef 1px; background-color: white; padding-top:10px; padding-bottom:10px; padding-right: 15px; min-width: 199px;">
						
						    <% if (result == null) {%> 
								<div style="vertical-align: middle; text-align:center;">-</div>
							<%} 
							else{ %>
							<table cellpadding="3" style="width:100%;" bgcolor="<%= (result.isSkipped() )? '#ffb136': result.isPassed()? '#01a982' : '#ff454f' %>">
								<tr>
									<td>
										<table cellpadding="0" style="width:100%;">
											<tr>
												<td style="width: 65%; text-align: center;"> 
													<div style="color: white; font-weight: bold;">${result.getDurationString()}</div>
													<div class="test-text" style="color: white;">Duration</div>
												</td>	
												<% if (result!=null && !result.isPassed()){ %>									
												<td style="text-align: center; <%= (result.isSkipped() )? 'border-left: solid 1px #f3aa3d;' : 'border-left: solid 1px #da2630;' %>">									
													<div style="color: white; font-weight: bold;">${result.getAge()}</div>
													<div style="color: white;"">Age</div>
												</td>
												<%}%>
											</tr>
										</table>
									</td>
								</tr>
							</table>
							<%}%>								
					</td>				
				<%}%>
			</tr>
	<%}%>
</BODY>
