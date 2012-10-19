[#ftl]
[#include "/WEB-INF/pages/include/header_administration.ftl"]
<div class="container">
	<div class="row">
	
		<div class="span3">
			<ul class="nav nav-list sibsp-sidenav affix-top">
          		<li class="active"><a href="${rootURL}/administration/"><i class="icon-chevron-right"></i> [@s.text name="sibsp.application.administration.general"/]</a></li>
          		<li class=""><a href="#buttonGroups"><i class="icon-chevron-right"></i> [@s.text name="sibsp.application.administration.useraccounts"/]</a></li>
          		<li class=""><a href="#buttonDropdowns"><i class="icon-chevron-right"></i> [@s.text name="sibsp.application.administration.coretypesandextensions"/]</a></li>
        	</ul>
		</div>
		
		<div class="span9">
			<div class="content content-full-witdh">    	
				<div class="content-inner">
    	
					<h2>[@s.text name="sibsp.application.administration.general.title"/]</h2>
					<h4>[@s.text name="admin.config.setup.title"/]</h4>
					<blockquote><p>[@s.text name="admin.config.setup.message"/]</p></blockquote>
		
					[@s.actionmessage/]
					[#if warnings?size>0]
					<div class="alert alert-error">	    
						<ul>
							[#list warnings as w]
							<li><span>${w!}</span></li>
							[/#list]
						</ul>
					</div>
        			[/#if]
        			[#if (actionErrors?exists && actionErrors?size > 0)]
        			<div class="alert alert-error">	
        				<ul>
        					[#list actionErrors as error]
        					<li><span>${error}</span></li>
        					[/#list]
        				</ul>
        			</div>
        			[/#if]
		
					[@s.form action="configuration" method="post" cssClass="form-horizontal"]
					<div class="control-group">
						<label class="control-label" for="dataDir">[@s.text name="admin.config.setup.datadir.label"/] <a href="#" rel="tooltip" data-placement="top" data-original-title="[@s.text name="admin.config.setup.datadir.label.tooltip"/]"><i class="icon-info-sign"></i></a></label>
						<div class="controls">
							[@s.textfield key="admin.config.setup.datadir" name="dataDirPath" size="80" required="true" value="${dataDir}" readonly="true" cssStyle="width:306px;" placeholder="%{getText('admin.config.setup.datadir.label')}"/]
						</div>
					</div>
					<div class="control-group">
						<label class="control-label" for="urlPath">[@s.text name="admin.config.setup.urlpath.label"/] <a href="#" rel="tooltip" data-placement="top" data-original-title="[@s.text name="admin.config.setup.urlpath.label.tooltip"/]"><i class="icon-info-sign"></i></a></label>
						<div class="controls">
							[@s.textfield key="admin.config.setup.urlpath" name="rootURL" size="80" required="true" cssStyle="width:306px;" value="${rootURL}" placeholder="%{getText('admin.config.setup.urlpath.label')}"/]
						</div>
					</div>
					<div class="control-group">
						<div class="controls">
							<button type="submit" class="btn">[@s.text name="admin.config.setup.saveButton"/]</button>
						</div>
					</div>
					[/@s.form]
	
    
				</div>	
			</div>
		</div>
		
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]